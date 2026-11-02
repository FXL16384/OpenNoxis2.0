package noxisclient.api.events.impl;

import net.minecraft.text.Text;
import noxisclient.api.events.Event;

public class ReceiveMessageEvent extends Event {
   private static final ReceiveMessageEvent INSTANCE = new ReceiveMessageEvent();
   public Text message;

   private ReceiveMessageEvent() {
   }

   public static ReceiveMessageEvent get(Text message) {
      INSTANCE.message = message;
      INSTANCE.setCancelled(false);
      return INSTANCE;
   }
}
