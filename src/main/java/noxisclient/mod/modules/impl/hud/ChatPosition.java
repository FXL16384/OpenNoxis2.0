package noxisclient.mod.modules.impl.hud;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Style;
import noxisclient.Alien;
import noxisclient.api.utils.math.Easing;
import noxisclient.mod.modules.HudElement;
import noxisclient.mod.modules.Module;
import noxisclient.mod.modules.settings.impl.BooleanSetting;
import noxisclient.mod.modules.settings.impl.ColorSetting;
import noxisclient.mod.modules.settings.impl.EnumSetting;
import noxisclient.mod.modules.settings.impl.SliderSetting;

public class ChatPosition extends Module implements HudElement {
   public static ChatPosition INSTANCE;
   public final SliderSetting x = this.add(new SliderSetting("X", 2.0, -500.0, 2000.0, 1.0));
   public final SliderSetting y = this.add(new SliderSetting("Y", 2.0, -500.0, 2000.0, 1.0));
   public final SliderSetting width = this.add(new SliderSetting("Width", 320.0, 40.0, 2000.0, 1.0));
   public final SliderSetting height = this.add(new SliderSetting("Height", 180.0, 20.0, 2000.0, 1.0));
   public final ColorSetting friends = this.add(new ColorSetting("Friends", new Color(85, 255, 255)).injectBoolean(false));
   public final ColorSetting enemys = this.add(new ColorSetting("Enemys", new Color(255, 85, 85)).injectBoolean(false));
   public final ColorSetting self = this.add(new ColorSetting("Self", new Color(85, 255, 85)).injectBoolean(false));
   public final BooleanSetting keepHistory = this.add(new BooleanSetting("KeepHistory", true));
   public final BooleanSetting infiniteChat = this.add(new BooleanSetting("InfiniteChat", true));
   public final BooleanSetting hideIndicator = this.add(new BooleanSetting("HideIndicator", true));
   public final SliderSetting animationTime = this.add(new SliderSetting("AnimationTime", 300.0, 0.0, 1000.0, 1.0));
   public final EnumSetting<Easing> ease = this.add(new EnumSetting<>("Ease", Easing.CubicInOut));
   public final SliderSetting animateOffset = this.add(new SliderSetting("Offset", -40.0, -200.0, 100.0, 1.0));
   public final BooleanSetting fade = this.add(new BooleanSetting("Fade", true));
   public final BooleanSetting blur = this.add(new BooleanSetting("Blur", false).setParent());
   public final SliderSetting blurRadius = this.add(new SliderSetting("BlurRadius", 10.0, 1.0, 50.0, 1.0, this.blur::isOpen));

   public ChatPosition() {
      super("Chat", Module.Category.HUD);
      this.setChinese("聊天");
      INSTANCE = this;
   }

   public static boolean shouldEdit() {
      return INSTANCE != null && INSTANCE.isOn();
   }

   public static int getXPos() {
      return shouldEdit() ? INSTANCE.x.getValueInt() : 2;
   }

   public static int getYPos() {
      return shouldEdit() ? INSTANCE.y.getValueInt() : 2;
   }

   public static int getWidthValue() {
      return shouldEdit() ? INSTANCE.width.getValueInt() : 320;
   }

   public static int getHeightValue() {
      return shouldEdit() ? INSTANCE.height.getValueInt() : 180;
   }

   public static boolean shouldBlur() {
      return shouldEdit() && INSTANCE.blur.getValue();
   }

   public static float getBlurRadius() {
      return shouldBlur() ? INSTANCE.blurRadius.getValueFloat() : 0.0F;
   }

