package noxisclient.mod.modules.impl.render;

import net.minecraft.client.option.Perspective;
import net.minecraft.client.util.math.MatrixStack;
import noxisclient.api.utils.math.Easing;
import noxisclient.api.utils.math.FadeUtils;
import noxisclient.mod.modules.Module;
import noxisclient.mod.modules.settings.impl.BooleanSetting;
import noxisclient.mod.modules.settings.impl.EnumSetting;
import noxisclient.mod.modules.settings.impl.SliderSetting;

public class CameraClip extends Module {
   public static CameraClip INSTANCE;
   public final SliderSetting getDistance = this.add(new SliderSetting("Distance", 4.0, 1.0, 20.0));
   public final SliderSetting animateTime = this.add(new SliderSetting("AnimationTime", 200, 0, 1000));
   private final EnumSetting<Easing> ease = this.add(new EnumSetting<>("Ease", Easing.CubicInOut));
   final FadeUtils animation = new FadeUtils(300L);
   private final BooleanSetting noFront = this.add(new BooleanSetting("NoFront", false));
   boolean first = false;

   public CameraClip() {
      super("CameraClip", Module.Category.Render);
      this.setChinese("摄像机穿墙");
      INSTANCE = this;
   }

   @Override
   public void onRender3D(MatrixStack matrixStack) {
      if (mc.options.getPerspective() == Perspective.THIRD_PERSON_FRONT && this.noFront.getValue()) {
         mc.options.setPerspective(Perspective.FIRST_PERSON);
      }

      this.animation.setLength((long)this.animateTime.getValueInt());
      if (mc.options.getPerspective() == Perspective.FIRST_PERSON) {
         if (!this.first) {
            this.first = true;
            this.animation.reset();
         }
      } else if (this.first) {
         this.first = false;
         this.animation.reset();
      }
   }

   public double getDistance() {
      double quad = mc.options.getPerspective() == Perspective.FIRST_PERSON
         ? 1.0 - this.animation.ease(this.ease.getValue())
         : this.animation.ease(this.ease.getValue());
      return this.getDistance.getValue() * quad - 1.0 + 1.0;
   }
}
