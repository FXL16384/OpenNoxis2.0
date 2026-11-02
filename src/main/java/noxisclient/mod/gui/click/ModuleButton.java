package noxisclient.mod.gui.click;

import java.awt.Color;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.gui.DrawContext;
import noxisclient.Alien;
import noxisclient.api.utils.math.Animation;
import noxisclient.api.utils.math.Easing;
import noxisclient.api.utils.render.Render2DUtil;
import noxisclient.core.impl.FontManager;
import noxisclient.mod.gui.items.Item;
import noxisclient.mod.modules.Module;
import noxisclient.mod.modules.impl.client.ClickGui;
import noxisclient.mod.modules.impl.client.Fonts;
import noxisclient.mod.modules.settings.Setting;
import noxisclient.mod.modules.settings.impl.BindSetting;
import noxisclient.mod.modules.settings.impl.BooleanSetting;
import noxisclient.mod.modules.settings.impl.ColorSetting;
import noxisclient.mod.modules.settings.impl.EnumSetting;
import noxisclient.mod.modules.settings.impl.SliderSetting;
import noxisclient.mod.modules.settings.impl.StringSetting;
import org.lwjgl.glfw.GLFW;

public class ModuleButton extends AbstractComponent {
   private final Module module;
   private final Frame parentFrame;
   private final List<SettingButton> settingButtons = new ArrayList<>();
   private boolean open = false;
   private boolean hovered = false;
   private boolean visible = true;
   private final Animation openAnimation = new Animation();
   private float animationProgress = 0.0F;
   private float renderedSettingsHeight = 0.0F;
   public static String SEARCH_TEXT = null;
   private static final List<float[]> ENABLED_SHADER_RECTS = new ArrayList<>();
   private static final List<float[]> DISABLED_SHADER_RECTS = new ArrayList<>();
   private static final float MODULE_HEIGHT = 13.0F;
   private static final float SCALE = 1.0F;
   private static final long ANIMATION_LENGTH = 200L;

   public ModuleButton(Module module, Frame parentFrame, float x, float y, float width) {
      super(x, y, width, 13.0F);
      this.module = module;
      this.parentFrame = parentFrame;
      this.setupSettings();
   }

   private void setupSettings() {
      for (Setting setting : this.module.getSettings()) {
         SettingButton button = this.createSettingButton(setting);
         if (button != null) {
            this.settingButtons.add(button);
         }
      }
   }

   private SettingButton createSettingButton(Setting setting) {
      if (setting instanceof BooleanSetting boolSetting) {
         return new BooleanSettingButton(boolSetting, this, this.x, this.y);
      } else if (setting instanceof SliderSetting sliderSetting) {
         return new SliderSettingButton(sliderSetting, this, this.x, this.y);
      } else if (setting instanceof EnumSetting<?> enumSetting) {
         return new EnumSettingButton(enumSetting, this, this.x, this.y);
      } else if (setting instanceof ColorSetting colorSetting) {
         return new ColorSettingButton(colorSetting, this, this.x, this.y);
      } else if (setting instanceof BindSetting bindSetting) {
         return new BindSettingButton(bindSetting, this, this.x, this.y);
      } else {
         return setting instanceof StringSetting stringSetting ? new StringSettingButton(stringSetting, this, this.x, this.y) : null;
      }
   }

   @Override
   public void render(DrawContext context, int mouseX, int mouseY, float delta) {
      this.render(context, this.x, this.y, mouseX, mouseY, delta);
   }

   public void render(DrawContext context, float ix, float iy, int mouseX, int mouseY, float delta) {
      beginShaderBatch();
      this.renderBackground(context, ix, iy, mouseX, mouseY, delta);
      flushShaderBatch(context);
      this.renderForeground(context, ix, iy, mouseX, mouseY, delta);
   }

   public void renderBackground(DrawContext context, float ix, float iy, int mouseX, int mouseY, float delta) {
      this.renderPass(context, ix, iy, mouseX, mouseY, delta, true);
   }

   public void renderForeground(DrawContext context, float ix, float iy, int mouseX, int mouseY, float delta) {
      this.renderPass(context, ix, iy, mouseX, mouseY, delta, false);
   }

