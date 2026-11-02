package noxisclient.api.utils.skija;

import com.mojang.blaze3d.systems.RenderSystem;
import java.awt.Color;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import noxisclient.core.impl.FontManager;
import noxisclient.mod.gui.fonts.FontRenderer;
import noxisclient.mod.modules.impl.client.FontMode;
import org.jetbrains.skija.Canvas;
import org.jetbrains.skija.Data;
import org.jetbrains.skija.EncodedImageFormat;
import org.jetbrains.skija.FilterTileMode;
import org.jetbrains.skija.Font;
import org.jetbrains.skija.FontEdging;
import org.jetbrains.skija.FontMetrics;
import org.jetbrains.skija.FontStyle;
import org.jetbrains.skija.Image;
import org.jetbrains.skija.ImageFilter;
import org.jetbrains.skija.Paint;
import org.jetbrains.skija.Path;
import org.jetbrains.skija.Shader;
import org.jetbrains.skija.Surface;
import org.jetbrains.skija.Typeface;

public class SkijaGlowRenderer {
   private static final MinecraftClient mc = MinecraftClient.getInstance();
   private static final Map<String, Typeface> typefaceCache = new HashMap<>();
   private static final Map<String, Identifier> textureCache = new HashMap<>();
   private static final Set<Identifier> readyTextures = new HashSet<>();
   private static final Map<Identifier, int[]> textureSizes = new HashMap<>();

   private static Typeface getTypeface(String forceFontFile) {
      String cacheKey = getFontCacheKey(forceFontFile);
      return typefaceCache.computeIfAbsent(cacheKey, ignored -> loadTypeface(forceFontFile));
   }

   private static Typeface loadTypeface(String forceFontFile) {
      FontMode mode = FontManager.getMode();
      if (forceFontFile == null && mode == FontMode.SYSTEM) {
         Typeface typeface = Typeface.makeFromName(FontManager.getConfiguredFontName(), getSkijaFontStyle(FontManager.getConfiguredFontStyle()));
         return typeface != null ? typeface : Typeface.makeDefault();
      } else {
         String resourcePath = forceFontFile != null ? "assets/alienclient/font/" + forceFontFile : mode.getResourcePath();

         try {
            Typeface var7;
            try (InputStream is = SkijaGlowRenderer.class.getClassLoader().getResourceAsStream(resourcePath)) {
               if (is == null) {
                  return Typeface.makeDefault();
               }

               byte[] fontData = is.readAllBytes();

               try (Data data = Data.makeFromBytes(fontData)) {
                  Typeface typeface = Typeface.makeFromData(data);
                  var7 = typeface != null ? typeface : Typeface.makeDefault();
               }
            }

            return var7;
         } catch (Exception var12) {
            return Typeface.makeDefault();
         }
      }
   }

   public static String getFontCacheKey(String forceFontFile) {
      if (forceFontFile != null) {
         return "asset:" + forceFontFile;
      } else {
         FontMode mode = FontManager.getMode();
         return mode == FontMode.SYSTEM
            ? "system:" + FontManager.getConfiguredFontName().toLowerCase(Locale.ROOT) + ":" + FontManager.getConfiguredFontStyle()
            : "asset:" + mode.getResourcePath();
      }
   }

   private static FontStyle getSkijaFontStyle(int awtStyle) {
      return switch (awtStyle) {
         case 1 -> FontStyle.BOLD;
         case 2 -> FontStyle.ITALIC;
         case 3 -> FontStyle.BOLD_ITALIC;
         default -> FontStyle.NORMAL;
      };
   }

