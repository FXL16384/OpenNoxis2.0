package noxisclient.mod.modules.impl.client;

import java.awt.Color;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import noxisclient.Alien;
import noxisclient.api.utils.Wrapper;
import noxisclient.api.utils.render.Render2DUtil;
import noxisclient.mod.modules.Module;
import noxisclient.mod.modules.impl.misc.ShulkerViewer;
import noxisclient.mod.modules.settings.impl.ColorSetting;
import noxisclient.mod.modules.settings.impl.EnumSetting;
import noxisclient.mod.modules.settings.impl.SliderSetting;

public class EcCache extends Module {
   public static EcCache INSTANCE;
   private final EnumSetting<EcCache.Corner> corner = this.add(new EnumSetting<>("Corner", EcCache.Corner.RightBottom));
   private final SliderSetting slotSize = this.add(new SliderSetting("SlotSize", 18.0, 12.0, 24.0, 1.0));
   private final SliderSetting xOffset = this.add(new SliderSetting("X", 0.0, -400.0, 400.0, 1.0));
   private final SliderSetting yOffset = this.add(new SliderSetting("Y", 0.0, -300.0, 300.0, 1.0));
   private final ColorSetting innerBackground = this.add(new ColorSetting("Background", new Color(18, 18, 18, 236)));

   public EcCache() {
      super("EcCache", "Show cached ender chest items in inventory.", Module.Category.Client);
      this.setChinese("EcCache");
      INSTANCE = this;
   }

   public void renderPreview(DrawContext context, int mouseX, int mouseY) {
      if (this.isOn() && Alien.PLAYER.known && Wrapper.mc.textRenderer != null) {
         int cols = 9;
         int rows = 3;
         int slotSize = this.slotSize.getValueInt();
         int outerPad = 6;
         int innerPad = 4;
         int titleHeight = 9 + 6;
         int gridWidth = cols * slotSize;
         int gridHeight = rows * slotSize;
         int width = gridWidth + outerPad * 2;
         int height = titleHeight + gridHeight + outerPad + innerPad;
         int screenWidth = Wrapper.mc.getWindow().getScaledWidth();
         int screenHeight = Wrapper.mc.getWindow().getScaledHeight();

         int x = switch ((EcCache.Corner)this.corner.getValue()) {
            case LeftTop, LeftBottom -> 8;
            case RightTop, RightBottom -> screenWidth - width - 8;
         } + this.xOffset.getValueInt();

         int y = switch ((EcCache.Corner)this.corner.getValue()) {
            case LeftTop, RightTop -> 8;
            case LeftBottom, RightBottom -> screenHeight - height - 8;
         } + this.yOffset.getValueInt();
         ClickGui gui = ClickGui.getInstance();
         int backgroundColor = gui != null ? gui.defaultColor.getValue().getRGB() : new Color(30, 30, 30, 236).getRGB();
         int innerColor = this.innerBackground.getValue().getRGB();
         float radius = 6.0F;
         Render2DUtil.drawRoundedRect(context.getMatrices(), (float)x, (float)y, (float)width, (float)height, radius, backgroundColor);
         Render2DUtil.drawRoundedRect(
            context.getMatrices(),
            (float)x + 2.0F,
            (float)(y + titleHeight) - 1.0F,
            (float)width - 4.0F,
            (float)(height - titleHeight) - 1.0F,
            Math.max(0.0F, radius - 2.0F),
            innerColor
         );
         context.drawText(Wrapper.mc.textRenderer, Text.literal("Ender Chest"), x + outerPad, y + 3, -1, true);
         ItemStack hoveredShulker = ItemStack.EMPTY;

         for (int i = 0; i < 27; i++) {
            ItemStack stack = Alien.PLAYER.ENDERCHEST_ITEM.get(i);
            int col = i % cols;
            int row = i / cols;
            int itemX = x + outerPad + col * slotSize;
            int itemY = y + titleHeight + innerPad + row * slotSize;
            context.drawItem(stack, itemX, itemY);
            context.drawItemInSlot(Wrapper.mc.textRenderer, stack, itemX, itemY);
            boolean hovered = mouseX >= itemX && mouseX <= itemX + 16 && mouseY >= itemY && mouseY <= itemY + 16;
            if (hovered && ShulkerViewer.hasItems(stack)) {
               hoveredShulker = stack;
            }
         }

         if (!hoveredShulker.isEmpty()) {
            this.renderNestedPreview(context, hoveredShulker, x, y + height + 6, width, gui, backgroundColor, innerColor, radius, titleHeight);
         }
      }
   }

   private void renderNestedPreview(
      DrawContext context, ItemStack stack, int x, int y, int width, ClickGui gui, int backgroundColor, int innerColor, float radius, int titleHeight
   ) {
      int cols = 9;
      int rows = 3;
      int slotSize = this.slotSize.getValueInt();
      int outerPad = 6;
      int innerPad = 4;
      int height = titleHeight + rows * slotSize + outerPad + innerPad;
      ItemStack[] previewItems = new ItemStack[27];
      ShulkerViewer.getItemsInContainerItem(stack, previewItems);
      Render2DUtil.drawRoundedRect(context.getMatrices(), (float)x, (float)y, (float)width, (float)height, radius, backgroundColor);
      Render2DUtil.drawRoundedRect(
         context.getMatrices(),
         (float)x + 2.0F,
         (float)(y + titleHeight) - 1.0F,
         (float)width - 4.0F,
         (float)(height - titleHeight) - 1.0F,
         Math.max(0.0F, radius - 2.0F),
         innerColor
      );
      context.drawText(Wrapper.mc.textRenderer, stack.getName(), x + outerPad, y + 3, -1, true);

      for (int i = 0; i < previewItems.length; i++) {
         int col = i % cols;
         int row = i / cols;
         int itemX = x + outerPad + col * slotSize;
         int itemY = y + titleHeight + innerPad + row * slotSize;
         context.drawItem(previewItems[i], itemX, itemY);
         context.drawItemInSlot(Wrapper.mc.textRenderer, previewItems[i], itemX, itemY);
      }
   }

   public static enum Corner {
      LeftTop,
      RightTop,
      LeftBottom,
      RightBottom;
   }
}
