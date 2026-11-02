package noxisclient.mod.modules.impl.player;

import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.FireworkRocketEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import noxisclient.Alien;
import noxisclient.api.events.impl.ClientTickEvent;
import noxisclient.asm.accessors.IFireworkRocketEntity;
import noxisclient.mod.modules.Module;
import noxisclient.mod.modules.impl.movement.ElytraFly;
import noxisclient.mod.modules.impl.movement.TargetFollow;
import noxisclient.mod.modules.settings.impl.BooleanSetting;
import noxisclient.mod.modules.settings.impl.SliderSetting;

public class FireworkBoost extends Module {
   private final SliderSetting speedConfig = this.add(new SliderSetting("Speed", 1.5, 1.0, 10.0, 0.01));
   private final BooleanSetting accelerateConfig = this.add(new BooleanSetting("Accelerate", false).setParent());
   private final SliderSetting accelSpeedConfig = this.add(new SliderSetting("AccelSpeed", 0.5, 0.1, 5.0, 0.1, this.accelerateConfig::isOpen));
   private final SliderSetting maxSpeedConfig = this.add(new SliderSetting("MaxSpeed", 3.0, 1.0, 10.0, 0.1, this.accelerateConfig::isOpen));
   private int tickCounter;
   private double speed;

   public FireworkBoost() {
      super("FireworkBoost", Module.Category.Player);
      this.setChinese("烟花加速");
   }

   @Override
   public void onDisable() {
      this.speed = 0.0;
      this.tickCounter = 0;
   }

   public void onTick(ClientTickEvent event) {
      if (!event.isPost() && !nullCheck() && !mc.player.isInLava()) {
         if (this.isBoostedByRocket() && this.isFallFlying()) {
            Vec3d rotation = this.getBoostRotationVector();
            Vec3d velocity = mc.player.getVelocity();
            if (this.accelerateConfig.getValue()) {
               this.speed = this.speed + this.accelSpeedConfig.getValue() * (double)this.tickCounter;
               if (this.speed < this.maxSpeedConfig.getValue()) {
                  this.tickCounter++;
               } else {
                  this.speed = MathHelper.clamp(this.speed, 0.0, this.maxSpeedConfig.getValue());
               }
            } else {
               this.speed = this.speedConfig.getValue();
               this.tickCounter = 0;
            }

            mc.player
               .setVelocity(
                  velocity.add(
                     rotation.x * 0.1 + (rotation.x * this.speed - velocity.x) * 0.5,
                     rotation.y * 0.1 + (rotation.y * this.speed - velocity.y) * 0.5,
                     rotation.z * 0.1 + (rotation.z * this.speed - velocity.z) * 0.5
                  )
               );
         } else {
            this.speed = 0.0;
            this.tickCounter = 0;
         }
      }
   }

   private boolean isFallFlying() {
      return ElytraFly.INSTANCE != null && ElytraFly.INSTANCE.isOn() && ElytraFly.INSTANCE.mode.is(ElytraFly.Mode.Rotation)
         ? ElytraFly.INSTANCE.isFallFlying()
         : mc.player.isFallFlying();
   }

   private Vec3d getBoostRotationVector() {
      if (TargetFollow.INSTANCE != null && TargetFollow.INSTANCE.shouldControlFireworkBoost()) {
         return TargetFollow.INSTANCE.getFireworkBoostVector();
      } else {
         return ElytraFly.INSTANCE != null && ElytraFly.INSTANCE.isOn() && ElytraFly.INSTANCE.mode.is(ElytraFly.Mode.Rotation)
            ? ElytraFly.INSTANCE.getFireworkBoostVector()
            : mc.player.getRotationVector();
      }
   }

   private boolean isBoostedByRocket() {
      for (Entity entity : Alien.THREAD.getEntities()) {
         if (entity instanceof FireworkRocketEntity rocket
            && ((IFireworkRocketEntity)rocket).hookWasShotByEntity()
            && ((IFireworkRocketEntity)rocket).getShooter() == mc.player) {
            return true;
         }
      }

      return false;
   }

   @Override
   public String getInfo() {
      return this.accelerateConfig.getValue() ? "Accel" : String.format("%.1f", this.speedConfig.getValue());
   }
}
