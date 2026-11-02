package noxisclient.api.events.impl;

import net.minecraft.entity.Entity;
import noxisclient.api.events.Event;

public class EntitySpawnEvent extends Event {
   private static final EntitySpawnEvent INSTANCE = new EntitySpawnEvent();
   private Entity entity;

   private EntitySpawnEvent() {
   }

   public static EntitySpawnEvent get(Entity entity) {
      INSTANCE.entity = entity;
      INSTANCE.setCancelled(false);
      return INSTANCE;
   }

   public Entity getEntity() {
      return this.entity;
   }
}
