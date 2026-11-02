package noxisclient.mod.modules.impl.combat;

import net.minecraft.block.Block;
import net.minecraft.client.gui.screen.ingame.CreativeInventoryScreen;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.PickaxeItem;
import net.minecraft.item.SwordItem;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult.Type;
import net.minecraft.util.math.MathHelper;
import noxisclient.Alien;
import noxisclient.api.events.impl.ClientTickEvent;
import noxisclient.api.events.impl.InteractBlockEvent;
import noxisclient.api.utils.math.Timer;
import noxisclient.api.utils.player.EntityUtil;
import noxisclient.api.utils.player.InventoryUtil;
import noxisclient.api.utils.world.BlockUtil;
import noxisclient.mod.modules.Module;
import noxisclient.mod.modules.impl.render.SpearFix;
import noxisclient.mod.modules.settings.impl.BooleanSetting;
import noxisclient.mod.modules.settings.impl.SliderSetting;

public class Mainhand extends Module {
   private static final int INVALID_SLOT = -1;
   private final SliderSetting slot = this.add(new SliderSetting("Slot", 5.0, 1.0, 9.0, 1.0));
   private final SliderSetting delay = this.add(new SliderSetting("Delay", 0.0, 0.0, 1000.0, 25.0).setSuffix("ms"));
   private final SliderSetting swapDelay = this.add(new SliderSetting("SwapDelay", 0.0, 0.0, 1000.0, 25.0).setSuffix("ms"));
   private final BooleanSetting autoSelect = this.add(new BooleanSetting("AutoSelect", true));
   private final BooleanSetting lethal = this.add(new BooleanSetting("Lethal", true, this.autoSelect::getValue));
   private final SliderSetting health = this.add(new SliderSetting("Health", 14.0, 0.0, 36.0, 0.5, this.autoSelect::getValue));
   private final BooleanSetting gappleBind = this.add(new BooleanSetting("GappleBind", true).setParent());
   private final SliderSetting safe = this.add(new SliderSetting("SafeHealth", 16.0, 0.0, 36.0, 0.5, this.gappleBind::isOpen));
   private final BooleanSetting switchEat = this.add(new BooleanSetting("SwitchEat", false).setParent());
   private final BooleanSetting switchEatSword = this.add(new BooleanSetting("Sword", true, this.switchEat::isOpen));
   private final BooleanSetting switchEatPickaxe = this.add(new BooleanSetting("Pickaxe", true, this.switchEat::isOpen));
   private final BooleanSetting switchEatSpear = this.add(new BooleanSetting("Spear", true, this.switchEat::isOpen));
   private final BooleanSetting switchEatTotem = this.add(new BooleanSetting("Totem", true, this.switchEat::isOpen));
   private final BooleanSetting ignoreSpear = this.add(new BooleanSetting("IgnoreSpear", false, this.switchEat::isOpen));
   private final Timer swapTimer = new Timer();
   private final Timer selectTimer = new Timer();
   private final Timer dangerTimer = new Timer();
   private final Timer interactTimer = new Timer();
   private int lastSourceSlot = -1;
   private int switchEatReturnSlot = -1;
   private boolean switchEatSwapped;

   public Mainhand() {
      super("Mainhand", "Auto totem for main hand.", Module.Category.Combat);
      this.setChinese("主手");
   }

   @Override
   public void onEnable() {
      this.lastSourceSlot = -1;
      this.switchEatReturnSlot = -1;
      this.switchEatSwapped = false;
      this.swapTimer.setMs((long)this.delay.getValue());
      this.selectTimer.setMs((long)this.swapDelay.getValue());
      this.dangerTimer.setMs(750L);
      this.interactTimer.setMs(200L);
   }

   public void onTick(ClientTickEvent event) {
      if (!event.isPost() && !nullCheck()) {
         if (!this.canOperate()) {
            this.restoreSwitchEatSlot();
         } else {
            this.updateSwitchEat();
            this.updateDangerTimer();
            Item targetItem = this.getTargetItem();
            int targetSlot = this.getTargetSlot();
            if (!mc.player.getInventory().getStack(targetSlot).isOf(targetItem)) {
               this.moveItemToHotbar(targetItem, targetSlot);
            }

            if (this.autoSelect.getValue()) {
               this.selectTotemSlot(targetSlot);
            }
         }
      }
   }

   public void onInteractBlock(InteractBlockEvent event) {
      if (!nullCheck() && event.isPost()) {
         if (this.isUsingClickableBlock()) {
            this.interactTimer.reset();
         }
      }
   }

