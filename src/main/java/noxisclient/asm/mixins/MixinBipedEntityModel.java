package noxisclient.asm.mixins;

import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.entity.model.AnimalModel;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.render.entity.model.BipedEntityModel.ArmPose;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Arm;
import net.minecraft.util.math.MathHelper;
import noxisclient.mod.modules.impl.render.SpearFix;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({BipedEntityModel.class})
public abstract class MixinBipedEntityModel<T extends LivingEntity> extends AnimalModel<T> {
   @Shadow
   @Final
   public ModelPart field_3401;
   @Shadow
   @Final
   public ModelPart field_27433;
   @Shadow
   @Final
   public ModelPart field_3391;
   @Shadow
   @Final
   public ModelPart field_3398;
   @Shadow
   public ArmPose field_3395;
   @Shadow
   public ArmPose field_3399;

   @Shadow
   protected abstract ModelPart method_2808(Arm var1);

   @Inject(
      method = {"positionRightArm"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void spearFix$positionRightArm(T entity, CallbackInfo ci) {
      if (this.spearFix$handleSpearHold(entity, Arm.RIGHT) && !entity.isUsingItem() && this.handSwingProgress > 0.0F) {
         ci.cancel();
      }
   }

   @Inject(
      method = {"positionLeftArm"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void spearFix$positionLeftArm(T entity, CallbackInfo ci) {
      if (this.spearFix$handleSpearHold(entity, Arm.LEFT) && !entity.isUsingItem() && this.handSwingProgress > 0.0F) {
         ci.cancel();
      }
   }

   @Inject(
      method = {"animateArms"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/util/math/MathHelper;sin(F)F",
         ordinal = 3
      )},
      cancellable = true
   )
   private void spearFix$animateArms(T entity, float animationProgress, CallbackInfo ci) {
      SpearFix spearFix = SpearFix.INSTANCE;
      if (spearFix != null && spearFix.shouldAnimateThirdPerson(entity.getMainHandStack())) {
         Arm arm = entity.getMainArm();
         this.field_3401.yaw = this.field_3401.yaw - this.field_3391.yaw;
         this.field_27433.yaw = this.field_27433.yaw - this.field_3391.yaw;
         this.field_27433.pitch = this.field_27433.pitch - this.field_3391.yaw;
         ModelPart var10000 = this.method_2808(arm);
         var10000.pitch = var10000.pitch + spearFix.getThirdPersonSwing(this.handSwingProgress);
         ci.cancel();
      }
   }

   @Unique
   private boolean spearFix$handleSpearHold(T entity, Arm arm) {
      SpearFix spearFix = SpearFix.INSTANCE;
      if (spearFix == null) {
         return false;
      } else {
         ItemStack itemStack = entity.getMainArm() == arm ? entity.getMainHandStack() : entity.getOffHandStack();
         if (!spearFix.shouldAnimateThirdPerson(itemStack)) {
            return false;
         } else {
            ModelPart usedArm = this.method_2808(arm);
            usedArm.yaw = -0.1F * this.field_3398.yaw;
            usedArm.pitch = (float) (-Math.PI / 2) + this.field_3398.pitch + 0.8F;
            if (entity.isFallFlying() || entity.getLeaningPitch(spearFix.getTickDelta()) > 0.0F) {
               usedArm.pitch -= 0.9599311F;
            }

            usedArm.roll = usedArm.roll + (arm == Arm.RIGHT ? -1.0F : 1.0F) * 0.03F * MathHelper.sin(((float)entity.age + spearFix.getTickDelta()) * 0.15F);
            return true;
         }
      }
   }
}
