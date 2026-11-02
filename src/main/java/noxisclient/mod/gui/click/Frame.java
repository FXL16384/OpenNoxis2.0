package noxisclient.mod.gui.click;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.client.gui.DrawContext;
import noxisclient.Alien;
import noxisclient.Vitality;
import noxisclient.api.utils.render.Render2DUtil;
import noxisclient.core.impl.FontManager;
import noxisclient.mod.gui.items.Item;
import noxisclient.mod.modules.Module;
import noxisclient.mod.modules.impl.client.ClickGui;
import noxisclient.mod.modules.impl.client.Fonts;

public class Frame extends AbstractComponent {
   private static final List<float[]> TITLE_SHADER_RECTS = new ArrayList<>();
   private final Module.Category category;
   private final List<ModuleButton> moduleButtons = new ArrayList<>();
   private SearchButton searchButton;
   private boolean open = true;
   private boolean dragging = false;
   private float dragX;
   private float dragY;
   private float fheight = 0.0F;
   private float px;
   private float py;
   private float inner = 0.0F;
   private static final float FRAME_WIDTH = 105.0F;
   private static final float FRAME_HEADER_HEIGHT = 15.0F;
   private static final float SCALE = 1.0F;
   private static final float MODULE_EDGE_GAP = 1.0F;

   public Frame(Module.Category category, float x, float y) {
      super(x, y, 105.0F, 15.0F);
      this.category = category;
      this.setupModules();
   }

   private void setupModules() {
      List<Module> modules = new ArrayList<>();

      for (Module module : Vitality.MODULE.getModules()) {
         if (module.getCategory() == this.category) {
            modules.add(module);
         }
      }

      modules.sort(Comparator.comparing(m -> m.getName().toLowerCase()));
      float moduleWidth = this.width * 1.0F - 2.0F;
      if (this.category == Module.Category.Client) {
         this.searchButton = new SearchButton(this.x, this.y + 15.0F, moduleWidth);
      }

      for (Module modulex : modules) {
         this.moduleButtons.add(new ModuleButton(modulex, this, this.x, this.y + 15.0F, moduleWidth));
      }
   }

