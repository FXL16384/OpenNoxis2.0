package noxisclient.mod.modules.impl.combat;

import net.minecraft.block.Block;
import net.minecraft.client.gui.screen.ingame.CreativeInventoryScreen;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.gui.screen.ingame.ShulkerBoxScreen;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.AxeItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.PickaxeItem;
import net.minecraft.item.ShieldItem;
import net.minecraft.item.SwordItem;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.EntitySpawnS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;
import net.minecraft.network.packet.s2c.play.InventoryS2CPacket;
import net.minecraft.registry.Registries;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult.Type;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import noxisclient.Alien;
import noxisclient.api.events.impl.ClientTickEvent;
import noxisclient.api.events.impl.InventoryS2CPacketEvent;
import noxisclient.api.events.impl.PacketEvent;
import noxisclient.api.events.impl.TotemEvent;
import noxisclient.api.utils.math.Timer;
import noxisclient.api.utils.player.EntityUtil;
import noxisclient.api.utils.player.InventoryUtil;
import noxisclient.api.utils.world.BlockUtil;
import noxisclient.mod.modules.Module;
import noxisclient.mod.modules.settings.impl.BooleanSetting;
import noxisclient.mod.modules.settings.impl.EnumSetting;
import noxisclient.mod.modules.settings.impl.SliderSetting;
import noxisclient.mod.modules.settings.impl.StringSetting;

public class Offhand extends Module {
   private static final int INVALID_SLOT = -1;
   private static final int OFFHAND_SCREEN_SLOT = 45;
   private static final int OFFHAND_SWAP_BUTTON = 40;
   private final EnumSetting<Offhand.ItemMode> item = this.add(new EnumSetting<>("Item", Offhand.ItemMode.Totem));
   private final SliderSetting delay = this.add(new SliderSetting("Delay", 0.0, 0.0, 1000.0, 25.0).setSuffix("ms"));
   private final SliderSetting health = this.add(new SliderSetting("Health", 14.0, 0.0, 36.0, 0.5));
   private final BooleanSetting lethal = this.add(new BooleanSetting("Lethal", true));
   private final BooleanSetting swap117 = this.add(new BooleanSetting("1.17+", true));
   private final BooleanSetting containers = this.add(new BooleanSetting("Containers", false));
   private final BooleanSetting gappleBind = this.add(new BooleanSetting("GappleBind", true).setParent());
   private final SliderSetting safe = this.add(new SliderSetting("SafeHealth", 16.0, 0.0, 36.0, 0.5, this.gappleBind::isOpen));
   private final BooleanSetting sword = this.add(new BooleanSetting("Sword", true, this.gappleBind::isOpen));
   private final BooleanSetting pickaxe = this.add(new BooleanSetting("Pickaxe", true, this.gappleBind::isOpen));
   private final BooleanSetting crystals = this.add(new BooleanSetting("Crystals", true, this.gappleBind::isOpen));
   private final BooleanSetting totems = this.add(new BooleanSetting("Totems", true, this.gappleBind::isOpen));
   private final BooleanSetting crapple = this.add(new BooleanSetting("Crapple", false, this.gappleBind::isOpen));
   private final StringSetting custom = this.add(new StringSetting("Custom", "minecraft:shield", () -> this.item.is(Offhand.ItemMode.Custom)));
   private final Timer timer = new Timer();
   private final Timer dangerTimer = new Timer();
   private boolean clearedTotem;
   private boolean isTotemInOffHand;
   private boolean isGappleInOffHand;
   private boolean targetFromGappleBind;
   private int gappleReturnSlot = -1;
   private Item gappleReturnItem = Items.AIR;

   public Offhand() {
      super("Offhand", "Manages your offhand automatically.", Module.Category.Combat);
      this.setChinese("副手");
      this.gappleBind.setOpen(false);
   }

   @Override
   public void onEnable() {
      this.timer.setMs((long)this.delay.getValue());
      this.dangerTimer.setMs(750L);
      this.clearedTotem = false;
      this.gappleBind.setOpen(false);
      this.clearGappleReturn();
   }

   public void onTick(ClientTickEvent event) {
      if (!event.isPost() && !nullCheck()) {
         this.updateDangerTimer();
         this.execute(false);
      }
   }