   private boolean canOperate() {
      return mc.interactionManager != null
         && !(mc.currentScreen instanceof CreativeInventoryScreen)
         && mc.player.currentScreenHandler == mc.player.playerScreenHandler;
   }

   private void updateSwitchEat() {
      if (!this.switchEat.getValue()) {
         this.restoreSwitchEatSlot();
      } else {
         ItemStack mainHand = mc.player.getMainHandStack();
         boolean spear = SpearFix.isSpearItem(mainHand);
         if (this.ignoreSpear.getValue() && spear) {
            this.restoreSwitchEatSlot();
         } else if (!mc.options.useKey.isPressed()) {
            this.restoreSwitchEatSlot();
         } else if (!this.switchEatSwapped && this.isSwitchEatSource(mainHand, spear) && !this.isGapple(mc.player.getOffHandStack())) {
            int gappleSlot = InventoryUtil.findItem(Items.ENCHANTED_GOLDEN_APPLE);
            if (gappleSlot == -1) {
               gappleSlot = InventoryUtil.findItem(Items.GOLDEN_APPLE);
            }

            if (gappleSlot != -1) {
               this.switchEatReturnSlot = mc.player.getInventory().selectedSlot;
               InventoryUtil.switchToSlot(gappleSlot);
               this.switchEatSwapped = true;
            }
         }
      }
   }

   private boolean isSwitchEatSource(ItemStack stack, boolean spear) {
      if (spear) {
         return this.switchEatSpear.getValue();
      } else if (stack.getItem() instanceof SwordItem) {
         return this.switchEatSword.getValue();
      } else {
         return stack.getItem() instanceof PickaxeItem
            ? this.switchEatPickaxe.getValue()
            : stack.isOf(Items.TOTEM_OF_UNDYING) && this.switchEatTotem.getValue();
      }
   }

   private boolean isGapple(ItemStack stack) {
      return stack.isOf(Items.ENCHANTED_GOLDEN_APPLE) || stack.isOf(Items.GOLDEN_APPLE);
   }

   private void restoreSwitchEatSlot() {
      if (this.switchEatSwapped && this.switchEatReturnSlot >= 0 && this.switchEatReturnSlot < 9 && mc.player != null && mc.getNetworkHandler() != null) {
         InventoryUtil.switchToSlot(this.switchEatReturnSlot);
      }

      this.switchEatSwapped = false;
      this.switchEatReturnSlot = -1;
   }

   private Item getTargetItem() {
      int targetSlot = this.getTargetSlot();
      boolean selectedTargetSlot = mc.player.getInventory().selectedSlot == targetSlot;
      boolean canGapple = selectedTargetSlot
         && this.gappleBind.getValue()
         && mc.options.useKey.isPressed()
         && this.isMainhandManagedItem()
         && !this.isUsingClickableBlock()
         && (double)EntityUtil.getHealth(mc.player) >= this.safe.getValue();
      return canGapple ? Items.ENCHANTED_GOLDEN_APPLE : Items.TOTEM_OF_UNDYING;
   }

   private boolean isMainhandManagedItem() {
      ItemStack mainHand = mc.player.getMainHandStack();
      return mc.player.getOffHandStack().getComponents().contains(DataComponentTypes.FOOD)
         ? false
         : mainHand.isOf(Items.TOTEM_OF_UNDYING) || mainHand.isOf(Items.ENCHANTED_GOLDEN_APPLE);
   }

   private void moveItemToHotbar(Item item, int targetSlot) {
      if (this.swapTimer.passed((long)this.delay.getValue()) && this.interactTimer.passed(200L)) {
         int sourceSlot = this.findSlot(item, targetSlot);
         if (sourceSlot != -1) {
            ScreenHandler handler = mc.player.currentScreenHandler;
            if (sourceSlot < handler.slots.size()) {
               ItemStack sourceStack = handler.getSlot(sourceSlot).getStack();
               if (!this.isFood(sourceStack) || mc.options.useKey.isPressed()) {
                  mc.interactionManager.clickSlot(handler.syncId, sourceSlot, targetSlot, SlotActionType.SWAP, mc.player);
                  mc.player.getInventory().updateItems();
                  this.lastSourceSlot = sourceSlot;
                  this.swapTimer.reset();
                  if (mc.options.useKey.isPressed() && this.isFood(mc.player.getInventory().getStack(targetSlot))) {
                     mc.interactionManager.interactItem(mc.player, Hand.OFF_HAND);
                  }
               }
            }
         }
      }
   }

