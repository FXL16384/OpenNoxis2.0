package noxisclient.mod.modules.impl.combat;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ExperienceBottleItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.util.Hand;
import noxisclient.Alien;
import noxisclient.api.events.impl.UpdateEvent;
import noxisclient.api.utils.math.Timer;
import noxisclient.api.utils.player.EntityUtil;
import noxisclient.api.utils.player.InventoryUtil;
import noxisclient.mod.modules.Module;
import noxisclient.mod.modules.settings.impl.BooleanSetting;
import noxisclient.mod.modules.settings.impl.EnumSetting;
import noxisclient.mod.modules.settings.impl.SliderSetting;

public class AutoXP extends Module {
   public static AutoXP INSTANCE;
   private final BooleanSetting multiTask = this.add(new BooleanSetting("MultiTask", false));
   private final EnumSetting<AutoXP.SwapMode> swap = this.add(new EnumSetting<>("Swap", AutoXP.SwapMode.Silent));
   private final SliderSetting delay = this.add(new SliderSetting("Delay", 1.0, 1.0, 10.0));
   private final SliderSetting shiftTicks = this.add(new SliderSetting("ShiftTicks", 1, 1, 64));
   private final BooleanSetting durabilityCheck = this.add(new BooleanSetting("DurabilityCheck", true));
   private final BooleanSetting rotate = this.add(new BooleanSetting("Rotate", false));
   private final BooleanSetting swing = this.add(new BooleanSetting("Swing", false));
   private final Timer delayTimer = new Timer();

   public AutoXP() {
      super("AutoXP", "Automatically throws xp silently.", Module.Category.Combat);
      this.setChinese("自动经验瓶");
      INSTANCE = this;
   }

   @Override
   public String getInfo() {
      return String.valueOf(InventoryUtil.getItemCount(Items.EXPERIENCE_BOTTLE));
   }

   public void onUpdate(UpdateEvent event) {
      if (!nullCheck() && this.delayTimer.passedMs(this.delay.getValue() * 50.0)) {
         if (!mc.player.isUsingItem() || this.multiTask.getValue()) {
            if (this.durabilityCheck.getValue() && this.areItemsFullDura(mc.player)) {
               this.disable();
            } else {
               int slot = this.findXpSlot();
               if (slot == -1) {
                  this.disable();
               } else {
                  boolean inventorySwap = this.swap.getValue() == AutoXP.SwapMode.Inventory;
                  int selectedSlot = mc.player.getInventory().selectedSlot;
                  float yaw = mc.player.getYaw();
                  float pitch = this.rotate.getValue() ? 90.0F : mc.player.getPitch();
                  if (inventorySwap) {
                     Alien.INVENTORY.inventorySwap(slot, selectedSlot);
                  } else {
                     Alien.INVENTORY.setSlot(slot);
                  }

                  if (this.rotate.getValue()) {
                     Alien.ROTATION.snapAt(yaw, pitch);
                  }

                  for (int i = 0; i < this.shiftTicks.getValueInt(); i++) {
                     sendSequencedPacket(id -> new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, id, yaw, pitch));
                     if (this.swing.getValue()) {
                        mc.player.swingHand(Hand.MAIN_HAND);
                     }
                  }

                  if (this.rotate.getValue()) {
                     Alien.ROTATION.snapBack();
                  }

                  if (inventorySwap) {
                     Alien.INVENTORY.inventorySwap(slot, selectedSlot);
                     EntityUtil.syncInventory();
                  } else {
                     Alien.INVENTORY.syncToClient();
                  }

                  this.delayTimer.reset();
               }
            }
         }
      }
   }

   private int findXpSlot() {
      return this.swap.getValue() == AutoXP.SwapMode.Inventory ? InventoryUtil.findItemInventorySlot(Items.EXPERIENCE_BOTTLE) : this.findXpHotbarSlot();
   }

   private int findXpHotbarSlot() {
      for (int i = 0; i < 9; i++) {
         ItemStack stack = mc.player.getInventory().getStack(i);
         if (stack.getItem() instanceof ExperienceBottleItem) {
            return i;
         }
      }

      return -1;
   }

   private boolean areItemsFullDura(PlayerEntity player) {
      if (this.isItemFullDura(player.getMainHandStack()) && this.isItemFullDura(player.getOffHandStack())) {
         for (ItemStack stack : player.getArmorItems()) {
            if (!this.isItemFullDura(stack)) {
               return false;
            }
         }

         return true;
      } else {
         return false;
      }
   }

   private boolean isItemFullDura(ItemStack stack) {
      if (stack.isEmpty()) {
         return true;
      } else {
         int maxDura = stack.getMaxDamage();
         int currentDura = stack.getDamage();
         return currentDura == 0 || maxDura == 0;
      }
   }

   private static enum SwapMode {
      Silent,
      Inventory;
   }
}
