package noxisclient.mod.modules.impl.render;

import noxisclient.Vitality;
import noxisclient.api.events.impl.Render3DEvent;
import noxisclient.api.utils.math.Animation;
import noxisclient.api.utils.math.Easing;
import noxisclient.mod.modules.Module;
import noxisclient.mod.modules.settings.impl.EnumSetting;
import noxisclient.mod.modules.settings.impl.SliderSetting;

public class Zoom extends Module {
   public static Zoom INSTANCE;
   public static boolean on = false;
   public final EnumSetting<Easing> ease = this.add(new EnumSetting<>("Ease", Easing.CubicInOut));
   final SliderSetting fov = this.add(new SliderSetting("ZoomFov", 60.0, 0.0, 130.0, 1.0));
   final Animation animation = new Animation();
   private final SliderSetting animTime = this.add(new SliderSetting("AnimTime", 300, 0, 1000));
   public double currentFov;

   public Zoom() {
      super("Zoom", Module.Category.Render);
      this.setChinese("放大");
      INSTANCE = this;
      Vitality.EVENT_BUS.subscribe(new Zoom.ZoomAnim());
   }

   @Override
   public void onEnable() {
      if (nullCheck()) {
         this.disable();
      }
   }

   public class ZoomAnim {
      public void onRender3D(Render3DEvent event) {
         if (Zoom.this.isOn()) {
            Zoom.this.currentFov = Zoom.this.animation.get(Zoom.this.fov.getValue(), (long)Zoom.this.animTime.getValueInt(), Zoom.this.ease.getValue());
            Zoom.on = true;
         } else if (Zoom.on) {
            Zoom.this.currentFov = Zoom.this.animation.get(0.0, (long)Zoom.this.animTime.getValueInt(), Zoom.this.ease.getValue());
            if ((int)Zoom.this.currentFov == 0) {
               Zoom.on = false;
            }
         }
      }
   }
}
