package noxisclient.asm.mixins;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.projectile.FireworkRocketEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Vec3d;
import noxisclient.Alien;
import noxisclient.api.events.impl.FireworkShooterRotationEvent;
import noxisclient.api.events.impl.RemoveFireworkEvent;
import noxisclient.api.utils.Wrapper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({FireworkRocketEntity.class})
public class MixinFireworkRocketEntity implements Wrapper {
   @Shadow
   private int field_7613;

   @Inject(
      method = {"tick"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/entity/projectile/FireworkRocketEntity;updateRotation()V",
         shift = Shift.AFTER
      )},
      cancellable = true
   )
   private void hookTickPre(CallbackInfo ci) {
      FireworkRocketEntity rocketEntity = FireworkRocketEntity.class.cast(this);
      RemoveFireworkEvent removeFireworkEvent = RemoveFireworkEvent.get(rocketEntity);
      Alien.EVENT_BUS.post(removeFireworkEvent);
      if (removeFireworkEvent.isCancelled()) {
         ci.cancel();
         if (this.field_7613 == 0 && !rocketEntity.isSilent()) {
            mc.world
               .playSound(
                  null,
                  rocketEntity.getX(),
                  rocketEntity.getY(),
                  rocketEntity.getZ(),
                  SoundEvents.ENTITY_FIREWORK_ROCKET_LAUNCH,
                  SoundCategory.AMBIENT,
                  3.0F,
                  1.0F
               );
         }

         this.field_7613++;
         if (mc.world.isClient) {
            mc.world
               .addParticle(
                  ParticleTypes.FIREWORK,
                  rocketEntity.getX(),
                  rocketEntity.getY(),
                  rocketEntity.getZ(),
                  mc.world.random.nextGaussian() * 0.05,
                  -rocketEntity.getVelocity().y * 0.5,
                  mc.world.random.nextGaussian() * 0.05
               );
         }
      }
   }

   @Redirect(
      method = {"tick"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/entity/LivingEntity;getRotationVector()Lnet/minecraft/util/math/Vec3d;"
      ),
      require = 0
   )
   public Vec3d hook(LivingEntity instance) {
      FireworkShooterRotationEvent event = FireworkShooterRotationEvent.get(instance, instance.getYaw(), instance.getPitch());
      Alien.EVENT_BUS.post(event);
      return event.isCancelled() ? event.getRotationVector() : instance.getRotationVector();
   }
}
