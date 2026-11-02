package noxisclient.mod.gui.click;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.Window;
import noxisclient.mod.modules.impl.client.ClickGui;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.nanovg.NVGColor;
import org.lwjgl.nanovg.NVGPaint;
import org.lwjgl.nanovg.NanoVG;
import org.lwjgl.nanovg.NanoVGGL3;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;

final class OpalRenderUtil {
   private static final NVGPaint PAINT = NVGPaint.create();
   private static final NVGColor COLOR_1 = NVGColor.create();
   private static final NVGColor COLOR_2 = NVGColor.create();
   private static long vg;
   private static boolean failed;
   private static OpalRenderUtil.GlState glState;

   private OpalRenderUtil() {
   }

   static void roundedAll(DrawContext context, float x, float y, float width, float height, float radius, int color) {
      roundedVarying(context, x, y, width, height, radius, radius, radius, radius, color);
   }

   static void roundedTop(DrawContext context, float x, float y, float width, float height, float radius, int color) {
      roundedVarying(context, x, y, width, height, radius, radius, 0.0F, 0.0F, color);
   }

   static void roundedBottom(DrawContext context, float x, float y, float width, float height, float radius, int color) {
      roundedVarying(context, x, y, width, height, 0.0F, 0.0F, radius, radius, color);
   }

   static void roundedBottomHorizontalGradient(DrawContext context, float x, float y, float width, float height, float radius, int leftColor, int rightColor) {
      roundedVaryingHorizontalGradient(context, x, y, width, height, 0.0F, 0.0F, radius, radius, leftColor, rightColor);
   }

   private static void roundedVarying(
      DrawContext context, float x, float y, float width, float height, float topLeft, float topRight, float bottomRight, float bottomLeft, int color
   ) {
      long handle = context();
      if (handle != 0L && !(width <= 0.0F) && !(height <= 0.0F) && (color >>> 24 & 0xFF) != 0) {
         OpalRenderUtil.Rect rect = transform(context, x, y, width, height);
         if (!(rect.width <= 0.0F) && !(rect.height <= 0.0F)) {
            if (beginFrame(handle)) {
               applyColor(color, COLOR_1);
               NanoVG.nvgBeginPath(handle);
               NanoVG.nvgFillColor(handle, COLOR_1);
               NanoVG.nvgRoundedRectVarying(
                  handle,
                  rect.x,
                  rect.y,
                  rect.width,
                  rect.height,
                  topLeft * rect.scaleX,
                  topRight * rect.scaleX,
                  bottomRight * rect.scaleX,
                  bottomLeft * rect.scaleX
               );
               NanoVG.nvgFill(handle);
               NanoVG.nvgClosePath(handle);
               endFrame(handle);
            }
         }
      }
   }

   private static void roundedVaryingHorizontalGradient(
      DrawContext context,
      float x,
      float y,
      float width,
      float height,
      float topLeft,
      float topRight,
      float bottomRight,
      float bottomLeft,
      int leftColor,
      int rightColor
   ) {
      long handle = context();
      if (handle != 0L && !(width <= 0.0F) && !(height <= 0.0F)) {
         OpalRenderUtil.Rect rect = transform(context, x, y, width, height);
         if (!(rect.width <= 0.0F) && !(rect.height <= 0.0F)) {
            if (beginFrame(handle)) {
               applyColor(leftColor, COLOR_1);
               applyColor(rightColor, COLOR_2);
               NanoVG.nvgLinearGradient(handle, rect.x, rect.y + rect.height * 0.5F, rect.x + rect.width, rect.y + rect.height * 0.5F, COLOR_1, COLOR_2, PAINT);
               NanoVG.nvgBeginPath(handle);
               NanoVG.nvgFillPaint(handle, PAINT);
               NanoVG.nvgRoundedRectVarying(
                  handle,
                  rect.x,
                  rect.y,
                  rect.width,
                  rect.height,
                  topLeft * rect.scaleX,
                  topRight * rect.scaleX,
                  bottomRight * rect.scaleX,
                  bottomLeft * rect.scaleX
               );
               NanoVG.nvgFill(handle);
               NanoVG.nvgClosePath(handle);
               endFrame(handle);
            }
         }
      }
   }

   private static long context() {
      if (failed) {
         return 0L;
      } else {
         if (vg == 0L) {
            try {
               vg = NanoVGGL3.nvgCreate(3);
            } catch (Throwable var1) {
               failed = true;
               return 0L;
            }

            if (vg == 0L) {
               failed = true;
            }
         }

         return vg;
      }
   }

