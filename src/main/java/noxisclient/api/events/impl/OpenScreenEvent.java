package noxisclient.api.events.impl;

import net.minecraft.client.gui.screen.Screen;
import noxisclient.api.events.Event;

public class OpenScreenEvent extends Event {
   private static final OpenScreenEvent INSTANCE = new OpenScreenEvent();
   public Screen screen;

   private OpenScreenEvent() {
   }

   public static OpenScreenEvent get(Screen screen) {
      INSTANCE.screen = screen;
      INSTANCE.setCancelled(false);
      return INSTANCE;
   }
}