   public void renderGlowLayer(
      DrawContext context, List<SkijaGlowRenderer.TextEntry> entries, float fontSize, float fontHeight, float glowRadius, float glowAlpha
   ) {
      if (context != null && entries != null && !entries.isEmpty()) {
         double scale = mc.getWindow().getScaleFactor();
         float padding = Math.max(glowRadius * 3.0F, (float)(15.0 / scale));
         float alphaMultiplier = Math.max(0.0F, Math.min(1.0F, glowAlpha));
         RenderSystem.enableBlend();
         RenderSystem.defaultBlendFunc();

         for (SkijaGlowRenderer.TextEntry entry : entries) {
            if (entry != null && entry.text != null && !entry.text.isEmpty()) {
               Color baseColor = new Color(entry.color, true);
               int glowA = Math.max(0, Math.min(255, Math.round((float)baseColor.getAlpha() * alphaMultiplier)));
               if (glowA > 0) {
                  Color transparentText = new Color(baseColor.getRed(), baseColor.getGreen(), baseColor.getBlue(), 0);
                  Color glowColor = new Color(baseColor.getRed(), baseColor.getGreen(), baseColor.getBlue(), glowA);
                  Identifier texture = createGlowTexture(entry.text, fontSize, transparentText, glowColor, glowRadius, false, null);
                  if (isTextureReady(texture)) {
                     int[] size = getTextureSize(texture);
                     if (size != null && size.length >= 2 && size[0] > 0 && size[1] > 0) {
                        int drawX = Math.round(entry.x - padding);
                        int drawY = Math.round(entry.y - padding);
                        int drawWidth = Math.max(1, (int)Math.ceil((double)size[0] / scale));
                        int drawHeight = Math.max(1, (int)Math.ceil((double)size[1] / scale));
                        context.drawTexture(texture, drawX, drawY, drawWidth, drawHeight, 0.0F, 0.0F, size[0], size[1], size[0], size[1]);
                     }
                  }
               }
            }
         }

         RenderSystem.disableBlend();
      }
   }

   public static Identifier createGlowTexture(
      String text, float fontSize, Color textColor, Color glowColor, float glowRadius, boolean useGradient, Color gradientEndColor
   ) {
      return createGlowTexture(text, fontSize, textColor, glowColor, glowRadius, useGradient, gradientEndColor, null);
   }

   public static Identifier createGlowTexture(
      String text, float fontSize, Color textColor, Color glowColor, float glowRadius, boolean useGradient, Color gradientEndColor, String forceFontFile
   ) {
      double scale = mc.getWindow().getScaleFactor();
      float scaledFontSize = (float)((double)fontSize * scale);
      float scaledGlowRadius = (float)((double)glowRadius * scale);
      String fontKey = getFontCacheKey(forceFontFile);
      String cacheKey = String.format(
         "%s_%s_%.1f_%08x_%08x_%.1f_%b_%s",
         text,
         fontKey,
         scaledFontSize,
         textColor.getRGB(),
         glowColor.getRGB(),
         scaledGlowRadius,
         useGradient,
         gradientEndColor != null ? Integer.toHexString(gradientEndColor.getRGB()) : "null"
      );
      if (textureCache.containsKey(cacheKey)) {
         return textureCache.get(cacheKey);
      } else {
         try {
            Typeface typeface = getTypeface(forceFontFile);
            Font font = new Font(typeface != null ? typeface : Typeface.makeDefault(), scaledFontSize);
            font.setEdging(FontEdging.SUBPIXEL_ANTI_ALIAS);
            font.setSubpixel(true);
            FontMetrics metrics = font.getMetrics();
            List<SkijaGlowRenderer.FormattedTextSegment> textSegments = getFormattedTextSegments(text, textColor);
            List<SkijaGlowRenderer.FormattedTextSegment> glowSegments = getFormattedTextSegments(text, glowColor);
            float textWidth = measureTextWidth(font, textSegments);
            if (textWidth <= 0.0F) {
               font.close();
               return null;
            } else {
               float textHeight = metrics.getDescent() - metrics.getAscent();
               float padding = Math.max(scaledGlowRadius * 3.0F, 15.0F);
               int width = (int)Math.ceil((double)(textWidth + padding * 2.0F));
               int height = (int)Math.ceil((double)(textHeight + padding * 2.0F));

               Identifier var32;
               try (
                  Surface surface = Surface.makeRasterN32Premul(width, height);
                  Paint paint = new Paint();
               ) {
                  Canvas canvas = surface.getCanvas();
                  canvas.clear(0);
                  float x = padding;
                  float y = padding - metrics.getAscent();
                  paint.setAntiAlias(true);
                  if (scaledGlowRadius > 0.0F) {
                     try (ImageFilter blur = ImageFilter.makeBlur(scaledGlowRadius, scaledGlowRadius, FilterTileMode.CLAMP)) {
                        paint.setImageFilter(blur);
                        drawTextSegments(canvas, glowSegments, x, y, font, paint);
                     }
                  }

                  paint.setImageFilter(null);
                  if (textColor.getAlpha() > 0) {
                     if (useGradient && gradientEndColor != null && !hasFormattingCodes(text)) {
                        int[] colors = new int[]{colorToSkija(textColor), colorToSkija(gradientEndColor)};
                        paint.setShader(Shader.makeLinearGradient(padding, y, padding + textWidth, y, colors));
                        canvas.drawString(text, padding, y, font, paint);
                     } else {
                        drawTextSegments(canvas, textSegments, padding, y, font, paint);
                     }
                  }

                  NativeImage nativeImage;
                  try (
                     Image image = surface.makeImageSnapshot();
                     Data data = image.encodeToData(EncodedImageFormat.PNG);
                  ) {
                     nativeImage = NativeImage.read(data.toByteBuffer());
                  }

                  if (nativeImage == null) {
                     return null;
                  }

                  Identifier textureId = Identifier.of("alienclient", "glow_" + UUID.randomUUID().toString().substring(0, 8));
                  NativeImageBackedTexture texture = new NativeImageBackedTexture(nativeImage);
                  textureSizes.put(textureId, new int[]{width, height});
                  if (RenderSystem.isOnRenderThread()) {
                     mc.getTextureManager().registerTexture(textureId, texture);
                     RenderSystem.bindTexture(texture.getGlId());
                     RenderSystem.texParameter(3553, 10241, 9729);
                     RenderSystem.texParameter(3553, 10240, 9729);
                     readyTextures.add(textureId);
                  } else {
                     RenderSystem.recordRenderCall(() -> {
                        mc.getTextureManager().registerTexture(textureId, texture);
                        RenderSystem.bindTexture(texture.getGlId());
                        RenderSystem.texParameter(3553, 10241, 9729);
                        RenderSystem.texParameter(3553, 10240, 9729);
                        readyTextures.add(textureId);
                     });
                  }

                  textureCache.put(cacheKey, textureId);
                  font.close();
                  var32 = textureId;
               }

               return var32;
            }
         } catch (Exception var44) {
            var44.printStackTrace();
            return null;
         }
      }
   }

