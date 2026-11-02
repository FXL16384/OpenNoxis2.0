package noxisclient.mod.modules.impl.hud;

import java.awt.Color;
import net.minecraft.client.gui.DrawContext;
import noxisclient.api.utils.render.TextUtil;
import noxisclient.mod.modules.HudElement;
import noxisclient.mod.modules.Module;
import noxisclient.mod.modules.settings.impl.BooleanSetting;
import noxisclient.mod.modules.settings.impl.ColorSetting;
import noxisclient.mod.modules.settings.impl.SliderSetting;

public class DirectionHud extends Module implements HudElement {
   public static DirectionHud INSTANCE;
   private int hudX = 2;
   private int hudY = -1;
   private final SliderSetting size = this.add(new SliderSetting("Size", 1.0, 0.5, 3.0, 0.05));
   private final BooleanSetting shadow = this.add(new BooleanSetting("Shadow", true));
   private final ColorSetting color = this.add(new ColorSetting("Color", new Color(208, 0, 0)));

   public DirectionHud() {
      super("Direction", Module.Category.HUD);
      this.setChinese("方向显示");
      INSTANCE = this;
   }

   private String getText() {
      if (mc.player == null) {
         return "North 搂7[搂f-Z搂7]";
      } else {
         float yaw = mc.player.getYaw();
         yaw = (yaw % 360.0F + 360.0F) % 360.0F;
         String f;
         String a;
         if ((double)yaw >= 337.5 || (double)yaw < 22.5) {
            f = "South";
            a = "+Z";
         } else if ((double)yaw < 67.5) {
            f = "Southwest";
            a = "-X +Z";
         } else if ((double)yaw < 112.5) {
            f = "West";
            a = "-X";
         } else if ((double)yaw < 157.5) {
            f = "Northwest";
            a = "-X -Z";
         } else if ((double)yaw < 202.5) {
            f = "North";
            a = "-Z";
         } else if ((double)yaw < 247.5) {
            f = "Northeast";
            a = "+X -Z";
         } else if ((double)yaw < 292.5) {
            f = "East";
            a = "+X";
         } else {
            f = "Southeast";
            a = "+X +Z";
         }

         return f + " 搂7[搂f" + a + "搂7]";
      }
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
      return this.hudX;
   }

   @Override
   public void setElementX(int x) {
      this.hudX = x;
   }

   @Override
   public int getElementY() {
      return this.hudY < 0 ? mc.getWindow().getScaledHeight() - this.getElementHeight() * 2 : this.hudY;
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
   public void renderElement(DrawContext context, float tickDelta) {
      TextUtil.drawHudString(
         context,
         this.getText(),
         (double)this.getElementX(),
         (double)this.getElementY(),
         this.color.getValue().getRGB(),
         this.shadow.getValue(),
         this.size.getValueFloat()
      );
   }
}
