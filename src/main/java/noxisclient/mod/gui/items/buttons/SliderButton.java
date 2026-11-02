package noxisclient.mod.gui.items.buttons;

import java.awt.Color;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.Formatting;
import noxisclient.api.utils.render.Render2DUtil;
import noxisclient.mod.gui.ClickGuiScreen;
import noxisclient.mod.gui.items.Component;
import noxisclient.mod.modules.impl.client.ClickGui;
import noxisclient.mod.modules.settings.impl.SliderSetting;
import org.lwjgl.glfw.GLFW;

public class SliderButton extends Button {
   private final double min;
   private final double max;
   private final double difference;
   public final SliderSetting setting;
   public boolean isListening;
   private String currentString = "";
   private boolean drag = false;
   private boolean selected;

   public SliderButton(SliderSetting setting) {
      super(setting.getName());
      this.setting = setting;
      this.min = setting.getMin();
      this.max = setting.getMax();
      this.difference = this.max - this.min;
   }

   @Override
   public void drawScreen(DrawContext context, int mouseX, int mouseY, float partialTicks) {
      this.dragSetting(mouseX, mouseY);
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

      Color color = ClickGui.getInstance().color.getValue();
      float progressWidth = this.setting.getValue() <= this.min ? 0.0F : (float)this.width * (float)this.partialMultiplier();
      if (progressWidth > 0.0F) {
         Render2DUtil.drawRoundedRect(context.getMatrices(), this.x, this.y, progressWidth, (float)this.height, radius, color);
      }

      if (this.isListening) {
         if (this.selected) {
            float textWidth = (float)mc.textRenderer.getWidth(this.currentString);
            Render2DUtil.drawRoundedRect(context.getMatrices(), this.x + 5.0F, this.y + 2.0F, textWidth, 9.0F, radius, new Color(0, 120, 215, 160));
         }

         this.drawString(
            this.currentString + StringButton.getIdleSign(),
            (double)(this.x + 5.0F),
            (double)(this.y - 2.0F - (float)ClickGuiScreen.getInstance().getTextOffset()),
            this.getState() ? enableTextColor : defaultTextColor
         );
      } else {
         this.drawString(
            this.getName() + " " + Formatting.GRAY + this.setting.getValueFloat() + this.setting.getSuffix(),
            (double)(this.x + 5.0F),
            (double)(this.y - 2.0F - (float)ClickGuiScreen.getInstance().getTextOffset()),
            enableTextColor
         );
      }
   }

   @Override
   public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
      if (this.isHovering(mouseX, mouseY)) {
         sound();
         if (mouseButton == 0) {
            if (this.isListening) {
               this.toggle();
            } else {
               this.setSettingFromX(mouseX);
               this.drag = true;
            }
         } else if (mouseButton == 1) {
            this.toggle();
         }
      }
   }

   @Override
   public boolean isHovering(int mouseX, int mouseY) {
      for (Component component : ClickGuiScreen.getInstance().getComponents()) {
         if (component.drag) {
            return false;
         }
      }

      return (float)mouseX >= this.getX()
         && (float)mouseX <= this.getX() + (float)this.getWidth() + 8.0F
         && (float)mouseY >= this.getY()
         && (float)mouseY <= this.getY() + (float)this.height - 1.0F;
   }

   @Override
   public void update() {
      this.setHidden(!this.setting.isVisible());
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
                  this.setString(StringButton.removeLastChar(this.currentString));
               }
         }
      }
   }

   private void enterString() {
      if (!this.currentString.isEmpty() && this.isNumeric(this.currentString)) {
         this.setting.setValue(Double.parseDouble(this.currentString));
      } else {
         this.setting.setValue(this.setting.getDefaultValue());
      }

      this.onMouseClick();
   }

   public void setString(String newString) {
      this.currentString = newString;
   }

   private void dragSetting(int mouseX, int mouseY) {
      if (this.drag && this.isHovering(mouseX, mouseY) && GLFW.glfwGetMouseButton(mc.getWindow().getHandle(), 0) == 1) {
         this.setSettingFromX(mouseX);
      } else {
         this.drag = false;
      }
   }

   @Override
   public void toggle() {
      this.setString(this.setting.getValueFloat() + "");
      this.isListening = !this.isListening;
      this.selected = false;
   }

   @Override
   public boolean getState() {
      return !this.isListening;
   }

   private void setSettingFromX(int mouseX) {
      double percent = (double)((float)mouseX - this.x) / ((double)this.width + 7.4);
      double result = Math.min(this.setting.getMin() + this.difference * percent, this.max);
      this.setting.setValue(result);
   }

   private double part() {
      return this.setting.getValue() - this.min;
   }

   private double partialMultiplier() {
      return Math.min(this.part() / this.difference, 1.0);
   }

   private boolean isNumeric(String str) {
      return str.matches("-?\\d+(\\.\\d+)?");
   }
}
