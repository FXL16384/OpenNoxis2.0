package noxisclient.mod.modules.impl.player;

import net.minecraft.block.BlockState;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.BlockPos.Mutable;
import noxisclient.Alien;
import noxisclient.api.events.impl.ClientTickEvent;
import noxisclient.api.events.impl.UpdateEvent;
import noxisclient.api.utils.Wrapper;
import noxisclient.api.utils.math.Timer;
import noxisclient.api.utils.player.EntityUtil;
import noxisclient.api.utils.player.InventoryUtil;
import noxisclient.api.utils.world.BlockPosX;
import noxisclient.mod.modules.Module;
import noxisclient.mod.modules.settings.impl.BindSetting;
import noxisclient.mod.modules.settings.impl.BooleanSetting;
import noxisclient.mod.modules.settings.impl.SliderSetting;

public class AutoPotion extends Module {
   public static AutoPotion INSTANCE;
   private final SliderSetting delay = this.add(new SliderSetting("Delay", 1.0, 0.0, 10.0, 0.1).setSuffix("s"));
   private final BooleanSetting speed = this.add(new BooleanSetting("Speed", false));
   private final BooleanSetting resistance = this.add(new BooleanSetting("Resistance", false));
   private final BooleanSetting strength = this.add(new BooleanSetting("Strength", false));
   private final BooleanSetting usingPause = this.add(new BooleanSetting("UsingPause", true));
   private final BooleanSetting dynamic = this.add(new BooleanSetting("Smart", false));
   private final BooleanSetting onlyGround = this.add(new BooleanSetting("OnlyGround", false));
   private final BooleanSetting inventory = this.add(new BooleanSetting("InventorySwap", true));
   private final BindSetting speedKey = this.add(new BindSetting("SpeedKey", -1));
   private final BindSetting strengthKey = this.add(new BindSetting("StrengthKey", -1));
   private final BindSetting resistanceKey = this.add(new BindSetting("ResistanceKey", -1));
   private final Timer delayTimer = new Timer();
   private boolean throwing = false;
   private boolean turtlePress;
   private boolean speedPress;
   private boolean strengthPress;

   public AutoPotion() {
      super("AutoPotion", "Automatically throw potions when needed. / 在需要时自动喷药", Module.Category.Player);
      this.setChinese("自动药水");
      INSTANCE = this;
      Alien.EVENT_BUS.subscribe(new AutoPotion.AutoPotTick());
   }

   public static int findPotionInventorySlot(StatusEffect targetEffect) {
      for (int i = 35; i >= 0; i--) {
         ItemStack itemStack = mc.player.getInventory().getStack(i);
         if (Item.getRawId(itemStack.getItem()) == Item.getRawId(Items.SPLASH_POTION)) {
            PotionContentsComponent potionContentsComponent = itemStack.getOrDefault(DataComponentTypes.POTION_CONTENTS, PotionContentsComponent.DEFAULT);

            for (StatusEffectInstance effect : potionContentsComponent.getEffects()) {
               if (effect.getEffectType().value() == targetEffect) {
                  return i < 9 ? i + 36 : i;
               }
            }
         }
      }

      return -1;
   }

   public static int findPotion(StatusEffect targetEffect) {
      for (int i = 0; i < 9; i++) {
         ItemStack itemStack = mc.player.getInventory().getStack(i);
         if (Item.getRawId(itemStack.getItem()) == Item.getRawId(Items.SPLASH_POTION)) {
            PotionContentsComponent potionContentsComponent = itemStack.getOrDefault(DataComponentTypes.POTION_CONTENTS, PotionContentsComponent.DEFAULT);

            for (StatusEffectInstance effect : potionContentsComponent.getEffects()) {
               if (effect.getEffectType().value() == targetEffect) {
                  return i;
               }
            }
         }
      }

      return -1;
   }

   private Vec3d[] getBlockAllFaceCenters(BlockPos pos) {
      double bx = (double)pos.getX();
      double by = (double)pos.getY();
      double bz = (double)pos.getZ();
      return new Vec3d[]{
         new Vec3d(bx + 0.5, by, bz + 0.5),
         new Vec3d(bx + 0.5, by + 1.0, bz + 0.5),
         new Vec3d(bx, by + 0.5, bz + 0.5),
         new Vec3d(bx + 1.0, by + 0.5, bz + 0.5),
         new Vec3d(bx + 0.5, by + 0.5, bz),
         new Vec3d(bx + 0.5, by + 0.5, bz + 1.0)
      };
   }

