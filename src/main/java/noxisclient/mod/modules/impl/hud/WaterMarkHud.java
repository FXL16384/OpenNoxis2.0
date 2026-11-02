package noxisclient.mod.modules.impl.hud;

import com.mojang.blaze3d.systems.RenderSystem;
import java.awt.Color;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import noxisclient.api.utils.render.ColorUtil;
import noxisclient.api.utils.render.TextUtil;
import noxisclient.api.utils.skija.SkijaGlowRenderer;
import noxisclient.core.impl.FontManager;
import noxisclient.mod.gui.fonts.FontRenderer;
import noxisclient.mod.modules.HudElement;
import noxisclient.mod.modules.Module;
import noxisclient.mod.modules.settings.impl.BooleanSetting;
import noxisclient.mod.modules.settings.impl.ColorSetting;
import noxisclient.mod.modules.settings.impl.EnumSetting;
import noxisclient.mod.modules.settings.impl.SliderSetting;
import noxisclient.mod.modules.settings.impl.StringSetting;

public class WaterMarkHud extends Module implements HudElement {
   public static WaterMarkHud INSTANCE;
   private int hudX = 2;
   private int hudY = 2;
   private final StringSetting title = this.add(new StringSetting("Title", "%hackname% %version%"));
   private final BooleanSetting shadow = this.add(new BooleanSetting("Shadow", true));
   private final SliderSetting size = this.add(new SliderSetting("Size", 1.0, 0.5, 3.0, 0.05));
   private final ColorSetting color = this.add(new ColorSetting("Color", new Color(208, 0, 0)));
   private final EnumSetting<WaterMarkHud.ColorMode> colorMode = this.add(new EnumSetting<>("ColorMode", WaterMarkHud.ColorMode.Pulse));
   private final SliderSetting pulseSpeed = this.add(new SliderSetting("PulseSpeed", 1.0, 0.0, 5.0, 0.1));
   private final SliderSetting pulseCounter = this.add(new SliderSetting("Counter", 10, 1, 50));
   private final ColorSetting endColor = this.add(new ColorSetting("SecondColor", new Color(255, 0, 0)).injectBoolean(true));
   private final SliderSetting rainbowSpeed = this.add(new SliderSetting("RainbowSpeed", 4.0, 1.0, 10.0, 0.1));
   private final SliderSetting saturation = this.add(new SliderSetting("Saturation", 130, 1, 255));
   private final SliderSetting rainbowDelay = this.add(new SliderSetting("Delay", 350, 0, 1000));
   private final SliderSetting glowRadius = this.add(new SliderSetting("GlowRadius", 4.5, 0.1, 10.0, 0.1));
   private final SliderSetting glowOpacity = this.add(new SliderSetting("GlowOpacity", 1.0, 0.1, 1.0, 0.05));
   private final SliderSetting glowYOffset = this.add(new SliderSetting("GlowYOffset", -20.0, -20.0, 10.0, 0.1));
   private final BooleanSetting glowGradient = this.add(new BooleanSetting("Gradient", true));
   private final BooleanSetting useSkija = this.add(new BooleanSetting("Skija", true));
   public final BooleanSetting artist = this.add(new BooleanSetting("Artist", false).setParent());
   public final StringSetting artistText = this.add(new StringSetting("ArtistText", "NoxisClient V2", () -> this.artist.isOpen()));
   public final SliderSetting artistSize = this.add(new SliderSetting("ArtistSize", 8.0, 1.0, 20.0, 0.1, () -> this.artist.isOpen()));
   private Identifier currentGlowTexture = null;
   private String lastGlowConfig = "";
   private Identifier currentArtistTexture = null;
   private String lastArtistConfig = "";

   public WaterMarkHud() {
      super("WaterMark", Module.Category.HUD);
      this.setChinese("水印");
      INSTANCE = this;
   }

   public void setWatermarkText(String text) {
      this.title.setValue(text);
   }

   public String getWatermarkTextValue() {
      return this.title.getValue();
   }

   private String getWatermarkText() {
      return this.title.getValue().replaceAll("%version%", "2.0").replaceAll("%hackname%", "Noxis");
   }

   private Color getCurrentColor() {
      if (this.colorMode.is(WaterMarkHud.ColorMode.Pulse)) {
         return this.endColor.booleanValue
            ? ColorUtil.pulseColor(this.color.getValue(), this.endColor.getValue(), 0.0, this.pulseCounter.getValueInt(), this.pulseSpeed.getValue())
            : ColorUtil.pulseColor(this.color.getValue(), 0.0, this.pulseCounter.getValueInt(), this.pulseSpeed.getValue());
      } else if (this.colorMode.is(WaterMarkHud.ColorMode.Rainbow)) {
         double state = Math.ceil(((double)System.currentTimeMillis() * this.rainbowSpeed.getValue() + 20.0 * this.rainbowDelay.getValue()) / 20.0);
         Color rainbow = Color.getHSBColor((float)(state % 360.0 / 360.0), this.saturation.getValueFloat() / 255.0F, 1.0F);
         return ColorUtil.injectAlpha(rainbow, this.color.getValue().getAlpha());
      } else {
         return this.color.getValue();
      }
   }

