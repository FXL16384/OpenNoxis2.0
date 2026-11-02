package noxisclient.api.events.impl;

import net.minecraft.block.entity.BlockEntity;
import noxisclient.api.events.Event;

public class RenderBlockEntityEvent extends Event {
   private static final RenderBlockEntityEvent INSTANCE = new RenderBlockEntityEvent();
   public BlockEntity blockEntity;

   public static RenderBlockEntityEvent get(BlockEntity blockEntity) {
      INSTANCE.setCancelled(false);
      INSTANCE.blockEntity = blockEntity;
      return INSTANCE;
   }
}
