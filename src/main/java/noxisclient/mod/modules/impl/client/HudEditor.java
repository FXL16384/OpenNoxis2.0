package noxisclient.mod.modules.impl.client;

import noxisclient.api.events.impl.TickEvent;
import noxisclient.mod.gui.click.NewClickGuiScreen;
import noxisclient.mod.modules.Module;

public class HudEditor extends Module {
   public static HudEditor INSTANCE;

   public HudEditor() {
      super("HudEditor", "HudEditor", Module.Category.Client);
      this.setChinese("界面编辑器");
      INSTANCE = this;
   }

   @Override
   public void onEnable() {
      super.onEnable();
      if (mc.player != null) {
         mc.execute(() -> {
            if (!(mc.currentScreen instanceof NewClickGuiScreen)) {
               mc.setScreen(NewClickGuiScreen.getInstance());
            }
         });
      }
   }

   @Override
   public void onDisable() {
      super.onDisable();
      if (mc.currentScreen instanceof NewClickGuiScreen) {
         mc.execute(() -> mc.setScreen(null));
      }
   }

   public void onTick(TickEvent event) {
      if (!(mc.currentScreen instanceof NewClickGuiScreen)) {
         this.disable();
      }
   }
}
