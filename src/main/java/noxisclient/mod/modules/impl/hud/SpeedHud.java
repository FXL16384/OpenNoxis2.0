package noxisclient.mod.modules.impl.hud;

import java.awt.Color;
import java.text.DecimalFormat;
import net.minecraft.client.gui.DrawContext;
import noxisclient.Alien;
import noxisclient.api.utils.render.ColorUtil;
import noxisclient.api.utils.render.TextUtil;
import noxisclient.mod.modules.HudElement;
import noxisclient.mod.modules.Module;
import noxisclient.mod.modules.settings.impl.BooleanSetting;
import noxisclient.mod.modules.settings.impl.ColorSetting;
import noxisclient.mod.modules.settings.impl.EnumSetting;
import noxisclient.mod.modules.settings.impl.SliderSetting;

public class SpeedHud extends Module implements HudElement {
   public static SpeedHud INSTANCE;
   private int hudX = 2;
   private int hudY = 86;
   private HudElement.AlignMode alignMode = HudElement.AlignMode.LEFT;
   private final SliderSetting size = this.add(new SliderSetting("Size", 1.0, 0.5, 3.0, 0.05));
   private final BooleanSetting shadow = this.add(new BooleanSetting("Shadow", true));
   private final ColorSetting color = this.add(new ColorSetting("Color", new Color(208, 0, 0)));
   private final EnumSetting<SpeedHud.ColorMode> colorMode = this.add(new EnumSetting<>("ColorMode", SpeedHud.ColorMode.Pulse));
   private final SliderSetting pulseSpeed = this.add(new SliderSetting("PulseSpeed", 1.0, 0.0, 5.0, 0.1));
   private final SliderSetting pulseCounter = this.add(new SliderSetting("Counter", 10, 1, 50));
   private final ColorSetting endColor = this.add(new ColorSetting("SecondColor", new Color(255, 0, 0)).injectBoolean(true));
   private final SliderSetting rainbowSpeed = this.add(new SliderSetting("RainbowSpeed", 4.0, 1.0, 10.0, 0.1));
   private final SliderSetting saturation = this.add(new SliderSetting("Saturation", 130, 1, 255));
   private final SliderSetting rainbowDelay = this.add(new SliderSetting("Delay", 350, 0, 1000));
   private final DecimalFormat df = new DecimalFormat("0.0");

   public SpeedHud() {
      super("SpeedHud", Module.Category.HUD);
      this.setChinese("速度显示");
      INSTANCE = this;
   }

   private String getText() {
      if (mc.player == null) {
         return "Speed 搂f0.0km/h";
      } else {
         double x = mc.player.getX() - mc.player.prevX;
         double z = mc.player.getZ() - mc.player.prevZ;
         double dist = Math.sqrt(x * x + z * z) / 1000.0;
         double div = 1.388888888888889E-5;
         float timer = Alien.TIMER.get();
         return "Speed 搂f" + this.df.format(dist / div * (double)timer) + "km/h";
      }
   }

   private Color getCurrentColor() {
      if (this.colorMode.is(SpeedHud.ColorMode.Pulse)) {
         return this.endColor.booleanValue
            ? ColorUtil.pulseColor(this.color.getValue(), this.endColor.getValue(), 0.0, this.pulseCounter.getValueInt(), this.pulseSpeed.getValue())
            : ColorUtil.pulseColor(this.color.getValue(), 0.0, this.pulseCounter.getValueInt(), this.pulseSpeed.getValue());
      } else if (this.colorMode.is(SpeedHud.ColorMode.Rainbow)) {
         double state = Math.ceil(((double)System.currentTimeMillis() * this.rainbowSpeed.getValue() + 20.0 * this.rainbowDelay.getValue()) / 20.0);
         Color rainbow = Color.getHSBColor((float)(state % 360.0 / 360.0), this.saturation.getValueFloat() / 255.0F, 1.0F);
         return ColorUtil.injectAlpha(rainbow, this.color.getValue().getAlpha());
      } else {
         return this.color.getValue();
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
         context, this.getText(), (double)renderX, (double)this.hudY, this.getCurrentColor().getRGB(), this.shadow.getValue(), this.size.getValueFloat()
      );
   }

   private static enum ColorMode {
      Custom,
      Pulse,
      Rainbow;
   }
}
