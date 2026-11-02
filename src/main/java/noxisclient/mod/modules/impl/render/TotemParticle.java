package noxisclient.mod.modules.impl.render;

import java.awt.Color;
import java.util.Random;
import noxisclient.api.events.impl.TotemParticleEvent;
import noxisclient.api.utils.render.ColorUtil;
import noxisclient.mod.modules.Module;
import noxisclient.mod.modules.settings.impl.ColorSetting;
import noxisclient.mod.modules.settings.impl.SliderSetting;

public class TotemParticle extends Module {
   public static TotemParticle INSTANCE;
   public final SliderSetting velocityXZ = this.add(new SliderSetting("VelocityXZ", 100.0, 0.0, 500.0, 1.0).setSuffix("%"));
   public final SliderSetting velocityY = this.add(new SliderSetting("VelocityY", 100.0, 0.0, 500.0, 1.0).setSuffix("%"));
   final Random random = new Random();
   private final ColorSetting color = this.add(new ColorSetting("Color", new Color(255, 255, 255, 255)));
   private final ColorSetting color2 = this.add(new ColorSetting("Color2", new Color(0, 0, 0, 255)));

   public TotemParticle() {
      super("TotemParticle", Module.Category.Render);
      this.setChinese("自定义图腾粒子");
      INSTANCE = this;
   }

   public void idk(TotemParticleEvent event) {
      event.cancel();
      event.velocityZ = event.velocityZ * (this.velocityXZ.getValue() / 100.0);
      event.velocityX = event.velocityX * (this.velocityXZ.getValue() / 100.0);
      event.velocityY = event.velocityY * (this.velocityY.getValue() / 100.0);
      event.color = ColorUtil.fadeColor(this.color.getValue(), this.color2.getValue(), this.random.nextDouble());
   }
}
