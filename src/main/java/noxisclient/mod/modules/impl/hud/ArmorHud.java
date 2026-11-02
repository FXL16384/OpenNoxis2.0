package noxisclient.mod.modules.impl.hud;

import java.awt.Color;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.ItemStack;
import noxisclient.api.utils.player.EntityUtil;
import noxisclient.api.utils.render.ColorUtil;
import noxisclient.api.utils.render.TextUtil;
import noxisclient.core.impl.FontManager;
import noxisclient.mod.modules.HudElement;
import noxisclient.mod.modules.Module;
import noxisclient.mod.modules.settings.impl.BooleanSetting;
import noxisclient.mod.modules.settings.impl.SliderSetting;

public class ArmorHud extends Module implements HudElement {
   public static ArmorHud INSTANCE;
   private int hudX = -1;
   private int hudY = -1;
   private HudElement.AlignMode alignMode = HudElement.AlignMode.LEFT;
   private static final int ITEM_SIZE = 16;
   private static final int ITEM_SPACING = 20;
   private static final int ARMOR_SLOTS = 4;
   public final BooleanSetting shadow = this.add(new BooleanSetting("Shadow", true));
   public final BooleanSetting durability = this.add(new BooleanSetting("Durability", true));
   public final SliderSetting size = this.add(new SliderSetting("Size", 1.0, 0.5, 3.0, 0.05));
   private static final Color MIN_COLOR = new Color(196, 0, 0);
   private static final Color MAX_COLOR = new Color(0, 227, 0);

   public ArmorHud() {
      super("Armor", Module.Category.HUD);
      this.setChinese("装备显示");
      INSTANCE = this;
   }

   private void initDefaultPosition() {
      if (mc != null && mc.getWindow() != null) {
         this.hudX = mc.getWindow().getScaledWidth() / 2 - this.getElementWidth() / 2;
         this.hudY = mc.getWindow().getScaledHeight() - 55 - this.getDurabilityOffset();
         this.alignMode = HudElement.AlignMode.LEFT;
      }
   }

   @Override
   public void onRender2D(DrawContext drawContext, float tickDelta) {
      this.renderElement(drawContext, tickDelta);
   }

   @Override
   public String getElementName() {
      return this.getChinese() != null ? this.getChinese() : this.getName();
   }

   @Override
   public int getElementX() {
      if (this.hudX < 0) {
         this.initDefaultPosition();
      }

      return this.alignMode == HudElement.AlignMode.RIGHT ? this.hudX - this.getElementWidth() : this.hudX;
   }

   @Override
   public void setElementX(int x) {
      if (this.alignMode == HudElement.AlignMode.RIGHT) {
         this.hudX = x + this.getElementWidth();
      } else {
         this.hudX = x;
      }
   }

   @Override
   public int getElementY() {
      if (this.hudY < 0) {
         this.initDefaultPosition();
      }

      return this.hudY;
   }

   @Override
   public void setElementY(int y) {
      this.hudY = y;
   }

   @Override
   public int getElementWidth() {
      return 80;
   }

   @Override
   public int getElementHeight() {
      return !this.durability.getValue() ? 16 : 16 + this.getDurabilityOffset();
   }

   @Override
   public boolean isElementVisible() {
      return this.isOn();
   }

   @Override
   public HudElement.AlignMode getAlignMode() {
      return this.alignMode;
   }

   @Override
   public void setAlignMode(HudElement.AlignMode mode) {
      if (mode != this.alignMode) {
         if (mode == HudElement.AlignMode.RIGHT && this.alignMode == HudElement.AlignMode.LEFT) {
            this.hudX = this.hudX + this.getElementWidth();
         } else if (mode == HudElement.AlignMode.LEFT && this.alignMode == HudElement.AlignMode.RIGHT) {
            this.hudX = this.hudX - this.getElementWidth();
         }

         this.alignMode = mode;
      }
   }

   @Override
   public int getRightAnchorX() {
      return this.alignMode == HudElement.AlignMode.RIGHT ? this.hudX : this.hudX + this.getElementWidth();
   }

   @Override
   public void setRightAnchorX(int rightX) {
      if (this.alignMode == HudElement.AlignMode.RIGHT) {
         this.hudX = rightX;
      } else {
         this.hudX = rightX - this.getElementWidth();
      }
   }

   @Override
   public void renderElement(DrawContext context, float tickDelta) {
      if (mc.player != null) {
         int baseX = this.getElementX();
         int baseY = this.getElementY() + this.getDurabilityOffset();
         int drawX = baseX;

         for (int i = 3; i >= 0; i--) {
            ItemStack stack = mc.player.getInventory().getArmorStack(i);
            if (!stack.isEmpty()) {
               context.drawItem(stack, drawX, baseY);
               context.drawItemInSlot(mc.textRenderer, stack, drawX, baseY);
               if (this.durability.getValue() && stack.isDamageable()) {
                  int damage = EntityUtil.getDamagePercent(stack);
                  Color color = ColorUtil.fadeColor(MIN_COLOR, MAX_COLOR, (float)damage / 100.0F);
                  String damageText = damage + "%";
                  float textScale = this.getDurabilityTextScale();
                  float textX = (float)drawX + (16.0F - TextUtil.getHudWidth(damageText, textScale)) / 2.0F;
                  float textY = (float)baseY - TextUtil.getHudHeight(textScale) / 2.0F;
                  TextUtil.drawHudString(context, damageText, (double)textX, (double)textY, color.getRGB(), this.shadow.getValue(), textScale);
               }
            }

            drawX += 20;
         }
      }
   }

   private float getDurabilityTextScale() {
      float baseScale = FontManager.isCustom() ? 1.0F : 0.5F;
      return baseScale * this.size.getValueFloat();
   }

   private int getDurabilityOffset() {
      return !this.durability.getValue() ? 0 : (int)Math.ceil((double)(TextUtil.getHudHeight(this.getDurabilityTextScale()) / 2.0F));
   }
}
