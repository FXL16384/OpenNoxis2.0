package noxisclient.mod.gui.items.buttons;

import com.mojang.blaze3d.systems.RenderSystem;
import java.awt.Color;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.render.VertexFormat.DrawMode;
import net.minecraft.client.util.math.MatrixStack;
import noxisclient.api.utils.render.ColorUtil;
import noxisclient.api.utils.render.Render2DUtil;
import noxisclient.mod.gui.ClickGuiScreen;
import noxisclient.mod.modules.impl.client.ClickGui;
import noxisclient.mod.modules.settings.impl.ColorSetting;
import org.lwjgl.glfw.GLFW;

public class PickerButton extends Button {
   static MatrixStack matrixStack;
   private final ColorSetting setting;
   boolean pickingColor;
   boolean pickingHue;
   boolean pickingAlpha;
   boolean open;
   float[] hsb = new float[]{1.0F, 1.0F, 1.0F, 1.0F};

   public PickerButton(ColorSetting setting) {
      super(setting.getName());
      this.setting = setting;
   }

   public static boolean mouseOver(int minX, int minY, int maxX, int maxY, int mX, int mY) {
      return mX >= minX && mY >= minY && mX <= maxX && mY <= maxY;
   }

   public static Color getColor(Color color, float alpha) {
      float red = (float)color.getRed() / 255.0F;
      float green = (float)color.getGreen() / 255.0F;
      float blue = (float)color.getBlue() / 255.0F;
      return new Color(red, green, blue, alpha);
   }

   public static void drawPickerBase(int pickerX, int pickerY, int pickerWidth, int pickerHeight, float red, float green, float blue, float alpha) {
      RenderSystem.enableBlend();
      RenderSystem.setShader(GameRenderer::getPositionColorProgram);
      BufferBuilder bufferBuilder = Tessellator.getInstance().begin(DrawMode.QUADS, VertexFormats.POSITION_COLOR);
      bufferBuilder.vertex(matrixStack.peek().getPositionMatrix(), (float)pickerX, (float)pickerY, 0.0F).color(1.0F, 1.0F, 1.0F, 1.0F);
      bufferBuilder.vertex(matrixStack.peek().getPositionMatrix(), (float)pickerX, (float)(pickerY + pickerHeight), 0.0F).color(1.0F, 1.0F, 1.0F, 1.0F);
      bufferBuilder.vertex(matrixStack.peek().getPositionMatrix(), (float)(pickerX + pickerWidth), (float)(pickerY + pickerHeight), 0.0F)
         .color(red, green, blue, 1.0F);
      bufferBuilder.vertex(matrixStack.peek().getPositionMatrix(), (float)(pickerX + pickerWidth), (float)pickerY, 0.0F).color(red, green, blue, 1.0F);
      BufferRenderer.drawWithGlobalProgram(bufferBuilder.end());
      RenderSystem.disableBlend();
      RenderSystem.enableBlend();
      RenderSystem.setShader(GameRenderer::getPositionColorProgram);
      bufferBuilder = Tessellator.getInstance().begin(DrawMode.QUADS, VertexFormats.POSITION_COLOR);
      bufferBuilder.vertex(matrixStack.peek().getPositionMatrix(), (float)pickerX, (float)pickerY, 0.0F).color(0.0F, 0.0F, 0.0F, 0.0F);
      bufferBuilder.vertex(matrixStack.peek().getPositionMatrix(), (float)pickerX, (float)(pickerY + pickerHeight), 0.0F).color(0.0F, 0.0F, 0.0F, 1.0F);
      bufferBuilder.vertex(matrixStack.peek().getPositionMatrix(), (float)(pickerX + pickerWidth), (float)(pickerY + pickerHeight), 0.0F)
         .color(0.0F, 0.0F, 0.0F, 1.0F);
      bufferBuilder.vertex(matrixStack.peek().getPositionMatrix(), (float)(pickerX + pickerWidth), (float)pickerY, 0.0F).color(0.0F, 0.0F, 0.0F, 0.0F);
      BufferRenderer.drawWithGlobalProgram(bufferBuilder.end());
      RenderSystem.disableBlend();
   }

