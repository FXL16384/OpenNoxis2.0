package noxisclient.mod.gui.fonts;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.font.FontRenderContext;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.Closeable;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.function.Function;
import net.minecraft.client.font.GlyphRenderer;
import net.minecraft.client.font.RenderableGlyph;
import net.minecraft.client.font.TextHandler;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.BuiltBuffer;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.render.VertexFormat.DrawMode;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.client.texture.NativeImage.Format;
import net.minecraft.client.util.BufferAllocator;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.CharacterVisitor;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Style;
import net.minecraft.text.TextColor;
import noxisclient.mod.modules.impl.client.Fonts;

public class FontRenderer implements net.minecraft.client.font.Glyph, Closeable {
   private static final FontRenderer.BufferedVertexConsumer TEXT_BUFFER = new FontRenderer.BufferedVertexConsumer(786432);
   private static final FontRenderer.BufferedVertexConsumer LAYER_BUFFER = new FontRenderer.BufferedVertexConsumer(786432);
   private static RenderLayer currentRenderLayer;
   private static final VertexConsumerProvider LAYER_PROVIDER = renderLayer -> {
      if (currentRenderLayer != null && currentRenderLayer != renderLayer) {
         flushLayerBuffer();
      }

      if (!LAYER_BUFFER.isBuilding()) {
         LAYER_BUFFER.begin(renderLayer.getDrawMode(), renderLayer.getVertexFormat());
      }

      currentRenderLayer = renderLayer;
      return LAYER_BUFFER;
   };
   private static final int[] COLOR_CODES = new int[32];
   private static final char[] DEFAULT_PREBAKE_CHARS = " !\"#$%&'()*+,-./0123456789:;<=>?@ABCDEFGHIJKLMNOPQRSTUVWXYZ[\\]^_`abcdefghijklmnopqrstuvwxyz{|}~§∞"
      .toCharArray();
   private static final float HACHIMI_RASTER_SCALE = 2.0F;
   private static int zOffset;
   private final FontRenderer.FontAtlas atlas;
   private final TextHandler textHandler;
   private float drawX;
   private float drawY;

   public FontRenderer(Font font, float sizePx) {
      this(createAtlas(font, sizePx));
   }

   public FontRenderer(Font font, float sizePx, String prebakeCharacters) {
      this(createAtlas(font, sizePx, prebakeCharacters));
   }

   public FontRenderer(Font font, Font ignoredSecondFont, float sizePx) {
      this(font, sizePx);
   }

   public FontRenderer(Font font, float sizePx, int ignoredCharactersPerPage, int ignoredPaddingBetweenCharacters, String ignoredPrebakeCharacters) {
      this(font, sizePx);
   }

   public FontRenderer(
      Font font, Font ignoredSecondFont, float sizePx, int ignoredCharactersPerPage, int ignoredPaddingBetweenCharacters, String ignoredPrebakeCharacters
   ) {
      this(font, sizePx);
   }

   private FontRenderer(FontRenderer.FontAtlas atlas) {
      this.atlas = atlas;
      this.textHandler = new TextHandler((character, style) -> this.getAdvance(Character.toString(character).charAt(0)));
   }

   private static FontRenderer.FontAtlas createAtlas(Font font, float sizePx) {
      return createAtlas(font, sizePx, null);
   }

   private static FontRenderer.FontAtlas createAtlas(Font font, float sizePx, String prebakeCharacters) {
      boolean smooth = smooth();
      boolean hachimi = useHachimiAntiAlias();
      float rasterScale = hachimi ? 2.0F : 1.0F;
      FontRenderer.FontAtlas atlas = new FontRenderer.FontAtlas(font.deriveFont(sizePx * rasterScale), true, true, smooth, hachimi, rasterScale);
      if (prebakeCharacters != null && !prebakeCharacters.isEmpty()) {
         char[] additionalCharacters = prebakeCharacters.toCharArray();
         char[] allCharacters = new char[DEFAULT_PREBAKE_CHARS.length + additionalCharacters.length];
         System.arraycopy(DEFAULT_PREBAKE_CHARS, 0, allCharacters, 0, DEFAULT_PREBAKE_CHARS.length);
         System.arraycopy(additionalCharacters, 0, allCharacters, DEFAULT_PREBAKE_CHARS.length, additionalCharacters.length);
         atlas.generate(allCharacters);
      } else {
         atlas.generate(DEFAULT_PREBAKE_CHARS);
      }

      return atlas;
   }

