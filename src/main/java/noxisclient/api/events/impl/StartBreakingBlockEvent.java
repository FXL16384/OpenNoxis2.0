package noxisclient.api.events.impl;

import net.minecraft.util.math.BlockPos;
import noxisclient.api.events.Event;

public class StartBreakingBlockEvent extends Event {
   private BlockPos blockPos;
   private static final StartBreakingBlockEvent INSTANCE = new StartBreakingBlockEvent();

   private StartBreakingBlockEvent() {
   }

   public BlockPos getBlockPos() {
      return this.blockPos;
   }

   public static StartBreakingBlockEvent get(BlockPos blockPos) {
      INSTANCE.blockPos = blockPos;
      INSTANCE.setCancelled(false);
      return INSTANCE;
   }
}
