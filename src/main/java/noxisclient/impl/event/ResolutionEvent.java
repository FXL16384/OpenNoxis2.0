package noxisclient.impl.event;

import net.minecraft.client.util.Window;

public class ResolutionEvent {
   private final Window window;

   public ResolutionEvent(Window window) {
      this.window = window;
   }

   public Window getWindow() {
      return this.window;
   }
}
