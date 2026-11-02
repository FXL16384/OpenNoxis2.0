package noxisclient.core.impl;

import com.mojang.blaze3d.systems.RenderSystem;
import java.awt.Color;
import java.awt.Font;
import java.io.InputStream;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.font.TextRenderer.TextLayerType;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.VertexConsumerProvider.Immediate;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import noxisclient.Vitality;
import noxisclient.mod.gui.fonts.FontRenderer;
import noxisclient.mod.modules.Module;
import noxisclient.mod.modules.impl.client.FontMode;
import noxisclient.mod.modules.impl.client.Fonts;
import org.joml.Matrix4f;
import org.lwjgl.glfw.GLFW;

public class FontManager {
   public static final FontManager INSTANCE = new FontManager();
   private static final String FONT_DIR = "assets/alienclient/font/";
   private static final String DEFAULT_FONT = "assets/alienclient/font/default.ttf";
   private static final String MISANS_FONT = "assets/alienclient/font/misans-regular.ttf";
   private static final String BOREL_FONT = "assets/alienclient/font/borel-regular.ttf";
   private static final String HOPRC_FONT = "assets/alienclient/font/hoprc.ttf";
   private static final String HYW_FONT = "assets/alienclient/font/hyw.ttf";
   private static final String CUSTOM_OTF_FONT = "assets/alienclient/font/font.otf";
   private static final ConcurrentHashMap<String, FontRenderer> sizedRenderers = new ConcurrentHashMap<>();
   private static final ConcurrentHashMap<String, Font> awtFontCache = new ConcurrentHashMap<>();
   public static FontRenderer ui;
   public static FontRenderer uiNameTags;
   public static FontRenderer small;
   public static FontRenderer icon;
   private FontRenderer renderer;

   public static void init() {
      if (canCreateTexture()) {
         INSTANCE.reload();
      }
   }

   public void reload() {
      clearSizedRenderers();
      FontRenderer next = this.createRenderer();
      FontRenderer old = this.renderer;
      this.renderer = next;
      ui = next;
      uiNameTags = next;
      small = next;
      icon = next;
      close(old);
   }

   public FontRenderer getRenderer() {
      if (this.renderer == null) {
         this.reload();
      }

      return this.renderer;
   }

   public static FontRenderer create(int size, String font, int style) {
      return new FontRenderer(new Font(font, style, size), (float)size);
   }

   public static FontRenderer create(int size, String font, int style, String ignoredAlternate) {
      return create(size, font, style);
   }

   public static FontRenderer createWithoutOffset(int size, String font, int style) {
      return create(size, font, style);
   }

   public static FontRenderer assets(float size, String font, int style) {
      return create((int)size, font, style);
   }

   public static FontRenderer assets(float size, String font, int style, String ignoredAlternate) {
      return create((int)size, font, style);
   }

   public static FontRenderer assets(float size, String font, int style, String ignoredAlternate, int ignoredAlternateStyle) {
      return create((int)size, font, style);
   }

   public static FontRenderer assetsWithoutOffset(float size, String font, int style) {
      return create((int)size, font, style);
   }

   public static void setUi(FontRenderer renderer) {
      INSTANCE.setRenderer(renderer);
   }

   public static void setUiNameTags(FontRenderer renderer) {
      INSTANCE.setRenderer(renderer);
   }

   public static void setSmall(FontRenderer renderer) {
      INSTANCE.setRenderer(renderer);
   }

   public static void setIcon(FontRenderer renderer) {
      INSTANCE.setRenderer(renderer);
   }

   public static FontRenderer nameTags() {
      return INSTANCE.getRenderer();
   }

   public static FontRenderer get() {
      return get(getSize());
   }

   public static FontRenderer get(float size) {
      float safeSize = Math.max(1.0F, size);
      Fonts fonts = Fonts.INSTANCE;
      if (fonts != null && Math.abs(safeSize - fonts.size.getValueFloat()) < 0.01F && INSTANCE.renderer != null) {
         return INSTANCE.getRenderer();
      } else {
         FontMode mode = getMode();
         int style = getStyle();
         String cacheKey = mode.name() + "_" + getFontName().toLowerCase(Locale.ROOT) + "_" + style + "_" + safeSize + "_" + getAntiAliasCacheKey();
         return sizedRenderers.computeIfAbsent(cacheKey, ignored -> createHudRenderer(mode, safeSize, style));
      }
   }

