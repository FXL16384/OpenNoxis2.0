package noxisclient.mod.modules.impl.player;

import java.util.HashMap;
import java.util.Map.Entry;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.ArmorMaterials;
import net.minecraft.item.ElytraItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.network.packet.c2s.play.PickFromInventoryC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket.Mode;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Hand;
import noxisclient.Alien;
import noxisclient.api.events.impl.UpdateEvent;
import noxisclient.api.utils.player.EntityUtil;
import noxisclient.api.utils.player.InventoryUtil;
import noxisclient.api.utils.player.MovementUtil;
import noxisclient.mod.modules.Module;
import noxisclient.mod.modules.impl.movement.ElytraFly;
import noxisclient.mod.modules.settings.enums.SwingSide;
import noxisclient.mod.modules.settings.impl.BindSetting;
import noxisclient.mod.modules.settings.impl.BooleanSetting;
import noxisclient.mod.modules.settings.impl.EnumSetting;
import noxisclient.mod.modules.settings.impl.SliderSetting;

public class AutoArmor extends Module {
   public static AutoArmor INSTANCE;
   private final EnumSetting<AutoArmor.EnchantPriority> head = this.add(new EnumSetting<>("Head", AutoArmor.EnchantPriority.Protection));
   private final EnumSetting<AutoArmor.EnchantPriority> body = this.add(new EnumSetting<>("Body", AutoArmor.EnchantPriority.Protection));
   private final EnumSetting<AutoArmor.EnchantPriority> tights = this.add(new EnumSetting<>("Tights", AutoArmor.EnchantPriority.Protection));
   private final EnumSetting<AutoArmor.EnchantPriority> feet = this.add(new EnumSetting<>("Feet", AutoArmor.EnchantPriority.Protection));
   private final BindSetting priorityKey = this.add(new BindSetting("PriorityKey", -1));
   private final BooleanSetting armorProtect = this.add(new BooleanSetting("ArmorProtect", false));
   private final SliderSetting minDurability = this.add(new SliderSetting("MinDurability", 25.0, 0.0, 100.0, 1.0, this.armorProtect::getValue).setSuffix("%"));
   private final BindSetting protectKey = this.add(new BindSetting("ProtectKey", -1, this.armorProtect::getValue));
   private final BooleanSetting ignoreCurse = this.add(new BooleanSetting("IgnoreCurse", true));
   private final BooleanSetting noMove = this.add(new BooleanSetting("NoMove", false));
   private final SliderSetting delay = this.add(new SliderSetting("Delay", 3.0, 0.0, 10.0, 1.0));
   private final BooleanSetting autoElytra = this.add(new BooleanSetting("AutoElytra", true));
   private final EnumSetting<AutoArmor.HotbarSwapMode> hotbarSwap = this.add(new EnumSetting<>("HotbarSwap", AutoArmor.HotbarSwapMode.Swap));
   private final EnumSetting<AutoArmor.InventorySwapMode> inventorySwap = this.add(new EnumSetting<>("InventorySwap", AutoArmor.InventorySwapMode.ClickSlot));
   private int tickDelay = 0;
   private boolean priorityKeyDown = false;
   private boolean preferElytra = false;

   public AutoArmor() {
      super("AutoArmor", Module.Category.Player);
      this.setChinese("自动穿甲");
      INSTANCE = this;
   }

