package noxisclient.api.events.impl;

import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import noxisclient.api.events.Event;
import noxisclient.api.utils.Wrapper;

public class SetCurrentHandEvent extends Event {
   private final Hand hand;

   public SetCurrentHandEvent(Hand hand) {
      this.hand = hand;
   }

   public Hand getHand() {
      return this.hand;
   }

   public ItemStack getStackInHand() {
      return Wrapper.mc.player.getStackInHand(this.hand);
   }
}
