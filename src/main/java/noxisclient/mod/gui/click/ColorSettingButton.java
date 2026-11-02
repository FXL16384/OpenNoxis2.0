package noxisclient.mod.gui.click;

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
import net.minecraft.util.math.MathHelper;
import noxisclient.api.utils.math.Timer;
import noxisclient.api.utils.render.Render2DUtil;
import noxisclient.core.impl.FontManager;
import noxisclient.mod.gui.items.Item;
import noxisclient.mod.modules.impl.client.ClickGui;
import noxisclient.mod.modules.impl.client.Fonts;
import noxisclient.mod.modules.settings.impl.ColorSetting;
import org.apache.commons.lang3.ArrayUtils;
import org.joml.Matrix4f;

public class ColorSettingButton extends SettingButton {
   private static final float THUNDER_COLLAPSED_HEIGHT = 15.0F;
   private static final float THUNDER_EXPANDED_HEIGHT = 88.0F;
   private static final float THUNDER_PICKER_Y = 18.0F;
   private static final float THUNDER_PICKER_HEIGHT = 50.0F;
   private static final float THUNDER_ACTION_Y = 76.0F;
   private static final float THUNDER_ACTION_HEIGHT = 9.0F;
   private static Color copiedColor = Color.WHITE;
   private final ColorSetting colorSetting;
   private boolean open;
   private final float[] selectedColor;
   private char[] buffer;
   private boolean typing;
   private boolean idling;
   private boolean selected;
   private boolean draggingPicker;
   private boolean draggingHue;
   private boolean draggingAlpha;
   private final Timer idleTimer = new Timer();

   public ColorSettingButton(ColorSetting setting, ModuleButton parent, float x, float y) {
      super(setting, parent, x, y, 99.0F, 9.0F + 4.0F);
      this.colorSetting = setting;
      Color color = setting.getValue();
      float[] hsb = Color.RGBtoHSB(color.getRed(), color.getGreen(), color.getBlue(), null);
      this.selectedColor = new float[]{hsb[0], hsb[1], hsb[2], (float)color.getAlpha() / 255.0F};
      this.buffer = Integer.toHexString(color.getRGB() & 16777215).toCharArray();
   }

   @Override
   public void render(DrawContext context, int mouseX, int mouseY, float delta) {
      ModuleButton.beginShaderBatch();
      this.renderBackground(context, mouseX, mouseY, delta);
      ModuleButton.flushShaderBatch(context);
      this.renderForeground(context, mouseX, mouseY, delta);
   }

