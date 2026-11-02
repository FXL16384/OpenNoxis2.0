package noxisclient.mod.modules.impl.hud;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import noxisclient.api.events.impl.Render2DEvent;
import noxisclient.api.utils.math.SmoothAnimationUtil;
import noxisclient.mod.modules.HudElement;
import noxisclient.mod.modules.Module;
import noxisclient.mod.modules.settings.impl.SliderSetting;

public class PlayerModel extends Module implements HudElement {
   public static PlayerModel INSTANCE;
   private int hudX = 560;
   private int hudY = 240;
   private final SliderSetting modelSize = this.add(new SliderSetting("ModelSize", 36.0, 5.0, 200.0, 1.0));
   private final SmoothAnimationUtil smoothLookX = new SmoothAnimationUtil(0.0, 0.18, 0.05);
   private final SmoothAnimationUtil smoothLookY = new SmoothAnimationUtil(0.0, 0.18, 0.05);

   public PlayerModel() {
      super("PlayerModel", Module.Category.HUD);
      this.setChinese("玩家模型");
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
      int size = (int)this.modelSize.getValueFloat();
      return size * 2;
   }

   @Override
   public int getElementHeight() {
      int size = (int)this.modelSize.getValueFloat();
      return size * 2;
   }

   @Override
   public boolean isElementVisible() {
      return this.isOn();
   }

   @Override
   public void renderElement(DrawContext context, float tickDelta) {
      if (this.isOn() && mc.player != null && !mc.options.hudHidden) {
         this.renderModel(context);
      }
   }

   public void onRender2D(Render2DEvent event) {
      if (this.isOn() && mc.player != null && !mc.options.hudHidden) {
         this.renderModel(event.drawContext);
      }
   }

   private void renderModel(DrawContext context) {
      int size = (int)this.modelSize.getValueFloat();
      int left = this.hudX;
      int top = this.hudY;
      int right = left + this.getElementWidth();
      int bottom = top + this.getElementHeight();
      float centerX = (float)(left + right) / 2.0F;
      float centerY = (float)(top + bottom) / 2.0F;
      double windowWidth = (double)mc.getWindow().getScaledWidth();
      double windowHeight = (double)mc.getWindow().getScaledHeight();
      float rawLookX = (float)((double)centerX - mc.mouse.getX() * windowWidth / (double)mc.getWindow().getWidth());
      float rawLookY = (float)((double)centerY - mc.mouse.getY() * windowHeight / (double)mc.getWindow().getHeight());
      this.smoothLookX.setTarget((double)rawLookX);
      this.smoothLookY.setTarget((double)rawLookY);
      float lookX = (float)this.smoothLookX.update();
      float lookY = (float)this.smoothLookY.update();
      context.getMatrices().push();
      context.getMatrices().translate(0.0F, 0.0F, 1000.0F);
      InventoryScreen.drawEntity(context, left, top, right, bottom, size, 0.0625F, lookX, lookY, mc.player);
      context.getMatrices().pop();
   }
}
