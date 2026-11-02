package noxisclient.api.utils.render;

import java.awt.Color;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.Camera;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Vec3d;
import noxisclient.api.utils.Wrapper;
import noxisclient.core.impl.FontManager;
import noxisclient.mod.modules.impl.client.Fonts;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.opengl.GL11;

public class TextUtil implements Wrapper {
   public static final Matrix4f lastProjMat = new Matrix4f();
   public static final Matrix4f lastModMat = new Matrix4f();
   public static final Matrix4f lastWorldSpaceMatrix = new Matrix4f();

   public static float getWidth(String s) {
      return (float)mc.textRenderer.getWidth(s);
   }

   public static float getHeight() {
      return 9.0F;
   }

   public static float getHudWidth(String text, float scale) {
      float safeScale = sanitizeHudScale(scale);
      String normalized = FontManager.normalizeFormattingPrefixes(text);
      return FontManager.isCustom()
         ? FontManager.get(FontManager.getSize() * safeScale).getWidth(normalized)
         : (float)mc.textRenderer.getWidth(normalized) * safeScale;
   }

   public static float getHudHeight(float scale) {
      float safeScale = sanitizeHudScale(scale);
      return FontManager.isCustom() ? FontManager.get(FontManager.getSize() * safeScale).getFontHeight() : 9.0F * safeScale;
   }

   public static float getHudFontSize(float scale) {
      float baseSize = FontManager.isCustom() ? FontManager.getSize() : 9.0F;
      float scaledSize = baseSize * sanitizeHudScale(scale);
      return FontManager.isCustom() ? Math.max(1.0F, scaledSize * 0.5F) : scaledSize;
   }

   public static float getHudRenderOffsetX() {
      return FontManager.isCustom() && Fonts.INSTANCE != null ? Fonts.INSTANCE.translate.getValueFloat() : 0.0F;
   }

   public static float getHudRenderOffsetY() {
      return FontManager.isCustom() && Fonts.INSTANCE != null ? Fonts.INSTANCE.shift.getValueFloat() : 0.0F;
   }

   public static void drawStringWithScale(DrawContext drawContext, String text, float x, float y, int color, float scale) {
      MatrixStack matrixStack = drawContext.getMatrices();
      if (scale != 1.0F) {
         matrixStack.push();
         matrixStack.scale(scale, scale, 1.0F);
         if (scale > 1.0F) {
            matrixStack.translate(-x / scale, -y / scale, 0.0F);
         } else {
            matrixStack.translate(x / scale / 2.0F, y / scale / 2.0F, 0.0F);
         }
      }

      drawString(drawContext, text, (double)x, (double)y, color);
      matrixStack.pop();
   }

   public static void drawStringScale(DrawContext drawContext, String text, float x, float y, int color, float scale, boolean shadow) {
      MatrixStack matrixStack = drawContext.getMatrices();
      if (scale != 1.0F) {
         matrixStack.push();
         matrixStack.scale(scale, scale, 1.0F);
         if (scale > 1.0F) {
            matrixStack.translate(-x / scale, -y / scale, 0.0F);
         } else {
            matrixStack.translate(x / scale / 2.0F, y / scale / 2.0F, 0.0F);
         }
      }

      drawContext.drawText(mc.textRenderer, text, (int)x, (int)y, color, shadow);
      matrixStack.pop();
   }

   public static void drawString(DrawContext drawContext, String text, double x, double y, int color) {
      drawString(drawContext, text, x, y, color, false);
   }

   public static void drawString(DrawContext drawContext, String text, double x, double y, int color, boolean customFont) {
      drawString(drawContext, text, x, y, color, customFont, true);
   }

   public static void drawHudString(DrawContext drawContext, String text, double x, double y, int color, boolean shadow) {
      drawHudString(drawContext, text, x, y, color, shadow, 1.0F);
   }

