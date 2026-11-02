package noxisclient.api.utils.render;

import com.mojang.blaze3d.systems.RenderSystem;
import java.awt.Color;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.render.VertexFormat.DrawMode;
import net.minecraft.client.util.math.MatrixStack;
import noxisclient.Alien;
import noxisclient.api.utils.Wrapper;
import org.joml.Matrix4f;

public class Render2DUtil implements Wrapper {
   private static final float SOFT_AA_WIDTH = 1.0F;

   public static void rect(MatrixStack stack, float x1, float y1, float x2, float y2, int color) {
      rectFilled(stack, x1, y1, x2, y2, color);
   }

   public static void arrow(MatrixStack matrixStack, float x, float y, Color color) {
      drawRectWithOutline(matrixStack, x - 1.0F, y - 1.0F, 2.0F, 2.0F, color, Color.BLACK);
   }

   public static void rectFilled(MatrixStack matrix, float x1, float y1, float x2, float y2, int color) {
      float f = (float)(color >> 24 & 0xFF) / 255.0F;
      float g = (float)(color >> 16 & 0xFF) / 255.0F;
      float h = (float)(color >> 8 & 0xFF) / 255.0F;
      float j = (float)(color & 0xFF) / 255.0F;
      if (!((double)f <= 0.01)) {
         if (x1 < x2) {
            float i = x1;
            x1 = x2;
            x2 = i;
         }

         if (y1 < y2) {
            float i = y1;
            y1 = y2;
            y2 = i;
         }

         RenderSystem.enableBlend();
         RenderSystem.setShader(GameRenderer::getPositionColorProgram);
         BufferBuilder bufferBuilder = Tessellator.getInstance().begin(DrawMode.QUADS, VertexFormats.POSITION_COLOR);
         bufferBuilder.vertex(matrix.peek().getPositionMatrix(), x1, y2, 0.0F).color(g, h, j, f);
         bufferBuilder.vertex(matrix.peek().getPositionMatrix(), x2, y2, 0.0F).color(g, h, j, f);
         bufferBuilder.vertex(matrix.peek().getPositionMatrix(), x2, y1, 0.0F).color(g, h, j, f);
         bufferBuilder.vertex(matrix.peek().getPositionMatrix(), x1, y1, 0.0F).color(g, h, j, f);
         BufferRenderer.drawWithGlobalProgram(bufferBuilder.end());
         RenderSystem.disableBlend();
      }
   }

   public static void horizontalGradient(MatrixStack matrices, float x1, float y1, float x2, float y2, Color startColor, Color endColor) {
      Matrix4f matrix = matrices.peek().getPositionMatrix();
      RenderSystem.enableBlend();
      RenderSystem.setShader(GameRenderer::getPositionColorProgram);
      BufferBuilder bufferBuilder = Tessellator.getInstance().begin(DrawMode.QUADS, VertexFormats.POSITION_COLOR);
      bufferBuilder.vertex(matrix, x1, y1, 0.0F).color(startColor.getRGB());
      bufferBuilder.vertex(matrix, x1, y2, 0.0F).color(startColor.getRGB());
      bufferBuilder.vertex(matrix, x2, y2, 0.0F).color(endColor.getRGB());
      bufferBuilder.vertex(matrix, x2, y1, 0.0F).color(endColor.getRGB());
      BufferRenderer.drawWithGlobalProgram(bufferBuilder.end());
      RenderSystem.disableBlend();
   }

   public static void horizontalGradient(MatrixStack matrices, float x1, float y1, float x2, float y2, int startColor, int endColor) {
      Matrix4f matrix = matrices.peek().getPositionMatrix();
      RenderSystem.enableBlend();
      RenderSystem.setShader(GameRenderer::getPositionColorProgram);
      BufferBuilder bufferBuilder = Tessellator.getInstance().begin(DrawMode.QUADS, VertexFormats.POSITION_COLOR);
      bufferBuilder.vertex(matrix, x1, y1, 0.0F).color(startColor);
      bufferBuilder.vertex(matrix, x1, y2, 0.0F).color(startColor);
      bufferBuilder.vertex(matrix, x2, y2, 0.0F).color(endColor);
      bufferBuilder.vertex(matrix, x2, y1, 0.0F).color(endColor);
      BufferRenderer.drawWithGlobalProgram(bufferBuilder.end());
      RenderSystem.disableBlend();
   }

   public static void verticalGradient(MatrixStack matrices, float x1, float y1, float x2, float y2, Color startColor, Color endColor) {
      Matrix4f matrix = matrices.peek().getPositionMatrix();
      RenderSystem.enableBlend();
      RenderSystem.setShader(GameRenderer::getPositionColorProgram);
      BufferBuilder bufferBuilder = Tessellator.getInstance().begin(DrawMode.QUADS, VertexFormats.POSITION_COLOR);
      bufferBuilder.vertex(matrix, x1, y1, 0.0F).color(startColor.getRGB());
      bufferBuilder.vertex(matrix, x2, y1, 0.0F).color(startColor.getRGB());
      bufferBuilder.vertex(matrix, x2, y2, 0.0F).color(endColor.getRGB());
      bufferBuilder.vertex(matrix, x1, y2, 0.0F).color(endColor.getRGB());
      BufferRenderer.drawWithGlobalProgram(bufferBuilder.end());
      RenderSystem.disableBlend();
   }

   public static void verticalGradient(MatrixStack matrices, float x1, float y1, float x2, float y2, int startColor, int endColor) {
      Matrix4f matrix = matrices.peek().getPositionMatrix();
      RenderSystem.enableBlend();
      RenderSystem.setShader(GameRenderer::getPositionColorProgram);
      BufferBuilder bufferBuilder = Tessellator.getInstance().begin(DrawMode.QUADS, VertexFormats.POSITION_COLOR);
      bufferBuilder.vertex(matrix, x1, y1, 0.0F).color(startColor);
      bufferBuilder.vertex(matrix, x2, y1, 0.0F).color(startColor);
      bufferBuilder.vertex(matrix, x2, y2, 0.0F).color(endColor);
      bufferBuilder.vertex(matrix, x1, y2, 0.0F).color(endColor);
      BufferRenderer.drawWithGlobalProgram(bufferBuilder.end());
      RenderSystem.disableBlend();
   }

   public static void drawLine(MatrixStack matrices, float x, float y, float x1, float y1, int color) {
      Matrix4f matrix = matrices.peek().getPositionMatrix();
      RenderSystem.enableBlend();
      RenderSystem.setShader(GameRenderer::getPositionColorProgram);
      BufferBuilder bufferBuilder = Tessellator.getInstance().begin(DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);
      bufferBuilder.vertex(matrix, x, y, 0.0F).color(color);
      bufferBuilder.vertex(matrix, x1, y1, 0.0F).color(color);
      BufferRenderer.drawWithGlobalProgram(bufferBuilder.end());
      RenderSystem.disableBlend();
   }

   public static void drawRectWithOutline(MatrixStack matrices, float x, float y, float width, float height, Color c, Color c2) {
      Matrix4f matrix = matrices.peek().getPositionMatrix();
      RenderSystem.enableBlend();
      RenderSystem.setShader(GameRenderer::getPositionColorProgram);
      BufferBuilder buffer = Tessellator.getInstance().begin(DrawMode.QUADS, VertexFormats.POSITION_COLOR);
      buffer.vertex(matrix, x, y + height, 0.0F).color(c.getRGB());
      buffer.vertex(matrix, x + width, y + height, 0.0F).color(c.getRGB());
      buffer.vertex(matrix, x + width, y, 0.0F).color(c.getRGB());
      buffer.vertex(matrix, x, y, 0.0F).color(c.getRGB());
      BufferRenderer.drawWithGlobalProgram(buffer.end());
      buffer = Tessellator.getInstance().begin(DrawMode.DEBUG_LINE_STRIP, VertexFormats.POSITION_COLOR);
      buffer.vertex(matrix, x, y + height, 0.0F).color(c2.getRGB());
      buffer.vertex(matrix, x + width, y + height, 0.0F).color(c2.getRGB());
      buffer.vertex(matrix, x + width, y, 0.0F).color(c2.getRGB());
      buffer.vertex(matrix, x, y, 0.0F).color(c2.getRGB());
      buffer.vertex(matrix, x, y + height, 0.0F).color(c2.getRGB());
      BufferRenderer.drawWithGlobalProgram(buffer.end());
      RenderSystem.disableBlend();
   }

   public static void drawRect(MatrixStack matrices, float x, float y, float width, float height, int c) {
      Matrix4f matrix = matrices.peek().getPositionMatrix();
      RenderSystem.enableBlend();
      RenderSystem.setShader(GameRenderer::getPositionColorProgram);
      BufferBuilder bufferBuilder = Tessellator.getInstance().begin(DrawMode.QUADS, VertexFormats.POSITION_COLOR);
      bufferBuilder.vertex(matrix, x, y + height, 0.0F).color(c);
      bufferBuilder.vertex(matrix, x + width, y + height, 0.0F).color(c);
      bufferBuilder.vertex(matrix, x + width, y, 0.0F).color(c);
      bufferBuilder.vertex(matrix, x, y, 0.0F).color(c);
      BufferRenderer.drawWithGlobalProgram(bufferBuilder.end());
      RenderSystem.disableBlend();
   }

   public static void drawRect(MatrixStack matrices, float x, float y, float width, float height, Color c) {
      drawRect(matrices, x, y, width, height, c.getRGB());
   }

   public static void drawRect(DrawContext drawContext, float x, float y, float width, float height, Color c) {
      drawRect(drawContext.getMatrices(), x, y, width, height, c);
   }

   public static boolean isHovered(double mouseX, double mouseY, double x, double y, double width, double height) {
      return mouseX >= x && mouseX - width <= x && mouseY >= y && mouseY - height <= y;
   }

   public static void drawGlow(MatrixStack matrices, float x, float y, float width, float height, int color) {
      Matrix4f matrix = matrices.peek().getPositionMatrix();
      int startColor = ColorUtil.injectAlpha(color, 20);
      RenderSystem.setShader(GameRenderer::getPositionColorProgram);
      RenderSystem.disableCull();
      RenderSystem.enableBlend();
      BufferBuilder bufferBuilder = Tessellator.getInstance().begin(DrawMode.QUADS, VertexFormats.POSITION_COLOR);
      float halfWidth = width / 2.0F;
      float halfHeight = height / 2.0F;
      float centerX = x + halfWidth;
      float centerY = y + halfHeight;
      float x2 = x + width;
      float y2 = y + height;
      bufferBuilder.vertex(matrix, centerX, centerY, 0.0F).color(color);
      bufferBuilder.vertex(matrix, x, centerY, 0.0F).color(startColor);
      bufferBuilder.vertex(matrix, x, y, 0.0F).color(startColor);
      bufferBuilder.vertex(matrix, centerX, y, 0.0F).color(startColor);
      bufferBuilder.vertex(matrix, centerX, centerY, 0.0F).color(color);
      bufferBuilder.vertex(matrix, centerX, y, 0.0F).color(startColor);
      bufferBuilder.vertex(matrix, x2, y, 0.0F).color(startColor);
      bufferBuilder.vertex(matrix, x2, centerY, 0.0F).color(startColor);
      bufferBuilder.vertex(matrix, centerX, centerY, 0.0F).color(color);
      bufferBuilder.vertex(matrix, x, centerY, 0.0F).color(startColor);
      bufferBuilder.vertex(matrix, x, y2, 0.0F).color(startColor);
      bufferBuilder.vertex(matrix, centerX, y2, 0.0F).color(startColor);
      bufferBuilder.vertex(matrix, centerX, centerY, 0.0F).color(color);
      bufferBuilder.vertex(matrix, x2, centerY, 0.0F).color(startColor);
      bufferBuilder.vertex(matrix, x2, y2, 0.0F).color(startColor);
      bufferBuilder.vertex(matrix, centerX, y2, 0.0F).color(startColor);
      BufferRenderer.drawWithGlobalProgram(bufferBuilder.end());
      RenderSystem.disableBlend();
      RenderSystem.enableCull();
   }

