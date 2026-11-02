package noxisclient.mod.modules.impl.hud;

import com.mojang.blaze3d.systems.RenderSystem;
import java.awt.Color;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import noxisclient.Alien;
import noxisclient.api.events.impl.PacketEvent;
import noxisclient.api.utils.math.Timer;
import noxisclient.api.utils.render.ColorUtil;
import noxisclient.api.utils.render.TextUtil;
import noxisclient.api.utils.skija.SkijaGlowRenderer;
import noxisclient.mod.gui.fonts.FontRenderer;
import noxisclient.mod.modules.HudElement;
import noxisclient.mod.modules.Module;
import noxisclient.mod.modules.settings.impl.BooleanSetting;
import noxisclient.mod.modules.settings.impl.ColorSetting;
import noxisclient.mod.modules.settings.impl.EnumSetting;
import noxisclient.mod.modules.settings.impl.SliderSetting;

public class HUD extends Module implements HudElement {
   public static HUD INSTANCE;
   private static final int LINE_SPACING = 2;
   private int hudX = 2;
   private int hudY = 50;
   private HudElement.AlignMode alignMode = HudElement.AlignMode.LEFT;
   private final BooleanSetting packets = this.add(new BooleanSetting("Packets", true));
   private final BooleanSetting fps = this.add(new BooleanSetting("FPS", true));
   private final BooleanSetting ping = this.add(new BooleanSetting("Ping", true));
   private final BooleanSetting time = this.add(new BooleanSetting("Time", false));
   private final BooleanSetting tps = this.add(new BooleanSetting("TPS", true));
   private final BooleanSetting shadow = this.add(new BooleanSetting("Shadow", true));
   private final SliderSetting size = this.add(new SliderSetting("Size", 1.0, 0.5, 3.0, 0.05));
   private final BooleanSetting glow = this.add(new BooleanSetting("Glow", false).setParent());
   private final SliderSetting glowRadius = this.add(new SliderSetting("GlowRadius", 3.0, 0.5, 8.0, 0.1, this.glow::isOpen));
   private final SliderSetting glowOpacity = this.add(new SliderSetting("GlowOpacity", 0.6, 0.1, 1.0, 0.05, this.glow::isOpen));
   private final ColorSetting color = this.add(new ColorSetting("Color", new Color(208, 0, 0)));
   private final EnumSetting<HUD.ColorMode> colorMode = this.add(new EnumSetting<>("ColorMode", HUD.ColorMode.Pulse));
   private final SliderSetting pulseSpeed = this.add(new SliderSetting("PulseSpeed", 1.0, 0.0, 5.0, 0.1, () -> this.colorMode.is(HUD.ColorMode.Pulse)));
   private final SliderSetting pulseCounter = this.add(new SliderSetting("Counter", 10, 1, 50, () -> this.colorMode.is(HUD.ColorMode.Pulse)));
   private final ColorSetting secondColor = this.add(
      new ColorSetting("SecondColor", new Color(255, 0, 0), () -> this.colorMode.is(HUD.ColorMode.Pulse)).injectBoolean(true)
   );
   private final SliderSetting rainbowSpeed = this.add(new SliderSetting("RainbowSpeed", 4.0, 1.0, 10.0, 0.1, () -> this.colorMode.is(HUD.ColorMode.Rainbow)));
   private final SliderSetting saturation = this.add(new SliderSetting("Saturation", 130, 1, 255, () -> this.colorMode.is(HUD.ColorMode.Rainbow)));
   private final SliderSetting rainbowDelay = this.add(new SliderSetting("Delay", 350, 0, 1000, () -> this.colorMode.is(HUD.ColorMode.Rainbow)));
   private int incomingPackets;
   private int outgoingPackets;
   private int lastIn;
   private int lastOut;
   private int cachedPing;
   private String cachedTime = "";
   private final Timer packetTimer = new Timer();
   private final Timer pingTimer = new Timer();
   private final Timer timeTimer = new Timer();
   private final SimpleDateFormat timeFormat = new SimpleDateFormat("h:mm a", Locale.ENGLISH);
   private final Map<Integer, Identifier> glowLineTextures = new HashMap<>();
   private final Map<Integer, String> glowLineConfigs = new HashMap<>();

