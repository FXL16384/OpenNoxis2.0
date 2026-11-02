package noxisclient.mod.modules.impl.movement;

import net.minecraft.entity.projectile.FishingBobberEntity;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket.Action;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.Full;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Direction.AxisDirection;
import noxisclient.Alien;
import noxisclient.api.events.impl.EntityVelocityUpdateEvent;
import noxisclient.api.events.impl.PacketEvent;
import noxisclient.api.events.impl.TickEvent;
import noxisclient.api.events.impl.UpdateRotateEvent;
import noxisclient.api.utils.math.Timer;
import noxisclient.api.utils.player.EntityUtil;
import noxisclient.api.utils.player.MovementUtil;
import noxisclient.asm.accessors.IEntityVelocityUpdateS2CPacket;
import noxisclient.mod.modules.Module;
import noxisclient.mod.modules.impl.exploit.Blink;
import noxisclient.mod.modules.settings.impl.BooleanSetting;
import noxisclient.mod.modules.settings.impl.EnumSetting;
import noxisclient.mod.modules.settings.impl.SliderSetting;

public class Velocity extends Module {
   public static Velocity INSTANCE;
   private final EnumSetting<Velocity.Mode> mode = this.add(new EnumSetting<>("Mode", Velocity.Mode.Plain));
   private final SliderSetting lagPause = this.add(
      new SliderSetting("LagPause", 50, 0, 500, () -> this.mode.is(Velocity.Mode.Grim) || this.mode.is(Velocity.Mode.Wall))
   );
   public final BooleanSetting ignorePearlLag = this.add(
      new BooleanSetting("IgnorePearlLag", true, () -> this.mode.is(Velocity.Mode.Grim) || this.mode.is(Velocity.Mode.Wall)).setParent()
   );
   private final SliderSetting phaseTime = this.add(
      new SliderSetting("PhaseTime", 250, 0, 1000, () -> (this.mode.is(Velocity.Mode.Grim) || this.mode.is(Velocity.Mode.Wall)) && this.ignorePearlLag.isOpen())
   );
   public final BooleanSetting noRotation = this.add(
      new BooleanSetting("NoRotation", false, () -> this.mode.is(Velocity.Mode.Grim) || this.mode.is(Velocity.Mode.Wall))
   );
   public final BooleanSetting flagInWall = this.add(
      new BooleanSetting("FlagInWall", false, () -> this.mode.is(Velocity.Mode.Grim) || this.mode.is(Velocity.Mode.Wall)).setParent()
   );
   public final BooleanSetting whenPushOutOfBlocks = this.add(
      new BooleanSetting("WhilePushOut", false, () -> (this.mode.is(Velocity.Mode.Grim) || this.mode.is(Velocity.Mode.Wall)) && this.flagInWall.isOpen())
   );
   public final BooleanSetting staticSetting = this.add(new BooleanSetting("Static", false, () -> this.mode.is(Velocity.Mode.Grim)));
   public final BooleanSetting clientSide = this.add(new BooleanSetting("ClientSide", false, () -> this.mode.is(Velocity.Mode.Grim)));
   public final BooleanSetting cancelAll = this.add(new BooleanSetting("CancelAll", false, () -> !this.mode.is(Velocity.Mode.None)));
   private final SliderSetting horizontal = this.add(
      new SliderSetting("Horizontal", 0.0, 0.0, 100.0, 1.0, () -> !this.mode.is(Velocity.Mode.None) && !this.cancelAll.getValue())
   );
   private final SliderSetting vertical = this.add(
      new SliderSetting("Vertical", 0.0, 0.0, 100.0, 1.0, () -> !this.mode.is(Velocity.Mode.None) && !this.cancelAll.getValue())
   );
   public final BooleanSetting whileLiquid = this.add(new BooleanSetting("WhileLiquid", false));
   public final BooleanSetting whileElytra = this.add(new BooleanSetting("FallFlying", false));
   public final BooleanSetting noClimb = this.add(new BooleanSetting("NoClimb", false));
   public final BooleanSetting waterPush = this.add(new BooleanSetting("NoWaterPush", false));
   public final BooleanSetting entityPush = this.add(new BooleanSetting("NoEntityPush", true));
   public final BooleanSetting blockPush = this.add(new BooleanSetting("NoBlockPush", true));
   public final BooleanSetting fishBob = this.add(new BooleanSetting("NoFishBob", true));
   public final Timer pearlTimer = new Timer();
   private final Timer lagBackTimer = new Timer();
   private boolean flag;
   static boolean pushOutOfBlocks = false;

