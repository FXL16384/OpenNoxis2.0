package noxisclient.mod.gui.items.buttons;

import java.awt.Color;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.Formatting;
import noxisclient.api.utils.math.Timer;
import noxisclient.api.utils.render.Render2DUtil;
import noxisclient.mod.gui.ClickGuiScreen;
import noxisclient.mod.modules.impl.client.ClickGui;
import noxisclient.mod.modules.settings.impl.StringSetting;

public class StringButton extends Button {
   private static final Timer idleTimer = new Timer();
   private static boolean idle;
   private final StringSetting setting;
   public boolean isListening;
   private String currentString = "";
   private boolean selected;

   public StringButton(StringSetting setting) {
      super(setting.getName());
      this.setting = setting;
   }

   public static String removeLastChar(String str) {
      String output = "";
      if (str != null && !str.isEmpty()) {
         output = str.substring(0, str.length() - 1);
      }

      return output;
   }

   public static String getIdleSign() {
      if (idleTimer.passed(500L)) {
         idle = !idle;
         idleTimer.reset();
      }

      return idle ? "_" : "";
   }

   @Override
   public void drawScreen(DrawContext context, int mouseX, int mouseY, float partialTicks) {
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

      if (this.isListening) {
         if (this.selected) {
            float textWidth = (float)mc.textRenderer.getWidth(this.currentString);
            Render2DUtil.drawRoundedRect(context.getMatrices(), this.x + 5.0F, this.y + 2.0F, textWidth, 9.0F, radius, new Color(0, 120, 215, 160));
         }

         this.drawString(
            this.currentString + getIdleSign(),
            (double)(this.x + 5.0F),
            (double)(this.y - 2.0F - (float)ClickGuiScreen.getInstance().getTextOffset()),
            this.getState() ? enableTextColor : defaultTextColor
         );
      } else {
         this.drawString(
            this.setting.getName() + ": " + Formatting.GRAY + this.setting.getValue(),
            (double)(this.x + 5.0F),
            (double)(this.y - 2.0F - (float)ClickGuiScreen.getInstance().getTextOffset()),
            this.getState() ? enableTextColor : defaultTextColor
         );
      }
   }

   @Override
   public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
      super.mouseClicked(mouseX, mouseY, mouseButton);
   }

   @Override
   public void onKeyTyped(char typedChar, int keyCode) {
      if (this.isListening) {
         if (this.selected) {
            this.setString(String.valueOf(typedChar));
            this.selected = false;
         } else {
            this.setString(this.currentString + typedChar);
         }
      }
   }

   @Override
   public void onKeyPressed(int key) {
      if (this.isListening) {
         boolean control = InputUtil.isKeyPressed(mc.getWindow().getHandle(), 341) || InputUtil.isKeyPressed(mc.getWindow().getHandle(), 345);
         if (control) {
            if (key == 86) {
               String clipboard = mc.keyboard.getClipboard();
               if (this.selected) {
                  this.setString(clipboard);
                  this.selected = false;
               } else {
                  this.setString(this.currentString + clipboard);
               }

               return;
            }

            if (key == 67) {
               mc.keyboard.setClipboard(this.currentString);
               return;
            }

            if (key == 65) {
               this.selected = true;
               return;
            }
         }

         switch (key) {
            case 256:
               this.isListening = false;
               this.selected = false;
               break;
            case 257:
            case 335:
               this.enterString();
               this.selected = false;
               break;
            case 259:
               if (this.selected) {
                  this.setString("");
                  this.selected = false;
               } else {
                  this.setString(removeLastChar(this.currentString));
               }
         }
      }
   }

   @Override
   public void update() {
      this.setHidden(!this.setting.isVisible());
   }

   private void enterString() {
      if (this.currentString.isEmpty()) {
         this.setting.setValue(this.setting.getDefaultValue());
      } else {
         this.setting.setValue(this.currentString);
      }

      this.onMouseClick();
   }

   @Override
   public void toggle() {
      this.setString(this.setting.getValue());
      this.isListening = !this.isListening;
      this.selected = false;
   }

   @Override
   public boolean getState() {
      return !this.isListening;
   }

   public void setString(String newString) {
      this.currentString = newString;
   }
}