   public void onUpdate(UpdateEvent event) {
      if (EntityUtil.inInventory()) {
         if (mc.player.playerScreenHandler == mc.player.currentScreenHandler) {
            if (!MovementUtil.isMoving() || !this.noMove.getValue()) {
               this.handlePriorityKey();
               if (this.tickDelay > 0) {
                  this.tickDelay--;
               } else {
                  this.tickDelay = this.delay.getValueInt();
                  boolean forceArmor = this.armorProtect.getValue() && this.protectKey.isPressed();
                  if (!this.armorProtect.getValue() || forceArmor || !this.moveLowArmorToInventory()) {
                     HashMap<EquipmentSlot, int[]> armorMap = new HashMap<>(4);
                     armorMap.put(EquipmentSlot.FEET, new int[]{36, this.getProtection(mc.player.getInventory().getStack(36)), -1, -1});
                     armorMap.put(EquipmentSlot.LEGS, new int[]{37, this.getProtection(mc.player.getInventory().getStack(37)), -1, -1});
                     armorMap.put(EquipmentSlot.CHEST, new int[]{38, this.getProtection(mc.player.getInventory().getStack(38)), -1, -1});
                     armorMap.put(EquipmentSlot.HEAD, new int[]{39, this.getProtection(mc.player.getInventory().getStack(39)), -1, -1});
                     boolean preferElytraNow = this.shouldPreferElytra();
                     boolean hasUsableElytra = preferElytraNow && this.hasUsableElytra();

                     for (int s = 0; s < 36; s++) {
                        ItemStack stack = mc.player.getInventory().getStack(s);
                        if (this.isWearable(stack) && (!this.armorProtect.getValue() || forceArmor || !this.isDurabilityLow(stack))) {
                           if (stack.getItem() instanceof ElytraItem) {
                              if (preferElytraNow && ElytraItem.isUsable(stack)) {
                                 int[] chestData = armorMap.get(EquipmentSlot.CHEST);
                                 ItemStack currentChest = mc.player.getInventory().getStack(chestData[0]);
                                 if (!(currentChest.getItem() instanceof ElytraItem) || !ElytraItem.isUsable(currentChest)) {
                                    chestData[2] = s;
                                    chestData[3] = this.getProtection(stack);
                                 }
                              }
                           } else {
                              int protection = this.getProtection(stack);
                              EquipmentSlot slot = ((ArmorItem)stack.getItem()).getSlotType();
                              if (slot != EquipmentSlot.CHEST || !hasUsableElytra) {
                                 for (Entry e : armorMap.entrySet()) {
                                    if (protection > 0 && e.getKey() == slot && protection > ((int[])e.getValue())[1] && protection > ((int[])e.getValue())[3]) {
                                       ((int[])e.getValue())[2] = s;
                                       ((int[])e.getValue())[3] = protection;
                                    }
                                 }
                              }
                           }
                        }
                     }

                     for (Entry equipmentSlotEntry : armorMap.entrySet()) {
                        if (((int[])equipmentSlotEntry.getValue())[2] != -1) {
                           this.equipArmor(
                              ((int[])equipmentSlotEntry.getValue())[0], ((int[])equipmentSlotEntry.getValue())[2], ((int[])equipmentSlotEntry.getValue())[1]
                           );
                           return;
                        }
                     }
                  }
               }
            }
         }
      }
   }

   private void handlePriorityKey() {
      boolean pressed = this.priorityKey.isPressed();
      if (!pressed) {
         this.priorityKeyDown = false;
      } else if (!this.priorityKeyDown) {
         this.priorityKeyDown = true;
         this.togglePriority(this.head);
         this.togglePriority(this.body);
         this.togglePriority(this.tights);
         this.togglePriority(this.feet);
      }
   }

   private void togglePriority(EnumSetting<AutoArmor.EnchantPriority> setting) {
      setting.setValue(setting.is(AutoArmor.EnchantPriority.Protection) ? AutoArmor.EnchantPriority.Blast : AutoArmor.EnchantPriority.Protection);
   }

   public boolean macroChestSwap(AutoArmor.ChestPriority priority, boolean firework, SwingSide swing) {
      if (nullCheck() || !EntityUtil.inInventory()) {
         return false;
      } else if (mc.player.playerScreenHandler != mc.player.currentScreenHandler) {
         return false;
      } else if (MovementUtil.isMoving() && this.noMove.getValue()) {
         return false;
      } else {
         boolean swapped = this.swapChest(priority, firework, swing);
         if (swapped) {
            this.tickDelay = this.delay.getValueInt();
         }

         return swapped;
      }
   }

   private boolean swapChest(AutoArmor.ChestPriority priority, boolean firework, SwingSide swing) {
      ItemStack chestStack = mc.player.getInventory().getStack(38);
      if (chestStack.getItem() instanceof ArmorItem armorItem && armorItem.getSlotType() == EquipmentSlot.CHEST) {
         int elytraSlot = this.getElytraSlot();
         if (elytraSlot == -1) {
            return false;
         }

         this.swapChestItem(elytraSlot);
         this.preferElytra = true;
         if (firework && !mc.player.isOnGround()) {
            mc.getNetworkHandler().sendPacket(new ClientCommandC2SPacket(mc.player, Mode.START_FALL_FLYING));
            mc.player.startFallFlying();
            this.useFirework(swing);
         }

         return true;
      }

      int chestplateSlot = this.getChestplateSlot(priority);
      if (chestplateSlot == -1) {
         return false;
      } else {
         this.swapChestItem(chestplateSlot);
         this.preferElytra = false;
         return true;
      }
   }

