package noxisclient.asm.mixins;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.PostEffectProcessor;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.render.VertexConsumerProvider.Immediate;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LightningEntity;
import noxisclient.Alien;
import noxisclient.api.utils.Wrapper;
import noxisclient.core.impl.ShaderManager;
import noxisclient.mod.modules.impl.player.Freecam;
import noxisclient.mod.modules.impl.render.Chams;
import noxisclient.mod.modules.impl.render.ShaderModule;
import noxisclient.mod.modules.impl.render.SkyBox;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(
   value = {WorldRenderer.class},
   priority = 900
)
public abstract class MixinWorldRenderer {
   @Unique
   boolean renderingChams = false;
   @Unique
   boolean renderingEntity = false;
   @Unique
   boolean renderingLightningShader = false;

   @Shadow
   abstract void method_22977(Entity var1, double var2, double var4, double var6, float var8, MatrixStack var9, VertexConsumerProvider var10);

   @Inject(
      method = {"renderEntity"},
      at = {@At("HEAD")}
   )
   private void injectChamsForEntity(
      Entity entity,
      double cameraX,
      double cameraY,
      double cameraZ,
      float tickDelta,
      MatrixStack matrices,
      VertexConsumerProvider vertexConsumers,
      CallbackInfo ci
   ) {
      if (Chams.INSTANCE != null && Chams.INSTANCE.isOn() && Chams.INSTANCE.throughWall.getValue()) {
         if (Chams.INSTANCE.chams(entity)) {
            if (this.renderingEntity) {
               Wrapper.mc.getBufferBuilders().getEntityVertexConsumers().draw();
               this.renderingEntity = false;
            }

            GL11.glEnable(32823);
            GL11.glPolygonOffset(1.0F, -1000000.0F);
            this.renderingChams = true;
         } else {
            this.renderingEntity = true;
         }
      }
   }

   @Inject(
      method = {"renderEntity"},
      at = {@At("RETURN")}
   )
   private void injectChamsForEntityPost(
      Entity entity,
      double cameraX,
      double cameraY,
      double cameraZ,
      float tickDelta,
      MatrixStack matrices,
      VertexConsumerProvider vertexConsumers,
      CallbackInfo ci
   ) {
      if (Chams.INSTANCE != null && Chams.INSTANCE.isOn() && Chams.INSTANCE.throughWall.getValue() && this.renderingChams) {
         Wrapper.mc.getBufferBuilders().getEntityVertexConsumers().draw();
         GL11.glPolygonOffset(1.0F, 1000000.0F);
         GL11.glDisable(32823);
         this.renderingChams = false;
      }
   }

   @Inject(
      method = {"renderEntity"},
      at = {@At("RETURN")}
   )
   private void injectLightningShaderPost(
      Entity entity,
      double cameraX,
      double cameraY,
      double cameraZ,
      float tickDelta,
      MatrixStack matrices,
      VertexConsumerProvider vertexConsumers,
      CallbackInfo ci
   ) {
      if (!this.renderingLightningShader) {
         if (entity instanceof LightningEntity && ShaderModule.INSTANCE != null && Alien.SHADER != null && ShaderModule.INSTANCE.shouldRenderLighting()) {
            this.renderingLightningShader = true;
            Alien.SHADER.applyShader(() -> {
               Immediate immediate = MinecraftClient.getInstance().getBufferBuilders().getEntityVertexConsumers();
               this.method_22977(entity, cameraX, cameraY, cameraZ, tickDelta, matrices, immediate);
               immediate.draw();
            }, ShaderModule.INSTANCE.mode.getValue());
            this.renderingLightningShader = false;
         }
      }
   }

   @Redirect(
      method = {"render"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/gl/PostEffectProcessor;render(F)V",
         ordinal = 0
      ),
      require = 0
   )
   void replaceShaderHook(PostEffectProcessor instance, float tickDelta) {
      if (ShaderModule.INSTANCE != null && ShaderModule.INSTANCE.isOn() && Wrapper.mc.world != null && Alien.SHADER != null) {
         ShaderManager.Shader shaders = ShaderModule.INSTANCE.mode.getValue();
         Alien.SHADER.setupShader(shaders, Alien.SHADER.getShaderOutline(shaders));
      } else {
         instance.render(tickDelta);
      }
   }

   @Redirect(
      method = {"render"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/render/WorldRenderer;renderSky(Lorg/joml/Matrix4f;Lorg/joml/Matrix4f;FLnet/minecraft/client/render/Camera;ZLjava/lang/Runnable;)V"
      ),
      require = 0
   )
   private void skyShaderHook(
      WorldRenderer instance, Matrix4f viewMatrix, Matrix4f projectionMatrix, float tickDelta, Camera camera, boolean thickFog, Runnable runnable
   ) {
      if (SkyBox.INSTANCE != null && SkyBox.INSTANCE.isOn() && Alien.SHADER != null) {
         Alien.SHADER.applySky(() -> instance.renderSky(viewMatrix, projectionMatrix, tickDelta, camera, thickFog, runnable));
         RenderSystem.disableDepthTest();
         RenderSystem.clear(256, MinecraftClient.IS_SYSTEM_MAC);
         RenderSystem.enableDepthTest();
      } else {
         instance.renderSky(viewMatrix, projectionMatrix, tickDelta, camera, thickFog, runnable);
      }
   }

   @Redirect(
      method = {"render"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/render/WorldRenderer;renderClouds(Lnet/minecraft/client/util/math/MatrixStack;Lorg/joml/Matrix4f;Lorg/joml/Matrix4f;FDDD)V"
      ),
      require = 0
   )
   private void skyBoxCloudsHook(
      WorldRenderer instance,
      MatrixStack matrices,
      Matrix4f viewMatrix,
      Matrix4f projectionMatrix,
      float tickDelta,
      double cameraX,
      double cameraY,
      double cameraZ
   ) {
      if (SkyBox.INSTANCE == null || !SkyBox.INSTANCE.isOn()) {
         instance.renderClouds(matrices, viewMatrix, projectionMatrix, tickDelta, cameraX, cameraY, cameraZ);
      }
   }

   @Inject(
      method = {"renderWeather"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void skyBoxWeatherHook(LightmapTextureManager manager, float tickDelta, double cameraX, double cameraY, double cameraZ, CallbackInfo ci) {
      if (SkyBox.INSTANCE != null && SkyBox.INSTANCE.isOn()) {
         ci.cancel();
      }
   }

   @ModifyArg(
      method = {"render"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/render/WorldRenderer;setupTerrain(Lnet/minecraft/client/render/Camera;Lnet/minecraft/client/render/Frustum;ZZ)V"
      ),
      index = 3
   )
   private boolean renderSetupTerrainModifyArg(boolean spectator) {
      return Freecam.INSTANCE != null && Freecam.INSTANCE.isOn() || spectator;
   }
}