   @Override
   public void render(DrawContext context, int mouseX, int mouseY, float delta) {
      ClickGui gui = ClickGui.getInstance();
      boolean exhibition = gui.isExhibitionStyle();
      boolean thunder = gui.isThunderStyle();
      boolean opal = gui.isOpalStyle();
      float modulePadding = opal ? 0.0F : (exhibition ? 0.0F : (thunder ? 3.0F : 1.0F));
      float moduleGap = gui.moduleGap.getValueFloat() * 1.0F;
      this.width = gui.width.getValueFloat();
      this.height = opal ? 20.0F : (thunder ? 18.0F : 9.0F + 6.0F + gui.height.getValueFloat() * 2.0F);
      float cornerRadius = gui.getGuiCornerRadius();
      if (this.dragging) {
         this.x = (float)mouseX - this.dragX;
         this.y = (float)mouseY - this.dragY;
      }

      boolean standardLayout = !exhibition && !thunder && !opal;
      int contentCount = 0;
      this.fheight = !exhibition && !opal ? (thunder ? 3.0F : 1.0F) : 0.0F;
      if (this.searchButton != null) {
         this.fheight = this.fheight + this.searchButton.getHeight();
         contentCount++;
      }

      for (ModuleButton moduleButton : this.moduleButtons) {
         if (moduleButton.isVisible()) {
            this.fheight = this.fheight + moduleButton.getHeight();
            this.fheight = this.fheight + moduleButton.getAnimatedSettingsHeight();
            contentCount++;
         }
      }

      if (contentCount > 0) {
         this.fheight += moduleGap * (float)(standardLayout ? contentCount - 1 : contentCount);
      }

      float bgBottomPad = standardLayout ? 1.0F : (!opal && !exhibition ? 5.0F : 0.0F);
      int headerColor = opal
         ? gui.getOpalHeaderColor()
         : (exhibition ? gui.getExhibitionHeaderColor() : (thunder ? gui.getThunderHeaderColor() : gui.color.getValue().getRGB()));
      float headerH = this.height * 1.0F;
      float bgY = this.y + headerH;
      float bgHeight = this.fheight + bgBottomPad;
      float totalHeight = headerH + bgHeight;
      boolean titleShader = gui.shouldRenderTitleBarShader();
      if (!titleShader && !exhibition && !thunder && !opal && gui.glow.getValue() && gui.glowFrame.getValue()) {
         float intensity = gui.glowIntensity.getValueFloat();
         int layers = gui.glowLayers.getValueInt();
         boolean rounded = cornerRadius > 0.0F;
         boolean showFullFrameGlow = this.open && gui.backGround.booleanValue;
         if (!showFullFrameGlow || !rounded) {
            Render2DUtil.drawGlowEffect(context.getMatrices(), this.x, this.y, this.width * 1.0F, this.height * 1.0F, headerColor, intensity, layers);
         } else if (gui.border.getValue() && gui.gradientBorder.getValue()) {
            int borderColor1 = gui.borderColor.getValue().getRGB();
            int borderColor2 = gui.borderColor2.getValue().getRGB();
            Render2DUtil.drawGradientRoundedOutlineGlow(
               context.getMatrices(), this.x, this.y, this.width * 1.0F, totalHeight, cornerRadius, borderColor1, borderColor2, intensity, layers
            );
         } else {
            int glowColor = gui.border.getValue() ? gui.borderColor.getValue().getRGB() : headerColor;
            Render2DUtil.drawRoundedOutlineGlow(
               context.getMatrices(), this.x, this.y, this.width * 1.0F, totalHeight, cornerRadius, glowColor, intensity, layers
            );
         }
      }

      if (thunder) {
         if (this.open) {
            Render2DUtil.drawRoundedRect(
               context.getMatrices(), this.x + 3.0F, this.y + this.height * 1.0F - 6.0F, this.width * 1.0F - 6.0F, bgHeight, 1.0F, gui.getThunderPanelColor()
            );
         }

         Render2DUtil.drawRoundedRect(context.getMatrices(), this.x + 2.0F, this.y - 5.0F, this.width * 1.0F - 4.0F, this.height * 1.0F, 1.0F, headerColor);
      } else if (opal && this.open) {
         OpalRenderUtil.roundedTop(context, this.x, this.y, this.width * 1.0F, this.height * 1.0F, cornerRadius, headerColor);
      } else if (opal) {
         OpalRenderUtil.roundedAll(context, this.x, this.y, this.width * 1.0F, this.height * 1.0F, cornerRadius, headerColor);
      } else if (this.open) {
         this.drawTopRoundedRect(context, this.x, this.y, this.width * 1.0F, this.height * 1.0F, cornerRadius, headerColor);
      } else {
         this.drawRoundedOrSquareRect(context, this.x, this.y, this.width * 1.0F, this.height * 1.0F, cornerRadius, headerColor);
      }

      if (titleShader) {
         this.queueTitleShader(thunder);
      }

      if (!thunder && !opal && this.open && gui.backGround.booleanValue) {
         int bgColor = exhibition ? gui.getExhibitionPanelColor() : gui.backGround.getValue().getRGB();
         this.drawBottomRoundedRect(context, this.x, bgY, this.width * 1.0F, bgHeight, cornerRadius, bgColor);
      }

      if (!titleShader) {
         this.renderTitleForeground(context, gui, exhibition, thunder, opal);
      }

      if (this.open) {
         if (gui.border.getValue()) {
            int borderColor = gui.borderColor.getValue().getRGB();
            float borderThickness = gui.borderThickness.getValueFloat();
            if (gui.gradientBorder.getValue()) {
               int borderColor2 = gui.borderColor2.getValue().getRGB();
               Render2DUtil.drawStraightSideBorder(context.getMatrices(), this.x, bgY, this.width * 1.0F, bgHeight, borderThickness, borderColor, borderColor2);
            } else {
               Render2DUtil.drawStraightSideBorder(context.getMatrices(), this.x, bgY, this.width * 1.0F, bgHeight, borderThickness, borderColor);
            }
         }

         this.inner = this.y + this.height * 1.0F + (opal ? 0.0F : (exhibition ? 0.0F : (thunder ? -4.0F : 1.0F)));
         float moduleX = this.x + modulePadding;
         float contentStartY = this.inner;
         ModuleButton.beginShaderBatch();
         if (this.searchButton != null) {
            this.searchButton.setX(moduleX);
            this.searchButton.setY(this.inner);
            this.searchButton.setWidth(this.width - 2.0F * modulePadding);
            this.searchButton.renderBackground(context, mouseX, mouseY, delta);
            this.inner = this.inner + this.searchButton.getHeight() + moduleGap;
         }

         float moduleStartY = this.inner;

         for (ModuleButton moduleButtonx : this.moduleButtons) {
            moduleButtonx.setWidth(this.width - 2.0F * modulePadding);
            moduleButtonx.renderBackground(context, moduleX, this.inner, mouseX, mouseY, delta);
            if (moduleButtonx.isVisible()) {
               this.inner = this.inner + moduleButtonx.getHeight() + moduleGap;
               this.inner = this.inner + moduleButtonx.getRenderedSettingsHeight();
            }
         }

         ModuleButton.flushShaderBatch(context);
         this.inner = contentStartY;
         if (this.searchButton != null) {
            this.searchButton.setX(moduleX);
            this.searchButton.setY(this.inner);
            this.searchButton.setWidth(this.width - 2.0F * modulePadding);
            this.searchButton.renderForeground(context, mouseX, mouseY, delta);
            this.inner = this.inner + this.searchButton.getHeight() + moduleGap;
         }

         this.inner = moduleStartY;

         for (ModuleButton moduleButtonxx : this.moduleButtons) {
            moduleButtonxx.setWidth(this.width - 2.0F * modulePadding);
            moduleButtonxx.renderForeground(context, moduleX, this.inner, mouseX, mouseY, delta);
            if (moduleButtonxx.isVisible()) {
               this.inner = this.inner + moduleButtonxx.getHeight() + moduleGap;
               this.inner = this.inner + moduleButtonxx.getRenderedSettingsHeight();
            }
         }
      }

      this.px = (float)mouseX;
      this.py = (float)mouseY;
   }