   public int drawString(MatrixStack stack, String text, float x, float y, int color) {
      return this.drawString(stack, text, x, y, color, true);
   }

   public int drawString(MatrixStack stack, String text, double x, double y, int color) {
      return this.drawString(stack, text, (float)x, (float)y, color, true);
   }

   public void drawString(MatrixStack stack, String text, double x, double y, int color, boolean shadow) {
      this.drawString(stack, text, (float)x, (float)y, color, shadow);
   }

   public void drawString(MatrixStack stack, String text, double x, double y, Color color) {
      this.drawString(stack, text, (float)x, (float)y, color.getRGB(), false);
   }

   public void drawString(MatrixStack stack, String text, double x, double y, Color color, boolean shadow) {
      this.drawString(stack, text, (float)x, (float)y, color.getRGB(), shadow);
   }

   public void drawString(MatrixStack stack, String text, float x, float y, float r, float g, float b, float a) {
      this.drawString(stack, text, x, y, rgbaToInt(r, g, b, a), false);
   }

   public void drawString(MatrixStack stack, String text, float x, float y, float r, float g, float b, float a, boolean shadow) {
      this.drawString(stack, text, x, y, rgbaToInt(r, g, b, a), shadow);
   }

   public int drawString(MatrixStack stack, String text, float x, float y, int color, boolean shadow) {
      int width;
      if (shadow) {
         width = this.drawRaw(stack, text, x + shadowOffset(), y + shadowOffset(), color, true);
         width = Math.max(width, this.drawRaw(stack, text, x, y, color, false));
      } else {
         width = this.drawRaw(stack, text, x, y, color, false);
      }

      this.flush();
      return width;
   }

   public Font getFont() {
      return this.atlas.font;
   }

   public void drawStringWithShadow(MatrixStack stack, String text, double x, double y, int color) {
      this.drawString(stack, text, (float)x, (float)y, color, true);
   }

   public void drawStringWithShadow(MatrixStack stack, String text, double x, double y, Color color) {
      this.drawString(stack, text, (float)x, (float)y, color.getRGB(), true);
   }

   public void drawStringWithShadow(MatrixStack stack, String text, float x, float y, float r, float g, float b, float a) {
      this.drawString(stack, text, x, y, rgbaToInt(r, g, b, a), true);
   }

   public void drawCenteredString(MatrixStack stack, String text, double x, double y, int color) {
      this.drawString(stack, text, (float)(x - (double)(this.getWidth(text) / 2.0F)), (float)y, color, true);
   }

   public void drawCenteredString(MatrixStack stack, String text, double x, double y, Color color) {
      this.drawCenteredString(stack, text, x, y, color.getRGB());
   }

   public void drawGradientString(MatrixStack stack, String text, float x, float y) {
      this.drawString(stack, text, x, y, -1, false);
   }

   public void drawGradientCenteredString(MatrixStack stack, String text, float x, float y) {
      this.drawGradientString(stack, text, x - this.getWidth(text) / 2.0F, y);
   }

   public void drawStringWithGlow(MatrixStack stack, String text, float x, float y, int textColor, int glowColor, float intensity) {
      this.drawStringWithGlow(stack, text, x, y, textColor, glowColor, intensity, false);
   }

