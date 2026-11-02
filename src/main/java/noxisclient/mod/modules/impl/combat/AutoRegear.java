package noxisclient.mod.modules.impl.combat;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.ShulkerBoxBlock;
import net.minecraft.client.gui.screen.ingame.ShulkerBoxScreen;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ShulkerBoxScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.hit.HitResult.Type;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import noxisclient.Alien;
import noxisclient.api.events.impl.UpdateEvent;
import noxisclient.api.utils.math.Timer;
import noxisclient.api.utils.player.InventoryUtil;
import noxisclient.api.utils.world.BlockUtil;
import noxisclient.core.Manager;
import noxisclient.core.impl.AnimationManager;
import noxisclient.mod.modules.Module;
import noxisclient.mod.modules.settings.impl.BindSetting;
import noxisclient.mod.modules.settings.impl.BooleanSetting;
import noxisclient.mod.modules.settings.impl.SliderSetting;

public class AutoRegear extends Module {
   public static AutoRegear INSTANCE;
   private static final String CURRENT_KIT_FILE = "autoregear-current-kit.txt";
   private final BooleanSetting rotate = this.add(new BooleanSetting("Rotate", true));
   private final BooleanSetting autoDisable = this.add(new BooleanSetting("AutoDisable", true));
   private final SliderSetting disableTime = this.add(new SliderSetting("DisableTime", 500, 0, 1000));
   private final SliderSetting delay = this.add(new SliderSetting("Delay", 0, 0, 1000));
   public final BooleanSetting animation = this.add(new BooleanSetting("Animation", true));
   private final SliderSetting range = this.add(new SliderSetting("Range", 4.0, 0.0, 6.0, 0.1));
   private final BooleanSetting inventory = this.add(new BooleanSetting("InventorySwap", true));
   private final BooleanSetting mine = this.add(new BooleanSetting("Mine", true));
   private final BooleanSetting autoClose = this.add(new BooleanSetting("AutoClose", true));
   private final BooleanSetting enderChest = this.add(new BooleanSetting("EnderChest", false));
   private final BooleanSetting fallbackPlace = this.add(new BooleanSetting("FallbackPlace", true));
   private final BindSetting placeKey = this.add(new BindSetting("PlaceKey", -1));
   private final Timer timer = new Timer();
   private final Timer timeoutTimer = new Timer();
   private final Map<Integer, ItemStack> desiredInventory = new HashMap<>();
   private final List<BlockPos> openList = new ArrayList<>();
   public BlockPos placePos = null;
   private BlockPos openPos = null;
   private BlockPos pendingMinePos = null;
   private boolean opened = false;
   private boolean wasPlaceKeyPressed = false;
   private boolean waitForNextTick = false;

   public AutoRegear() {
      super("AutoRegear", Module.Category.Combat);
      this.setChinese("自动补给");
      INSTANCE = this;
      this.loadSavedKit();
   }

   @Override
   public void onEnable() {
      this.resetState();
      if (this.desiredInventory.isEmpty()) {
         this.sendMessage("§cNo kit loaded. Use kit load <name>.");
         this.disable();
      }
   }

   @Override
   public void onDisable() {
      this.opened = false;
      this.pendingMinePos = null;
      if (this.mine.getValue() && this.placePos != null && mc.world != null && mc.world.getBlockState(this.placePos).getBlock() instanceof ShulkerBoxBlock) {
         this.clickMine(this.placePos);
      }

      this.placePos = null;
      this.openPos = null;
   }

   public boolean loadKit(String name) {
      Map<Integer, ItemStack> loaded = this.readKit(name);
      if (loaded.isEmpty()) {
         this.sendMessage("§cKit [" + name + "] is empty or not found.");
         return false;
      } else {
         this.desiredInventory.clear();
         this.desiredInventory.putAll(loaded);
         this.resetState();
         this.saveCurrentKit(name);
         this.sendMessage("§aLoaded kit [" + name + "].");
         return true;
      }
   }

   public boolean hasLoadedKit() {
      return !this.desiredInventory.isEmpty();
   }

   public Map<Integer, ItemStack> getDesiredInventory() {
      return new HashMap<>(this.desiredInventory);
   }

   public boolean isKitItemEqual(ItemStack stack1, ItemStack stack2) {
      return this.isItemEqual(stack1, stack2);
   }

   private void resetState() {
      this.opened = false;
      this.openPos = null;
      this.placePos = null;
      this.waitForNextTick = false;
      this.timeoutTimer.reset();
      this.timer.reset();
   }

