package noxisclient.api.events.impl;

import net.minecraft.client.input.Input;
import noxisclient.api.events.Event;

public class MovementSlowdownEvent extends Event {
   public final Input input;

   public MovementSlowdownEvent(Input input) {
      this.input = input;
   }

   public Input getInput() {
      return this.input;
   }
}
