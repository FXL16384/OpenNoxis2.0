package noxisclient.mod.modules.impl.render;

import java.awt.Color;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.block.entity.EnderChestBlockEntity;
import net.minecraft.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;
import noxisclient.Alien;
import noxisclient.api.utils.render.Render3DUtil;
import noxisclient.api.utils.world.BlockUtil;
import noxisclient.mod.modules.Module;
import noxisclient.mod.modules.settings.impl.ColorSetting;

public class Tracers extends Module {
   private final ColorSetting item = this.add(new ColorSetting("Item", new Color(255, 255, 255, 100)).injectBoolean(true));
   private final ColorSetting player = this.add(new ColorSetting("Player", new Color(255, 255, 255, 100)).injectBoolean(true));
   private final ColorSetting chest = this.add(new ColorSetting("Chest", new Color(255, 255, 255, 100)).injectBoolean(false));
   private final ColorSetting enderChest = this.add(new ColorSetting("EnderChest", new Color(255, 100, 255, 100)).injectBoolean(false));
   private final ColorSetting shulkerBox = this.add(new ColorSetting("ShulkerBox", new Color(15, 255, 255, 100)).injectBoolean(false));

   public Tracers() {
      super("Tracers", Module.Category.Render);
      this.setChinese("追踪者");
   }

   @Override
   public void onRender3D(MatrixStack matrixStack) {
      mc.options.getBobView().setValue(false);
      if (this.item.booleanValue || this.player.booleanValue) {
         for (Entity entity : Alien.THREAD.getEntities()) {
            if (entity instanceof ItemEntity && this.item.booleanValue) {
               this.drawLine(entity.getPos(), this.item.getValue());
            } else if (entity instanceof PlayerEntity && this.player.booleanValue && entity != mc.player) {
               this.drawLine(entity.getPos(), this.player.getValue());
            }
         }
      }

      for (BlockEntity blockEntity : BlockUtil.getTileEntities()) {
         if (blockEntity instanceof ChestBlockEntity && this.chest.booleanValue) {
            this.drawLine(blockEntity.getPos().toCenterPos(), this.chest.getValue());
         } else if (blockEntity instanceof EnderChestBlockEntity && this.enderChest.booleanValue) {
            this.drawLine(blockEntity.getPos().toCenterPos(), this.enderChest.getValue());
         } else if (blockEntity instanceof ShulkerBoxBlockEntity && this.shulkerBox.booleanValue) {
            this.drawLine(blockEntity.getPos().toCenterPos(), this.shulkerBox.getValue());
         }
      }
   }

   private void drawLine(Vec3d pos, Color color) {
      Render3DUtil.drawLine(
         pos,
         mc.gameRenderer
            .getCamera()
            .getPos()
            .add(
               Vec3d.fromPolar(mc.player.getPitch(mc.getRenderTickCounter().getTickDelta(true)), mc.player.getYaw(mc.getRenderTickCounter().getTickDelta(true)))
                  .multiply(0.2)
            ),
         color
      );
   }
}