   public static void drawCircle(MatrixStack matrices, float cx, float cy, float r, Color c, int segments) {
      Matrix4f matrix = matrices.peek().getPositionMatrix();
      RenderSystem.enableBlend();
      RenderSystem.disableCull();
      RenderSystem.setShader(GameRenderer::getPositionColorProgram);
      BufferBuilder bufferBuilder = Tessellator.getInstance().begin(DrawMode.TRIANGLE_FAN, VertexFormats.POSITION_COLOR);
      bufferBuilder.vertex(matrix, cx, cy, 0.0F).color(c.getRGB());

      for (int i = 0; i <= segments; i++) {
         double keyCodec = (double)i * (Math.PI * 2) / (double)segments;
         float x = (float)((double)cx + Math.cos(keyCodec) * (double)r);
         float y = (float)((double)cy + Math.sin(keyCodec) * (double)r);
         bufferBuilder.vertex(matrix, x, y, 0.0F).color(c.getRGB());
      }

      BufferRenderer.drawWithGlobalProgram(bufferBuilder.end());
      RenderSystem.enableCull();
      RenderSystem.disableBlend();
   }

   public static void drawPill(MatrixStack matrices, float x, float y, float width, float height, Color c) {
      float r = height / 2.0F;
      drawRect(matrices, x + r, y, width - 2.0F * r, height, c);
      drawCircle(matrices, x + r, y + r, r, c, 64);
      drawCircle(matrices, x + width - r, y + r, r, c, 64);
   }

   public static void drawRoundedStroke(MatrixStack matrices, float x, float y, float width, float height, float radius, Color c, int seg) {
      Matrix4f matrix = matrices.peek().getPositionMatrix();
      RenderSystem.enableBlend();
      RenderSystem.setShader(GameRenderer::getPositionColorProgram);
      BufferBuilder buffer = Tessellator.getInstance().begin(DrawMode.DEBUG_LINE_STRIP, VertexFormats.POSITION_COLOR);
      float r = Math.min(radius, Math.min(width, height) / 2.0F);
      float x1 = x + r;
      float y1 = y + r;
      float x2 = x + width - r;
      float y2 = y + height - r;

      for (int i = 0; i <= seg; i++) {
         double keyCodec = (-Math.PI / 2) + (double)i * (Math.PI / 2) / (double)seg;
         buffer.vertex(matrix, (float)((double)x2 + Math.cos(keyCodec) * (double)r), (float)((double)y1 + Math.sin(keyCodec) * (double)r), 0.0F)
            .color(c.getRGB());
      }

      for (int var21 = 0; var21 <= seg; var21++) {
         double keyCodec = 0.0 + (double)var21 * (Math.PI / 2) / (double)seg;
         buffer.vertex(matrix, (float)((double)x2 + Math.cos(keyCodec) * (double)r), (float)((double)y2 + Math.sin(keyCodec) * (double)r), 0.0F)
            .color(c.getRGB());
      }

      for (int var22 = 0; var22 <= seg; var22++) {
         double keyCodec = (Math.PI / 2) + (double)var22 * (Math.PI / 2) / (double)seg;
         buffer.vertex(matrix, (float)((double)x1 + Math.cos(keyCodec) * (double)r), (float)((double)y2 + Math.sin(keyCodec) * (double)r), 0.0F)
            .color(c.getRGB());
      }

      for (int var23 = 0; var23 <= seg; var23++) {
         double keyCodec = Math.PI + (double)var23 * (Math.PI / 2) / (double)seg;
         buffer.vertex(matrix, (float)((double)x1 + Math.cos(keyCodec) * (double)r), (float)((double)y1 + Math.sin(keyCodec) * (double)r), 0.0F)
            .color(c.getRGB());
      }

      BufferRenderer.drawWithGlobalProgram(buffer.end());
      RenderSystem.disableBlend();
   }

   public static void drawRainbowRoundedStroke(MatrixStack matrices, float x, float y, float width, float height, float radius, int seg, float speed, int alpha) {
      Matrix4f matrix = matrices.peek().getPositionMatrix();
      RenderSystem.enableBlend();
      RenderSystem.setShader(GameRenderer::getPositionColorProgram);
      BufferBuilder buffer = Tessellator.getInstance().begin(DrawMode.DEBUG_LINE_STRIP, VertexFormats.POSITION_COLOR);
      float r = Math.min(radius, Math.min(width, height) / 2.0F);
      float x1 = x + r;
      float y1 = y + r;
      float x2 = x + width - r;
      float y2 = y + height - r;
      double t = (double)(System.currentTimeMillis() % (long)(1000.0F / Math.max(0.001F, speed))) / (double)(1000.0F / Math.max(0.001F, speed));

      for (int i2 = 0; i2 <= seg; i2++) {
         double keyCodec = (-Math.PI / 2) + (double)i2 * (Math.PI / 2) / (double)seg;
         float hue = (float)((keyCodec + (Math.PI * 2)) / (Math.PI * 2) + t) % 1.0F;
         int rgb2 = Color.HSBtoRGB(hue, 1.0F, 1.0F);
         int argb2 = alpha << 24 | rgb2 & 16777215;
         buffer.vertex(matrix, (float)((double)x2 + Math.cos(keyCodec) * (double)r), (float)((double)y1 + Math.sin(keyCodec) * (double)r), 0.0F).color(argb2);
      }

      for (int var53 = 0; var53 <= seg; var53++) {
         double keyCodec = 0.0 + (double)var53 * (Math.PI / 2) / (double)seg;
         float hue = (float)((keyCodec + (Math.PI * 2)) / (Math.PI * 2) + t) % 1.0F;
         int rgb2 = Color.HSBtoRGB(hue, 1.0F, 1.0F);
         int argb2 = alpha << 24 | rgb2 & 16777215;
         buffer.vertex(matrix, (float)((double)x2 + Math.cos(keyCodec) * (double)r), (float)((double)y2 + Math.sin(keyCodec) * (double)r), 0.0F).color(argb2);
      }

      for (int var54 = 0; var54 <= seg; var54++) {
         double keyCodec = (Math.PI / 2) + (double)var54 * (Math.PI / 2) / (double)seg;
         float hue = (float)((keyCodec + (Math.PI * 2)) / (Math.PI * 2) + t) % 1.0F;
         int rgb2 = Color.HSBtoRGB(hue, 1.0F, 1.0F);
         int argb2 = alpha << 24 | rgb2 & 16777215;
         buffer.vertex(matrix, (float)((double)x1 + Math.cos(keyCodec) * (double)r), (float)((double)y2 + Math.sin(keyCodec) * (double)r), 0.0F).color(argb2);
      }

      for (int var55 = 0; var55 <= seg; var55++) {
         double keyCodec = Math.PI + (double)var55 * (Math.PI / 2) / (double)seg;
         float hue = (float)((keyCodec + (Math.PI * 2)) / (Math.PI * 2) + t) % 1.0F;
         int rgb2 = Color.HSBtoRGB(hue, 1.0F, 1.0F);
         int argb2 = alpha << 24 | rgb2 & 16777215;
         buffer.vertex(matrix, (float)((double)x1 + Math.cos(keyCodec) * (double)r), (float)((double)y1 + Math.sin(keyCodec) * (double)r), 0.0F).color(argb2);
      }

      BufferRenderer.drawWithGlobalProgram(buffer.end());
      BufferBuilder inner = Tessellator.getInstance().begin(DrawMode.DEBUG_LINE_STRIP, VertexFormats.POSITION_COLOR);
      float rIn = Math.max(0.5F, r - 1.2F);

      for (int i = 0; i <= seg; i++) {
         double a2 = (-Math.PI / 2) + (double)i * (Math.PI / 2) / (double)seg;
         float hue2 = (float)((a2 + (Math.PI * 2)) / (Math.PI * 2) + t) % 1.0F;
         int rgb = Color.HSBtoRGB(hue2, 1.0F, 1.0F);
         int argb = alpha << 24 | rgb & 16777215;
         inner.vertex(matrix, (float)((double)x2 + Math.cos(a2) * (double)rIn), (float)((double)y1 + Math.sin(a2) * (double)rIn), 0.0F).color(argb);
      }

      for (int var38 = 0; var38 <= seg; var38++) {
         double a3 = 0.0 + (double)var38 * (Math.PI / 2) / (double)seg;
         float hue3 = (float)((a3 + (Math.PI * 2)) / (Math.PI * 2) + t) % 1.0F;
         int rgb = Color.HSBtoRGB(hue3, 1.0F, 1.0F);
         int argb = alpha << 24 | rgb & 16777215;
         inner.vertex(matrix, (float)((double)x2 + Math.cos(a3) * (double)rIn), (float)((double)y2 + Math.sin(a3) * (double)rIn), 0.0F).color(argb);
      }

      for (int var39 = 0; var39 <= seg; var39++) {
         double a4 = (Math.PI / 2) + (double)var39 * (Math.PI / 2) / (double)seg;
         float hue4 = (float)((a4 + (Math.PI * 2)) / (Math.PI * 2) + t) % 1.0F;
         int rgb = Color.HSBtoRGB(hue4, 1.0F, 1.0F);
         int argb = alpha << 24 | rgb & 16777215;
         inner.vertex(matrix, (float)((double)x1 + Math.cos(a4) * (double)rIn), (float)((double)y2 + Math.sin(a4) * (double)rIn), 0.0F).color(argb);
      }

      for (int var40 = 0; var40 <= seg; var40++) {
         double a5 = Math.PI + (double)var40 * (Math.PI / 2) / (double)seg;
         float hue5 = (float)((a5 + (Math.PI * 2)) / (Math.PI * 2) + t) % 1.0F;
         int rgb = Color.HSBtoRGB(hue5, 1.0F, 1.0F);
         int argb = alpha << 24 | rgb & 16777215;
         inner.vertex(matrix, (float)((double)x1 + Math.cos(a5) * (double)rIn), (float)((double)y1 + Math.sin(a5) * (double)rIn), 0.0F).color(argb);
      }

      BufferRenderer.drawWithGlobalProgram(inner.end());
      RenderSystem.disableBlend();
   }

   public static void drawRoundedRect(MatrixStack matrices, float x, float y, float width, float height, float radius, Color c) {
      drawRoundedRect(matrices, x, y, width, height, radius, c.getRGB());
   }