   private float[] findBestSplashAngle() {
      if (!this.dynamic.getValue()) {
         return new float[]{Alien.ROTATION.rotationYaw, 90.0F};
      } else if (mc.world == null) {
         return new float[]{Alien.ROTATION.rotationYaw, 90.0F};
      } else {
         boolean canThrowUp = this.isAtThrowUpHeight();
         if (mc.player.isOnGround() && !canThrowUp) {
            return new float[]{Alien.ROTATION.rotationYaw, 90.0F};
         } else {
            Vec3d eyePos = mc.player.getEyePos();
            BlockPos eyeBlock = BlockPos.ofFloored(eyePos);
            double bestDistSq = Double.MAX_VALUE;
            Vec3d bestTarget = null;

            for (int x = -1; x <= 1; x++) {
               for (int y = -1; y <= 1; y++) {
                  for (int z = -1; z <= 1; z++) {
                     BlockPos check = eyeBlock.add(x, y, z);
                     if (!check.equals(eyeBlock) && !mc.world.isAir(check)) {
                        BlockState state = mc.world.getBlockState(check);
                        if (state.isFullCube(mc.world, check) && state.isSideSolidFullSquare(mc.world, check, Direction.UP)) {
                           Vec3d faceCenter = this.getNearestFace(eyePos, check);
                           double distSq = eyePos.squaredDistanceTo(faceCenter);
                           if (distSq < bestDistSq) {
                              bestDistSq = distSq;
                              bestTarget = faceCenter;
                           }
                        }
                     }
                  }
               }
            }

            return bestTarget != null ? this.calcAngle(eyePos, bestTarget) : new float[]{Alien.ROTATION.rotationYaw, canThrowUp ? -90.0F : 90.0F};
         }
      }
   }

   private Vec3d getNearestFace(Vec3d from, BlockPos block) {
      double cx = (double)block.getX() + 0.5;
      double cy = (double)block.getY() + 0.5;
      double cz = (double)block.getZ() + 0.5;
      Vec3d[] faces = new Vec3d[]{
         new Vec3d((double)block.getX(), cy, cz),
         new Vec3d((double)(block.getX() + 1), cy, cz),
         new Vec3d(cx, (double)block.getY(), cz),
         new Vec3d(cx, (double)(block.getY() + 1), cz),
         new Vec3d(cx, cy, (double)block.getZ()),
         new Vec3d(cx, cy, (double)(block.getZ() + 1))
      };
      Vec3d best = faces[0];
      double bestDist = from.squaredDistanceTo(best);

      for (int i = 1; i < faces.length; i++) {
         double dist = from.squaredDistanceTo(faces[i]);
         if (dist < bestDist) {
            bestDist = dist;
            best = faces[i];
         }
      }

      return best;
   }

   private boolean isAtThrowUpHeight() {
      int groundY = this.getGroundYBelowPlayer();
      return groundY != Integer.MIN_VALUE && mc.player.getY() >= (double)groundY + 8.0;
   }

   private int getGroundYBelowPlayer() {
      BlockPos playerPos = mc.player.getBlockPos();
      Mutable check = new Mutable();
      int x = playerPos.getX();
      int z = playerPos.getZ();

      for (int y = playerPos.getY() - 1; y >= mc.world.getBottomY(); y--) {
         check.set(x, y, z);
         if (!mc.world.isAir(check)) {
            BlockState state = mc.world.getBlockState(check);
            if (state.isFullCube(mc.world, check) && state.isSideSolidFullSquare(mc.world, check, Direction.UP)) {
               return y;
            }
         }
      }

      return Integer.MIN_VALUE;
   }

   private float[] calcAngle(Vec3d from, Vec3d to) {
      double dx = to.x - from.x;
      double dy = to.y - from.y;
      double dz = to.z - from.z;
      double h = Math.sqrt(dx * dx + dz * dz);
      double velocity = 1.5;
      double gravity = 0.05;
      double v2 = velocity * velocity;
      if (h < 0.1) {
         float yaw = (float)(MathHelper.atan2(dz, dx) * 180.0 / Math.PI) - 90.0F;
         float pitch = (float)(Math.atan2(-dy, 0.1) * 180.0 / Math.PI);
         return new float[]{yaw, pitch};
      } else {
         float yaw = (float)(MathHelper.atan2(dz, dx) * 180.0 / Math.PI) - 90.0F;
         double discriminant = v2 * v2 - gravity * (gravity * h * h + 2.0 * dy * v2);
         double pitchRad;
         if (discriminant >= 0.0) {
            double sqrtD = Math.sqrt(discriminant);
            double tanTheta = (v2 + sqrtD) / (gravity * h);
            pitchRad = Math.atan(tanTheta);
         } else {
            pitchRad = Math.atan2(-dy, h);
         }

         float pitch = -((float)Math.toDegrees(pitchRad));
         pitch = MathHelper.clamp(pitch, -90.0F, 85.0F);
         return new float[]{yaw, pitch};
      }
   }

   @Override
   public void onDisable() {
      this.throwing = false;
   }