   private int findSlot(Item item, int targetSlot) {
      if (this.lastSourceSlot != -1) {
         int inventorySlot = this.toInventorySlot(this.lastSourceSlot);
         if (inventorySlot >= 0 && inventorySlot < 36 && inventorySlot != targetSlot && mc.player.getInventory().getStack(inventorySlot).isOf(item)) {
            return this.lastSourceSlot;
         }
      }

      for (int i = 35; i >= 9; i--) {
         if (i != targetSlot && mc.player.getInventory().getStack(i).isOf(item)) {
            return this.toScreenSlot(i);
         }
      }

      return -1;
   }

   private void selectTotemSlot(int targetSlot) {
      ItemStack stack = mc.player.getInventory().getStack(targetSlot);
      if (stack.isOf(Items.TOTEM_OF_UNDYING)
         && mc.player.getInventory().selectedSlot != targetSlot
         && (!mc.player.isUsingItem() || mc.player.getActiveHand() != Hand.MAIN_HAND)
         && this.shouldSelectTotem()
         && this.selectTimer.passed((long)this.swapDelay.getValue())) {
         InventoryUtil.switchToSlot(targetSlot);
         this.switchEatSwapped = false;
         this.switchEatReturnSlot = -1;
         this.selectTimer.reset();
      }
   }

   private boolean shouldSelectTotem() {
      float playerHealth = EntityUtil.getHealth(mc.player);
      return (double)playerHealth <= this.health.getValue() ? true : this.isDangerActive();
   }

   private int getTargetSlot() {
      return MathHelper.clamp(this.slot.getValueInt() - 1, 0, 8);
   }

   private int toScreenSlot(int inventorySlot) {
      return inventorySlot < 9 ? inventorySlot + 36 : inventorySlot;
   }

   private int toInventorySlot(int screenSlot) {
      return screenSlot >= 36 && screenSlot <= 44 ? screenSlot - 36 : screenSlot;
   }

   private boolean isUsingClickableBlock() {
      if (mc.crosshairTarget instanceof BlockHitResult hit && hit.getType() == Type.BLOCK) {
         Block block = mc.world.getBlockState(hit.getBlockPos()).getBlock();
         return !mc.player.isSneaking() && BlockUtil.isClickable(block);
      }

      return false;
   }

   private boolean isFood(ItemStack stack) {
      return !stack.isEmpty() && stack.getComponents().contains(DataComponentTypes.FOOD);
   }

   private void updateDangerTimer() {
      if (this.lethal.getValue() && this.autoSelect.getValue()) {
         for (Entity entity : Alien.THREAD.getEntities()) {
            if (entity instanceof EndCrystalEntity && this.isLethalCrystal(entity)) {
               this.dangerTimer.reset();
               return;
            }
         }
      }
   }

   private boolean isDangerActive() {
      return this.lethal.getValue() && this.autoSelect.getValue() && !this.dangerTimer.passed(750L);
   }

   private boolean isLethalCrystal(Entity crystal) {
      return mc.player.squaredDistanceTo(crystal) <= 144.0
         && AutoCrystal.INSTANCE.calculateDamage(crystal.getPos(), mc.player, mc.player) >= EntityUtil.getHealth(mc.player);
   }

   private double getCrystalDamage() {
      double crystalDamage = 0.0;

      for (Entity entity : Alien.THREAD.getEntities()) {
         if (entity instanceof EndCrystalEntity && mc.player.squaredDistanceTo(entity) <= 144.0) {
            double damage = (double)AutoCrystal.INSTANCE.calculateDamage(entity.getPos(), mc.player, mc.player);
            crystalDamage = Math.max(crystalDamage, damage);
         }
      }

      return crystalDamage;
   }

   private int getFallDamage() {
      StatusEffectInstance jumpBoost = mc.player.getStatusEffect(StatusEffects.JUMP_BOOST);
      float boost = jumpBoost == null ? 0.0F : (float)(jumpBoost.getAmplifier() + 1);
      return Math.max(0, MathHelper.ceil(mc.player.fallDistance - 3.0F - boost));
   }

   @Override
   public void onDisable() {
      if (!nullCheck()) {
         this.restoreSwitchEatSlot();
      } else {
         this.switchEatSwapped = false;
         this.switchEatReturnSlot = -1;
      }
   }

   @Override
   public String getInfo() {
      Item item = nullCheck() ? Items.TOTEM_OF_UNDYING : this.getTargetItem();
      int count = nullCheck() ? 0 : mc.player.getInventory().main.stream().filter(stack -> stack.isOf(item)).mapToInt(ItemStack::getCount).sum();
      return String.valueOf(count);
   }
}