   public static void drawRoundedRect(MatrixStack matrices, float x, float y, float width, float height, float radius, int color) {
      if (radius <= 0.0F) {
         drawRect(matrices, x, y, width, height, color);
      } else {
         float r = Math.min(radius, Math.min(width, height) / 2.0F);
         drawRect(matrices, x + r, y, width - 2.0F * r, height, color);
         drawRect(matrices, x, y + r, r, height - 2.0F * r, color);
         drawRect(matrices, x + width - r, y + r, r, height - 2.0F * r, color);
         withRoundedAntiAlias(r, () -> {
            Matrix4f matrixx = matrices.peek().getPositionMatrix();
            RenderSystem.enableBlend();
            RenderSystem.disableCull();
            RenderSystem.setShader(GameRenderer::getPositionColorProgram);
            int seg = getRoundedSegmentCount(r);
            float cx = x + r;
            float cy = y + r;
            BufferBuilder buffer = Tessellator.getInstance().begin(DrawMode.TRIANGLE_FAN, VertexFormats.POSITION_COLOR);
            buffer.vertex(matrixx, cx, cy, 0.0F).color(color);

            for (int i = 0; i <= seg; i++) {
               double angle = Math.PI + (double)i * (Math.PI / 2) / (double)seg;
               buffer.vertex(matrixx, (float)((double)cx + Math.cos(angle) * (double)r), (float)((double)cy + Math.sin(angle) * (double)r), 0.0F).color(color);
            }

            BufferRenderer.drawWithGlobalProgram(buffer.end());
            cx = x + width - r;
            cy = y + r;
            buffer = Tessellator.getInstance().begin(DrawMode.TRIANGLE_FAN, VertexFormats.POSITION_COLOR);
            buffer.vertex(matrixx, cx, cy, 0.0F).color(color);

            for (int i = 0; i <= seg; i++) {
               double angle = (Math.PI * 3.0 / 2.0) + (double)i * (Math.PI / 2) / (double)seg;
               buffer.vertex(matrixx, (float)((double)cx + Math.cos(angle) * (double)r), (float)((double)cy + Math.sin(angle) * (double)r), 0.0F).color(color);
            }

            BufferRenderer.drawWithGlobalProgram(buffer.end());
            cx = x + width - r;
            cy = y + height - r;
            buffer = Tessellator.getInstance().begin(DrawMode.TRIANGLE_FAN, VertexFormats.POSITION_COLOR);
            buffer.vertex(matrixx, cx, cy, 0.0F).color(color);

            for (int i = 0; i <= seg; i++) {
               double angle = (double)i * (Math.PI / 2) / (double)seg;
               buffer.vertex(matrixx, (float)((double)cx + Math.cos(angle) * (double)r), (float)((double)cy + Math.sin(angle) * (double)r), 0.0F).color(color);
            }

            BufferRenderer.drawWithGlobalProgram(buffer.end());
            cx = x + r;
            cy = y + height - r;
            buffer = Tessellator.getInstance().begin(DrawMode.TRIANGLE_FAN, VertexFormats.POSITION_COLOR);
            buffer.vertex(matrixx, cx, cy, 0.0F).color(color);

            for (int i = 0; i <= seg; i++) {
               double angle = (Math.PI / 2) + (double)i * (Math.PI / 2) / (double)seg;
               buffer.vertex(matrixx, (float)((double)cx + Math.cos(angle) * (double)r), (float)((double)cy + Math.sin(angle) * (double)r), 0.0F).color(color);
            }

            BufferRenderer.drawWithGlobalProgram(buffer.end());
            RenderSystem.enableCull();
            RenderSystem.disableBlend();
         });
         Matrix4f matrix = matrices.peek().getPositionMatrix();
         RenderSystem.enableBlend();
         RenderSystem.disableCull();
         RenderSystem.setShader(GameRenderer::getPositionColorProgram);
         drawRoundedOuterAA(matrix, x, y, width, height, r, color, getRoundedSegmentCount(r), true, true, true, true, true, true, true, true);
         RenderSystem.enableCull();
         RenderSystem.disableBlend();
      }
   }

   public static void drawTopRoundedRect(MatrixStack matrices, float x, float y, float width, float height, float radius, int color) {
      if (radius <= 0.0F) {
         drawRect(matrices, x, y, width, height, color);
      } else {
         float r = Math.min(radius, Math.min(width, height) / 2.0F);
         drawRect(matrices, x + r, y, width - 2.0F * r, height, color);
         drawRect(matrices, x, y + r, r, height - r, color);
         drawRect(matrices, x + width - r, y + r, r, height - r, color);
         Matrix4f matrix = matrices.peek().getPositionMatrix();
         RenderSystem.enableBlend();
         RenderSystem.disableCull();
         RenderSystem.setShader(GameRenderer::getPositionColorProgram);
         int seg = getRoundedSegmentCount(r);
         drawCornerFill(matrix, x + r, y + r, r, color, seg, Math.PI, Math.PI * 3.0 / 2.0);
         drawCornerFill(matrix, x + width - r, y + r, r, color, seg, Math.PI * 3.0 / 2.0, Math.PI * 2);
         drawRoundedOuterAA(matrix, x, y, width, height, r, color, seg, true, true, false, true, true, true, false, false);
         RenderSystem.enableCull();
         RenderSystem.disableBlend();
      }
   }

   public static void drawBottomRoundedRect(MatrixStack matrices, float x, float y, float width, float height, float radius, int color) {
      if (radius <= 0.0F) {
         drawRect(matrices, x, y, width, height, color);
      } else {
         float r = Math.min(radius, Math.min(width, height) / 2.0F);
         drawRect(matrices, x + r, y, width - 2.0F * r, height, color);
         drawRect(matrices, x, y, r, height - r, color);
         drawRect(matrices, x + width - r, y, r, height - r, color);
         Matrix4f matrix = matrices.peek().getPositionMatrix();
         RenderSystem.enableBlend();
         RenderSystem.disableCull();
         RenderSystem.setShader(GameRenderer::getPositionColorProgram);
         int seg = getRoundedSegmentCount(r);
         drawCornerFill(matrix, x + width - r, y + height - r, r, color, seg, 0.0, Math.PI / 2);
         drawCornerFill(matrix, x + r, y + height - r, r, color, seg, Math.PI / 2, Math.PI);
         drawRoundedOuterAA(matrix, x, y, width, height, r, color, seg, false, true, true, true, false, false, true, true);
         RenderSystem.enableCull();
         RenderSystem.disableBlend();
      }
   }

   public static void drawDropShadow(MatrixStack matrices, float x, float y, float width, float height, float radius) {
   }

   public static void drawStringWithGlow(
      DrawContext context, TextRenderer textRenderer, String text, float x, float y, int textColor, int glowColor, float intensity, boolean shadow
   ) {
      int layers = 3;

      for (int i = layers; i > 0; i--) {
         float offset = (float)i * intensity * 0.8F;
         int baseAlpha = glowColor >> 24 & 0xFF;
         int alpha = (int)(200.0F * (1.0F - (float)i / (float)(layers + 1)) * 0.8F);
         if (alpha > 10) {
            int currentGlowColor = alpha << 24 | glowColor & 16777215;

            for (int j = 0; j < 8; j++) {
               double angle = (Math.PI / 4) * (double)j;
               int gx = (int)((double)x + Math.cos(angle) * (double)offset);
               int gy = (int)((double)y + Math.sin(angle) * (double)offset);
               context.drawText(textRenderer, text, gx, gy, currentGlowColor, false);
            }
         }
      }

      context.drawText(textRenderer, text, (int)x, (int)y, textColor, shadow);
   }

   public static void drawGlowEffect(MatrixStack matrices, float x, float y, float width, float height, int color, float intensity) {
      drawGlowEffect(matrices, x, y, width, height, color, intensity, 4);
   }

   public static void drawGlowEffect(MatrixStack matrices, float x, float y, float width, float height, int color, float intensity, int layers) {
      Matrix4f matrix = matrices.peek().getPositionMatrix();
      RenderSystem.enableBlend();
      RenderSystem.setShader(GameRenderer::getPositionColorProgram);
      RenderSystem.disableCull();
      int baseAlpha = color >> 24 & 0xFF;
      int red = color >> 16 & 0xFF;
      int green = color >> 8 & 0xFF;
      int blue = color & 0xFF;

      for (int i = layers; i >= 0; i--) {
         float expand = (float)i * intensity;
         int alpha = (int)((float)baseAlpha * (1.0F - (float)i / (float)(layers + 1)) * 0.3F);
         if (alpha > 0) {
            int glowColor = alpha << 24 | red << 16 | green << 8 | blue;
            BufferBuilder bufferBuilder = Tessellator.getInstance().begin(DrawMode.QUADS, VertexFormats.POSITION_COLOR);
            float x1 = x - expand;
            float y1 = y - expand;
            float x2 = x + width + expand;
            float y2 = y + height + expand;
            bufferBuilder.vertex(matrix, x1, y2, 0.0F).color(glowColor);
            bufferBuilder.vertex(matrix, x2, y2, 0.0F).color(glowColor);
            bufferBuilder.vertex(matrix, x2, y1, 0.0F).color(glowColor);
            bufferBuilder.vertex(matrix, x1, y1, 0.0F).color(glowColor);
            BufferRenderer.drawWithGlobalProgram(bufferBuilder.end());
         }
      }

      RenderSystem.enableCull();
      RenderSystem.disableBlend();
   }

   public static void drawInnerGlow(MatrixStack matrices, float x, float y, float width, float height, int color, float intensity) {
      Matrix4f matrix = matrices.peek().getPositionMatrix();
      RenderSystem.enableBlend();
      RenderSystem.setShader(GameRenderer::getPositionColorProgram);
      RenderSystem.disableCull();
      int baseAlpha = color >> 24 & 0xFF;
      int red = color >> 16 & 0xFF;
      int green = color >> 8 & 0xFF;
      int blue = color & 0xFF;

      for (int i = 3; i >= 0; i--) {
         float shrink = (float)i * intensity;
         int alpha = (int)((float)baseAlpha * (1.0F - (float)i / 4.0F) * 0.5F);
         if (alpha > 0) {
            int glowColor = alpha << 24 | red << 16 | green << 8 | blue;
            BufferBuilder bufferBuilder = Tessellator.getInstance().begin(DrawMode.QUADS, VertexFormats.POSITION_COLOR);
            float x1 = x + shrink;
            float y1 = y + shrink;
            float x2 = x + width - shrink;
            float y2 = y + height - shrink;
            bufferBuilder.vertex(matrix, x1, y2, 0.0F).color(glowColor);
            bufferBuilder.vertex(matrix, x2, y2, 0.0F).color(glowColor);
            bufferBuilder.vertex(matrix, x2, y1, 0.0F).color(glowColor);
            bufferBuilder.vertex(matrix, x1, y1, 0.0F).color(glowColor);
            BufferRenderer.drawWithGlobalProgram(bufferBuilder.end());
         }
      }

      RenderSystem.enableCull();
      RenderSystem.disableBlend();
   }

   public static void drawBorderGlow(MatrixStack matrices, float x, float y, float width, float height, int color, float thickness) {
      Matrix4f matrix = matrices.peek().getPositionMatrix();
      RenderSystem.enableBlend();
      RenderSystem.setShader(GameRenderer::getPositionColorProgram);
      int baseAlpha = color >> 24 & 0xFF;
      int red = color >> 16 & 0xFF;
      int green = color >> 8 & 0xFF;
      int blue = color & 0xFF;

      for (int i = 3; i >= 0; i--) {
         float expand = (float)i * 0.5F;
         int alpha = (int)((float)baseAlpha * (1.0F - (float)i / 4.0F) * 0.6F);
         if (alpha > 0) {
            int glowColor = alpha << 24 | red << 16 | green << 8 | blue;
            BufferBuilder bufferBuilder = Tessellator.getInstance().begin(DrawMode.DEBUG_LINE_STRIP, VertexFormats.POSITION_COLOR);
            float x1 = x - expand;
            float y1 = y - expand;
            float x2 = x + width + expand;
            float y2 = y + height + expand;
            bufferBuilder.vertex(matrix, x1, y2, 0.0F).color(glowColor);
            bufferBuilder.vertex(matrix, x2, y2, 0.0F).color(glowColor);
            bufferBuilder.vertex(matrix, x2, y1, 0.0F).color(glowColor);
            bufferBuilder.vertex(matrix, x1, y1, 0.0F).color(glowColor);
            bufferBuilder.vertex(matrix, x1, y2, 0.0F).color(glowColor);
            BufferRenderer.drawWithGlobalProgram(bufferBuilder.end());
         }
      }

      RenderSystem.disableBlend();
   }

