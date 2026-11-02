package noxisclient.mod.gui.click;

import java.awt.Color;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.InputUtil;
import noxisclient.mod.gui.items.Item;
import noxisclient.mod.modules.impl.client.ClickGui;
import noxisclient.mod.modules.settings.impl.BindSetting;

public class BindSettingButton extends SettingButton {
   private final BindSetting bindSetting;
   private boolean listening;

   public BindSettingButton(BindSetting setting, ModuleButton parent, float x, float y) {
      super(setting, parent, x, y, 99.0F, 9.0F + 4.0F);
      this.bindSetting = setting;
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
      int hoverAlpha = this.getHoverAlpha((double)mouseX, (double)mouseY);
      if (this.shouldUseShaderBackground()) {
         this.queueShaderBackground(context, this.listening || this.isParentModuleEnabled());
      } else {
         this.rect(context, this.x, this.y, this.width * 1.0F, this.height * 1.0F, new Color(hoverAlpha, hoverAlpha, hoverAlpha, hoverAlpha).getRGB());
      }
   }

   @Override
   public void renderForeground(DrawContext context, int mouseX, int mouseY, float delta) {
      this.height = 9.0F + 4.0F;
      int keyCode = this.bindSetting.getValue();
      boolean hovered = this.isWithin((double)mouseX, (double)mouseY);
      boolean shiftPressed = InputUtil.isKeyPressed(mc.getWindow().getHandle(), 340) || InputUtil.isKeyPressed(mc.getWindow().getHandle(), 344);
      boolean showingBindMode = !this.listening && hovered && shiftPressed && this.setting.getName().equals("Key");
      String val = this.listening ? "..." : (showingBindMode ? this.getBindModeDisplay() : this.getKeyDisplayName(keyCode));
      ClickGui gui = ClickGui.getInstance();
      if (gui.isThunderStyle()) {
         this.drawString(context, this.setting.getName(), this.x + 6.0F, this.y + this.height * 1.0F / 2.0F - 1.0F, gui.getThunderTextColor(false));
         float valueWidth = (float)mc.textRenderer.getWidth(val) * 1.0F;
         float tagX = this.x + this.width * 1.0F - valueWidth - 11.0F;
         context.fill((int)tagX, (int)(this.y + 2.0F), (int)(tagX + valueWidth + 4.0F), (int)(this.y + 12.0F), -1811939328);
         this.drawString(context, val, tagX + 2.0F, this.y + this.height * 1.0F / 2.0F - 1.0F, gui.getThunderTextColor(true));
      } else {
         this.drawString(context, this.setting.getName(), this.x + 2.0F, this.y + 4.0F, gui.getDefaultTextColor());
         float textLeng = (float)mc.textRenderer.getWidth(this.setting.getName()) * 1.0F;
         int valueText = gui.getEnableTextColor();
         this.drawString(context, " " + val, this.x + 2.0F + textLeng, this.y + 4.0F, valueText);
      }
   }

   private String getBindModeDisplay() {
      return "Mode: " + (this.bindSetting.isHoldEnable() ? "Hold" : "Toggle");
   }

   private String getKeyDisplayName(int keyCode) {
      return this.bindSetting.getKeyString();
   }

   @Override
   public void mouseClicked(double mouseX, double mouseY, int button) {
      if (this.visible) {
         if (this.isWithin(mouseX, mouseY)) {
            boolean shiftPressed = InputUtil.isKeyPressed(mc.getWindow().getHandle(), 340) || InputUtil.isKeyPressed(mc.getWindow().getHandle(), 344);
            if (button == 0) {
               if (shiftPressed && this.setting.getName().equals("Key")) {
                  this.bindSetting.setHoldEnable(!this.bindSetting.isHoldEnable());
               } else {
                  this.listening = !this.listening;
               }
            } else if (button == 1 && !this.listening) {
               this.bindSetting.setValue(-1);
            } else if (this.listening) {
               if (button != 1) {
                  this.bindSetting.setValue(-(button + 1));
               }

               this.listening = false;
            }

            Item.sound();
         }
      }
   }

   @Override
   public void mouseReleased(double mouseX, double mouseY, int button) {
   }

   @Override
   public void keyPressed(int keyCode, int scanCode, int modifiers) {
      if (this.listening) {
         if (keyCode != 256 && keyCode != 259) {
            this.bindSetting.setValue(keyCode);
         } else {
            this.bindSetting.setValue(-1);
         }

         this.listening = false;
      }
   }

   @Override
   public void charTyped(char chr, int modifiers) {
   }

   public boolean isListening() {
      return this.listening;
   }

   public void setListening(boolean listening) {
      this.listening = listening;
   }
}
