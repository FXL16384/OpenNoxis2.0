package noxisclient.mod.modules.impl.hud;

import java.awt.Color;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.ItemStack;
import noxisclient.api.utils.render.TextUtil;
import noxisclient.core.impl.FontManager;
import noxisclient.mod.modules.HudElement;
import noxisclient.mod.modules.Module;
import noxisclient.mod.modules.settings.impl.BooleanSetting;
import noxisclient.mod.modules.settings.impl.ColorSetting;
import noxisclient.mod.modules.settings.impl.SliderSetting;

public class BackpackContents extends Module implements HudElement {
   public static BackpackContents INSTANCE;
   private int hudX = 100;
   private int hudY = 100;
   private final SliderSetting slotSize = this.add(new SliderSetting("SlotSize", 16, 12, 24));
   private final SliderSetting spacing = this.add(new SliderSetting("Spacing", 2, 0, 6));
   private final BooleanSetting showBackground = this.add(new BooleanSetting("ShowBackground", true));
   private final SliderSetting backgroundAlpha = this.add(new SliderSetting("BackgroundAlpha", 80, 0, 255));
   private final BooleanSetting showBorder = this.add(new BooleanSetting("ShowBorder", true));
   private final ColorSetting borderColor = this.add(new ColorSetting("BorderColor", new Color(255, 255, 255)));
   private final BooleanSetting showCount = this.add(new BooleanSetting("ShowCount", true));
   private final SliderSetting fontScale = this.add(new SliderSetting("Size", 1.0, 0.5, 3.0, 0.05, this.showCount::getValue));
   private final BooleanSetting showDurability = this.add(new BooleanSetting("ShowDurability", true));

   public BackpackContents() {
      super("Inventory", Module.Category.HUD);
      this.setChinese("背包物资显示");
      INSTANCE = this;
   }

   @Override
   public String getElementName() {
      return this.getChinese() != null ? this.getChinese() : this.getName();
   }

   @Override
   public int getElementX() {
      return this.hudX;
   }

   @Override
   public void setElementX(int x) {
      this.hudX = x;
   }

   @Override
   public int getElementY() {
      return this.hudY;
   }

   @Override
   public void setElementY(int y) {
      this.hudY = y;
   }

   @Override
   public int getElementWidth() {
      return 9 * this.slotSize.getValueInt() + 8 * this.spacing.getValueInt();
   }

   @Override
   public int getElementHeight() {
      return 3 * this.slotSize.getValueInt() + 2 * this.spacing.getValueInt();
   }

   @Override
   public boolean isElementVisible() {
      return this.isOn();
   }

   @Override
   public void onRender2D(DrawContext drawContext, float tickDelta) {
      this.renderElement(drawContext, tickDelta);
   }

   @Override
   public void renderElement(DrawContext drawContext, float tickDelta) {
      if (mc.player != null) {
         int ss = this.slotSize.getValueInt();
         int sp = this.spacing.getValueInt();
         int tw = this.getElementWidth();
         int th = this.getElementHeight();
         int sx = this.hudX;
         int sy = this.hudY;
         if (this.showBackground.getValue()) {
            drawContext.fill(sx, sy, sx + tw, sy + th, this.backgroundAlpha.getValueInt() << 24 | 2105376);
         }

         if (this.showBorder.getValue()) {
            int c = this.borderColor.getValue().getRGB();
            drawContext.fill(sx - 1, sy - 1, sx + tw + 1, sy, c);
            drawContext.fill(sx - 1, sy + th, sx + tw + 1, sy + th + 1, c);
            drawContext.fill(sx - 1, sy, sx, sy + th, c);
            drawContext.fill(sx + tw, sy, sx + tw + 1, sy + th, c);
         }

         for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
               ItemStack stack = mc.player.getInventory().getStack(row * 9 + col + 9);
               if (!stack.isEmpty()) {
                  int x = sx + col * (ss + sp);
                  int y = sy + row * (ss + sp);
                  drawContext.drawItem(stack, x + 1, y + 1);
                  if (this.showCount.getValue() || this.showDurability.getValue()) {
                     ItemStack overlayStack = stack.copy();
                     if (!this.showCount.getValue() || FontManager.isCustom()) {
                        overlayStack.setCount(1);
                     }

                     if (!this.showDurability.getValue() && overlayStack.isDamageable()) {
                        overlayStack.setDamage(0);
                     }

                     drawContext.drawItemInSlot(mc.textRenderer, overlayStack, x + 1, y + 1);
                  }

                  if (FontManager.isCustom() && this.showCount.getValue() && stack.getCount() != 1) {
                     String count = String.valueOf(stack.getCount());
                     float scale = this.fitCountScale(count, this.fontScale.getValueFloat(), ss);
                     float textX = (float)(x + ss) - 1.0F - TextUtil.getHudWidth(count, scale);
                     float textY = (float)y + 18.0F - TextUtil.getHudHeight(scale);
                     TextUtil.drawHudString(drawContext, count, (double)textX, (double)textY, Color.WHITE.getRGB(), true, scale);
                  }
               }
            }
         }
      }
   }

   private float fitCountScale(String text, float requestedScale, int slotWidth) {
      float availableWidth = Math.max(1.0F, (float)slotWidth - 2.0F);
      float requestedWidth = TextUtil.getHudWidth(text, requestedScale);
      return requestedWidth <= availableWidth ? requestedScale : Math.max(0.1F, requestedScale * availableWidth / requestedWidth);
   }
}
