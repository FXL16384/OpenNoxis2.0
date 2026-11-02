package noxisclient.mod.modules.impl.combat;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.ShulkerBoxBlock;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ShulkerBoxScreenHandler;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import noxisclient.Alien;
import noxisclient.api.events.impl.UpdateEvent;
import noxisclient.api.utils.math.Timer;
import noxisclient.api.utils.world.BlockUtil;
import noxisclient.mod.modules.Module;
import noxisclient.mod.modules.settings.impl.BooleanSetting;
import noxisclient.mod.modules.settings.impl.SliderSetting;

public class AntiRegear extends Module {
   public static AntiRegear INSTANCE;
   public static boolean hideShulkerUI = false;
   private final SliderSetting range = this.add(new SliderSetting("Range", 4.0, 1.0, 8.0, 0.1));
   public final BooleanSetting rotate = this.add(new BooleanSetting("Rotate", true));
   public final BooleanSetting drop = this.add(new BooleanSetting("Drop", true));
   public final BooleanSetting IgnoreSelf = this.add(new BooleanSetting("IgnoreSelf", true));
   public final BooleanSetting NoUi = this.add(new BooleanSetting("NoUi", false));
   public final Set<BlockPos> safe = ConcurrentHashMap.newKeySet();
   private final Map<BlockPos, Long> safeAddTime = new ConcurrentHashMap<>();
   private static final long SAFE_GRACE_PERIOD_MS = 2000L;
   private final Set<BlockPos> openedBoxes = ConcurrentHashMap.newKeySet();
   private final Set<BlockPos> autoOpenAttemptedBoxes = ConcurrentHashMap.newKeySet();
   private static final long OPEN_RESPONSE_TIMEOUT_MS = 250L;
   private static final long FAILED_OPEN_RETRY_DELAY_MS = 700L;
   private static final int MAX_FAILED_OPEN_ATTEMPTS = 3;
   private final Timer openTimer = new Timer();
   private final Timer failedOpenRetryTimer = new Timer();
   private final Timer closeCooldown = new Timer();
   private BlockPos targetBox = null;
   private BlockPos failedOpenPos = null;
   private int failedOpenAttempts = 0;
   private boolean processedThisBox = false;
   private boolean droppedThisBox = false;
   private BlockPos manualBoxPos = null;
   private int previousShulkerCount = -1;
   private boolean inPlacementMode = false;
   private long placementModeEndTime = 0L;
   private static final long PLACEMENT_MODE_DURATION_MS = 3000L;
   private final Set<BlockPos> prePlacementSnapshot = ConcurrentHashMap.newKeySet();
   private final Set<BlockPos> inventoryDetectedPositions = ConcurrentHashMap.newKeySet();
   private BlockPos screenOpenCrosshairPos = null;
   private static final long RECENT_SAFE_THRESHOLD_MS = 5000L;

   public AntiRegear() {
      super("AntiRegear", Module.Category.Combat);
      this.setChinese("反补");
      INSTANCE = this;
   }

   public void addSafe(BlockPos pos) {
      this.safe.add(pos);
      this.safeAddTime.put(pos, System.currentTimeMillis());
   }

   public boolean isHandlingOpenedContainer() {
      if (this.isOn() && this.drop.getValue() && mc.player != null) {
         ScreenHandler handler = mc.player.currentScreenHandler;
         return !this.isTargetShulkerHandler(handler) ? false : this.targetBox != null || hideShulkerUI || handler instanceof ShulkerBoxScreenHandler;
      } else {
         return false;
      }
   }

   @Override
   public void onEnable() {
      this.targetBox = null;
      this.processedThisBox = false;
      this.droppedThisBox = false;
      hideShulkerUI = false;
      this.openedBoxes.clear();
      this.autoOpenAttemptedBoxes.clear();
      this.failedOpenPos = null;
      this.failedOpenAttempts = 0;
      this.safeAddTime.clear();
      this.previousShulkerCount = -1;
      this.inPlacementMode = false;
      this.placementModeEndTime = 0L;
      this.prePlacementSnapshot.clear();
      this.inventoryDetectedPositions.clear();
      this.screenOpenCrosshairPos = null;
   }

   @Override
   public void onDisable() {
      hideShulkerUI = false;
      this.targetBox = null;
      this.processedThisBox = false;
      this.droppedThisBox = false;
      this.openedBoxes.clear();
      this.autoOpenAttemptedBoxes.clear();
      this.failedOpenPos = null;
      this.failedOpenAttempts = 0;
      this.safeAddTime.clear();
      this.previousShulkerCount = -1;
      this.inPlacementMode = false;
      this.placementModeEndTime = 0L;
      this.prePlacementSnapshot.clear();
      this.inventoryDetectedPositions.clear();
      this.screenOpenCrosshairPos = null;
   }

