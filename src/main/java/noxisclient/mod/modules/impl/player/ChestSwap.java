package noxisclient.mod.modules.impl.player;

import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.ArmorMaterials;
import net.minecraft.item.ElytraItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket.Mode;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Hand;
import noxisclient.api.utils.player.InventoryUtil;
import noxisclient.mod.modules.Module;
import noxisclient.mod.modules.settings.impl.BooleanSetting;
import noxisclient.mod.modules.settings.impl.EnumSetting;

public class ChestSwap extends Module {
   public final EnumSetting<ChestSwap.Priority> priority = this.add(new EnumSetting<>("Priority", ChestSwap.Priority.NETHERITE));
   public final BooleanSetting autoFirework = this.add(new BooleanSetting("AutoFirework", false));

   public ChestSwap() {
      super("ChestSwap", Module.Category.Player);
      this.setChinese("胸甲切换");
   }

   @Override
   public void onEnable() {
      this.swap(this.priority.getValue(), this.autoFirework.getValue());
      this.disable();
   }

   public void swap(ChestSwap.Priority priority, boolean autoFirework) {
      if (!Module.nullCheck()) {
         ItemStack armorStack = Module.mc.player.getInventory().getArmorStack(2);
         if (armorStack.getItem() instanceof ArmorItem armorItem && armorItem.getSlotType() == EquipmentSlot.CHEST) {
            int elytraSlot = this.getElytraSlot();
            if (elytraSlot != -1) {
               this.swapItems(elytraSlot);
               if (autoFirework && !Module.mc.player.isOnGround()) {
                  Module.mc.getNetworkHandler().sendPacket(new ClientCommandC2SPacket(Module.mc.player, Mode.START_FALL_FLYING));
                  Module.mc.player.startFallFlying();
                  this.useFirework();
                  return;
               }
            }

            return;
         }

         int chestplateSlot = this.getChestplateSlot(priority);
         if (chestplateSlot != -1) {
            this.swapItems(chestplateSlot);
         }
      }
   }

   private void swapItems(int slot) {
      int targetSlot = slot < 9 ? slot + 36 : slot;
      Module.mc.interactionManager.clickSlot(Module.mc.player.currentScreenHandler.syncId, targetSlot, 0, SlotActionType.PICKUP, Module.mc.player);
      Module.mc.interactionManager.clickSlot(Module.mc.player.currentScreenHandler.syncId, 6, 0, SlotActionType.PICKUP, Module.mc.player);
      Module.mc.interactionManager.clickSlot(Module.mc.player.currentScreenHandler.syncId, targetSlot, 0, SlotActionType.PICKUP, Module.mc.player);
   }

   private void useFirework() {
      int slot = -1;

      for (int i = 0; i < 45; i++) {
         ItemStack stack = Module.mc.player.getInventory().getStack(i);
         if (stack.getItem() == Items.FIREWORK_ROCKET) {
            slot = i;
            break;
         }
      }

      if (slot != -1) {
         if (slot < 9) {
            int oldSlot = Module.mc.player.getInventory().selectedSlot;
            InventoryUtil.switchToSlot(slot);
            Module.mc.interactionManager.interactItem(Module.mc.player, Hand.MAIN_HAND);
            InventoryUtil.switchToSlot(oldSlot);
         } else {
            Module.mc.interactionManager.clickSlot(Module.mc.player.currentScreenHandler.syncId, slot, 0, SlotActionType.PICKUP, Module.mc.player);
            Module.mc
               .interactionManager
               .clickSlot(
                  Module.mc.player.currentScreenHandler.syncId, Module.mc.player.getInventory().selectedSlot + 36, 0, SlotActionType.PICKUP, Module.mc.player
               );
            Module.mc.interactionManager.clickSlot(Module.mc.player.currentScreenHandler.syncId, slot, 0, SlotActionType.PICKUP, Module.mc.player);
            Module.mc.interactionManager.interactItem(Module.mc.player, Hand.MAIN_HAND);
            Module.mc.interactionManager.clickSlot(Module.mc.player.currentScreenHandler.syncId, slot, 0, SlotActionType.PICKUP, Module.mc.player);
            Module.mc
               .interactionManager
               .clickSlot(
                  Module.mc.player.currentScreenHandler.syncId, Module.mc.player.getInventory().selectedSlot + 36, 0, SlotActionType.PICKUP, Module.mc.player
               );
            Module.mc.interactionManager.clickSlot(Module.mc.player.currentScreenHandler.syncId, slot, 0, SlotActionType.PICKUP, Module.mc.player);
         }
      }
   }

   private int getChestplateSlot(ChestSwap.Priority priority) {
      int slot = -1;

      for (int i = 0; i < 36; i++) {
         ItemStack stack = Module.mc.player.getInventory().getStack(i);
         Item var6 = stack.getItem();
         if (var6 instanceof ArmorItem) {
            ArmorItem armorItem = (ArmorItem)var6;
            if (armorItem.getSlotType() == EquipmentSlot.CHEST) {
               if (armorItem.getMaterial() == ArmorMaterials.NETHERITE && priority == ChestSwap.Priority.NETHERITE) {
                  slot = i;
                  break;
               }

               if (armorItem.getMaterial() == ArmorMaterials.DIAMOND && priority == ChestSwap.Priority.DIAMOND) {
                  slot = i;
                  break;
               }

               slot = i;
            }
         }
      }

      return slot;
   }

   private int getElytraSlot() {
      int slot = -1;

      for (int i = 0; i < 36; i++) {
         ItemStack stack = Module.mc.player.getInventory().getStack(i);
         if (stack.getItem() instanceof ElytraItem) {
            slot = i;
            break;
         }
      }

      return slot;
   }

   public static enum Priority {
      NETHERITE,
      DIAMOND;
   }
}