   private void swapChestItem(int inventorySlot) {
      int targetSlot = this.toScreenSlot(inventorySlot);
      mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, targetSlot, 0, SlotActionType.PICKUP, mc.player);
      mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, this.getArmorScreenSlot(38), 0, SlotActionType.PICKUP, mc.player);
      mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, targetSlot, 0, SlotActionType.PICKUP, mc.player);
      EntityUtil.syncInventory();
   }

   private void useFirework(SwingSide swing) {
      int slot = this.getFireworkSlot();
      if (slot != -1) {
         if (slot < 9) {
            int oldSlot = mc.player.getInventory().selectedSlot;
            InventoryUtil.switchToSlot(slot);
            this.useSelectedFirework(swing);
            InventoryUtil.switchToSlot(oldSlot);
         } else {
            int screenSlot = this.toScreenSlot(slot);
            int hotbarSlot = mc.player.getInventory().selectedSlot + 36;
            mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, screenSlot, 0, SlotActionType.PICKUP, mc.player);
            mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, hotbarSlot, 0, SlotActionType.PICKUP, mc.player);
            mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, screenSlot, 0, SlotActionType.PICKUP, mc.player);
            this.useSelectedFirework(swing);
            mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, screenSlot, 0, SlotActionType.PICKUP, mc.player);
            mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, hotbarSlot, 0, SlotActionType.PICKUP, mc.player);
            mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, screenSlot, 0, SlotActionType.PICKUP, mc.player);
         }
      }
   }

   private void useSelectedFirework(SwingSide swing) {
      sendSequencedPacket(id -> new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, id, Alien.ROTATION.getLastYaw(), Alien.ROTATION.getLastPitch()));
      EntityUtil.swingHand(Hand.MAIN_HAND, swing);
   }

   private int getFireworkSlot() {
      for (int i = 0; i < 36; i++) {
         if (mc.player.getInventory().getStack(i).getItem() == Items.FIREWORK_ROCKET) {
            return i;
         }
      }

      return -1;
   }

   private int getChestplateSlot(AutoArmor.ChestPriority priority) {
      int slot = -1;

      for (int i = 0; i < 36; i++) {
         ItemStack stack = mc.player.getInventory().getStack(i);
         Item var6 = stack.getItem();
         if (var6 instanceof ArmorItem) {
            ArmorItem armorItem = (ArmorItem)var6;
            if (armorItem.getSlotType() == EquipmentSlot.CHEST
               && (!this.armorProtect.getValue() || this.protectKey.isPressed() || !this.isDurabilityLow(stack))) {
               if (armorItem.getMaterial() == ArmorMaterials.NETHERITE && priority == AutoArmor.ChestPriority.NETHERITE) {
                  return i;
               }

               if (armorItem.getMaterial() == ArmorMaterials.DIAMOND && priority == AutoArmor.ChestPriority.DIAMOND) {
                  return i;
               }

               slot = i;
            }
         }
      }

      return slot;
   }

   private int getElytraSlot() {
      for (int i = 0; i < 36; i++) {
         ItemStack stack = mc.player.getInventory().getStack(i);
         if (stack.getItem() instanceof ElytraItem
            && ElytraItem.isUsable(stack)
            && (!this.armorProtect.getValue() || this.protectKey.isPressed() || !this.isDurabilityLow(stack))) {
            return i;
         }
      }

      return -1;
   }

   private boolean moveLowArmorToInventory() {
      for (int armorSlot = 36; armorSlot <= 39; armorSlot++) {
         ItemStack stack = mc.player.getInventory().getStack(armorSlot);
         if (this.isWearable(stack) && this.isDurabilityLow(stack)) {
            int emptySlot = this.getEmptyInventorySlot();
            if (emptySlot != -1 && mc.player.currentScreenHandler.getCursorStack().isEmpty()) {
               mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, this.getArmorScreenSlot(armorSlot), 0, SlotActionType.PICKUP, mc.player);
               mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, this.toScreenSlot(emptySlot), 0, SlotActionType.PICKUP, mc.player);
               EntityUtil.syncInventory();
               return true;
            }

            return false;
         }
      }

      return false;
   }

   private int getEmptyInventorySlot() {
      for (int i = 0; i < 36; i++) {
         if (mc.player.getInventory().getStack(i).isEmpty()) {
            return i;
         }
      }

      return -1;
   }

   private boolean shouldPreferElytra() {
      return this.preferElytra || this.autoElytra.getValue() && ElytraFly.INSTANCE.isOn() && !ElytraFly.INSTANCE.packet.getValue();
   }

   private boolean hasUsableElytra() {
      ItemStack chestStack = mc.player.getInventory().getStack(38);
      return chestStack.getItem() instanceof ElytraItem && ElytraItem.isUsable(chestStack)
         ? !this.armorProtect.getValue() || this.protectKey.isPressed() || !this.isDurabilityLow(chestStack)
         : this.getElytraSlot() != -1;
   }

   private boolean isWearable(ItemStack stack) {
      return stack.getItem() instanceof ArmorItem || stack.getItem() instanceof ElytraItem;
   }

   private boolean isDurabilityLow(ItemStack stack) {
      if (!stack.isDamageable()) {
         return false;
      } else {
         int maxDamage = stack.getMaxDamage();
         if (maxDamage <= 0) {
            return false;
         } else {
            double durability = (double)(maxDamage - stack.getDamage()) * 100.0 / (double)maxDamage;
            return durability < this.minDurability.getValue();
         }
      }
   }

   private int toScreenSlot(int inventorySlot) {
      return inventorySlot < 9 ? inventorySlot + 36 : inventorySlot;
   }

   private int getArmorScreenSlot(int armorInventorySlot) {
      return 44 - armorInventorySlot;
   }

   private void equipArmor(int armorInventorySlot, int newArmorSlot, int oldProtection) {
      if (newArmorSlot < 9) {
         switch ((AutoArmor.HotbarSwapMode)this.hotbarSwap.getValue()) {
            case Swap:
               mc.interactionManager
                  .clickSlot(mc.player.currentScreenHandler.syncId, this.getArmorScreenSlot(armorInventorySlot), newArmorSlot, SlotActionType.SWAP, mc.player);
               EntityUtil.syncInventory();
               break;
            case Switch:
               int old = mc.player.getInventory().selectedSlot;
               InventoryUtil.switchToSlot(newArmorSlot);
               sendSequencedPacket(id -> new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, id, Alien.ROTATION.getLastYaw(), Alien.ROTATION.getLastPitch()));
               InventoryUtil.switchToSlot(old);
         }
      } else {
         int armorSlot = this.getArmorScreenSlot(armorInventorySlot);
         switch ((AutoArmor.InventorySwapMode)this.inventorySwap.getValue()) {
            case ClickSlot:
               mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, newArmorSlot, 0, SlotActionType.PICKUP, mc.player);
               mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, armorSlot, 0, SlotActionType.PICKUP, mc.player);
               if (oldProtection != -1) {
                  mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, newArmorSlot, 0, SlotActionType.PICKUP, mc.player);
               }
               break;
            case Pick:
               mc.getNetworkHandler().sendPacket(new PickFromInventoryC2SPacket(newArmorSlot));
               sendSequencedPacket(id -> new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, id, Alien.ROTATION.getLastYaw(), Alien.ROTATION.getLastPitch()));
               mc.getNetworkHandler().sendPacket(new PickFromInventoryC2SPacket(newArmorSlot));
         }

         EntityUtil.syncInventory();
      }
   }

   private int getProtection(ItemStack is) {
      if (is.getItem() instanceof ArmorItem || is.getItem() == Items.ELYTRA) {
         int prot = 0;
         EquipmentSlot slot;
         if (is.getItem() instanceof ArmorItem ai) {
            slot = ai.getSlotType();
         } else {
            slot = EquipmentSlot.BODY;
         }

         if (is.getItem() instanceof ElytraItem) {
            if (!ElytraItem.isUsable(is)) {
               return 0;
            }

            prot = 1;
         }

         int blastMultiplier = 1;
         int protectionMultiplier = 1;
         switch (slot) {
            case HEAD:
               if (this.head.is(AutoArmor.EnchantPriority.Protection)) {
                  protectionMultiplier *= 2;
               } else {
                  blastMultiplier *= 2;
               }
               break;
            case CHEST:
            case BODY:
               if (this.body.is(AutoArmor.EnchantPriority.Protection)) {
                  protectionMultiplier *= 2;
               } else {
                  blastMultiplier *= 2;
               }
               break;
            case LEGS:
               if (this.tights.is(AutoArmor.EnchantPriority.Protection)) {
                  protectionMultiplier *= 2;
               } else {
                  blastMultiplier *= 2;
               }
               break;
            case FEET:
               if (this.feet.is(AutoArmor.EnchantPriority.Protection)) {
                  protectionMultiplier *= 2;
               } else {
                  blastMultiplier *= 2;
               }
         }

         if (is.hasEnchantments()) {
            prot += InventoryUtil.getEnchantmentLevel(is, Enchantments.PROTECTION) * protectionMultiplier;
            prot += InventoryUtil.getEnchantmentLevel(is, Enchantments.BLAST_PROTECTION) * blastMultiplier;
            if (InventoryUtil.getEnchantmentLevel(is, Enchantments.BINDING_CURSE) > 0 && this.ignoreCurse.getValue()) {
               prot = -999;
            }
         }

         return (is.getItem() instanceof ArmorItem ? ((ArmorItem)is.getItem()).getProtection() : 0) + prot;
      } else {
         return !is.isEmpty() ? 0 : -1;
      }
   }

   public static enum ChestPriority {
      NETHERITE,
      DIAMOND;
   }

   private static enum EnchantPriority {
      Blast,
      Protection;
   }

   public static enum HotbarSwapMode {
      Swap,
      Switch;
   }

   public static enum InventorySwapMode {
      ClickSlot,
      Pick;
   }
}
