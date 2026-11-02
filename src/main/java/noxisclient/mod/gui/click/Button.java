package noxisclient.mod.gui.click;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.math.MathHelper;
import noxisclient.api.utils.math.Animation;
import noxisclient.api.utils.math.Easing;

public abstract class Button extends Component {
   protected final Frame frame;
   protected Animation hoverAnimation = new Animation();
   private boolean hovered = false;

   public Button(Frame frame, float x, float y, float width, float height) {
      super(x, y, width, height);
      this.frame = frame;
   }

   @Override
   public void render(DrawContext context, float mouseX, float mouseY, float delta) {
      this.hovered = this.isWithin(mouseX, mouseY);
   }

   @Override
   public void mouseReleased(double mouseX, double mouseY, int button) {
   }

   @Override
   public void keyPressed(int keyCode, int scanCode, int modifiers) {
   }

   @Override
   public void charTyped(char chr, int modifiers) {
   }

   protected int getHoverAlpha() {
      double factor = this.hoverAnimation.get(this.hovered ? 1.0 : 0.0, 50L, Easing.Linear);
      return (int)(80.0F * MathHelper.clamp((float)factor, 0.0F, 1.0F));
   }

   public Frame getFrame() {
      return this.frame;
   }
}
