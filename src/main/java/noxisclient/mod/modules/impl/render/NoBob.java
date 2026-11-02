package noxisclient.mod.modules.impl.render;

import noxisclient.api.events.impl.TickEvent;
import noxisclient.mod.modules.Module;
import noxisclient.mod.modules.settings.impl.SliderSetting;

public class NoBob extends Module {
   public static NoBob INSTANCE;
   public final SliderSetting multiplier = this.add(new SliderSetting("Multiplier", 0.0, 0.0, 1.0, 0.01));

   public NoBob() {
      super("NoBob", "Prevents the bobbing animation.", Module.Category.Render);
      INSTANCE = this;
   }

   public void onTick(TickEvent event) {
      if (!event.isPre() && mc.player != null) {
         mc.player.horizontalSpeed = mc.player.horizontalSpeed * this.multiplier.getValueFloat();
      }
   }
}