   public static FontRenderer getSmall() {
      return get(6.0F);
   }

   public static FontRenderer getArtistFont(float size) {
      String cacheKey = "artist_" + size + "_" + getAntiAliasCacheKey();
      return sizedRenderers.computeIfAbsent(cacheKey, ignored -> createAssetRenderer("assets/alienclient/font/borel-regular.ttf", Math.max(1.0F, size), 0));
   }

   public static boolean isCustom() {
      return Fonts.useHudFont();
   }

   public static FontMode getMode() {
      return !Fonts.isCustomFontEnabled() ? FontMode.MINECRAFT : resolveFontMode(getFontName());
   }

   public static float getSize() {
      return Fonts.INSTANCE == null ? 8.0F : Fonts.INSTANCE.size.getValueFloat();
   }

   public static float getWidth(String text) {
      String normalized = normalizeFormattingPrefixes(text);
      if (isCustom()) {
         return get().getWidth(normalized);
      } else {
         MinecraftClient mc = MinecraftClient.getInstance();
         return mc.textRenderer == null ? 0.0F : (float)mc.textRenderer.getWidth(normalized);
      }
   }

   public static float getFontHeight() {
      if (isCustom()) {
         return get().getFontHeight();
      } else {
         MinecraftClient mc = MinecraftClient.getInstance();
         return mc.textRenderer == null ? 9.0F : 9.0F;
      }
   }

   public static float getFontHeight(String text) {
      if (isCustom()) {
         return get().getFontHeight(text);
      } else {
         MinecraftClient mc = MinecraftClient.getInstance();
         return mc.textRenderer == null ? 9.0F : 9.0F;
      }
   }

   public static float getNameTagsWidth(String text) {
      if (Fonts.useNameTagsFont()) {
         return INSTANCE.getRenderer().getWidth(text);
      } else {
         MinecraftClient mc = MinecraftClient.getInstance();
         return mc.textRenderer == null ? 0.0F : (float)mc.textRenderer.getWidth(text);
      }
   }

   public static float getNameTagsHeight() {
      if (Fonts.useNameTagsFont()) {
         return INSTANCE.getRenderer().getFontHeight();
      } else {
         MinecraftClient mc = MinecraftClient.getInstance();
         return mc.textRenderer == null ? 9.0F : 9.0F;
      }
   }

   public static void drawNameTagsString(MatrixStack stack, String text, float x, float y, int color, boolean shadow) {
      INSTANCE.drawString(stack, text, x, y, color, shadow);
   }

   public static String normalizeFormattingPrefixes(String text) {
      return text == null ? "" : text.replace('搂', '§');
   }

   public static float getRenderWidth(String text) {
      String normalized = normalizeFormattingPrefixes(text);
      if (Fonts.useRenderFont()) {
         return INSTANCE.getRenderer().getWidth(normalized);
      } else {
         MinecraftClient mc = MinecraftClient.getInstance();
         return mc.textRenderer == null ? 0.0F : (float)mc.textRenderer.getWidth(normalized);
      }
   }

   public static float getRenderHeight() {
      if (Fonts.useRenderFont()) {
         return INSTANCE.getRenderer().getFontHeight();
      } else {
         MinecraftClient mc = MinecraftClient.getInstance();
         return mc.textRenderer == null ? 9.0F : 9.0F;
      }
   }

   public static void drawRenderString(MatrixStack matrices, String text, float x, float y, int color, boolean shadow) {
      String normalized = normalizeFormattingPrefixes(text);
      if (Fonts.useRenderFont()) {
         INSTANCE.drawString(matrices, normalized, x, y, color, shadow);
      } else {
         MinecraftClient mc = MinecraftClient.getInstance();
         if (mc.textRenderer != null) {
            drawVanillaString(matrices, mc.getBufferBuilders().getEntityVertexConsumers(), normalized, x, y, color, shadow);
         }
      }
   }

   public static void drawRenderStringSeeThrough(MatrixStack matrices, Immediate immediate, String text, float x, float y, int color, boolean shadow) {
      String normalized = normalizeFormattingPrefixes(text);
      if (Fonts.useRenderFont()) {
         INSTANCE.drawString(matrices, normalized, x, y, color, shadow);
      } else {
         MinecraftClient mc = MinecraftClient.getInstance();
         if (mc.textRenderer != null) {
            drawVanillaString(matrices, immediate, normalized, x, y, color, shadow);
         }
      }
   }

