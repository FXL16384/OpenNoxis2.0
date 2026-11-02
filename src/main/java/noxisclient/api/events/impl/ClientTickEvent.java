package noxisclient.api.events.impl;

import noxisclient.api.events.Event;

public class ClientTickEvent extends Event {
   private static final ClientTickEvent instance = new ClientTickEvent();

   private ClientTickEvent() {
   }

   public static ClientTickEvent get(Event.Stage stage) {
      instance.stage = stage;
      return instance;
   }
}
