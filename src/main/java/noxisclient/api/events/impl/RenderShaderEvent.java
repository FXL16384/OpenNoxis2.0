package noxisclient.api.events.impl;

import net.minecraft.client.util.math.MatrixStack;
import noxisclient.api.events.Event;

public class RenderShaderEvent extends Event {
   private final MatrixStack matrices;
   private final float tickDelta;

   public RenderShaderEvent(MatrixStack matrices, float tickDelta) {
      this.matrices = matrices;
      this.tickDelta = tickDelta;
   }

   public MatrixStack getMatrices() {
      return this.matrices;
   }

   public float getTickDelta() {
      return this.tickDelta;
   }
}