   public float width(String text) {
      if (Fonts.useRenderFont()) {
         return this.getRenderer().getWidth(text);
      } else {
         MinecraftClient mc = MinecraftClient.getInstance();
         return mc.textRenderer == null ? 0.0F : (float)mc.textRenderer.getWidth(text);
      }
   }

   public float height() {
      if (Fonts.useRenderFont()) {
         return this.getRenderer().getFontHeight();
      } else {
         MinecraftClient mc = MinecraftClient.getInstance();
         return mc.textRenderer == null ? 9.0F : 9.0F;
      }
   }

   public void drawString(DrawContext context, String text, float x, float y, int color, boolean shadow) {
      if (Fonts.useRenderFont()) {
         this.drawString(context.getMatrices(), text, x, y, color, shadow);
      } else {
         context.drawText(MinecraftClient.getInstance().textRenderer, text, (int)x, (int)y, color, shadow);
      }
   }

   public void drawString(MatrixStack stack, String text, float x, float y, int color, boolean shadow) {
      this.getRenderer().drawString(stack, text, x, y, color, shadow);
   }

   public void drawString(DrawContext context, Text text, float x, float y, Color color, boolean shadow) {
      this.drawString(context, text.getString(), x, y, color.getRGB(), shadow);
   }

   public void flush() {
      if (this.renderer != null) {
         this.renderer.flush();
      }
   }

   private void setRenderer(FontRenderer renderer) {
      FontRenderer old = this.renderer;
      this.renderer = renderer;
      ui = renderer;
      uiNameTags = renderer;
      small = renderer;
      icon = renderer;
      close(old);
   }

   private FontRenderer createRenderer() {
      Fonts fonts = Fonts.INSTANCE;
      String name = fonts == null ? "msyh" : fonts.fontName.getValue();
      int size = fonts == null ? 8 : fonts.size.getValueInt();
      int style = fonts == null ? 0 : fonts.style.getValue().awtStyle;
      return createHudRenderer(resolveFontMode(name), (float)size, style);
   }

   private static FontRenderer createHudRenderer(FontMode mode, float size, int style) {
      String prebakeCharacters = getModuleNamePrebakeCharacters();

      return switch (mode) {
         case SYSTEM -> new FontRenderer(new Font(getFontName(), style, Math.max(1, Math.round(size))), size, prebakeCharacters);
         default -> createAssetRenderer(mode.getResourcePath(), size, style, prebakeCharacters);
      };
   }

   private static FontRenderer createAssetRenderer(String resource, float size, int style) {
      return createAssetRenderer(resource, size, style, "");
   }

   private static FontRenderer createAssetRenderer(String resource, float size, int style, String prebakeCharacters) {
      try {
         Font font = awtFontCache.computeIfAbsent(resource, key -> {
            try {
               Font var2;
               try (InputStream stream = FontManager.class.getClassLoader().getResourceAsStream(key)) {
                  if (stream == null) {
                     return null;
                  }

                  var2 = Font.createFont(0, stream);
               }

               return var2;
            } catch (Exception var6) {
               return null;
            }
         });
         if (font != null) {
            return new FontRenderer(font.deriveFont(style, size), size, prebakeCharacters);
         }
      } catch (Exception var5) {
      }

      return new FontRenderer(new Font(getFontName(), style, Math.max(1, Math.round(size))), size, prebakeCharacters);
   }

   private static String getModuleNamePrebakeCharacters() {
      if (Vitality.MODULE == null) {
         return "";
      } else {
         StringBuilder characters = new StringBuilder();

         for (Module module : Vitality.MODULE.getModules()) {
            if (module.getChinese() != null) {
               characters.append(module.getChinese());
            }
         }

         return characters.append("开启关闭客户端设置字体界面").toString();
      }
   }

   private static String getFontName() {
      return Fonts.INSTANCE == null ? "msyh" : Fonts.INSTANCE.fontName.getValue();
   }

