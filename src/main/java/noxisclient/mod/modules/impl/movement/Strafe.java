package noxisclient.mod.modules.impl.movement;

import java.util.Objects;
import net.minecraft.entity.effect.StatusEffects;
import noxisclient.api.events.impl.MoveEvent;
import noxisclient.api.utils.path.BaritoneUtil;
import noxisclient.api.utils.player.EntityUtil;
import noxisclient.api.utils.player.MovementUtil;
import noxisclient.mod.modules.Module;
import noxisclient.mod.modules.settings.impl.BooleanSetting;

public class Strafe extends Module {
   public static Strafe INSTANCE;
   private final BooleanSetting airStop = this.add(new BooleanSetting("AirStop", true));
   private final BooleanSetting slowCheck = this.add(new BooleanSetting("SlowCheck", true));

   public Strafe() {
      super("Strafe", "Modifies sprinting", Module.Category.Movement);
      this.setChinese("灵活移动");
      INSTANCE = this;
   }

   public void onStrafe(MoveEvent event) {
      if (!BaritoneUtil.isActive()) {
         if (!mc.player.isSneaking()
            && !Fly.INSTANCE.isOn()
            && !HoleSnap.INSTANCE.isOn()
            && !Speed.INSTANCE.isOn()
            && !mc.player.isFallFlying()
            && !EntityUtil.isInsideBlock()
            && !mc.player.isInLava()
            && !mc.player.isTouchingWater()
            && !mc.player.getAbilities().flying) {
            if (!MovementUtil.isMoving()) {
               if (this.airStop.getValue()) {
                  MovementUtil.setMotionX(0.0);
                  MovementUtil.setMotionZ(0.0);
               }
            } else {
               double[] dir = MovementUtil.directionSpeed(this.getBaseMoveSpeed());
               event.setX(dir[0]);
               event.setZ(dir[1]);
            }
         }
      }
   }

   public double getBaseMoveSpeed() {
      double n = 0.2873;
      if (mc.player.hasStatusEffect(StatusEffects.SPEED) && (!this.slowCheck.getValue() || !mc.player.hasStatusEffect(StatusEffects.SLOWNESS))) {
         n *= 1.0 + 0.2 * (double)(Objects.requireNonNull(mc.player.getStatusEffect(StatusEffects.SPEED)).getAmplifier() + 1);
      }

      return n;
   }
}
