package noxisclient.mod.modules.impl.misc;

import net.minecraft.block.Blocks;
import net.minecraft.fluid.LavaFluid.Still;
import net.minecraft.util.math.BlockPos;
import noxisclient.Alien;
import noxisclient.api.events.impl.UpdateEvent;
import noxisclient.api.utils.math.Timer;
import noxisclient.api.utils.player.EntityUtil;
import noxisclient.api.utils.player.InventoryUtil;
import noxisclient.api.utils.world.BlockUtil;
import noxisclient.mod.modules.Module;
import noxisclient.mod.modules.settings.impl.BooleanSetting;
import noxisclient.mod.modules.settings.impl.SliderSetting;

public class LavaFiller extends Module {
   public final SliderSetting placeDelay = this.add(new SliderSetting("PlaceDelay", 50, 0, 500).setSuffix("ms"));
   private final BooleanSetting inventory = this.add(new BooleanSetting("InventorySwap", true));
   private final SliderSetting range = this.add(new SliderSetting("Range", 5.0, 0.0, 8.0, 0.1));
   private final SliderSetting blocksPer = this.add(new SliderSetting("BlocksPer", 1, 1, 8));
   private final BooleanSetting detectMining = this.add(new BooleanSetting("DetectMining", false));
   private final BooleanSetting usingPause = this.add(new BooleanSetting("UsingPause", true));
   private final BooleanSetting rotate = this.add(new BooleanSetting("Rotate", true));
   private final BooleanSetting packetPlace = this.add(new BooleanSetting("PacketPlace", true));
   private final Timer timer = new Timer();
   int progress = 0;

   public LavaFiller() {
      super("LavaFiller", Module.Category.Misc);
      this.setChinese("自动填岩浆");
   }

   public void onUpdate(UpdateEvent event) {
      if (this.timer.passed((long)this.placeDelay.getValue())) {
         if (!this.inventory.getValue() || EntityUtil.inInventory()) {
            this.progress = 0;
            if (this.getBlock() != -1) {
               if (!this.usingPause.getValue() || !mc.player.isUsingItem()) {
                  for (BlockPos pos : BlockUtil.getSphere(this.range.getValueFloat())) {
                     if (mc.world.getBlockState(pos).getBlock() == Blocks.LAVA && mc.world.getBlockState(pos).getFluidState().getFluid() instanceof Still) {
                        this.tryPlaceBlock(pos);
                     }
                  }
               }
            }
         }
      }
   }

   private void tryPlaceBlock(BlockPos pos) {
      if (pos != null) {
         if (!this.detectMining.getValue() || !Alien.BREAK.isMining(pos)) {
            if ((double)this.progress < this.blocksPer.getValue()) {
               int block = this.getBlock();
               if (block != -1) {
                  if (BlockUtil.canPlace(pos, this.range.getValue(), false)) {
                     int old = mc.player.getInventory().selectedSlot;
                     this.doSwap(block);
                     BlockUtil.placeBlock(pos, this.rotate.getValue(), this.packetPlace.getValue());
                     if (this.inventory.getValue()) {
                        this.doSwap(block);
                        EntityUtil.syncInventory();
                     } else {
                        this.doSwap(old);
                     }

                     this.progress++;
                     this.timer.reset();
                  }
               }
            }
         }
      }
   }

   private void doSwap(int slot) {
      if (this.inventory.getValue()) {
         Alien.INVENTORY.inventorySwap(slot, mc.player.getInventory().selectedSlot);
      } else {
         Alien.INVENTORY.setSlot(slot);
      }
   }

   private int getBlock() {
      return this.inventory.getValue() ? InventoryUtil.findBlockInventorySlot(Blocks.OBSIDIAN) : InventoryUtil.findBlock(Blocks.OBSIDIAN);
   }
}