   private void drawRoundedOrSquareRect(DrawContext context, float x, float y, float width, float height, float radius, int color) {
      Render2DUtil.drawRoundedRect(context.getMatrices(), x, y, width, height, radius, color);
   }

   private void drawTopRoundedRect(DrawContext context, float x, float y, float width, float height, float radius, int color) {
      if (radius <= 0.0F) {
         Render2DUtil.drawRect(context.getMatrices(), x, y, width, height, color);
      } else {
         Render2DUtil.drawTopRoundedRect(context.getMatrices(), x, y, width, height, radius, color);
      }
   }

   private void drawBottomRoundedRect(DrawContext context, float x, float y, float width, float height, float radius, int color) {
      if (radius <= 0.0F) {
         Render2DUtil.drawRect(context.getMatrices(), x, y, width, height, color);
      } else {
         Render2DUtil.drawBottomRoundedRect(context.getMatrices(), x, y, width, height, radius, color);
      }
   }

   public static void beginTitleShaderBatch() {
      TITLE_SHADER_RECTS.clear();
   }

   public static void flushTitleShaderBatch(DrawContext context) {
      ClickGui gui = ClickGui.getInstance();
      if (Alien.SHADER != null && gui != null && gui.shouldRenderTitleBarShader() && !TITLE_SHADER_RECTS.isEmpty()) {
         Alien.SHADER.applyGuiShaderRects(TITLE_SHADER_RECTS, gui.shaderMode.getValue(), context.getMatrices(), gui.getShaderAlpha());
      }

      TITLE_SHADER_RECTS.clear();
   }

