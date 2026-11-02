package noxisclient.mod.modules.impl.hud;

import java.awt.Color;
import net.minecraft.client.gui.DrawContext;
import noxisclient.api.events.impl.PacketEvent;
import noxisclient.api.utils.math.Timer;
import noxisclient.api.utils.render.TextUtil;
import noxisclient.mod.modules.HudElement;
import noxisclient.mod.modules.Module;
import noxisclient.mod.modules.settings.impl.BooleanSetting;
import noxisclient.mod.modules.settings.impl.ColorSetting;
import noxisclient.mod.modules.settings.impl.SliderSetting;

public class PacketHud extends Module implements HudElement {
   public static PacketHud INSTANCE;
   private int hudX = 2;
   private int hudY = 122;
   private final SliderSetting size = this.add(new SliderSetting("Size", 1.0, 0.5, 3.0, 0.05));
   private final BooleanSetting shadow = this.add(new BooleanSetting("Shadow", true));
   private final ColorSetting color = this.add(new ColorSetting("Color", new Color(208, 0, 0)));
   private int incomingPackets = 0;
   private int outgoingPackets = 0;
   private int lastIn = 0;
   private int lastOut = 0;
   private final Timer resetTimer = new Timer();

   public PacketHud() {
      super("Packets", Module.Category.HUD);
      this.setChinese("数据包显示");
      INSTANCE = this;
   }

   private String getText() {
      return "Packets 搂f" + this.lastIn + "<-" + this.lastOut;
   }

   public void onReceive(PacketEvent.Receive e) {
      this.incomingPackets++;
   }

   public void onSend(PacketEvent.Send e) {
      this.outgoingPackets++;
   }

   @Override
   public void onRender2D(DrawContext drawContext, float tickDelta) {
      if (this.resetTimer.passedMs(150.0)) {
         this.lastIn = this.incomingPackets;
         this.lastOut = this.outgoingPackets;
         this.incomingPackets = 0;
         this.outgoingPackets = 0;
         this.resetTimer.reset();
      }

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
}