   public void onTotem(TotemEvent event) {
      if (!nullCheck() && event.getPlayer() == mc.player) {
         if (!mc.player.getMainHandStack().isOf(Items.TOTEM_OF_UNDYING) && mc.player.getOffHandStack().isOf(Items.TOTEM_OF_UNDYING)) {
            mc.player.getOffHandStack().decrement(1);
            this.clearedTotem = true;
         }

         this.timer.setMs((long)this.delay.getValue());
         this.execute(true);
      }
   }

   public void onInventory(InventoryS2CPacketEvent event) {
      if (this.clearedTotem) {
         InventoryS2CPacket packet = event.packet;
         if (packet.getContents().size() > 45 && packet.getContents().get(45).getItem() == Items.AIR) {
            event.cancel();
         }

         this.clearedTotem = false;
      }
   }

   public void onPacket(PacketEvent.Receive event) {
      if (!nullCheck()) {
         Packet<?> packet = event.getPacket();
         if (packet instanceof EntityStatusS2CPacket status && status.getStatus() == 35 && status.getEntity(mc.world) == mc.player) {
            this.timer.setMs((long)this.delay.getValue());
            this.execute(true);
            return;
         }

         if (packet instanceof EntitySpawnS2CPacket spawn && this.lethal.getValue() && spawn.getEntityType() == EntityType.END_CRYSTAL) {
            Vec3d pos = new Vec3d(spawn.getX(), spawn.getY(), spawn.getZ());
            if (this.isLethalCrystal(pos)) {
               this.dangerTimer.reset();
               this.timer.setMs((long)this.delay.getValue());
               this.execute(true);
            }
         }
      }
   }

   private void execute(boolean force) {
      if (this.canOperate() && (force || this.timer.passed((long)this.delay.getValue()))) {
         Offhand.ItemMode targetMode = this.getTargetMode();
         if (targetMode != null) {
            if (!this.targetFromGappleBind && this.gappleReturnSlot != -1 && !this.shouldUseTotem()) {
               if (this.restoreGappleBindItem()) {
                  this.timer.reset();
               }
            } else {
               Item targetItem = this.getItem(targetMode);
               if (targetItem != null && !mc.player.getOffHandStack().isOf(targetItem)) {
                  int sourceSlot = this.findSlot(targetItem);
                  if (sourceSlot != -1) {
                     boolean shouldRecordGappleReturn = this.targetFromGappleBind && this.gappleReturnSlot == -1;
                     Item returnItem = mc.player.getOffHandStack().isEmpty() ? Items.AIR : mc.player.getOffHandStack().getItem();
                     if (this.moveToOffhand(sourceSlot, targetItem)) {
                        if (shouldRecordGappleReturn) {
                           this.recordGappleReturn(sourceSlot, returnItem);
                        } else if (targetItem == Items.TOTEM_OF_UNDYING) {
                           this.clearGappleReturn();
                        }

                        this.timer.reset();
                        this.isTotemInOffHand = targetItem == Items.TOTEM_OF_UNDYING;
                        this.isGappleInOffHand = targetItem == Items.ENCHANTED_GOLDEN_APPLE || targetItem == Items.GOLDEN_APPLE;
                        if (mc.options.useKey.isPressed() && this.isFood(mc.player.getOffHandStack())) {
                           mc.interactionManager.interactItem(mc.player, Hand.OFF_HAND);
                        }
                     }
                  }
               } else {
                  this.isTotemInOffHand = targetItem == Items.TOTEM_OF_UNDYING;
                  this.isGappleInOffHand = targetItem == Items.ENCHANTED_GOLDEN_APPLE || targetItem == Items.GOLDEN_APPLE;
               }
            }
         }
      }
   }

   private boolean canOperate() {
      if (mc.player == null || mc.interactionManager == null) {
         return false;
      } else if (!this.swap117.getValue() && mc.currentScreen instanceof CreativeInventoryScreen) {
         return false;
      } else {
         return !EntityUtil.inInventory() && !this.containers.getValue()
            ? false
            : this.containers.getValue() || !(mc.currentScreen instanceof ShulkerBoxScreen) && !(mc.currentScreen instanceof GenericContainerScreen);
      }
   }

   private Offhand.ItemMode getTargetMode() {
      this.targetFromGappleBind = false;
      Offhand.ItemMode wanted = Offhand.ItemMode.Totem;
      if (!this.shouldUseTotem()) {
         Offhand.ItemMode configured = this.item.getValue();
         Item configuredItem = this.getItem(configured);
         if (configuredItem != null
            && (double)EntityUtil.getHealth(mc.player) > this.health.getValue()
            && mc.player.fallDistance < 16.0F
            && this.getItemCount(configuredItem) > 0) {
            wanted = configured;
         }
      }

      Item wantedItem = this.getItem(wanted);
      if (wantedItem instanceof ShieldItem) {
         return wanted;
      } else {
         Offhand.ItemMode gapple = this.getGappleBindMode(wanted);
         this.targetFromGappleBind = gapple != null;
         return gapple != null ? gapple : wanted;
      }
   }

