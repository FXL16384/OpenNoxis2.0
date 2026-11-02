package noxisclient.mod.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import java.util.ArrayList;
import java.util.Comparator;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import noxisclient.Vitality;
import noxisclient.api.utils.Wrapper;
import noxisclient.mod.Mod;
import noxisclient.mod.gui.items.Component;
import noxisclient.mod.gui.items.Item;
import noxisclient.mod.gui.items.buttons.ModuleButton;
import noxisclient.mod.modules.Module;
import noxisclient.mod.modules.impl.client.ClickGui;

public class ClickGuiScreen extends Screen {
   private static ClickGuiScreen INSTANCE = new ClickGuiScreen();
   private final ArrayList<Component> components = new ArrayList<>();

   public ClickGuiScreen() {
      super(Text.literal("Vitality"));
      this.setInstance();
      this.load();
   }

   public static ClickGuiScreen getInstance() {
      if (INSTANCE == null) {
         INSTANCE = new ClickGuiScreen();
      }

      return INSTANCE;
   }

   private void setInstance() {
      INSTANCE = this;
   }

   private void load() {
      int categoryCount = Module.Category.values().length;
      int componentWidth = 93;
      int componentSpacing = 1;
      int totalWidth = categoryCount * (componentWidth + componentSpacing) - componentSpacing;
      int screenWidth = Wrapper.mc.getWindow().getScaledWidth();
      int startX = (screenWidth - totalWidth) / 2 - 50;
      int x = startX;

      for (final Module.Category category : Module.Category.values()) {
         this.components.add(new Component(category.toString(), category, x, 4, true) {
            @Override
            public void setupItems() {
               for (Module module : Vitality.MODULE.getModules()) {
                  if (module.getCategory().equals(category)) {
                     this.addButton(new ModuleButton(module));
                  }
               }
            }
         });
         x += componentWidth + componentSpacing;
      }

      this.components.forEach(components -> components.getItems().sort(Comparator.comparing(Mod::getName)));
   }

   @Override
   public void render(DrawContext context, int mouseX, int mouseY, float delta) {
      float keyCodec = (float)ClickGui.getInstance().alphaValue;
      float scale = 0.92F + 0.08F * keyCodec;
      float slideY = (1.0F - keyCodec) * 20.0F;
      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, keyCodec);
      Item.context = context;
      int minX = Integer.MAX_VALUE;
      int minY = Integer.MAX_VALUE;
      int maxX = Integer.MIN_VALUE;
      int maxY = Integer.MIN_VALUE;

      for (Component c : this.components) {
         minX = Math.min(minX, c.getX());
         minY = Math.min(minY, c.getY());
         maxX = Math.max(maxX, c.getX() + c.getWidth());
         maxY = Math.max(maxY, c.getY() + c.getHeight());
      }

      context.getMatrices().push();
      context.getMatrices().translate(0.0F, slideY, 0.0F);
      context.getMatrices().scale(scale, scale, 1.0F);

      for (Component component : this.components) {
         for (ModuleButton button : component.getItems()) {
            button.setHidden(false);
         }

         component.drawScreen(context, mouseX, mouseY, delta);
      }

      context.getMatrices().pop();
      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
   }

   @Override
   public boolean mouseReleased(double mouseX, double mouseY, int releaseButton) {
      this.components.forEach(components -> components.mouseReleased((int)mouseX, (int)mouseY, releaseButton));
      return super.mouseReleased(mouseX, mouseY, releaseButton);
   }

   @Override
   public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
      if (InputUtil.isKeyPressed(Wrapper.mc.getWindow().getHandle(), 340)) {
         if (verticalAmount < 0.0) {
            this.components.forEach(component -> component.setX(component.getX() - 15));
         } else if (verticalAmount > 0.0) {
            this.components.forEach(component -> component.setX(component.getX() + 15));
         }
      } else if (verticalAmount < 0.0) {
         this.components.forEach(component -> component.setY(component.getY() - 15));
      } else if (verticalAmount > 0.0) {
         this.components.forEach(component -> component.setY(component.getY() + 15));
      }

      return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
   }

   @Override
   public boolean charTyped(char chr, int modifiers) {
      this.components.forEach(component -> component.onKeyTyped(chr, modifiers));
      return super.charTyped(chr, modifiers);
   }

   @Override
   public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
      this.components.forEach(component -> component.onKeyPressed(keyCode));
      return super.keyPressed(keyCode, scanCode, modifiers);
   }

   @Override
   public boolean mouseClicked(double mouseX, double mouseY, int clickedButton) {
      this.components.forEach(components -> components.mouseClicked((int)mouseX, (int)mouseY, clickedButton));
      return super.mouseClicked(mouseX, mouseY, clickedButton);
   }

   @Override
   public boolean shouldPause() {
      return false;
   }

   public final ArrayList<Component> getComponents() {
      return this.components;
   }

   public int getTextOffset() {
      return -ClickGui.getInstance().textOffset.getValueInt() - 6;
   }
}