   public static void drawLeftGradientRect(int left, int top, int right, int bottom, int startColor, int endColor) {
      RenderSystem.enableBlend();
      RenderSystem.setShader(GameRenderer::getPositionColorProgram);
      BufferBuilder bufferBuilder = Tessellator.getInstance().begin(DrawMode.QUADS, VertexFormats.POSITION_COLOR);
      bufferBuilder.vertex(matrixStack.peek().getPositionMatrix(), (float)right, (float)top, 0.0F)
         .color(
            (float)(endColor >> 24 & 0xFF) / 255.0F,
            (float)(endColor >> 16 & 0xFF) / 255.0F,
            (float)(endColor >> 8 & 0xFF) / 255.0F,
            (float)(endColor >> 24 & 0xFF) / 255.0F
         );
      bufferBuilder.vertex(matrixStack.peek().getPositionMatrix(), (float)left, (float)top, 0.0F)
         .color(
            (float)(startColor >> 16 & 0xFF) / 255.0F,
            (float)(startColor >> 8 & 0xFF) / 255.0F,
            (float)(startColor & 0xFF) / 255.0F,
            (float)(startColor >> 24 & 0xFF) / 255.0F
         );
      bufferBuilder.vertex(matrixStack.peek().getPositionMatrix(), (float)left, (float)bottom, 0.0F)
         .color(
            (float)(startColor >> 16 & 0xFF) / 255.0F,
            (float)(startColor >> 8 & 0xFF) / 255.0F,
            (float)(startColor & 0xFF) / 255.0F,
            (float)(startColor >> 24 & 0xFF) / 255.0F
         );
      bufferBuilder.vertex(matrixStack.peek().getPositionMatrix(), (float)right, (float)bottom, 0.0F)
         .color(
            (float)(endColor >> 24 & 0xFF) / 255.0F,
            (float)(endColor >> 16 & 0xFF) / 255.0F,
            (float)(endColor >> 8 & 0xFF) / 255.0F,
            (float)(endColor >> 24 & 0xFF) / 255.0F
         );
      BufferRenderer.drawWithGlobalProgram(bufferBuilder.end());
      RenderSystem.disableBlend();
   }

   public static int gradientColor(int color, int percentage) {
      int r = ((color & 0xFF0000) >> 16) * (100 + percentage) / 100;
      int g = ((color & 0xFF00) >> 8) * (100 + percentage) / 100;
      int elementCodec = (color & 0xFF) * (100 + percentage) / 100;
      return new Color(r, g, elementCodec).hashCode();
   }

   public static void drawGradientRect(float left, float top, float right, float bottom, int startColor, int endColor, boolean hovered) {
      if (hovered) {
         startColor = gradientColor(startColor, -20);
         endColor = gradientColor(endColor, -20);
      }

      float c = (float)(startColor >> 24 & 0xFF) / 255.0F;
      float c1 = (float)(startColor >> 16 & 0xFF) / 255.0F;
      float c2 = (float)(startColor >> 8 & 0xFF) / 255.0F;
      float c3 = (float)(startColor & 0xFF) / 255.0F;
      float c4 = (float)(endColor >> 24 & 0xFF) / 255.0F;
      float c5 = (float)(endColor >> 16 & 0xFF) / 255.0F;
      float c6 = (float)(endColor >> 8 & 0xFF) / 255.0F;
      float c7 = (float)(endColor & 0xFF) / 255.0F;
      RenderSystem.enableBlend();
      RenderSystem.setShader(GameRenderer::getPositionColorProgram);
      BufferBuilder bufferBuilder = Tessellator.getInstance().begin(DrawMode.QUADS, VertexFormats.POSITION_COLOR);
      bufferBuilder.vertex(matrixStack.peek().getPositionMatrix(), right, top, 0.0F).color(c1, c2, c3, c);
      bufferBuilder.vertex(matrixStack.peek().getPositionMatrix(), left, top, 0.0F).color(c1, c2, c3, c);
      bufferBuilder.vertex(matrixStack.peek().getPositionMatrix(), left, bottom, 0.0F).color(c5, c6, c7, c4);
      bufferBuilder.vertex(matrixStack.peek().getPositionMatrix(), right, bottom, 0.0F).color(c5, c6, c7, c4);
      BufferRenderer.drawWithGlobalProgram(bufferBuilder.end());
      RenderSystem.disableBlend();
   }