   private Offhand.ItemMode getGappleBindMode(Offhand.ItemMode fallback) {
      if (this.gappleBind.getValue()
         && fallback != Offhand.ItemMode.Gapple
         && mc.currentScreen == null
         && mc.options.useKey.isPressed()
         && !this.isUsingClickableBlock()) {
         Item mainHand = mc.player.getMainHandStack().getItem();
         if (mainHand != Items.TOTEM_OF_UNDYING && (double)EntityUtil.getHealth(mc.player) < this.safe.getValue()) {
            return null;
         } else if (mainHand == Items.TOTEM_OF_UNDYING) {
            return Offhand.ItemMode.Gapple;
         } else {
            return this.isValidGappleBindItem(mainHand) ? Offhand.ItemMode.Gapple : null;
         }
      } else {
         return null;
      }
   }

   private boolean shouldUseTotem() {
      float playerHealth = EntityUtil.getHealth(mc.player);
      return (double)playerHealth <= this.health.getValue() ? true : this.isDangerActive();
   }

   private boolean isValidGappleBindItem(Item item) {
      if (item instanceof SwordItem) {
         return this.sword.getValue();
      } else if (item instanceof AxeItem) {
         return true;
      } else if (item instanceof PickaxeItem) {
         return this.pickaxe.getValue();
      } else {
         return item == Items.TOTEM_OF_UNDYING ? this.totems.getValue() : item == Items.END_CRYSTAL && this.crystals.getValue();
      }
   }

   private int findSlot(Item item) {
      ScreenHandler handler = mc.player.currentScreenHandler;
      if (this.containers.getValue() && mc.currentScreen instanceof HandledScreen && !(mc.currentScreen instanceof InventoryScreen)) {
         for (Slot slot : handler.slots) {
            if (slot.id != 45 && slot.getStack().isOf(item)) {
               return slot.id;
            }
         }
      }

      for (int i = 35; i >= 0; i--) {
         if (mc.player.getInventory().getStack(i).isOf(item)) {
            return this.toScreenSlot(i);
         }
      }

      return -1;
   }

   private boolean moveToOffhand(int sourceSlot, Item item) {
      ScreenHandler handler = mc.player.currentScreenHandler;
      if (sourceSlot >= handler.slots.size()) {
         return false;
      } else if (this.swap117.getValue()) {
         mc.interactionManager.clickSlot(handler.syncId, sourceSlot, 40, SlotActionType.SWAP, mc.player);
         EntityUtil.syncInventory();
         return true;
      } else if (handler.slots.size() <= 45) {
         return false;
      } else {
         this.clickSwap(handler, sourceSlot, 45, item);
         EntityUtil.syncInventory();
         return true;
      }
   }

   private void clickSwap(ScreenHandler handler, int sourceSlot, int targetSlot, Item item) {
      if (!handler.getCursorStack().isOf(item)) {
         this.pickupSlot(handler, sourceSlot);
      }

      if (handler.getCursorStack().isOf(item)) {
         this.pickupSlot(handler, targetSlot);
      }

      if (!handler.getCursorStack().isEmpty()) {
         this.pickupSlot(handler, sourceSlot);
      }
   }

   private void pickupSlot(ScreenHandler handler, int slot) {
      mc.interactionManager.clickSlot(handler.syncId, slot, 0, SlotActionType.PICKUP, mc.player);
   }

