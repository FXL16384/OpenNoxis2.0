package noxisclient.asm.mixins;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.gui.screen.ingame.ScreenHandlerProvider;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.Text;
import noxisclient.Alien;
import noxisclient.api.utils.Wrapper;
import noxisclient.core.impl.AnimationManager;
import noxisclient.mod.modules.impl.client.EcCache;
import noxisclient.mod.modules.impl.combat.AntiRegear;
import noxisclient.mod.modules.impl.misc.ShulkerViewer;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({HandledScreen.class})
public abstract class MixinHandledScreen<T extends ScreenHandler> extends Screen implements ScreenHandlerProvider<T> {
   @Unique
   private static final ItemStack[] ITEMS = new ItemStack[27];
   @Shadow
   @Nullable
   protected Slot field_2787;
   @Shadow
   protected int field_2776;
   @Shadow
   protected int field_2800;

   protected MixinHandledScreen(Text title) {
      super(title);
   }

   @Inject(
      method = {"mouseClicked"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void mouseClicked(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> cir) {
      if (button == 2
         && this.field_2787 != null
         && !this.field_2787.getStack().isEmpty()
         && Wrapper.mc.player.currentScreenHandler.getCursorStack().isEmpty()
         && ShulkerViewer.INSTANCE.isOn()) {
         ItemStack itemStack = this.field_2787.getStack();
         if (ShulkerViewer.hasItems(itemStack) || itemStack.getItem() == Items.ENDER_CHEST && Alien.PLAYER.known) {
            cir.setReturnValue(ShulkerViewer.openContainer(this.field_2787.getStack(), ITEMS, false));
         }
      }
   }

   @Inject(
      method = {"render"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void onRenderHeadAntiRegear(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
      if (AntiRegear.INSTANCE != null && AntiRegear.INSTANCE.isOn()) {
         if (AntiRegear.INSTANCE.drop.getValue() && AntiRegear.INSTANCE.NoUi.getValue()) {
            if (AntiRegear.hideShulkerUI) {
               ci.cancel();
            }
         }
      }
   }

   @Inject(
      method = {"render"},
      at = {@At("RETURN")}
   )
   private void onRender(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
      if (ShulkerViewer.INSTANCE.isOn()
         && ShulkerViewer.INSTANCE.toolTips.getValue()
         && this.field_2787 != null
         && !this.field_2787.getStack().isEmpty()
         && this.client.player.playerScreenHandler.getCursorStack().isEmpty()
         && (ShulkerViewer.hasItems(this.field_2787.getStack()) || this.field_2787.getStack().getItem() == Items.ENDER_CHEST && Alien.PLAYER.known)) {
         ShulkerViewer.renderShulkerToolTip(context, mouseX, mouseY, this.field_2787.getStack());
      }

      if ((Object)this instanceof InventoryScreen && EcCache.INSTANCE != null) {
         EcCache.INSTANCE.renderPreview(context, mouseX, mouseY);
      }

      AnimationManager.render(context);
   }

   @Shadow
   @Override
   public abstract void renderBackground(DrawContext var1, int var2, int var3, float var4);
}