   public void onUpdate(UpdateEvent event) {
      if (!this.inventory.getValue() || EntityUtil.inInventory()) {
         if (this.delayTimer.passedMs(this.delay.getValue() * 1000.0)) {
            if (!this.onlyGround.getValue()
               || (mc.player.isOnGround() || Alien.PLAYER.isInWeb(mc.player)) && !mc.world.isAir(new BlockPosX(mc.player.getPos().add(0.0, -1.0, 0.0)))) {
               if (this.resistance.getValue()
                  && (!mc.player.hasStatusEffect(StatusEffects.RESISTANCE) || mc.player.getStatusEffect(StatusEffects.RESISTANCE).getAmplifier() < 2)) {
                  this.throwing = this.checkThrow(StatusEffects.RESISTANCE.value());
                  if (this.isThrow()) {
                     this.throwPotion(StatusEffects.RESISTANCE.value());
                     return;
                  }
               }

               if (this.speed.getValue() && !mc.player.hasStatusEffect(StatusEffects.SPEED)) {
                  this.throwing = this.checkThrow(StatusEffects.SPEED.value());
                  if (this.isThrow()) {
                     this.throwPotion(StatusEffects.SPEED.value());
                     return;
                  }
               }

               if (this.strength.getValue() && !mc.player.hasStatusEffect(StatusEffects.STRENGTH)) {
                  this.throwing = this.checkThrow(StatusEffects.STRENGTH.value());
                  if (this.isThrow()) {
                     this.throwPotion(StatusEffects.STRENGTH.value());
                     return;
                  }
               }
            }
         }
      }
   }

   public void throwPotion(StatusEffect targetEffect) {
      int oldSlot = mc.player.getInventory().selectedSlot;
      int newSlot;
      if (this.inventory.getValue() && (newSlot = findPotionInventorySlot(targetEffect)) != -1) {
         float[] ang = this.findBestSplashAngle();
         Alien.ROTATION.snapAt(ang[0], ang[1]);
         Alien.INVENTORY.inventorySwap(newSlot, mc.player.getInventory().selectedSlot);
         sendSequencedPacket(id -> new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, id, Alien.ROTATION.getLastYaw(), Alien.ROTATION.getLastPitch()));
         Alien.INVENTORY.inventorySwap(newSlot, mc.player.getInventory().selectedSlot);
         EntityUtil.syncInventory();
         Alien.ROTATION.snapBack();
         this.delayTimer.reset();
      } else {
         newSlot = findPotion(targetEffect);
         if (newSlot != -1) {
            float[] ang = this.findBestSplashAngle();
            Alien.ROTATION.snapAt(ang[0], ang[1]);
            InventoryUtil.switchToSlot(newSlot);
            sendSequencedPacket(id -> new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, id, Alien.ROTATION.getLastYaw(), Alien.ROTATION.getLastPitch()));
            InventoryUtil.switchToSlot(oldSlot);
            Alien.ROTATION.snapBack();
            this.delayTimer.reset();
         }
      }
   }

   public boolean isThrow() {
      return this.throwing;
   }

   public boolean checkThrow(StatusEffect targetEffect) {
      if (!EntityUtil.inInventory()) {
         return false;
      } else {
         return this.usingPause.getValue() && mc.player.isUsingItem()
            ? false
            : findPotion(targetEffect) != -1 || this.inventory.getValue() && findPotionInventorySlot(targetEffect) != -1;
      }
   }

   public class AutoPotTick {
      public void onTick(ClientTickEvent event) {
         if (!Module.nullCheck() && !event.isPost()) {
            if (!AutoPotion.this.inventory.getValue() || EntityUtil.inInventory()) {
               if (Wrapper.mc.currentScreen == null) {
                  if (AutoPotion.this.resistanceKey.isPressed()) {
                     if (!AutoPotion.this.turtlePress && AutoPotion.this.checkThrow(StatusEffects.RESISTANCE.value())) {
                        AutoPotion.this.throwPotion(StatusEffects.RESISTANCE.value());
                        AutoPotion.this.turtlePress = true;
                        return;
                     }
                  } else {
                     AutoPotion.this.turtlePress = false;
                  }

                  if (AutoPotion.this.strengthKey.isPressed()) {
                     if (!AutoPotion.this.strengthPress && AutoPotion.this.checkThrow(StatusEffects.STRENGTH.value())) {
                        AutoPotion.this.throwPotion(StatusEffects.STRENGTH.value());
                        AutoPotion.this.strengthPress = true;
                        return;
                     }
                  } else {
                     AutoPotion.this.strengthPress = false;
                  }

                  if (AutoPotion.this.speedKey.isPressed()) {
                     if (!AutoPotion.this.speedPress && AutoPotion.this.checkThrow(StatusEffects.SPEED.value())) {
                        AutoPotion.this.throwPotion(StatusEffects.SPEED.value());
                        AutoPotion.this.speedPress = true;
                     }
                  } else {
                     AutoPotion.this.speedPress = false;
                  }
               } else {
                  AutoPotion.this.speedPress = false;
                  AutoPotion.this.turtlePress = false;
                  AutoPotion.this.strengthPress = false;
               }
            }
         }
      }
   }
}
