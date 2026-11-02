package noxisclient.asm.mixins;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.Mouse;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.Formatting;
import noxisclient.Alien;
import noxisclient.api.interfaces.IMouseHook;
import noxisclient.core.impl.CommandManager;
import noxisclient.mod.modules.impl.client.ClientSetting;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({Mouse.class})
public class MixinMouse implements IMouseHook {
   @Shadow
   private boolean field_1783;
   @Final
   @Shadow
   private MinecraftClient field_1779;
   @Shadow
   private double field_1795;
   @Shadow
   private double field_1794;

   @Inject(
      method = {"onMouseButton"},
      at = {@At("HEAD")}
   )
   private void onMouse(long window, int button, int action, int mods, CallbackInfo ci) {
      try {
         if (Alien.MODULE == null) {
            return;
         }

         int key = -(button + 1);
         if (action == 1) {
            Alien.MODULE.onKeyPressed(key);
         }

         if (action == 0) {
            Alien.MODULE.onKeyReleased(key);
         }
      } catch (Exception var8) {
         var8.printStackTrace();
         if (ClientSetting.INSTANCE != null && ClientSetting.INSTANCE.debug.getValue()) {
            CommandManager.sendMessage(Formatting.DARK_RED + "[ERROR] onMouse " + var8.getMessage());
         }
      }
   }

   @Override
   public void alienClient$lock() {
      if (this.field_1779.isWindowFocused() && !this.field_1783) {
         if (!MinecraftClient.IS_SYSTEM_MAC) {
            KeyBinding.updatePressedStates();
         }

         this.field_1783 = true;
         this.field_1795 = (double)this.field_1779.getWindow().getWidth() / 2.0;
         this.field_1794 = (double)this.field_1779.getWindow().getHeight() / 2.0;
         InputUtil.setCursorParameters(this.field_1779.getWindow().getHandle(), 212995, this.field_1795, this.field_1794);
      }
   }
}