   public static void drawRoundedOutlineGlow(
      MatrixStack matrices, float x, float y, float width, float height, float radius, int color, float intensity, int layers
   ) {
      float baseThickness = Math.max(0.75F, intensity * 0.35F);
      int baseAlpha = color >> 24 & 0xFF;
      int rgb = color & 16777215;

      for (int i = layers; i >= 1; i--) {
         float progress = (float)i / (float)layers;
         float expand = progress * intensity * 1.25F;
         int alpha = (int)((float)baseAlpha * (1.0F - progress * 0.72F) * 0.55F);
         if (alpha > 2) {
            int glowColor = alpha << 24 | rgb;
            float thickness = baseThickness + progress * 0.9F;
            drawRoundedBorder(matrices, x - expand, y - expand, width + expand * 2.0F, height + expand * 2.0F, radius + expand, glowColor, thickness);
         }
      }
   }

   public static void drawGradientRoundedOutlineGlow(
      MatrixStack matrices, float x, float y, float width, float height, float radius, int color1, int color2, float intensity, int layers
   ) {
      float baseThickness = Math.max(0.75F, intensity * 0.35F);
      int baseAlpha1 = color1 >> 24 & 0xFF;
      int baseAlpha2 = color2 >> 24 & 0xFF;
      int rgb1 = color1 & 16777215;
      int rgb2 = color2 & 16777215;

      for (int i = layers; i >= 1; i--) {
         float progress = (float)i / (float)layers;
         float expand = progress * intensity * 1.25F;
         int alpha1 = (int)((float)baseAlpha1 * (1.0F - progress * 0.72F) * 0.55F);
         int alpha2 = (int)((float)baseAlpha2 * (1.0F - progress * 0.72F) * 0.55F);
         if (alpha1 > 2 || alpha2 > 2) {
            int glowColor1 = alpha1 << 24 | rgb1;
            int glowColor2 = alpha2 << 24 | rgb2;
            float thickness = baseThickness + progress * 0.9F;
            drawGradientRoundedBorder(
               matrices, x - expand, y - expand, width + expand * 2.0F, height + expand * 2.0F, radius + expand, glowColor1, glowColor2, thickness
            );
         }
      }
   }

   public static void drawBorder(MatrixStack matrices, float x, float y, float width, float height, int color, float thickness) {
      Matrix4f matrix = matrices.peek().getPositionMatrix();
      RenderSystem.enableBlend();
      RenderSystem.setShader(GameRenderer::getPositionColorProgram);
      BufferBuilder bufferBuilder = Tessellator.getInstance().begin(DrawMode.QUADS, VertexFormats.POSITION_COLOR);
      float x2 = x + width;
      float y2 = y + height;
      bufferBuilder.vertex(matrix, x, y + thickness, 0.0F).color(color);
      bufferBuilder.vertex(matrix, x2, y + thickness, 0.0F).color(color);
      bufferBuilder.vertex(matrix, x2, y, 0.0F).color(color);
      bufferBuilder.vertex(matrix, x, y, 0.0F).color(color);
      bufferBuilder.vertex(matrix, x, y2, 0.0F).color(color);
      bufferBuilder.vertex(matrix, x2, y2, 0.0F).color(color);
      bufferBuilder.vertex(matrix, x2, y2 - thickness, 0.0F).color(color);
      bufferBuilder.vertex(matrix, x, y2 - thickness, 0.0F).color(color);
      bufferBuilder.vertex(matrix, x, y2 - thickness, 0.0F).color(color);
      bufferBuilder.vertex(matrix, x + thickness, y2 - thickness, 0.0F).color(color);
      bufferBuilder.vertex(matrix, x + thickness, y + thickness, 0.0F).color(color);
      bufferBuilder.vertex(matrix, x, y + thickness, 0.0F).color(color);
      bufferBuilder.vertex(matrix, x2 - thickness, y2 - thickness, 0.0F).color(color);
      bufferBuilder.vertex(matrix, x2, y2 - thickness, 0.0F).color(color);
      bufferBuilder.vertex(matrix, x2, y + thickness, 0.0F).color(color);
      bufferBuilder.vertex(matrix, x2 - thickness, y + thickness, 0.0F).color(color);
      BufferRenderer.drawWithGlobalProgram(bufferBuilder.end());
      RenderSystem.disableBlend();
   }

   public static void drawGradientBorder(MatrixStack matrices, float x, float y, float width, float height, int color1, int color2, float thickness) {
      Matrix4f matrix = matrices.peek().getPositionMatrix();
      RenderSystem.enableBlend();
      RenderSystem.setShader(GameRenderer::getPositionColorProgram);
      float x2 = x + width;
      float y2 = y + height;
      BufferBuilder bufferBuilder = Tessellator.getInstance().begin(DrawMode.QUADS, VertexFormats.POSITION_COLOR);
      bufferBuilder.vertex(matrix, x, y + thickness, 0.0F).color(color1);
      bufferBuilder.vertex(matrix, x2, y + thickness, 0.0F).color(color2);
      bufferBuilder.vertex(matrix, x2, y, 0.0F).color(color2);
      bufferBuilder.vertex(matrix, x, y, 0.0F).color(color1);
      BufferRenderer.drawWithGlobalProgram(bufferBuilder.end());
      bufferBuilder = Tessellator.getInstance().begin(DrawMode.QUADS, VertexFormats.POSITION_COLOR);
      bufferBuilder.vertex(matrix, x, y2, 0.0F).color(color1);
      bufferBuilder.vertex(matrix, x2, y2, 0.0F).color(color2);
      bufferBuilder.vertex(matrix, x2, y2 - thickness, 0.0F).color(color2);
      bufferBuilder.vertex(matrix, x, y2 - thickness, 0.0F).color(color1);
      BufferRenderer.drawWithGlobalProgram(bufferBuilder.end());
      bufferBuilder = Tessellator.getInstance().begin(DrawMode.QUADS, VertexFormats.POSITION_COLOR);
      bufferBuilder.vertex(matrix, x, y2 - thickness, 0.0F).color(color2);
      bufferBuilder.vertex(matrix, x + thickness, y2 - thickness, 0.0F).color(color2);
      bufferBuilder.vertex(matrix, x + thickness, y + thickness, 0.0F).color(color1);
      bufferBuilder.vertex(matrix, x, y + thickness, 0.0F).color(color1);
      BufferRenderer.drawWithGlobalProgram(bufferBuilder.end());
      bufferBuilder = Tessellator.getInstance().begin(DrawMode.QUADS, VertexFormats.POSITION_COLOR);
      bufferBuilder.vertex(matrix, x2 - thickness, y2 - thickness, 0.0F).color(color2);
      bufferBuilder.vertex(matrix, x2, y2 - thickness, 0.0F).color(color2);
      bufferBuilder.vertex(matrix, x2, y + thickness, 0.0F).color(color1);
      bufferBuilder.vertex(matrix, x2 - thickness, y + thickness, 0.0F).color(color1);
      BufferRenderer.drawWithGlobalProgram(bufferBuilder.end());
      RenderSystem.disableBlend();
   }

   public static void drawRoundedBorder(MatrixStack matrices, float x, float y, float width, float height, float radius, int color, float thickness) {
      if (radius <= 0.0F) {
         drawBorder(matrices, x, y, width, height, color, thickness);
      } else {
         Matrix4f matrix = matrices.peek().getPositionMatrix();
         RenderSystem.enableBlend();
         RenderSystem.setShader(GameRenderer::getPositionColorProgram);
         RenderSystem.disableCull();
         float r = Math.min(radius, Math.min(width, height) / 2.0F);
         float rInner = Math.max(0.0F, r - thickness);
         int seg = 64;
         if (r > 0.0F) {
            BufferBuilder buf = Tessellator.getInstance().begin(DrawMode.QUADS, VertexFormats.POSITION_COLOR);
            buf.vertex(matrix, x + r, y + thickness, 0.0F).color(color);
            buf.vertex(matrix, x + width - r, y + thickness, 0.0F).color(color);
            buf.vertex(matrix, x + width - r, y, 0.0F).color(color);
            buf.vertex(matrix, x + r, y, 0.0F).color(color);
            BufferRenderer.drawWithGlobalProgram(buf.end());
         }

         if (r > 0.0F) {
            BufferBuilder buf = Tessellator.getInstance().begin(DrawMode.QUADS, VertexFormats.POSITION_COLOR);
            buf.vertex(matrix, x + r, y + height, 0.0F).color(color);
            buf.vertex(matrix, x + width - r, y + height, 0.0F).color(color);
            buf.vertex(matrix, x + width - r, y + height - thickness, 0.0F).color(color);
            buf.vertex(matrix, x + r, y + height - thickness, 0.0F).color(color);
            BufferRenderer.drawWithGlobalProgram(buf.end());
         }

         BufferBuilder bufLeft = Tessellator.getInstance().begin(DrawMode.QUADS, VertexFormats.POSITION_COLOR);
         bufLeft.vertex(matrix, x, y + height - r, 0.0F).color(color);
         bufLeft.vertex(matrix, x + thickness, y + height - r, 0.0F).color(color);
         bufLeft.vertex(matrix, x + thickness, y + r, 0.0F).color(color);
         bufLeft.vertex(matrix, x, y + r, 0.0F).color(color);
         BufferRenderer.drawWithGlobalProgram(bufLeft.end());
         BufferBuilder bufRight = Tessellator.getInstance().begin(DrawMode.QUADS, VertexFormats.POSITION_COLOR);
         bufRight.vertex(matrix, x + width - thickness, y + height - r, 0.0F).color(color);
         bufRight.vertex(matrix, x + width, y + height - r, 0.0F).color(color);
         bufRight.vertex(matrix, x + width, y + r, 0.0F).color(color);
         bufRight.vertex(matrix, x + width - thickness, y + r, 0.0F).color(color);
         BufferRenderer.drawWithGlobalProgram(bufRight.end());
         drawCornerBorder(matrix, x + r, y + r, r, rInner, color, seg, Math.PI, (float) (Math.PI * 3.0 / 2.0));
         drawCornerBorder(matrix, x + width - r, y + r, r, rInner, color, seg, (float) (Math.PI * 3.0 / 2.0), 6.283185F);
         drawCornerBorder(matrix, x + width - r, y + height - r, r, rInner, color, seg, 0.0, 1.570796F);
         drawCornerBorder(matrix, x + r, y + height - r, r, rInner, color, seg, 1.570796F, 3.141593F);
         RenderSystem.enableCull();
         RenderSystem.disableBlend();
      }
   }

   private static void drawCornerBorder(Matrix4f matrix, float cx, float cy, float r, float rInner, int color, int segments, double startAngle, double endAngle) {
      withRoundedAntiAlias(r, () -> {
         double sweep = endAngle - startAngle;
         BufferBuilder buf = Tessellator.getInstance().begin(DrawMode.TRIANGLE_STRIP, VertexFormats.POSITION_COLOR);

         for (int i = 0; i <= segments; i++) {
            double angle = startAngle + (double)i * sweep / (double)segments;
            float cos = (float)Math.cos(angle);
            float sin = (float)Math.sin(angle);
            float ox = cx + cos * r;
            float oy = cy + sin * r;
            float ix = cx + cos * rInner;
            float iy = cy + sin * rInner;
            buf.vertex(matrix, ox, oy, 0.0F).color(color);
            buf.vertex(matrix, ix, iy, 0.0F).color(color);
         }

         BufferRenderer.drawWithGlobalProgram(buf.end());
      });
   }