   private void renderPass(DrawContext context, float ix, float iy, int mouseX, int mouseY, float delta, boolean backgroundPass) {
      this.x = ix;
      this.y = iy;
      ClickGui gui = ClickGui.getInstance();
      boolean exhibition = gui.isExhibitionStyle();
      boolean thunder = gui.isThunderStyle();
      boolean opal = gui.isOpalStyle();
      this.height = opal ? 20.0F : (thunder ? 14.0F : 9.0F + 4.0F + gui.height.getValueFloat() * 2.0F);
      this.updateVisibility();
      boolean isSearching = SEARCH_TEXT != null && !SEARCH_TEXT.isEmpty();
      boolean matchesSearch = isSearching && this.module.getDisplayName().toLowerCase().contains(SEARCH_TEXT.toLowerCase());
      if (!this.visible) {
         this.renderedSettingsHeight = 0.0F;
      } else {
         this.hovered = this.isHovered((double)mouseX, (double)mouseY);
         boolean fill = this.module.isOn();
         float animatedHeight = backgroundPass ? this.getAnimatedSettingsHeight() : this.renderedSettingsHeight;
         if (backgroundPass) {
            this.renderedSettingsHeight = animatedHeight;
         }

         boolean deletePressed = this.hovered && this.isDeletePressed();
         if (isSearching) {
            int var10000 = matchesSearch ? 60 : 30;
         } else {
            byte var56 = 60;
         }

         Color unfilledColor = gui.defaultColor.getValue();
         int buttonColor = fill ? gui.color.getValue().getRGB() : unfilledColor.getRGB();
         boolean shaderTarget = gui.shouldRenderModuleShader(fill);
         if (backgroundPass) {
            if (thunder && animatedHeight > 0.5F) {
               Render2DUtil.drawRoundedRect(
                  context.getMatrices(),
                  this.x + 4.0F,
                  this.y + 2.0F,
                  this.width * 1.0F - 8.0F,
                  this.height * 1.0F + animatedHeight,
                  1.0F,
                  gui.getThunderPanelColor()
               );
            } else if (opal && animatedHeight > 0.5F) {
               context.fill(
                  (int)this.x,
                  (int)(this.y + this.height * 1.0F),
                  (int)(this.x + this.width * 1.0F),
                  (int)(this.y + this.height * 1.0F + animatedHeight),
                  1073741824
               );
            }

            if (!shaderTarget && !exhibition && !thunder && !opal && fill && gui.glow.getValue() && gui.glowButtons.getValue()) {
               float intensity = gui.glowIntensity.getValueFloat();
               int layers = gui.glowLayers.getValueInt();
               Render2DUtil.drawGlowEffect(context.getMatrices(), this.x, this.y, this.width * 1.0F, this.height * 1.0F, buttonColor, intensity, layers);
            }

            if (shaderTarget) {
               this.queueShaderOverlay(context, fill, thunder);
            } else if (exhibition) {
               int rowColor = gui.getExhibitionRowColor(fill, this.hovered);
               this.fill(context, (double)this.x, (double)this.y, (double)(this.x + this.width * 1.0F), (double)(this.y + this.height * 1.0F), rowColor);
            } else if (thunder) {
               if (fill) {
                  Render2DUtil.horizontalGradient(
                     context.getMatrices(),
                     this.x + 4.0F,
                     this.y + 1.0F,
                     this.x + this.width * 1.0F - 4.0F,
                     this.y + this.height * 1.0F - 1.0F,
                     gui.getThunderGradientStart(1.0F),
                     gui.getThunderGradientEnd(1.0F)
                  );
               } else {
                  context.fill(
                     (int)(this.x + 4.0F),
                     (int)(this.y + 1.0F),
                     (int)(this.x + this.width * 1.0F - 4.0F),
                     (int)(this.y + this.height * 1.0F - 1.0F),
                     gui.getThunderRowColor(false, this.hovered)
                  );
               }
            } else if (opal) {
               boolean lastModule = this.parentFrame.isLastVisibleModule(this);
               if (lastModule) {
                  OpalRenderUtil.roundedBottom(context, this.x, this.y, this.width * 1.0F, this.height * 1.0F, 5.0F, gui.getOpalModuleBaseColor());
               } else {
                  context.fill((int)this.x, (int)this.y, (int)(this.x + this.width * 1.0F), (int)(this.y + this.height * 1.0F), gui.getOpalModuleBaseColor());
               }

               if (this.hovered && !fill) {
                  context.fill(
                     (int)this.x, (int)this.y, (int)(this.x + this.width * 1.0F), (int)(this.y + this.height * 1.0F), gui.hoverColor.getValue().getRGB()
                  );
               }

               if (fill) {
                  if (lastModule) {
                     OpalRenderUtil.roundedBottomHorizontalGradient(
                        context, this.x, this.y, this.width * 1.0F, this.height * 1.0F, 5.0F, gui.getOpalGradientStart(0.4F), gui.getOpalGradientEnd(0.4F)
                     );
                  } else {
                     Render2DUtil.horizontalGradient(
                        context.getMatrices(),
                        this.x,
                        this.y,
                        this.x + this.width * 1.0F,
                        this.y + this.height * 1.0F,
                        gui.getOpalGradientStart(0.4F),
                        gui.getOpalGradientEnd(0.4F)
                     );
                  }
               }
            } else {
               this.fill(context, (double)this.x, (double)this.y, (double)(this.x + this.width * 1.0F), (double)(this.y + this.height * 1.0F), buttonColor);
            }

            if (animatedHeight > 0.5F) {
               this.renderSettingsBackgrounds(context, mouseX, mouseY, delta, animatedHeight, exhibition, thunder, opal);
            }
         } else {
            int whiteText = opal
               ? gui.getOpalTextColor(fill)
               : (exhibition ? gui.getExhibitionTextColor(fill) : (thunder ? gui.getThunderTextColor(fill) : gui.getTextColor(fill)));
            int grayText = gui.getDefaultTextColor();
            int colorText = !exhibition && !opal
               ? (isSearching ? (matchesSearch ? whiteText : (thunder ? gui.getThunderTextColor(false) : grayText)) : whiteText)
               : whiteText;
            String name = this.module.getDisplayName();
            float textOffset = gui.textOffset.getValueFloat();
            float textX = opal ? this.x + 6.0F : (thunder ? this.x + 7.0F : this.x + 2.0F);
            float textY = opal ? this.y + 6.0F + textOffset : (thunder ? this.y + this.height * 1.0F / 2.0F - 4.0F + textOffset : this.y + 3.0F + textOffset);
            if (Fonts.useClickGuiFont()) {
               if (!exhibition && !thunder && !opal && fill && gui.glow.getValue() && gui.glowText.getValue()) {
                  float intensity = gui.textGlowIntensity.getValueFloat();
                  FontManager.ui.drawStringWithGlow(context.getMatrices(), name, textX, textY, colorText, buttonColor, intensity, gui.shadow.getValue());
               } else {
                  FontManager.ui.drawString(context.getMatrices(), name, textX, textY, colorText, gui.shadow.getValue());
               }
            } else {
               if (!exhibition && !thunder && !opal && fill && gui.glow.getValue() && gui.glowText.getValue()) {
                  float intensity = gui.textGlowIntensity.getValueFloat();
                  int layers = 3;

                  for (int i = layers; i > 0; i--) {
                     float offset = (float)i * intensity * 0.8F;
                     int alpha = (int)(200.0F * (1.0F - (float)i / (float)(layers + 1)) * 0.8F);
                     if (alpha > 10) {
                        int glowColor = alpha << 24 | buttonColor & 16777215;

                        for (int j = 0; j < 8; j++) {
                           double angle = (Math.PI / 4) * (double)j;
                           int gx = (int)((double)textX + Math.cos(angle) * (double)offset);
                           int gy = (int)((double)textY + Math.sin(angle) * (double)offset);
                           context.drawText(mc.textRenderer, name, gx, gy, glowColor, false);
                        }
                     }
                  }
               }

               context.drawText(mc.textRenderer, name, (int)textX, (int)textY, colorText, gui.shadow.getValue());
            }

            boolean showModuleExpandIndicator = (exhibition || thunder || opal || gui.moduleExpand.getValue()) && !this.settingButtons.isEmpty();
            if (showModuleExpandIndicator) {
               String indicator = deletePressed ? "Del" : (opal ? (this.open ? "^" : "v") : (this.open ? "-" : "+"));
               int indicatorColor = deletePressed
                  ? (thunder ? gui.getThunderDangerColor() : new Color(255, 85, 85, 255).getRGB())
                  : (opal ? colorText : (thunder ? gui.getThunderAccentColor() : gui.getExhibitionIndicatorColor()));
               float indicatorWidth = (float)mc.textRenderer.getWidth(indicator) * 1.0F;
               float indicatorX = this.x + this.width * 1.0F - indicatorWidth - (thunder ? 11.0F : (opal ? 10.0F : 3.0F)) * 1.0F;
               float indicatorY = opal
                  ? this.y + 6.0F + textOffset
                  : (thunder ? this.y + this.height * 1.0F / 2.0F - 4.0F + textOffset : this.y + 3.0F + textOffset);
               if (Fonts.useClickGuiFont()) {
                  FontManager.ui.drawString(context.getMatrices(), indicator, indicatorX, indicatorY, indicatorColor, gui.shadow.getValue());
               } else {
                  context.drawText(mc.textRenderer, indicator, (int)indicatorX, (int)indicatorY, indicatorColor, gui.shadow.getValue());
               }
            } else if (!exhibition && !thunder && !opal && deletePressed) {
               String delText = "Del";
               float delTextWidth = (float)mc.textRenderer.getWidth(delText) * 1.0F;
               float delTextX = this.x + this.width * 1.0F - delTextWidth - 4.0F;
               float delTextY = this.y + 3.0F + textOffset;
               int delColor = new Color(255, 85, 85, 255).getRGB();
               if (Fonts.useClickGuiFont()) {
                  FontManager.ui.drawString(context.getMatrices(), delText, delTextX, delTextY, delColor, gui.shadow.getValue());
               } else {
                  context.drawText(mc.textRenderer, delText, (int)delTextX, (int)delTextY, delColor, gui.shadow.getValue());
               }
            }

            if (animatedHeight > 0.5F) {
               this.renderSettingsForegrounds(context, mouseX, mouseY, delta, animatedHeight, exhibition, thunder, opal);
               if (fill && !shaderTarget && !exhibition && !thunder && !opal) {
                  Color accentColor = gui.color.getValue();
                  this.fill(
                     context,
                     (double)this.x,
                     (double)(this.y + this.height * 1.0F),
                     (double)(this.x + 1.0F),
                     (double)(this.y + this.height * 1.0F + animatedHeight),
                     accentColor.getRGB()
                  );
                  this.fill(
                     context,
                     (double)(this.x + this.width * 1.0F - 1.0F),
                     (double)(this.y + this.height * 1.0F),
                     (double)(this.x + this.width * 1.0F),
                     (double)(this.y + this.height * 1.0F + animatedHeight),
                     accentColor.getRGB()
                  );
                  this.fill(
                     context,
                     (double)this.x,
                     (double)(this.y + this.height * 1.0F + animatedHeight - 1.0F),
                     (double)(this.x + this.width * 1.0F),
                     (double)(this.y + this.height * 1.0F + animatedHeight),
                     accentColor.getRGB()
                  );
               }
            }
         }
      }
   }