   public HUD() {
      super("HUD", Module.Category.HUD);
      this.setChinese("界面");
      INSTANCE = this;
   }

   public void onReceive(PacketEvent.Receive event) {
      this.incomingPackets++;
   }

   public void onSend(PacketEvent.Send event) {
      this.outgoingPackets++;
   }

   @Override
   public void onRender2D(DrawContext drawContext, float tickDelta) {
      this.updateCaches();
      this.renderElement(drawContext, tickDelta);
   }

   @Override
   public void onDisable() {
      this.clearGlowLineCache();
      super.onDisable();
   }

   private void updateCaches() {
      if (this.packetTimer.passedMs(150.0)) {
         this.lastIn = this.incomingPackets;
         this.lastOut = this.outgoingPackets;
         this.incomingPackets = 0;
         this.outgoingPackets = 0;
         this.packetTimer.reset();
      }

      if (this.pingTimer.passedMs(1000.0)) {
         this.pingTimer.reset();
         if (mc.getNetworkHandler() != null && mc.player != null) {
            PlayerListEntry entry = mc.getNetworkHandler().getPlayerListEntry(mc.player.getUuid());
            if (entry != null) {
               this.cachedPing = entry.getLatency();
            }
         }
      }

      if (this.timeTimer.passedMs(1000.0) || this.cachedTime.isEmpty()) {
         this.cachedTime = this.timeFormat.format(new Date());
         this.timeTimer.reset();
      }
   }

   private List<String> getLines() {
      List<String> lines = new ArrayList<>(5);
      if (this.packets.getValue()) {
         lines.add("Packets §f" + this.lastIn + "<-" + this.lastOut);
      }

      if (this.fps.getValue()) {
         lines.add("FPS §f" + Alien.FPS.getFps());
      }

      if (this.ping.getValue()) {
         lines.add("Ping §f" + this.cachedPing + "ms");
      }

      if (this.time.getValue()) {
         lines.add("Time §f" + this.cachedTime);
      }

      if (this.tps.getValue()) {
         lines.add("TPS §f" + Alien.SERVER.getTPS() + " [" + Alien.SERVER.getCurrentTPS() + "]");
      }

      return lines;
   }

   private float getTextWidth(String text) {
      return TextUtil.getHudWidth(text, this.size.getValueFloat());
   }

   private Color getRenderColor(int lineIndex) {
      Color baseColor = this.color.getValue();
      if (this.colorMode.is(HUD.ColorMode.Pulse)) {
         double pulseIndex = (double)lineIndex;
         if (this.secondColor.booleanValue) {
            return ColorUtil.pulseColor(baseColor, this.secondColor.getValue(), pulseIndex, this.pulseCounter.getValueInt(), this.pulseSpeed.getValue());
         } else {
            Color pulsedColor = ColorUtil.pulseColor(baseColor, pulseIndex, this.pulseCounter.getValueInt(), this.pulseSpeed.getValue());
            return ColorUtil.injectAlpha(pulsedColor, baseColor.getAlpha());
         }
      } else if (this.colorMode.is(HUD.ColorMode.Rainbow)) {
         double state = Math.ceil(((double)System.currentTimeMillis() * this.rainbowSpeed.getValue() + (double)lineIndex * this.rainbowDelay.getValue()) / 20.0);
         Color rainbow = Color.getHSBColor((float)(state % 360.0 / 360.0), this.saturation.getValueFloat() / 255.0F, 1.0F);
         return ColorUtil.injectAlpha(rainbow, baseColor.getAlpha());
      } else {
         return baseColor;
      }
   }

   @Override
   public String getElementName() {
      return this.getChinese() != null ? this.getChinese() : this.getName();
   }

