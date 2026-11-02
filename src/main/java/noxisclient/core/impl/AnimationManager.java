package noxisclient.core.impl;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.item.ItemStack;
import noxisclient.asm.accessors.IHandledScreen;

public class AnimationManager {
   public static final List<AnimationManager.MovingItem> ANIMATIONS = new ArrayList<>();

   public static int[] getHandledScreenOrigin() {
      MinecraftClient mc = MinecraftClient.getInstance();
      if (mc.currentScreen instanceof HandledScreen handledScreen) {
         IHandledScreen var3 = (IHandledScreen)handledScreen;
         return new int[]{var3.getX(), var3.getY()};
      } else {
         return new int[]{0, 0};
      }
   }

   public static void render(DrawContext ctx) {
      if (!ANIMATIONS.isEmpty()) {
         Iterator<AnimationManager.MovingItem> it = ANIMATIONS.iterator();
         MinecraftClient mc = MinecraftClient.getInstance();

         while (it.hasNext()) {
            AnimationManager.MovingItem anim = it.next();
            float p = anim.getProgress();
            float x = anim.startX + (anim.endX - anim.startX) * p;
            float y = anim.startY + (anim.endY - anim.startY) * p;

            try {
               if (anim.stack != null) {
                  ctx.drawItem(anim.stack, (int)x, (int)y);
                  ctx.drawItemInSlot(mc.textRenderer, anim.stack, (int)x, (int)y);
               }
            } catch (Exception var8) {
               var8.printStackTrace();
            }

            if (anim.isFinished()) {
               it.remove();
            }
         }
      }
   }

   public static class MovingItem {
      public ItemStack stack;
      public float startX;
      public float startY;
      public float endX;
      public float endY;
      public long startTime;
      public long duration = 200L;

      public float getProgress() {
         float t = (float)(System.currentTimeMillis() - this.startTime) / (float)this.duration;
         t = Math.min(1.0F, t);
         return 1.0F - (float)Math.pow((double)(1.0F - t), 3.0);
      }

      public boolean isFinished() {
         return System.currentTimeMillis() - this.startTime > this.duration;
      }
   }
}
