package noxisclient.mod.modules.impl.misc;

import net.minecraft.block.Blocks;
import net.minecraft.client.gui.screen.recipebook.RecipeResultCollection;
import net.minecraft.item.BedItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.recipe.RecipeEntry;
import net.minecraft.screen.CraftingScreenHandler;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import noxisclient.api.events.impl.UpdateEvent;
import noxisclient.api.utils.player.InventoryUtil;
import noxisclient.api.utils.world.BlockUtil;
import noxisclient.mod.modules.Module;
import noxisclient.mod.modules.settings.impl.BooleanSetting;
import noxisclient.mod.modules.settings.impl.SliderSetting;

public class BedCrafter extends Module {
   public static BedCrafter INSTANCE;
   private final BooleanSetting rotate = this.add(new BooleanSetting("Rotate", false));
   private final SliderSetting range = this.add(new SliderSetting("Range", 5, 0, 8));
   private final BooleanSetting disable = this.add(new BooleanSetting("Disable", true));

   public BedCrafter() {
      super("BedCrafter", Module.Category.Misc);
      this.setChinese("自动制作床");
      INSTANCE = this;
   }

   public static int getEmptySlots() {
      int emptySlots = 0;

      for (int i = 0; i < 36; i++) {
         ItemStack itemStack = mc.player.getInventory().getStack(i);
         if (itemStack.isEmpty()) {
            emptySlots++;
         }
      }

      return emptySlots;
   }

   public void onUpdate(UpdateEvent event) {
      if (getEmptySlots() == 0) {
         if (mc.player.currentScreenHandler instanceof CraftingScreenHandler) {
            mc.player.closeHandledScreen();
         }

         if (this.disable.getValue()) {
            this.disable();
         }
      } else {
         if (mc.player.currentScreenHandler instanceof CraftingScreenHandler) {
            boolean craft = false;

            for (RecipeResultCollection recipeResult : mc.player.getRecipeBook().getOrderedResults()) {
               for (RecipeEntry recipe : recipeResult.getRecipes(true)) {
                  if (recipe.value().getResult(mc.world.getRegistryManager()).getItem() instanceof BedItem) {
                     for (int i = 0; i < getEmptySlots(); i++) {
                        craft = true;
                        mc.interactionManager.clickRecipe(mc.player.currentScreenHandler.syncId, recipe, false);
                        mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, 0, 1, SlotActionType.QUICK_MOVE, mc.player);
                     }
                     break;
                  }
               }
            }

            if (!craft) {
               if (mc.player.currentScreenHandler instanceof CraftingScreenHandler) {
                  mc.player.closeHandledScreen();
               }

               if (this.disable.getValue()) {
                  this.disable();
               }
            }
         } else {
            this.doPlace();
         }
      }
   }

   private void doPlace() {
      BlockPos bestPos = null;
      double getDistance = 100.0;
      boolean place = true;

      for (BlockPos pos : BlockUtil.getSphere(this.range.getValueFloat())) {
         if (mc.world.getBlockState(pos).getBlock() == Blocks.CRAFTING_TABLE && BlockUtil.getClickSideStrict(pos) != null) {
            place = false;
            bestPos = pos;
            break;
         }

         if (BlockUtil.canPlace(pos) && (bestPos == null || (double)MathHelper.sqrt((float)mc.player.squaredDistanceTo(pos.toCenterPos())) < getDistance)) {
            bestPos = pos;
            getDistance = (double)MathHelper.sqrt((float)mc.player.squaredDistanceTo(pos.toCenterPos()));
         }
      }

      if (bestPos != null) {
         if (!place) {
            BlockUtil.clickBlock(bestPos, BlockUtil.getClickSide(bestPos), this.rotate.getValue());
         } else {
            if (InventoryUtil.findItem(Items.CRAFTING_TABLE) == -1) {
               return;
            }

            int old = mc.player.getInventory().selectedSlot;
            InventoryUtil.switchToSlot(InventoryUtil.findItem(Items.CRAFTING_TABLE));
            BlockUtil.placeBlock(bestPos, this.rotate.getValue());
            InventoryUtil.switchToSlot(old);
         }
      }
   }
}
