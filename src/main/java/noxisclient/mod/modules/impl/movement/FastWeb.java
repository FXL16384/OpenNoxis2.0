package noxisclient.mod.modules.impl.movement;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.block.CobwebBlock;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket.Action;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import noxisclient.Alien;
import noxisclient.api.events.impl.TimerEvent;
import noxisclient.api.events.impl.UpdateEvent;
import noxisclient.api.utils.Wrapper;
import noxisclient.api.utils.player.MovementUtil;
import noxisclient.mod.modules.Module;
import noxisclient.mod.modules.settings.impl.BooleanSetting;
import noxisclient.mod.modules.settings.impl.EnumSetting;
import noxisclient.mod.modules.settings.impl.SliderSetting;

public class FastWeb extends Module implements Wrapper {
   public static FastWeb INSTANCE;
   public final EnumSetting<FastWeb.Mode> mode = this.add(new EnumSetting<>("Mode", FastWeb.Mode.Vanilla));
   public final BooleanSetting onlySneak = this.add(new BooleanSetting("OnlySneak", true));
   private final SliderSetting fastSpeed = this.add(
      new SliderSetting("Speed", 3.0, 0.0, 8.0, () -> this.mode.getValue() == FastWeb.Mode.Vanilla || this.mode.getValue() == FastWeb.Mode.Strict)
   );
   public final SliderSetting xZSlow = this.add(
      new SliderSetting("XZSpeed", 25.0, 0.0, 100.0, 0.1, () -> this.mode.getValue() == FastWeb.Mode.Custom).setSuffix("%")
   );
   public final SliderSetting ySlow = this.add(
      new SliderSetting("YSpeed", 100.0, 0.0, 100.0, 0.1, () -> this.mode.getValue() == FastWeb.Mode.Custom).setSuffix("%")
   );
   public final BooleanSetting grimKeepY = this.add(new BooleanSetting("GrimKeepY", true, () -> this.mode.getValue() == FastWeb.Mode.Grim));
   public final BooleanSetting grimMineWhenJump = this.add(new BooleanSetting("GrimMineWhenJump", false, () -> this.mode.getValue() == FastWeb.Mode.Grim));
   public final BooleanSetting grimStrafe = this.add(new BooleanSetting("GrimStrafe", true, () -> this.mode.getValue() == FastWeb.Mode.Grim));
   public final SliderSetting grimStrafeSpeed = this.add(
      new SliderSetting("GrimStrafeSpeed", 0.64, 0.0, 1.0, 0.01, () -> this.mode.getValue() == FastWeb.Mode.Grim && this.grimStrafe.getValue())
   );
   public final BooleanSetting grimLegacy = this.add(new BooleanSetting("GrimLegacy", false, () -> this.mode.getValue() == FastWeb.Mode.Grim));
   public final BooleanSetting abortPacket = this.add(
      new BooleanSetting("AbortPacket", true, () -> this.mode.getValue() == FastWeb.Mode.Grim && this.grimLegacy.getValue())
   );
   private boolean work = false;

   public FastWeb() {
      super("FastWeb", "So you don't need to keep timer on keybind", Module.Category.Movement);
      this.setChinese("蜘蛛网加");
      INSTANCE = this;
   }

   @Override
   public String getInfo() {
      return this.mode.getValue().name();
   }

   public boolean isPlayerInWeb() {
      return Alien.PLAYER.isInWeb(mc.player);
   }

   public boolean isSneakCondition() {
      return mc.options.sneakKey.isPressed() || !this.onlySneak.getValue();
   }

   public boolean isOnGroundOrGoingUp() {
      return mc.player.isOnGround() || mc.player.getVelocity().y >= 0.0;
   }

   public void onUpdate(UpdateEvent event) {
      this.work = !mc.player.isOnGround() && this.isSneakCondition() && this.isPlayerInWeb();
      if (this.work && this.mode.is(FastWeb.Mode.Vanilla)) {
         MovementUtil.setMotionY(-this.fastSpeed.getValue());
      }

      if (this.mode.is(FastWeb.Mode.Grim) && this.isSneakCondition() && this.isPlayerInWeb()) {
         this.handleGrimUpdate();
      }
   }

   private void handleGrimUpdate() {
      if (this.grimLegacy.getValue()) {
         for (BlockPos pos : this.getIntersectingWebs()) {
            if (this.abortPacket.getValue()) {
               mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(Action.ABORT_DESTROY_BLOCK, pos, Direction.DOWN));
            }

            mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(Action.STOP_DESTROY_BLOCK, pos, Direction.DOWN));
         }
      }

      if (this.grimMineWhenJump.getValue() && !mc.player.isFallFlying() && this.isOnGroundOrGoingUp() && mc.options.jumpKey.isPressed()) {
         for (BlockPos pos : this.getIntersectingWebs()) {
            mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(Action.START_DESTROY_BLOCK, pos, Direction.UP));
            mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(Action.STOP_DESTROY_BLOCK, pos, Direction.UP));
         }
      }

      if (this.grimStrafe.getValue() && !mc.player.isFallFlying() && MovementUtil.isMoving()) {
         double speed = this.grimStrafeSpeed.getValue();
         double[] dir = MovementUtil.directionSpeed(speed);
         mc.player.setVelocity(dir[0], mc.player.getVelocity().y, dir[1]);
      }
   }

   public void onTimer(TimerEvent event) {
      if (this.work && this.mode.getValue() == FastWeb.Mode.Strict) {
         event.set(this.fastSpeed.getValueFloat());
      }
   }

   public Vec3d getWebSlowdownModifier(BlockPos pos, Vec3d originalMultiplier) {
      switch ((FastWeb.Mode)this.mode.getValue()) {
         case Custom:
            return new Vec3d(this.xZSlow.getValue() / 100.0, this.ySlow.getValue() / 100.0, this.xZSlow.getValue() / 100.0);
         case Ignore:
            return null;
         case Grim:
            if (this.grimKeepY.getValue()) {
               return new Vec3d(originalMultiplier.x, 1.0, originalMultiplier.z);
            } else if (this.grimMineWhenJump.getValue() && !mc.player.isFallFlying() && this.isOnGroundOrGoingUp() && mc.options.jumpKey.isPressed()) {
               return null;
            } else if (mc.player.isFallFlying()) {
               return originalMultiplier;
            } else {
               if (this.grimStrafe.getValue() && MovementUtil.isMoving()) {
                  return null;
               }

               return originalMultiplier;
            }
         default:
            return originalMultiplier;
      }
   }

   public List<BlockPos> getIntersectingWebs() {
      int radius = 2;
      ArrayList<BlockPos> blocks = new ArrayList<>();

      for (int x = radius; x > -radius; x--) {
         for (int y = radius; y > -radius; y--) {
            for (int z = radius; z > -radius; z--) {
               BlockPos blockPos = BlockPos.ofFloored(mc.player.getX() + (double)x, mc.player.getY() + (double)y, mc.player.getZ() + (double)z);
               if ((!(mc.player.getPos().distanceTo(blockPos.toCenterPos()) > 1.0) || !(mc.player.getEyePos().distanceTo(blockPos.toCenterPos()) > 1.0))
                  && mc.world.getBlockState(blockPos).getBlock() instanceof CobwebBlock) {
                  blocks.add(blockPos);
               }
            }
         }
      }

      return blocks;
   }

   public static enum Mode {
      Vanilla,
      Strict,
      Custom,
      Ignore,
      Grim;
   }
}
