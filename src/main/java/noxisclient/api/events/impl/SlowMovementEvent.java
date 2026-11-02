package noxisclient.api.events.impl;

import net.minecraft.block.BlockState;
import noxisclient.api.events.Event;

public class SlowMovementEvent extends Event {
   private final BlockState state;
   private float multiplier = 1.0F;

   public SlowMovementEvent(BlockState state) {
      this.state = state;
   }

   public BlockState getState() {
      return this.state;
   }

   public float getMultiplier() {
      return this.multiplier;
   }

   public void setMultiplier(float multiplier) {
      this.multiplier = multiplier;
   }
}
