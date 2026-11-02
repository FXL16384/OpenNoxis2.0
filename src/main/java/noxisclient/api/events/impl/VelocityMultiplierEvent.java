package noxisclient.api.events.impl;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import noxisclient.api.events.Event;

public class VelocityMultiplierEvent extends Event {
   private final BlockState state;

   public VelocityMultiplierEvent(BlockState state) {
      this.state = state;
   }

   public Block getBlock() {
      return this.state.getBlock();
   }
}
