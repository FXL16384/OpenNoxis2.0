package noxisclient.mod.gui.click;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import noxisclient.api.utils.render.Render2DUtil;
import noxisclient.core.impl.FontManager;
import noxisclient.mod.gui.items.Item;
import noxisclient.mod.modules.impl.client.ClickGui;
import noxisclient.mod.modules.impl.client.Fonts;
import noxisclient.mod.modules.impl.render.LogoutSpots;
import noxisclient.mod.modules.settings.impl.EnumSetting;

public class SoundSelectScreen extends Screen {
   private static final int ROW_HEIGHT = 16;
   private static final int HEADER_HEIGHT = 16;
   private static final int GAP = 4;
   private static final LogoutSpots.MioSound[] SOUNDS = LogoutSpots.MioSound.values();
   private final EnumSetting<?> setting;
   private final Screen parent;
   private double scroll;

   public SoundSelectScreen(EnumSetting<?> setting, Screen parent) {
      super(Text.literal("Sound Select"));
      this.setting = setting;
      this.parent = parent;
   }

   public static boolean isMioSoundSetting(EnumSetting<?> setting) {
      return setting != null && setting.getValue() instanceof LogoutSpots.MioSound;
   }

   public static void open(EnumSetting<?> setting, Screen parent) {
      if (isMioSoundSetting(setting)) {
         MinecraftClient.getInstance().setScreen(new SoundSelectScreen(setting, parent));
      }
   }

   @Override
   public void render(DrawContext context, int mouseX, int mouseY, float delta) {
      int panelWidth = Math.min(140, this.width - 24);
      int panelHeight = Math.min(this.height - 32, 16 + SOUNDS.length * 16 + 8);
      int left = (this.width - panelWidth) / 2;
      int top = (this.height - panelHeight) / 2;
      int right = left + panelWidth;
      int bottom = top + panelHeight;
      ClickGui gui = ClickGui.getInstance();
      int accent = gui.color.getValue().getRGB();
      int base = gui.backGround.booleanValue ? gui.backGround.getValue().getRGB() : gui.defaultColor.getValue().getRGB();
      int row = gui.defaultColor.getValue().getRGB();
      int rowHover = gui.hoverColor.getValue().getRGB();
      float radius = gui.getGuiCornerRadius();
      context.getMatrices().push();
      context.getMatrices().translate(0.0F, 0.0F, 1000.0F);
      if (!gui.isExhibitionStyle() && gui.glow.getValue() && gui.glowFrame.getValue()) {
         Render2DUtil.drawGlowEffect(
            context.getMatrices(),
            (float)left,
            (float)top,
            (float)panelWidth,
            (float)panelHeight,
            accent,
            gui.glowIntensity.getValueFloat(),
            gui.glowLayers.getValueInt()
         );
      }

      Render2DUtil.drawRoundedRect(context.getMatrices(), (float)left, (float)top, (float)panelWidth, (float)panelHeight, radius, base);
      context.fill(left, top, right, top + 16, accent);
      if (gui.border.getValue()) {
         if (gui.gradientBorder.getValue()) {
            Render2DUtil.drawGradientRoundedBorder(
               context.getMatrices(),
               (float)left,
               (float)top,
               (float)panelWidth,
               (float)panelHeight,
               radius,
               gui.borderColor.getValue().getRGB(),
               gui.borderColor2.getValue().getRGB(),
               gui.borderThickness.getValueFloat()
            );
         } else {
            Render2DUtil.drawRoundedBorder(
               context.getMatrices(),
               (float)left,
               (float)top,
               (float)panelWidth,
               (float)panelHeight,
               radius,
               gui.borderColor.getValue().getRGB(),
               gui.borderThickness.getValueFloat()
            );
         }
      }

      this.drawString(context, this.setting.getName(), (float)(left + 8), (float)(top + 5), gui.getEnableTextColor());
      int clipTop = top + 16 + 4;
      int clipBottom = bottom - 4;
      context.enableScissor(left, clipTop, right, clipBottom);
      int y = (int)((double)clipTop - this.scroll);
      String section = "";

      for (LogoutSpots.MioSound sound : SOUNDS) {
         String nextSection = this.isKillstreak(sound) ? "Killstreaks" : "System";
         if (!nextSection.equals(section)) {
            section = nextSection;
            if (y + 16 >= clipTop && y <= clipBottom) {
               this.drawString(context, nextSection, (float)(left + 8), (float)(y + 4), gui.getDefaultTextColor());
            }

            y += 16;
         }

         if (y + 16 >= clipTop && y <= clipBottom) {
            boolean hovered = mouseX >= left + 4 && mouseX <= right - 4 && mouseY >= y && mouseY <= y + 16;
            boolean selected = this.setting.getValue() == sound;
            context.fill(left + 4, y, right - 4, y + 16 - 1, selected ? accent : (hovered ? rowHover : row));
            this.drawString(context, sound.toString(), (float)(left + 10), (float)(y + 4), selected ? gui.getEnableTextColor() : gui.getDefaultTextColor());
         }

         y += 16;
      }

      context.disableScissor();
      context.getMatrices().pop();
   }

