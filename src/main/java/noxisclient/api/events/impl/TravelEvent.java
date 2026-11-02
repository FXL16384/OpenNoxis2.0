package noxisclient.api.events.impl;

import net.minecraft.entity.player.PlayerEntity;
import noxisclient.api.events.Event;

public class TravelEvent extends Event {
   private static final TravelEvent INSTANCE = new TravelEvent();
   private PlayerEntity entity;

   private TravelEvent() {
   }

   public static TravelEvent get(Event.Stage stage, PlayerEntity entity) {
      INSTANCE.entity = entity;
      INSTANCE.stage = stage;
      INSTANCE.setCancelled(false);
      return INSTANCE;
   }

   public PlayerEntity getEntity() {
      return this.entity;
   }
}