   @Override
   public int getElementX() {
      return this.alignMode == HudElement.AlignMode.RIGHT ? this.hudX - this.getElementWidth() : this.hudX;
   }

   @Override
   public void setElementX(int x) {
      if (this.alignMode == HudElement.AlignMode.RIGHT) {
         this.hudX = x + this.getElementWidth();
      } else {
         this.hudX = x;
      }
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
      float maxWidth = this.getTextWidth(this.getName());

      for (String line : this.getLines()) {
         maxWidth = Math.max(maxWidth, this.getTextWidth(line));
      }

      return (int)Math.ceil((double)maxWidth) + 4;
   }

   @Override
   public int getElementHeight() {
      int lineCount = Math.max(1, this.getLines().size());
      float lineHeight = TextUtil.getHudHeight(this.size.getValueFloat());
      float lineSpacing = 2.0F * this.size.getValueFloat();
      return (int)Math.ceil((double)((float)lineCount * lineHeight + (float)Math.max(0, lineCount - 1) * lineSpacing)) + 2;
   }

   @Override
   public boolean isElementVisible() {
      return this.isOn();
   }

   @Override
   public HudElement.AlignMode getAlignMode() {
      return this.alignMode;
   }

   @Override
   public void setAlignMode(HudElement.AlignMode mode) {
      if (mode != this.alignMode) {
         if (mode == HudElement.AlignMode.RIGHT && this.alignMode == HudElement.AlignMode.LEFT) {
            this.hudX = this.hudX + this.getElementWidth();
         } else if (mode == HudElement.AlignMode.LEFT && this.alignMode == HudElement.AlignMode.RIGHT) {
            this.hudX = this.hudX - this.getElementWidth();
         }

         this.alignMode = mode;
      }
   }

   @Override
   public int getRightAnchorX() {
      return this.alignMode == HudElement.AlignMode.RIGHT ? this.hudX : this.hudX + this.getElementWidth();
   }

   @Override
   public void setRightAnchorX(int rightX) {
      if (this.alignMode == HudElement.AlignMode.RIGHT) {
         this.hudX = rightX;
      } else {
         this.hudX = rightX - this.getElementWidth();
      }
   }

