package noxisclient.api.events.impl;

import noxisclient.api.events.Event;

public class SendMessageEvent extends Event {
   private static final SendMessageEvent INSTANCE = new SendMessageEvent();
   public String defaultMessage;
   public String message;

   private SendMessageEvent() {
   }

   public static SendMessageEvent get(String message) {
      INSTANCE.defaultMessage = message;
      INSTANCE.message = message;
      INSTANCE.setCancelled(false);
      return INSTANCE;
   }
}
