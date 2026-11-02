package noxisclient.mod.modules.impl.player;

import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.Full;
import net.minecraft.util.math.Vec3d;
import noxisclient.api.events.impl.PacketEvent;
import noxisclient.api.events.impl.UpdateEvent;
import noxisclient.asm.accessors.IPlayerMoveC2SPacket;
import noxisclient.mod.modules.Module;
import noxisclient.mod.modules.impl.exploit.BowBomb;
import noxisclient.mod.modules.settings.impl.EnumSetting;
import noxisclient.mod.modules.settings.impl.SliderSetting;

public class NoFall extends Module {
   private final EnumSetting<NoFallMode> mode = this.add(new EnumSetting<>("Mode", NoFallMode.Packet));
   private final EnumSetting<NoFall.FixMode> fixMode = this.add(new EnumSetting<>("FixMode", NoFall.FixMode.Retry, () -> this.mode.is(NoFallMode.GrimNew)));
   private final SliderSetting getDistance = this.add(new SliderSetting("Distance", 3.0, 0.0, 8.0, 0.1));
   private boolean fell = false;
   private boolean sendingGrimNew = false;
   private int jumpTicks = 0;
   private int interceptTicks = 0;
   private int inputTicks = 0;

   public NoFall() {
      super("NoFall", "Prevents fall damage.", Module.Category.Player);
      this.setChinese("没有摔落伤害");
   }

   @Override
   public void onDisable() {
      this.fell = false;
      this.sendingGrimNew = false;
      this.jumpTicks = 0;
      this.interceptTicks = 0;
      if (this.inputTicks > 0 && !nullCheck() && !mc.options.jumpKey.isPressed()) {
         mc.player.input.jumping = false;
      }

      this.inputTicks = 0;
      super.onDisable();
   }

   @Override
   public String getInfo() {
      return this.mode.getValue().name();
   }

   public void onUpdate(UpdateEvent event) {
      if (!nullCheck()) {
         if (this.mode.is(NoFallMode.Grim) && this.checkFalling()) {
            mc.getNetworkHandler()
               .sendPacket(new Full(mc.player.getX(), mc.player.getY() + 1.0E-9, mc.player.getZ(), mc.player.getYaw(), mc.player.getPitch(), false));
            mc.player.onLanding();
         }

         if (this.mode.is(NoFallMode.GrimNew)) {
            if (mc.player.fallDistance >= (float)this.getDistance.getValue() && !mc.player.isOnGround()) {
               this.fell = true;
            }

            this.updateInputJump();
            if (this.fell && this.isFix(NoFall.FixMode.NearGround) && this.jumpTicks <= 0 && this.isGroundedEnough()) {
               this.interceptTicks = 3;
            }

            if (this.jumpTicks > 0) {
               if (!mc.player.isOnGround() && !this.isGroundedEnough()) {
                  this.jumpTicks--;
               } else {
                  this.doJump();
                  this.jumpTicks = 0;
               }
            }

            if (this.interceptTicks > 0) {
               this.interceptTicks--;
            }
         } else {
            this.jumpTicks = 0;
            this.interceptTicks = 0;
            if (this.inputTicks > 0 && !mc.options.jumpKey.isPressed()) {
               mc.player.input.jumping = false;
            }

            this.inputTicks = 0;
         }
      }
   }

   private boolean checkFalling() {
      return mc.player.fallDistance > (float)mc.player.getSafeFallDistance() && !mc.player.isOnGround() && !mc.player.isFallFlying();
   }

   private boolean isGroundedEnough() {
      return mc.world != null
         && mc.player.getVelocity().getY() <= 0.0
         && mc.world.getBlockCollisions(mc.player, mc.player.getBoundingBox().offset(0.0, -0.05, 0.0)).iterator().hasNext();
   }

   private void doJump() {
      Vec3d velocity = mc.player.getVelocity();
      if (this.isFix(NoFall.FixMode.Input)) {
         this.forceInputJump();
      }

      mc.player.jump();
      mc.player.setVelocity(velocity.x, Math.max(mc.player.getVelocity().getY(), 0.42), velocity.z);
   }

   private void armJump() {
      this.jumpTicks = 5;
   }

   private void forceInputJump() {
      mc.player.input.jumping = true;
      this.inputTicks = 2;
   }

   private void updateInputJump() {
      if (this.inputTicks > 0) {
         mc.player.input.jumping = true;
         this.inputTicks--;
         if (this.inputTicks == 0 && !mc.options.jumpKey.isPressed()) {
            mc.player.input.jumping = false;
         }
      }
   }

   private void sendGrimNewFull(double x, double y, double z, float yaw, float pitch) {
      this.sendingGrimNew = true;

      try {
         mc.getNetworkHandler().sendPacket(new Full(x, y, z, yaw, pitch, false));
      } finally {
         this.sendingGrimNew = false;
      }
   }

   private boolean shouldReplaceLanding(PlayerMoveC2SPacket packet) {
      return this.fell && (packet.isOnGround() || this.interceptTicks > 0 || mc.player.isOnGround() || this.isGroundedEnough());
   }

   private boolean isFix(NoFall.FixMode mode) {
      return this.fixMode.is(mode);
   }

   public void onPacketSend(PacketEvent.Send event) {
      if (!nullCheck()) {
         for (ItemStack is : mc.player.getArmorItems()) {
            if (is.getItem() == Items.ELYTRA) {
               return;
            }
         }

         if (this.mode.is(NoFallMode.Packet) || this.mode.is(NoFallMode.GrimNew)) {
            if (event.getPacket() instanceof PlayerMoveC2SPacket packet2) {
               if (this.mode.is(NoFallMode.GrimNew)) {
                  if (this.sendingGrimNew) {
                     return;
                  }

                  if (this.shouldReplaceLanding(packet2)) {
                     if (packet2 instanceof Full) {
                        IPlayerMoveC2SPacket accessor = (IPlayerMoveC2SPacket)packet2;
                        accessor.setY(packet2.getY(mc.player.getY()) + 10.0);
                        accessor.setOnGround(false);
                     } else {
                        event.cancel();
                        this.sendGrimNewFull(
                           packet2.getX(mc.player.getX()),
                           packet2.getY(mc.player.getY()) + 10.0,
                           packet2.getZ(mc.player.getZ()),
                           packet2.getYaw(mc.player.getYaw()),
                           packet2.getPitch(mc.player.getPitch())
                        );
                     }

                     this.armJump();
                     this.fell = false;
                     this.interceptTicks = 0;
                  }

                  return;
               }

               if (mc.player.fallDistance >= (float)this.getDistance.getValue() && !BowBomb.send) {
                  ((IPlayerMoveC2SPacket)packet2).setOnGround(true);
               }
            }
         }
      }
   }

   public static enum FixMode {
      Retry,
      NearGround,
      Input;
   }
}