   public void onUpdate(UpdateEvent event) {
      if (mc.player != null && mc.world != null) {
         this.checkInventoryAndHandlePlacement();
         this.cleanupSafePositions();
         if (this.isTargetShulkerHandler(mc.player.currentScreenHandler)) {
            this.captureScreenOpenCrosshair();
            if (this.manualBoxPos == null) {
               if (this.targetBox != null) {
                  this.manualBoxPos = this.targetBox;
               } else if (this.screenOpenCrosshairPos != null) {
                  this.manualBoxPos = this.screenOpenCrosshairPos;
               } else {
                  double nearestDist = Double.MAX_VALUE;

                  for (BlockPos pos : BlockUtil.getSphere((float)this.range.getValue())) {
                     BlockState state = mc.world.getBlockState(pos);
                     if (state.getBlock() instanceof ShulkerBoxBlock) {
                        double dist = mc.player.squaredDistanceTo((double)pos.getX() + 0.5, (double)pos.getY() + 0.5, (double)pos.getZ() + 0.5);
                        if (dist < nearestDist) {
                           nearestDist = dist;
                           this.manualBoxPos = pos;
                        }
                     }
                  }

                  if (nearestDist > this.range.getValue() * this.range.getValue()) {
                     this.manualBoxPos = null;
                  }
               }
            }

            if (this.IgnoreSelf.getValue() && this.targetBox == null) {
               if (this.manualBoxPos == null || !this.safe.contains(this.manualBoxPos)) {
                  this.tryProtectNewShulkers();
               }

               if (this.manualBoxPos != null && this.safe.contains(this.manualBoxPos)) {
                  hideShulkerUI = false;
                  return;
               }

               if (this.manualBoxPos == null) {
                  hideShulkerUI = false;
                  return;
               }
            }

            this.handleShulkerScreen();
         } else {
            hideShulkerUI = false;
            this.droppedThisBox = false;
            this.manualBoxPos = null;
            this.screenOpenCrosshairPos = null;
            if (this.processedThisBox) {
               this.processedThisBox = false;
            }

            if (this.targetBox != null && !this.openTimer.passedMs(250.0)) {
               hideShulkerUI = this.NoUi.getValue() && !this.safe.contains(this.targetBox);
            } else {
               if (this.failedOpenPos != null && !(mc.world.getBlockState(this.failedOpenPos).getBlock() instanceof ShulkerBoxBlock)) {
                  this.failedOpenPos = null;
                  this.failedOpenAttempts = 0;
               }

               if (this.targetBox != null) {
                  if (this.targetBox.equals(this.failedOpenPos)) {
                     this.failedOpenAttempts++;
                  } else {
                     this.failedOpenPos = this.targetBox;
                     this.failedOpenAttempts = 1;
                  }

                  this.failedOpenRetryTimer.reset();
                  if (this.failedOpenAttempts >= 3) {
                     this.openedBoxes.add(this.targetBox);
                     this.failedOpenPos = null;
                     this.failedOpenAttempts = 0;
                  }
               }

               this.targetBox = null;
               this.scanAndOpenShulker();
            }
         }
      }
   }

   private void cleanupSafePositions() {
      long now = System.currentTimeMillis();
      if (!this.IgnoreSelf.getValue()) {
         this.safe.clear();
         this.safeAddTime.clear();
      } else {
         Iterator<BlockPos> it = this.safe.iterator();

         while (it.hasNext()) {
            BlockPos pos = it.next();
            BlockState state = mc.world.getBlockState(pos);
            boolean isStillValid = state.getBlock() instanceof ShulkerBoxBlock || state.getBlock() == Blocks.ENDER_CHEST;
            if (!isStillValid) {
               Long addTime = this.safeAddTime.get(pos);
               boolean inGracePeriod = addTime != null && now - addTime < 2000L;
               if (!inGracePeriod) {
                  it.remove();
                  this.safeAddTime.remove(pos);
               }
            }
         }
      }

      Iterator<BlockPos> it = this.openedBoxes.iterator();

      while (it.hasNext()) {
         BlockPos pos = it.next();
         BlockState state = mc.world.getBlockState(pos);
         if (!(state.getBlock() instanceof ShulkerBoxBlock)) {
            it.remove();
         }
      }

      Iterator<BlockPos> attemptedIt = this.autoOpenAttemptedBoxes.iterator();

      while (attemptedIt.hasNext()) {
         BlockPos pos = attemptedIt.next();
         BlockState state = mc.world.getBlockState(pos);
         if (!(state.getBlock() instanceof ShulkerBoxBlock)) {
            attemptedIt.remove();
         }
      }

      Iterator<BlockPos> it3 = this.inventoryDetectedPositions.iterator();

      while (it3.hasNext()) {
         BlockPos pos = it3.next();
         BlockState state = mc.world.getBlockState(pos);
         if (!(state.getBlock() instanceof ShulkerBoxBlock)) {
            it3.remove();
            this.safe.remove(pos);
            this.safeAddTime.remove(pos);
         }
      }
   }