   private void renderSettingsBackgrounds(
      DrawContext context, int mouseX, int mouseY, float delta, float animatedHeight, boolean exhibition, boolean thunder, boolean opal
   ) {
      this.renderSettingsPass(context, mouseX, mouseY, delta, animatedHeight, exhibition, thunder, opal, true);
   }

   private void renderSettingsForegrounds(
      DrawContext context, int mouseX, int mouseY, float delta, float animatedHeight, boolean exhibition, boolean thunder, boolean opal
   ) {
      this.renderSettingsPass(context, mouseX, mouseY, delta, animatedHeight, exhibition, thunder, opal, false);
   }

   private void renderSettingsPass(
      DrawContext context, int mouseX, int mouseY, float delta, float animatedHeight, boolean exhibition, boolean thunder, boolean opal, boolean backgroundPass
   ) {
      float off = this.y + this.height * 1.0F + (!exhibition && !opal ? (thunder ? 2.0F : 1.0F) : 0.0F);
      float clipBottom = off + animatedHeight;
      float settingInset = opal ? 0.0F : (exhibition ? 1.0F : (thunder ? 3.0F : 2.0F));
      if (ClickGuiScissor.enable(context, this.x, off, this.x + this.width * 1.0F, clipBottom)) {
         for (SettingButton settingButton : this.settingButtons) {
            if (settingButton.getSetting().isVisible()) {
               float settingHeight = this.getSettingHeight(settingButton);
               if (off >= clipBottom) {
                  break;
               }

               settingButton.setX(this.x + settingInset);
               settingButton.setY(off);
               settingButton.setWidth(this.width - settingInset * 2.0F);
               if (backgroundPass) {
                  settingButton.setShaderClipBottom(clipBottom);
                  settingButton.renderBackground(context, mouseX, mouseY, delta);
                  settingButton.clearShaderClip();
               } else {
                  settingButton.renderForeground(context, mouseX, mouseY, delta);
               }

               off += settingHeight;
            }
         }

         context.disableScissor();
      }
   }

