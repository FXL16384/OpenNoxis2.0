package noxisclient.mod.modules.impl.hud;

import java.awt.Color;
import net.minecraft.client.gui.DrawContext;
import noxisclient.api.utils.render.ColorUtil;
import noxisclient.api.utils.render.TextUtil;
import noxisclient.mod.modules.HudElement;
import noxisclient.mod.modules.Module;
import noxisclient.mod.modules.settings.impl.BooleanSetting;
import noxisclient.mod.modules.settings.impl.ColorSetting;
import noxisclient.mod.modules.settings.impl.EnumSetting;
import noxisclient.mod.modules.settings.impl.SliderSetting;

public class WelcomerHud extends Module implements HudElement {
   public static WelcomerHud INSTANCE;
   private int hudX = -1;
   private int hudY = 2;
   private final SliderSetting size = this.add(new SliderSetting("Size", 1.0, 0.5, 3.0, 0.05));
   private final BooleanSetting shadow = this.add(new BooleanSetting("Shadow", true));
   private final ColorSetting color = this.add(new ColorSetting("Color", new Color(208, 0, 0)));
   private final EnumSetting<WelcomerHud.ColorMode> colorMode = this.add(new EnumSetting<>("ColorMode", WelcomerHud.ColorMode.Pulse));
   private final SliderSetting pulseSpeed = this.add(new SliderSetting("PulseSpeed", 1.0, 0.0, 5.0, 0.1));
   private final SliderSetting pulseCounter = this.add(new SliderSetting("Counter", 10, 1, 50));
   private final ColorSetting endColor = this.add(new ColorSetting("SecondColor", new Color(255, 0, 0)).injectBoolean(true));
   private final SliderSetting rainbowSpeed = this.add(new SliderSetting("RainbowSpeed", 4.0, 1.0, 10.0, 0.1));
   private final SliderSetting saturation = this.add(new SliderSetting("Saturation", 130, 1, 255));
   private final SliderSetting rainbowDelay = this.add(new SliderSetting("Delay", 350, 0, 1000));

   public WelcomerHud() {
      super("Welcomer", Module.Category.HUD);
      this.setChinese("欢迎词");
      INSTANCE = this;
   }

   private String getText() {
      return mc.player == null ? "Welcome :')" : "Welcome, 搂f" + mc.player.getName().getString() + " 搂r:')";
   }

   private Color getCurrentColor() {
      if (this.colorMode.is(WelcomerHud.ColorMode.Pulse)) {
         return this.endColor.booleanValue
            ? ColorUtil.pulseColor(this.color.getValue(), this.endColor.getValue(), 0.0, this.pulseCounter.getValueInt(), this.pulseSpeed.getValue())
            : ColorUtil.pulseColor(this.color.getValue(), 0.0, this.pulseCounter.getValueInt(), this.pulseSpeed.getValue());
      } else if (this.colorMode.is(WelcomerHud.ColorMode.Rainbow)) {
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
      return this.hudX < 0 ? mc.getWindow().getScaledWidth() / 2 - this.getElementWidth() / 2 : this.hudX;
   }

   @Override
   public void setElementX(int x) {
      this.hudX = x;
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
      TextUtil.drawHudString(
         context,
         this.getText(),
         (double)this.getElementX(),
         (double)this.getElementY(),
         this.getCurrentColor().getRGB(),
         this.shadow.getValue(),
         this.size.getValueFloat()
      );
   }

   private static enum ColorMode {
      Custom,
      Pulse,
      Rainbow;
   }
}