   @Override
   public void renderElement(DrawContext context, float tickDelta) {
      List<String> lines = this.getLines();
      if (lines.isEmpty()) {
         this.clearGlowLineCache();
      } else {
         if (this.glow.getValue()) {
            this.cleanupUnusedGlowLines(lines.size());
         } else {
            this.clearGlowLineCache();
         }

         float y = (float)this.hudY;

         for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            float x = this.alignMode == HudElement.AlignMode.RIGHT ? (float)this.hudX - this.getTextWidth(line) : (float)this.hudX;
            Color renderColor = this.getRenderColor(i);
            if (this.glow.getValue()) {
               this.renderGlowLine(context, i, line, x, y, renderColor);
            }

            TextUtil.drawHudString(context, line, (double)x, (double)y, renderColor.getRGB(), this.shadow.getValue(), this.size.getValueFloat());
            y += TextUtil.getHudHeight(this.size.getValueFloat()) + 2.0F * this.size.getValueFloat();
         }
      }
   }

   private boolean renderGlowLine(DrawContext context, int lineIndex, String text, float x, float y, Color textColor) {
      if (text != null && !text.isEmpty()) {
         float radius = this.glowRadius.getValueFloat();
         float opacity = this.glowOpacity.getValueFloat();
         String maskText = FontRenderer.stripControlCodes(text);
         float fontSize = TextUtil.getHudFontSize(this.size.getValueFloat());
         double guiScale = Math.max(1.0, mc.getWindow().getScaleFactor());
         String config = String.format("hud_%d_%s_%.2f_%s_%.2f_%.2f", lineIndex, maskText, radius, SkijaGlowRenderer.getFontCacheKey(null), fontSize, guiScale);
         Identifier glowTexture = this.glowLineTextures.get(lineIndex);
         boolean needsUpdate = glowTexture == null || !config.equals(this.glowLineConfigs.get(lineIndex)) || !SkijaGlowRenderer.isTextureReady(glowTexture);
         if (needsUpdate) {
            if (glowTexture != null) {
               SkijaGlowRenderer.cleanupTexture(glowTexture);
            }

            Identifier newTexture = SkijaGlowRenderer.createGlowTexture(maskText, fontSize, new Color(255, 255, 255, 0), Color.WHITE, radius, false, null, null);
            if (newTexture == null) {
               this.glowLineTextures.remove(lineIndex);
               this.glowLineConfigs.remove(lineIndex);
               return false;
            }

            glowTexture = newTexture;
            this.glowLineTextures.put(lineIndex, newTexture);
            this.glowLineConfigs.put(lineIndex, config);
         }

         if (glowTexture != null && SkijaGlowRenderer.isTextureReady(glowTexture)) {
            int[] textureSize = SkijaGlowRenderer.getTextureSize(glowTexture);
            if (textureSize != null && textureSize.length >= 2) {
               float logicalScale = (float)guiScale;
               float padding = Math.max(radius * 3.0F, 15.0F / logicalScale);
               int tintAlpha = Math.max(0, Math.min(255, Math.round((float)textColor.getAlpha() * opacity)));
               Color tint = new Color(textColor.getRed(), textColor.getGreen(), textColor.getBlue(), tintAlpha);
               this.drawGlowTexture(
                  context,
                  context.getMatrices(),
                  glowTexture,
                  x + TextUtil.getHudRenderOffsetX() - padding,
                  y + TextUtil.getHudRenderOffsetY() - padding,
                  (float)textureSize[0] / logicalScale,
                  (float)textureSize[1] / logicalScale,
                  tint
               );
               return true;
            } else {
               return false;
            }
         } else {
            return false;
         }
      } else {
         return false;
      }
   }

   private void drawGlowTexture(
      DrawContext context, MatrixStack matrices, Identifier texture, float screenX, float screenY, float screenW, float screenH, Color tint
   ) {
      if (SkijaGlowRenderer.isTextureReady(texture)) {
         RenderSystem.enableBlend();
         RenderSystem.defaultBlendFunc();
         RenderSystem.setShaderColor(
            (float)tint.getRed() / 255.0F, (float)tint.getGreen() / 255.0F, (float)tint.getBlue() / 255.0F, (float)tint.getAlpha() / 255.0F
         );
         matrices.push();
         matrices.translate(screenX, screenY, 0.0F);
         int width = (int)Math.ceil((double)screenW);
         int height = (int)Math.ceil((double)screenH);
         context.drawTexture(texture, 0, 0, 0.0F, 0.0F, width, height, width, height);
         matrices.pop();
         RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
      }
   }

   private void cleanupUnusedGlowLines(int visibleLines) {
      List<Integer> staleLines = new ArrayList<>();

      for (Integer lineIndex : this.glowLineTextures.keySet()) {
         if (lineIndex >= visibleLines) {
            staleLines.add(lineIndex);
         }
      }

      for (Integer lineIndexx : staleLines) {
         Identifier texture = this.glowLineTextures.remove(lineIndexx);
         if (texture != null) {
            SkijaGlowRenderer.cleanupTexture(texture);
         }

         this.glowLineConfigs.remove(lineIndexx);
      }
   }

   private void clearGlowLineCache() {
      if (!this.glowLineTextures.isEmpty()) {
         for (Identifier texture : new ArrayList<>(this.glowLineTextures.values())) {
            if (texture != null) {
               SkijaGlowRenderer.cleanupTexture(texture);
            }
         }

         this.glowLineTextures.clear();
         this.glowLineConfigs.clear();
      }
   }

   private static enum ColorMode {
      Custom,
      Pulse,
      Rainbow;
   }
}
