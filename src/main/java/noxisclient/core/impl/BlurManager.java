package noxisclient.core.impl;

import com.mojang.blaze3d.platform.GlStateManager.DstFactor;
import com.mojang.blaze3d.platform.GlStateManager.SrcFactor;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.PostEffectProcessor;
import net.minecraft.util.Identifier;
import noxisclient.api.interfaces.IShaderEffectHook;
import noxisclient.api.utils.Wrapper;
import org.ladysnake.satin.api.managed.ManagedShaderEffect;
import org.ladysnake.satin.api.managed.ShaderEffectManager;

public class BlurManager implements Wrapper {
   public static final ManagedShaderEffect BLUR = ShaderEffectManager.getInstance().manage(Identifier.of("noxisclient", "shaders/post/blur.json"));

   public void applyBlur(float radius, float startX, float startY, float width, float height) {
      this.applyBlur(radius, startX, startY, width, height, 0.0F);
   }

   public void applyRoundedBlur(float radius, float startX, float startY, float width, float height, float cornerRadius) {
      this.applyBlur(radius, startX, startY, width, height, cornerRadius);
   }

   public void applyBlur(float radius, float startX, float startY, float width, float height, float cornerRadius) {
      if (mc.getWindow() != null && mc.getFramebuffer() != null) {
         Framebuffer framebuffer = mc.getFramebuffer();
         PostEffectProcessor effect = BLUR.getShaderEffect();
         if (effect != null) {
            ((IShaderEffectHook)effect).alienClient$addHook("bufIn", framebuffer);
            float scaleFactor = (float)mc.getWindow().getScaleFactor();
            float framebufferWidth = Math.max(1.0F, (float)mc.getWindow().getFramebufferWidth());
            float framebufferHeight = Math.max(1.0F, (float)mc.getWindow().getFramebufferHeight());
            BLUR.setUniformValue("texelSize", 1.0F / framebufferWidth, 1.0F / framebufferHeight);
            BLUR.setUniformValue("radius", Math.max(0.0F, radius));
            BLUR.setUniformValue("BlurXY", startX * scaleFactor, framebufferHeight - (startY + height) * scaleFactor);
            BLUR.setUniformValue("BlurCoord", width * scaleFactor, height * scaleFactor);
            BLUR.setUniformValue("BlurRadius", Math.max(0.0F, cornerRadius * scaleFactor));
            BLUR.render(mc.getRenderTickCounter().getTickDelta(true));
            Framebuffer outBuffer = effect.getSecondaryTarget("bufOut");
            if (outBuffer != null) {
               framebuffer.beginWrite(false);
               RenderSystem.enableBlend();
               RenderSystem.blendFuncSeparate(SrcFactor.SRC_ALPHA, DstFactor.ONE_MINUS_SRC_ALPHA, SrcFactor.ZERO, DstFactor.ONE);
               RenderSystem.backupProjectionMatrix();
               outBuffer.draw(outBuffer.textureWidth, outBuffer.textureHeight, false);
               RenderSystem.restoreProjectionMatrix();
               RenderSystem.defaultBlendFunc();
               RenderSystem.disableBlend();
            }
         }
      }
   }
}
