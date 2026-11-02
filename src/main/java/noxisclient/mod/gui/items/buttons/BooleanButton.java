package noxisclient.mod.gui.items.buttons;

import java.awt.Color;
import net.minecraft.client.gui.DrawContext;
import noxisclient.api.utils.render.Render2DUtil;
import noxisclient.mod.gui.ClickGuiScreen;
import noxisclient.mod.modules.impl.client.ClickGui;
import noxisclient.mod.modules.settings.impl.BooleanSetting;

public class BooleanButton extends Button {
   private final BooleanSetting setting;

   public BooleanButton(BooleanSetting setting) {
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

      if (this.getState()) {
         Render2DUtil.drawRoundedRect(
            context.getMatrices(), this.x, this.y, (float)this.width, (float)this.height, radius, ClickGui.getInstance().color.getValue()
         );
      }

      this.drawString(
         this.getName(),
         (double)(this.x + 5.0F),
         (double)(this.y - 2.0F - (float)ClickGuiScreen.getInstance().getTextOffset()),
         this.getState() ? enableTextColor : defaultTextColor
      );
      if (this.setting.hasParent()) {
         this.drawString(
            this.setting.isOpen() ? "-" : "+",
            (double)(this.x + (float)this.width - 15.0F),
            (double)(this.y - 2.0F - (float)ClickGuiScreen.getInstance().getTextOffset()),
            new Color(150, 150, 200)
         );
      }
   }

   @Override
   public void update() {
      this.setHidden(!this.setting.isVisible());
   }

   @Override
   public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
      super.mouseClicked(mouseX, mouseY, mouseButton);
      if (mouseButton == 1 && this.isHovering(mouseX, mouseY)) {
         sound();
         this.setting.setOpen(!this.setting.isOpen());
      }
   }

   @Override
   public void toggle() {
      this.setting.setValue(!this.setting.getValue());
   }

   @Override
   public boolean getState() {
      return this.setting.getValue();
   }
}