   @Override
   public void onRender2D(DrawContext drawContext, float tickDelta) {
      this.renderElement(drawContext, tickDelta);
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
      if (this.artist.getValue()) {
         FontRenderer artistFont = FontManager.getArtistFont(this.artistSize.getValueFloat());
         if (artistFont != null) {
            return (int)artistFont.getWidth(this.artistText.getValue()) + 4;
         }
      }

      return (int)Math.ceil((double)TextUtil.getHudWidth(this.getWatermarkText(), this.size.getValueFloat())) + 4;
   }

   @Override
   public int getElementHeight() {
      if (this.artist.getValue()) {
         FontRenderer artistFont = FontManager.getArtistFont(this.artistSize.getValueFloat());
         if (artistFont != null) {
            return (int)artistFont.getFontHeight() + 4;
         }
      }

      return (int)Math.ceil((double)TextUtil.getHudHeight(this.size.getValueFloat())) + 4;
   }

   @Override
   public boolean isElementVisible() {
      return this.isEnabled();
   }

   @Override
   public void renderElement(DrawContext context, float tickDelta) {
      if (this.artist.getValue() && this.artistText.getValue() != null && !this.artistText.getValue().isEmpty()) {
         this.drawArtistWatermark(context);
      } else {
         this.renderNormalWatermark(context);
      }
   }

   private void renderNormalWatermark(DrawContext context) {
      String text = this.getWatermarkText();
      MatrixStack ms = context.getMatrices();
      Color textColor = this.getCurrentColor();
      Color glowColor = new Color(
         textColor.getRed(), textColor.getGreen(), textColor.getBlue(), Math.min(255, (int)((double)textColor.getAlpha() * this.glowOpacity.getValue()))
      );
      float yOff = (float)this.glowYOffset.getValue();
      if (this.useSkija.getValue()) {
         float fontSize = TextUtil.getHudFontSize(this.size.getValueFloat());
         String config = String.format(
            "%s_%f_%08x_%08x_%.2f_%b_%s_%.2f",
            text,
            this.glowRadius.getValue(),
            textColor.getRGB(),
            glowColor.getRGB(),
            this.glowOpacity.getValue(),
            this.glowGradient.getValue(),
            SkijaGlowRenderer.getFontCacheKey(null),
            fontSize
         );
         boolean needsUpdate = this.currentGlowTexture == null
            || !config.equals(this.lastGlowConfig)
            || !SkijaGlowRenderer.isTextureReady(this.currentGlowTexture);
         if (needsUpdate && this.currentGlowTexture != null) {
            SkijaGlowRenderer.cleanupTexture(this.currentGlowTexture);
            this.currentGlowTexture = null;
         }

         if (needsUpdate) {
            Color transparentText = new Color(textColor.getRed(), textColor.getGreen(), textColor.getBlue(), 0);
            this.currentGlowTexture = SkijaGlowRenderer.createGlowTexture(
               text,
               fontSize,
               transparentText,
               glowColor,
               (float)this.glowRadius.getValue(),
               this.glowGradient.getValue(),
               this.glowGradient.getValue() ? glowColor : null,
               null
            );
            this.lastGlowConfig = config;
         }

         if (this.currentGlowTexture != null && SkijaGlowRenderer.isTextureReady(this.currentGlowTexture)) {
            int[] textureSize = SkijaGlowRenderer.getTextureSize(this.currentGlowTexture);
            if (textureSize != null && textureSize.length >= 2) {
               float guiScale = (float)Math.max(1.0, mc.getWindow().getScaleFactor());
               float pad = Math.max((float)this.glowRadius.getValue() * 3.0F, 15.0F / guiScale);
               RenderSystem.enableBlend();
               RenderSystem.defaultBlendFunc();
               RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
               ms.push();
               ms.translate((float)this.hudX + TextUtil.getHudRenderOffsetX() - pad, (float)this.hudY + yOff + TextUtil.getHudRenderOffsetY() - pad, 0.0F);
               int textureWidth = (int)Math.ceil((double)((float)textureSize[0] / guiScale));
               int textureHeight = (int)Math.ceil((double)((float)textureSize[1] / guiScale));
               context.drawTexture(this.currentGlowTexture, 0, 0, 0.0F, 0.0F, textureWidth, textureHeight, textureWidth, textureHeight);
               ms.pop();
               RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            }
         }

         TextUtil.drawHudString(context, text, (double)this.hudX, (double)this.hudY, textColor.getRGB(), this.shadow.getValue(), this.size.getValueFloat());
      } else {
         this.renderSimpleGlow(
            context,
            text,
            (float)this.hudX,
            (float)this.hudY + yOff,
            glowColor,
            (int)this.glowRadius.getValue() * 2,
            this.shadow.getValue(),
            this.size.getValueFloat()
         );
         TextUtil.drawHudString(context, text, (double)this.hudX, (double)this.hudY, textColor.getRGB(), this.shadow.getValue(), this.size.getValueFloat());
      }
   }

