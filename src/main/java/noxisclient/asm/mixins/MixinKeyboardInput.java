package noxisclient.asm.mixins;

import net.minecraft.client.input.KeyboardInput;
import noxisclient.Alien;
import noxisclient.api.events.impl.KeyboardInputEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({KeyboardInput.class})
public class MixinKeyboardInput {
   @Inject(
      method = {"tick"},
      at = {@At(
         value = "FIELD",
         target = "Lnet/minecraft/client/input/KeyboardInput;sneaking:Z",
         shift = Shift.AFTER
      )},
      cancellable = true
   )
   private void keyInput(boolean slowDown, float slowDownFactor, CallbackInfo ci) {
      KeyboardInputEvent event = KeyboardInputEvent.get();
      Alien.EVENT_BUS.post(event);
      if (event.isCancelled()) {
         ci.cancel();
      }
   }
}
