package noxisclient.api.events.impl;

import net.minecraft.entity.Entity;
import noxisclient.api.events.Event;

public class RenderEntityEvent extends Event {
   private static final RenderEntityEvent INSTANCE = new RenderEntityEvent();
   private Entity entity;

   private RenderEntityEvent() {
   }

   public static RenderEntityEvent get(Entity entity) {
      INSTANCE.entity = entity;
      INSTANCE.setCancelled(false);
      return INSTANCE;
   }

   public Entity getEntity() {
      return this.entity;
   }
}
