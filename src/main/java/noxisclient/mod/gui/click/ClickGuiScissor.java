package noxisclient.mod.gui.click;

import net.minecraft.client.gui.DrawContext;
import org.joml.Matrix4f;
import org.joml.Vector3f;

final class ClickGuiScissor {
   private ClickGuiScissor() {
   }

   static boolean enable(DrawContext context, float x1, float y1, float x2, float y2) {
      Matrix4f matrix = context.getMatrices().peek().getPositionMatrix();
      Vector3f p1 = matrix.transformPosition(x1, y1, 0.0F, new Vector3f());
      Vector3f p2 = matrix.transformPosition(x2, y2, 0.0F, new Vector3f());
      int left = (int)Math.floor((double)Math.min(p1.x, p2.x));
      int top = (int)Math.floor((double)Math.min(p1.y, p2.y));
      int right = (int)Math.ceil((double)Math.max(p1.x, p2.x));
      int bottom = (int)Math.ceil((double)Math.max(p1.y, p2.y));
      if (right > left && bottom > top) {
         context.enableScissor(left, top, right, bottom);
         return true;
      } else {
         return false;
      }
   }
}
