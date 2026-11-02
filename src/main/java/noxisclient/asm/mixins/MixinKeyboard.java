package noxisclient.asm.mixins;

import net.minecraft.client.Keyboard;
import net.minecraft.client.util.NarratorManager;
import net.minecraft.util.Formatting;
import noxisclient.Alien;
import noxisclient.api.utils.Wrapper;
import noxisclient.core.impl.CommandManager;
import noxisclient.mod.modules.impl.client.ClientSetting;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({Keyboard.class})
public class MixinKeyboard implements Wrapper {
   @Inject(
      method = {"onKey"},
      at = {@At("HEAD")}
   )
   private void onKey(long windowPointer, int key, int scanCode, int action, int modifiers, CallbackInfo ci) {
      try {
         if (Alien.MODULE == null) {
            return;
         }

         if (action == 1) {
            Alien.MODULE.onKeyPressed(key);
         }

         if (action == 0) {
            Alien.MODULE.onKeyReleased(key);
         }
      } catch (Exception var9) {
         var9.printStackTrace();
         if (ClientSetting.INSTANCE != null && ClientSetting.INSTANCE.debug.getValue()) {
            CommandManager.sendMessage(Formatting.DARK_RED + "[ERROR] onKey " + var9.getMessage());
         }
      }
   }

   @Redirect(
      method = {"onKey"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/util/NarratorManager;isActive()Z"
      ),
      require = 0
   )
   public boolean hook(NarratorManager instance) {
      return false;
   }
}
