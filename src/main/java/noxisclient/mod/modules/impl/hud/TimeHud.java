package noxisclient.mod.modules.impl.hud;

import java.awt.Color;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import net.minecraft.client.gui.DrawContext;
import noxisclient.api.utils.render.TextUtil;
import noxisclient.mod.modules.HudElement;
import noxisclient.mod.modules.Module;
import noxisclient.mod.modules.settings.impl.BooleanSetting;
import noxisclient.mod.modules.settings.impl.ColorSetting;
import noxisclient.mod.modules.settings.impl.SliderSetting;

public class TimeHud extends Module implements HudElement {
   public static TimeHud INSTANCE;
   private int anchorX = 2;
   private int hudY = 98;
   private boolean isRightAligned = false;
   private boolean needsInitAnchor = false;
   private int loadedX = 2;
   private final SliderSetting size = this.add(new SliderSetting("Size", 1.0, 0.5, 3.0, 0.05));
   private final BooleanSetting shadow = this.add(new BooleanSetting("Shadow", true));
   private final ColorSetting color = this.add(new ColorSetting("Color", new Color(208, 0, 0)));

   public TimeHud() {
      super("Time", Module.Category.HUD);
      this.setChinese("时间显示");
      INSTANCE = this;
   }

   private String getText() {
      return "Time 搂f" + new SimpleDateFormat("h:mm a", Locale.ENGLISH).format(new Date());
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
      return this.isRightAligned ? this.anchorX - this.getElementWidth() : this.anchorX;
   }

   @Override
   public void setElementX(int x) {
      if (mc != null && mc.getWindow() != null) {
         this.needsInitAnchor = false;
         int screenWidth = mc.getWindow().getScaledWidth();
         int hudCenterX = x + this.getElementWidth() / 2;
         if (hudCenterX > screenWidth / 2) {
            this.isRightAligned = true;
            this.anchorX = x + this.getElementWidth();
         } else {
            this.isRightAligned = false;
            this.anchorX = x;
         }
      } else {
         this.loadedX = x;
         this.anchorX = x;
         this.needsInitAnchor = true;
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
   public void renderElement(DrawContext context, float tickDelta) {
      if (this.needsInitAnchor) {
         this.setElementX(this.loadedX);
      }

      TextUtil.drawHudString(
         context,
         this.getText(),
         (double)this.getElementX(),
         (double)this.hudY,
         this.color.getValue().getRGB(),
         this.shadow.getValue(),
         this.size.getValueFloat()
      );
   }
}
