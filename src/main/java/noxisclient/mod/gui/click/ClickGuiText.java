package noxisclient.mod.gui.click;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import noxisclient.core.impl.FontManager;
import noxisclient.mod.modules.impl.client.ClickGui;
import noxisclient.mod.modules.impl.client.Fonts;

final class ClickGuiText {
   private static final MinecraftClient MC = MinecraftClient.getInstance();
   private static final float MARQUEE_SPEED = 32.0F;
   private static final long MARQUEE_PAUSE_MS = 700L;

   private ClickGuiText() {
   }

   static float width(String text) {
      if (text == null) {
         return 0.0F;
      } else {
         return Fonts.useClickGuiFont() ? FontManager.ui.getWidth(text) : (float)MC.textRenderer.getWidth(text);
      }
   }

   static float height() {
      return Fonts.useClickGuiFont() ? FontManager.ui.getFontHeight() : 9.0F;
   }

   static boolean shouldMarquee(String text, float maxWidth) {
      ClickGui gui = ClickGui.getInstance();
      return gui != null && gui.marquee.getValue() && maxWidth > 0.0F && width(text) > maxWidth + 0.5F;
   }

   static void draw(DrawContext context, String text, float x, float y, int color, boolean shadow) {
      if (text != null) {
         if (Fonts.useClickGuiFont()) {
            FontManager.ui.drawString(context.getMatrices(), text, x, y, color, shadow);
         } else {
            context.drawText(MC.textRenderer, text, (int)x, (int)y, color, shadow);
         }
      }
   }

   static void drawMarquee(DrawContext context, String text, float x, float y, float maxWidth, int color, boolean shadow) {
      drawMarquee(context, text, x, y, x, maxWidth, color, shadow);
   }

   static void drawMarquee(DrawContext context, String text, float drawX, float y, float clipX, float maxWidth, int color, boolean shadow) {
      if (!shouldMarquee(text, maxWidth)) {
         draw(context, text, drawX, y, color, shadow);
      } else if (ClickGuiScissor.enable(context, clipX, y - 2.0F, clipX + maxWidth, y + height() + 4.0F)) {
         draw(context, text, clipX - marqueeOffset(width(text) - maxWidth), y, color, shadow);
         context.disableScissor();
      }
   }

   private static float marqueeOffset(float overflow) {
      overflow = Math.max(0.0F, overflow);
      long travelMs = Math.max(900L, (long)(overflow / 32.0F * 1000.0F));
      long cycleMs = 700L + travelMs + 700L + travelMs;
      long phase = System.currentTimeMillis() % cycleMs;
      if (phase < 700L) {
         return 0.0F;
      } else {
         phase -= 700L;
         if (phase < travelMs) {
            return overflow * (float)phase / (float)travelMs;
         } else {
            phase -= travelMs;
            if (phase < 700L) {
               return overflow;
            } else {
               phase -= 700L;
               return overflow * (1.0F - (float)phase / (float)travelMs);
            }
         }
      }
   }
}