   public Velocity() {
      super("Velocity", Module.Category.Movement);
      this.setChinese("反击退");
      INSTANCE = this;
   }

   @Override
   public String getInfo() {
      return this.mode.is(Velocity.Mode.None)
         ? null
         : this.mode.getValue().name()
            + ", "
            + (this.cancelAll.getValue() ? "Cancel" : this.horizontal.getValueInt() + "%, " + this.vertical.getValueInt() + "%");
   }

   public void onRotate(UpdateRotateEvent event) {
      if (this.noRotation()) {
         event.setRotation(Alien.ROTATION.rotationYaw, 89.0F);
      }
   }

   public boolean noRotation() {
      return this.isOn() && (this.mode.is(Velocity.Mode.Grim) || this.mode.is(Velocity.Mode.Wall)) && EntityUtil.isInsideBlock() && this.noRotation.getValue();
   }

   public void onVelocity(EntityVelocityUpdateEvent event) {
      if (!nullCheck()) {
         if (event.getEntity() == mc.player) {
            if (!this.mode.is(Velocity.Mode.None)) {
               if (!mc.player.isInFluid() || this.whileLiquid.getValue()) {
                  if (!mc.player.isFallFlying() || this.whileElytra.getValue()) {
                     switch ((Velocity.Mode)this.mode.getValue()) {
                        case Grim:
                           if (!this.lagBackTimer.passedMs(this.lagPause.getValue())) {
                              return;
                           }

                           if (!EntityUtil.isInsideBlock() && this.getPos() == null && (!this.staticSetting.getValue() || !MovementUtil.isStatic())) {
                              return;
                           }

                           if (event.getX() != 0.0 || event.getZ() != 0.0) {
                              this.flag = true;
                           }
                           break;
                        case Wall:
                           if (!this.lagBackTimer.passedMs(this.lagPause.getValue())) {
                              return;
                           }

                           if (!EntityUtil.isInsideBlock()) {
                              return;
                           }

                           if (event.getX() != 0.0 || event.getZ() != 0.0) {
                              this.flag = true;
                           }
                     }

                     if (this.cancelAll.getValue()) {
                        event.cancel();
                     } else {
                        double h = (double)this.horizontal.getValueInt() / 100.0;
                        double v = (double)this.vertical.getValueInt() / 100.0;
                        event.setX(event.getX() * h);
                        event.setZ(event.getZ() * h);
                        event.setY(event.getY() * v);
                     }
                  }
               }
            }
         }
      }
   }

   public void onReceivePacket(PacketEvent.Receive event) {
      if (event.getPacket() instanceof PlayerPositionLookS2CPacket
         && (!this.ignorePearlLag.getValue() || this.pearlTimer.passed((long)this.phaseTime.getValueInt()))) {
         this.lagBackTimer.reset();
      }

      if (!nullCheck()) {
         if (!mc.player.isInFluid() || this.whileLiquid.getValue()) {
            if (this.mode.is(Velocity.Mode.Grim) && this.clientSide.getValue() && event.getPacket() instanceof EntityVelocityUpdateS2CPacket) {
               EntityVelocityUpdateS2CPacket velocityPacket = (EntityVelocityUpdateS2CPacket)event.getPacket();
               if (velocityPacket.getEntityId() == mc.player.getId()) {
                  IEntityVelocityUpdateS2CPacket accessor = (IEntityVelocityUpdateS2CPacket)velocityPacket;
                  accessor.setX(0);
                  accessor.setY(0);
                  accessor.setZ(0);
               }
            }

            if (this.fishBob.getValue() && event.getPacket() instanceof EntityStatusS2CPacket) {
               EntityStatusS2CPacket packet = (EntityStatusS2CPacket)event.getPacket();
               if (packet.getStatus() == 31 && packet.getEntity(mc.world) instanceof FishingBobberEntity fishHook && fishHook.getHookedEntity() == mc.player) {
                  event.setCancelled(true);
               }
            }
         }
      }
   }