   public static void drawOutlineRect(double left, double top, double right, double bottom, Color color, float lineWidth) {
      if (left < right) {
         double i = left;
         left = right;
         right = i;
      }

      if (top < bottom) {
         double j = top;
         top = bottom;
         bottom = j;
      }

      float f3 = (float)(color.getRGB() >> 24 & 0xFF) / 255.0F;
      float f = (float)(color.getRGB() >> 16 & 0xFF) / 255.0F;
      float f1 = (float)(color.getRGB() >> 8 & 0xFF) / 255.0F;
      float f2 = (float)(color.getRGB() & 0xFF) / 255.0F;
      RenderSystem.enableBlend();
      RenderSystem.setShader(GameRenderer::getPositionColorProgram);
      BufferBuilder bufferBuilder = Tessellator.getInstance().begin(DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);
      bufferBuilder.vertex(matrixStack.peek().getPositionMatrix(), (float)left, (float)bottom, 0.0F).color(f, f1, f2, f3);
      bufferBuilder.vertex(matrixStack.peek().getPositionMatrix(), (float)right, (float)bottom, 0.0F).color(f, f1, f2, f3);
      bufferBuilder.vertex(matrixStack.peek().getPositionMatrix(), (float)right, (float)top, 0.0F).color(f, f1, f2, f3);
      bufferBuilder.vertex(matrixStack.peek().getPositionMatrix(), (float)left, (float)top, 0.0F).color(f, f1, f2, f3);
      BufferRenderer.drawWithGlobalProgram(bufferBuilder.end());
      RenderSystem.disableBlend();
   }

   @Override
   public void drawScreen(DrawContext context, int mouseX, int mouseY, float partialTicks) {
      matrixStack = context.getMatrices();
      Color color = ClickGui.getInstance().color.getValue();
      boolean hovered = this.isHovering(mouseX, mouseY);
      float radius = 3.0F;
      Render2DUtil.drawRoundedRect(
         context.getMatrices(), this.x, this.y, (float)this.width, (float)this.height, radius, ClickGui.getInstance().defaultColor.getValue()
      );
      if (hovered) {
         Render2DUtil.drawRoundedRect(
            context.getMatrices(), this.x, this.y, (float)this.width, (float)this.height, radius, ClickGui.getInstance().hoverColor.getValue()
         );
      }

      if (this.getState()) {
         Render2DUtil.drawRoundedRect(
            context.getMatrices(), this.x, this.y, (float)this.width, (float)this.height, radius, ClickGui.getInstance().color.getValue()
         );
      }

      float colorBoxX = this.x + (float)this.width - 18.0F;
      float colorBoxY = this.y + 2.0F;
      float colorBoxWidth = 14.0F;
      float colorBoxHeight = (float)this.height - 4.0F;
      Render2DUtil.drawRect(matrixStack, colorBoxX, colorBoxY, colorBoxWidth, colorBoxHeight, ColorUtil.injectAlpha(this.setting.getValue(), 255));
      Render2DUtil.drawRect(matrixStack, colorBoxX, colorBoxY, colorBoxWidth, 1.0F, new Color(255, 255, 255, 100));
      Render2DUtil.drawRect(matrixStack, colorBoxX, colorBoxY, 1.0F, colorBoxHeight, new Color(255, 255, 255, 100));
      Render2DUtil.drawRect(matrixStack, colorBoxX, colorBoxY + colorBoxHeight - 1.0F, colorBoxWidth, 1.0F, new Color(255, 255, 255, 100));
      this.drawString(this.getName(), (double)(this.x + 5.0F), (double)(this.y - 2.0F - (float)ClickGuiScreen.getInstance().getTextOffset()), enableTextColor);
      if (this.open) {
         Render2DUtil.drawRoundedRect(
            context.getMatrices(), this.x, this.y + (float)this.height, (float)this.width, 119.0F, 4.0F, ClickGui.getInstance().defaultColor.getValue()
         );
         Render2DUtil.drawRoundedRect(context.getMatrices(), this.x, this.y + (float)this.height, (float)this.width, 119.0F, 4.0F, new Color(255, 255, 255, 50));
         this.drawPicker(this.setting, (int)this.x, (int)this.y + this.height, (int)this.x, (int)this.y + 103, (int)this.x, (int)this.y + 95, mouseX, mouseY);
         this.drawString(
            "sync",
            (double)(this.x + 5.0F),
            (double)(this.y + 96.0F + (float)this.height - (float)ClickGuiScreen.getInstance().getTextOffset()),
            this.setting.sync ? ColorUtil.injectAlpha(color, 255).getRGB() : (this.isInsideRainbow(mouseX, mouseY) ? enableTextColor : defaultTextColor)
         );
      }
   }

