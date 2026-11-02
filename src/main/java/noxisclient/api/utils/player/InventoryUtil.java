package noxisclient.api.utils.player;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Map.Entry;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PickFromInventoryC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry.Reference;
import net.minecraft.screen.slot.SlotActionType;
import noxisclient.api.utils.Wrapper;
import noxisclient.api.utils.world.BlockUtil;
import noxisclient.mod.modules.impl.client.AntiCheat;

public class InventoryUtil implements Wrapper {
   static int lastSlot = -1;
   static int lastSelect = -1;

   public static void inventorySwap(int slot, int selectedSlot) {
      if (slot == lastSlot) {
         switchToSlot(lastSelect);
         lastSlot = -1;
         lastSelect = -1;
      } else if (slot - 36 != selectedSlot) {
         if (EntityUtil.inInventory()) {
            if (AntiCheat.INSTANCE.invSwapBypass.getValue()) {
               if (slot - 36 >= 0) {
                  lastSlot = slot;
                  lastSelect = selectedSlot;
                  switchToSlot(slot - 36);
                  return;
               }

               mc.getNetworkHandler().sendPacket(new PickFromInventoryC2SPacket(slot));
            } else {
               mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, slot, selectedSlot, SlotActionType.SWAP, mc.player);
               mc.player.getInventory().updateItems();
            }
         }
      }
   }

   public static void switchToSlot(int slot) {
      mc.player.getInventory().selectedSlot = slot;
      mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(slot));
   }

   public static void switchToSlotSilent(int slot) {
      mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(slot));
   }

   public static int findItem(Item input) {
      for (int i = 0; i < 9; i++) {
         Item item = mc.player.getInventory().getStack(i).getItem();
         if (Item.getRawId(item) == Item.getRawId(input)) {
            return i;
         }
      }

      return -1;
   }

   public static int getFood() {
      for (int i = 0; i < 9; i++) {
         if (mc.player.getInventory().getStack(i).getComponents().contains(DataComponentTypes.FOOD)) {
            return i;
         }
      }

      return -1;
   }

   public static int getPotionCount(StatusEffect targetEffect) {
      int count = 0;

      for (int i = 35; i >= 0; i--) {
         ItemStack itemStack = mc.player.getInventory().getStack(i);
         if (Item.getRawId(itemStack.getItem()) == Item.getRawId(Items.SPLASH_POTION)) {
            PotionContentsComponent potionContentsComponent = itemStack.getOrDefault(DataComponentTypes.POTION_CONTENTS, PotionContentsComponent.DEFAULT);

            for (StatusEffectInstance effect : potionContentsComponent.getEffects()) {
               if (effect.getEffectType().value() == targetEffect) {
                  count += itemStack.getCount();
               }
            }
         }
      }

      return count;
   }

   public static int getItemCount(Class<?> clazz) {
      int count = 0;

      for (Entry<Integer, ItemStack> entry : getInventoryAndHotbarSlots().entrySet()) {
         if (entry.getValue().getItem() instanceof BlockItem && clazz.isInstance(((BlockItem)entry.getValue().getItem()).getBlock())) {
            count += entry.getValue().getCount();
         }
      }

      return count;
   }

   public static int getItemCount(Item item) {
      int count = 0;

      for (Entry<Integer, ItemStack> entry : getInventoryAndHotbarSlots().entrySet()) {
         if (entry.getValue().getItem() == item) {
            count += entry.getValue().getCount();
         }
      }

      if (mc.player.getOffHandStack().getItem() == item) {
         count += mc.player.getOffHandStack().getCount();
      }

      return count;
   }

   public static int findClass(Class<?> clazz) {
      for (int i = 0; i < 9; i++) {
         ItemStack stack = mc.player.getInventory().getStack(i);
         if (stack != ItemStack.EMPTY) {
            if (clazz.isInstance(stack.getItem())) {
               return i;
            }

            if (stack.getItem() instanceof BlockItem && clazz.isInstance(((BlockItem)stack.getItem()).getBlock())) {
               return i;
            }
         }
      }

      return -1;
   }

   public static int findClassInventorySlot(Class<?> clazz) {
      if (AntiCheat.INSTANCE.priorHotbar.getValue()) {
         for (int i = 0; i < 36; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (stack != ItemStack.EMPTY) {
               if (clazz.isInstance(stack.getItem())) {
                  return i < 9 ? i + 36 : i;
               }

               if (stack.getItem() instanceof BlockItem && clazz.isInstance(((BlockItem)stack.getItem()).getBlock())) {
                  return i < 9 ? i + 36 : i;
               }
            }
         }
      } else {
         for (int ix = 35; ix >= 0; ix--) {
            ItemStack stack = mc.player.getInventory().getStack(ix);
            if (stack != ItemStack.EMPTY) {
               if (clazz.isInstance(stack.getItem())) {
                  return ix < 9 ? ix + 36 : ix;
               }

               if (stack.getItem() instanceof BlockItem && clazz.isInstance(((BlockItem)stack.getItem()).getBlock())) {
                  return ix < 9 ? ix + 36 : ix;
               }
            }
         }
      }

      return -1;
   }

   public static int findBlock(Block blockIn) {
      for (int i = 0; i < 9; i++) {
         ItemStack stack = mc.player.getInventory().getStack(i);
         if (stack != ItemStack.EMPTY && stack.getItem() instanceof BlockItem && ((BlockItem)stack.getItem()).getBlock() == blockIn) {
            return i;
         }
      }

      return -1;
   }

   public static int findUnBlock() {
      for (int i = 0; i < 9; i++) {
         ItemStack stack = mc.player.getInventory().getStack(i);
         if (!(stack.getItem() instanceof BlockItem)) {
            return i;
         }
      }

      return -1;
   }

   public static int findBlock() {
      for (int i = 0; i < 9; i++) {
         ItemStack stack = mc.player.getInventory().getStack(i);
         if (stack.getItem() instanceof BlockItem
            && !BlockUtil.isClickable(Block.getBlockFromItem(stack.getItem()))
            && ((BlockItem)stack.getItem()).getBlock() != Blocks.COBWEB) {
            return i;
         }
      }

      return -1;
   }

   public static int findBlockInventorySlot(Block block) {
      return findItemInventorySlot(block.asItem());
   }

   public static int findItemInventorySlot(Item item) {
      if (AntiCheat.INSTANCE.priorHotbar.getValue()) {
         return findItemInventorySlotFromZero(item.asItem());
      } else {
         for (int i = 35; i >= 0; i--) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (stack.getItem() == item) {
               return i < 9 ? i + 36 : i;
            }
         }

         return -1;
      }
   }

   public static int findItemInventorySlotFromZero(Item item) {
      for (int i = 0; i < 36; i++) {
         ItemStack stack = mc.player.getInventory().getStack(i);
         if (stack.getItem() == item) {
            return i < 9 ? i + 36 : i;
         }
      }

      return -1;
   }

   public static Map<Integer, ItemStack> getInventoryAndHotbarSlots() {
      HashMap<Integer, ItemStack> fullInventorySlots = new HashMap<>();

      for (int current = 0; current <= 35; current++) {
         fullInventorySlots.put(current, mc.player.getInventory().getStack(current));
      }

      return fullInventorySlots;
   }

   public static int getEnchantmentLevel(ItemStack stack, RegistryKey<Enchantment> enchantment) {
      if (mc != null && mc.world != null) {
         Registry<Enchantment> registry = mc.world.getRegistryManager().get(RegistryKeys.ENCHANTMENT);
         Optional<Reference<Enchantment>> entry = registry.getEntry(enchantment);
         return entry.isEmpty() ? 0 : EnchantmentHelper.getLevel(entry.get(), stack);
      } else {
         return 0;
      }
   }
}