   private static boolean beginFrame(long handle) {
      MinecraftClient client = MinecraftClient.getInstance();
      if (client != null && client.getWindow() != null) {
         Window window = client.getWindow();
         float scaleFactor = (float)window.getScaleFactor();
         RenderSystem.assertOnRenderThread();
         glState = OpalRenderUtil.GlState.capture();
         RenderSystem.enableBlend();
         RenderSystem.defaultBlendFunc();
         RenderSystem.disableDepthTest();
         NanoVG.nvgBeginFrame(handle, (float)window.getFramebufferWidth() / scaleFactor, (float)window.getFramebufferHeight() / scaleFactor, scaleFactor);
         ClickGui gui = ClickGui.getInstance();
         NanoVG.nvgGlobalAlpha(handle, gui == null ? 1.0F : Math.max(0.0F, Math.min(1.0F, (float)gui.alphaValue)));
         return true;
      } else {
         return false;
      }
   }

   private static void endFrame(long handle) {
      try {
         NanoVG.nvgEndFrame(handle);
      } finally {
         if (glState != null) {
            glState.restore();
            glState = null;
         } else {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client != null && client.getWindow() != null) {
               GL11.glViewport(0, 0, client.getWindow().getFramebufferWidth(), client.getWindow().getFramebufferHeight());
            }

            RenderSystem.enableDepthTest();
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
         }
      }
   }

   private static void applyColor(int color, NVGColor out) {
      float red = (float)(color >>> 16 & 0xFF) / 255.0F;
      float green = (float)(color >>> 8 & 0xFF) / 255.0F;
      float blue = (float)(color & 0xFF) / 255.0F;
      float alpha = (float)(color >>> 24 & 0xFF) / 255.0F;
      NanoVG.nvgRGBAf(red, green, blue, alpha, out);
   }

   private static OpalRenderUtil.Rect transform(DrawContext context, float x, float y, float width, float height) {
      Matrix4f matrix = context.getMatrices().peek().getPositionMatrix();
      Vector3f topLeft = matrix.transformPosition(x, y, 0.0F, new Vector3f());
      Vector3f bottomRight = matrix.transformPosition(x + width, y + height, 0.0F, new Vector3f());
      float tx = Math.min(topLeft.x, bottomRight.x);
      float ty = Math.min(topLeft.y, bottomRight.y);
      float tw = Math.abs(bottomRight.x - topLeft.x);
      float th = Math.abs(bottomRight.y - topLeft.y);
      return new OpalRenderUtil.Rect(tx, ty, tw, th, width == 0.0F ? 1.0F : tw / width);
   }

   private static record GlState(
      int program,
      int vertexArray,
      int arrayBuffer,
      int elementArrayBuffer,
      int activeTexture,
      int texture2d,
      int[] viewport,
      int stencilFunc,
      int stencilRef,
      int stencilValueMask,
      int stencilWriteMask,
      int stencilFail,
      int stencilPassDepthFail,
      int stencilPassDepthPass,
      boolean blend,
      boolean depthTest,
      boolean cullFace,
      boolean scissorTest,
      boolean stencilTest
   ) {
      static OpalRenderUtil.GlState capture() {
         int[] viewport = new int[4];
         GL11.glGetIntegerv(2978, viewport);
         return new OpalRenderUtil.GlState(
            GL11.glGetInteger(35725),
            GL11.glGetInteger(34229),
            GL11.glGetInteger(34964),
            GL11.glGetInteger(34965),
            GL11.glGetInteger(34016),
            GL11.glGetInteger(32873),
            viewport,
            GL11.glGetInteger(2962),
            GL11.glGetInteger(2967),
            GL11.glGetInteger(2963),
            GL11.glGetInteger(2968),
            GL11.glGetInteger(2964),
            GL11.glGetInteger(2965),
            GL11.glGetInteger(2966),
            GL11.glIsEnabled(3042),
            GL11.glIsEnabled(2929),
            GL11.glIsEnabled(2884),
            GL11.glIsEnabled(3089),
            GL11.glIsEnabled(2960)
         );
      }

      void restore() {
         GL20.glUseProgram(this.program);
         GL30.glBindVertexArray(this.vertexArray);
         GL15.glBindBuffer(34962, this.arrayBuffer);
         GL15.glBindBuffer(34963, this.elementArrayBuffer);
         GL13.glActiveTexture(this.activeTexture);
         GL11.glBindTexture(3553, this.texture2d);
         GL11.glViewport(this.viewport[0], this.viewport[1], this.viewport[2], this.viewport[3]);
         GL11.glStencilFunc(this.stencilFunc, this.stencilRef, this.stencilValueMask);
         GL11.glStencilMask(this.stencilWriteMask);
         GL11.glStencilOp(this.stencilFail, this.stencilPassDepthFail, this.stencilPassDepthPass);
         setEnabled(3042, this.blend);
         setEnabled(2929, this.depthTest);
         setEnabled(2884, this.cullFace);
         setEnabled(3089, this.scissorTest);
         setEnabled(2960, this.stencilTest);
         RenderSystem.defaultBlendFunc();
      }

      private static void setEnabled(int capability, boolean enabled) {
         if (enabled) {
            GL11.glEnable(capability);
         } else {
            GL11.glDisable(capability);
         }
      }
   }

   private static record Rect(float x, float y, float width, float height, float scaleX) {
   }
}
