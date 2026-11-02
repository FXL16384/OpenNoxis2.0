package noxisclient.asm.mixins;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.DownloadingTerrainScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({DownloadingTerrainScreen.class})
public class MixinLevelLoadingScreen {
   @Inject(
      method = {"render"},
      at = {@At("HEAD")}
   )
   private void onRenderPre(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
   }

   @Inject(
      method = {"render"},
      at = {@At("TAIL")}
   )
   private void onRenderPost(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
   }
}