   private void loadSavedKit() {
      try {
         File file = Manager.getFile("autoregear-current-kit.txt");
         if (!file.exists()) {
            return;
         }

         BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8));
         String name = reader.readLine();
         reader.close();
         if (name == null || name.isBlank()) {
            return;
         }

         Map<Integer, ItemStack> loaded = this.readKit(name.trim());
         if (!loaded.isEmpty()) {
            this.desiredInventory.clear();
            this.desiredInventory.putAll(loaded);
         }
      } catch (Exception var5) {
         var5.printStackTrace();
      }
   }

   private void saveCurrentKit(String name) {
      try {
         OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(Manager.getFile("autoregear-current-kit.txt")), StandardCharsets.UTF_8);
         writer.write(name);
         writer.close();
      } catch (Exception var3) {
         var3.printStackTrace();
      }
   }

   private Map<Integer, ItemStack> readKit(String name) {
      Map<Integer, ItemStack> result = new HashMap<>();

      try {
         File file = Manager.getFile(name + ".kit");
         if (!file.exists()) {
            return result;
         }

         BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8));

         String line;
         while ((line = reader.readLine()) != null) {
            String[] parts = line.split(":", 2);
            if (parts.length >= 2) {
               try {
                  int slot = Integer.parseInt(parts[0]);
                  ItemStack stack = this.parseKitStack(parts[1]);
                  if (!stack.isEmpty()) {
                     result.put(slot, stack);
                  }
               } catch (Exception var9) {
               }
            }
         }

         reader.close();
      } catch (Exception var10) {
         var10.printStackTrace();
      }

      return result;
   }

   private ItemStack parseKitStack(String rawKey) {
      boolean isTurtle = rawKey.endsWith(":turtle");
      boolean isStrength = rawKey.endsWith(":strength");
      String key = rawKey;
      if (isTurtle) {
         key = rawKey.substring(0, rawKey.length() - ":turtle".length());
      } else if (isStrength) {
         key = rawKey.substring(0, rawKey.length() - ":strength".length());
      }

      for (Item item : Registries.ITEM) {
         if (item.getTranslationKey().equals(key)) {
            ItemStack stack = new ItemStack(item);
            if (isTurtle) {
               stack.set(DataComponentTypes.CUSTOM_NAME, Text.literal("turtle"));
            } else if (isStrength) {
               stack.set(DataComponentTypes.CUSTOM_NAME, Text.literal("strength"));
            }

            return stack;
         }
      }

      return ItemStack.EMPTY;
   }

   public void onUpdate(UpdateEvent event) {
      if (mc.player != null && mc.world != null) {
         this.openList.removeIf(pos -> {
            Block block = mc.world.getBlockState(pos).getBlock();
            return !(block instanceof ShulkerBoxBlock) && block != Blocks.ENDER_CHEST;
         });
         boolean isContainerGui = mc.currentScreen instanceof ShulkerBoxScreen || this.isEnderChestGui();
         if (isContainerGui) {
            this.opened = true;
            if (this.openPos != null && !this.openList.contains(this.openPos)) {
               this.openList.add(this.openPos);
            }

            this.processCurrentContainer();
         } else {
            if (this.opened) {
               this.opened = false;
               BlockPos finishedPos = this.openPos;
               if (finishedPos != null && !this.openList.contains(finishedPos)) {
                  this.openList.add(finishedPos);
               }

               this.pendingMinePos = finishedPos != null && mc.world.getBlockState(finishedPos).getBlock() instanceof ShulkerBoxBlock ? finishedPos : null;
               this.openPos = null;
               this.placePos = null;
            }

            if (this.pendingMinePos != null) {
               if (this.mine.getValue() && mc.world.getBlockState(this.pendingMinePos).getBlock() instanceof ShulkerBoxBlock) {
                  this.clickMine(this.pendingMinePos);
               }

               this.pendingMinePos = null;
               if (this.autoDisable.getValue()) {
                  this.disable();
                  return;
               }
            }

            boolean pressed = this.placeKey.isPressed();
            if (pressed && !this.wasPlaceKeyPressed && mc.currentScreen == null) {
               this.doPlaceOrOpen();
            }

            this.wasPlaceKeyPressed = pressed;
            this.tryOpenPending();
            if (this.autoDisable.getValue() && this.openPos == null && this.placePos == null && this.timeoutTimer.passed((long)this.disableTime.getValueInt())) {
               this.disable();
            }
         }
      }
   }

   private void doPlaceOrOpen() {
      if (this.desiredInventory.isEmpty()) {
         this.sendMessage("§cNo kit loaded. Use kit load <name>.");
      } else {
         BlockPos existing = this.findExistingEnderChest();
         if (existing != null) {
            this.openPos = existing;
            this.openContainer(existing);
         } else {
            AutoRegear.Placement placement = this.findPlacement();
            if (placement == null) {
               this.sendMessage("§4No place position found.");
            } else {
               int slot = this.findContainerItem();
               if (slot == -1) {
                  this.sendMessage(this.enderChest.getValue() ? "§4No shulkerbox or enderchest found." : "§4No shulkerbox found.");
               } else {
                  int oldSlot = mc.player.getInventory().selectedSlot;
                  if (this.inventory.getValue()) {
                     Alien.INVENTORY.inventorySwap(slot, oldSlot);
                     this.placeBlock(placement);
                     Alien.INVENTORY.inventorySwap(slot, oldSlot);
                  } else {
                     Alien.INVENTORY.setSlot(slot);
                     this.placeBlock(placement);
                     Alien.INVENTORY.syncToClient();
                  }

                  this.placePos = placement.pos;
                  this.openPos = placement.pos;
                  this.waitForNextTick = true;
                  this.timer.reset();
               }
            }
         }
      }
   }

   private BlockPos findExistingEnderChest() {
      return !this.enderChest.getValue()
         ? null
         : BlockUtil.getSphere((float)this.range.getValue())
            .stream()
            .filter(pos -> !this.openList.contains(pos))
            .filter(pos -> mc.player.squaredDistanceTo(pos.toCenterPos()) <= this.range.getValue() * this.range.getValue())
            .filter(pos -> mc.world.getBlockState(pos).getBlock() == Blocks.ENDER_CHEST)
            .filter(this::canOpen)
            .min(Comparator.comparingDouble(pos -> mc.player.squaredDistanceTo(pos.toCenterPos())))
            .orElse(null);
   }

   private AutoRegear.Placement findPlacement() {
      AutoRegear.Placement crosshair = this.findCrosshairPlacement();
      if (crosshair != null) {
         return crosshair;
      } else {
         return !this.fallbackPlace.getValue()
            ? null
            : BlockUtil.getSphere((float)this.range.getValue())
               .stream()
               .map(this::validPlacement)
               .filter(placement -> placement != null)
               .min(Comparator.comparingDouble(placement -> mc.player.squaredDistanceTo(placement.pos.toCenterPos())))
               .orElse(null);
      }
   }

   private AutoRegear.Placement findCrosshairPlacement() {
      HitResult target = mc.crosshairTarget;
      if (target instanceof BlockHitResult hit && target.getType() == Type.BLOCK) {
         BlockPos hitPos = hit.getBlockPos();
         Direction side = hit.getSide();
         List<BlockPos> candidates = new ArrayList<>();
         BlockPos primary = hitPos.offset(side);
         candidates.add(primary);

         for (Direction diagonal : net.minecraft.util.math.Direction.Type.HORIZONTAL) {
            candidates.add(primary.offset(diagonal));
         }

         if (side.getAxis().isHorizontal()) {
            candidates.add(primary.up());
            candidates.add(hitPos.up().offset(side));

            for (Direction diagonal : net.minecraft.util.math.Direction.Type.HORIZONTAL) {
               if (diagonal != side && diagonal != side.getOpposite()) {
                  candidates.add(hitPos.offset(diagonal).offset(side));
               }
            }
         }

         for (BlockPos pos : candidates) {
            AutoRegear.Placement placement = this.validPlacement(pos);
            if (placement != null) {
               return placement;
            }
         }

         return null;
      }

      return null;
   }

   private AutoRegear.Placement validPlacement(BlockPos pos) {
      if (mc.player.squaredDistanceTo(pos.toCenterPos()) > this.range.getValue() * this.range.getValue()) {
         return null;
      } else if (!BlockUtil.clientCanPlace(pos, false)) {
         return null;
      } else {
         for (Direction support : Direction.values()) {
            if (support != Direction.UP) {
               BlockPos supportPos = pos.offset(support);
               if (BlockUtil.canClick(supportPos) && !BlockUtil.canReplace(supportPos) && BlockUtil.isStrictDirection(supportPos, support.getOpposite())) {
                  Direction openDirection = support == Direction.DOWN ? Direction.UP : support.getOpposite();
                  if (this.isOpenDirectionClear(pos, openDirection)) {
                     return new AutoRegear.Placement(pos, supportPos, support.getOpposite(), openDirection);
                  }
               }
            }
         }

         return null;
      }
   }

   private boolean canOpen(BlockPos pos) {
      Block block = mc.world.getBlockState(pos).getBlock();
      if (block == Blocks.ENDER_CHEST) {
         return mc.world.isAir(pos.up());
      } else if (block instanceof ShulkerBoxBlock) {
         Direction facing = mc.world.getBlockState(pos).get(ShulkerBoxBlock.FACING);
         return this.isOpenDirectionClear(pos, facing);
      } else {
         return false;
      }
   }

   private boolean isOpenDirectionClear(BlockPos pos, Direction direction) {
      return mc.world.isAir(pos.offset(direction));
   }

   private void placeBlock(AutoRegear.Placement placement) {
      if (AntiRegear.INSTANCE != null) {
         AntiRegear.INSTANCE.addSafe(placement.pos);
      }

      BlockUtil.clickBlock(placement.supportPos, placement.clickSide, this.rotate.getValue());
   }

   private void tryOpenPending() {
      if (this.openPos != null && mc.currentScreen == null) {
         if (this.waitForNextTick) {
            this.waitForNextTick = false;
         } else if (mc.player.squaredDistanceTo(this.openPos.toCenterPos()) > this.range.getValue() * this.range.getValue()) {
            this.openPos = null;
            this.placePos = null;
         } else {
            Block block = mc.world.getBlockState(this.openPos).getBlock();
            if ((this.openPos.equals(this.placePos) && block instanceof ShulkerBoxBlock || block == Blocks.ENDER_CHEST) && this.canOpen(this.openPos)) {
               this.openContainer(this.openPos);
            }
         }
      }
   }

   private void openContainer(BlockPos pos) {
      if (!(mc.player.squaredDistanceTo(pos.toCenterPos()) > this.range.getValue() * this.range.getValue())) {
         if (mc.world.getBlockState(pos).getBlock() == Blocks.ENDER_CHEST) {
            Alien.PLAYER.markEnderChestOpening();
         }

         BlockUtil.clickBlock(pos, BlockUtil.getClickSide(pos), this.rotate.getValue());
      }
   }

   private void clickMine(BlockPos pos) {
      Direction side = BlockUtil.getClickSide(pos);
      if (side == null) {
         side = Direction.UP;
      }

      if (this.rotate.getValue()) {
         Alien.ROTATION.lookAt(pos.toCenterPos());
      }

      mc.interactionManager.attackBlock(pos, side);
      mc.player.swingHand(Hand.MAIN_HAND);
      if (this.rotate.getValue()) {
         Alien.ROTATION.snapBack();
      }
   }

   private int findContainerItem() {
      if (this.enderChest.getValue()) {
         int enderChestSlot = this.findEnderChest();
         if (enderChestSlot != -1) {
            return enderChestSlot;
         }
      }

      return this.findShulker();
   }

   private int findEnderChest() {
      if (this.inventory.getValue()) {
         for (int i = 0; i < 36; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (!stack.isEmpty() && stack.getItem() == Items.ENDER_CHEST) {
               return i < 9 ? i + 36 : i;
            }
         }

         return -1;
      } else {
         return InventoryUtil.findItem(Items.ENDER_CHEST);
      }
   }

   private int findShulker() {
      if (this.inventory.getValue()) {
         for (int i = 0; i < 36; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (!stack.isEmpty() && stack.getItem() instanceof BlockItem blockItem && blockItem.getBlock() instanceof ShulkerBoxBlock) {
               return i < 9 ? i + 36 : i;
            }
         }

         return -1;
      } else {
         return InventoryUtil.findClass(ShulkerBoxBlock.class);
      }
   }

   private boolean isEnderChestGui() {
      return this.openPos != null
         && mc.world.getBlockState(this.openPos).getBlock() == Blocks.ENDER_CHEST
         && mc.player.currentScreenHandler instanceof GenericContainerScreenHandler;
   }

   private void processCurrentContainer() {
      if (!(this.delay.getValue() > 0.0) || this.timer.passed((long)this.delay.getValueInt())) {
         ScreenHandler handler = mc.player.currentScreenHandler;
         if (handler instanceof ShulkerBoxScreenHandler || handler instanceof GenericContainerScreenHandler) {
            boolean moved = false;
            int moves = this.delay.getValue() <= 0.0 ? 36 : 1;

            for (int i = 0; i < moves && this.processOneItem(handler); i++) {
               moved = true;
            }

            if (moved) {
               this.timer.reset();
            } else {
               if (this.isKitted()) {
                  if (!this.autoClose.getValue() || !this.timer.passed(150L)) {
                     return;
                  }

                  if (!this.clearCursor(handler)) {
                     return;
                  }

                  if (this.shouldKeepOpenForAntiRegear()) {
                     return;
                  }

                  mc.player.closeHandledScreen();
               } else if (!this.hasNeededItem(handler)) {
                  if (!this.autoClose.getValue() || !this.timer.passed(150L)) {
                     return;
                  }

                  if (!this.clearCursor(handler)) {
                     return;
                  }

                  if (this.shouldKeepOpenForAntiRegear()) {
                     return;
                  }

                  mc.player.closeHandledScreen();
               } else if (this.autoDisable.getValue()) {
                  this.timeoutToDisable();
               }
            }
         }
      }
   }

   private boolean shouldKeepOpenForAntiRegear() {
      return this.openPos == null && AntiRegear.INSTANCE != null && AntiRegear.INSTANCE.isHandlingOpenedContainer();
   }

   private boolean processOneItem(ScreenHandler handler) {
      for (Entry<Integer, ItemStack> entry : this.desiredInventory.entrySet()) {
         int targetSlot = entry.getKey();
         ItemStack targetStack = entry.getValue();
         ItemStack playerStack = mc.player.getInventory().getStack(targetSlot);
         if (this.needsItem(playerStack, targetStack)) {
            Slot source = this.findContainerSlot(handler, targetStack, playerStack);
            if (source != null) {
               int windowSlotId = (targetSlot < 9 ? targetSlot + 36 : targetSlot) + 18;
               this.addAnimation(handler, source.id, windowSlotId);
               mc.interactionManager.clickSlot(handler.syncId, source.id, 0, SlotActionType.PICKUP, mc.player);
               mc.interactionManager.clickSlot(handler.syncId, windowSlotId, 0, SlotActionType.PICKUP, mc.player);
               this.clearCursor(handler, source.id);
               return true;
            }
         }
      }

      return false;
   }

   private boolean clearCursor(ScreenHandler handler) {
      return this.clearCursor(handler, -1);
   }

   private boolean clearCursor(ScreenHandler handler, int preferredSlot) {
      if (mc.player.currentScreenHandler.getCursorStack().isEmpty()) {
         return true;
      } else {
         if (preferredSlot >= 0) {
            mc.interactionManager.clickSlot(handler.syncId, preferredSlot, 0, SlotActionType.PICKUP, mc.player);
            if (mc.player.currentScreenHandler.getCursorStack().isEmpty()) {
               return true;
            }
         }

         for (Slot slot : handler.slots) {
            if (slot.id < 27 && slot.getStack().isEmpty()) {
               mc.interactionManager.clickSlot(handler.syncId, slot.id, 0, SlotActionType.PICKUP, mc.player);
               return mc.player.currentScreenHandler.getCursorStack().isEmpty();
            }
         }

         return false;
      }
   }

   private Slot findContainerSlot(ScreenHandler handler, ItemStack targetStack) {
      return this.findContainerSlot(handler, targetStack, ItemStack.EMPTY);
   }

   private Slot findContainerSlot(ScreenHandler handler, ItemStack targetStack, ItemStack playerStack) {
      for (Slot slot : handler.slots) {
         if (slot.id < 27 && this.isValidSourceStack(slot.getStack(), targetStack, playerStack)) {
            return slot;
         }
      }

      return null;
   }

   private boolean isValidSourceStack(ItemStack sourceStack, ItemStack targetStack, ItemStack playerStack) {
      if (sourceStack.isEmpty() || !this.isItemEqual(sourceStack, targetStack)) {
         return false;
      } else {
         return !playerStack.isEmpty() && this.isItemEqual(playerStack, targetStack) ? ItemStack.areItemsAndComponentsEqual(sourceStack, playerStack) : true;
      }
   }

   private boolean hasNeededItem(ScreenHandler handler) {
      for (Entry<Integer, ItemStack> entry : this.desiredInventory.entrySet()) {
         ItemStack playerStack = mc.player.getInventory().getStack(entry.getKey());
         if (this.needsItem(playerStack, entry.getValue()) && this.findContainerSlot(handler, entry.getValue(), playerStack) != null) {
            return true;
         }
      }

      return false;
   }

   private boolean needsItem(ItemStack playerStack, ItemStack targetStack) {
      boolean hasShulker = !playerStack.isEmpty() && playerStack.getItem() instanceof BlockItem blockItem && blockItem.getBlock() instanceof ShulkerBoxBlock;
      if (hasShulker) {
         return false;
      } else if (playerStack.isEmpty()) {
         return true;
      } else {
         return !this.isItemEqual(playerStack, targetStack) ? true : playerStack.getCount() < playerStack.getMaxCount();
      }
   }

   private void addAnimation(ScreenHandler handler, int sourceSlot, int targetSlot) {
      if (this.animation.getValue()) {
         Slot from = handler.getSlot(sourceSlot);
         Slot to = handler.getSlot(targetSlot);
         if (from != null && !from.getStack().isEmpty()) {
            AnimationManager.MovingItem anim = new AnimationManager.MovingItem();
            anim.stack = from.getStack().copy();
            int[] screenOrigin = AnimationManager.getHandledScreenOrigin();
            anim.startX = (float)(screenOrigin[0] + from.x);
            anim.startY = (float)(screenOrigin[1] + from.y);
            anim.endX = to != null ? (float)(screenOrigin[0] + to.x) : anim.startX;
            anim.endY = to != null ? (float)(screenOrigin[1] + to.y) : anim.startY + 20.0F;
            anim.startTime = System.currentTimeMillis();
            anim.duration = 350L;
            AnimationManager.ANIMATIONS.add(anim);
         }
      }
   }

   private boolean isItemEqual(ItemStack stack1, ItemStack stack2) {
      if (stack1.isEmpty() && stack2.isEmpty()) {
         return true;
      } else if (!stack1.isEmpty() && !stack2.isEmpty()) {
         boolean var10000;
         label86: {
            if (stack1.getItem() instanceof BlockItem blockItem1 && blockItem1.getBlock() instanceof ShulkerBoxBlock) {
               var10000 = true;
               break label86;
            }

            var10000 = false;
         }

         boolean isShulker1;
         label81: {
            isShulker1 = var10000;
            if (stack2.getItem() instanceof BlockItem blockItem2 && blockItem2.getBlock() instanceof ShulkerBoxBlock) {
               var10000 = true;
               break label81;
            }

            var10000 = false;
         }

         boolean isShulker2 = var10000;
         if (isShulker1 && isShulker2) {
            return true;
         } else if (!ItemStack.areItemsEqual(stack1, stack2)) {
            return false;
         } else if (stack1.getItem() != Items.SPLASH_POTION) {
            return true;
         } else {
            boolean hasTurtle = false;
            boolean hasStrength = false;
            PotionContentsComponent potion = stack1.getOrDefault(DataComponentTypes.POTION_CONTENTS, PotionContentsComponent.DEFAULT);

            for (StatusEffectInstance effect : potion.getEffects()) {
               if (effect.getEffectType().value() == StatusEffects.RESISTANCE.value()) {
                  hasTurtle = true;
               }

               if (effect.getEffectType().value() == StatusEffects.STRENGTH.value()) {
                  hasStrength = true;
               }
            }

            boolean wantsTurtle = stack2.contains(DataComponentTypes.CUSTOM_NAME) && stack2.getName().getString().equals("turtle");
            boolean wantsStrength = stack2.contains(DataComponentTypes.CUSTOM_NAME) && stack2.getName().getString().equals("strength");
            return hasTurtle == wantsTurtle && hasStrength == wantsStrength;
         }
      } else {
         return false;
      }
   }

   private boolean isKitted() {
      for (Entry<Integer, ItemStack> entry : this.desiredInventory.entrySet()) {
         ItemStack stack = mc.player.getInventory().getStack(entry.getKey());
         if (!this.isItemEqual(stack, entry.getValue())) {
            return false;
         }

         if (!stack.isEmpty() && stack.getCount() < stack.getMaxCount()) {
            return false;
         }
      }

      return true;
   }

   private void timeoutToDisable() {
      if (this.timeoutTimer.passed((long)this.disableTime.getValueInt())) {
         this.disable();
      }
   }

   private static record Placement(BlockPos pos, BlockPos supportPos, Direction clickSide, Direction openDirection) {
   }
}
