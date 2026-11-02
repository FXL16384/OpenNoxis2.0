package noxisclient.mod.modules.impl.hud;

import java.awt.Color;
import net.minecraft.client.gui.DrawContext;
import noxisclient.api.utils.render.TextUtil;
import noxisclient.mod.modules.HudElement;
import noxisclient.mod.modules.Module;
import noxisclient.mod.modules.settings.impl.BooleanSetting;
import noxisclient.mod.modules.settings.impl.ColorSetting;
import noxisclient.mod.modules.settings.impl.EnumSetting;
import noxisclient.mod.modules.settings.impl.SliderSetting;

public class ServerInfoHud extends Module implements HudElement {
   public static ServerInfoHud INSTANCE;
   private int hudX = 2;
   private int hudY = 110;
   private final EnumSetting<ServerInfoHud.Mode> mode = this.add(new EnumSetting<>("Mode", ServerInfoHud.Mode.IP));
   private final SliderSetting size = this.add(new SliderSetting("Size", 1.0, 0.5, 3.0, 0.05));
   private final BooleanSetting shadow = this.add(new BooleanSetting("Shadow", true));
   private final ColorSetting color = this.add(new ColorSetting("Color", new Color(208, 0, 0)));

   public ServerInfoHud() {
      super("ServerInfo", Module.Category.HUD);
      this.setChinese("服务器信息");
      INSTANCE = this;
   }

   private String getText() {
      return this.mode.getValue() == ServerInfoHud.Mode.IP
         ? "Server 搂f" + (mc.isInSingleplayer() ? "SinglePlayer" : (mc.getCurrentServerEntry() != null ? mc.getCurrentServerEntry().address : "Unknown"))
         : "Brand 搂f"
            + (mc.isInSingleplayer() ? "Vanilla" : (mc.getNetworkHandler() != null ? mc.getNetworkHandler().getBrand().replaceAll("\\(.*?\\)", "") : "Unknown"));
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
         context, this.getText(), (double)this.hudX, (double)this.hudY, this.color.getValue().getRGB(), this.shadow.getValue(), this.size.getValueFloat()
      );
   }

   private static enum Mode {
      IP,
      Brand;
   }
}
