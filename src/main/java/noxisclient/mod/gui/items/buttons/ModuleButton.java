package noxisclient.mod.gui.items.buttons;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.DrawContext;
import noxisclient.api.utils.math.Animation;
import noxisclient.api.utils.render.Render2DUtil;
import noxisclient.mod.gui.ClickGuiScreen;
import noxisclient.mod.gui.items.Item;
import noxisclient.mod.modules.Module;
import noxisclient.mod.modules.impl.client.ClickGui;
import noxisclient.mod.modules.settings.Setting;
import noxisclient.mod.modules.settings.impl.BindSetting;
import noxisclient.mod.modules.settings.impl.BooleanSetting;
import noxisclient.mod.modules.settings.impl.ColorSetting;
import noxisclient.mod.modules.settings.impl.EnumSetting;
import noxisclient.mod.modules.settings.impl.SliderSetting;
import noxisclient.mod.modules.settings.impl.StringSetting;

public class ModuleButton extends Button {
   private final Module module;
   private List<Item> items = new ArrayList<>();
   public boolean subOpen;
   public double itemHeight;
   public final Animation animation = new Animation();

   public ModuleButton(Module module) {
      super(module.getName());
      this.module = module;
      this.initSettings();
   }

   public void initSettings() {
      ArrayList<Item> newItems = new ArrayList<>();

      for (Setting setting : this.module.getSettings()) {
         if (setting instanceof BooleanSetting) {
            Setting s = (BooleanSetting)setting;
            newItems.add(new BooleanButton((BooleanSetting)s));
         }

         if (setting instanceof BindSetting) {
            Setting s = (BindSetting)setting;
            newItems.add(new BindButton((BindSetting)s));
         }

         if (setting instanceof StringSetting) {
            Setting s = (StringSetting)setting;
            newItems.add(new StringButton((StringSetting)s));
         }

         if (setting instanceof SliderSetting) {
            Setting s = (SliderSetting)setting;
            newItems.add(new SliderButton((SliderSetting)s));
         }

         if (setting instanceof EnumSetting) {
            Setting s = (EnumSetting)setting;
            newItems.add(new EnumButton((EnumSetting<?>)s));
         }

         if (setting instanceof ColorSetting) {
            Setting s = (ColorSetting)setting;
            newItems.add(new PickerButton((ColorSetting)s));
         }
      }

      this.items = newItems;
   }

   @Override
   public void update() {
      for (Item item : this.items) {
         item.update();
      }
   }

   @Override
   public void drawScreen(DrawContext context, int mouseX, int mouseY, float partialTicks) {
      boolean hovered = this.isHovering(mouseX, mouseY);
      boolean pressed = this.getState();
      float radius = 3.0F;
      Render2DUtil.drawRoundedRect(
         context.getMatrices(), this.x, this.y, (float)this.width, (float)this.height, radius, ClickGui.getInstance().defaultColor.getValue()
      );
      if (hovered) {
         Render2DUtil.drawRoundedRect(
            context.getMatrices(), this.x, this.y, (float)this.width, (float)this.height, radius, ClickGui.getInstance().hoverColor.getValue()
         );
      }

      if (pressed) {
         Render2DUtil.drawRoundedRect(
            context.getMatrices(), this.x, this.y, (float)this.width, (float)this.height, radius, ClickGui.getInstance().color.getValue()
         );
      }

      String displayName = this.module.getDisplayName();
      this.drawString(
         displayName,
         (double)(this.x + 5.0F),
         (double)(this.y - 2.0F - (float)ClickGuiScreen.getInstance().getTextOffset()),
         this.getState() ? ClickGui.getInstance().enableTextColor.getValue().getRGB() : ClickGui.getInstance().defaultTextColor.getValue().getRGB()
      );
      if (!this.items.isEmpty()) {
         this.drawString(
            this.subOpen ? "-" : "+",
            (double)(this.x + (float)this.width - 15.0F),
            (double)(this.y - 2.0F - (float)ClickGuiScreen.getInstance().getTextOffset()),
            new Color(150, 150, 200)
         );
      }

      if (this.subOpen || this.itemHeight > 0.0) {
         float height = (float)(this.height + 2);

         for (Item item : this.items) {
            if (!item.isHidden()) {
               item.setHeight(this.height);
               item.setLocation(this.x, this.y + height);
               item.setWidth(this.width);
               item.drawScreen(context, mouseX, mouseY, partialTicks);
               height += (float)(item.getHeight() + 2);
            }
         }
      }
   }

   @Override
   public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
      super.mouseClicked(mouseX, mouseY, mouseButton);
      if (!this.items.isEmpty()) {
         boolean clickedModule = this.isHovering(mouseX, mouseY);
         if (mouseButton == 1 && clickedModule) {
            this.subOpen = !this.subOpen;
            sound();
         }

         if (this.subOpen && !clickedModule) {
            for (Item item : this.items) {
               if (!item.isHidden()) {
                  item.mouseClicked(mouseX, mouseY, mouseButton);
               }
            }
         }
      }
   }

   @Override
   public void onKeyTyped(char typedChar, int keyCode) {
      super.onKeyTyped(typedChar, keyCode);
      if (!this.items.isEmpty() && this.subOpen) {
         for (Item item : this.items) {
            if (!item.isHidden()) {
               item.onKeyTyped(typedChar, keyCode);
            }
         }
      }
   }

   @Override
   public void onKeyPressed(int key) {
      super.onKeyPressed(key);
      if (!this.items.isEmpty() && this.subOpen) {
         for (Item item : this.items) {
            if (!item.isHidden()) {
               item.onKeyPressed(key);
            }
         }
      }
   }

   public int getButtonHeight() {
      return super.getHeight();
   }

   public int getItemHeight() {
      int height = 3;

      for (Item item : this.items) {
         if (!item.isHidden()) {
            height += item.getHeight() + 2;
         }
      }

      return height;
   }

   @Override
   public int getHeight() {
      if (this.subOpen) {
         int height = super.getHeight();

         for (Item item : this.items) {
            if (!item.isHidden()) {
               height += item.getHeight() + 1;
            }
         }

         return height + 2;
      } else {
         return super.getHeight();
      }
   }

   public Module getModule() {
      return this.module;
   }

   public void setSubOpen(boolean subOpen) {
      this.subOpen = subOpen;
   }

   @Override
   public void toggle() {
      this.module.toggle();
   }

   @Override
   public boolean getState() {
      return this.module.isOn();
   }
}