   private void handleShulkerScreen() {
      if (this.drop.getValue()) {
         ScreenHandler handler = mc.player.currentScreenHandler;
         if (this.isTargetShulkerHandler(handler)) {
            if (this.IgnoreSelf.getValue() && this.targetBox == null) {
               if (this.manualBoxPos == null) {
                  double nearestDist = Double.MAX_VALUE;

                  for (BlockPos pos : BlockUtil.getSphere((float)this.range.getValue())) {
                     BlockState state = mc.world.getBlockState(pos);
                     if (state.getBlock() instanceof ShulkerBoxBlock) {
                        double dist = mc.player.squaredDistanceTo((double)pos.getX() + 0.5, (double)pos.getY() + 0.5, (double)pos.getZ() + 0.5);
                        if (dist < nearestDist) {
                           nearestDist = dist;
                           this.manualBoxPos = pos;
                        }
                     }
                  }

                  if (nearestDist > this.range.getValue() * this.range.getValue()) {
                     this.manualBoxPos = null;
                  }
               }

               if (this.manualBoxPos == null) {
                  hideShulkerUI = false;
                  return;
               }

               if (this.safe.contains(this.manualBoxPos)) {
                  hideShulkerUI = false;
                  return;
               }
            }

            if (this.NoUi.getValue() && !hideShulkerUI) {
               hideShulkerUI = true;
            } else if (!this.NoUi.getValue()) {
               hideShulkerUI = false;
            }

            if (!this.droppedThisBox) {
               for (int i = 0; i < 27; i++) {
                  if (!handler.getSlot(i).getStack().isEmpty()) {
                     mc.interactionManager.clickSlot(handler.syncId, i, 1, SlotActionType.THROW, mc.player);
                  }
               }

               this.droppedThisBox = true;
               this.processedThisBox = true;
            } else if (this.openTimer.passedMs(100.0)) {
               mc.player.closeHandledScreen();
            }
         }
      }
   }

   private boolean isTargetShulkerHandler(ScreenHandler handler) {
      if (handler instanceof ShulkerBoxScreenHandler) {
         return true;
      } else {
         if (this.targetBox != null && handler instanceof GenericContainerScreenHandler generic && generic.getRows() == 3) {
            return true;
         }

         return false;
      }
   }

   private void scanAndOpenShulker() {
      if (this.targetBox == null) {
         double scanRange = this.range.getValue() + 3.0;

         for (BlockPos pos : BlockUtil.getSphere((float)scanRange)) {
            BlockState state = mc.world.getBlockState(pos);
            if (state.getBlock() instanceof ShulkerBoxBlock
               && this.canOpenShulker(pos, state)
               && !this.openedBoxes.contains(pos)
               && (!pos.equals(this.failedOpenPos) || this.failedOpenRetryTimer.passedMs(700.0))
               && (!this.IgnoreSelf.getValue() || !this.safe.contains(pos))) {
               double distSq = mc.player.squaredDistanceTo((double)pos.getX() + 0.5, (double)pos.getY() + 0.5, (double)pos.getZ() + 0.5);
               if (!(Math.sqrt(distSq) > this.range.getValue())) {
                  this.targetBox = pos;
                  this.openedBoxes.add(pos);
                  this.openShulker(pos);
                  return;
               }
            }
         }
      }
   }

   private boolean canOpenShulker(BlockPos pos, BlockState state) {
      Direction facing = state.get(ShulkerBoxBlock.FACING);
      BlockPos openPos = pos.offset(facing);
      return mc.world.getBlockState(openPos).getCollisionShape(mc.world, openPos).isEmpty();
   }

   private void checkInventoryAndHandlePlacement() {
      if (mc.player != null) {
         int currentCount = this.countShulkersInInventory();
         if (this.previousShulkerCount == -1) {
            this.previousShulkerCount = currentCount;
         } else {
            if (currentCount == this.previousShulkerCount - 1) {
               this.capturePlacedPositionFromCrosshair();
               this.startPlacementMode();
            }

            this.previousShulkerCount = currentCount;
            if (this.inPlacementMode) {
               this.handlePlacementModeScan();
            }
         }
      }
   }