   public void drawStringWithGlow(MatrixStack stack, String text, float x, float y, int textColor, int glowColor, float intensity, boolean shadow) {
      if (shadow) {
         int shadowColor = (textColor & 16579836) >> 2 | textColor & 0xFF000000;
         this.drawString(stack, text, x + shadowOffset(), y + shadowOffset(), shadowColor, false);
      }

      int layers = 3;

      for (int i = layers; i > 0; i--) {
         float offset = (float)i * intensity * 0.5F;
         int alpha = (int)((float)(glowColor >>> 24 & 0xFF) * (1.0F - (float)i / (float)(layers + 1)) * 0.6F);
         if (alpha > 2) {
            int color = glowColor & 16777215 | alpha << 24;

            for (int j = 0; j < 8; j++) {
               double angle = (Math.PI / 4) * (double)j;
               this.drawString(stack, text, x + (float)Math.cos(angle) * offset, y + (float)Math.sin(angle) * offset, color, false);
            }
         }
      }

      this.drawString(stack, text, x, y, textColor, false);
   }

   public int drawOrderedText(MatrixStack stack, OrderedText text, float x, float y, int color, boolean shadow) {
      int width;
      if (shadow) {
         FontRenderer.OrderedTextDrawer drawer = new FontRenderer.OrderedTextDrawer(stack, color, 0.25F, this.atlas);
         width = drawer.draw(text, x + shadowOffset(), y + shadowOffset());
         drawer = new FontRenderer.OrderedTextDrawer(stack, color, 1.0F, this.atlas);
         width = Math.max(width, drawer.draw(text, x, y));
      } else {
         FontRenderer.OrderedTextDrawer drawer = new FontRenderer.OrderedTextDrawer(stack, color, 1.0F, this.atlas);
         width = drawer.draw(text, x, y);
      }

      this.flush();
      return width;
   }

   private int drawRaw(MatrixStack stack, String text, float x, float y, int color, boolean shadow) {
      if (text == null) {
         return 0;
      } else {
         if ((color & -67108864) == 0) {
            color |= -16777216;
         }

         if (shadow) {
            color = (color & 16579836) >> 2 | color & 0xFF000000;
         }

         this.atlas.ensureGlyphs(text);
         this.drawX = x * 2.0F;
         this.drawY = y * 2.0F;
         this.drawChars(stack, text, shadow, color);
         return (int)(this.drawX / 4.0F);
      }
   }

