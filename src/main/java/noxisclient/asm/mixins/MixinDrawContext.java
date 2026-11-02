package noxisclient.asm.mixins;

import net.minecraft.block.ShulkerBoxBlock;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import noxisclient.mod.modules.impl.misc.ShulkerViewer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({DrawContext.class})
public class MixinDrawContext {
   @Unique
   private static final ItemStack[] ITEMS = new ItemStack[27];
   @Final
   @Shadow
   private MatrixStack field_44657;

   @Inject(
      method = {"drawItemInSlot(Lnet/minecraft/client/font/TextRenderer;Lnet/minecraft/item/ItemStack;IILjava/lang/String;)V"},
      at = {@At("TAIL")}
   )
   public void onDrawItem(TextRenderer textRenderer, ItemStack stack, int x, int y, String countOverride, CallbackInfo ci) {
      if (ShulkerViewer.INSTANCE.isOn()
         && ShulkerViewer.INSTANCE.icon.getValue()
         && stack.getItem() instanceof BlockItem
         && ((BlockItem)stack.getItem()).getBlock() instanceof ShulkerBoxBlock) {
         ShulkerViewer.getItemsInContainerItem(stack, ITEMS);

         for (ItemStack itemStack : ITEMS) {
            if (!itemStack.isEmpty()) {
               this.field_44657.push();
               this.field_44657.scale(0.5F, 0.5F, 1.0F);
               this.method_51427(itemStack, x * 2 + 20, y * 2 + 20);
               this.field_44657.pop();
               return;
            }
         }
      }
   }

   @Shadow
   public void method_51427(ItemStack item, int x, int y) {
   }
}
