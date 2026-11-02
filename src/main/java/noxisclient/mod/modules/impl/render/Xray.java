package noxisclient.mod.modules.impl.render;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.BlockPos.Mutable;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import noxisclient.Alien;
import noxisclient.api.events.impl.AmbientOcclusionEvent;
import noxisclient.api.events.impl.ChunkOcclusionEvent;
import noxisclient.api.events.impl.RenderBlockEntityEvent;
import noxisclient.mod.gui.windows.WindowsScreen;
import noxisclient.mod.gui.windows.impl.ItemSelectWindow;
import noxisclient.mod.modules.Module;
import noxisclient.mod.modules.settings.impl.BooleanSetting;

public class Xray extends Module {
   public static Xray INSTANCE;
   public final BooleanSetting edit = this.add(new BooleanSetting("Edit", false).injectTask(this::openGui));
   private static final ThreadLocal<Mutable> EXPOSED_POS = ThreadLocal.withInitial(Mutable::new);

   public Xray() {
      super("Xray", Module.Category.Render);
      this.setChinese("矿物透视");
      INSTANCE = this;
   }

   private void openGui() {
      this.edit.setValueWithoutTask(false);
      if (!nullCheck()) {
         mc.setScreen(new WindowsScreen(new ItemSelectWindow(Alien.XRAY)));
      }
   }

   @Override
   public void onEnable() {
      if (!nullCheck()) {
         mc.worldRenderer.reload();
      }
   }

   @Override
   public void onDisable() {
      mc.worldRenderer.reload();
   }

   public boolean isBlocked(Block block) {
      return !Alien.XRAY.inWhitelist(block.getTranslationKey());
   }

   public void onRenderBlockEntity(RenderBlockEntityEvent event) {
      if (this.isBlocked(event.blockEntity.getCachedState().getBlock())) {
         event.cancel();
      }
   }

   public void onChunkOcclusion(ChunkOcclusionEvent event) {
      event.cancel();
   }

   public void onAmbientOcclusion(AmbientOcclusionEvent event) {
      event.lightLevel = 1.0F;
   }

   public static boolean shouldBlock(BlockState state) {
      return INSTANCE.isOn() && INSTANCE.isBlocked(state.getBlock());
   }

   public boolean modifyDrawSide(BlockState state, BlockView view, BlockPos pos, Direction facing, boolean returns) {
      if (!returns && !this.isBlocked(state.getBlock())) {
         BlockPos adjPos = pos.offset(facing);
         BlockState adjState = view.getBlockState(adjPos);
         return adjState.getCullingFace(view, adjPos, facing.getOpposite()) != VoxelShapes.fullCube()
            || adjState.getBlock() != state.getBlock()
            || isExposed(adjPos);
      } else {
         return returns;
      }
   }

   public static boolean isExposed(BlockPos blockPos) {
      for (Direction direction : Direction.values()) {
         if (!mc.world.getBlockState(EXPOSED_POS.get().set(blockPos, direction)).isOpaque()) {
            return true;
         }
      }

      return false;
   }
}