   private void drawChars(MatrixStack stack, String text, boolean shadow, int color) {
      float alpha = (float)(color >> 24 & 0xFF) / 255.0F;
      float red = (float)(color >> 16 & 0xFF) / 255.0F;
      float green = (float)(color >> 8 & 0xFF) / 255.0F;
      float blue = (float)(color & 0xFF) / 255.0F;
      stack.push();
      stack.translate((float)getTranslate(), (float)getShift(), 0.0F);
      stack.scale(0.5F, 0.5F, 0.5F);
      if (!TEXT_BUFFER.isBuilding()) {
         TEXT_BUFFER.begin(DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
      }

      for (int i = 0; i < text.length(); i++) {
         char c = text.charAt(i);
         if ((c == 167 || c == 25602) && i + 1 < text.length()) {
            int colorIndex = "0123456789abcdefklmnor".indexOf(Character.toLowerCase(text.charAt(i + 1)));
            if (colorIndex < 16) {
               if (colorIndex < 0) {
                  colorIndex = 15;
               }

               if (shadow) {
                  colorIndex += 16;
               }

               int codeColor = COLOR_CODES[colorIndex];
               red = (float)(codeColor >> 16 & 0xFF) / 255.0F;
               green = (float)(codeColor >> 8 & 0xFF) / 255.0F;
               blue = (float)(codeColor & 0xFF) / 255.0F;
            } else {
               red = (float)(color >> 16 & 0xFF) / 255.0F;
               green = (float)(color >> 8 & 0xFF) / 255.0F;
               blue = (float)(color & 0xFF) / 255.0F;
            }

            i++;
         } else {
            this.atlas.bind();
            float advance = this.atlas.draw(stack, TEXT_BUFFER, c, this.drawX, this.drawY, red, blue, green, alpha);
            this.drawX += advance;
         }
      }

      stack.pop();
   }

   public void flush() {
      flushTextBuffer(this.atlas);
      flushLayerBuffer();
   }

   private static void flushTextBuffer(FontRenderer.FontAtlas atlas) {
      if (TEXT_BUFFER.isBuilding()) {
         GlStateManager._enableBlend();
         GlStateManager._blendFunc(770, 771);
         atlas.bind();
         RenderSystem.setShader(GameRenderer::getPositionTexColorProgram);
         RenderSystem.disableDepthTest();
         FontRenderer.BufferedVertexConsumer.draw(TEXT_BUFFER);
         RenderSystem.enableDepthTest();
         atlas.unbind();
      }
   }

   public static void flushLayerBuffer() {
      if (LAYER_BUFFER.isBuilding() && currentRenderLayer != null) {
         currentRenderLayer.startDrawing();
         RenderSystem.disableDepthTest();
         BufferRenderer.drawWithGlobalProgram(LAYER_BUFFER.end());
         RenderSystem.enableDepthTest();
         currentRenderLayer.endDrawing();
         currentRenderLayer = null;
      }
   }

   public VertexConsumerProvider vertexConsumers() {
      return LAYER_PROVIDER;
   }

   public TextHandler textHandler() {
      return this.textHandler;
   }

   public static String stripControlCodes(String text) {
      if (text == null) {
         return "";
      } else {
         String normalized = text.replace('搂', '§');
         StringBuilder builder = new StringBuilder(normalized.length());

         for (int i = 0; i < normalized.length(); i++) {
            char current = normalized.charAt(i);
            if (current == 167 && i + 1 < normalized.length()) {
               i++;
            } else {
               builder.append(current);
            }
         }

         return builder.toString();
      }
   }

   public float getWidth(String text) {
      return this.width(text);
   }

   public float width(String text) {
      if (text == null) {
         return 0.0F;
      } else {
         this.atlas.ensureGlyphs(text);
         float width = 0.0F;

         for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if ((c == 167 || c == 25602) && i + 1 < text.length()) {
               i++;
            } else {
               width += this.atlas.width(c) - 8.0F;
            }
         }

         return width / 2.0F + 1.0F;
      }
   }

   public float getStringHeight(String ignoredText) {
      return this.getFontHeight();
   }

   public float getFontHeight(String ignoredText) {
      return this.getFontHeight();
   }

   public float getFontHeight() {
      return this.atlas.height() / 2.0F - 1.0F;
   }

   public float getAdvance(char c) {
      this.atlas.ensureGlyph(c);
      return (this.atlas.width(c) - 8.0F) / 2.0F;
   }

   @Override
   public float getAdvance() {
      return 0.0F;
   }

   @Override
   public GlyphRenderer bake(Function<RenderableGlyph, GlyphRenderer> function) {
      return null;
   }

   @Override
   public void close() {
      this.atlas.close();
   }

   public static void zOffset(int z) {
      zOffset = z;
   }

   public static int zOffset() {
      return zOffset;
   }

   private static int rgbaToInt(float r, float g, float b, float a) {
      int red = (int)(clamp01(r) * 255.0F) & 0xFF;
      int green = (int)(clamp01(g) * 255.0F) & 0xFF;
      int blue = (int)(clamp01(b) * 255.0F) & 0xFF;
      int alpha = (int)((a > 1.0F ? Math.min(a, 255.0F) / 255.0F : clamp01(a)) * 255.0F) & 0xFF;
      return alpha << 24 | red << 16 | green << 8 | blue;
   }

   private static float clamp01(float value) {
      return Math.max(0.0F, Math.min(1.0F, value));
   }

   private static int getShift() {
      return Fonts.INSTANCE == null ? 0 : Fonts.INSTANCE.shift.getValueInt();
   }

   private static float shadowOffset() {
      Fonts fonts = Fonts.INSTANCE;
      if (fonts == null) {
         return 1.0F;
      } else {
         return fonts.shadowMode.is(Fonts.ShadowMode.Shoreline) ? 0.5F : fonts.shadowOffset.getValueFloat();
      }
   }

