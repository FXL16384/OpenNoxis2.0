package noxisclient.mod.gui.click;

import net.minecraft.client.gui.DrawContext;
import noxisclient.Alien;
import noxisclient.api.utils.render.Render2DUtil;
import noxisclient.mod.modules.impl.client.ClickGui;
import noxisclient.mod.modules.impl.client.Fonts;
import noxisclient.mod.modules.settings.Setting;

public abstract class SettingButton extends AbstractComponent {
   protected final Setting setting;
   protected final ModuleButton parent;
   protected static final float SCALE = 1.0F;
   protected boolean lastHovered = false;
   private float shaderClipBottom = Float.NaN;

   public SettingButton(Setting setting, ModuleButton parent, float x, float y, float width, float height) {
      super(x, y, width, height);
      this.setting = setting;
      this.parent = parent;
   }

   public Setting getSetting() {
      return this.setting;
   }

   public ModuleButton getParent() {
      return this.parent;
   }

   public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
   }

   public void renderForeground(DrawContext context, int mouseX, int mouseY, float delta) {
   }

   public void setShaderClipBottom(float shaderClipBottom) {
      this.shaderClipBottom = shaderClipBottom;
   }

   public void clearShaderClip() {
      this.shaderClipBottom = Float.NaN;
   }

   protected boolean shouldUseShaderBackground() {
      ClickGui gui = ClickGui.getInstance();
      return Alien.SHADER != null && gui != null && gui.shader.getValue();
   }

   protected boolean isParentModuleEnabled() {
      return this.parent != null && this.parent.getModule().isOn();
   }

   protected void queueShaderBackground(DrawContext context, boolean enabled) {
      this.queueShaderBackground(context, enabled, this.x, this.y, this.width * 1.0F, this.height * 1.0F);
   }

   protected void queueShaderBackground(DrawContext context, boolean enabled, float rectX, float rectY, float rectWidth, float rectHeight) {
      if (this.shouldUseShaderBackground() && !(rectWidth <= 0.0F) && !(rectHeight <= 0.0F)) {
         if (!Float.isNaN(this.shaderClipBottom)) {
            rectHeight = Math.min(rectHeight, this.shaderClipBottom - rectY);
         }

         if (!(rectHeight <= 0.0F)) {
            ModuleButton.queueGuiShaderRect(enabled, context, rectX, rectY, rectWidth, rectHeight);
         }
      }
   }

   protected void rect(DrawContext context, float x, float y, float width, float height, int color) {
      if (ClickGui.getInstance().isThunderStyle()) {
         int alpha = color >>> 24 & 0xFF;
         if (alpha > 0) {
            context.fill((int)x, (int)y, (int)(x + width), (int)(y + height), color);
         }
      } else {
         Render2DUtil.drawRect(context.getMatrices(), x, y, width, height, color);
      }
   }

   protected void fill(DrawContext context, float x, float y, float width, float height, int color) {
      if (ClickGui.getInstance().isThunderStyle()) {
         context.fill((int)x, (int)y, (int)(x + width), (int)(y + height), color);
      } else {
         Render2DUtil.drawRect(context.getMatrices(), x, y, width, height, color);
      }
   }

   protected boolean isWithin(double mouseX, double mouseY) {
      return mouseX >= (double)this.x
         && mouseX <= (double)(this.x + this.width * 1.0F)
         && mouseY >= (double)this.y
         && mouseY <= (double)(this.y + this.height * 1.0F);
   }

   protected int getHoverAlpha(double mouseX, double mouseY) {
      boolean hovered = this.isWithin(mouseX, mouseY);
      return hovered ? 80 : 0;
   }

   protected void drawString(DrawContext context, String text, float x, float y, int color) {
      ClickGui gui = ClickGui.getInstance();
      if (gui.isThunderStyle()) {
         y -= 3.0F;
      }

      float clipX = Math.max(this.x, x);
      float clipRight = this.x + this.width * 1.0F - 2.0F;
      boolean shadow = Fonts.useClickGuiFont() && gui.shadow.getValue();
      ClickGuiText.drawMarquee(context, text, x, y, clipX, clipRight - clipX, color, shadow);
   }
}
