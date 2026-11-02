package noxisclient.asm.mixins;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.LogoDrawer;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.SplashTextRenderer;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import noxisclient.Alien;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({TitleScreen.class})
public abstract class MixinTitleScreen extends Screen {
   @Unique
   private static final int NOXIS_TITLE_BUTTON_Y_OFFSET = 12;

   protected MixinTitleScreen(Text title) {
      super(title);
   }

   @Inject(
      method = {"renderPanoramaBackground"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void renderNoxisTitleShader(DrawContext context, float delta, CallbackInfo ci) {
      noxisclient.mod.modules.impl.client.TitleScreen titleScreen = noxisclient.mod.modules.impl.client.TitleScreen.INSTANCE;
      if (titleScreen != null && titleScreen.isOn() && Alien.SHADER != null) {
         if (Alien.SHADER.renderTitleScreenMenu(titleScreen.getShaderMode(), (float)this.width, (float)this.height, delta, titleScreen.getRenderTime())) {
            ci.cancel();
         }
      }
   }

   @Inject(
      method = {"init"},
      at = {@At("TAIL")}
   )
   private void moveNoxisTitleButtons(CallbackInfo ci) {
      for (Element element : this.children()) {
         if (element instanceof ButtonWidget buttonWidget) {
            buttonWidget.setY(buttonWidget.getY() + 12);
         }
      }
   }

   @Redirect(
      method = {"render"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/gui/LogoDrawer;draw(Lnet/minecraft/client/gui/DrawContext;IF)V"
      )
   )
   private void drawNoxisClientLogo(LogoDrawer logoDrawer, DrawContext context, int screenWidth, float alpha) {
   }

   @Redirect(
      method = {"render"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/gui/screen/SplashTextRenderer;render(Lnet/minecraft/client/gui/DrawContext;ILnet/minecraft/client/font/TextRenderer;I)V"
      )
   )
   private void hideMinecraftSplashText(SplashTextRenderer splashTextRenderer, DrawContext context, int screenWidth, TextRenderer textRenderer, int color) {
   }
}
