package noxisclient.api.events.impl;

import noxisclient.api.events.Event;

public class TickEvent extends Event {
   private static final TickEvent instance = new TickEvent();

   private TickEvent() {
   }

   public static TickEvent get(Event.Stage stage) {
      instance.stage = stage;
      return instance;
   }
}