   public static void drawGradientRoundedBorder(
      MatrixStack matrices, float x, float y, float width, float height, float radius, int color1, int color2, float thickness
   ) {
      if (radius <= 0.0F) {
         drawGradientBorder(matrices, x, y, width, height, color1, color2, thickness);
      } else {
         Matrix4f matrix = matrices.peek().getPositionMatrix();
         RenderSystem.enableBlend();
         RenderSystem.setShader(GameRenderer::getPositionColorProgram);
         RenderSystem.disableCull();
         float r = Math.min(radius, Math.min(width, height) / 2.0F);
         float rInner = Math.max(0.0F, r - thickness);
         int seg = 64;
         BufferBuilder bufTop = Tessellator.getInstance().begin(DrawMode.QUADS, VertexFormats.POSITION_COLOR);
         bufTop.vertex(matrix, x + r, y + thickness, 0.0F).color(color1);
         bufTop.vertex(matrix, x + width - r, y + thickness, 0.0F).color(color2);
         bufTop.vertex(matrix, x + width - r, y, 0.0F).color(color2);
         bufTop.vertex(matrix, x + r, y, 0.0F).color(color1);
         BufferRenderer.drawWithGlobalProgram(bufTop.end());
         BufferBuilder bufBottom = Tessellator.getInstance().begin(DrawMode.QUADS, VertexFormats.POSITION_COLOR);
         bufBottom.vertex(matrix, x + r, y + height, 0.0F).color(color1);
         bufBottom.vertex(matrix, x + width - r, y + height, 0.0F).color(color2);
         bufBottom.vertex(matrix, x + width - r, y + height - thickness, 0.0F).color(color2);
         bufBottom.vertex(matrix, x + r, y + height - thickness, 0.0F).color(color1);
         BufferRenderer.drawWithGlobalProgram(bufBottom.end());
         BufferBuilder bufLeft = Tessellator.getInstance().begin(DrawMode.QUADS, VertexFormats.POSITION_COLOR);
         bufLeft.vertex(matrix, x, y + height - r, 0.0F).color(color2);
         bufLeft.vertex(matrix, x + thickness, y + height - r, 0.0F).color(color2);
         bufLeft.vertex(matrix, x + thickness, y + r, 0.0F).color(color1);
         bufLeft.vertex(matrix, x, y + r, 0.0F).color(color1);
         BufferRenderer.drawWithGlobalProgram(bufLeft.end());
         BufferBuilder bufRight = Tessellator.getInstance().begin(DrawMode.QUADS, VertexFormats.POSITION_COLOR);
         bufRight.vertex(matrix, x + width - thickness, y + height - r, 0.0F).color(color2);
         bufRight.vertex(matrix, x + width, y + height - r, 0.0F).color(color2);
         bufRight.vertex(matrix, x + width, y + r, 0.0F).color(color1);
         bufRight.vertex(matrix, x + width - thickness, y + r, 0.0F).color(color1);
         BufferRenderer.drawWithGlobalProgram(bufRight.end());
         int topMidColor = blendColors(color1, color2, 0.5F);
         int bottomMidColor = blendColors(color1, color2, 0.5F);
         drawCornerBorderGradient(matrix, x + r, y + r, r, rInner, color1, topMidColor, seg, Math.PI, (float) (Math.PI * 3.0 / 2.0));
         drawCornerBorderGradient(matrix, x + width - r, y + r, r, rInner, topMidColor, color2, seg, (float) (Math.PI * 3.0 / 2.0), 6.283185F);
         drawCornerBorderGradient(matrix, x + width - r, y + height - r, r, rInner, bottomMidColor, color2, seg, 0.0, 1.570796F);
         drawCornerBorderGradient(matrix, x + r, y + height - r, r, rInner, color1, bottomMidColor, seg, 1.570796F, 3.141593F);
         RenderSystem.enableCull();
         RenderSystem.disableBlend();
      }
   }

   private static void drawCornerBorderGradient(
      Matrix4f matrix, float cx, float cy, float r, float rInner, int colorStart, int colorEnd, int segments, double startAngle, double endAngle
   ) {
      withRoundedAntiAlias(r, () -> {
         double sweep = endAngle - startAngle;
         BufferBuilder buf = Tessellator.getInstance().begin(DrawMode.TRIANGLE_STRIP, VertexFormats.POSITION_COLOR);

         for (int i = 0; i <= segments; i++) {
            double angle = startAngle + (double)i * sweep / (double)segments;
            float t = (float)i / (float)segments;
            int c = blendColors(colorStart, colorEnd, t);
            float cos = (float)Math.cos(angle);
            float sin = (float)Math.sin(angle);
            float ox = cx + cos * r;
            float oy = cy + sin * r;
            float ix = cx + cos * rInner;
            float iy = cy + sin * rInner;
            buf.vertex(matrix, ox, oy, 0.0F).color(c);
            buf.vertex(matrix, ix, iy, 0.0F).color(c);
         }

         BufferRenderer.drawWithGlobalProgram(buf.end());
      });
   }

   private static void withRoundedAntiAlias(float radius, Runnable action) {
      if (!(radius <= 0.0F) && Alien.ANTI_ALIAS != null) {
         Alien.ANTI_ALIAS.withAA(action);
      } else {
         action.run();
      }
   }

   private static int getRoundedSegmentCount(float radius) {
      return Math.max(48, Math.min(96, (int)(radius * 10.0F)));
   }

   private static void drawCornerFill(Matrix4f matrix, float cx, float cy, float radius, int color, int segments, double startAngle, double endAngle) {
      BufferBuilder buffer = Tessellator.getInstance().begin(DrawMode.TRIANGLE_FAN, VertexFormats.POSITION_COLOR);
      buffer.vertex(matrix, cx, cy, 0.0F).color(color);
      double sweep = endAngle - startAngle;

      for (int i = 0; i <= segments; i++) {
         double angle = startAngle + (double)i * sweep / (double)segments;
         buffer.vertex(matrix, (float)((double)cx + Math.cos(angle) * (double)radius), (float)((double)cy + Math.sin(angle) * (double)radius), 0.0F)
            .color(color);
      }

      BufferRenderer.drawWithGlobalProgram(buffer.end());
   }

   private static void drawRoundedOuterAA(
      Matrix4f matrix,
      float x,
      float y,
      float width,
      float height,
      float radius,
      int color,
      int segments,
      boolean fadeTop,
      boolean fadeRight,
      boolean fadeBottom,
      boolean fadeLeft,
      boolean topLeft,
      boolean topRight,
      boolean bottomRight,
      boolean bottomLeft
   ) {
      float topStart = x + (topLeft ? radius : 0.0F);
      float topEnd = x + width - (topRight ? radius : 0.0F);
      float bottomStart = x + (bottomLeft ? radius : 0.0F);
      float bottomEnd = x + width - (bottomRight ? radius : 0.0F);
      float leftStart = y + (topLeft ? radius : 0.0F);
      float leftEnd = y + height - (bottomLeft ? radius : 0.0F);
      float rightStart = y + (topRight ? radius : 0.0F);
      float rightEnd = y + height - (bottomRight ? radius : 0.0F);
      if (fadeTop) {
         drawHorizontalFade(matrix, topStart, y, topEnd - topStart, 1.0F, color, true);
      }

      if (fadeBottom) {
         drawHorizontalFade(matrix, bottomStart, y + height, bottomEnd - bottomStart, 1.0F, color, false);
      }

      if (fadeLeft) {
         drawVerticalFade(matrix, x, leftStart, leftEnd - leftStart, 1.0F, color, true);
      }

      if (fadeRight) {
         drawVerticalFade(matrix, x + width, rightStart, rightEnd - rightStart, 1.0F, color, false);
      }

      if (topLeft) {
         drawCornerOuterFade(matrix, x + radius, y + radius, radius, color, segments, Math.PI, Math.PI * 3.0 / 2.0);
      }

      if (topRight) {
         drawCornerOuterFade(matrix, x + width - radius, y + radius, radius, color, segments, Math.PI * 3.0 / 2.0, Math.PI * 2);
      }

      if (bottomRight) {
         drawCornerOuterFade(matrix, x + width - radius, y + height - radius, radius, color, segments, 0.0, Math.PI / 2);
      }

      if (bottomLeft) {
         drawCornerOuterFade(matrix, x + radius, y + height - radius, radius, color, segments, Math.PI / 2, Math.PI);
      }
   }

   private static void drawRoundedOuterAAGradient(
      Matrix4f matrix,
      float x,
      float y,
      float width,
      float height,
      float radius,
      int color1,
      int color2,
      int segments,
      boolean fadeTop,
      boolean fadeRight,
      boolean fadeBottom,
      boolean fadeLeft,
      boolean topLeft,
      boolean topRight,
      boolean bottomRight,
      boolean bottomLeft
   ) {
      float topStart = x + (topLeft ? radius : 0.0F);
      float topEnd = x + width - (topRight ? radius : 0.0F);
      float bottomStart = x + (bottomLeft ? radius : 0.0F);
      float bottomEnd = x + width - (bottomRight ? radius : 0.0F);
      float leftStart = y + (topLeft ? radius : 0.0F);
      float leftEnd = y + height - (bottomLeft ? radius : 0.0F);
      float rightStart = y + (topRight ? radius : 0.0F);
      float rightEnd = y + height - (bottomRight ? radius : 0.0F);
      int mid = blendColors(color1, color2, 0.5F);
      if (fadeTop) {
         drawHorizontalFadeGradient(matrix, topStart, y, topEnd - topStart, 1.0F, color1, color2, true);
      }

      if (fadeBottom) {
         drawHorizontalFadeGradient(matrix, bottomStart, y + height, bottomEnd - bottomStart, 1.0F, color1, color2, false);
      }

      if (fadeLeft) {
         drawVerticalFadeGradient(matrix, x, leftStart, leftEnd - leftStart, 1.0F, color1, color2, true);
      }

      if (fadeRight) {
         drawVerticalFadeGradient(matrix, x + width, rightStart, rightEnd - rightStart, 1.0F, color1, color2, false);
      }

      if (topLeft) {
         drawCornerOuterFadeGradient(matrix, x + radius, y + radius, radius, color1, mid, segments, Math.PI, Math.PI * 3.0 / 2.0);
      }

      if (topRight) {
         drawCornerOuterFadeGradient(matrix, x + width - radius, y + radius, radius, mid, color2, segments, Math.PI * 3.0 / 2.0, Math.PI * 2);
      }

      if (bottomRight) {
         drawCornerOuterFadeGradient(matrix, x + width - radius, y + height - radius, radius, mid, color2, segments, 0.0, Math.PI / 2);
      }

      if (bottomLeft) {
         drawCornerOuterFadeGradient(matrix, x + radius, y + height - radius, radius, color1, mid, segments, Math.PI / 2, Math.PI);
      }
   }