   public static void drawHudString(DrawContext drawContext, String text, double x, double y, int color, boolean shadow, float scale) {
      float safeScale = sanitizeHudScale(scale);
      String normalized = FontManager.normalizeFormattingPrefixes(text);
      if (FontManager.isCustom()) {
         FontManager.get(FontManager.getSize() * safeScale).drawString(drawContext.getMatrices(), normalized, x, y, color, shadow);
      } else if (Math.abs(safeScale - 1.0F) < 0.001F) {
         drawContext.drawText(mc.textRenderer, normalized, (int)x, (int)y, color, shadow);
      } else {
         MatrixStack matrices = drawContext.getMatrices();
         matrices.push();
         matrices.translate((float)x, (float)y, 0.0F);
         matrices.scale(safeScale, safeScale, 1.0F);
         drawContext.drawText(mc.textRenderer, normalized, 0, 0, color, shadow);
         matrices.pop();
      }
   }

   public static void drawString(DrawContext drawContext, String text, double x, double y, int color, boolean customFont, boolean shadow) {
      if (customFont) {
         FontManager.ui.drawString(drawContext.getMatrices(), text, (double)((int)x), (double)((int)y), color, shadow);
      } else {
         drawContext.drawText(mc.textRenderer, text, (int)x, (int)y, color, shadow);
      }
   }

   private static float sanitizeHudScale(float scale) {
      return Math.max(0.1F, scale);
   }

   public static void drawStringPulse(
      DrawContext drawContext, String text, double x, double y, Color startColor, Color endColor, double speed, int counter, boolean customFont
   ) {
      int startRgb = startColor.getRGB();
      int endRgb = endColor.getRGB();
      char[] stringToCharArray = text.toCharArray();
      int index = 0;
      boolean color = false;
      String s = null;

      for (char c : stringToCharArray) {
         if (c == 167) {
            color = true;
         } else if (color) {
            s = c == 'r' ? null : "§" + c;
            color = false;
         } else {
            index++;
            if (s != null) {
               drawString(drawContext, s + c, x, y, startRgb, customFont);
            } else {
               int pulseColor = ColorUtil.pulseColor(startRgb, endRgb, (double)index, counter, speed);
               drawString(drawContext, String.valueOf(c), x, y, pulseColor, customFont);
            }

            x += customFont ? (double)FontManager.ui.getAdvance(c) : (double)mc.textRenderer.getWidth(String.valueOf(c));
         }
      }
   }

   public static void drawStringPulse(
      DrawContext drawContext, String text, double x, double y, Color startColor, Color endColor, double speed, int counter, boolean customFont, boolean shadow
   ) {
      int startRgb = startColor.getRGB();
      int endRgb = endColor.getRGB();
      char[] stringToCharArray = text.toCharArray();
      int index = 0;
      boolean color = false;
      String s = null;

      for (char c : stringToCharArray) {
         if (c == 167) {
            color = true;
         } else if (color) {
            s = c == 'r' ? null : "§" + c;
            color = false;
         } else {
            index++;
            if (s != null) {
               drawString(drawContext, s + c, x, y, startRgb, customFont, shadow);
            } else {
               int pulseColor = ColorUtil.pulseColor(startRgb, endRgb, (double)index, counter, speed);
               drawString(drawContext, String.valueOf(c), x, y, pulseColor, customFont, shadow);
            }

            x += customFont ? (double)FontManager.ui.getAdvance(c) : (double)mc.textRenderer.getWidth(String.valueOf(c));
         }
      }
   }

   public static Vec3d worldSpaceToScreenSpace(Vec3d pos) {
      Camera camera = mc.getEntityRenderDispatcher().camera;
      int displayHeight = mc.getWindow().getHeight();
      int[] viewport = new int[4];
      GL11.glGetIntegerv(2978, viewport);
      Vector3f target = new Vector3f();
      double deltaX = pos.x - camera.getPos().x;
      double deltaY = pos.y - camera.getPos().y;
      double deltaZ = pos.z - camera.getPos().z;
      Vector4f transformedCoordinates = new Vector4f((float)deltaX, (float)deltaY, (float)deltaZ, 1.0F).mul(lastWorldSpaceMatrix);
      Matrix4f matrixProj = new Matrix4f(lastProjMat);
      Matrix4f matrixModel = new Matrix4f(lastModMat);
      matrixProj.mul(matrixModel).project(transformedCoordinates.x(), transformedCoordinates.y(), transformedCoordinates.z(), viewport, target);
      return new Vec3d(
         (double)target.x / mc.getWindow().getScaleFactor(), (double)((float)displayHeight - target.y) / mc.getWindow().getScaleFactor(), (double)target.z
      );
   }
}