   private static int getTranslate() {
      return Fonts.INSTANCE == null ? 0 : Fonts.INSTANCE.translate.getValueInt();
   }

   private static boolean smooth() {
      return Fonts.INSTANCE != null && Fonts.INSTANCE.isOn() && Fonts.INSTANCE.smooth.getValue();
   }

   private static boolean useHachimiAntiAlias() {
      return smooth() && Fonts.INSTANCE.antiAliasMode.is(Fonts.AntiAliasMode.Hachimi);
   }

   static {
      for (int i = 0; i < 32; i++) {
         int offset = (i >> 3 & 1) * 85;
         int red = (i >> 2 & 1) * 170 + offset;
         int green = (i >> 1 & 1) * 170 + offset;
         int blue = (i & 1) * 170 + offset;
         if (i == 6) {
            red += 85;
         }

         if (i >= 16) {
            red /= 4;
            green /= 4;
            blue /= 4;
         }

         COLOR_CODES[i] = (red & 0xFF) << 16 | (green & 0xFF) << 8 | blue & 0xFF;
      }
   }

   private static final class BufferedVertexConsumer implements VertexConsumer {
      private final BufferAllocator allocator;
      private BufferBuilder builder;
      private DrawMode drawMode;
      private VertexFormat vertexFormat;

      BufferedVertexConsumer(int size) {
         this.allocator = new BufferAllocator(size);
      }

      FontRenderer.BufferedVertexConsumer begin(DrawMode drawMode, VertexFormat vertexFormat) {
         this.drawMode = drawMode;
         this.vertexFormat = vertexFormat;
         this.builder = new BufferBuilder(this.allocator, drawMode, vertexFormat);
         return this;
      }

      boolean isBuilding() {
         return this.builder != null;
      }

      BufferBuilder builder() {
         if (this.builder == null) {
            if (this.drawMode == null || this.vertexFormat == null) {
               throw new IllegalArgumentException("Tried to invoke empty font buffer");
            }

            this.builder = new BufferBuilder(this.allocator, this.drawMode, this.vertexFormat);
         }

         return this.builder;
      }

      BuiltBuffer end() {
         if (this.builder == null) {
            throw new IllegalArgumentException("Tried to end empty font buffer");
         } else {
            BuiltBuffer buffer = this.builder.endNullable();
            this.builder = null;
            return buffer;
         }
      }

      static void draw(FontRenderer.BufferedVertexConsumer consumer) {
         if (consumer.isBuilding()) {
            BuiltBuffer buffer = consumer.end();
            if (buffer != null) {
               BufferRenderer.drawWithGlobalProgram(buffer);
            }
         }
      }

      @Override
      public VertexConsumer vertex(float x, float y, float z) {
         return this.builder().vertex(x, y, z);
      }

      @Override
      public VertexConsumer color(int red, int green, int blue, int alpha) {
         return this.builder().color(red, green, blue, alpha);
      }

      @Override
      public VertexConsumer texture(float u, float v) {
         return this.builder().texture(u, v);
      }

      @Override
      public VertexConsumer overlay(int u, int v) {
         return this.builder().overlay(u, v);
      }

      @Override
      public VertexConsumer light(int u, int v) {
         return this.builder().light(u, v);
      }

      @Override
      public VertexConsumer normal(float x, float y, float z) {
         return this.builder().normal(x, y, z);
      }
   }

   private static final class FontAtlas implements Closeable {
      private final Font font;
      private final Font fallbackFont;
      private final boolean antiAlias;
      private final boolean fractionalMetrics;
      private final boolean smooth;
      private final boolean hachimi;
      private final float rasterScale;
      private final int glyphPadding;
      private final int drawInset;
      private final HashMap<Character, FontRenderer.GlyphInfo> glyphs = new HashMap<>();
      private final LinkedHashSet<Character> characters = new LinkedHashSet<>();
      private int imageSize;
      private int maxHeight = -1;
      private NativeImageBackedTexture texture;