   @Override
   public boolean mouseClicked(double mouseX, double mouseY, int button) {
      LogoutSpots.MioSound hovered = this.hoveredSound(mouseX, mouseY);
      if (hovered == null) {
         return super.mouseClicked(mouseX, mouseY, button);
      } else if (button == 0) {
         this.setSound(hovered);
         if (this.client != null) {
            this.client.setScreen(this.parent);
         }

         Item.sound();
         return true;
      } else if (button == 1) {
         LogoutSpots.playMioSound(hovered);
         return true;
      } else {
         return super.mouseClicked(mouseX, mouseY, button);
      }
   }

   @Override
   public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
      this.scroll = Math.max(0.0, Math.min(this.maxScroll(), this.scroll - verticalAmount * 16.0));
      return true;
   }

   @Override
   public void close() {
      this.returnToParent();
   }

   @Override
   public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
      if (keyCode == 256) {
         this.returnToParent();
         return true;
      } else {
         return super.keyPressed(keyCode, scanCode, modifiers);
      }
   }

   @Override
   public boolean shouldCloseOnEsc() {
      return false;
   }

   private void returnToParent() {
      if (this.client != null) {
         this.client.setScreen((Screen)(this.parent != null ? this.parent : NewClickGuiScreen.getInstance()));
      }
   }

   @Override
   public boolean shouldPause() {
      return false;
   }

   private LogoutSpots.MioSound hoveredSound(double mouseX, double mouseY) {
      int panelWidth = Math.min(140, this.width - 24);
      int panelHeight = Math.min(this.height - 32, 16 + SOUNDS.length * 16 + 8);
      int left = (this.width - panelWidth) / 2;
      int top = (this.height - panelHeight) / 2;
      int right = left + panelWidth;
      int clipTop = top + 16 + 4;
      int clipBottom = top + panelHeight - 4;
      if (!(mouseX < (double)(left + 4)) && !(mouseX > (double)(right - 4)) && !(mouseY < (double)clipTop) && !(mouseY > (double)clipBottom)) {
         int y = (int)((double)clipTop - this.scroll);
         String section = "";

         for (LogoutSpots.MioSound sound : SOUNDS) {
            String nextSection = this.isKillstreak(sound) ? "Killstreaks" : "System";
            if (!nextSection.equals(section)) {
               section = nextSection;
               y += 16;
            }

            if (mouseY >= (double)y && mouseY <= (double)(y + 16)) {
               return sound;
            }

            y += 16;
         }

         return null;
      } else {
         return null;
      }
   }

   private double maxScroll() {
      int panelHeight = Math.min(this.height - 32, 16 + SOUNDS.length * 16 + 8);
      int visibleHeight = panelHeight - 16 - 8;
      int contentHeight = SOUNDS.length * 16 + 32;
      return (double)Math.max(0, contentHeight - visibleHeight);
   }

   private boolean isKillstreak(LogoutSpots.MioSound sound) {
      return switch (sound) {
         case DOMINATING, DOUBLEKILL, GODLIKE, HEADHUNTER, HUMILIATION, IMPRESSIVE, KILLINGSPREE, LUDICROUSKILL, MEGAKILL, MONSTERKILL, MULTIKILL, PERFECT, RAMPAGE, TRIPLEKILL, ULTRAKILL, UNSTOPPABLE, WICKEDSICK -> true;
         default -> false;
      };
   }

   private void drawString(DrawContext context, String text, float x, float y, int color) {
      if (Fonts.useClickGuiFont()) {
         FontManager.ui.drawString(context.getMatrices(), text, x, y, color, ClickGui.getInstance().shadow.getValue());
      } else {
         context.drawText(this.textRenderer, text, (int)x, (int)y, color, ClickGui.getInstance().shadow.getValue());
      }
   }

   private void setSound(LogoutSpots.MioSound sound) {
      ((EnumSetting<LogoutSpots.MioSound>)this.setting).setValue(sound);
   }
}