   private static void drawHorizontalFade(Matrix4f matrix, float x, float y, float width, float aaWidth, int color, boolean top) {
      if (!(width <= 0.0F) && !(aaWidth <= 0.0F)) {
         int transparent = transparent(color);
         BufferBuilder buffer = Tessellator.getInstance().begin(DrawMode.QUADS, VertexFormats.POSITION_COLOR);
         if (top) {
            buffer.vertex(matrix, x, y, 0.0F).color(color);
            buffer.vertex(matrix, x + width, y, 0.0F).color(color);
            buffer.vertex(matrix, x + width, y - aaWidth, 0.0F).color(transparent);
            buffer.vertex(matrix, x, y - aaWidth, 0.0F).color(transparent);
         } else {
            buffer.vertex(matrix, x, y + aaWidth, 0.0F).color(transparent);
            buffer.vertex(matrix, x + width, y + aaWidth, 0.0F).color(transparent);
            buffer.vertex(matrix, x + width, y, 0.0F).color(color);
            buffer.vertex(matrix, x, y, 0.0F).color(color);
         }

         BufferRenderer.drawWithGlobalProgram(buffer.end());
      }
   }

   private static void drawVerticalFade(Matrix4f matrix, float x, float y, float height, float aaWidth, int color, boolean left) {
      if (!(height <= 0.0F) && !(aaWidth <= 0.0F)) {
         int transparent = transparent(color);
         BufferBuilder buffer = Tessellator.getInstance().begin(DrawMode.QUADS, VertexFormats.POSITION_COLOR);
         if (left) {
            buffer.vertex(matrix, x, y + height, 0.0F).color(color);
            buffer.vertex(matrix, x, y, 0.0F).color(color);
            buffer.vertex(matrix, x - aaWidth, y, 0.0F).color(transparent);
            buffer.vertex(matrix, x - aaWidth, y + height, 0.0F).color(transparent);
         } else {
            buffer.vertex(matrix, x + aaWidth, y + height, 0.0F).color(transparent);
            buffer.vertex(matrix, x + aaWidth, y, 0.0F).color(transparent);
            buffer.vertex(matrix, x, y, 0.0F).color(color);
            buffer.vertex(matrix, x, y + height, 0.0F).color(color);
         }

         BufferRenderer.drawWithGlobalProgram(buffer.end());
      }
   }

   private static void drawHorizontalFadeGradient(Matrix4f matrix, float x, float y, float width, float aaWidth, int color1, int color2, boolean top) {
      if (!(width <= 0.0F) && !(aaWidth <= 0.0F)) {
         int transparent1 = transparent(color1);
         int transparent2 = transparent(color2);
         BufferBuilder buffer = Tessellator.getInstance().begin(DrawMode.QUADS, VertexFormats.POSITION_COLOR);
         if (top) {
            buffer.vertex(matrix, x, y, 0.0F).color(color1);
            buffer.vertex(matrix, x + width, y, 0.0F).color(color2);
            buffer.vertex(matrix, x + width, y - aaWidth, 0.0F).color(transparent2);
            buffer.vertex(matrix, x, y - aaWidth, 0.0F).color(transparent1);
         } else {
            buffer.vertex(matrix, x, y + aaWidth, 0.0F).color(transparent1);
            buffer.vertex(matrix, x + width, y + aaWidth, 0.0F).color(transparent2);
            buffer.vertex(matrix, x + width, y, 0.0F).color(color2);
            buffer.vertex(matrix, x, y, 0.0F).color(color1);
         }

         BufferRenderer.drawWithGlobalProgram(buffer.end());
      }
   }

   private static void drawVerticalFadeGradient(Matrix4f matrix, float x, float y, float height, float aaWidth, int color1, int color2, boolean left) {
      if (!(height <= 0.0F) && !(aaWidth <= 0.0F)) {
         int transparent1 = transparent(color1);
         int transparent2 = transparent(color2);
         BufferBuilder buffer = Tessellator.getInstance().begin(DrawMode.QUADS, VertexFormats.POSITION_COLOR);
         if (left) {
            buffer.vertex(matrix, x, y + height, 0.0F).color(color2);
            buffer.vertex(matrix, x, y, 0.0F).color(color1);
            buffer.vertex(matrix, x - aaWidth, y, 0.0F).color(transparent1);
            buffer.vertex(matrix, x - aaWidth, y + height, 0.0F).color(transparent2);
         } else {
            buffer.vertex(matrix, x + aaWidth, y + height, 0.0F).color(transparent2);
            buffer.vertex(matrix, x + aaWidth, y, 0.0F).color(transparent1);
            buffer.vertex(matrix, x, y, 0.0F).color(color1);
            buffer.vertex(matrix, x, y + height, 0.0F).color(color2);
         }

         BufferRenderer.drawWithGlobalProgram(buffer.end());
      }
   }

   private static void drawHorizontalInnerFade(Matrix4f matrix, float x, float y, float width, float aaWidth, int color, boolean downward) {
      if (!(width <= 0.0F) && !(aaWidth <= 0.0F)) {
         int transparent = transparent(color);
         BufferBuilder buffer = Tessellator.getInstance().begin(DrawMode.QUADS, VertexFormats.POSITION_COLOR);
         if (downward) {
            buffer.vertex(matrix, x, y + aaWidth, 0.0F).color(transparent);
            buffer.vertex(matrix, x + width, y + aaWidth, 0.0F).color(transparent);
            buffer.vertex(matrix, x + width, y, 0.0F).color(color);
            buffer.vertex(matrix, x, y, 0.0F).color(color);
         } else {
            buffer.vertex(matrix, x, y, 0.0F).color(color);
            buffer.vertex(matrix, x + width, y, 0.0F).color(color);
            buffer.vertex(matrix, x + width, y - aaWidth, 0.0F).color(transparent);
            buffer.vertex(matrix, x, y - aaWidth, 0.0F).color(transparent);
         }

         BufferRenderer.drawWithGlobalProgram(buffer.end());
      }
   }

   private static void drawVerticalInnerFade(Matrix4f matrix, float x, float y, float height, float aaWidth, int color, boolean toRight) {
      if (!(height <= 0.0F) && !(aaWidth <= 0.0F)) {
         int transparent = transparent(color);
         BufferBuilder buffer = Tessellator.getInstance().begin(DrawMode.QUADS, VertexFormats.POSITION_COLOR);
         if (toRight) {
            buffer.vertex(matrix, x + aaWidth, y + height, 0.0F).color(transparent);
            buffer.vertex(matrix, x + aaWidth, y, 0.0F).color(transparent);
            buffer.vertex(matrix, x, y, 0.0F).color(color);
            buffer.vertex(matrix, x, y + height, 0.0F).color(color);
         } else {
            buffer.vertex(matrix, x, y + height, 0.0F).color(color);
            buffer.vertex(matrix, x, y, 0.0F).color(color);
            buffer.vertex(matrix, x - aaWidth, y, 0.0F).color(transparent);
            buffer.vertex(matrix, x - aaWidth, y + height, 0.0F).color(transparent);
         }

         BufferRenderer.drawWithGlobalProgram(buffer.end());
      }
   }

   private static void drawHorizontalInnerFadeGradient(Matrix4f matrix, float x, float y, float width, float aaWidth, int color1, int color2, boolean downward) {
      if (!(width <= 0.0F) && !(aaWidth <= 0.0F)) {
         int transparent1 = transparent(color1);
         int transparent2 = transparent(color2);
         BufferBuilder buffer = Tessellator.getInstance().begin(DrawMode.QUADS, VertexFormats.POSITION_COLOR);
         if (downward) {
            buffer.vertex(matrix, x, y + aaWidth, 0.0F).color(transparent1);
            buffer.vertex(matrix, x + width, y + aaWidth, 0.0F).color(transparent2);
            buffer.vertex(matrix, x + width, y, 0.0F).color(color2);
            buffer.vertex(matrix, x, y, 0.0F).color(color1);
         } else {
            buffer.vertex(matrix, x, y, 0.0F).color(color1);
            buffer.vertex(matrix, x + width, y, 0.0F).color(color2);
            buffer.vertex(matrix, x + width, y - aaWidth, 0.0F).color(transparent2);
            buffer.vertex(matrix, x, y - aaWidth, 0.0F).color(transparent1);
         }

         BufferRenderer.drawWithGlobalProgram(buffer.end());
      }
   }

   private static void drawVerticalInnerFadeGradient(Matrix4f matrix, float x, float y, float height, float aaWidth, int color1, int color2, boolean toRight) {
      if (!(height <= 0.0F) && !(aaWidth <= 0.0F)) {
         int transparent1 = transparent(color1);
         int transparent2 = transparent(color2);
         BufferBuilder buffer = Tessellator.getInstance().begin(DrawMode.QUADS, VertexFormats.POSITION_COLOR);
         if (toRight) {
            buffer.vertex(matrix, x + aaWidth, y + height, 0.0F).color(transparent2);
            buffer.vertex(matrix, x + aaWidth, y, 0.0F).color(transparent1);
            buffer.vertex(matrix, x, y, 0.0F).color(color1);
            buffer.vertex(matrix, x, y + height, 0.0F).color(color2);
         } else {
            buffer.vertex(matrix, x, y + height, 0.0F).color(color2);
            buffer.vertex(matrix, x, y, 0.0F).color(color1);
            buffer.vertex(matrix, x - aaWidth, y, 0.0F).color(transparent1);
            buffer.vertex(matrix, x - aaWidth, y + height, 0.0F).color(transparent2);
         }

         BufferRenderer.drawWithGlobalProgram(buffer.end());
      }
   }

   private static void drawCornerInnerFade(Matrix4f matrix, float cx, float cy, float radius, int color, int segments, double startAngle, double endAngle) {
      if (!(radius <= 0.0F)) {
         float innerRadius = Math.max(0.0F, radius - 1.0F);
         BufferBuilder buffer = Tessellator.getInstance().begin(DrawMode.TRIANGLE_STRIP, VertexFormats.POSITION_COLOR);
         double sweep = endAngle - startAngle;
         int transparent = transparent(color);

         for (int i = 0; i <= segments; i++) {
            double angle = startAngle + (double)i * sweep / (double)segments;
            float cos = (float)Math.cos(angle);
            float sin = (float)Math.sin(angle);
            buffer.vertex(matrix, cx + cos * radius, cy + sin * radius, 0.0F).color(color);
            buffer.vertex(matrix, cx + cos * innerRadius, cy + sin * innerRadius, 0.0F).color(transparent);
         }

         BufferRenderer.drawWithGlobalProgram(buffer.end());
      }
   }

   private static void drawCornerInnerFadeGradient(
      Matrix4f matrix, float cx, float cy, float radius, int colorStart, int colorEnd, int segments, double startAngle, double endAngle
   ) {
      if (!(radius <= 0.0F)) {
         float innerRadius = Math.max(0.0F, radius - 1.0F);
         BufferBuilder buffer = Tessellator.getInstance().begin(DrawMode.TRIANGLE_STRIP, VertexFormats.POSITION_COLOR);
         double sweep = endAngle - startAngle;

         for (int i = 0; i <= segments; i++) {
            double angle = startAngle + (double)i * sweep / (double)segments;
            float cos = (float)Math.cos(angle);
            float sin = (float)Math.sin(angle);
            float t = (float)i / (float)segments;
            int edge = blendColors(colorStart, colorEnd, t);
            int center = transparent(edge);
            buffer.vertex(matrix, cx + cos * radius, cy + sin * radius, 0.0F).color(edge);
            buffer.vertex(matrix, cx + cos * innerRadius, cy + sin * innerRadius, 0.0F).color(center);
         }

         BufferRenderer.drawWithGlobalProgram(buffer.end());
      }
   }