   @Override
   public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
      ClickGui gui = ClickGui.getInstance();
      boolean thunder = gui.isThunderStyle();
      this.height = thunder ? (this.open ? 88.0F : 15.0F) : 9.0F + 4.0F;
      if (thunder) {
         if (this.shouldUseShaderBackground()) {
            this.queueShaderBackground(context, this.isShaderActive(), this.x, this.y, this.width * 1.0F, 15.0F);
         }
      } else {
         int hoverAlpha = this.getHoverAlpha((double)mouseX, (double)mouseY);
         if (this.shouldUseShaderBackground()) {
            this.queueShaderBackground(context, this.isShaderActive());
         } else {
            this.rect(context, this.x, this.y, this.width * 1.0F, this.height * 1.0F, new Color(hoverAlpha, hoverAlpha, hoverAlpha, hoverAlpha).getRGB());
         }
      }
   }

   @Override
   public void renderForeground(DrawContext context, int mouseX, int mouseY, float delta) {
      ClickGui gui = ClickGui.getInstance();
      boolean thunder = gui.isThunderStyle();
      this.height = thunder ? (this.open ? 88.0F : 15.0F) : 9.0F + 4.0F;
      if (thunder) {
         this.renderThunder(context, mouseX, mouseY);
      } else {
         int originalColor = this.colorSetting.getValue().getRGB();
         int whiteText = gui.getTextColor(!this.colorSetting.injectBoolean || this.colorSetting.booleanValue);
         this.fill(context, this.x + this.width * 1.0F - 11.0F, this.y + 2.0F, 10.0F, 10.0F, originalColor);
         this.drawString(context, this.setting.getName(), this.x + 2.0F, this.y + 4.0F, whiteText);
         if (this.open) {
            float pickerX = this.x + 1.0F;
            float pickerY = this.y + this.height * 1.0F + 2.0F;
            float pickerWidth = this.width * 1.0F - 1.0F;
            float pickerHeight = this.width * 1.0F;
            float hueX = this.x + 1.0F;
            float hueY = this.y + this.height * 1.0F + 4.0F + this.width * 1.0F;
            float hueWidth = this.width * 1.0F - 2.0F;
            float hueHeight = 8.0F;
            float alphaX = this.x + 1.0F;
            float alphaY = this.y + this.height * 1.0F + 14.0F + this.width * 1.0F;
            float alphaWidth = this.width * 1.0F - 2.0F;
            float alphaHeight = 8.0F;
            float alphaOffset = this.colorSetting.isAlphaEnabled() ? 0.0F : -10.0F;
            if (NewClickGuiScreen.MOUSE_LEFT_HOLD) {
               if (this.draggingPicker) {
                  this.selectedColor[1] = MathHelper.clamp(((float)mouseX - pickerX) / pickerWidth, 0.0F, 1.0F);
                  this.selectedColor[2] = MathHelper.clamp(1.0F - ((float)mouseY - pickerY) / pickerHeight, 0.0F, 1.0F);
               }

               if (this.draggingHue) {
                  this.selectedColor[0] = MathHelper.clamp(((float)mouseX - hueX) / hueWidth, 0.0F, 1.0F);
               }

               if (this.draggingAlpha && this.colorSetting.isAlphaEnabled()) {
                  this.selectedColor[3] = MathHelper.clamp(((float)mouseX - alphaX) / alphaWidth, 0.0F, 1.0F);
               }

               if (this.draggingPicker || this.draggingHue || this.draggingAlpha && this.colorSetting.isAlphaEnabled()) {
                  this.applySelectedColor();
               }
            } else {
               this.draggingPicker = false;
               this.draggingHue = false;
               this.draggingAlpha = false;
            }

            float[] hsb = this.selectedColor;
            int pureColor = Color.HSBtoRGB(hsb[0], 1.0F, 1.0F);
            Color pure = new Color(pureColor);
            this.drawPickerBase(context.getMatrices(), pickerX, pickerY, pickerWidth, pickerHeight, pure);
            float indicatorX = pickerX + hsb[1] * pickerWidth - 1.0F;
            float indicatorY = pickerY + (1.0F - hsb[2]) * pickerHeight - 1.0F;
            this.fill(context, indicatorX, indicatorY, 3.0F, 3.0F, -1);

            for (int i = 0; i < 6; i++) {
               float startHue = (float)i / 6.0F;
               float endHue = (float)(i + 1) / 6.0F;
               Render2DUtil.horizontalGradient(
                  context.getMatrices(),
                  hueX + hueWidth * startHue,
                  hueY,
                  hueX + hueWidth * endHue + 1.0F,
                  hueY + hueHeight,
                  Color.HSBtoRGB(startHue, 1.0F, 1.0F),
                  Color.HSBtoRGB(endHue, 1.0F, 1.0F)
               );
            }

            this.fill(context, hueX + hueWidth * hsb[0], hueY, 1.0F, hueHeight, -1);
            if (this.colorSetting.isAlphaEnabled()) {
               Color alphaBase = Color.getHSBColor(hsb[0], hsb[1], hsb[2]);
               Render2DUtil.horizontalGradient(
                  context.getMatrices(),
                  alphaX,
                  alphaY,
                  alphaX + alphaWidth + 1.0F,
                  alphaY + alphaHeight,
                  new Color(alphaBase.getRed(), alphaBase.getGreen(), alphaBase.getBlue(), 0),
                  new Color(alphaBase.getRed(), alphaBase.getGreen(), alphaBase.getBlue(), 255)
               );
               this.fill(context, alphaX + alphaWidth * hsb[3], alphaY, 1.0F, alphaHeight, -1);
            }

            String text = new String(this.buffer);
            if (this.typing && this.selected) {
               float textWidth = Fonts.useClickGuiFont() ? FontManager.ui.getWidth(text) : (float)mc.textRenderer.getWidth(text);
               this.rect(
                  context,
                  this.x + 3.0F,
                  this.y + this.height * 1.0F + 26.0F + this.width * 1.0F + alphaOffset,
                  textWidth * 1.0F,
                  9.0F * 1.0F,
                  new Color(0, 120, 215, 160).getRGB()
               );
            }

            String renderText = this.typing ? text + this.getInsertionPoint() : text;
            this.drawString(context, "#" + renderText, this.x + 3.0F, this.y + this.height * 1.0F + 26.0F + this.width * 1.0F + alphaOffset, whiteText);
            float syncY = this.y + this.height * 1.0F + 26.0F + this.width * 1.0F + alphaOffset + 9.0F * 1.0F + 2.0F;
            int syncColor = this.colorSetting.sync ? this.colorSetting.getValue().getRGB() : gui.getDefaultTextColor();
            this.drawString(context, "sync", this.x + 3.0F, syncY, syncColor);
            this.parent.offset(this.getPickerHeight());
         }
      }
   }

   private boolean isShaderActive() {
      return this.colorSetting.injectBoolean ? this.colorSetting.booleanValue : this.isParentModuleEnabled();
   }

   private void renderThunder(DrawContext context, int mouseX, int mouseY) {
      if (!this.draggingPicker && !this.draggingHue && !this.draggingAlpha) {
         this.syncSelectedColorFromSetting();
      }

      Color current = this.colorSetting.getValue();
      boolean colorHovered = this.isMouseOver((double)mouseX, (double)mouseY, this.x, this.y + 5.0F, Math.min(90.0F, this.width) * 1.0F, 7.0F);
      int whiteText = ClickGui.getInstance().getThunderTextColor(false);
      this.drawString(context, this.setting.getName(), this.x + 6.0F, this.y + 8.0F, whiteText);
      float swatchX = colorHovered ? this.x + this.width * 1.0F - 22.5F : this.x + this.width * 1.0F - 22.0F;
      float swatchY = colorHovered ? this.y + 4.5F : this.y + 5.0F;
      float swatchW = colorHovered ? 15.0F : 14.0F;
      float swatchH = colorHovered ? 8.0F : 7.0F;
      Render2DUtil.drawRoundedRect(context.getMatrices(), swatchX, swatchY, swatchW, swatchH, 1.0F, current.getRGB());
      if (this.open) {
         if (NewClickGuiScreen.MOUSE_LEFT_HOLD) {
            this.updateThunderDrag(mouseX, mouseY);
         } else {
            this.draggingPicker = false;
            this.draggingHue = false;
            this.draggingAlpha = false;
         }

         this.renderThunderPicker(context, current);
         this.renderThunderActionButtons(context, mouseX, mouseY, current);
      }
   }

   private void renderThunderPicker(DrawContext context, Color current) {
      float cx = this.x + 6.0F;
      float cy = this.y + 18.0F;
      float cw = this.width * 1.0F - (this.colorSetting.isAlphaEnabled() ? 38.0F : 25.0F) * 1.0F;
      float ch = 50.0F;
      Color colorA = Color.getHSBColor(this.selectedColor[0], 0.0F, 1.0F);
      Color colorB = Color.getHSBColor(this.selectedColor[0], 1.0F, 1.0F);
      Render2DUtil.horizontalGradient(context.getMatrices(), cx + 2.0F, cy, cx + cw, cy + ch, colorA, colorB);
      this.drawBlackValueOverlay(context, cx + 2.0F, cy, cx + cw, cy + ch);

      for (float i = 1.0F; i < ch - 2.0F; i++) {
         float curHue = i / ch;
         context.fill((int)(cx + cw + 4.0F), (int)(cy + i), (int)(cx + cw + 12.0F), (int)(cy + i + 1.0F), Color.getHSBColor(curHue, 1.0F, 1.0F).getRGB());
      }

      if (this.colorSetting.isAlphaEnabled()) {
         this.drawAlphaBar(context, cx + cw + 17.0F, cy + 1.0F, 8.0F, ch - 3.0F, current);
      }

      float saturationX = cx + cw * this.selectedColor[1];
      float brightnessY = cy + ch * (1.0F - this.selectedColor[2]);
      float hueY = cy + (ch - 3.0F) * this.selectedColor[0];
      float alphaY = cy + (ch - 3.0F) * (1.0F - this.selectedColor[3]);
      context.fill((int)(cx + cw + 3.0F), (int)(hueY + 0.5F), (int)(cx + cw + 13.0F), (int)(hueY + 1.5F), -1);
      if (this.colorSetting.isAlphaEnabled()) {
         context.fill((int)(cx + cw + 16.0F), (int)(alphaY + 0.5F), (int)(cx + cw + 26.0F), (int)(alphaY + 1.5F), -1);
      }

      Render2DUtil.drawRoundedRect(context.getMatrices(), saturationX - 1.5F, brightnessY - 1.5F, 3.0F, 3.0F, 1.5F, -1);
   }

   private void drawBlackValueOverlay(DrawContext context, float x1, float y1, float x2, float y2) {
      int top = (int)y1;
      int bottom = Math.max(top + 1, (int)y2);

      for (int yy = top; yy < bottom; yy++) {
         float progress = (float)(yy - top) / (float)Math.max(1, bottom - top - 1);
         int alpha = MathHelper.clamp((int)(progress * 255.0F), 0, 255);
         context.fill((int)x1, yy, (int)x2, yy + 1, alpha << 24);
      }
   }

   private void drawAlphaBar(DrawContext context, float x, float y, float width, float height, Color color) {
      int top = (int)y;
      int bottom = Math.max(top + 1, (int)(y + height));
      int left = (int)x;
      int right = (int)(x + width);

      for (int yy = top; yy < bottom; yy++) {
         float progress = (float)(yy - top) / (float)Math.max(1, bottom - top - 1);
         int alpha = MathHelper.clamp((int)((1.0F - progress) * 255.0F), 0, 255);
         int lineColor = alpha << 24 | color.getRGB() & 16777215;
         context.fill(left, yy, right, yy + 1, lineColor);
      }
   }

   private void renderThunderActionButtons(DrawContext context, int mouseX, int mouseY, Color current) {
      this.drawThunderActionButton(context, mouseX, mouseY, "Copy", this.x + 7.0F, this.y + 76.0F, 27.0F, 9.0F, new Color(4342338));
      this.drawThunderActionButton(
         context, mouseX, mouseY, "RB", this.x + 37.0F, this.y + 76.0F, 22.0F, 9.0F, this.colorSetting.sync ? current : new Color(4342338)
      );
      this.drawThunderActionButton(context, mouseX, mouseY, "Paste", this.x + 62.0F, this.y + 76.0F, 31.0F, 9.0F, copiedColor);
   }

   private void drawThunderActionButton(DrawContext context, int mouseX, int mouseY, String label, float bx, float by, float bw, float bh, Color color) {
      boolean hovered = this.isMouseOver((double)mouseX, (double)mouseY, bx, by, bw, bh);
      context.fill((int)bx, (int)by, (int)(bx + bw), (int)(by + bh), color.getRGB());
      int textColor = hovered ? -1543503873 : (this.isDark(color) ? -1 : -16777216);
      float tx = bx + (bw - this.getTextWidth(label)) / 2.0F;
      this.drawString(context, label, tx, by + 2.0F, textColor);
   }

   @Override
   public float getHeight() {
      if (ClickGui.getInstance().isThunderStyle()) {
         return this.open ? 88.0F : 15.0F;
      } else {
         return this.open ? this.height + this.getPickerHeight() : this.height;
      }
   }

   private boolean isMouseOver(double mouseX, double mouseY, float x, float y, float width, float height) {
      return mouseX >= (double)x && mouseX <= (double)(x + width) && mouseY >= (double)y && mouseY <= (double)(y + height);
   }

   private void applySelectedColor() {
      Color color = Color.getHSBColor(this.selectedColor[0], this.selectedColor[1], this.selectedColor[2]);
      float alpha = this.colorSetting.isAlphaEnabled()
         ? MathHelper.clamp(this.selectedColor[3], 0.0F, 1.0F)
         : (float)this.colorSetting.getValue().getAlpha() / 255.0F;
      color = new Color((float)color.getRed() / 255.0F, (float)color.getGreen() / 255.0F, (float)color.getBlue() / 255.0F, alpha);
      this.colorSetting.setValue(color);
   }

   private void updateThunderDrag(int mouseX, int mouseY) {
      float cx = this.x + 6.0F;
      float cy = this.y + 18.0F;
      float cw = this.width * 1.0F - (this.colorSetting.isAlphaEnabled() ? 38.0F : 25.0F) * 1.0F;
      float ch = 50.0F;
      if (this.draggingPicker) {
         this.selectedColor[1] = MathHelper.clamp(((float)mouseX - cx) / cw, 0.0F, 1.0F);
         this.selectedColor[2] = MathHelper.clamp((ch - ((float)mouseY - cy)) / ch, 0.0F, 1.0F);
      }

      if (this.draggingHue) {
         this.selectedColor[0] = MathHelper.clamp(((float)mouseY - cy) / ch, 0.0F, 1.0F);
      }

      if (this.draggingAlpha && this.colorSetting.isAlphaEnabled()) {
         this.selectedColor[3] = MathHelper.clamp((ch - ((float)mouseY - cy)) / ch, 0.0F, 1.0F);
      }

      if (this.draggingPicker || this.draggingHue || this.draggingAlpha && this.colorSetting.isAlphaEnabled()) {
         this.applySelectedColor();
      }
   }

   private void syncSelectedColorFromSetting() {
      Color color = this.colorSetting.getValue();
      float[] hsb = Color.RGBtoHSB(color.getRed(), color.getGreen(), color.getBlue(), null);
      this.selectedColor[0] = hsb[0];
      this.selectedColor[1] = hsb[1];
      this.selectedColor[2] = hsb[2];
      this.selectedColor[3] = (float)color.getAlpha() / 255.0F;
   }

   private boolean isDark(Color color) {
      double luminance = (double)color.getRed() * 0.299 + (double)color.getGreen() * 0.587 + (double)color.getBlue() * 0.114;
      return luminance < 128.0;
   }

   private float getTextWidth(String text) {
      return Fonts.useClickGuiFont() ? FontManager.ui.getWidth(text) : (float)mc.textRenderer.getWidth(text);
   }

   private void drawPickerBase(MatrixStack matrices, float pickerX, float pickerY, float pickerWidth, float pickerHeight, Color hueColor) {
      Matrix4f matrix = matrices.peek().getPositionMatrix();
      RenderSystem.enableBlend();
      RenderSystem.defaultBlendFunc();
      RenderSystem.setShader(GameRenderer::getPositionColorProgram);
      BufferBuilder buffer = Tessellator.getInstance().begin(DrawMode.QUADS, VertexFormats.POSITION_COLOR);
      buffer.vertex(matrix, pickerX, pickerY, 0.0F).color(1.0F, 1.0F, 1.0F, 1.0F);
      buffer.vertex(matrix, pickerX, pickerY + pickerHeight, 0.0F).color(1.0F, 1.0F, 1.0F, 1.0F);
      buffer.vertex(matrix, pickerX + pickerWidth, pickerY + pickerHeight, 0.0F)
         .color((float)hueColor.getRed() / 255.0F, (float)hueColor.getGreen() / 255.0F, (float)hueColor.getBlue() / 255.0F, 1.0F);
      buffer.vertex(matrix, pickerX + pickerWidth, pickerY, 0.0F)
         .color((float)hueColor.getRed() / 255.0F, (float)hueColor.getGreen() / 255.0F, (float)hueColor.getBlue() / 255.0F, 1.0F);
      BufferRenderer.drawWithGlobalProgram(buffer.end());
      buffer = Tessellator.getInstance().begin(DrawMode.QUADS, VertexFormats.POSITION_COLOR);
      buffer.vertex(matrix, pickerX, pickerY, 0.0F).color(0.0F, 0.0F, 0.0F, 0.0F);
      buffer.vertex(matrix, pickerX, pickerY + pickerHeight, 0.0F).color(0.0F, 0.0F, 0.0F, 1.0F);
      buffer.vertex(matrix, pickerX + pickerWidth, pickerY + pickerHeight, 0.0F).color(0.0F, 0.0F, 0.0F, 1.0F);
      buffer.vertex(matrix, pickerX + pickerWidth, pickerY, 0.0F).color(0.0F, 0.0F, 0.0F, 0.0F);
      BufferRenderer.drawWithGlobalProgram(buffer.end());
      RenderSystem.disableBlend();
   }

   private float getPickerHeight() {
      float textHeight = 9.0F * 1.0F;
      float alphaHeight = this.colorSetting.isAlphaEnabled() ? 10.0F : 0.0F;
      return this.width * 1.0F + 8.0F + 8.0F + 2.0F * textHeight + alphaHeight;
   }

   @Override
   public void mouseClicked(double mouseX, double mouseY, int button) {
      if (this.visible) {
         if (ClickGui.getInstance().isThunderStyle()) {
            this.mouseClickedThunder(mouseX, mouseY, button);
         } else {
            if (this.isWithin(mouseX, mouseY)) {
               if (button == 1) {
                  this.open = !this.open;
                  Item.sound();
               } else if (button == 0 && this.colorSetting.injectBoolean) {
                  this.colorSetting.booleanValue = !this.colorSetting.booleanValue;
                  Item.sound();
               }
            }

            if (this.open && button == 0) {
               float pickerX = this.x + 1.0F;
               float pickerY = this.y + this.height * 1.0F + 2.0F;
               float pickerWidth = this.width * 1.0F - 1.0F;
               float pickerHeight = this.width * 1.0F;
               float hueX = this.x + 1.0F;
               float hueY = this.y + this.height * 1.0F + 4.0F + this.width * 1.0F;
               float hueWidth = this.width * 1.0F - 2.0F;
               float hueHeight = 8.0F;
               float alphaX = this.x + 1.0F;
               float alphaY = this.y + this.height * 1.0F + 14.0F + this.width * 1.0F;
               float alphaWidth = this.width * 1.0F - 2.0F;
               float alphaHeight = 8.0F;
               float alphaOffset = this.colorSetting.isAlphaEnabled() ? 0.0F : -10.0F;
               this.draggingPicker = this.isMouseOver(mouseX, mouseY, pickerX, pickerY, pickerWidth, pickerHeight);
               this.draggingHue = this.isMouseOver(mouseX, mouseY, hueX, hueY, hueWidth, hueHeight);
               this.draggingAlpha = this.colorSetting.isAlphaEnabled() && this.isMouseOver(mouseX, mouseY, alphaX, alphaY, alphaWidth, alphaHeight);
               float inputBoxX = this.x + 3.0F;
               float inputBoxY = this.y + this.height * 1.0F + 26.0F + this.width * 1.0F + alphaOffset;
               float inputBoxWidth = this.width * 1.0F - 6.0F;
               float inputBoxHeight = 9.0F * 1.0F;
               if (this.isMouseOver(mouseX, mouseY, inputBoxX, inputBoxY, inputBoxWidth, inputBoxHeight)) {
                  this.typing = !this.typing;
                  this.selected = false;
                  if (!this.typing) {
                     this.buffer = Integer.toHexString(this.colorSetting.getValue().getRGB() & 16777215).toCharArray();
                  }

                  Item.sound();
               }

               float syncY = this.y + this.height * 1.0F + 26.0F + this.width * 1.0F + alphaOffset + 9.0F * 1.0F + 2.0F;
               float syncX = this.x + 3.0F;
               float syncWidth = (float)mc.textRenderer.getWidth("sync") * 1.0F;
               float syncHeight = 9.0F * 1.0F;
               if (this.isMouseOver(mouseX, mouseY, syncX, syncY, syncWidth, syncHeight)) {
                  this.colorSetting.sync = !this.colorSetting.sync;
                  Item.sound();
               }
            }
         }
      }
   }

   private void mouseClickedThunder(double mouseX, double mouseY, int button) {
      boolean colorHovered = this.isMouseOver(mouseX, mouseY, this.x, this.y + 5.0F, Math.min(90.0F, this.width) * 1.0F, 7.0F);
      if (colorHovered) {
         if (button == 0) {
            this.open = !this.open;
            Item.sound();
         } else if (button == 1 && this.colorSetting.injectBoolean) {
            this.colorSetting.booleanValue = !this.colorSetting.booleanValue;
            Item.sound();
         }
      }

      if (this.open && button == 0) {
         if (this.isMouseOver(mouseX, mouseY, this.x + 7.0F, this.y + 76.0F, 27.0F, 9.0F)) {
            copiedColor = this.colorSetting.getValue();
            Item.sound();
         } else if (this.isMouseOver(mouseX, mouseY, this.x + 37.0F, this.y + 76.0F, 22.0F, 9.0F)) {
            this.colorSetting.sync = !this.colorSetting.sync;
            Item.sound();
         } else if (this.isMouseOver(mouseX, mouseY, this.x + 62.0F, this.y + 76.0F, 31.0F, 9.0F)) {
            Color current = this.colorSetting.getValue();
            int alpha = this.colorSetting.isAlphaEnabled() ? copiedColor.getAlpha() : current.getAlpha();
            this.colorSetting.setValue(new Color(copiedColor.getRed(), copiedColor.getGreen(), copiedColor.getBlue(), alpha));
            this.syncSelectedColorFromSetting();
            Item.sound();
         } else {
            float cx = this.x + 6.0F;
            float cy = this.y + 18.0F;
            float cw = this.width * 1.0F - (this.colorSetting.isAlphaEnabled() ? 38.0F : 25.0F) * 1.0F;
            float ch = 50.0F;
            this.draggingAlpha = this.colorSetting.isAlphaEnabled() && this.isMouseOver(mouseX, mouseY, cx + cw + 17.0F, cy, 8.0F, ch);
            this.draggingHue = this.isMouseOver(mouseX, mouseY, cx + cw + 4.0F, cy, 8.0F, ch);
            this.draggingPicker = this.isMouseOver(mouseX, mouseY, cx, cy, cw, ch);
            this.updateThunderDrag((int)mouseX, (int)mouseY);
         }
      }
   }

   @Override
   public void mouseReleased(double mouseX, double mouseY, int button) {
      if (button == 0) {
         this.draggingPicker = false;
         this.draggingHue = false;
         this.draggingAlpha = false;
      }
   }

   @Override
   public void keyPressed(int keyCode, int scanCode, int modifiers) {
      if (this.typing) {
         boolean control = (modifiers & 2) != 0;
         if (control) {
            if (keyCode == 86) {
               String clipboard = mc.keyboard.getClipboard();
               if (this.selected) {
                  this.buffer = new char[0];

                  for (char c : clipboard.toCharArray()) {
                     if (this.isHexDigit(c) && this.buffer.length < 8) {
                        this.buffer = ArrayUtils.add(this.buffer, c);
                     }
                  }

                  this.selected = false;
               } else {
                  for (char cx : clipboard.toCharArray()) {
                     if (this.isHexDigit(cx) && this.buffer.length < 8) {
                        this.buffer = ArrayUtils.add(this.buffer, cx);
                     }
                  }
               }

               return;
            }

            if (keyCode == 67) {
               mc.keyboard.setClipboard(new String(this.buffer));
               return;
            }

            if (keyCode == 65) {
               this.selected = true;
               return;
            }
         }

         switch (keyCode) {
            case 256:
               this.buffer = Integer.toHexString(this.colorSetting.getValue().getRGB() & 16777215).toCharArray();
               this.typing = false;
               this.selected = false;
               break;
            case 257:
               try {
                  Color color = Color.decode("#" + new String(this.buffer).trim());
                  int alpha = this.colorSetting.isAlphaEnabled() ? (int)(this.selectedColor[3] * 255.0F) : this.colorSetting.getValue().getAlpha();
                  color = new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha);
                  this.colorSetting.setValue(color);
               } catch (NumberFormatException var10) {
               }

               this.buffer = Integer.toHexString(this.colorSetting.getValue().getRGB() & 16777215).toCharArray();
               this.typing = false;
               this.selected = false;
            case 258:
            default:
               break;
            case 259:
               if (this.selected) {
                  this.buffer = new char[0];
                  this.selected = false;
               } else if (this.buffer.length != 0) {
                  this.buffer = ArrayUtils.remove(this.buffer, this.buffer.length - 1);
               }
         }
      }
   }

   @Override
   public void charTyped(char chr, int modifiers) {
      if (this.typing && this.isHexDigit(chr)) {
         if (this.selected) {
            this.buffer = new char[]{chr};
            this.selected = false;
         } else if (this.buffer.length < 8) {
            this.buffer = ArrayUtils.add(this.buffer, chr);
         }
      }
   }

   private boolean isHexDigit(char ch) {
      return ch >= '0' && ch <= '9' || ch >= 'A' && ch <= 'F' || ch >= 'a' && ch <= 'f';
   }

   public String getInsertionPoint() {
      if (this.idleTimer.passed(250L)) {
         this.idling = !this.idling;
         this.idleTimer.reset();
      }

      return this.idling && this.typing ? "_" : "";
   }
}