   private void renderSimpleGlow(DrawContext context, String text, float x, float y, Color textColor, int layers, boolean drawShadow, float scale) {
      for (int i = layers; i > 0; i--) {
         float offset = (float)i * 0.5F;
         int alpha = textColor.getAlpha() / (i + 1);
         int glowColor = new Color(textColor.getRed(), textColor.getGreen(), textColor.getBlue(), Math.min(255, alpha)).getRGB();
         TextUtil.drawHudString(context, text, (double)(x - offset), (double)(y - offset), glowColor, drawShadow, scale);
         TextUtil.drawHudString(context, text, (double)(x + offset), (double)(y - offset), glowColor, drawShadow, scale);
         TextUtil.drawHudString(context, text, (double)(x - offset), (double)(y + offset), glowColor, drawShadow, scale);
         TextUtil.drawHudString(context, text, (double)(x + offset), (double)(y + offset), glowColor, drawShadow, scale);
      }
   }

   private void drawArtistWatermark(DrawContext drawContext) {
      FontRenderer artistFont = FontManager.getArtistFont(this.artistSize.getValueFloat());
      if (artistFont == null) {
         this.renderNormalWatermark(drawContext);
      } else {
         String text = this.artistText.getValue();
         MatrixStack matrixStack = drawContext.getMatrices();
         Color textColor = this.getCurrentColor();
         Color glowColor = new Color(
            textColor.getRed(), textColor.getGreen(), textColor.getBlue(), Math.min(255, (int)((double)textColor.getAlpha() * this.glowOpacity.getValue()))
         );
         if (this.useSkija.getValue()) {
            float artistGlowFontSize = Math.max(1.0F, this.artistSize.getValueFloat() * 0.5F);
            String currentConfig = String.format(
               "%s_%f_%f_%08x_%08x_%.2f_%b_%.2f",
               text,
               this.artistSize.getValueFloat(),
               this.glowRadius.getValue(),
               textColor.getRGB(),
               glowColor.getRGB(),
               this.glowOpacity.getValue(),
               this.glowGradient.getValue(),
               artistGlowFontSize
            );
            boolean needsUpdate = this.currentArtistTexture == null
               || !currentConfig.equals(this.lastArtistConfig)
               || !SkijaGlowRenderer.isTextureReady(this.currentArtistTexture);
            if (needsUpdate && this.currentArtistTexture != null) {
               SkijaGlowRenderer.cleanupTexture(this.currentArtistTexture);
               this.currentArtistTexture = null;
            }

            if (needsUpdate) {
               this.currentArtistTexture = SkijaGlowRenderer.createGlowTexture(
                  text,
                  artistGlowFontSize,
                  textColor,
                  glowColor,
                  (float)this.glowRadius.getValue(),
                  this.glowGradient.getValue(),
                  this.glowGradient.getValue() ? glowColor : null,
                  "borel-regular.ttf"
               );
               this.lastArtistConfig = currentConfig;
            }

            if (this.currentArtistTexture != null && SkijaGlowRenderer.isTextureReady(this.currentArtistTexture)) {
               int[] textureSize = SkijaGlowRenderer.getTextureSize(this.currentArtistTexture);
               if (textureSize != null && textureSize.length >= 2) {
                  float guiScale = (float)Math.max(1.0, mc.getWindow().getScaleFactor());
                  float glowPadding = Math.max((float)this.glowRadius.getValue() * 3.0F, 15.0F / guiScale);
                  RenderSystem.enableBlend();
                  RenderSystem.defaultBlendFunc();
                  RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
                  matrixStack.push();
                  matrixStack.translate(
                     (float)this.hudX + TextUtil.getHudRenderOffsetX() - glowPadding, (float)this.hudY + TextUtil.getHudRenderOffsetY() - glowPadding, 0.0F
                  );
                  drawContext.drawTexture(
                     this.currentArtistTexture,
                     0,
                     0,
                     0.0F,
                     0.0F,
                     (int)Math.ceil((double)((float)textureSize[0] / guiScale)),
                     (int)Math.ceil((double)((float)textureSize[1] / guiScale)),
                     (int)Math.ceil((double)((float)textureSize[0] / guiScale)),
                     (int)Math.ceil((double)((float)textureSize[1] / guiScale))
                  );
                  matrixStack.pop();
                  RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
                  return;
               }

               return;
            }
         }

         artistFont.drawString(
            matrixStack,
            text,
            (float)this.hudX,
            (float)this.hudY,
            (float)textColor.getRed() / 255.0F,
            (float)textColor.getGreen() / 255.0F,
            (float)textColor.getBlue() / 255.0F,
            (float)textColor.getAlpha() / 255.0F,
            this.shadow.getValue()
         );
      }
   }

   @Override
   public void onDisable() {
      super.onDisable();
      if (this.currentGlowTexture != null) {
         SkijaGlowRenderer.cleanupTexture(this.currentGlowTexture);
         this.currentGlowTexture = null;
      }

      if (this.currentArtistTexture != null) {
         SkijaGlowRenderer.cleanupTexture(this.currentArtistTexture);
         this.currentArtistTexture = null;
      }
   }

   private static enum ColorMode {
      Custom,
      Pulse,
      Rainbow;
   }
}
