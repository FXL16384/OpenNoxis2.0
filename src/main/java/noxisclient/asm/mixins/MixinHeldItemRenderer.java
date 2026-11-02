package noxisclient.asm.mixins;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalBooleanRef;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Arm;
import net.minecraft.util.Hand;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import noxisclient.Vitality;
import noxisclient.api.events.impl.HeldItemRendererEvent;
import noxisclient.mod.modules.impl.render.AnimationsModule;
import noxisclient.mod.modules.impl.render.SpearFix;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({HeldItemRenderer.class})
public abstract class MixinHeldItemRenderer {
   @Shadow
   private ItemStack field_4047;
   @Shadow
   private ItemStack field_4048;
   @Shadow
   public float field_4043;
   @Shadow
   public float field_4052;
   @Shadow
   public float field_4053;
   @Shadow
   public float field_4051;

   @Inject(
      method = {"updateHeldItems"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void animations$updateHeldItems(CallbackInfo ci) {
      if (AnimationsModule.INSTANCE != null && AnimationsModule.INSTANCE.shouldNoSwitchAnimation()) {
         ItemStack itemStack = MinecraftClient.getInstance().player.getMainHandStack();
         ItemStack itemStack2 = MinecraftClient.getInstance().player.getOffHandStack();
         ci.cancel();
         this.field_4043 = 1.0F;
         this.field_4052 = 1.0F;
         this.field_4053 = 1.0F;
         this.field_4051 = 1.0F;
         this.field_4047 = itemStack;
         this.field_4048 = itemStack2;
      }
   }

   @ModifyArg(
      method = {"updateHeldItems"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/util/math/MathHelper;clamp(FFF)F",
         ordinal = 2
      ),
      index = 0
   )
   private float animations$equipProgressMainhand(float value) {
      if (MinecraftClient.getInstance().player == null) {
         return value;
      } else {
         float f = MinecraftClient.getInstance().player.getAttackCooldownProgress(1.0F);
         float modified = AnimationsModule.INSTANCE != null && AnimationsModule.INSTANCE.shouldOldSwingAnimation() ? 1.0F : f * f * f;
         return (ItemStack.areEqual(this.field_4047, MinecraftClient.getInstance().player.getMainHandStack()) ? modified : 0.0F) - this.field_4043;
      }
   }

   @Inject(
      method = {"renderFirstPersonItem"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/render/item/HeldItemRenderer;renderItem(Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/item/ItemStack;Lnet/minecraft/client/render/model/json/ModelTransformationMode;ZLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V"
      )}
   )
   private void onRenderItem(
      AbstractClientPlayerEntity player,
      float tickDelta,
      float pitch,
      Hand hand,
      float swingProgress,
      ItemStack item,
      float equipProgress,
      MatrixStack matrices,
      VertexConsumerProvider vertexConsumers,
      int light,
      CallbackInfo ci
   ) {
      HeldItemRendererEvent event = HeldItemRendererEvent.get(hand, item, equipProgress, matrices);
      Vitality.EVENT_BUS.post(event);
   }

   @WrapOperation(
      method = {"renderFirstPersonItem"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/util/math/MatrixStack;translate(FFF)V",
         ordinal = 12
      )},
      require = 0
   )
   private void spearFix$suppressNormalSwing(
      MatrixStack instance, float x, float y, float z, Operation<Void> original, @Local(argsOnly = true) ItemStack itemStack
   ) {
      if (SpearFix.INSTANCE == null || !SpearFix.INSTANCE.shouldAnimateFirstPerson(itemStack)) {
         original.call(instance, x, y, z);
      }
   }

   @WrapOperation(
      method = {"renderFirstPersonItem"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/render/item/HeldItemRenderer;applySwingOffset(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/util/Arm;F)V"
      )},
      require = 0
   )
   private void spearFix$applySwingOffset(
      HeldItemRenderer instance, MatrixStack matrices, Arm arm, float swingProgress, Operation<Void> original, @Local(argsOnly = true) ItemStack itemStack
   ) {
      if (SpearFix.INSTANCE != null && SpearFix.INSTANCE.shouldAnimateFirstPerson(itemStack)) {
         SpearFix.INSTANCE.applyStabAnimation(matrices, swingProgress, itemStack);
      } else {
         original.call(instance, matrices, arm, swingProgress);
      }
   }

