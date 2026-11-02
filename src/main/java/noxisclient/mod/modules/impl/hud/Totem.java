package noxisclient.mod.modules.impl.hud;

import java.awt.Color;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.Items;
import noxisclient.api.utils.player.InventoryUtil;
import noxisclient.api.utils.render.TextUtil;
import noxisclient.mod.modules.Module;
import noxisclient.mod.modules.settings.impl.BooleanSetting;
import noxisclient.mod.modules.settings.impl.SliderSetting;

public class Totem extends Module {
   public static Totem INSTANCE;
   public final BooleanSetting lockOnHotbar = this.add(new BooleanSetting("LockOnHotbar", true));
   public final SliderSetting posX = this.add(new SliderSetting("PosX", 0.0, 0.0, 1920.0, 1.0));
   public final SliderSetting posY = this.add(new SliderSetting("PosY", 150.0, 0.0, 1080.0, 1.0));
   public final SliderSetting size = this.add(new SliderSetting("Size", 1.0, 0.5, 3.0, 0.05));
   private final int width = 16;
   private final int height = 16;

   public Totem() {
      super("Totem", Module.Category.HUD);
      this.setChinese("图腾");
      INSTANCE = this;
   }

   @Override
   public void onRender2D(DrawContext drawContext, float tickDelta) {
      if (mc.player != null) {
         MatrixStack matrixStack = drawContext.getMatrices();
         int renderX;
         int renderY;
         if (this.lockOnHotbar.getValue()) {
            renderX = mc.getWindow().getWidth() / 4 - 8;
            renderY = mc.getWindow().getScaledHeight() - 16;
            renderY = mc.player.isCreative() ? renderY - 28 : renderY - 38;
         } else {
            renderX = (int)this.posX.getValue();
            renderY = (int)this.posY.getValue();
         }

         matrixStack.push();
         int count = InventoryUtil.getItemCount(Items.TOTEM_OF_UNDYING);
         drawContext.drawItem(Items.TOTEM_OF_UNDYING.getDefaultStack(), renderX + 16 - 16, renderY + 16 - 16);
         String countText = String.valueOf(count);
         float textScale = this.size.getValueFloat();
         float textX = (float)(renderX + 16) - TextUtil.getHudWidth(countText, textScale) + 2.0F;
         float textY = (float)(renderY + 16) - TextUtil.getHudHeight(textScale) + 4.0F;
         TextUtil.drawHudString(drawContext, countText, (double)textX, (double)textY, new Color(255, 255, 255, 255).getRGB(), true, textScale);
         matrixStack.pop();
      }
   }
}