   public void onUpdate(TickEvent event) {
      if (!nullCheck()) {
         if (!event.isPost() && (!mc.player.isInFluid() || this.whileLiquid.getValue())) {
            if (this.flagInWall.getValue()) {
               pushOutOfBlocks = false;
               pushOutOfBlocks(mc.player.getX() - (double)mc.player.getWidth() * 0.35, mc.player.getZ() + (double)mc.player.getWidth() * 0.35);
               pushOutOfBlocks(mc.player.getX() - (double)mc.player.getWidth() * 0.35, mc.player.getZ() - (double)mc.player.getWidth() * 0.35);
               pushOutOfBlocks(mc.player.getX() + (double)mc.player.getWidth() * 0.35, mc.player.getZ() - (double)mc.player.getWidth() * 0.35);
               pushOutOfBlocks(mc.player.getX() + (double)mc.player.getWidth() * 0.35, mc.player.getZ() + (double)mc.player.getWidth() * 0.35);
            }

            if (!Blink.INSTANCE.isOn() || !Blink.INSTANCE.pauseModule.getValue()) {
               if (this.flag) {
                  if (this.lagBackTimer.passedMs(this.lagPause.getValue())
                     && (this.flagInWall.getValue() && (!pushOutOfBlocks || this.whenPushOutOfBlocks.getValue()) || !EntityUtil.isInsideBlock())) {
                     mc.getNetworkHandler()
                        .sendPacket(
                           new Full(
                              mc.player.getX(),
                              mc.player.getY(),
                              mc.player.getZ(),
                              Alien.ROTATION.rotationYaw,
                              Alien.ROTATION.rotationPitch,
                              mc.player.isOnGround()
                           )
                        );
                     BlockPos pos = this.getPos();
                     if (pos != null) {
                        mc.getNetworkHandler()
                           .sendPacket(new PlayerActionC2SPacket(Action.STOP_DESTROY_BLOCK, pos, mc.player.getHorizontalFacing().getOpposite()));
                     }
                  }

                  this.flag = false;
               }
            }
         }
      }
   }

   public BlockPos getPos() {
      return null;
   }

   private static void pushOutOfBlocks(double x, double z) {
      BlockPos blockPos = BlockPos.ofFloored(x, mc.player.getY(), z);
      if (wouldCollideAt(blockPos)) {
         double d = x - (double)blockPos.getX();
         double e = z - (double)blockPos.getZ();
         Direction direction = null;
         double f = Double.MAX_VALUE;

         for (Direction direction2 : new Direction[]{Direction.WEST, Direction.EAST, Direction.NORTH, Direction.SOUTH}) {
            double g = direction2.getAxis().choose(d, 0.0, e);
            double h = direction2.getDirection() == AxisDirection.POSITIVE ? 1.0 - g : g;
            if (h < f && !wouldCollideAt(blockPos.offset(direction2))) {
               f = h;
               direction = direction2;
            }
         }

         if (direction != null) {
            pushOutOfBlocks = true;
         }
      }
   }

   private static boolean wouldCollideAt(BlockPos pos) {
      Box box = mc.player.getBoundingBox();
      Box box2 = new Box((double)pos.getX(), box.minY, (double)pos.getZ(), (double)pos.getX() + 1.0, box.maxY, (double)pos.getZ() + 1.0).contract(1.0E-7);
      return mc.player.getWorld().canCollide(mc.player, box2);
   }

   public static enum Mode {
      Plain,
      Grim,
      Wall,
      None;
   }
}