   @WrapOperation(
      method = {"renderFirstPersonItem"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/render/item/HeldItemRenderer;applyEquipOffset(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/util/Arm;F)V",
         ordinal = 8
      )},
      require = 0
   )
   private void spearFix$applyEquipOffset(
      HeldItemRenderer instance,
      MatrixStack matrices,
      Arm arm,
      float equipProgress,
      Operation<Void> original,
      @Local(argsOnly = true) AbstractClientPlayerEntity player,
      @Local(argsOnly = true) ItemStack itemStack
   ) {
      if (SpearFix.INSTANCE != null && SpearFix.INSTANCE.shouldSuppressEquip(itemStack, player.handSwingProgress)) {
         original.call(instance, matrices, arm, 0.0F);
      } else {
         original.call(instance, matrices, arm, equipProgress);
      }
   }

   @Inject(
      method = {"renderFirstPersonItem"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/item/ItemStack;getUseAction()Lnet/minecraft/util/UseAction;"
      )}
   )
   private void spearFix$useAnimation(
      AbstractClientPlayerEntity player,
      float tickDelta,
      float pitch,
      Hand hand,
      float swingProgress,
      ItemStack item,
      float equipProgress,
      MatrixStack matrices,
      VertexConsumerProvider vertexConsumers,
      int light,
      CallbackInfo ci,
      @Share("spearFixSuppress") LocalBooleanRef suppress
   ) {
      if (SpearFix.INSTANCE != null && SpearFix.INSTANCE.shouldUseAnimate(item)) {
         boolean spearUse = player.getActiveHand() == hand && SpearFix.INSTANCE.isUsingSpear(player);
         if (spearUse) {
            Arm arm = hand == Hand.MAIN_HAND ? player.getMainArm() : player.getMainArm().getOpposite();
            float useTicks = Math.max(0.0F, (float)player.getItemUseTime() + tickDelta - 1.0F);
            SpearFix.INSTANCE.applyFirstPersonUseAnimation(matrices, player, arm, item, useTicks);
            suppress.set(true);
         }
      }
   }

   @WrapOperation(
      method = {"renderFirstPersonItem"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/render/item/HeldItemRenderer;applyEquipOffset(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/util/Arm;F)V",
         ordinal = 2
      )},
      require = 0
   )
   private void spearFix$suppressEquipOffsetForUseAnimation(
      HeldItemRenderer instance,
      MatrixStack matrices,
      Arm arm,
      float equipProgress,
      Operation<Void> original,
      @Share("spearFixSuppress") LocalBooleanRef suppress
   ) {
      if (!suppress.get()) {
         original.call(instance, matrices, arm, equipProgress);
      }
   }

   @Inject(
      method = {"applyEatOrDrinkTransformation"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void hookApplyEatOrDrinkTransformation(MatrixStack matrices, float tickDelta, Arm arm, ItemStack stack, PlayerEntity player, CallbackInfo ci) {
      if (AnimationsModule.INSTANCE != null && AnimationsModule.INSTANCE.shouldTransformEat()) {
         ci.cancel();
         float f = (float)MinecraftClient.getInstance().player.getItemUseTimeLeft() - tickDelta + 1.0F;
         float g = f / (float)stack.getMaxUseTime(MinecraftClient.getInstance().player);
         if (g < 0.8F) {
            float h = MathHelper.abs(MathHelper.cos(f / 4.0F * (float) Math.PI) * 0.1F);
            matrices.translate(0.0F, h * AnimationsModule.INSTANCE.getEatTransformFactor(), 0.0F);
         }

         float h = 1.0F - (float)Math.pow((double)g, 27.0);
         int i = arm == Arm.RIGHT ? 1 : -1;
         matrices.translate(h * 0.6F * (float)i, h * -0.5F, h * 0.0F);
         matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees((float)i * h * 90.0F));
         matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(h * 10.0F));
         matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees((float)i * h * 30.0F));
      }
   }
}
