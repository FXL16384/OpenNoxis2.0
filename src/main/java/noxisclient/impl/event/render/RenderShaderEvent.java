package noxisclient.impl.event.render;

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

   public static class BlockEntities extends RenderShaderEvent {
      public BlockEntities(MatrixStack matrices, float tickDelta) {
         super(matrices, tickDelta);
      }
   }
}