   private static List<SkijaGlowRenderer.FormattedTextSegment> getFormattedTextSegments(String text, Color baseColor) {
      ArrayList<SkijaGlowRenderer.FormattedTextSegment> segments = new ArrayList<>();
      String normalized = FontManager.normalizeFormattingPrefixes(text);
      StringBuilder currentText = new StringBuilder();
      Color currentColor = baseColor;

      for (int i = 0; i < normalized.length(); i++) {
         char current = normalized.charAt(i);
         if (current == 167 && i + 1 < normalized.length()) {
            Formatting formatting = Formatting.byCode(normalized.charAt(i + 1));
            if (formatting != null) {
               if (formatting.isColor() || formatting == Formatting.RESET) {
                  flushTextSegment(segments, currentText, currentColor);
                  currentColor = formatting == Formatting.RESET ? baseColor : withAlpha(formatting.getColorValue(), baseColor.getAlpha());
               }

               i++;
               continue;
            }
         }

         currentText.append(current);
      }

      flushTextSegment(segments, currentText, currentColor);
      return segments;
   }

   private static void flushTextSegment(ArrayList<SkijaGlowRenderer.FormattedTextSegment> segments, StringBuilder text, Color color) {
      if (text.length() != 0) {
         segments.add(new SkijaGlowRenderer.FormattedTextSegment(text.toString(), color));
         text.setLength(0);
      }
   }

   private static float measureTextWidth(Font font, List<SkijaGlowRenderer.FormattedTextSegment> segments) {
      float width = 0.0F;

      for (SkijaGlowRenderer.FormattedTextSegment segment : segments) {
         width += font.measureTextWidth(segment.text);
      }

      return width;
   }

   private static void drawTextSegments(Canvas canvas, List<SkijaGlowRenderer.FormattedTextSegment> segments, float x, float y, Font font, Paint paint) {
      float currentX = x;
      paint.setShader(null);

      for (SkijaGlowRenderer.FormattedTextSegment segment : segments) {
         paint.setColor(colorToSkija(segment.color));
         canvas.drawString(segment.text, currentX, y, font, paint);
         currentX += font.measureTextWidth(segment.text);
      }
   }