   private void updateVisibility() {
      boolean isSearching = SEARCH_TEXT != null && !SEARCH_TEXT.isEmpty();
      boolean matchesSearch = isSearching && this.module.getDisplayName().toLowerCase().contains(SEARCH_TEXT.toLowerCase());
      this.visible = !isSearching || matchesSearch;
   }

   private float updateAnimationProgress() {
      this.animationProgress = (float)this.openAnimation.get(this.open ? 1.0 : 0.0, 200L, Easing.CubicOut);
      if (this.animationProgress < 0.001F) {
         this.animationProgress = 0.0F;
      } else if (this.animationProgress > 0.999F) {
         this.animationProgress = 1.0F;
      }

      return this.animationProgress;
   }

   private float getSettingHeight(SettingButton settingButton) {
      return settingButton instanceof EnumSettingButton enumButton ? enumButton.getTotalHeight() : settingButton.getHeight();
   }

   private float getSettingsHeight() {
      float totalHeight = 0.0F;
      int visibleSettings = 0;

      for (SettingButton settingButton : this.settingButtons) {
         if (settingButton.getSetting().isVisible()) {
            totalHeight += this.getSettingHeight(settingButton);
            visibleSettings++;
         }
      }

      ClickGui gui = ClickGui.getInstance();
      boolean exhibition = gui.isExhibitionStyle();
      boolean thunder = gui.isThunderStyle();
      boolean opal = gui.isOpalStyle();
      return totalHeight + (visibleSettings > 0 ? (!exhibition && !opal ? (thunder ? 4.0F : 3.0F) : 0.0F) : 0.0F);
   }

