package noxisclient.asm.mixins;

import java.awt.Color;
import java.util.List;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Drawable;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.ScreenRect;
import net.minecraft.client.gui.Selectable;
import net.minecraft.client.gui.screen.DownloadingTerrainScreen;
import net.minecraft.client.gui.screen.ProgressScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextIconButtonWidget;
import net.minecraft.client.gui.widget.TexturedButtonWidget;
import net.minecraft.client.util.math.MatrixStack;
import noxisclient.Alien;
import noxisclient.api.utils.render.Render2DUtil;
import noxisclient.mod.modules.impl.client.ClickGui;
import noxisclient.mod.modules.impl.client.ClientSetting;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({Screen.class})
public class MixinScreen {
   @Shadow
   public int field_22789;
   @Shadow
   public int field_22790;
   @Shadow
   protected MinecraftClient field_22787;
   @Shadow
   @Final
   private List<Element> field_22786;
   @Shadow
   @Final
   private List<Selectable> field_33815;
   @Shadow
   @Final
   private List<Drawable> field_33816;

   @Inject(
      method = {"addDrawableChild"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private <T extends Element & Drawable & Selectable> void skipNoxisTitleExtraButtons(T drawableElement, CallbackInfoReturnable<T> cir) {
      if ((Object)this instanceof TitleScreen && isBlockedTitleButton(drawableElement)) {
         cir.setReturnValue(drawableElement);
      }
   }

   @Inject(
      method = {"render"},
      at = {@At("HEAD")}
   )
   private void removeNoxisTitleExtraButtons(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
      if ((Object)this instanceof TitleScreen) {
         this.field_22786.removeIf(MixinScreen::isBlockedTitleButton);
         this.field_33815.removeIf(MixinScreen::isBlockedTitleButton);
         this.field_33816.removeIf(MixinScreen::isBlockedTitleButton);
      }
   }

   @Inject(
      method = {"renderBackground"},
      at = {@At("HEAD")},
      cancellable = true
   )
   public void renderInGameBackgroundHook(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
      ci.cancel();
      if (this.field_22787.world == null) {
         boolean isMainMenu = (Object)this instanceof TitleScreen;
         boolean isLoading = (Object)this instanceof ProgressScreen || (Object)this instanceof DownloadingTerrainScreen;
         if (isMainMenu && noxisclient.mod.modules.impl.client.TitleScreen.shouldRender()) {
            noxisclient.mod.modules.impl.client.TitleScreen titleScreen = noxisclient.mod.modules.impl.client.TitleScreen.INSTANCE;
            if (titleScreen != null
               && Alien.SHADER != null
               && Alien.SHADER
                  .renderTitleScreenMenu(titleScreen.getShaderMode(), (float)this.field_22789, (float)this.field_22790, delta, titleScreen.getRenderTime())) {
               return;
            }
         }

         if (isMainMenu || isLoading) {
            float w = (float)this.field_22789;
            float h = (float)this.field_22790;
            MatrixStack m = context.getMatrices();
            Render2DUtil.verticalGradient(m, 0.0F, 0.0F, w, h, new Color(40, 10, 60, 255), new Color(200, 20, 150, 255));
            long now = System.currentTimeMillis();
            float phase = (float)(now % 6000L) / 6000.0F;
            float angle = 0.523599F;
            float dx = (float)Math.tan((double)angle) * h;
            float base = -h;
            float spacing = 36.0F;
            float shift = phase * spacing * 4.0F;
            Color c1 = new Color(220, 60, 170, 52);
            Color c2 = new Color(160, 40, 130, 36);

            for (float i = base; i < w; i += spacing) {
               float x0 = i + shift;
               Render2DUtil.drawLine(m, x0, 0.0F, x0 + dx, h, c1.getRGB());
            }

            for (float var29 = base + spacing / 2.0F; var29 < w; var29 += spacing) {
               float x0 = var29 + shift * 0.8F;
               Render2DUtil.drawLine(m, x0, 0.0F, x0 + dx, h, c2.getRGB());
            }

            float hSpacing = 64.0F;
            Color c3 = new Color(255, 255, 255, 18);

            for (float y = 0.0F; y <= h; y += hSpacing) {
               Render2DUtil.drawLine(m, 0.0F, y, w, y, c3.getRGB());
            }

            return;
         }

         this.method_57728(context, delta);
      }

      if (ClientSetting.INSTANCE.darkening.getValue() && !((Object)this instanceof TitleScreen)) {
         this.method_57735(context);
      }

      if (this.field_22787.world != null && ClickGui.getInstance().tint.booleanValue) {
         context.fillGradient(
            0, 0, this.field_22789, this.field_22790, ClickGui.getInstance().tint.getValue().getRGB(), ClickGui.getInstance().endColor.getValue().getRGB()
         );
      }
   }

   @Shadow
   protected void method_57728(DrawContext context, float delta) {
   }

   @Shadow
   protected void method_57735(DrawContext context) {
   }

   @Shadow
   public void method_25419() {
   }

   @Shadow
   public ScreenRect method_48202() {
      return null;
   }

   @Shadow
   protected <T extends Element & Drawable> T method_37063(T drawableElement) {
      return null;
   }

   @Unique
   private static boolean isBlockedTitleButton(Object element) {
      if (element instanceof TextIconButtonWidget) {
         return true;
      } else {
         return element instanceof TexturedButtonWidget && element instanceof ButtonWidget button
            ? "In-Game Account Switcher".equals(button.getMessage().getString())
            : false;
      }
   }
}
