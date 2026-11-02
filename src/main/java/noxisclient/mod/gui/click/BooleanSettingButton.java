package noxisclient.mod.gui.click;

import java.awt.Color;
import net.minecraft.client.gui.DrawContext;
import noxisclient.mod.gui.items.Item;
import noxisclient.mod.modules.impl.client.ClickGui;
import noxisclient.mod.modules.settings.impl.BooleanSetting;

public class BooleanSettingButton extends SettingButton {
   private final BooleanSetting boolSetting;

   public BooleanSettingButton(BooleanSetting setting, ModuleButton parent, float x, float y) {
      super(setting, parent, x, y, 99.0F, 9.0F + 4.0F);
      this.boolSetting = setting;
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
      this.height = 9.0F + 4.0F;
      ClickGui gui = ClickGui.getInstance();
      if (gui.isThunderStyle()) {
         if (this.shouldUseShaderBackground()) {
            this.queueShaderBackground(context, this.isShaderActive());
         }
      } else {
         int hoverAlpha = this.getHoverAlpha((double)mouseX, (double)mouseY);
         int buttonColor;
         if (this.boolSetting.getValue()) {
            Color baseColor = gui.color.getValue();
            int alpha = Math.min(255, 100 + hoverAlpha);
            buttonColor = new Color(baseColor.getRed(), baseColor.getGreen(), baseColor.getBlue(), alpha).getRGB();
         } else {
            buttonColor = new Color(hoverAlpha, hoverAlpha, hoverAlpha, hoverAlpha).getRGB();
         }

         if (this.shouldUseShaderBackground()) {
            this.queueShaderBackground(context, this.isShaderActive());
         } else {
            this.rect(context, this.x, this.y, this.width * 1.0F, this.height * 1.0F, buttonColor);
         }
      }
   }

   @Override
   public void renderForeground(DrawContext context, int mouseX, int mouseY, float delta) {
      this.height = 9.0F + 4.0F;
      ClickGui gui = ClickGui.getInstance();
      if (gui.isThunderStyle()) {
         if (this.isThunderExpandOnly()) {
            String symbol = this.boolSetting.isOpen() ? "-" : "+";
            float symbolWidth = (float)mc.textRenderer.getWidth(symbol) * 1.0F;
            this.drawString(context, this.setting.getName(), this.x + 6.0F, this.y + this.height * 1.0F / 2.0F - 1.0F, gui.getThunderTextColor(false));
            this.drawString(
               context, symbol, this.x + this.width * 1.0F - symbolWidth - 10.0F, this.y + this.height * 1.0F / 2.0F - 1.0F, gui.getThunderTextColor(true)
            );
         } else {
            int trackColor = this.boolSetting.getValue() ? gui.getThunderAccentColor() : -12434878;
            float trackX = this.x + this.width * 1.0F - 21.0F;
            float trackY = this.y + this.height * 1.0F / 2.0F - 4.0F;
            context.fill((int)trackX, (int)trackY, (int)(trackX + 15.0F), (int)(trackY + 8.0F), trackColor);
            float knobX = this.x + this.width * 1.0F - 20.0F + (this.boolSetting.getValue() ? 7.0F : 0.0F);
            context.fill(
               (int)knobX, (int)(this.y + this.height * 1.0F / 2.0F - 3.0F), (int)(knobX + 6.0F), (int)(this.y + this.height * 1.0F / 2.0F + 3.0F), -1
            );
            this.drawString(
               context, this.setting.getName(), this.x + 6.0F, this.y + this.height * 1.0F / 2.0F - 1.0F, gui.getThunderTextColor(this.boolSetting.getValue())
            );
         }
      } else {
         int textColor = gui.getTextColor(this.boolSetting.getValue());
         this.drawString(context, this.setting.getName(), this.x + 2.0F, this.y + 4.0F, textColor);
         if (this.boolSetting.hasParent()) {
            String expandSymbol = this.boolSetting.isOpen() ? "-" : "+";
            float symbolWidth = (float)mc.textRenderer.getWidth(expandSymbol) * 1.0F;
            this.drawString(context, expandSymbol, this.x + this.width * 1.0F - symbolWidth - 2.0F, this.y + 4.0F, gui.getEnableTextColor());
         }
      }
   }

   private boolean isShaderActive() {
      return this.boolSetting.getValue() || this.boolSetting.hasParent() && this.boolSetting.isOpen();
   }

   @Override
   public void mouseClicked(double mouseX, double mouseY, int button) {
      if (this.visible) {
         if (ClickGui.getInstance().isThunderStyle()) {
            if (this.isThunderExpandOnly()) {
               if (this.isWithin(mouseX, mouseY) && (button == 0 || button == 1)) {
                  this.boolSetting.setOpen(!this.boolSetting.isOpen());
                  Item.sound();
               }

               return;
            }

            float switchX = this.x + this.width * 1.0F - 22.0F;
            float switchY = this.y + this.height * 1.0F / 2.0F - 5.0F;
            boolean switchHovered = mouseX >= (double)switchX
               && mouseX <= (double)(switchX + 17.0F)
               && mouseY >= (double)switchY
               && mouseY <= (double)(switchY + 10.0F);
            if (switchHovered && (button == 0 || button == 1)) {
               this.boolSetting.setValue(!this.boolSetting.getValue());
               Item.sound();
               return;
            }
         }

         if (this.isWithin(mouseX, mouseY)) {
            if (button == 0) {
               this.boolSetting.setValue(!this.boolSetting.getValue());
               Item.sound();
            } else if (button == 1 && this.boolSetting.hasParent()) {
               this.boolSetting.setOpen(!this.boolSetting.isOpen());
               Item.sound();
            }
         }
      }
   }

   private boolean isThunderExpandOnly() {
      return this.parent.getModule() instanceof ClickGui
         && this.boolSetting.hasParent()
         && ("Elements".equals(this.setting.getName()) || "Colors".equals(this.setting.getName()));
   }

   @Override
   public void mouseReleased(double mouseX, double mouseY, int button) {
   }

   @Override
   public void keyPressed(int keyCode, int scanCode, int modifiers) {
   }

   @Override
   public void charTyped(char chr, int modifiers) {
   }
}