   public float getAnimatedSettingsHeight() {
      this.updateVisibility();
      return !this.visible ? 0.0F : this.getSettingsHeight() * this.updateAnimationProgress();
   }

   public float getRenderedSettingsHeight() {
      return this.renderedSettingsHeight;
   }

   private void fill(DrawContext context, double x1, double y1, double x2, double y2, int color) {
      Render2DUtil.drawRect(context.getMatrices(), (float)x1, (float)y1, (float)(x2 - x1), (float)(y2 - y1), color);
   }

   public static void beginShaderBatch() {
      ENABLED_SHADER_RECTS.clear();
      DISABLED_SHADER_RECTS.clear();
   }

   public static void queueGuiShaderRect(boolean enabled, DrawContext context, float x, float y, float width, float height) {
      ClickGui gui = ClickGui.getInstance();
      if (Alien.SHADER != null && gui != null && gui.shader.getValue() && context != null && !(width <= 0.0F) && !(height <= 0.0F)) {
         queueShaderRectRaw(enabled, x, y, width, height);
      }
   }

   public static void flushShaderBatch(DrawContext context) {
      ClickGui gui = ClickGui.getInstance();
      if (Alien.SHADER != null && gui != null) {
         flushFullUiShaderBatch(context, gui);
      }

      DISABLED_SHADER_RECTS.clear();
      ENABLED_SHADER_RECTS.clear();
   }

   private static void flushFullUiShaderBatch(DrawContext context, ClickGui gui) {
      if (!DISABLED_SHADER_RECTS.isEmpty() || !ENABLED_SHADER_RECTS.isEmpty()) {
         List<float[]> rects = new ArrayList<>(DISABLED_SHADER_RECTS.size() + ENABLED_SHADER_RECTS.size());
         addShaderRectsWithOpacity(rects, DISABLED_SHADER_RECTS, gui.getModuleShaderAlpha(false));
         addShaderRectsWithOpacity(rects, ENABLED_SHADER_RECTS, gui.getModuleShaderAlpha(true));
         if (!rects.isEmpty()) {
            Alien.SHADER.applyGuiShaderRects(rects, gui.shaderMode.getValue(), context.getMatrices(), 1.0F);
         }
      }
   }