      FontAtlas(Font font, boolean antiAlias, boolean fractionalMetrics, boolean smooth, boolean hachimi, float rasterScale) {
         this.font = font;
         this.fallbackFont = createFallbackFont(font);
         this.antiAlias = antiAlias;
         this.fractionalMetrics = fractionalMetrics;
         this.smooth = smooth;
         this.hachimi = hachimi;
         this.rasterScale = rasterScale;
         this.glyphPadding = Math.round(8.0F * rasterScale);
         this.drawInset = Math.round(2.0F * rasterScale);
      }

      void generate(char[] chars) {
         this.characters.clear();

         for (char c : chars) {
            this.characters.add(Character.valueOf(c));
         }

         this.rebuild();
      }

      void ensureGlyphs(String text) {
         if (text != null) {
            boolean changed = false;

            for (int i = 0; i < text.length(); i++) {
               char c = text.charAt(i);
               if ((c == 167 || c == 25602) && i + 1 < text.length()) {
                  i++;
               } else if (!this.glyphs.containsKey(c) && this.characters.add(Character.valueOf(c))) {
                  changed = true;
               }
            }

            if (changed) {
               FontRenderer.flushTextBuffer(this);
               this.rebuild();
            }
         }
      }

      void ensureGlyph(char c) {
         if (!this.glyphs.containsKey(c) && this.characters.add(Character.valueOf(c))) {
            FontRenderer.flushTextBuffer(this);
            this.rebuild();
         }
      }

      private void rebuild() {
         char[] chars = new char[this.characters.size()];
         int index = 0;

         for (char c : this.characters) {
            chars[index++] = c;
         }

         this.glyphs.clear();
         this.maxHeight = -1;
         double maxWidth = -0.25;
         double maxGlyphHeight = -0.25;
         FontRenderContext context = new FontRenderContext(new AffineTransform(), this.antiAlias, this.fractionalMetrics);

         for (char c : chars) {
            Rectangle2D bounds = this.fontFor(c).getStringBounds(Character.toString(c), context);
            maxWidth = Math.max(maxWidth, bounds.getWidth());
            maxGlyphHeight = Math.max(maxGlyphHeight, bounds.getHeight());
         }

         maxWidth += this.hachimi ? (double)this.glyphPadding : 2.0;
         maxGlyphHeight += this.hachimi ? 2.0 * (double)this.rasterScale : 2.0;
         this.imageSize = (int)Math.ceil(
               Math.max(
                     Math.ceil(Math.sqrt(maxWidth * maxWidth * (double)chars.length) / maxWidth),
                     Math.ceil(Math.sqrt(maxGlyphHeight * maxGlyphHeight * (double)chars.length) / maxGlyphHeight)
                  )
                  * Math.max(maxWidth, maxGlyphHeight)
            )
            + 1;
         BufferedImage image = new BufferedImage(this.imageSize, this.imageSize, 2);
         Graphics2D graphics = image.createGraphics();
         graphics.setColor(new Color(255, 255, 255, 0));
         graphics.fillRect(0, 0, this.imageSize, this.imageSize);
         graphics.setColor(Color.WHITE);
         if (this.smooth) {
            graphics.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
         } else {
            graphics.setRenderingHint(
               RenderingHints.KEY_FRACTIONALMETRICS,
               this.fractionalMetrics ? RenderingHints.VALUE_FRACTIONALMETRICS_ON : RenderingHints.VALUE_FRACTIONALMETRICS_OFF
            );
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, this.antiAlias ? RenderingHints.VALUE_ANTIALIAS_OFF : RenderingHints.VALUE_ANTIALIAS_ON);
            graphics.setRenderingHint(
               RenderingHints.KEY_TEXT_ANTIALIASING, this.antiAlias ? RenderingHints.VALUE_TEXT_ANTIALIAS_ON : RenderingHints.VALUE_TEXT_ANTIALIAS_OFF
            );
         }

         int rowHeight = 0;
         int x = 0;
         int y = 1;

