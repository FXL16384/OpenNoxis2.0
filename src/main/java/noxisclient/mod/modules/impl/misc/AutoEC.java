package noxisclient.mod.modules.impl.misc;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.item.Items;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import noxisclient.Alien;
import noxisclient.api.events.impl.UpdateEvent;
import noxisclient.api.utils.math.Timer;
import noxisclient.api.utils.player.InventoryUtil;
import noxisclient.api.utils.world.BlockUtil;
import noxisclient.mod.modules.Module;
import noxisclient.mod.modules.impl.combat.AntiRegear;
import noxisclient.mod.modules.settings.impl.BindSetting;
import noxisclient.mod.modules.settings.impl.BooleanSetting;
import noxisclient.mod.modules.settings.impl.SliderSetting;

public class AutoEC extends Module {
   public static AutoEC INSTANCE;
   private final SliderSetting range = this.add(new SliderSetting("Range", 4.0, 0.0, 6.0, 0.1));
   private final BooleanSetting rotate = this.add(new BooleanSetting("Rotate", true));
   private final BindSetting placeKey = this.add(new BindSetting("PlaceKey", -1));
   private final Timer openTimer = new Timer();
   private BlockPos pendingOpenPos;
   private boolean wasPlaceKeyPressed;

   public AutoEC() {
      super("AutoEC", Module.Category.Misc);
      this.setChinese("AutoEC");
      INSTANCE = this;
   }

   @Override
   public void onEnable() {
      this.pendingOpenPos = null;
      this.wasPlaceKeyPressed = false;
      this.openTimer.reset();
   }

   public void onUpdate(UpdateEvent event) {
      if (!nullCheck()) {
         if (mc.currentScreen == null && this.pendingOpenPos != null) {
            if (mc.world.getBlockState(this.pendingOpenPos).getBlock() == Blocks.ENDER_CHEST) {
               this.openEnderChest(this.pendingOpenPos);
               this.pendingOpenPos = null;
            } else if (this.openTimer.passed(1000L)) {
               this.pendingOpenPos = null;
            }
         }

         boolean pressed = this.placeKey.isPressed();
         if (pressed && !this.wasPlaceKeyPressed && mc.currentScreen == null) {
            this.openOrPlace();
         }

         this.wasPlaceKeyPressed = pressed;
      }
   }

   private void openOrPlace() {
      BlockPos existing = this.findExistingEnderChest();
      if (existing != null) {
         this.openEnderChest(existing);
      } else {
         BlockPos placePos = this.findPlacePos();
         if (placePos == null) {
            this.sendMessage("§4No place position found.");
         } else {
            int enderChestSlot = InventoryUtil.findItemInventorySlot(Items.ENDER_CHEST);
            if (enderChestSlot == -1) {
               this.sendMessage("§4No ender chest found.");
            } else {
               int oldSlot = mc.player.getInventory().selectedSlot;
               Alien.INVENTORY.inventorySwap(enderChestSlot, oldSlot);
               this.placeBlock(placePos);
               Alien.INVENTORY.inventorySwap(enderChestSlot, oldSlot);
               this.pendingOpenPos = placePos;
               this.openTimer.reset();
            }
         }
      }
   }

   private BlockPos findExistingEnderChest() {
      double bestDistance = Double.MAX_VALUE;
      BlockPos bestPos = null;

      for (BlockPos pos : BlockUtil.getSphere((float)this.range.getValue())) {
         if (mc.world.getBlockState(pos).getBlock() == Blocks.ENDER_CHEST && mc.world.isAir(pos.up())) {
            double distance = mc.player.squaredDistanceTo(pos.toCenterPos());
            if (distance < bestDistance) {
               bestDistance = distance;
               bestPos = pos;
            }
         }
      }

      return bestPos;
   }

   private BlockPos findPlacePos() {
      double bestDistance = Double.MAX_VALUE;
      BlockPos bestPos = null;

      for (BlockPos pos : BlockUtil.getSphere((float)this.range.getValue())) {
         if (mc.world.isAir(pos.up()) && BlockUtil.clientCanPlace(pos, false)) {
            BlockPos supportPos = pos.offset(Direction.DOWN);
            BlockState supportState = mc.world.getBlockState(supportPos);
            if (BlockUtil.canClick(supportPos)
               && BlockUtil.isStrictDirection(supportPos, Direction.UP)
               && !supportState.isAir()
               && supportState.isSolidBlock(mc.world, supportPos)) {
               double distance = (double)MathHelper.sqrt((float)mc.player.squaredDistanceTo(pos.toCenterPos()));
               if (distance < bestDistance) {
                  bestDistance = distance;
                  bestPos = pos;
               }
            }
         }
      }

      return bestPos;
   }

   private void placeBlock(BlockPos pos) {
      if (AntiRegear.INSTANCE != null) {
         AntiRegear.INSTANCE.addSafe(pos);
      }

      BlockUtil.clickBlock(pos.offset(Direction.DOWN), Direction.UP, this.rotate.getValue());
   }

   private void openEnderChest(BlockPos pos) {
      Alien.PLAYER.markEnderChestOpening();
      BlockUtil.clickBlock(pos, BlockUtil.getClickSide(pos), this.rotate.getValue());
   }
}