   private static void addShaderRectsWithOpacity(List<float[]> target, List<float[]> source, float opacity) {
      if (!(opacity <= 0.0F)) {
         for (float[] r : source) {
            target.add(new float[]{r[0], r[1], r[2], r[3], opacity});
         }
      }
   }

   private void queueShaderOverlay(DrawContext context, boolean enabled, boolean thunder) {
      ClickGui gui = ClickGui.getInstance();
      if (Alien.SHADER != null && gui.shouldRenderModuleShader(enabled)) {
         float shaderX = thunder ? this.x + 4.0F : this.x;
         float shaderY = thunder ? this.y + 1.0F : this.y;
         float shaderWidth = thunder ? this.width * 1.0F - 8.0F : this.width * 1.0F;
         float shaderHeight = thunder ? this.height * 1.0F - 2.0F : this.height * 1.0F;
         if (!(shaderWidth <= 0.0F) && !(shaderHeight <= 0.0F)) {
            queueGuiShaderRect(enabled, context, shaderX, shaderY, shaderWidth, shaderHeight);
         }
      }
   }

   private static void queueShaderRectRaw(boolean enabled, float x, float y, float width, float height) {
      (enabled ? ENABLED_SHADER_RECTS : DISABLED_SHADER_RECTS).add(new float[]{x, y, width, height});
   }

   @Override
   public void mouseClicked(double mouseX, double mouseY, int button) {
      if (this.visible) {
         boolean clickedModule = this.isHovered(mouseX, mouseY);
         if (clickedModule) {
            if (this.isDeletePressed() && (button == 0 || button == 1)) {
               this.resetModuleToDefaultConfig();
               Item.sound();
               return;
            }

            if (button == 0) {
               this.module.toggle();
               Item.sound();
            } else if (button == 1 && !this.settingButtons.isEmpty()) {
               this.open = !this.open;
               Item.sound();
            }
         }

         if (this.open && !clickedModule) {
            for (SettingButton settingButton : this.settingButtons) {
               if (settingButton.getSetting().isVisible()) {
                  settingButton.mouseClicked(mouseX, mouseY, button);
               }
            }
         }
      }
   }

   private boolean isDeletePressed() {
      if (mc != null && mc.getWindow() != null) {
         long handle = mc.getWindow().getHandle();
         return GLFW.glfwGetKey(handle, 261) == 1;
      } else {
         return false;
      }
   }

   private void resetModuleToDefaultConfig() {
      Map<String, String> defaults = this.loadBundledDefaultOptions();

      for (Setting setting : this.module.getSettings()) {
         String key = this.module.getName() + "_" + setting.getName();
         if (!this.applyDefaultConfigValue(setting, key, defaults)) {
            this.resetSettingToConstructorDefault(setting);
         }
      }

      String moduleState = defaults.get(this.module.getName() + "_state");
      if (moduleState != null) {
         boolean enabled = Boolean.parseBoolean(moduleState);
         if (this.module.isOn() != enabled) {
            this.module.toggle();
         }
      }
   }

   private Map<String, String> loadBundledDefaultOptions() {
      Map<String, String> defaults = new HashMap<>();

      try {
         label81: {
            Object var11;
            try (InputStream input = ModuleButton.class.getResourceAsStream("/noxisclient/options.txt")) {
               if (input != null) {
                  String line;
                  try (BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
                     while ((line = reader.readLine()) != null) {
                        int separator = line.indexOf(58);
                        if (separator > 0) {
                           defaults.put(line.substring(0, separator), line.substring(separator + 1));
                        }
                     }
                     break label81;
                  }
               }

               var11 = defaults;
            }

            return (Map<String, String>)var11;
         }
      } catch (Exception var10) {
      }

      return defaults;
   }