         for (char c : chars) {
            Font glyphFont = this.fontFor(c);
            graphics.setFont(glyphFont);
            FontMetrics metrics = graphics.getFontMetrics(glyphFont);
            FontRenderer.GlyphInfo glyph = new FontRenderer.GlyphInfo();
            Rectangle2D bounds = metrics.getStringBounds(Character.toString(c), graphics);
            glyph.width = bounds.getBounds().width + this.glyphPadding;
            glyph.height = bounds.getBounds().height;
            if (x + glyph.width >= this.imageSize) {
               x = 0;
               y += rowHeight;
               rowHeight = 0;
            }

            glyph.x = x;
            glyph.y = y;
            this.maxHeight = Math.max(this.maxHeight, glyph.height);
            rowHeight = Math.max(rowHeight, glyph.height);
            graphics.drawString(Character.toString(c), x + this.drawInset, y + metrics.getAscent());
            x += glyph.width;
            this.glyphs.put(c, glyph);
         }

         graphics.dispose();
         this.upload(image);
         image.flush();
      }

      private Font fontFor(char c) {
         return !this.font.canDisplay(c) && !Character.isISOControl(c) ? this.fallbackFont : this.font;
      }

      private static Font createFallbackFont(Font baseFont) {
         String[] names = new String[]{"Microsoft YaHei UI", "Microsoft YaHei", "SimHei", "SimSun", "NSimSun", "SansSerif", "Dialog"};

         for (String name : names) {
            Font font = new Font(name, baseFont.getStyle(), baseFont.getSize());
            if (font.canDisplay('中')) {
               return font;
            }
         }

         return new Font("Dialog", baseFont.getStyle(), baseFont.getSize());
      }

      private void upload(BufferedImage image) {
         try {
            NativeImage nativeImage = new NativeImage(Format.RGBA, image.getWidth(), image.getHeight(), false);

            for (int y = 0; y < image.getHeight(); y++) {
               for (int x = 0; x < image.getWidth(); x++) {
                  int argb = image.getRGB(x, y);
                  int alpha = argb >>> 24 & 0xFF;
                  int red = argb >>> 16 & 0xFF;
                  int green = argb >>> 8 & 0xFF;
                  int blue = argb & 0xFF;
                  nativeImage.setColor(x, y, alpha << 24 | blue << 16 | green << 8 | red);
               }
            }

            this.closeTexture();
            this.texture = new NativeImageBackedTexture(nativeImage);
            if (this.hachimi) {
               this.texture.setFilter(true, false);
            }
         } catch (Exception var10) {
            var10.printStackTrace();
         }
      }

      void bind() {
         if (this.texture != null) {
            GlStateManager._bindTexture(this.texture.getGlId());
            if (!this.hachimi) {
               GlStateManager._texParameter(3553, 10240, this.smooth ? 9729 : 9728);
            }

            RenderSystem.setShaderTexture(0, this.texture.getGlId());
         }
      }

      void unbind() {
         RenderSystem.setShaderTexture(0, 0);
         if (this.smooth && !this.hachimi) {
            GlStateManager._texParameter(3553, 10240, 9728);
         }
      }

