package noxisclient.mod.gui.click;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;

public abstract class Component {
   protected static final MinecraftClient mc = MinecraftClient.getInstance();
   protected float x;
   protected float y;
   protected float width;
   protected float height;
   protected boolean visible = true;

   public Component(float x, float y, float width, float height) {
      this.x = x;
      this.y = y;
      this.width = width;
      this.height = height;
   }

   public abstract void render(DrawContext var1, float var2, float var3, float var4);

   public abstract void mouseClicked(double var1, double var3, int var5);

   public abstract void mouseReleased(double var1, double var3, int var5);

   public abstract void keyPressed(int var1, int var2, int var3);

   public abstract void charTyped(char var1, int var2);

   public boolean isWithin(double mouseX, double mouseY) {
      return this.isWithin((float)mouseX, (float)mouseY);
   }

   public boolean isWithin(float mouseX, float mouseY) {
      return mouseX >= this.x && mouseX <= this.x + this.width && mouseY >= this.y && mouseY <= this.y + this.height;
   }

   public static boolean isMouseOver(double mouseX, double mouseY, double x, double y, double width, double height) {
      return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
   }

   public void fill(DrawContext context, float x1, float y1, float x2, float y2, int color) {
      context.fill((int)x1, (int)y1, (int)x2, (int)y2, color);
   }

   public void rect(DrawContext context, int color) {
      this.fill(context, this.x, this.y, this.x + this.width, this.y + this.height, color);
   }

   public void drawString(DrawContext context, String text, float x, float y, int color) {
      context.drawText(mc.textRenderer, text, (int)x, (int)y, color, false);
   }

   public void drawStringScaled(DrawContext context, String text, float x, float y, int color) {
      MatrixStack matrices = context.getMatrices();
      matrices.push();
      float scale = getScale();
      matrices.scale(scale, scale, scale);
      context.drawText(mc.textRenderer, text, (int)(x / scale), (int)(y / scale), color, false);
      matrices.pop();
   }

   public float textWidth(String text) {
      return (float)mc.textRenderer.getWidth(text);
   }

   public float textHeight() {
      return 9.0F;
   }

   public static float getScale() {
      return 1.0F;
   }

   public float getX() {
      return this.x;
   }

   public float getY() {
      return this.y;
   }

   public float getWidth() {
      return this.width * getScale();
   }

   public float getHeight() {
      return this.height * getScale();
   }

   public void setX(float x) {
      this.x = x;
   }

   public void setY(float y) {
      this.y = y;
   }

   public void setWidth(float width) {
      this.width = width;
   }

   public void setHeight(float height) {
      this.height = height;
   }

   public void setPos(float x, float y) {
      this.x = x;
      this.y = y;
   }

   public boolean isVisible() {
      return this.visible;
   }

   public void setVisible(boolean visible) {
      this.visible = visible;
   }
}