   private boolean applyDefaultConfigValue(Setting setting, String key, Map<String, String> defaults) {
      String value = defaults.get(key);
      if (value == null) {
         return false;
      } else {
         try {
            if (setting instanceof BooleanSetting booleanSetting) {
               booleanSetting.setValue(Boolean.parseBoolean(value));
            } else if (setting instanceof SliderSetting sliderSetting) {
               sliderSetting.setValue(Double.parseDouble(value));
            } else if (setting instanceof EnumSetting<?> enumSetting) {
               enumSetting.loadSetting(value);
            } else if (setting instanceof ColorSetting colorSetting) {
               colorSetting.setValue(new Color(Integer.parseInt(value), true));
               colorSetting.setSync(Boolean.parseBoolean(defaults.getOrDefault(key + "Sync", String.valueOf(colorSetting.getDefaultSync()))));
               if (colorSetting.injectBoolean) {
                  colorSetting.booleanValue = Boolean.parseBoolean(
                     defaults.getOrDefault(key + "Boolean", String.valueOf(colorSetting.getDefaultBooleanValue()))
                  );
               }
            } else if (setting instanceof BindSetting bindSetting) {
               bindSetting.setValue(Integer.parseInt(value));
               bindSetting.setHoldEnable(Boolean.parseBoolean(defaults.getOrDefault(key + "_hold", "false")));
               bindSetting.holding = false;
            } else if (setting instanceof StringSetting stringSetting) {
               stringSetting.setValue(value);
            }

            return true;
         } catch (Exception var11) {
            return false;
         }
      }
   }

   private void resetSettingToConstructorDefault(Setting setting) {
      if (setting instanceof BooleanSetting booleanSetting) {
         booleanSetting.setValue(booleanSetting.getDefaultValue());
      } else if (setting instanceof SliderSetting sliderSetting) {
         sliderSetting.setValue(sliderSetting.getDefaultValue());
      } else if (setting instanceof EnumSetting<?> enumSetting) {
         enumSetting.resetValue();
      } else if (setting instanceof ColorSetting colorSetting) {
         colorSetting.setValue(colorSetting.getDefaultValue());
         colorSetting.setSync(colorSetting.getDefaultSync());
         if (colorSetting.injectBoolean) {
            colorSetting.booleanValue = colorSetting.getDefaultBooleanValue();
         }
      } else if (setting instanceof BindSetting bindSetting) {
         bindSetting.setValue(bindSetting.getDefaultValue());
         bindSetting.setHoldEnable(false);
         bindSetting.holding = false;
      } else if (setting instanceof StringSetting stringSetting) {
         stringSetting.setValue(stringSetting.getDefaultValue());
      }
   }

   @Override
   public void mouseReleased(double mouseX, double mouseY, int button) {
      if (this.open) {
         for (SettingButton settingButton : this.settingButtons) {
            if (settingButton.getSetting().isVisible()) {
               settingButton.mouseReleased(mouseX, mouseY, button);
            }
         }
      }
   }

   @Override
   public void keyPressed(int keyCode, int scanCode, int modifiers) {
      if (this.open) {
         for (SettingButton settingButton : this.settingButtons) {
            if (settingButton.getSetting().isVisible()) {
               settingButton.keyPressed(keyCode, scanCode, modifiers);
            }
         }
      }
   }

   @Override
   public void charTyped(char chr, int modifiers) {
      if (this.open) {
         for (SettingButton settingButton : this.settingButtons) {
            if (settingButton.getSetting().isVisible()) {
               settingButton.charTyped(chr, modifiers);
            }
         }
      }
   }

   public float getFullHeight() {
      this.updateVisibility();
      if (!this.visible) {
         return 0.0F;
      } else {
         float totalHeight = 13.0F;
         if (this.open) {
            for (SettingButton settingButton : this.settingButtons) {
               if (settingButton.getSetting().isVisible()) {
                  totalHeight += this.getSettingHeight(settingButton) + 2.0F;
               }
            }
         }

         return totalHeight;
      }
   }

   public Module getModule() {
      return this.module;
   }

   public boolean isOpen() {
      return this.open;
   }

   public void restoreExpansion(boolean expanded) {
      this.open = expanded;
      this.animationProgress = expanded ? 1.0F : 0.0F;
      this.renderedSettingsHeight = 0.0F;
      this.openAnimation.from = expanded ? 1.0 : 0.0;
      this.openAnimation.to = expanded ? 1.0 : 0.0;
   }

   public Frame getParentFrame() {
      return this.parentFrame;
   }

   @Override
   public boolean isVisible() {
      return this.visible;
   }

   public List<SettingButton> getSettingButtons() {
      return this.settingButtons;
   }

   public void offset(float value) {
   }
}
