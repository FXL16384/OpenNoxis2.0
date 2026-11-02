package noxisclient.mod.modules.impl.misc;

import net.minecraft.client.gui.screen.DownloadingTerrainScreen;
import net.minecraft.client.gui.screen.ProgressScreen;
import noxisclient.api.events.impl.ClientTickEvent;
import noxisclient.mod.modules.Module;

public class NoTerrainScreen extends Module {
   public NoTerrainScreen() {
      super("NoTerrainScreen", Module.Category.Misc);
      this.setChinese("没有加载界面");
   }

   public void onEvent(ClientTickEvent event) {
      if (!nullCheck()) {
         if (mc.currentScreen instanceof DownloadingTerrainScreen || mc.currentScreen instanceof ProgressScreen) {
            mc.currentScreen = null;
         }
      }
   }
}