   private static void drawCornerOuterFade(Matrix4f matrix, float cx, float cy, float radius, int color, int segments, double startAngle, double endAngle) {
      BufferBuilder buffer = Tessellator.getInstance().begin(DrawMode.TRIANGLE_STRIP, VertexFormats.POSITION_COLOR);
      double sweep = endAngle - startAngle;
      int transparent = transparent(color);
      float outerRadius = radius + 1.0F;

      for (int i = 0; i <= segments; i++) {
         double angle = startAngle + (double)i * sweep / (double)segments;
         float cos = (float)Math.cos(angle);
         float sin = (float)Math.sin(angle);
         buffer.vertex(matrix, cx + cos * outerRadius, cy + sin * outerRadius, 0.0F).color(transparent);
         buffer.vertex(matrix, cx + cos * radius, cy + sin * radius, 0.0F).color(color);
      }

      BufferRenderer.drawWithGlobalProgram(buffer.end());
   }

   private static void drawCornerOuterFadeGradient(
      Matrix4f matrix, float cx, float cy, float radius, int colorStart, int colorEnd, int segments, double startAngle, double endAngle
   ) {
      BufferBuilder buffer = Tessellator.getInstance().begin(DrawMode.TRIANGLE_STRIP, VertexFormats.POSITION_COLOR);
      double sweep = endAngle - startAngle;
      float outerRadius = radius + 1.0F;

      for (int i = 0; i <= segments; i++) {
         double angle = startAngle + (double)i * sweep / (double)segments;
         float cos = (float)Math.cos(angle);
         float sin = (float)Math.sin(angle);
         float t = (float)i / (float)segments;
         int inner = blendColors(colorStart, colorEnd, t);
         int outer = transparent(inner);
         buffer.vertex(matrix, cx + cos * outerRadius, cy + sin * outerRadius, 0.0F).color(outer);
         buffer.vertex(matrix, cx + cos * radius, cy + sin * radius, 0.0F).color(inner);
      }

      BufferRenderer.drawWithGlobalProgram(buffer.end());
   }

   private static int transparent(int color) {
      return color & 16777215;
   }

   private static int blendColors(int color1, int color2, float t) {
      int r1 = color1 >> 16 & 0xFF;
      int g1 = color1 >> 8 & 0xFF;
      int b1 = color1 & 0xFF;
      int a1 = color1 >> 24 & 0xFF;
      int r2 = color2 >> 16 & 0xFF;
      int g2 = color2 >> 8 & 0xFF;
      int b2 = color2 & 0xFF;
      int a2 = color2 >> 24 & 0xFF;
      int r = (int)((float)r1 + (float)(r2 - r1) * t);
      int g = (int)((float)g1 + (float)(g2 - g1) * t);
      int b = (int)((float)b1 + (float)(b2 - b1) * t);
      int a = (int)((float)a1 + (float)(a2 - a1) * t);
      return a << 24 | r << 16 | g << 8 | b;
   }

   public static void drawTopRoundedBorder(MatrixStack matrices, float x, float y, float width, float height, float radius, int color, float thickness) {
      if (radius <= 0.0F) {
         drawBorder(matrices, x, y, width, height, color, thickness);
      } else {
         Matrix4f matrix = matrices.peek().getPositionMatrix();
         RenderSystem.enableBlend();
         RenderSystem.setShader(GameRenderer::getPositionColorProgram);
         RenderSystem.disableCull();
         float r = Math.min(radius, Math.min(width, height) / 2.0F);
         float rInner = Math.max(0.0F, r - thickness);
         int seg = 64;
         BufferBuilder buf = Tessellator.getInstance().begin(DrawMode.QUADS, VertexFormats.POSITION_COLOR);
         buf.vertex(matrix, x + r, y + thickness, 0.0F).color(color);
         buf.vertex(matrix, x + width - r, y + thickness, 0.0F).color(color);
         buf.vertex(matrix, x + width - r, y, 0.0F).color(color);
         buf.vertex(matrix, x + r, y, 0.0F).color(color);
         BufferRenderer.drawWithGlobalProgram(buf.end());
         buf = Tessellator.getInstance().begin(DrawMode.QUADS, VertexFormats.POSITION_COLOR);
         buf.vertex(matrix, x, y + height, 0.0F).color(color);
         buf.vertex(matrix, x + width, y + height, 0.0F).color(color);
         buf.vertex(matrix, x + width, y + height - thickness, 0.0F).color(color);
         buf.vertex(matrix, x, y + height - thickness, 0.0F).color(color);
         BufferRenderer.drawWithGlobalProgram(buf.end());
         buf = Tessellator.getInstance().begin(DrawMode.QUADS, VertexFormats.POSITION_COLOR);
         buf.vertex(matrix, x, y + height, 0.0F).color(color);
         buf.vertex(matrix, x + thickness, y + height, 0.0F).color(color);
         buf.vertex(matrix, x + thickness, y + r, 0.0F).color(color);
         buf.vertex(matrix, x, y + r, 0.0F).color(color);
         BufferRenderer.drawWithGlobalProgram(buf.end());
         buf = Tessellator.getInstance().begin(DrawMode.QUADS, VertexFormats.POSITION_COLOR);
         buf.vertex(matrix, x + width - thickness, y + height, 0.0F).color(color);
         buf.vertex(matrix, x + width, y + height, 0.0F).color(color);
         buf.vertex(matrix, x + width, y + r, 0.0F).color(color);
         buf.vertex(matrix, x + width - thickness, y + r, 0.0F).color(color);
         BufferRenderer.drawWithGlobalProgram(buf.end());
         drawCornerBorder(matrix, x + r, y + r, r, rInner, color, seg, Math.PI, (float) (Math.PI * 3.0 / 2.0));
         drawCornerBorder(matrix, x + width - r, y + r, r, rInner, color, seg, (float) (Math.PI * 3.0 / 2.0), 6.283185F);
         drawRoundedOuterAA(matrix, x, y, width, height, r, color, seg, true, true, false, true, true, true, false, false);
         drawHorizontalInnerFade(matrix, x + r, y + thickness, width - 2.0F * r, 1.0F, color, true);
         drawVerticalInnerFade(matrix, x + thickness, y + r, height - r, 1.0F, color, true);
         drawVerticalInnerFade(matrix, x + width - thickness, y + r, height - r, 1.0F, color, false);
         drawCornerInnerFade(matrix, x + r, y + r, rInner, color, seg, Math.PI, (float) (Math.PI * 3.0 / 2.0));
         drawCornerInnerFade(matrix, x + width - r, y + r, rInner, color, seg, (float) (Math.PI * 3.0 / 2.0), 6.283185F);
         RenderSystem.enableCull();
         RenderSystem.disableBlend();
      }
   }

   public static void drawTopRoundedBorder(
      MatrixStack matrices, float x, float y, float width, float height, float radius, int color1, int color2, float thickness
   ) {
      if (radius <= 0.0F) {
         drawGradientBorder(matrices, x, y, width, height, color1, color2, thickness);
      } else {
         Matrix4f matrix = matrices.peek().getPositionMatrix();
         RenderSystem.enableBlend();
         RenderSystem.setShader(GameRenderer::getPositionColorProgram);
         RenderSystem.disableCull();
         float r = Math.min(radius, Math.min(width, height) / 2.0F);
         float rInner = Math.max(0.0F, r - thickness);
         int seg = 64;
         BufferBuilder buf = Tessellator.getInstance().begin(DrawMode.QUADS, VertexFormats.POSITION_COLOR);
         buf.vertex(matrix, x + r, y + thickness, 0.0F).color(color1);
         buf.vertex(matrix, x + width - r, y + thickness, 0.0F).color(color2);
         buf.vertex(matrix, x + width - r, y, 0.0F).color(color2);
         buf.vertex(matrix, x + r, y, 0.0F).color(color1);
         BufferRenderer.drawWithGlobalProgram(buf.end());
         buf = Tessellator.getInstance().begin(DrawMode.QUADS, VertexFormats.POSITION_COLOR);
         buf.vertex(matrix, x, y + height, 0.0F).color(color1);
         buf.vertex(matrix, x + width, y + height, 0.0F).color(color2);
         buf.vertex(matrix, x + width, y + height - thickness, 0.0F).color(color2);
         buf.vertex(matrix, x, y + height - thickness, 0.0F).color(color1);
         BufferRenderer.drawWithGlobalProgram(buf.end());
         buf = Tessellator.getInstance().begin(DrawMode.QUADS, VertexFormats.POSITION_COLOR);
         buf.vertex(matrix, x, y + height, 0.0F).color(color2);
         buf.vertex(matrix, x + thickness, y + height, 0.0F).color(color2);
         buf.vertex(matrix, x + thickness, y + r, 0.0F).color(color1);
         buf.vertex(matrix, x, y + r, 0.0F).color(color1);
         BufferRenderer.drawWithGlobalProgram(buf.end());
         buf = Tessellator.getInstance().begin(DrawMode.QUADS, VertexFormats.POSITION_COLOR);
         buf.vertex(matrix, x + width - thickness, y + height, 0.0F).color(color2);
         buf.vertex(matrix, x + width, y + height, 0.0F).color(color2);
         buf.vertex(matrix, x + width, y + r, 0.0F).color(color1);
         buf.vertex(matrix, x + width - thickness, y + r, 0.0F).color(color1);
         BufferRenderer.drawWithGlobalProgram(buf.end());
         int topMid = blendColors(color1, color2, 0.5F);
         drawCornerBorderGradient(matrix, x + r, y + r, r, rInner, color1, topMid, seg, Math.PI, (float) (Math.PI * 3.0 / 2.0));
         drawCornerBorderGradient(matrix, x + width - r, y + r, r, rInner, topMid, color2, seg, (float) (Math.PI * 3.0 / 2.0), 6.283185F);
         drawRoundedOuterAAGradient(matrix, x, y, width, height, r, color1, color2, seg, true, true, false, true, true, true, false, false);
         drawHorizontalInnerFadeGradient(matrix, x + r, y + thickness, width - 2.0F * r, 1.0F, color1, color2, true);
         drawVerticalInnerFadeGradient(matrix, x + thickness, y + r, height - r, 1.0F, color1, color2, true);
         drawVerticalInnerFadeGradient(matrix, x + width - thickness, y + r, height - r, 1.0F, color1, color2, false);
         drawCornerInnerFadeGradient(matrix, x + r, y + r, rInner, color1, topMid, seg, Math.PI, (float) (Math.PI * 3.0 / 2.0));
         drawCornerInnerFadeGradient(matrix, x + width - r, y + r, rInner, topMid, color2, seg, (float) (Math.PI * 3.0 / 2.0), 6.283185F);
         RenderSystem.enableCull();
         RenderSystem.disableBlend();
      }
   }