   private static FontMode resolveFontMode(String fontName) {
      String name = fontName == null ? "" : fontName.toLowerCase(Locale.ROOT);
      if (name.contains("shoreline") || name.contains("verdana")) {
         return FontMode.SHORELINE;
      } else if (name.contains("misans")) {
         return FontMode.MISANS;
      } else if (name.contains("borel")) {
         return FontMode.BOREL;
      } else if (name.contains("hoprc")) {
         return FontMode.HOPRC;
      } else if (name.contains("hyw")) {
         return FontMode.HYW;
      } else if (name.contains("default")) {
         return FontMode.DEFAULT;
      } else {
         return !name.contains("font.otf") && !name.contains("custom") ? FontMode.SYSTEM : FontMode.CUSTOM_OTF;
      }
   }

   private static int getStyle() {
      return Fonts.INSTANCE == null ? 0 : Fonts.INSTANCE.style.getValue().awtStyle;
   }

   private static String getAntiAliasCacheKey() {
      Fonts fonts = Fonts.INSTANCE;
      return fonts == null ? "default" : fonts.smooth.getValue() + "_" + fonts.antiAliasMode.getValue().name();
   }

   public static String getConfiguredFontName() {
      return getFontName();
   }

   public static int getConfiguredFontStyle() {
      return getStyle();
   }

   private static boolean canCreateTexture() {
      try {
         MinecraftClient mc = MinecraftClient.getInstance();
         return mc != null && mc.getWindow() != null && RenderSystem.isOnRenderThread() && GLFW.glfwGetCurrentContext() != 0L;
      } catch (Throwable var1) {
         return false;
      }
   }

   private static void drawVanillaString(MatrixStack matrices, Immediate immediate, String text, float x, float y, int color, boolean shadow) {
      MinecraftClient mc = MinecraftClient.getInstance();
      TextRenderer textRenderer = mc.textRenderer;
      if (textRenderer != null) {
         Matrix4f matrix = matrices.peek().getPositionMatrix();
         if (shadow) {
            float shadowX = x + 0.5F;
            float shadowY = y + 0.5F;
            int currentColor = color;
            StringBuilder segment = new StringBuilder();

            for (int i = 0; i < text.length(); i++) {
               char c = text.charAt(i);
               if (c == 167 && i + 1 < text.length()) {
                  char code = text.charAt(i + 1);
                  Formatting formatting = Formatting.byCode(code);
                  if (formatting != null && formatting.isColor()) {
                     if (!segment.isEmpty()) {
                        int shadowColor = (currentColor & 16579836) >> 2 | currentColor & 0xFF000000;
                        textRenderer.draw(segment.toString(), shadowX, shadowY, shadowColor, false, matrix, immediate, TextLayerType.SEE_THROUGH, 0, 15728880);
                        shadowX += (float)textRenderer.getWidth(segment.toString());
                        segment = new StringBuilder();
                     }

                     Integer rgb = formatting.getColorValue();
                     if (rgb != null) {
                        currentColor = 0xFF000000 | rgb;
                     }
                  } else if (formatting == Formatting.RESET) {
                     if (!segment.isEmpty()) {
                        int shadowColor = (currentColor & 16579836) >> 2 | currentColor & 0xFF000000;
                        textRenderer.draw(segment.toString(), shadowX, shadowY, shadowColor, false, matrix, immediate, TextLayerType.SEE_THROUGH, 0, 15728880);
                        shadowX += (float)textRenderer.getWidth(segment.toString());
                        segment = new StringBuilder();
                     }

                     currentColor = color;
                  } else {
                     segment.append(c);
                     segment.append(code);
                  }

                  i++;
               } else {
                  segment.append(c);
               }
            }

            if (!segment.isEmpty()) {
               int shadowColor = (currentColor & 16579836) >> 2 | currentColor & 0xFF000000;
               textRenderer.draw(segment.toString(), shadowX, shadowY, shadowColor, false, matrix, immediate, TextLayerType.SEE_THROUGH, 0, 15728880);
            }

            immediate.draw();
         }

         textRenderer.draw(text, x, y, color, false, matrix, immediate, TextLayerType.SEE_THROUGH, 0, 15728880);
         immediate.draw();
      }
   }

   private static void close(FontRenderer renderer) {
      if (renderer != null && renderer != ui && renderer != uiNameTags && renderer != small && renderer != icon) {
         try {
            renderer.close();
         } catch (Exception var2) {
         }
      }
   }

   private static void clearSizedRenderers() {
      for (FontRenderer renderer : sizedRenderers.values()) {
         close(renderer);
      }

      sizedRenderers.clear();
   }
}
