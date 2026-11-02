package noxisclient.mod.modules.impl.player;

import java.util.Map;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.ElytraItem;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.SlotActionType;
import noxisclient.api.events.impl.UpdateEvent;
import noxisclient.api.utils.math.Timer;
import noxisclient.api.utils.player.EntityUtil;
import noxisclient.mod.modules.Module;
import noxisclient.mod.modules.impl.combat.AutoRegear;
import noxisclient.mod.modules.settings.impl.BooleanSetting;
import noxisclient.mod.modules.settings.impl.EnumSetting;
import noxisclient.mod.modules.settings.impl.SliderSetting;

public class InventoryManager extends Module {
   public static InventoryManager INSTANCE;
   private final SliderSetting delay = this.add(new SliderSetting("Delay", 100, 0, 1000).setSuffix("ms"));
   private final EnumSetting<InventoryManager.Range> range = this.add(new EnumSetting<>("Range", InventoryManager.Range.All));
   private final BooleanSetting slot = this.add(new BooleanSetting("Slot", true));
   private final BooleanSetting drop = this.add(new BooleanSetting("Drop", false));
   private final BooleanSetting onlyInventory = this.add(new BooleanSetting("OnlyInventory", true));
   private final Timer timer = new Timer();

   public InventoryManager() {
      super("InventoryManager", Module.Category.Player);
      this.setChinese("背包管理");
      INSTANCE = this;
   }

   @Override
   public void onEnable() {
      this.timer.reset();
   }

   public void onUpdate(UpdateEvent event) {
      if (!nullCheck() && this.canOperate()) {
         if (this.timer.passedMs(this.delay.getValue())) {
            if (mc.player.currentScreenHandler.getCursorStack().isEmpty()) {
               AutoRegear regear = AutoRegear.INSTANCE;
               if (regear != null && regear.hasLoadedKit()) {
                  Map<Integer, ItemStack> kit = regear.getDesiredInventory();
                  if (!kit.isEmpty()) {
                     if (this.slot.getValue() && this.moveOneKitItem(kit, regear)) {
                        this.timer.reset();
                     } else {
                        if (this.drop.getValue() && this.dropOneNonKitItem(kit, regear)) {
                           this.timer.reset();
                        }
                     }
                  }
               } else {
                  this.sendMessage("§cNo kit loaded. Use kit load <name>.");
                  this.disable();
               }
            }
         }
      }
   }

   private boolean canOperate() {
      return this.onlyInventory.getValue() ? mc.currentScreen instanceof InventoryScreen : EntityUtil.inInventory();
   }

   private boolean moveOneKitItem(Map<Integer, ItemStack> kit, AutoRegear regear) {
      for (int targetSlot = 0; targetSlot < 36; targetSlot++) {
         if (this.inRange(targetSlot)) {
            ItemStack targetStack = kit.get(targetSlot);
            if (targetStack != null && !targetStack.isEmpty()) {
               ItemStack currentStack = mc.player.getInventory().getStack(targetSlot);
               if (!regear.isKitItemEqual(currentStack, targetStack)) {
                  int sourceSlot = this.findSourceSlot(targetSlot, targetStack, kit, regear);
                  if (sourceSlot != -1) {
                     this.swapInventorySlots(targetSlot, sourceSlot);
                     return true;
                  }
               }
            }
         }
      }

      return false;
   }

   private int findSourceSlot(int targetSlot, ItemStack targetStack, Map<Integer, ItemStack> kit, AutoRegear regear) {
      if (targetSlot < 9 && (this.range.is(InventoryManager.Range.All) || this.range.is(InventoryManager.Range.Hotbar))) {
         int backpackSlot = this.findBackpackSourceForHotbar(targetStack, regear);
         if (backpackSlot != -1) {
            return backpackSlot;
         }
      }

      for (int slot = 0; slot < 36; slot++) {
         if (slot != targetSlot && this.canUseSourceSlot(slot)) {
            ItemStack stack = mc.player.getInventory().getStack(slot);
            if (!stack.isEmpty() && regear.isKitItemEqual(stack, targetStack)) {
               if (this.range.is(InventoryManager.Range.Hotbar) && slot >= 9) {
                  return slot;
               }

               ItemStack sourceTarget = kit.get(slot);
               if (sourceTarget == null || sourceTarget.isEmpty() || !regear.isKitItemEqual(stack, sourceTarget)) {
                  return slot;
               }
            }
         }
      }

      return -1;
   }

   private int findBackpackSourceForHotbar(ItemStack targetStack, AutoRegear regear) {
      for (int slot = 9; slot < 36; slot++) {
         ItemStack stack = mc.player.getInventory().getStack(slot);
         if (!stack.isEmpty() && regear.isKitItemEqual(stack, targetStack)) {
            return slot;
         }
      }

      return -1;
   }

   private boolean dropOneNonKitItem(Map<Integer, ItemStack> kit, AutoRegear regear) {
      for (int slot = 35; slot >= 0; slot--) {
         if (this.inRange(slot)) {
            ItemStack stack = mc.player.getInventory().getStack(slot);
            if (!stack.isEmpty() && !this.shouldKeep(stack, kit, regear)) {
               mc.interactionManager.clickSlot(mc.player.playerScreenHandler.syncId, this.toScreenSlot(slot), 1, SlotActionType.THROW, mc.player);
               mc.player.getInventory().updateItems();
               return true;
            }
         }
      }

      return false;
   }

   private boolean shouldKeep(ItemStack stack, Map<Integer, ItemStack> kit, AutoRegear regear) {
      return this.isInKit(stack, kit, regear) || stack.getItem() instanceof ArmorItem || stack.getItem() instanceof ElytraItem;
   }

   private boolean isInKit(ItemStack stack, Map<Integer, ItemStack> kit, AutoRegear regear) {
      for (ItemStack targetStack : kit.values()) {
         if (regear.isKitItemEqual(stack, targetStack)) {
            return true;
         }
      }

      return false;
   }

   private void swapInventorySlots(int firstSlot, int secondSlot) {
      this.pickup(firstSlot);
      this.pickup(secondSlot);
      this.pickup(firstSlot);
      mc.player.getInventory().updateItems();
   }

   private void pickup(int inventorySlot) {
      mc.interactionManager.clickSlot(mc.player.playerScreenHandler.syncId, this.toScreenSlot(inventorySlot), 0, SlotActionType.PICKUP, mc.player);
   }

   private int toScreenSlot(int inventorySlot) {
      return inventorySlot < 9 ? inventorySlot + 36 : inventorySlot;
   }

   private boolean inRange(int inventorySlot) {
      if (this.range.is(InventoryManager.Range.Inventory)) {
         return inventorySlot >= 9;
      } else {
         return this.range.is(InventoryManager.Range.Hotbar) ? inventorySlot < 9 : true;
      }
   }

   private boolean canUseSourceSlot(int inventorySlot) {
      return this.range.is(InventoryManager.Range.Hotbar) ? true : this.inRange(inventorySlot);
   }

   public static boolean canMerge(ItemStack source, ItemStack stack) {
      return ItemStack.areItemsAndComponentsEqual(source, stack);
   }

   private static enum Range {
      All("All"),
      Inventory("Inventory"),
      Hotbar("Hotbar");

      private final String name;

      private Range(String name) {
         this.name = name;
      }

      @Override
      public String toString() {
         return this.name;
      }
   }
}
