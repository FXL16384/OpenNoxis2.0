package noxisclient.mod.modules.impl.misc;

import noxisclient.Alien;
import noxisclient.api.events.impl.ServerRotationEvent;
import noxisclient.mod.modules.Module;
import noxisclient.mod.modules.settings.impl.BooleanSetting;

public class NoRotate extends Module {
   public static NoRotate INSTANCE;
   private final BooleanSetting positionAdjust = this.add(new BooleanSetting("PositionAdjust", false));

   public NoRotate() {
      super("NoRotate", "Prevents server from forcing rotations", Module.Category.Misc);
      this.setChinese("不转");
      INSTANCE = this;
   }

   public void onServerRotation(ServerRotationEvent event) {
      event.cancel();
      if (this.positionAdjust.getValue()) {
         float yaw = Alien.ROTATION.getLastYaw();
         float pitch = Alien.ROTATION.getLastPitch();
         event.setYaw(yaw);
         event.setPitch(pitch);
      }
   }
}