   private void startPlacementMode() {
      this.inPlacementMode = true;
      this.placementModeEndTime = System.currentTimeMillis() + 3000L;
      this.prePlacementSnapshot.clear();
      double scanRange = this.range.getValue() + 3.0;

      for (BlockPos pos : BlockUtil.getSphere((float)scanRange)) {
         if (mc.world.getBlockState(pos).getBlock() instanceof ShulkerBoxBlock) {
            this.prePlacementSnapshot.add(pos);
         }
      }
   }

   private void handlePlacementModeScan() {
      if (System.currentTimeMillis() >= this.placementModeEndTime) {
         this.inPlacementMode = false;
         this.prePlacementSnapshot.clear();
      } else {
         double scanRange = this.range.getValue() + 3.0;

         for (BlockPos pos : BlockUtil.getSphere((float)scanRange)) {
            if (mc.world.getBlockState(pos).getBlock() instanceof ShulkerBoxBlock && !this.prePlacementSnapshot.contains(pos) && !this.safe.contains(pos)) {
               this.addSafe(pos);
               this.inventoryDetectedPositions.add(pos);
            }
         }
      }
   }

   private int countShulkersInInventory() {
      int count = 0;

      for (int i = 0; i < 35; i++) {
         ItemStack stack = mc.player.getInventory().getStack(i);
         if (!stack.isEmpty()) {
            Item var5 = stack.getItem();
            if (var5 instanceof BlockItem) {
               BlockItem blockItem = (BlockItem)var5;
               if (blockItem.getBlock() instanceof ShulkerBoxBlock) {
                  count += stack.getCount();
               }
            }
         }
      }

      return count;
   }

   private void capturePlacedPositionFromCrosshair() {
      if (mc.crosshairTarget instanceof BlockHitResult hit) {
         BlockPos var4 = hit.getBlockPos().offset(hit.getSide());
         this.addSafe(var4);
         this.inventoryDetectedPositions.add(var4);
         BlockPos directPos = hit.getBlockPos();
         if (!directPos.equals(var4) && mc.world.getBlockState(directPos).getBlock() instanceof ShulkerBoxBlock) {
            this.addSafe(directPos);
            this.inventoryDetectedPositions.add(directPos);
         }
      }
   }

   private void captureScreenOpenCrosshair() {
      if (this.screenOpenCrosshairPos == null) {
         if (mc.crosshairTarget instanceof BlockHitResult hit) {
            BlockPos var4 = hit.getBlockPos();
            if (mc.world.getBlockState(var4).getBlock() instanceof ShulkerBoxBlock) {
               this.screenOpenCrosshairPos = var4;
            } else {
               BlockPos offsetPos = var4.offset(hit.getSide());
               if (mc.world.getBlockState(offsetPos).getBlock() instanceof ShulkerBoxBlock) {
                  this.screenOpenCrosshairPos = offsetPos;
               }
            }
         }
      }
   }

   private void tryProtectNewShulkers() {
      long now = System.currentTimeMillis();

      for (Entry<BlockPos, Long> entry : this.safeAddTime.entrySet()) {
         BlockPos pos = entry.getKey();
         long addTime = entry.getValue();
         if (now - addTime < 5000L) {
            BlockState state = mc.world.getBlockState(pos);
            if (state.getBlock() instanceof ShulkerBoxBlock) {
               double dist = mc.player.squaredDistanceTo((double)pos.getX() + 0.5, (double)pos.getY() + 0.5, (double)pos.getZ() + 0.5);
               if (Math.sqrt(dist) <= this.range.getValue()) {
                  if (this.manualBoxPos == null || !this.manualBoxPos.equals(pos)) {
                     this.manualBoxPos = pos;
                  }

                  return;
               }
            }
         }
      }
   }

   private void openShulker(BlockPos pos) {
      Direction side = BlockUtil.getClickSide(pos);
      Vec3d hitVec = pos.toCenterPos().add(Vec3d.of(side.getVector()).multiply(0.5));
      if (this.rotate.getValue()) {
         Alien.ROTATION.lookAt(hitVec);
      }

      this.openTimer.reset();
      if (this.NoUi.getValue() && !this.safe.contains(pos)) {
         hideShulkerUI = true;
      } else {
         hideShulkerUI = false;
      }

      BlockHitResult hitResult = new BlockHitResult(hitVec, side, pos, false);
      PlayerInteractBlockC2SPacket packet = new PlayerInteractBlockC2SPacket(Hand.MAIN_HAND, hitResult, 0);
      mc.getNetworkHandler().getConnection().send(packet);
   }
}
