package noxisclient.mod.gui.click;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

public abstract class AbstractComponent {
   protected static MinecraftClient mc;
   protected float x;
   protected float y;
   protected float width;
   protected float height;
   protected boolean visible = true;
   protected boolean hovered = false;

   public AbstractComponent(float x, float y, float width, float height) {
      this.x = x;
      this.y = y;
      this.width = width;
      this.height = height;
      if (mc == null) {
         mc = MinecraftClient.getInstance();
      }
   }

   public abstract void render(DrawContext var1, int var2, int var3, float var4);

   public abstract void mouseClicked(double var1, double var3, int var5);

   public abstract void mouseReleased(double var1, double var3, int var5);

   public abstract void keyPressed(int var1, int var2, int var3);

   public abstract void charTyped(char var1, int var2);

   public boolean isHovered(double mouseX, double mouseY) {
      return mouseX >= (double)this.x && mouseX <= (double)(this.x + this.width) && mouseY >= (double)this.y && mouseY <= (double)(this.y + this.height);
   }

   public void updateHovered(double mouseX, double mouseY) {
      this.hovered = this.isHovered(mouseX, mouseY);
   }

   public float getX() {
      return this.x;
   }

   public void setX(float x) {
      this.x = x;
   }

   public float getY() {
      return this.y;
   }

   public void setY(float y) {
      this.y = y;
   }

   public float getWidth() {
      return this.width;
   }

   public void setWidth(float width) {
      this.width = width;
   }

   public float getHeight() {
      return this.height;
   }

   public void setHeight(float height) {
      this.height = height;
   }

   public boolean isVisible() {
      return this.visible;
   }

   public void setVisible(boolean visible) {
      this.visible = visible;
   }

   public boolean isHovered() {
      return this.hovered;
   }
}
