package noxisclient.asm.mixins;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.Entity.RemovalReason;
import net.minecraft.entity.attribute.AttributeContainer;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.world.World;
import noxisclient.Alien;
import noxisclient.api.events.impl.LerpToEvent;
import noxisclient.api.events.impl.SprintEvent;
import noxisclient.mod.modules.impl.movement.ElytraFly;
import noxisclient.mod.modules.impl.movement.Velocity;
import noxisclient.mod.modules.impl.player.AntiEffects;
import noxisclient.mod.modules.impl.render.AnimationsModule;
import noxisclient.mod.modules.impl.render.SpearFix;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({LivingEntity.class})
public abstract class MixinLivingEntity extends Entity {
   @Final
   @Shadow
   private static EntityAttributeModifier field_6231;
   @Unique
   private boolean previousElytra = false;
   @Unique
   private long lastLerp = 0L;

   public MixinLivingEntity(EntityType<?> type, World world) {
      super(type, world);
   }

   @Shadow
   @Nullable
   public EntityAttributeInstance method_5996(RegistryEntry<EntityAttribute> attribute) {
      return this.method_6127().getCustomInstance(attribute);
   }

   @Shadow
   public AttributeContainer method_6127() {
      return null;
   }

   @Shadow
   @Override
   public abstract void remove(RemovalReason var1);

   @Shadow
   public abstract boolean method_6128();

   @Inject(
      method = {"getHandSwingDuration"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void getArmSwingAnimationEnd(CallbackInfoReturnable<Integer> info) {
      if (SpearFix.INSTANCE != null) {
         int spearSwing = SpearFix.INSTANCE.getSwingTicks(((LivingEntity)(Object)this));
         if (spearSwing > 0) {
            info.setReturnValue(spearSwing);
            return;
         }
      }

      if (AnimationsModule.INSTANCE != null && AnimationsModule.INSTANCE.shouldModifySwingSpeed(((LivingEntity)(Object)this))) {
         info.setReturnValue(AnimationsModule.INSTANCE.getSwingSpeed());
      }
   }

   @Inject(
      method = {"updateLimbs(F)V"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void animations$updateLimbs(float posDelta, CallbackInfo ci) {
      if (AnimationsModule.INSTANCE != null && AnimationsModule.INSTANCE.shouldCancelLimbSwing()) {
         ci.cancel();
      }
   }

   @Inject(
      method = {"updateLimbs(Z)V"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void animations$updateLimbs(boolean flutter, CallbackInfo ci) {
      if (AnimationsModule.INSTANCE != null && AnimationsModule.INSTANCE.shouldCancelLimbSwing()) {
         ci.cancel();
      }
   }

   @Inject(
      method = {"isFallFlying"},
      at = {@At("TAIL")},
      cancellable = true
   )
   public void recastOnLand(CallbackInfoReturnable<Boolean> cir) {
      boolean elytra = cir.getReturnValue();
      if (this.previousElytra && !elytra && ElytraFly.INSTANCE.isOn() && ElytraFly.INSTANCE.mode.is(ElytraFly.Mode.Bounce)) {
         cir.setReturnValue(ElytraFly.recastElytra(MinecraftClient.getInstance().player));
      }

      this.previousElytra = elytra;
   }

   @Inject(
      method = {"getFallFlyingTicks"},
      at = {@At("HEAD")},
      cancellable = true
   )
   public void getFallFlyingTicksHook(CallbackInfoReturnable<Integer> cir) {
      LivingEntity player = LivingEntity.class.cast(this);
      if (player == MinecraftClient.getInstance().player && ElytraFly.INSTANCE != null && ElytraFly.INSTANCE.shouldVisualSpoof()) {
         cir.setReturnValue(0);
      }
   }

   @Inject(
      method = {"getDimensions"},
      at = {@At("HEAD")},
      cancellable = true
   )
   public void getDimensionsHook(EntityPose pose, CallbackInfoReturnable<EntityDimensions> cir) {
      LivingEntity player = LivingEntity.class.cast(this);
      if (player == MinecraftClient.getInstance().player
         && ElytraFly.INSTANCE != null
         && ElytraFly.INSTANCE.isOn()
         && ElytraFly.INSTANCE.noAnimation.getValue()
         && player.isFallFlying()) {
         cir.setReturnValue(super.getDimensions(EntityPose.STANDING));
      }
   }

   @Redirect(
      method = {"travel"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/entity/LivingEntity;hasStatusEffect(Lnet/minecraft/registry/entry/RegistryEntry;)Z"
      ),
      require = 0
   )
   private boolean travelEffectHook(LivingEntity instance, RegistryEntry<StatusEffect> effect) {
      if (AntiEffects.INSTANCE.isOn()) {
         if (effect == StatusEffects.SLOW_FALLING && AntiEffects.INSTANCE.slowFalling.getValue()) {
            return false;
         }

         if (effect == StatusEffects.LEVITATION && AntiEffects.INSTANCE.levitation.getValue()) {
            return false;
         }
      }

      return instance.hasStatusEffect(effect);
   }

   @Redirect(
      method = {"applyMovementInput"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/entity/LivingEntity;isClimbing()Z"
      ),
      require = 0
   )
   public boolean climbingHook(LivingEntity instance) {
      return Velocity.INSTANCE.isOn() && Velocity.INSTANCE.noClimb.getValue() && LivingEntity.class.cast(this) == MinecraftClient.getInstance().player
         ? false
         : instance.isClimbing();
   }

   @Redirect(
      method = {"applyClimbingSpeed"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/entity/LivingEntity;isClimbing()Z"
      ),
      require = 0
   )
   public boolean climbingHook2(LivingEntity instance) {
      return instance.isClimbing();
   }

   @Inject(
      method = {"updateTrackedPositionAndAngles"},
      at = {@At("HEAD")}
   )
   private void lerpToHook(double x, double y, double z, float yRot, float xRot, int steps, CallbackInfo ci) {
      if (AnimationsModule.INSTANCE != null && AnimationsModule.INSTANCE.shouldNoInterpolation()) {
         this.setPos(x, y, z);
         this.setYaw(yRot);
         this.setPitch(xRot);
      }

      Alien.EVENT_BUS.post(LerpToEvent.get(LivingEntity.class.cast(this), x, y, z, yRot, xRot, this.lastLerp));
      this.lastLerp = System.currentTimeMillis();
   }

   @Inject(
      method = {"setSprinting"},
      at = {@At("HEAD")},
      cancellable = true
   )
   public void setSprintingHook(boolean sprinting, CallbackInfo ci) {
      if (LivingEntity.class.cast(this) == MinecraftClient.getInstance().player) {
         SprintEvent event = SprintEvent.get();
         Alien.EVENT_BUS.post(event);
         if (event.isCancelled()) {
            ci.cancel();
            sprinting = event.isSprint();
            super.setSprinting(sprinting);
            EntityAttributeInstance entityAttributeInstance = this.method_5996(EntityAttributes.GENERIC_MOVEMENT_SPEED);
            entityAttributeInstance.removeModifier(field_6231.id());
            if (sprinting) {
               entityAttributeInstance.addTemporaryModifier(field_6231);
            }
         }
      }
   }
}
