package noxisclient.mod.gui.click;

import net.minecraft.client.gui.DrawContext;
import noxisclient.Alien;
import noxisclient.api.utils.render.Render2DUtil;
import noxisclient.mod.gui.items.Item;
import noxisclient.mod.modules.impl.client.ClickGui;

public class SearchButton extends AbstractComponent {
   private static final float SCALE = 1.0F;
   private static final String PLACEHOLDER = "Search";
   private static final StringBuilder SEARCH_BUFFER = new StringBuilder();
   private boolean listening = false;

   public SearchButton(float x, float y, float width) {
      super(x, y, width, 13.0F);
   }

   @Override
   public void render(DrawContext context, int mouseX, int mouseY, float delta) {
      ModuleButton.beginShaderBatch();
      this.renderBackground(context, mouseX, mouseY, delta);
      ModuleButton.flushShaderBatch(context);
      this.renderForeground(context, mouseX, mouseY, delta);
   }

   public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
      this.renderParts(context, mouseX, mouseY, true);
   }

   public void renderForeground(DrawContext context, int mouseX, int mouseY, float delta) {
      this.renderParts(context, mouseX, mouseY, false);
   }

   private void renderParts(DrawContext context, int mouseX, int mouseY, boolean backgroundPass) {
      ClickGui gui = ClickGui.getInstance();
      boolean exhibitionSearch = gui.useExhibitionSearchBox();
      boolean thunder = gui.isThunderStyle();
      boolean opal = gui.isOpalStyle();
      boolean shaderBackground = Alien.SHADER != null && gui.shader.getValue();
      this.height = opal ? 20.0F : (thunder ? 14.0F : 9.0F + 4.0F + gui.height.getValueFloat() * 2.0F);
      this.hovered = this.isHovered((double)mouseX, (double)mouseY);
      String text = this.getDisplayText();
      float textOffset = gui.textOffset.getValueFloat();
      float textX;
      float textY;
      float textClipWidth;
      int textColor;
      if (exhibitionSearch) {
         float outerX = this.x;
         float outerY = this.y;
         float outerW = this.width * 1.0F;
         float outerH = this.height * 1.0F;
         float borderThickness = 0.5F;
         float borderInset = (1.0F - borderThickness) * 0.5F;
         int frameColor = this.listening ? gui.getExhibitionSelectedColor() : this.applyAlpha(gui.getExhibitionSeparatorColor(), 0.7F);
         float insetX = 2.0F;
         float insetY = 2.0F;
         float innerX = outerX + insetX;
         float innerY = outerY + insetY;
         float innerW = outerW - insetX * 2.0F;
         float innerH = outerH - insetY * 2.0F;
         if (backgroundPass) {
            if (shaderBackground) {
               ModuleButton.queueGuiShaderRect(this.isShaderActive(), context, outerX, outerY, outerW, outerH);
            } else {
               Render2DUtil.drawRect(context.getMatrices(), outerX, outerY, outerW, outerH, gui.getExhibitionPanelColor());
               Render2DUtil.drawBorder(
                  context.getMatrices(),
                  outerX + borderInset,
                  outerY + borderInset,
                  outerW - borderInset * 2.0F,
                  outerH - borderInset * 2.0F,
                  frameColor,
                  borderThickness
               );
            }
         }

         textX = innerX - 0.0F;
         textY = innerY + (innerH - 9.0F) * 0.5F + 1.0F + textOffset;
         textClipWidth = innerW;
         textColor = SEARCH_BUFFER.length() == 0 && !this.listening ? gui.getExhibitionTextColor(false) : gui.getExhibitionTextColor(true);
      } else if (thunder) {
         int color = this.hovered ? -872415232 : -419430400;
         if (backgroundPass) {
            if (shaderBackground) {
               ModuleButton.queueGuiShaderRect(
                  this.isShaderActive(), context, this.x + 4.0F, this.y + 1.0F, this.width * 1.0F - 8.0F, this.height * 1.0F - 2.0F
               );
            } else {
               Render2DUtil.drawRoundedRect(
                  context.getMatrices(), this.x + 4.0F, this.y + 1.0F, this.width * 1.0F - 8.0F, this.height * 1.0F - 2.0F, 1.0F, color
               );
            }
         }

         textX = this.x + 7.0F;
         textY = this.y + this.height * 1.0F / 2.0F - 6.0F + textOffset;
         textClipWidth = this.width * 1.0F - 14.0F;
         textColor = SEARCH_BUFFER.length() == 0 && !this.listening ? gui.getThunderTextColor(false) : gui.getThunderTextColor(true);
      } else if (opal) {
         int color = this.listening ? gui.getOpalGradientStart(0.4F) : gui.getOpalModuleBaseColor();
         if (backgroundPass) {
            if (shaderBackground) {
               ModuleButton.queueGuiShaderRect(this.isShaderActive(), context, this.x, this.y, this.width * 1.0F, this.height * 1.0F);
            } else {
               Render2DUtil.drawRect(context.getMatrices(), this.x, this.y, this.width * 1.0F, this.height * 1.0F, color);
               if (this.hovered && !this.listening) {
                  Render2DUtil.drawRect(context.getMatrices(), this.x, this.y, this.width * 1.0F, this.height * 1.0F, gui.hoverColor.getValue().getRGB());
               }
            }
         }

         textX = this.x + 6.0F;
         textY = this.y + 6.0F + textOffset;
         textClipWidth = this.width * 1.0F - 12.0F;
         textColor = SEARCH_BUFFER.length() == 0 && !this.listening ? gui.getOpalTextColor(false) : gui.getOpalTextColor(true);
      } else {
         int color = gui.color.getValue().getRGB();
         if (backgroundPass) {
            if (shaderBackground) {
               ModuleButton.queueGuiShaderRect(this.isShaderActive(), context, this.x, this.y, this.width * 1.0F, this.height * 1.0F);
            } else {
               Render2DUtil.drawRect(context.getMatrices(), this.x, this.y, this.width * 1.0F, this.height * 1.0F, color);
            }
         }

         textX = this.x + 2.0F;
         textY = this.y + 3.0F + textOffset;
         textClipWidth = this.width * 1.0F - 4.0F;
         textColor = SEARCH_BUFFER.length() == 0 && !this.listening ? gui.getDefaultTextColor() : gui.getEnableTextColor();
      }

      if (!backgroundPass) {
         ClickGuiText.drawMarquee(context, text, textX, textY, textClipWidth, textColor, gui.shadow.getValue());
      }
   }

   private boolean isShaderActive() {
      return this.listening || SEARCH_BUFFER.length() > 0;
   }

   private String getDisplayText() {
      boolean exhibition = ClickGui.getInstance().useExhibitionSearchBox();
      boolean thunder = ClickGui.getInstance().isThunderStyle();
      boolean opal = ClickGui.getInstance().isOpalStyle();
      if (this.listening) {
         return "Search: " + SEARCH_BUFFER + "_";
      } else if (SEARCH_BUFFER.length() > 0) {
         return "Search: " + SEARCH_BUFFER;
      } else {
         return !exhibition && !thunder && !opal ? "Search" : "Search...";
      }
   }

   @Override
   public void mouseClicked(double mouseX, double mouseY, int button) {
      if (button == 0 && this.isHovered(mouseX, mouseY)) {
         this.listening = true;
         ModuleButton.SEARCH_TEXT = SEARCH_BUFFER.toString();
         Item.sound();
      }
   }

   @Override
   public void mouseReleased(double mouseX, double mouseY, int button) {
   }

   @Override
   public void keyPressed(int keyCode, int scanCode, int modifiers) {
      if (this.listening) {
         if (keyCode != 257 && keyCode != 256) {
            if (keyCode == 259 && SEARCH_BUFFER.length() > 0) {
               SEARCH_BUFFER.deleteCharAt(SEARCH_BUFFER.length() - 1);
               ModuleButton.SEARCH_TEXT = SEARCH_BUFFER.length() > 0 ? SEARCH_BUFFER.toString() : "";
            }
         } else {
            this.clear();
         }
      }
   }

   @Override
   public void charTyped(char chr, int modifiers) {
      if (this.listening && chr >= ' ' && chr <= '~') {
         SEARCH_BUFFER.append(chr);
         ModuleButton.SEARCH_TEXT = SEARCH_BUFFER.toString();
      }
   }

   public boolean isListening() {
      return this.listening;
   }

   private int applyAlpha(int color, float factor) {
      int alpha = color >>> 24 & 0xFF;
      int scaledAlpha = Math.max(0, Math.min(255, (int)((float)alpha * factor)));
      return scaledAlpha << 24 | color & 16777215;
   }

   public void clear() {
      this.listening = false;
      SEARCH_BUFFER.setLength(0);
      ModuleButton.SEARCH_TEXT = null;
   }
}
