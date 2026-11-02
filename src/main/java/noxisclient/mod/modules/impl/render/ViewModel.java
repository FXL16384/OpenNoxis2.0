package noxisclient.mod.modules.impl.render;

import net.minecraft.util.Hand;
import net.minecraft.util.math.RotationAxis;
import noxisclient.api.events.impl.HeldItemRendererEvent;
import noxisclient.mod.modules.Module;
import noxisclient.mod.modules.settings.impl.SliderSetting;

public class ViewModel extends Module {
   public static ViewModel INSTANCE;
   public final SliderSetting scaleX = this.add(new SliderSetting("ScaleX", 1.0, 0.1F, 5.0, 0.1));
   public final SliderSetting scaleY = this.add(new SliderSetting("ScaleY", 1.0, 0.1F, 5.0, 0.1));
   public final SliderSetting scaleZ = this.add(new SliderSetting("ScaleZ", 1.0, 0.1F, 5.0, 0.1));
   public final SliderSetting positionX = this.add(new SliderSetting("PositionX", 0.0, -3.0, 3.0, 0.1));
   public final SliderSetting positionY = this.add(new SliderSetting("PositionY", 0.0, -3.0, 3.0, 0.1));
   public final SliderSetting positionZ = this.add(new SliderSetting("PositionZ", 0.0, -3.0, 3.0, 0.1));
   public final SliderSetting rotationX = this.add(new SliderSetting("RotationX", 0.0, -180.0, 180.0, 0.1));
   public final SliderSetting rotationY = this.add(new SliderSetting("RotationY", 0.0, -180.0, 180.0, 0.1));
   public final SliderSetting rotationZ = this.add(new SliderSetting("RotationZ", 0.0, -180.0, 180.0, 0.1));

   public ViewModel() {
      super("ViewModel", Module.Category.Render);
      this.setChinese("手持模型");
      INSTANCE = this;
   }

   public void onHeldItemRender(HeldItemRendererEvent event) {
      if (event.getHand() == Hand.MAIN_HAND) {
         event.getStack().translate(-this.positionX.getValueFloat(), this.positionY.getValueFloat(), this.positionZ.getValueFloat());
         event.getStack().scale(this.scaleX.getValueFloat(), this.scaleY.getValueFloat(), this.scaleZ.getValueFloat());
         event.getStack().multiply(RotationAxis.POSITIVE_X.rotationDegrees(-this.rotationX.getValueFloat()));
         event.getStack().multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-this.rotationY.getValueFloat()));
         event.getStack().multiply(RotationAxis.POSITIVE_Z.rotationDegrees(-this.rotationZ.getValueFloat()));
      } else {
         event.getStack().translate(this.positionX.getValueFloat(), this.positionY.getValueFloat(), this.positionZ.getValueFloat());
         event.getStack().scale(this.scaleX.getValueFloat(), this.scaleY.getValueFloat(), this.scaleZ.getValueFloat());
         event.getStack().multiply(RotationAxis.POSITIVE_X.rotationDegrees(this.rotationX.getValueFloat()));
         event.getStack().multiply(RotationAxis.POSITIVE_Y.rotationDegrees(this.rotationY.getValueFloat()));
         event.getStack().multiply(RotationAxis.POSITIVE_Z.rotationDegrees(this.rotationZ.getValueFloat()));
      }
   }
}
