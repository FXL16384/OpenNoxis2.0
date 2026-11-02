package noxisclient.mod.modules.impl.hud;

import java.awt.Color;
import net.minecraft.client.gui.DrawContext;
import noxisclient.Alien;
import noxisclient.api.utils.render.TextUtil;
import noxisclient.mod.modules.HudElement;
import noxisclient.mod.modules.Module;
import noxisclient.mod.modules.settings.impl.BooleanSetting;
import noxisclient.mod.modules.settings.impl.ColorSetting;
import noxisclient.mod.modules.settings.impl.SliderSetting;

public class TPSHud extends Module implements HudElement {
   public static TPSHud INSTANCE;
   private int hudX = 2;
   private int hudY = 74;
   private HudElement.AlignMode alignMode = HudElement.AlignMode.LEFT;
   private final SliderSetting size = this.add(new SliderSetting("Size", 1.0, 0.5, 3.0, 0.05));
   private final BooleanSetting shadow = this.add(new BooleanSetting("Shadow", true));
   private final ColorSetting color = this.add(new ColorSetting("Color", new Color(208, 0, 0)));

   public TPSHud() {
      super("TPS", Module.Category.HUD);
      this.setChinese("TPS显示");
      INSTANCE = this;
   }

   private String getText() {
      return "TPS 搂f" + Alien.SERVER.getTPS() + " [" + Alien.SERVER.getCurrentTPS() + "]";
   }

   @Override
   public void onRender2D(DrawContext drawContext, float tickDelta) {
      this.renderElement(drawContext, tickDelta);
   }

   @Override
   public String getElementName() {
      return this.getChinese() != null ? this.getChinese() : this.getName();
   }

   @Override
   public int getElementX() {
      return this.alignMode == HudElement.AlignMode.RIGHT ? this.hudX - this.getElementWidth() : this.hudX;
   }

   @Override
   public void setElementX(int x) {
      if (this.alignMode == HudElement.AlignMode.RIGHT) {
         this.hudX = x + this.getElementWidth();
      } else {
         this.hudX = x;
      }
   }

   @Override
   public int getElementY() {
      return this.hudY;
   }

   @Override
   public void setElementY(int y) {
      this.hudY = y;
   }

   @Override
   public int getElementWidth() {
      return (int)Math.ceil((double)TextUtil.getHudWidth(this.getText(), this.size.getValueFloat())) + 4;
   }

   @Override
   public int getElementHeight() {
      return (int)Math.ceil((double)TextUtil.getHudHeight(this.size.getValueFloat())) + 2;
   }

   @Override
   public boolean isElementVisible() {
      return this.isOn();
   }

   @Override
   public HudElement.AlignMode getAlignMode() {
      return this.alignMode;
   }

   @Override
   public void setAlignMode(HudElement.AlignMode mode) {
      if (mode != this.alignMode) {
         if (mode == HudElement.AlignMode.RIGHT && this.alignMode == HudElement.AlignMode.LEFT) {
            this.hudX = this.hudX + this.getElementWidth();
         } else if (mode == HudElement.AlignMode.LEFT && this.alignMode == HudElement.AlignMode.RIGHT) {
            this.hudX = this.hudX - this.getElementWidth();
         }

         this.alignMode = mode;
      }
   }

   @Override
   public int getRightAnchorX() {
      return this.alignMode == HudElement.AlignMode.RIGHT ? this.hudX : this.hudX + this.getElementWidth();
   }

   @Override
   public void setRightAnchorX(int rightX) {
      if (this.alignMode == HudElement.AlignMode.RIGHT) {
         this.hudX = rightX;
      } else {
         this.hudX = rightX - this.getElementWidth();
      }
   }

   @Override
   public void renderElement(DrawContext context, float tickDelta) {
      int renderX = this.getElementX();
      TextUtil.drawHudString(
         context, this.getText(), (double)renderX, (double)this.hudY, this.color.getValue().getRGB(), this.shadow.getValue(), this.size.getValueFloat()
      );
   }
}