   public static OrderedText colorizeNames(OrderedText text) {
      if (shouldEdit() && text != null && hasEnabledNameColor()) {
         List<ChatPosition.StyledGlyph> glyphs = new ArrayList<>();
         StringBuilder content = new StringBuilder();
         text.accept((index, style, codePoint) -> {
            glyphs.add(new ChatPosition.StyledGlyph(index, style, codePoint, content.length()));
            content.appendCodePoint(codePoint);
            return true;
         });
         if (glyphs.isEmpty()) {
            return text;
         } else {
            String plainText = content.toString();
            int[] colors = new int[plainText.length()];
            Arrays.fill(colors, -1);
            if (INSTANCE.enemys.booleanValue && Alien.ENEMY != null) {
               applyNameColor(plainText, new ArrayList<>(Alien.ENEMY.enemyList), INSTANCE.enemys.getValue(), colors);
            }

            if (INSTANCE.friends.booleanValue && Alien.FRIEND != null) {
               applyNameColor(plainText, new ArrayList<>(Alien.FRIEND.friendList), INSTANCE.friends.getValue(), colors);
            }

            if (INSTANCE.self.booleanValue && mc.player != null) {
               applyNameColor(plainText, List.of(mc.player.getGameProfile().getName()), INSTANCE.self.getValue(), colors);
            }

            boolean hasColor = false;

            for (int color : colors) {
               if (color != -1) {
                  hasColor = true;
                  break;
               }
            }

            return !hasColor ? text : visitor -> {
               for (ChatPosition.StyledGlyph glyph : glyphs) {
                  int colorx = colors[glyph.charIndex()];
                  Style style = colorx == -1 ? glyph.style() : glyph.style().withColor(colorx);
                  if (!visitor.accept(glyph.index(), style, glyph.codePoint())) {
                     return false;
                  }
               }

               return true;
            };
         }
      } else {
         return text;
      }
   }

   private static boolean hasEnabledNameColor() {
      return INSTANCE.friends.booleanValue || INSTANCE.enemys.booleanValue || INSTANCE.self.booleanValue;
   }

   private static void applyNameColor(String text, List<String> names, Color color, int[] colors) {
      int rgb = color.getRGB() & 16777215;

      for (String name : names) {
         if (name != null && !name.isEmpty()) {
            int fromIndex = 0;

            while (fromIndex < text.length()) {
               int index = text.indexOf(name, fromIndex);
               if (index < 0) {
                  break;
               }

               int end = index + name.length();
               if (isNameBoundary(text, index, end)) {
                  Arrays.fill(colors, index, end, rgb);
               }

               fromIndex = index + Math.max(1, name.length());
            }
         }
      }
   }

   private static boolean isNameBoundary(String text, int start, int end) {
      return (start == 0 || !isUsernameCharacter(text.charAt(start - 1))) && (end == text.length() || !isUsernameCharacter(text.charAt(end)));
   }

   private static boolean isUsernameCharacter(char character) {
      return Character.isLetterOrDigit(character) || character == '_';
   }

   @Override
   public String getElementName() {
      return this.getChinese() != null ? this.getChinese() : this.getName();
   }

   @Override
   public int getElementX() {
      return this.x.getValueInt();
   }

   @Override
   public void setElementX(int x) {
      this.x.setValue((double)x);
   }

   @Override
   public int getElementY() {
      return this.y.getValueInt();
   }

   @Override
   public void setElementY(int y) {
      this.y.setValue((double)y);
   }

   @Override
   public int getElementWidth() {
      return this.width.getValueInt();
   }

   @Override
   public int getElementHeight() {
      return this.height.getValueInt();
   }

   @Override
   public boolean isElementVisible() {
      return this.isOn();
   }

   @Override
   public void renderElement(DrawContext context, float tickDelta) {
      int x = this.getElementX();
      int y = this.getElementY();
      int w = this.getElementWidth();
      int h = this.getElementHeight();
      context.fill(x, y, x + w, y + h, 889192448);
      int lineX = x + 4;
      int lineY = y + h - 14;
      int lineHeight = 10;

      for (int i = 0; i < 6; i++) {
         int alpha = 80 - i * 10;
         if (alpha <= 0) {
            break;
         }

         int msgWidth = Math.max(40, w - 20 - i * 18);
         context.fill(lineX, lineY - i * lineHeight, lineX + msgWidth, lineY - i * lineHeight + 9, alpha << 24);
      }
   }

   @Override
   public void onRender2D(DrawContext drawContext, float tickDelta) {
   }

   private static record StyledGlyph(int index, Style style, int codePoint, int charIndex) {
   }
}