   private static boolean hasFormattingCodes(String text) {
      String normalized = FontManager.normalizeFormattingPrefixes(text);

      for (int i = 0; i < normalized.length() - 1; i++) {
         if (normalized.charAt(i) == 167 && Formatting.byCode(normalized.charAt(i + 1)) != null) {
            return true;
         }
      }

      return false;
   }

   private static Color withAlpha(Integer rgb, int alpha) {
      return rgb == null ? new Color(255, 255, 255, alpha) : new Color(alpha << 24 | rgb & 16777215, true);
   }

   public static Identifier createGlowDiamondTexture(float size, Color innerColor, Color glowColor, float glowRadius) {
      double scale = mc.getWindow().getScaleFactor();
      float scaledSize = (float)((double)size * scale);
      float scaledGlowRadius = (float)((double)glowRadius * scale);
      scaledSize = Math.max(2.0F, Math.min(128.0F, scaledSize));
      scaledGlowRadius = Math.max(0.0F, Math.min(48.0F, scaledGlowRadius));
      String cacheKey = String.format("diamond_%.2f_%08x_%08x_%.2f", scaledSize, innerColor.getRGB(), glowColor.getRGB(), scaledGlowRadius);
      if (textureCache.containsKey(cacheKey)) {
         return textureCache.get(cacheKey);
      } else {
         try {
            float padding = Math.max(scaledGlowRadius * 1.6F, 6.0F);
            int texSize = (int)Math.ceil((double)(scaledSize * 2.0F + padding * 2.0F));
            texSize = Math.max(8, Math.min(texSize, 1024));

            Identifier var23;
            try (
               Surface surface = Surface.makeRasterN32Premul(texSize, texSize);
               Paint paint = new Paint();
               Path glowPath = new Path();
               Path innerPath = new Path();
            ) {
               Canvas canvas = surface.getCanvas();
               canvas.clear(0);
               float cx = (float)texSize / 2.0F;
               float cy = (float)texSize / 2.0F;
               float outer = Math.min(scaledSize, (float)texSize / 2.0F - 2.0F);
               float inner = outer * 0.72F;
               glowPath.moveTo(cx, cy - outer);
               glowPath.lineTo(cx + outer, cy);
               glowPath.lineTo(cx, cy + outer);
               glowPath.lineTo(cx - outer, cy);
               glowPath.closePath();
               innerPath.moveTo(cx, cy - inner);
               innerPath.lineTo(cx + inner, cy);
               innerPath.lineTo(cx, cy + inner);
               innerPath.lineTo(cx - inner, cy);
               innerPath.closePath();
               paint.setAntiAlias(true);
               if (scaledGlowRadius > 0.0F) {
                  try (ImageFilter blur = ImageFilter.makeBlur(scaledGlowRadius, scaledGlowRadius, FilterTileMode.CLAMP)) {
                     paint.setImageFilter(blur);
                     paint.setShader(null);
                     paint.setColor(colorToSkija(glowColor));
                     canvas.drawPath(glowPath, paint);
                  }
               }

               paint.setImageFilter(null);
               paint.setShader(null);
               paint.setColor(colorToSkija(innerColor));
               canvas.drawPath(innerPath, paint);

               NativeImage nativeImage;
               try (
                  Image image = surface.makeImageSnapshot();
                  Data data = image.encodeToData(EncodedImageFormat.PNG);
               ) {
                  nativeImage = NativeImage.read(data.toByteBuffer());
               }

               if (nativeImage == null) {
                  return null;
               }

               Identifier textureId = Identifier.of("alienclient", "glow_diamond_" + UUID.randomUUID().toString().substring(0, 8));
               NativeImageBackedTexture texture = new NativeImageBackedTexture(nativeImage);
               textureSizes.put(textureId, new int[]{texSize, texSize});
               if (RenderSystem.isOnRenderThread()) {
                  mc.getTextureManager().registerTexture(textureId, texture);
                  RenderSystem.bindTexture(texture.getGlId());
                  RenderSystem.texParameter(3553, 10241, 9729);
                  RenderSystem.texParameter(3553, 10240, 9729);
                  readyTextures.add(textureId);
               } else {
                  RenderSystem.recordRenderCall(() -> {
                     mc.getTextureManager().registerTexture(textureId, texture);
                     RenderSystem.bindTexture(texture.getGlId());
                     RenderSystem.texParameter(3553, 10241, 9729);
                     RenderSystem.texParameter(3553, 10240, 9729);
                     readyTextures.add(textureId);
                  });
               }

               textureCache.put(cacheKey, textureId);
               var23 = textureId;
            }

            return var23;
         } catch (Throwable var39) {
            var39.printStackTrace();
            return null;
         }
      }
   }

