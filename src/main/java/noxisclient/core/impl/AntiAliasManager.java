package noxisclient.core.impl;

import com.mojang.blaze3d.systems.RenderSystem;
import java.util.ArrayDeque;
import java.util.Deque;
import noxisclient.api.utils.Wrapper;
import org.lwjgl.opengl.GL11;

public class AntiAliasManager implements Wrapper {
   private static final int GL_MULTISAMPLE = 32925;
   private final Deque<AntiAliasManager.StateSnapshot> stateStack = new ArrayDeque<>();
   private int lineHint = 4354;
   private int polygonHint = 4354;

   public void setQualityHigh() {
      this.lineHint = 4354;
      this.polygonHint = 4354;
   }

   public void setQualityLow() {
      this.lineHint = 4353;
      this.polygonHint = 4353;
   }

   public void enableLines() {
      RenderSystem.assertOnRenderThread();
      GL11.glEnable(2848);
      GL11.glHint(3154, this.lineHint);
   }

   public void disableLines() {
      RenderSystem.assertOnRenderThread();
      GL11.glDisable(2848);
   }

   public boolean isLinesEnabled() {
      return GL11.glIsEnabled(2848);
   }

   public void enablePolygons() {
      RenderSystem.assertOnRenderThread();
      GL11.glEnable(2881);
      GL11.glHint(3155, this.polygonHint);
   }

   public void disablePolygons() {
      RenderSystem.assertOnRenderThread();
      GL11.glDisable(2881);
   }

   public boolean isPolygonsEnabled() {
      return GL11.glIsEnabled(2881);
   }

   public void enableMultisample() {
      RenderSystem.assertOnRenderThread();
      GL11.glEnable(32925);
   }

   public void disableMultisample() {
      RenderSystem.assertOnRenderThread();
      GL11.glDisable(32925);
   }

   public void enableAll() {
      this.enableLines();
      this.enablePolygons();
      this.enableMultisample();
   }

   public void disableAll() {
      this.disableLines();
      this.disablePolygons();
      this.disableMultisample();
   }

   public void pushState() {
      RenderSystem.assertOnRenderThread();
      this.stateStack.push(AntiAliasManager.StateSnapshot.capture());
      this.enableAll();
   }

   public void popState() {
      RenderSystem.assertOnRenderThread();
      AntiAliasManager.StateSnapshot prev = this.stateStack.pollFirst();
      if (prev != null) {
         prev.restore();
      }
   }

   public void withAA(Runnable action) {
      this.pushState();

      try {
         action.run();
      } finally {
         this.popState();
      }
   }

   private static class StateSnapshot {
      final boolean lineSmooth;
      final boolean polygonSmooth;
      final boolean multisample;

      StateSnapshot(boolean lineSmooth, boolean polygonSmooth, boolean multisample) {
         this.lineSmooth = lineSmooth;
         this.polygonSmooth = polygonSmooth;
         this.multisample = multisample;
      }

      static AntiAliasManager.StateSnapshot capture() {
         return new AntiAliasManager.StateSnapshot(GL11.glIsEnabled(2848), GL11.glIsEnabled(2881), GL11.glIsEnabled(32925));
      }

      void restore() {
         if (this.lineSmooth) {
            GL11.glEnable(2848);
         } else {
            GL11.glDisable(2848);
         }

         if (this.polygonSmooth) {
            GL11.glEnable(2881);
         } else {
            GL11.glDisable(2881);
         }

         if (this.multisample) {
            GL11.glEnable(32925);
         } else {
            GL11.glDisable(32925);
         }
      }
   }
}
