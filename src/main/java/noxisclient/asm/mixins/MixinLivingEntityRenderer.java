package noxisclient.asm.mixins;

import java.awt.Color;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import noxisclient.api.utils.Wrapper;
import noxisclient.api.utils.math.MathUtil;
import noxisclient.core.impl.RotationManager;
import noxisclient.mod.modules.impl.client.ClientSetting;
import noxisclient.mod.modules.impl.movement.ElytraFly;
import noxisclient.mod.modules.impl.render.NoRender;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Mixin({LivingEntityRenderer.class})
public abstract class MixinLivingEntityRenderer<T extends LivingEntity, M extends EntityModel<T>> {
   @Unique
   private LivingEntity lastEntity;
   @Unique
   private float originalYaw;
   @Unique
   private float originalHeadYaw;
   @Unique
   private float originalBodyYaw;
   @Unique
   private float originalPitch;
   @Unique
   private float originalPrevYaw;
   @Unique
   private float originalPrevHeadYaw;
   @Unique
   private float originalPrevBodyYaw;
   @Unique
   private float originalPrevPitch;
   @Unique
   private boolean spoofedRotation;

   @Inject(
      method = {"render*"},
      at = {@At("HEAD")}
   )
   public void onRenderPre(T livingEntity, float f, float g, MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, int i, CallbackInfo ci) {
      this.spoofedRotation = false;
      if (MinecraftClient.getInstance().player != null && livingEntity == MinecraftClient.getInstance().player) {
         boolean lockElytraRotation = ElytraFly.INSTANCE != null && ElytraFly.INSTANCE.shouldLockVisualRotation();
         if (ClientSetting.INSTANCE.rotations.getValue() || lockElytraRotation) {
            float renderYaw = lockElytraRotation ? ElytraFly.INSTANCE.getVisualSpoofYaw() : RotationManager.getRenderYawOffset();
            float headYaw = lockElytraRotation ? ElytraFly.INSTANCE.getVisualSpoofYaw() : RotationManager.getRotationYawHead();
            float renderPitch = lockElytraRotation ? ElytraFly.INSTANCE.getVisualSpoofPitch() : RotationManager.getRenderPitch();
            this.originalYaw = livingEntity.getYaw();
            this.originalHeadYaw = livingEntity.headYaw;
            this.originalBodyYaw = livingEntity.bodyYaw;
            this.originalPitch = livingEntity.getPitch();
            this.originalPrevYaw = livingEntity.prevYaw;
            this.originalPrevHeadYaw = livingEntity.prevHeadYaw;
            this.originalPrevBodyYaw = livingEntity.prevBodyYaw;
            this.originalPrevPitch = livingEntity.prevPitch;
            livingEntity.setYaw(renderYaw);
            livingEntity.headYaw = headYaw;
            livingEntity.bodyYaw = renderYaw;
            livingEntity.setPitch(renderPitch);
            livingEntity.prevYaw = lockElytraRotation ? renderYaw : RotationManager.getPrevRenderYawOffset();
            livingEntity.prevHeadYaw = lockElytraRotation ? headYaw : RotationManager.getPrevRotationYawHead();
            livingEntity.prevBodyYaw = lockElytraRotation ? renderYaw : RotationManager.getPrevRenderYawOffset();
            livingEntity.prevPitch = lockElytraRotation ? renderPitch : RotationManager.getPrevRenderPitch();
            this.spoofedRotation = true;
         }
      }

      this.lastEntity = livingEntity;
   }

   @Inject(
      method = {"render*"},
      at = {@At("TAIL")}
   )
   public void onRenderPost(T livingEntity, float f, float g, MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, int i, CallbackInfo ci) {
      if (MinecraftClient.getInstance().player != null && livingEntity == MinecraftClient.getInstance().player && this.spoofedRotation) {
         livingEntity.setYaw(this.originalYaw);
         livingEntity.headYaw = this.originalHeadYaw;
         livingEntity.bodyYaw = this.originalBodyYaw;
         livingEntity.setPitch(this.originalPitch);
         livingEntity.prevYaw = this.originalPrevYaw;
         livingEntity.prevHeadYaw = this.originalPrevHeadYaw;
         livingEntity.prevBodyYaw = this.originalPrevBodyYaw;
         livingEntity.prevPitch = this.originalPrevPitch;
         this.spoofedRotation = false;
      }
   }

   @ModifyArgs(
      method = {"render*"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/render/entity/model/EntityModel;render(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumer;III)V"
      )
   )
   private void renderHook(Args args) {
      float alpha = -1.0F;
      if (NoRender.INSTANCE.isOn() && NoRender.INSTANCE.antiPlayerCollision.getValue() && this.lastEntity != Wrapper.mc.player) {
         LivingEntity livingEntity = this.lastEntity;
         PlayerEntity pl;
         if (this.lastEntity instanceof PlayerEntity && !(pl = (PlayerEntity)livingEntity).isInvisible()) {
            alpha = MathUtil.clamp((float)(Wrapper.mc.player.squaredDistanceTo(this.lastEntity.getPos()) / 3.0) + 0.2F, 0.0F, 1.0F);
         }
      }

      if (alpha != -1.0F) {
         args.set(4, this.applyOpacity(654311423, alpha));
      }
   }

   @Unique
   int applyOpacity(int color_int, float opacity) {
      opacity = Math.min(1.0F, Math.max(0.0F, opacity));
      Color color = new Color(color_int);
      return new Color(color.getRed(), color.getGreen(), color.getBlue(), (int)((float)color.getAlpha() * opacity)).getRGB();
   }
}