   public void renderTitleForeground(DrawContext context) {
      ClickGui gui = ClickGui.getInstance();
      if (gui != null && gui.shouldRenderTitleBarShader()) {
         this.renderTitleForeground(context, gui, gui.isExhibitionStyle(), gui.isThunderStyle(), gui.isOpalStyle());
      }
   }

   private void renderTitleForeground(DrawContext context, ClickGui gui, boolean exhibition, boolean thunder, boolean opal) {
      String categoryName = this.category.name();
      int headerColor = opal
         ? gui.getOpalHeaderColor()
         : (exhibition ? gui.getExhibitionHeaderColor() : (thunder ? gui.getThunderHeaderColor() : gui.color.getValue().getRGB()));
      float titleOffset = gui.titleOffset.getValueFloat();
      float textX = opal ? this.x + 5.0F : (thunder ? this.x + this.width * 1.0F / 2.0F - (float)mc.textRenderer.getWidth(categoryName) / 2.0F : this.x + 3.0F);
      float textY = opal ? this.y + 6.0F + titleOffset : (thunder ? this.y + this.height * 1.0F / 2.0F - 10.0F + titleOffset : this.y + 4.0F + titleOffset);
      if (Fonts.useClickGuiFont()) {
         if (!exhibition && !thunder && !opal && gui.glow.getValue() && gui.glowText.getValue()) {
            FontManager.ui
               .drawStringWithGlow(
                  context.getMatrices(), categoryName, textX, textY, -1, headerColor, gui.textGlowIntensity.getValueFloat(), gui.shadow.getValue()
               );
         } else {
            int textColor = opal
               ? gui.getOpalTextColor(true)
               : (exhibition ? gui.getExhibitionTextColor(true) : (thunder ? gui.getThunderTextColor(true) : gui.getEnableTextColor()));
            FontManager.ui.drawString(context.getMatrices(), categoryName, textX, textY, textColor, gui.shadow.getValue());
         }
      } else {
         if (!exhibition && !thunder && !opal && gui.glow.getValue() && gui.glowText.getValue()) {
            float intensity = gui.textGlowIntensity.getValueFloat();

            for (int i = 3; i > 0; i--) {
               float offset = (float)i * intensity * 0.8F;
               int alpha = (int)(200.0F * (1.0F - (float)i / 4.0F) * 0.8F);
               if (alpha > 10) {
                  int glowColor = alpha << 24 | headerColor & 16777215;

                  for (int j = 0; j < 8; j++) {
                     double angle = (Math.PI / 4) * (double)j;
                     context.drawText(
                        mc.textRenderer,
                        categoryName,
                        (int)((double)textX + Math.cos(angle) * (double)offset),
                        (int)((double)textY + Math.sin(angle) * (double)offset),
                        glowColor,
                        false
                     );
                  }
               }
            }
         }

         int textColor = opal
            ? gui.getOpalTextColor(true)
            : (exhibition ? gui.getExhibitionTextColor(true) : (thunder ? gui.getThunderTextColor(true) : gui.getEnableTextColor()));
         context.drawText(mc.textRenderer, categoryName, (int)textX, (int)textY, textColor, gui.shadow.getValue());
      }
   }

