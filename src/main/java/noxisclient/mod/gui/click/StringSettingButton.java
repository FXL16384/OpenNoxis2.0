package noxisclient.mod.gui.click;

import java.awt.Color;
import net.minecraft.client.gui.DrawContext;
import noxisclient.api.utils.math.Timer;
import noxisclient.core.impl.FontManager;
import noxisclient.mod.gui.items.Item;
import noxisclient.mod.modules.impl.client.ClickGui;
import noxisclient.mod.modules.impl.client.Fonts;
import noxisclient.mod.modules.settings.impl.StringSetting;
import org.apache.commons.lang3.ArrayUtils;

public class StringSettingButton extends SettingButton {
   private final StringSetting stringSetting;
   private char[] buffer;
   private boolean typing;
   private boolean idling;
   private boolean selected;
   private final Timer idleTimer = new Timer();

   public StringSettingButton(StringSetting setting, ModuleButton parent, float x, float y) {
      super(setting, parent, x, y, 99.0F, 9.0F + 4.0F);
      this.stringSetting = setting;
      this.buffer = setting.getValue().toCharArray();
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
            this.queueShaderBackground(context, this.typing || this.isParentModuleEnabled());
         }
      } else {
         int hoverAlpha = this.getHoverAlpha((double)mouseX, (double)mouseY);
         if (this.shouldUseShaderBackground()) {
            this.queueShaderBackground(context, this.typing || this.isParentModuleEnabled());
         } else {
            this.rect(context, this.x, this.y, this.width * 1.0F, this.height * 1.0F, new Color(hoverAlpha, hoverAlpha, hoverAlpha, hoverAlpha).getRGB());
         }
      }
   }

   @Override
   public void renderForeground(DrawContext context, int mouseX, int mouseY, float delta) {
      this.height = 9.0F + 4.0F;
      ClickGui gui = ClickGui.getInstance();
      if (gui.isThunderStyle()) {
         String text = new String(this.buffer);
         if (this.typing && this.selected) {
            float textWidth;
            if (Fonts.useClickGuiFont()) {
               textWidth = FontManager.ui.getWidth(text);
            } else {
               textWidth = (float)mc.textRenderer.getWidth(text);
            }

            this.rect(context, this.x + 6.0F, this.y + 4.0F, textWidth * 1.0F, 9.0F * 1.0F, new Color(0, 120, 215, 160).getRGB());
         }

         this.drawString(context, this.setting.getName(), this.x + 6.0F, this.y + this.height * 1.0F / 2.0F - 1.0F, gui.getThunderTextColor(false));
         String value = this.typing ? text + this.getInsertionPoint() : text;
         float valueWidth = Fonts.useClickGuiFont() ? FontManager.ui.getWidth(value) : (float)mc.textRenderer.getWidth(value);
         this.drawString(
            context, value, this.x + this.width * 1.0F - 6.0F - valueWidth, this.y + this.height * 1.0F / 2.0F - 1.0F, gui.getThunderTextColor(true)
         );
      } else {
         String text = new String(this.buffer);
         if (this.typing && this.selected) {
            float textWidth;
            if (Fonts.useClickGuiFont()) {
               textWidth = FontManager.ui.getWidth(text);
            } else {
               textWidth = (float)mc.textRenderer.getWidth(text);
            }

            this.rect(context, this.x + 2.0F, this.y + 4.0F, textWidth * 1.0F, 9.0F * 1.0F, new Color(0, 120, 215, 160).getRGB());
         }

         if (this.typing) {
            this.drawString(context, text + this.getInsertionPoint(), this.x + 2.0F, this.y + 4.0F, gui.getEnableTextColor());
         } else {
            this.drawString(context, this.setting.getName(), this.x + 2.0F, this.y + 4.0F, gui.getDefaultTextColor());
            float textLeng = (float)mc.textRenderer.getWidth(this.setting.getName()) * 1.0F;
            this.drawString(context, " " + text, this.x + 2.0F + textLeng, this.y + 4.0F, gui.getEnableTextColor());
         }
      }
   }

   @Override
   public void mouseClicked(double mouseX, double mouseY, int button) {
      if (this.visible) {
         if (this.isWithin(mouseX, mouseY)) {
            if (button == 0) {
               if (this.typing) {
                  this.stringSetting.setValue(new String(this.buffer));
                  this.typing = false;
                  this.selected = false;
               } else {
                  this.typing = true;
                  this.selected = false;
               }

               Item.sound();
            }
         } else {
            this.endTyping();
            this.selected = false;
         }
      }
   }

   @Override
   public void mouseReleased(double mouseX, double mouseY, int button) {
   }

   @Override
   public void keyPressed(int keyCode, int scanCode, int modifiers) {
      if (this.typing) {
         boolean control = (modifiers & 2) != 0;
         if (control) {
            if (keyCode == 86) {
               String clipboard = mc.keyboard.getClipboard();
               if (this.selected) {
                  this.buffer = clipboard.toCharArray();
                  this.selected = false;
               } else {
                  this.buffer = ArrayUtils.addAll(this.buffer, clipboard.toCharArray());
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
               this.buffer = this.stringSetting.getValue().toCharArray();
               this.typing = false;
               this.selected = false;
               break;
            case 257:
               this.stringSetting.setValue(new String(this.buffer));
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
      if (this.typing) {
         if (this.selected) {
            this.buffer = new char[]{chr};
            this.selected = false;
         } else {
            this.buffer = ArrayUtils.add(this.buffer, chr);
         }
      }
   }

   public void endTyping() {
      this.typing = false;
   }

   public boolean isTyping() {
      return this.typing;
   }

   public String getInsertionPoint() {
      if (this.idleTimer.passed(250L)) {
         this.idling = !this.idling;
         this.idleTimer.reset();
      }

      return this.idling && this.typing ? "_" : "";
   }
}