      float draw(MatrixStack stack, VertexConsumer consumer, char c, float x, float y, float red, float blue, float green, float alpha) {
         this.ensureGlyph(c);
         FontRenderer.GlyphInfo glyph = this.glyphs.get(c);
         if (glyph == null) {
            glyph = this.glyphs.get(' ');
         }

         if (glyph == null) {
            return 0.0F;
         } else {
            red *= RenderSystem.getShaderColor()[0];
            green *= RenderSystem.getShaderColor()[1];
            blue *= RenderSystem.getShaderColor()[2];
            alpha *= RenderSystem.getShaderColor()[3];
            float texelInset = this.hachimi ? 0.5F : 0.0F;
            float u0 = ((float)glyph.x + texelInset) / (float)this.imageSize;
            float v0 = ((float)glyph.y + texelInset) / (float)this.imageSize;
            float u1 = ((float)glyph.x + (float)glyph.width - texelInset) / (float)this.imageSize;
            float v1 = ((float)glyph.y + (float)glyph.height - texelInset) / (float)this.imageSize;
            float w = (float)glyph.width / this.rasterScale;
            float h = (float)glyph.height / this.rasterScale;
            consumer.vertex(stack.peek().getPositionMatrix(), x, y + h, (float)FontRenderer.zOffset()).color(red, green, blue, alpha).texture(u0, v1);
            consumer.vertex(stack.peek().getPositionMatrix(), x + w, y + h, (float)FontRenderer.zOffset()).color(red, green, blue, alpha).texture(u1, v1);
            consumer.vertex(stack.peek().getPositionMatrix(), x + w, y, (float)FontRenderer.zOffset()).color(red, green, blue, alpha).texture(u1, v0);
            consumer.vertex(stack.peek().getPositionMatrix(), x, y, (float)FontRenderer.zOffset()).color(red, green, blue, alpha).texture(u0, v0);
            return w - 8.0F;
         }
      }

      float width(char c) {
         this.ensureGlyph(c);
         FontRenderer.GlyphInfo glyph = this.glyphs.get(c);
         if (glyph == null) {
            glyph = this.glyphs.get(' ');
         }

         return glyph == null ? 0.0F : (float)glyph.width / this.rasterScale;
      }

      float height() {
         return (float)this.maxHeight / this.rasterScale;
      }

      @Override
      public void close() {
         this.closeTexture();
         this.glyphs.clear();
         this.characters.clear();
      }

      private void closeTexture() {
         if (this.texture != null) {
            this.texture.clearGlId();
            this.texture.close();
            this.texture = null;
         }
      }
   }

   private static final class GlyphInfo {
      int x;
      int y;
      int width;
      int height;
   }

   private static final class OrderedTextDrawer implements CharacterVisitor {
      private final float brightness;
      private final FontRenderer.FontAtlas atlas;
      private final int color;
      private final MatrixStack stack;
      private float x;
      private float y;

      OrderedTextDrawer(MatrixStack stack, int color, float brightness, FontRenderer.FontAtlas atlas) {
         this.color = color;
         this.brightness = brightness;
         this.atlas = atlas;
         this.stack = stack;
      }

      int draw(OrderedText text, float x, float y) {
         if (text == null) {
            return 0;
         } else {
            this.x = x * 2.0F;
            this.y = y * 2.0F;
            this.stack.push();
            this.stack.translate((float)FontRenderer.getTranslate(), (float)FontRenderer.getShift(), 0.0F);
            this.stack.scale(0.5F, 0.5F, 1.0F);
            if (!FontRenderer.TEXT_BUFFER.isBuilding()) {
               FontRenderer.TEXT_BUFFER.begin(DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
            }

            text.accept(this);
            this.stack.pop();
            return (int)(this.x / 4.0F);
         }
      }

      @Override
      public boolean accept(int index, Style style, int codePoint) {
         TextColor textColor = style.getColor();
         float alpha = (float)(this.color >> 24 & 0xFF) / 255.0F;
         float red;
         float green;
         float blue;
         if (textColor != null) {
            int rgb = textColor.getRgb();
            red = (float)(rgb >> 16 & 0xFF) / 255.0F;
            green = (float)(rgb >> 8 & 0xFF) / 255.0F;
            blue = (float)(rgb & 0xFF) / 255.0F;
         } else {
            red = (float)(this.color >> 16 & 0xFF) / 255.0F;
            green = (float)(this.color >> 8 & 0xFF) / 255.0F;
            blue = (float)(this.color & 0xFF) / 255.0F;
         }

         red *= this.brightness;
         green *= this.brightness;
         blue *= this.brightness;
         char character = Character.toString(codePoint).charAt(0);
         this.atlas.ensureGlyph(character);
         this.atlas.bind();
         this.x = this.x + this.atlas.draw(this.stack, FontRenderer.TEXT_BUFFER, character, this.x, this.y, red, blue, green, alpha);
         return true;
      }
   }
}
