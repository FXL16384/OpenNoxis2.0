package noxisclient.mod.modules.impl.movement;

import noxisclient.api.events.impl.UpdateEvent;
import noxisclient.api.utils.path.BaritoneUtil;
import noxisclient.mod.modules.Module;
import noxisclient.mod.modules.settings.impl.EnumSetting;

public class AutoWalk extends Module {
   public static AutoWalk INSTANCE;
   private final EnumSetting<AutoWalk.Mode> mode = this.add(new EnumSetting<>("Mode", AutoWalk.Mode.Forward));
   boolean start = false;

   public AutoWalk() {
      super("AutoWalk", Module.Category.Movement);
      this.setChinese("自动前进");
      INSTANCE = this;
   }

   @Override
   public void onEnable() {
      this.start = false;
   }

   @Override
   public void onLogout() {
      this.disable();
   }

   public void onUpdate(UpdateEvent event) {
      if (this.mode.is(AutoWalk.Mode.Forward)) {
         mc.options.forwardKey.setPressed(true);
      } else if (this.mode.is(AutoWalk.Mode.Path)) {
         if (!this.start) {
            BaritoneUtil.forward();
            this.start = true;
         } else if (!BaritoneUtil.isActive()) {
            this.disable();
         }
      }
   }

   @Override
   public void onDisable() {
      BaritoneUtil.cancelEverything();
   }

   public boolean forward() {
      return this.isOn() && this.mode.is(AutoWalk.Mode.Forward);
   }

   public static enum Mode {
      Forward,
      Path;
   }
}