   public static boolean isTextureReady(Identifier id) {
      return id != null && readyTextures.contains(id);
   }

   public static int[] getTextureSize(Identifier id) {
      return textureSizes.get(id);
   }

   public static void renderSimpleGlow(
      MatrixStack matrices, String text, float x, float y, Color color, float scale, float glowRadius, int layers, boolean shadow
   ) {
      FontRenderer font = FontManager.ui;
      if (font != null) {
         for (int i = layers; i > 0; i--) {
            float offset = (float)i * 0.5F * scale;
            int alpha = color.getAlpha() / (i + 1);
            Color glowColor = new Color(color.getRed(), color.getGreen(), color.getBlue(), Math.min(255, alpha));
            font.drawString(
               matrices,
               text,
               x - offset,
               y - offset,
               (float)glowColor.getRed() / 255.0F,
               (float)glowColor.getGreen() / 255.0F,
               (float)glowColor.getBlue() / 255.0F,
               (float)glowColor.getAlpha() / 255.0F,
               shadow
            );
            font.drawString(
               matrices,
               text,
               x + offset,
               y - offset,
               (float)glowColor.getRed() / 255.0F,
               (float)glowColor.getGreen() / 255.0F,
               (float)glowColor.getBlue() / 255.0F,
               (float)glowColor.getAlpha() / 255.0F,
               shadow
            );
            font.drawString(
               matrices,
               text,
               x - offset,
               y + offset,
               (float)glowColor.getRed() / 255.0F,
               (float)glowColor.getGreen() / 255.0F,
               (float)glowColor.getBlue() / 255.0F,
               (float)glowColor.getAlpha() / 255.0F,
               shadow
            );
            font.drawString(
               matrices,
               text,
               x + offset,
               y + offset,
               (float)glowColor.getRed() / 255.0F,
               (float)glowColor.getGreen() / 255.0F,
               (float)glowColor.getBlue() / 255.0F,
               (float)glowColor.getAlpha() / 255.0F,
               shadow
            );
         }

         font.drawString(
            matrices,
            text,
            x,
            y,
            (float)color.getRed() / 255.0F,
            (float)color.getGreen() / 255.0F,
            (float)color.getBlue() / 255.0F,
            (float)color.getAlpha() / 255.0F,
            shadow
         );
      }
   }

   private static int colorToSkija(Color c) {
      return c.getAlpha() << 24 | c.getRed() << 16 | c.getGreen() << 8 | c.getBlue();
   }

   public static void cleanupTexture(Identifier id) {
      if (id != null) {
         readyTextures.remove(id);
         textureSizes.remove(id);
         RenderSystem.recordRenderCall(() -> {
            if (mc.getTextureManager() != null) {
               mc.getTextureManager().destroyTexture(id);
            }
         });
         textureCache.entrySet().removeIf(e -> e.getValue().equals(id));
      }
   }

   public static void clearAllTextures() {
      for (Identifier id : textureCache.values()) {
         readyTextures.remove(id);
         textureSizes.remove(id);
         RenderSystem.recordRenderCall(() -> {
            if (mc.getTextureManager() != null) {
               mc.getTextureManager().destroyTexture(id);
            }
         });
      }

      textureCache.clear();
      readyTextures.clear();
      textureSizes.clear();
   }

   private static class FormattedTextSegment {
      final String text;
      final Color color;

      FormattedTextSegment(String text, Color color) {
         this.text = text;
         this.color = color;
      }
   }

   public static class TextEntry {
      public String text = "";
      public float x;
      public float y;
      public float width;
      public int color;

      public SkijaGlowRenderer.TextEntry set(String text, float x, float y, float width, int color) {
         this.text = text;
         this.x = x;
         this.y = y;
         this.width = width;
         this.color = color;
         return this;
      }
   }
}