   @Override
   public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
      if (mouseButton == 0) {
         int pickerWidth = (int)((float)this.width + 7.4F);
         int pickerHeight = 78;
         int hueSliderWidth = pickerWidth + 3;
         int hueSliderHeight = 7;
         int alphaSliderHeight = 7;
         if (mouseOver((int)this.x, (int)this.y + 15, (int)this.x + pickerWidth, (int)this.y + 15 + pickerHeight, mouseX, mouseY)) {
            this.pickingColor = true;
         }

         if (mouseOver((int)this.x, (int)this.y + 103, (int)this.x + hueSliderWidth, (int)this.y + 103 + hueSliderHeight, mouseX, mouseY)) {
            this.pickingHue = true;
         }

         if (mouseOver((int)this.x, (int)this.y + 95, (int)this.x + pickerWidth, (int)this.y + 95 + alphaSliderHeight, mouseX, mouseY)) {
            this.pickingAlpha = true;
         }
      }

      if (this.isHovering(mouseX, mouseY)) {
         if (mouseButton == 1) {
            sound();
            this.open = !this.open;
         } else if (mouseButton == 0 && this.setting.injectBoolean) {
            sound();
            boolean var9 = this.setting.booleanValue = !this.setting.booleanValue;
         }
      }

      if (mouseButton == 0 && this.isInsideRainbow(mouseX, mouseY) && this.open) {
         boolean var10 = this.setting.sync = !this.setting.sync;
      }
   }

   @Override
   public boolean getState() {
      return this.setting.booleanValue;
   }

   @Override
   public void update() {
      this.setHidden(!this.setting.isVisible());
   }

   @Override
   public void mouseReleased(int mouseX, int mouseY, int releaseButton) {
      this.pickingAlpha = false;
      this.pickingHue = false;
      this.pickingColor = false;
   }

   public boolean isInsideRainbow(int mouseX, int mouseY) {
      return mouseOver(
         (int)((float)((int)this.x) + 2.3F),
         (int)(this.y + 96.0F + (float)this.height + (float)this.getFontHeight() - (float)ClickGuiScreen.getInstance().getTextOffset()),
         (int)((float)((int)this.x) + 2.3F) + this.getWidth("sync"),
         (int)(this.y + 95.0F + (float)this.height + (float)this.getFontHeight() - (float)ClickGuiScreen.getInstance().getTextOffset()) + this.getFontHeight(),
         mouseX,
         mouseY
      );
   }

   @Override
   public int getHeight() {
      return this.open ? super.getHeight() + 119 : super.getHeight();
   }

   public void drawPicker(
      ColorSetting setting, int pickerX, int pickerY, int hueSliderX, int hueSliderY, int alphaSliderX, int alphaSliderY, int mouseX, int mouseY
   ) {
      int pickerWidth = (int)((float)this.width + 7.4F);
      int pickerHeight = 78;
      int hueSliderHeight = 7;
      int alphaSliderHeight = 7;
      if (this.pickingColor
         && (
            GLFW.glfwGetMouseButton(mc.getWindow().getHandle(), 0) != 1
               || !mouseOver(pickerX, pickerY, pickerX + pickerWidth, pickerY + pickerHeight, mouseX, mouseY)
         )) {
         this.pickingColor = false;
      }

      if (!this.pickingColor) {
         this.hsb = Color.RGBtoHSB(setting.getValue().getRed(), setting.getValue().getGreen(), setting.getValue().getBlue(), null);
      }

      float[] color = new float[]{this.hsb[0], this.hsb[1], this.hsb[2], (float)setting.getValue().getAlpha() / 255.0F};
      if (this.pickingHue
         && (
            GLFW.glfwGetMouseButton(mc.getWindow().getHandle(), 0) != 1
               || !mouseOver(hueSliderX, hueSliderY, hueSliderX + pickerWidth, hueSliderY + hueSliderHeight, mouseX, mouseY)
         )) {
         this.pickingHue = false;
      }

      if (this.pickingHue) {
         float restrictedX = (float)Math.min(Math.max(hueSliderX, mouseX), hueSliderX + pickerWidth);
         color[0] = (restrictedX - (float)hueSliderX) / (float)pickerWidth;
      }

      if (this.pickingAlpha
         && (
            GLFW.glfwGetMouseButton(mc.getWindow().getHandle(), 0) != 1
               || !mouseOver(alphaSliderX, alphaSliderY, alphaSliderX + pickerWidth, alphaSliderY + alphaSliderHeight, mouseX, mouseY)
         )) {
         this.pickingAlpha = false;
      }

      if (this.pickingAlpha) {
         float restrictedX = (float)Math.min(Math.max(alphaSliderX, mouseX), alphaSliderX + pickerWidth);
         color[3] = 1.0F - (restrictedX - (float)alphaSliderX) / (float)pickerWidth;
      }

      if (this.pickingColor) {
         float restrictedX = (float)Math.min(Math.max(pickerX, mouseX), pickerX + pickerWidth);
         float restrictedY = (float)Math.min(Math.max(pickerY, mouseY), pickerY + pickerHeight);
         color[1] = (restrictedX - (float)pickerX) / (float)pickerWidth;
         color[2] = 1.0F - (restrictedY - (float)pickerY) / (float)pickerHeight;
      }

      int selectedColor = Color.HSBtoRGB(color[0], 1.0F, 1.0F);
      float selectedRed = (float)(selectedColor >> 16 & 0xFF) / 255.0F;
      float selectedGreen = (float)(selectedColor >> 8 & 0xFF) / 255.0F;
      float selectedBlue = (float)(selectedColor & 0xFF) / 255.0F;
      drawPickerBase(pickerX, pickerY, pickerWidth, pickerHeight, selectedRed, selectedGreen, selectedBlue, color[3]);
      this.drawHueSlider(hueSliderX, hueSliderY, pickerWidth, hueSliderHeight, color[0]);
      int cursorX = (int)((float)pickerX + color[1] * (float)pickerWidth);
      int cursorY = (int)((float)(pickerY + pickerHeight) - color[2] * (float)pickerHeight);
      setting.setValue(getColor(new Color(Color.HSBtoRGB(color[0], color[1], color[2])), color[3]));
      Render2DUtil.arrow(matrixStack, (float)cursorX, (float)cursorY, setting.getValue());
      this.drawAlphaSlider(alphaSliderX, alphaSliderY, pickerWidth - 1, alphaSliderHeight, selectedRed, selectedGreen, selectedBlue, color[3]);
   }

   public void drawHueSlider(int x, int y, int width, int height, float hue) {
      int step = 0;
      if (height > width) {
         Render2DUtil.rect(matrixStack, (float)x, (float)y, (float)(x + width), (float)(y + 4), -65536);
         y += 4;

         for (int colorIndex = 0; colorIndex < 6; colorIndex++) {
            int previousStep = Color.HSBtoRGB((float)step / 6.0F, 1.0F, 1.0F);
            int nextStep = Color.HSBtoRGB((float)(step + 1) / 6.0F, 1.0F, 1.0F);
            drawGradientRect(
               (float)x,
               (float)y + (float)step * ((float)height / 6.0F),
               (float)(x + width),
               (float)y + (float)(step + 1) * ((float)height / 6.0F),
               previousStep,
               nextStep,
               false
            );
            step++;
         }

         int sliderMinY = (int)((float)y + (float)height * hue) - 4;
         Render2DUtil.rect(matrixStack, (float)x, (float)(sliderMinY - 1), (float)(x + width), (float)(sliderMinY + 1), -1);
         Render2DUtil.rect(matrixStack, (float)x, (float)(sliderMinY - 1), (float)(x + width), (float)(sliderMinY + 1), -1);
         Render2DUtil.rect(matrixStack, (float)x, (float)(sliderMinY - 1), (float)(x + width), (float)(sliderMinY + 1), -1);
      } else {
         for (int colorIndex = 0; colorIndex < 6; colorIndex++) {
            int previousStep = Color.HSBtoRGB((float)step / 6.0F, 1.0F, 1.0F);
            int nextStep = Color.HSBtoRGB((float)(step + 1) / 6.0F, 1.0F, 1.0F);
            drawGradientRect(
               (float)(x + step * (width / 6)), (float)y, (float)(x + (step + 1) * (width / 6) + 3), (float)(y + height), previousStep, nextStep, true
            );
            step++;
         }

         int sliderMinX = (int)((float)x + (float)width * hue);
         Render2DUtil.rect(matrixStack, (float)(sliderMinX - 1), (float)y - 1.2F, (float)(sliderMinX + 1), (float)(y + height) + 1.2F, -1);
         Render2DUtil.rect(matrixStack, (float)(sliderMinX - 1), (float)y - 1.2F, (float)(sliderMinX + 1), (float)(y + height) + 1.2F, -1);
      }
   }

   public void drawAlphaSlider(int x, int y, int width, int height, float red, float green, float blue, float alpha) {
      boolean left = true;
      int checkerBoardSquareSize = height / 2;

      for (int squareIndex = -checkerBoardSquareSize; squareIndex < width; squareIndex += checkerBoardSquareSize) {
         if (!left) {
            Render2DUtil.rect(matrixStack, (float)(x + squareIndex), (float)y, (float)(x + squareIndex + checkerBoardSquareSize), (float)(y + height), -1);
            Render2DUtil.rect(
               matrixStack,
               (float)(x + squareIndex),
               (float)(y + checkerBoardSquareSize),
               (float)(x + squareIndex + checkerBoardSquareSize),
               (float)(y + height),
               -7303024
            );
            if (squareIndex < width - checkerBoardSquareSize) {
               int minX = x + squareIndex + checkerBoardSquareSize;
               int maxX = Math.min(x + width, x + squareIndex + checkerBoardSquareSize * 2);
               Render2DUtil.rect(matrixStack, (float)minX, (float)y, (float)maxX, (float)(y + height), -7303024);
               Render2DUtil.rect(matrixStack, (float)minX, (float)(y + checkerBoardSquareSize), (float)maxX, (float)(y + height), -1);
            }
         }

         left = !left;
      }

      drawLeftGradientRect(x, y, x + width, y + height, new Color(red, green, blue, 1.0F).getRGB(), 0);
      int sliderMinX = (int)((float)(x + width) - (float)width * alpha);
      Render2DUtil.rect(matrixStack, (float)(sliderMinX - 1), (float)y - 1.2F, (float)(sliderMinX + 1), (float)(y + height) + 1.2F, -1);
      Render2DUtil.rect(matrixStack, (float)(sliderMinX - 1), (float)y - 1.2F, (float)(sliderMinX + 1), (float)(y + height) + 1.2F, -1);
      drawOutlineRect((double)sliderMinX - 1.2, (double)y - 1.2, (double)sliderMinX + 1.2, (double)(y + height) + 1.2, Color.BLACK, 1.0F);
   }
}