   private void queueTitleShader(boolean thunder) {
      ClickGui gui = ClickGui.getInstance();
      if (Alien.SHADER != null && gui.shouldRenderTitleBarShader()) {
         float shaderX = thunder ? this.x + 2.0F : this.x;
         float shaderY = thunder ? this.y - 5.0F : this.y;
         float shaderWidth = thunder ? this.width * 1.0F - 4.0F : this.width * 1.0F;
         float shaderHeight = this.height * 1.0F;
         if (!(shaderWidth <= 0.0F) && !(shaderHeight <= 0.0F)) {
            TITLE_SHADER_RECTS.add(new float[]{shaderX, shaderY, shaderWidth, shaderHeight});
         }
      }
   }

   public void offset(float value) {
      this.inner += value;
   }

   @Override
   public void mouseClicked(double mouseX, double mouseY, int button) {
      if (this.visible) {
         if (this.isHovered(mouseX, mouseY) && mouseY >= (double)this.y && mouseY <= (double)(this.y + this.height * 1.0F)) {
            if (button == 0) {
               this.dragging = true;
               this.dragX = (float)mouseX - this.x;
               this.dragY = (float)mouseY - this.y;
            } else if (button == 1) {
               this.open = !this.open;
               Item.sound();
            }
         } else {
            if (this.open) {
               if (this.searchButton != null) {
                  this.searchButton.mouseClicked(mouseX, mouseY, button);
               }

               for (ModuleButton moduleButton : this.moduleButtons) {
                  moduleButton.mouseClicked(mouseX, mouseY, button);
               }
            }
         }
      }
   }

   @Override
   public void mouseReleased(double mouseX, double mouseY, int button) {
      if (button == 0) {
         this.dragging = false;
      }

      if (this.open) {
         if (this.searchButton != null) {
            this.searchButton.mouseReleased(mouseX, mouseY, button);
         }

         for (ModuleButton moduleButton : this.moduleButtons) {
            moduleButton.mouseReleased(mouseX, mouseY, button);
         }
      }
   }

   @Override
   public void keyPressed(int keyCode, int scanCode, int modifiers) {
      if (this.open) {
         if (this.searchButton != null) {
            this.searchButton.keyPressed(keyCode, scanCode, modifiers);
            if (this.searchButton.isListening()) {
               return;
            }
         }

         for (ModuleButton moduleButton : this.moduleButtons) {
            moduleButton.keyPressed(keyCode, scanCode, modifiers);
         }
      }
   }

   @Override
   public void charTyped(char chr, int modifiers) {
      if (this.open) {
         if (this.searchButton != null) {
            this.searchButton.charTyped(chr, modifiers);
            if (this.searchButton.isListening()) {
               return;
            }
         }

         for (ModuleButton moduleButton : this.moduleButtons) {
            moduleButton.charTyped(chr, modifiers);
         }
      }
   }

   public boolean isBindListening() {
      for (ModuleButton moduleButton : this.moduleButtons) {
         for (SettingButton settingButton : moduleButton.getSettingButtons()) {
            if (settingButton instanceof BindSettingButton bindButton && bindButton.isListening()) {
               return true;
            }
         }
      }

      return false;
   }

   public boolean isSearchListening() {
      return this.searchButton != null && this.searchButton.isListening();
   }

   public void clearSearch() {
      if (this.searchButton != null) {
         this.searchButton.clear();
      }
   }

   public float getTotalHeight() {
      return this.height * 1.0F + this.fheight;
   }

   public Module.Category getCategory() {
      return this.category;
   }

   public boolean isOpen() {
      return this.open;
   }

   public boolean isLastVisibleModule(ModuleButton target) {
      ModuleButton lastVisible = null;

      for (ModuleButton moduleButton : this.moduleButtons) {
         if (moduleButton.isVisible()) {
            lastVisible = moduleButton;
         }
      }

      return target == lastVisible;
   }

   public void setOpen(boolean open) {
      this.open = open;
   }

   public List<ModuleButton> getModuleButtons() {
      return this.moduleButtons;
   }

   public boolean isDragging() {
      return this.dragging;
   }

   public void setDragging(boolean dragging) {
      this.dragging = dragging;
   }
}
