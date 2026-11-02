package noxisclient.mod.gui.items.buttons;

import java.awt.Color;
import net.minecraft.client.gui.DrawContext;
import noxisclient.api.utils.render.ColorUtil;
import noxisclient.api.utils.render.Render2DUtil;
import noxisclient.mod.gui.ClickGuiScreen;
import noxisclient.mod.gui.items.Component;
import noxisclient.mod.gui.items.Item;
import noxisclient.mod.modules.impl.client.ClickGui;

public class Button extends Item {
   private boolean state;
   public static int hoverColor = -2007673515;
   public static int defaultColor = 290805077;
   public static int defaultTextColor = -5592406;
   public static int enableTextColor = -1;

   public Button(String name) {
      super(name);
      this.setHeight(15);
   }

   @Override
   public void drawScreen(DrawContext context, int mouseX, int mouseY, float partialTicks) {
      Color color = ClickGui.getInstance().color.getValue();
      Render2DUtil.rect(
         context.getMatrices(),
         this.x,
         this.y,
         this.x + (float)this.width,
         this.y + (float)this.height - 0.5F,
         this.getState()
            ? (!this.isHovering(mouseX, mouseY) ? ColorUtil.injectAlpha(color, 220).getRGB() : ColorUtil.injectAlpha(color, 255).getRGB())
            : (!this.isHovering(mouseX, mouseY) ? defaultColor : hoverColor)
      );
      this.drawString(
         this.getName(),
         (double)(this.x + 2.3F),
         (double)(this.y - 2.0F - (float)ClickGuiScreen.getInstance().getTextOffset()),
         this.getState() ? enableTextColor : defaultTextColor
      );
   }

   @Override
   public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
      if (mouseButton == 0 && this.isHovering(mouseX, mouseY)) {
         this.onMouseClick();
      }
   }

   public void onMouseClick() {
      this.state = !this.state;
      this.toggle();
      sound();
   }

   public void toggle() {
   }

   public boolean getState() {
      return this.state;
   }

   @Override
   public int getHeight() {
      return this.height - 1;
   }

   public boolean isHovering(int mouseX, int mouseY) {
      for (Component component : ClickGuiScreen.getInstance().getComponents()) {
         if (component.drag) {
            return false;
         }
      }

      return (float)mouseX >= this.getX()
         && (float)mouseX <= this.getX() + (float)this.getWidth()
         && (float)mouseY >= this.getY()
         && (float)mouseY <= this.getY() + (float)this.height - 1.0F;
   }
}