   private int toScreenSlot(int inventorySlot) {
      return inventorySlot < 9 ? inventorySlot + 36 : inventorySlot;
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

   private void recordGappleReturn(int sourceSlot, Item returnItem) {
      if (this.gappleReturnSlot == -1) {
         this.gappleReturnSlot = sourceSlot;
         this.gappleReturnItem = returnItem;
      }
   }

   private boolean restoreGappleBindItem() {
      ScreenHandler handler = mc.player.currentScreenHandler;
      if (this.gappleReturnSlot >= 0 && this.gappleReturnSlot < handler.slots.size()) {
         ItemStack storedStack = handler.getSlot(this.gappleReturnSlot).getStack();
         if (this.gappleReturnItem != Items.AIR && !storedStack.isOf(this.gappleReturnItem)) {
            this.clearGappleReturn();
            return false;
         } else if (this.swapOffhandWithSlot(this.gappleReturnSlot)) {
            this.clearGappleReturn();
            EntityUtil.syncInventory();
            return true;
         } else {
            return false;
         }
      } else {
         this.clearGappleReturn();
         return false;
      }
   }

   private boolean swapOffhandWithSlot(int slot) {
      ScreenHandler handler = mc.player.currentScreenHandler;
      if (this.swap117.getValue()) {
         mc.interactionManager.clickSlot(handler.syncId, slot, 40, SlotActionType.SWAP, mc.player);
         return true;
      } else if (handler.slots.size() <= 45) {
         return false;
      } else {
         this.pickupSlot(handler, slot);
         this.pickupSlot(handler, 45);
         if (!handler.getCursorStack().isEmpty()) {
            this.pickupSlot(handler, slot);
         }

         return true;
      }
   }

   private void clearGappleReturn() {
      this.gappleReturnSlot = -1;
      this.gappleReturnItem = Items.AIR;
   }

   private void updateDangerTimer() {
      if (this.lethal.getValue()) {
         for (Entity entity : Alien.THREAD.getEntities()) {
            if (entity instanceof EndCrystalEntity && this.isLethalCrystal(entity.getPos())) {
               this.dangerTimer.reset();
               return;
            }
         }
      }
   }

   private boolean isDangerActive() {
      return this.lethal.getValue() && !this.dangerTimer.passed(750L);
   }

   private boolean isLethalCrystal(Vec3d pos) {
      return pos.squaredDistanceTo(mc.player.getPos()) <= 144.0 && this.getCrystalDamage(pos) >= (double)EntityUtil.getHealth(mc.player);
   }

   private double getCrystalDamage() {
      double crystalDamage = 0.0;

      for (Entity entity : Alien.THREAD.getEntities()) {
         if (entity instanceof EndCrystalEntity && mc.player.squaredDistanceTo(entity) <= 144.0) {
            crystalDamage = Math.max(crystalDamage, this.getCrystalDamage(entity.getPos()));
         }
      }

      return crystalDamage;
   }

   private double getCrystalDamage(Vec3d pos) {
      return (double)AutoCrystal.INSTANCE.calculateDamage(pos, mc.player, mc.player);
   }

   private Item getItem(Offhand.ItemMode mode) {
      return mode == Offhand.ItemMode.Custom ? this.getCustomItem() : mode.getItem(this.crapple.getValue());
   }

   private Item getCustomItem() {
      try {
         Identifier id = Identifier.of(this.custom.getValue().contains(":") ? this.custom.getValue() : "minecraft:" + this.custom.getValue());
         return Registries.ITEM.get(id);
      } catch (Exception var2) {
         return Items.SHIELD;
      }
   }

   private int getItemCount(Item item) {
      int count = 0;

      for (int i = 0; i < 36; i++) {
         ItemStack stack = mc.player.getInventory().getStack(i);
         if (stack.isOf(item)) {
            count += stack.getCount();
         }
      }

      if (mc.player.getOffHandStack().isOf(item)) {
         count += mc.player.getOffHandStack().getCount();
      }

      return count;
   }

   private int getFallDamage() {
      StatusEffectInstance jumpBoost = mc.player.getStatusEffect(StatusEffects.JUMP_BOOST);
      float boost = jumpBoost == null ? 0.0F : (float)(jumpBoost.getAmplifier() + 1);
      return Math.max(0, MathHelper.ceil(mc.player.fallDistance - 3.0F - boost));
   }

   @Override
   public String getInfo() {
      return this.item.getValue().toString();
   }

   public boolean isTotemInOffHand() {
      return this.isTotemInOffHand;
   }

   public boolean isGappleInOffHand() {
      return this.isGappleInOffHand;
   }

   public int getTotemCount() {
      return InventoryUtil.getItemCount(Items.TOTEM_OF_UNDYING);
   }

   public static enum ItemMode {
      Totem(Items.TOTEM_OF_UNDYING),
      Crystal(Items.END_CRYSTAL),
      Gapple(Items.ENCHANTED_GOLDEN_APPLE),
      Custom(null);

      private final Item item;

      private ItemMode(Item item) {
         this.item = item;
      }

      public Item getItem(boolean crapple) {
         return this == Gapple && crapple ? Items.GOLDEN_APPLE : this.item;
      }
   }
}
