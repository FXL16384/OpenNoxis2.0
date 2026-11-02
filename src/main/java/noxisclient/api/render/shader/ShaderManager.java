package noxisclient.api.render.shader;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.GlStateManager.DstFactor;
import com.mojang.blaze3d.platform.GlStateManager.SrcFactor;
import com.mojang.blaze3d.systems.RenderSystem;
import java.awt.Color;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.PostEffectProcessor;
import net.minecraft.client.render.OutlineVertexConsumerProvider;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderPhase;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.render.RenderLayer.MultiPhaseParameters;
import net.minecraft.client.render.RenderLayer.OutlineMode;
import net.minecraft.client.render.RenderPhase.Target;
import net.minecraft.client.render.RenderPhase.TextureBase;
import net.minecraft.client.render.VertexFormat.DrawMode;
import net.minecraft.client.util.BufferAllocator;
import net.minecraft.util.Identifier;
import noxisclient.api.render.satin.ManagedShaderEffect;
import noxisclient.api.render.satin.ShaderEffectManager;
import noxisclient.api.utils.Globals;

public class ShaderManager implements Globals {
   private final OutlineVertexConsumerProvider vertexConsumerProvider = new OutlineVertexConsumerProvider(
      VertexConsumerProvider.immediate(new BufferAllocator(256))
   );
   private final Function<TextureBase, RenderLayer> layerCreator;
   private final Target target = new Target("shader_target", () -> {
   }, () -> {
   });
   private ShaderFramebuffer framebuffer;
   public ManagedShaderEffect filledShaderEffect;
   public ManagedShaderEffect gradientShaderEffect;
   public ManagedShaderEffect imageShaderEffect;
   public ManagedShaderEffect glowingShaderEffect;
   public ManagedShaderEffect rainbowShaderEffect;

   public ShaderManager() {
      this.layerCreator = this.memoizeTexture(
         texture -> RenderLayer.of(
               "noxisclient_overlay",
               VertexFormats.POSITION_TEXTURE_COLOR,
               DrawMode.QUADS,
               1536,
               MultiPhaseParameters.builder()
                  .program(RenderPhase.OUTLINE_PROGRAM)
                  .cull(RenderPhase.DISABLE_CULLING)
                  .texture(texture)
                  .depthTest(RenderPhase.ALWAYS_DEPTH_TEST)
                  .target(this.target)
                  .build(OutlineMode.IS_OUTLINE)
            )
      );
   }

   public void reloadShaders() {
      if (this.framebuffer == null
         || this.filledShaderEffect == null
         || this.gradientShaderEffect == null
         || this.imageShaderEffect == null
         || this.glowingShaderEffect == null) {
         this.reloadShadersInternal();
      }
   }

   public void reloadShadersInternal() {
      this.framebuffer = new ShaderFramebuffer(mc.getFramebuffer().textureWidth, mc.getFramebuffer().textureHeight);
      this.filledShaderEffect = ShaderEffectManager.getInstance().manage(Identifier.of("noxisclient", "shaders/post/outline.json"));
      this.gradientShaderEffect = ShaderEffectManager.getInstance().manage(Identifier.of("noxisclient", "shaders/post/gradient.json"));
      this.imageShaderEffect = ShaderEffectManager.getInstance().manage(Identifier.of("noxisclient", "shaders/post/image.json"));
      this.glowingShaderEffect = ShaderEffectManager.getInstance().manage(Identifier.of("noxisclient", "shaders/post/glowing.json"));
      this.rainbowShaderEffect = ShaderEffectManager.getInstance().manage(Identifier.of("noxisclient", "shaders/post/rainbow.json"));
   }

   public void applyShader(ManagedShaderEffect shaderEffect, Runnable setup, Runnable runnable) {
      Framebuffer mcFramebuffer = mc.getFramebuffer();
      RenderSystem.assertOnRenderThreadOrInit();
      if (this.framebuffer.textureWidth != mcFramebuffer.textureWidth || this.framebuffer.textureHeight != mcFramebuffer.textureHeight) {
         this.framebuffer.resize(mcFramebuffer.textureWidth, mcFramebuffer.textureHeight, false);
      }

      GlStateManager._glBindFramebuffer(36009, this.framebuffer.fbo);
      this.framebuffer.beginWrite(false);
      runnable.run();
      this.framebuffer.endWrite();
      GlStateManager._glBindFramebuffer(36009, mcFramebuffer.fbo);
      mcFramebuffer.beginWrite(false);
      PostEffectProcessor effect = shaderEffect.getShaderEffect();
      if (effect != null) {
         Framebuffer bufOut = effect.getSecondaryTarget("bufOut");
         setup.run();
         this.framebuffer.clear(false);
         mcFramebuffer.beginWrite(false);
         RenderSystem.enableBlend();
         RenderSystem.blendFuncSeparate(SrcFactor.SRC_ALPHA, DstFactor.ONE_MINUS_SRC_ALPHA, SrcFactor.ZERO, DstFactor.ONE);
         RenderSystem.backupProjectionMatrix();
         if (bufOut != null) {
            bufOut.draw(bufOut.textureWidth, bufOut.textureHeight, false);
         }

         RenderSystem.restoreProjectionMatrix();
         RenderSystem.defaultBlendFunc();
         RenderSystem.disableBlend();
      }
   }

   public VertexConsumerProvider createVertexConsumers(VertexConsumerProvider parent, Color color) {
      return layer -> parent.getBuffer(layer);
   }

   private Function<TextureBase, RenderLayer> memoizeTexture(Function<TextureBase, RenderLayer> function) {
      return new Function<TextureBase, RenderLayer>() {
         private final Map<Identifier, RenderLayer> cache = new HashMap<>();

         public RenderLayer apply(TextureBase texture) {
            return function.apply(texture);
         }
      };
   }

   public ManagedShaderEffect getFilledShaderEffect() {
      return this.filledShaderEffect;
   }

   public ManagedShaderEffect getGradientShaderEffect() {
      return this.gradientShaderEffect;
   }

   public ManagedShaderEffect getImageShaderEffect() {
      return this.imageShaderEffect;
   }

   public ManagedShaderEffect getGlowingShaderEffect() {
      return this.glowingShaderEffect;
   }

   public ManagedShaderEffect getRainbowShaderEffect() {
      return this.rainbowShaderEffect;
   }
}
