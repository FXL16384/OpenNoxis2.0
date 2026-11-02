package noxisclient.mod.gui.items.buttons;

import java.awt.Color;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.Formatting;
import noxisclient.api.utils.render.Render2DUtil;
import noxisclient.mod.gui.ClickGuiScreen;
import noxisclient.mod.modules.impl.client.ClickGui;
import noxisclient.mod.modules.settings.impl.BindSetting;

public class BindButton extends Button {
   private final BindSetting setting;
   public boolean isListening;

   public BindButton(BindSetting setting) {
      super(setting.getName());
      this.setting = setting;
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

      if (!this.getState()) {
         Render2DUtil.drawRoundedRect(
            context.getMatrices(), this.x, this.y, (float)this.width, (float)this.height, radius, ClickGui.getInstance().color.getValue()
         );
      }

      if (this.isListening) {
         this.drawString(
            "Press Key...", (double)(this.x + 5.0F), (double)(this.y - 2.0F - (float)ClickGuiScreen.getInstance().getTextOffset()), enableTextColor
         );
      } else {
         String str = this.setting.getKeyString();
         if (this.isListening || !hovered || !InputUtil.isKeyPressed(mc.getWindow().getHandle(), 340) || !this.setting.getName().equals("Key")) {
            this.drawString(
               this.setting.getName() + " " + Formatting.GRAY + str,
               (double)(this.x + 5.0F),
               (double)(this.y - 2.0F - (float)ClickGuiScreen.getInstance().getTextOffset()),
               this.getState() ? enableTextColor : defaultTextColor
            );
         } else if (this.setting.isHoldEnable()) {
            this.drawString(
               "§7Toggle/§fHold", (double)(this.x + 5.0F), (double)(this.y - 2.0F - (float)ClickGuiScreen.getInstance().getTextOffset()), enableTextColor
            );
         } else {
            this.drawString(
               "§fToggle§7/Hold", (double)(this.x + 5.0F), (double)(this.y - 2.0F - (float)ClickGuiScreen.getInstance().getTextOffset()), enableTextColor
            );
         }
      }
   }

   @Override
   public void update() {
      this.setHidden(!this.setting.isVisible());
   }

   @Override
   public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
      if (mouseButton == 0 && this.isHovering(mouseX, mouseY)) {
         if (InputUtil.isKeyPressed(mc.getWindow().getHandle(), 340) && this.setting.getName().equals("Key")) {
            this.setting.setHoldEnable(!this.setting.isHoldEnable());
            sound();
         } else {
            this.onMouseClick();
         }
      } else if (this.isListening) {
         this.setting.setValue(-(mouseButton + 1));
         this.onMouseClick();
      }
   }

   @Override
   public void onKeyPressed(int key) {
      if (this.isListening) {
         this.setting.setValue(key);
         if (this.setting.getKeyString().equalsIgnoreCase("DELETE")) {
            this.setting.setValue(-1);
         }

         this.onMouseClick();
      }
   }

   @Override
   public void toggle() {
      this.isListening = !this.isListening;
   }

   @Override
   public boolean getState() {
      return !this.isListening;
   }
}