   public static void drawBottomRoundedBorder(MatrixStack matrices, float x, float y, float width, float height, float radius, int color, float thickness) {
      if (radius <= 0.0F) {
         drawBorder(matrices, x, y, width, height, color, thickness);
      } else {
         Matrix4f matrix = matrices.peek().getPositionMatrix();
         RenderSystem.enableBlend();
         RenderSystem.setShader(GameRenderer::getPositionColorProgram);
         RenderSystem.disableCull();
         float r = Math.min(radius, Math.min(width, height) / 2.0F);
         float rInner = Math.max(0.0F, r - thickness);
         int seg = 64;
         BufferBuilder buf = Tessellator.getInstance().begin(DrawMode.QUADS, VertexFormats.POSITION_COLOR);
         buf.vertex(matrix, x + r, y + height, 0.0F).color(color);
         buf.vertex(matrix, x + width - r, y + height, 0.0F).color(color);
         buf.vertex(matrix, x + width - r, y + height - thickness, 0.0F).color(color);
         buf.vertex(matrix, x + r, y + height - thickness, 0.0F).color(color);
         BufferRenderer.drawWithGlobalProgram(buf.end());
         buf = Tessellator.getInstance().begin(DrawMode.QUADS, VertexFormats.POSITION_COLOR);
         buf.vertex(matrix, x, y + height - r, 0.0F).color(color);
         buf.vertex(matrix, x + thickness, y + height - r, 0.0F).color(color);
         buf.vertex(matrix, x + thickness, y, 0.0F).color(color);
         buf.vertex(matrix, x, y, 0.0F).color(color);
         BufferRenderer.drawWithGlobalProgram(buf.end());
         buf = Tessellator.getInstance().begin(DrawMode.QUADS, VertexFormats.POSITION_COLOR);
         buf.vertex(matrix, x + width - thickness, y + height - r, 0.0F).color(color);
         buf.vertex(matrix, x + width, y + height - r, 0.0F).color(color);
         buf.vertex(matrix, x + width, y, 0.0F).color(color);
         buf.vertex(matrix, x + width - thickness, y, 0.0F).color(color);
         BufferRenderer.drawWithGlobalProgram(buf.end());
         drawCornerBorder(matrix, x + width - r, y + height - r, r, rInner, color, seg, 0.0, 1.570796F);
         drawCornerBorder(matrix, x + r, y + height - r, r, rInner, color, seg, 1.570796F, 3.141593F);
         drawRoundedOuterAA(matrix, x, y, width, height, r, color, seg, false, true, true, true, false, false, true, true);
         drawHorizontalInnerFade(matrix, x + r, y + height - thickness, width - 2.0F * r, 1.0F, color, false);
         drawVerticalInnerFade(matrix, x + thickness, y, height - r, 1.0F, color, true);
         drawVerticalInnerFade(matrix, x + width - thickness, y, height - r, 1.0F, color, false);
         drawCornerInnerFade(matrix, x + width - r, y + height - r, rInner, color, seg, 0.0, 1.570796F);
         drawCornerInnerFade(matrix, x + r, y + height - r, rInner, color, seg, 1.570796F, 3.141593F);
         RenderSystem.enableCull();
         RenderSystem.disableBlend();
      }
   }

   public static void drawBottomRoundedBorder(
      MatrixStack matrices, float x, float y, float width, float height, float radius, int color1, int color2, float thickness
   ) {
      if (radius <= 0.0F) {
         drawGradientBorder(matrices, x, y, width, height, color1, color2, thickness);
      } else {
         Matrix4f matrix = matrices.peek().getPositionMatrix();
         RenderSystem.enableBlend();
         RenderSystem.setShader(GameRenderer::getPositionColorProgram);
         RenderSystem.disableCull();
         float r = Math.min(radius, Math.min(width, height) / 2.0F);
         float rInner = Math.max(0.0F, r - thickness);
         int seg = 64;
         BufferBuilder buf = Tessellator.getInstance().begin(DrawMode.QUADS, VertexFormats.POSITION_COLOR);
         buf.vertex(matrix, x + r, y + height, 0.0F).color(color1);
         buf.vertex(matrix, x + width - r, y + height, 0.0F).color(color2);
         buf.vertex(matrix, x + width - r, y + height - thickness, 0.0F).color(color2);
         buf.vertex(matrix, x + r, y + height - thickness, 0.0F).color(color1);
         BufferRenderer.drawWithGlobalProgram(buf.end());
         buf = Tessellator.getInstance().begin(DrawMode.QUADS, VertexFormats.POSITION_COLOR);
         buf.vertex(matrix, x, y + height - r, 0.0F).color(color2);
         buf.vertex(matrix, x + thickness, y + height - r, 0.0F).color(color2);
         buf.vertex(matrix, x + thickness, y, 0.0F).color(color1);
         buf.vertex(matrix, x, y, 0.0F).color(color1);
         BufferRenderer.drawWithGlobalProgram(buf.end());
         buf = Tessellator.getInstance().begin(DrawMode.QUADS, VertexFormats.POSITION_COLOR);
         buf.vertex(matrix, x + width - thickness, y + height - r, 0.0F).color(color2);
         buf.vertex(matrix, x + width, y + height - r, 0.0F).color(color2);
         buf.vertex(matrix, x + width, y, 0.0F).color(color1);
         buf.vertex(matrix, x + width - thickness, y, 0.0F).color(color1);
         BufferRenderer.drawWithGlobalProgram(buf.end());
         int bottomMid = blendColors(color1, color2, 0.5F);
         drawCornerBorderGradient(matrix, x + width - r, y + height - r, r, rInner, bottomMid, color2, seg, 0.0, 1.570796F);
         drawCornerBorderGradient(matrix, x + r, y + height - r, r, rInner, color1, bottomMid, seg, 1.570796F, 3.141593F);
         drawRoundedOuterAAGradient(matrix, x, y, width, height, r, color1, color2, seg, false, true, true, true, false, false, true, true);
         drawHorizontalInnerFadeGradient(matrix, x + r, y + height - thickness, width - 2.0F * r, 1.0F, color1, color2, false);
         drawVerticalInnerFadeGradient(matrix, x + thickness, y, height - r, 1.0F, color1, color2, true);
         drawVerticalInnerFadeGradient(matrix, x + width - thickness, y, height - r, 1.0F, color1, color2, false);
         drawCornerInnerFadeGradient(matrix, x + width - r, y + height - r, rInner, bottomMid, color2, seg, 0.0, 1.570796F);
         drawCornerInnerFadeGradient(matrix, x + r, y + height - r, rInner, color1, bottomMid, seg, 1.570796F, 3.141593F);
         RenderSystem.enableCull();
         RenderSystem.disableBlend();
      }
   }

   public static void drawVerticalBorderLines(MatrixStack matrices, float x, float y, float width, float height, float thickness, int color) {
      Matrix4f matrix = matrices.peek().getPositionMatrix();
      RenderSystem.enableBlend();
      RenderSystem.setShader(GameRenderer::getPositionColorProgram);
      BufferBuilder buf = Tessellator.getInstance().begin(DrawMode.QUADS, VertexFormats.POSITION_COLOR);
      buf.vertex(matrix, x, y + height, 0.0F).color(color);
      buf.vertex(matrix, x + thickness, y + height, 0.0F).color(color);
      buf.vertex(matrix, x + thickness, y, 0.0F).color(color);
      buf.vertex(matrix, x, y, 0.0F).color(color);
      BufferRenderer.drawWithGlobalProgram(buf.end());
      buf = Tessellator.getInstance().begin(DrawMode.QUADS, VertexFormats.POSITION_COLOR);
      buf.vertex(matrix, x + width - thickness, y + height, 0.0F).color(color);
      buf.vertex(matrix, x + width, y + height, 0.0F).color(color);
      buf.vertex(matrix, x + width, y, 0.0F).color(color);
      buf.vertex(matrix, x + width - thickness, y, 0.0F).color(color);
      BufferRenderer.drawWithGlobalProgram(buf.end());
      RenderSystem.disableBlend();
   }

   public static void drawVerticalBorderLines(MatrixStack matrices, float x, float y, float width, float height, float thickness, int color1, int color2) {
      Matrix4f matrix = matrices.peek().getPositionMatrix();
      RenderSystem.enableBlend();
      RenderSystem.setShader(GameRenderer::getPositionColorProgram);
      BufferBuilder buf = Tessellator.getInstance().begin(DrawMode.QUADS, VertexFormats.POSITION_COLOR);
      buf.vertex(matrix, x, y + height, 0.0F).color(color2);
      buf.vertex(matrix, x + thickness, y + height, 0.0F).color(color2);
      buf.vertex(matrix, x + thickness, y, 0.0F).color(color1);
      buf.vertex(matrix, x, y, 0.0F).color(color1);
      BufferRenderer.drawWithGlobalProgram(buf.end());
      buf = Tessellator.getInstance().begin(DrawMode.QUADS, VertexFormats.POSITION_COLOR);
      buf.vertex(matrix, x + width - thickness, y + height, 0.0F).color(color2);
      buf.vertex(matrix, x + width, y + height, 0.0F).color(color2);
      buf.vertex(matrix, x + width, y, 0.0F).color(color1);
      buf.vertex(matrix, x + width - thickness, y, 0.0F).color(color1);
      BufferRenderer.drawWithGlobalProgram(buf.end());
      RenderSystem.disableBlend();
   }

   public static void drawStraightSideBorder(MatrixStack matrices, float x, float y, float width, float height, float thickness, int color) {
      Matrix4f matrix = matrices.peek().getPositionMatrix();
      RenderSystem.enableBlend();
      RenderSystem.setShader(GameRenderer::getPositionColorProgram);
      BufferBuilder buf = Tessellator.getInstance().begin(DrawMode.QUADS, VertexFormats.POSITION_COLOR);
      buf.vertex(matrix, x, y + height, 0.0F).color(color);
      buf.vertex(matrix, x + thickness, y + height, 0.0F).color(color);
      buf.vertex(matrix, x + thickness, y, 0.0F).color(color);
      buf.vertex(matrix, x, y, 0.0F).color(color);
      buf.vertex(matrix, x + width - thickness, y + height, 0.0F).color(color);
      buf.vertex(matrix, x + width, y + height, 0.0F).color(color);
      buf.vertex(matrix, x + width, y, 0.0F).color(color);
      buf.vertex(matrix, x + width - thickness, y, 0.0F).color(color);
      buf.vertex(matrix, x, y + height, 0.0F).color(color);
      buf.vertex(matrix, x + width, y + height, 0.0F).color(color);
      buf.vertex(matrix, x + width, y + height - thickness, 0.0F).color(color);
      buf.vertex(matrix, x, y + height - thickness, 0.0F).color(color);
      BufferRenderer.drawWithGlobalProgram(buf.end());
      RenderSystem.disableBlend();
   }

   public static void drawStraightSideBorder(MatrixStack matrices, float x, float y, float width, float height, float thickness, int color1, int color2) {
      Matrix4f matrix = matrices.peek().getPositionMatrix();
      RenderSystem.enableBlend();
      RenderSystem.setShader(GameRenderer::getPositionColorProgram);
      BufferBuilder buf = Tessellator.getInstance().begin(DrawMode.QUADS, VertexFormats.POSITION_COLOR);
      buf.vertex(matrix, x, y + height, 0.0F).color(color2);
      buf.vertex(matrix, x + thickness, y + height, 0.0F).color(color2);
      buf.vertex(matrix, x + thickness, y, 0.0F).color(color1);
      buf.vertex(matrix, x, y, 0.0F).color(color1);
      BufferRenderer.drawWithGlobalProgram(buf.end());
      buf = Tessellator.getInstance().begin(DrawMode.QUADS, VertexFormats.POSITION_COLOR);
      buf.vertex(matrix, x + width - thickness, y + height, 0.0F).color(color2);
      buf.vertex(matrix, x + width, y + height, 0.0F).color(color2);
      buf.vertex(matrix, x + width, y, 0.0F).color(color1);
      buf.vertex(matrix, x + width - thickness, y, 0.0F).color(color1);
      BufferRenderer.drawWithGlobalProgram(buf.end());
      buf = Tessellator.getInstance().begin(DrawMode.QUADS, VertexFormats.POSITION_COLOR);
      buf.vertex(matrix, x, y + height, 0.0F).color(color1);
      buf.vertex(matrix, x + width, y + height, 0.0F).color(color2);
      buf.vertex(matrix, x + width, y + height - thickness, 0.0F).color(color2);
      buf.vertex(matrix, x, y + height - thickness, 0.0F).color(color1);
      BufferRenderer.drawWithGlobalProgram(buf.end());
      RenderSystem.disableBlend();
   }
}
