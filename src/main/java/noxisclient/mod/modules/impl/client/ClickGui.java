package noxisclient.mod.modules.impl.client;

import java.awt.Color;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.sound.SoundEvents;
import noxisclient.Alien;
import noxisclient.Vitality;
import noxisclient.api.events.impl.ClientTickEvent;
import noxisclient.api.events.impl.Render2DEvent;
import noxisclient.api.utils.Wrapper;
import noxisclient.api.utils.math.Animation;
import noxisclient.api.utils.math.Easing;
import noxisclient.core.impl.ShaderManager;
import noxisclient.mod.gui.click.HudEditorScreen;
import noxisclient.mod.gui.click.NewClickGuiScreen;
import noxisclient.mod.gui.click.SoundSelectScreen;
import noxisclient.mod.gui.items.buttons.Button;
import noxisclient.mod.modules.Module;
import noxisclient.mod.modules.settings.impl.BooleanSetting;
import noxisclient.mod.modules.settings.impl.ColorSetting;
import noxisclient.mod.modules.settings.impl.EnumSetting;
import noxisclient.mod.modules.settings.impl.SliderSetting;

public class ClickGui extends Module {
   private static ClickGui INSTANCE;
   public final EnumSetting<ClickGui.Style> style = new EnumSetting<>("Style", ClickGui.Style.Normal);
   public final BooleanSetting autoSave = this.add(new BooleanSetting("AutoSave", true));
   public final BooleanSetting shadow = this.add(new BooleanSetting("Shadow", true));
   public final BooleanSetting marquee = this.add(new BooleanSetting("Marquee", true));
   public final BooleanSetting disableNotification = this.add(new BooleanSetting("DisableNotification", false));
   public final BooleanSetting sound = this.add(new BooleanSetting("Sound", true).setParent());
   public final BooleanSetting guiSound = this.add(new BooleanSetting("GuiSound", true));
   public final SliderSetting soundPitch = this.add(new SliderSetting("SoundPitch", 1.0, 0.0, 2.0, 0.1, this.sound::isOpen));
   public final SliderSetting height = this.add(new SliderSetting("Height", 0, 0, 7));
   public final SliderSetting width = this.add(new SliderSetting("Width", 100, 80, 150));
   public final SliderSetting moduleGap = this.add(new SliderSetting("ModuleGap", 1.0, 0.0, 10.0, 0.5));
   public final SliderSetting titleGap = this.add(new SliderSetting("TitleGap", 2.0, 0.0, 30.0, 0.5));
   public final SliderSetting size = this.add(new SliderSetting("Size", 1.0, 0.5, 1.5, 0.05));
   public final SliderSetting textOffset = this.add(new SliderSetting("TextOffset", 0.0, -5.0, 5.0, 1.0));
   public final SliderSetting titleOffset = this.add(new SliderSetting("TitleOffset", 0.0, -5.0, 5.0, 1.0));
   public final BooleanSetting fade = this.add(new BooleanSetting("Fade", true).setParent());
   public final SliderSetting length = this.add(new SliderSetting("Length", 250.0, 0.0, 1000.0, this.fade::isOpen));
   public final EnumSetting<Easing> easing = this.add(new EnumSetting<>("Easing", Easing.ExpoInOut, this.fade::isOpen));
   public final BooleanSetting blur = this.add(new BooleanSetting("Blur", false));
   public final BooleanSetting shader = this.add(new BooleanSetting("Shader", false).setParent());
   public final SliderSetting shaderAlpha = this.add(new SliderSetting("ShaderAlpha", 255.0, 0.0, 255.0, 1.0, this.shader::isOpen));
   public final SliderSetting shaderSpeed = this.add(new SliderSetting("Speed", 4.0, 0.0, 20.0, 0.1, this.shader::isOpen));
   public final SliderSetting shaderSize = this.add(new SliderSetting("ShaderSize", 1.0, 0.1, 5.0, 0.05, this.shader::isOpen));
   public final SliderSetting enabledShaderAlpha = this.add(new SliderSetting("EnabledAlpha", 255.0, 0.0, 255.0, 1.0, this::isFullUiAlphaSettingOpen));
   public final SliderSetting disabledShaderAlpha = this.add(new SliderSetting("DisabledAlpha", 255.0, 0.0, 255.0, 1.0, this::isFullUiAlphaSettingOpen));
   public final BooleanSetting titleBarShader = this.add(new BooleanSetting("TitleBar", false, this.shader::isOpen));
   public final EnumSetting<ShaderManager.GuiShader> shaderMode = this.add(new EnumSetting<>("ShaderMode", ShaderManager.GuiShader.Flow, this.shader::isOpen));
   public final BooleanSetting elements = this.add(new BooleanSetting("Elements", false).setParent().injectTask(this::keyCodec));
   public final BooleanSetting border = this.add(new BooleanSetting("Line", false, this.elements::isOpen).setParent());
   public final ColorSetting borderColor = this.add(
      new ColorSetting("LineColor", new Color(106, 97, 173, 255), () -> this.elements.isOpen() && this.border.isOpen())
   );
   public final SliderSetting borderThickness = this.add(
      new SliderSetting("LineThickness", 1.0, 0.5, 3.0, 0.5, () -> this.elements.isOpen() && this.border.isOpen())
   );
   public final BooleanSetting gradientBorder = this.add(new BooleanSetting("GradientLine", false, () -> this.elements.isOpen() && this.border.isOpen()));
   public final ColorSetting borderColor2 = this.add(
      new ColorSetting("LineColor2", new Color(255, 255, 255, 255), () -> this.elements.isOpen() && this.border.isOpen() && this.gradientBorder.getValue())
   );
   public final BooleanSetting moduleExpand = this.add(new BooleanSetting("ModuleExpand", false, this.elements::isOpen));
   public final EnumSetting<ClickGui.SearchMode> searchMode = this.add(new EnumSetting<>("SearchMode", ClickGui.SearchMode.Normal, this.elements::isOpen));
   public final BooleanSetting colors = this.add(new BooleanSetting("Colors", false).setParent().injectTask(this::elementCodec));
   public final ColorSetting color = this.add(new ColorSetting("Color", new Color(106, 97, 173, 255), this.colors::isOpen));
   public final ColorSetting hoverColor = this.add(new ColorSetting("HoverColor", new Color(50, 50, 50, 200), this.colors::isOpen));
   public final ColorSetting defaultColor = this.add(new ColorSetting("DefaultColor", new Color(30, 30, 30, 236), this.colors::isOpen));
   public final ColorSetting defaultTextColor = this.add(new ColorSetting("DefaultTextColor", new Color(220, 220, 220), this.colors::isOpen));
   public final ColorSetting enableTextColor = this.add(new ColorSetting("EnableTextColor", new Color(255, 255, 255), this.colors::isOpen));
   public final ColorSetting backGround = this.add(new ColorSetting("BackGround", new Color(30, 30, 30, 236), this.colors::isOpen).injectBoolean(true));
   public final ColorSetting tint = this.add(new ColorSetting("Tint", new Color(12, 60, 95, 56)).injectBoolean(true).allowClientColor(true));
   public final ColorSetting endColor = this.add(new ColorSetting("End", new Color(255, 120, 240, 72), () -> this.tint.booleanValue));
   public final BooleanSetting glow = this.add(new BooleanSetting("Glow", false).setParent());
   public final SliderSetting glowIntensity = this.add(new SliderSetting("GlowIntensity", 1.0, 1.0, 10.0, 0.5, this.glow::isOpen));
   public final SliderSetting glowLayers = this.add(new SliderSetting("GlowLayers", 2.0, 2.0, 8.0, 1.0, this.glow::isOpen));
   public final BooleanSetting glowFrame = this.add(new BooleanSetting("GlowFrame", true, this.glow::isOpen));
   public final BooleanSetting glowButtons = this.add(new BooleanSetting("GlowButtons", true, this.glow::isOpen));
   public final BooleanSetting glowText = this.add(new BooleanSetting("GlowText", true, this.glow::isOpen));
   public final SliderSetting textGlowIntensity = this.add(new SliderSetting("TextGlowIntensity", 2.0, 1.0, 5.0, 0.5, this.glow::isOpen));
   public double alphaValue;
   private final Animation animation = new Animation();
   private boolean pendingOpen;

   public ClickGui() {
      super("ClickGui", Module.Category.Client);
      this.setChinese("点击界面");
      INSTANCE = this;
      Vitality.EVENT_BUS.subscribe(new ClickGui.FadeOut());
   }

   public static ClickGui getInstance() {
      return INSTANCE;
   }

   public void keyCodec() {
      this.elements.setValueWithoutTask(false);
      this.elements.setOpen(!this.elements.isOpen());
   }

   public void elementCodec() {
      this.colors.setValueWithoutTask(false);
      this.colors.setOpen(!this.colors.isOpen());
   }

   public void styleCodec() {
      if (this.style.getValue() == ClickGui.Style.Thunder) {
         this.applyThunderDefaults();
      } else if (this.style.getValue() == ClickGui.Style.Opal) {
         this.applyOpalDefaults();
      }
   }

   public void applyThunderDefaults() {
      this.color.setValue(new Color(-6974059, true));
      this.endColor.setValue(new Color(-8365735, true));
      this.hoverColor.setValue(new Color(905969664, true));
      this.defaultColor.setValue(new Color(570425344, true));
      this.defaultTextColor.setValue(new Color(-6052957, true));
      this.enableTextColor.setValue(new Color(-1, true));
      this.backGround.setValue(new Color(-419430400, true));
      this.backGround.booleanValue = true;
      this.borderColor.setValue(new Color(1107296256, true));
      this.borderColor2.setValue(new Color(-8365735, true));
   }

   public void applyOpalDefaults() {
      this.width.setValue(110.0);
      this.color.setValue(new Color(-13778946, true));
      this.endColor.setValue(new Color(-14378549, true));
      this.hoverColor.setValue(new Color(855638016, true));
      this.defaultColor.setValue(new Color(-1289871827, true));
      this.defaultTextColor.setValue(new Color(-3355444, true));
      this.enableTextColor.setValue(new Color(-1, true));
      this.backGround.setValue(new Color(-653324529, true));
      this.backGround.booleanValue = true;
      this.borderColor.setValue(new Color(855638016, true));
      this.borderColor2.setValue(new Color(-14378549, true));
   }

   @Override
   public void onEnable() {
      if (nullCheck()) {
         this.alphaValue = 1.0;
      }

      this.updateColor();
      if (this.guiSound.getValue() && mc.getSoundManager() != null) {
         mc.getSoundManager().play(PositionedSoundInstance.master(SoundEvents.UI_BUTTON_CLICK, this.soundPitch.getValueFloat()));
      }

      if (Alien.SHADER != null) {
         Alien.SHADER.reloadShaders();
      }

      if (mc.mouse != null && mc.isOnThread()) {
         this.openScreen();
      } else {
         this.pendingOpen = true;
      }
   }

   @Override
   public void onDisable() {
      this.pendingOpen = false;
      if (mc.currentScreen instanceof SoundSelectScreen || mc.currentScreen instanceof HudEditorScreen) {
         mc.setScreen(null);
      } else if (mc.currentScreen instanceof NewClickGuiScreen) {
         mc.currentScreen.close();
      }

      if (this.guiSound.getValue() && mc.getSoundManager() != null) {
         mc.getSoundManager().play(PositionedSoundInstance.master(SoundEvents.UI_BUTTON_CLICK, this.soundPitch.getValueFloat()));
      }

      if (this.autoSave.getValue()) {
         Vitality.save();
      }
   }

   public void onUpdate(ClientTickEvent event) {
      if (!event.isPost()) {
         if (this.pendingOpen) {
            if (mc.mouse == null || !mc.isOnThread()) {
               return;
            }

            this.pendingOpen = false;
            this.openScreen();
         }

         this.updateColor();
         if (!(mc.currentScreen instanceof NewClickGuiScreen)
            && !(mc.currentScreen instanceof SoundSelectScreen)
            && !(mc.currentScreen instanceof HudEditorScreen)) {
            this.disable();
         }
      }
   }

   private void openScreen() {
      Screen previousScreen = mc.currentScreen;
      mc.setScreen(NewClickGuiScreen.getInstance().openFrom(previousScreen));
   }

   public void updateColor() {
      Button.hoverColor = this.hoverColor.getValue().getRGB();
      Button.defaultTextColor = this.defaultTextColor.getValue().getRGB();
      Button.defaultColor = this.defaultColor.getValue().getRGB();
      Button.enableTextColor = this.enableTextColor.getValue().getRGB();
   }

   public void renderVanillaBlur(float delta) {
      if (this.blur.getValue() && mc != null && mc.gameRenderer != null && mc.getFramebuffer() != null) {
         mc.options.getMenuBackgroundBlurriness().setValue(7);
         mc.gameRenderer.renderBlur(delta);
         mc.getFramebuffer().beginWrite(false);
         mc.options.getMenuBackgroundBlurriness().setValue(5);
      }
   }

   public boolean isExhibitionStyle() {
      return this.style.getValue() == ClickGui.Style.Exhibition;
   }

   public boolean isThunderStyle() {
      return this.style.getValue() == ClickGui.Style.Thunder;
   }

   public boolean isOpalStyle() {
      return this.style.getValue() == ClickGui.Style.Opal;
   }

   public float getGuiCornerRadius() {
      if (this.style.getValue() != ClickGui.Style.Thunder && this.style.getValue() != ClickGui.Style.Opal) {
         return this.style.getValue() == ClickGui.Style.Rounded ? 6.0F : 0.0F;
      } else {
         return 5.0F;
      }
   }

   public int getThunderHeaderColor() {
      return this.backGround.getValue().getRGB();
   }

   public int getThunderPanelColor() {
      return this.backGround.getValue().getRGB();
   }

   public int getThunderRowColor(boolean active, boolean hovered) {
      if (active) {
         return this.getThunderColor(0);
      } else {
         return hovered ? this.hoverColor.getValue().getRGB() : this.defaultColor.getValue().getRGB();
      }
   }

   public int getThunderSettingColor(boolean active, boolean hovered) {
      if (active) {
         return this.withAlpha(this.getThunderColor(0), hovered ? 210 : 180);
      } else {
         return hovered ? this.hoverColor.getValue().getRGB() : 0;
      }
   }

   public int getThunderAccentColor() {
      return this.color.getValue().getRGB();
   }

   public int getThunderBorderColor() {
      return this.borderColor.getValue().getRGB();
   }

   public int getThunderTextColor(boolean primary) {
      return this.getTextColor(primary);
   }

   public int getThunderDangerColor() {
      return -41364;
   }

   public int getThunderColor(int count) {
      return switch (count) {
         case 90, 180, 270 -> this.endColor.getValue().getRGB();
         default -> this.color.getValue().getRGB();
      };
   }

   public int getThunderGradientStart(float opacity) {
      return this.withAlpha(this.getThunderColor(270), (int)(255.0F * Math.max(0.0F, Math.min(1.0F, opacity))));
   }

   public int getThunderGradientEnd(float opacity) {
      return this.withAlpha(this.getThunderColor(0), (int)(255.0F * Math.max(0.0F, Math.min(1.0F, opacity))));
   }

   public int getOpalHeaderColor() {
      return this.backGround.getValue().getRGB();
   }

   public int getOpalModuleBaseColor() {
      return this.defaultColor.getValue().getRGB();
   }

   public int getOpalTextColor(boolean primary) {
      return this.getTextColor(primary);
   }

   public int getDefaultTextColor() {
      return this.defaultTextColor.getValue().getRGB();
   }

   public int getEnableTextColor() {
      return this.enableTextColor.getValue().getRGB();
   }

   public int getTextColor(boolean primary) {
      return primary ? this.getEnableTextColor() : this.getDefaultTextColor();
   }

   public int getOpalGradientStart(float opacity) {
      return this.withAlpha(this.color.getValue().getRGB(), (int)(255.0F * Math.max(0.0F, Math.min(1.0F, opacity))));
   }

   public int getOpalGradientEnd(float opacity) {
      return this.withAlpha(this.endColor.getValue().getRGB(), (int)(255.0F * Math.max(0.0F, Math.min(1.0F, opacity))));
   }

   private int withAlpha(int color, int alpha) {
      return Math.max(0, Math.min(255, alpha)) << 24 | color & 16777215;
   }

   public int getExhibitionHeaderColor() {
      return this.color.getValue().getRGB();
   }

   public int getExhibitionSelectedColor() {
      return this.color.getValue().getRGB();
   }

   public int getExhibitionPanelColor() {
      return this.backGround.getValue().getRGB();
   }

   public int getExhibitionRowColor(boolean active, boolean hovered) {
      if (active) {
         return this.getExhibitionSelectedColor();
      } else {
         return hovered ? this.hoverColor.getValue().getRGB() : this.defaultColor.getValue().getRGB();
      }
   }

   public int getExhibitionSeparatorColor() {
      return this.borderColor.getValue().getRGB();
   }

   public int getExhibitionTextColor(boolean primary) {
      return this.getTextColor(primary);
   }

   public int getExhibitionIndicatorColor() {
      return this.getEnableTextColor();
   }

   public boolean useExhibitionSearchBox() {
      return this.isExhibitionStyle() || this.searchMode.is(ClickGui.SearchMode.Exhibition);
   }

   public boolean shouldRenderModuleShader(boolean moduleEnabled) {
      return this.shader.getValue();
   }

   public boolean shouldRenderTitleBarShader() {
      return this.shader.getValue() && this.titleBarShader.getValue();
   }

   public float getModuleShaderAlpha(boolean moduleEnabled) {
      SliderSetting alphaSetting = moduleEnabled ? this.enabledShaderAlpha : this.disabledShaderAlpha;
      return Math.max(0.0F, Math.min(1.0F, alphaSetting.getValueFloat() / 255.0F));
   }

   public float getShaderAlpha() {
      return Math.max(0.0F, Math.min(1.0F, this.shaderAlpha.getValueFloat() / 255.0F));
   }

   private boolean isFullUiAlphaSettingOpen() {
      return this.shader.isOpen();
   }

   public class FadeOut {
      public void onRender2D(Render2DEvent event) {
         if (ClickGui.this.fade.getValue()) {
            if (ClickGui.this.alphaValue > 0.0 || ClickGui.this.isOn()) {
               ClickGui.this.alphaValue = ClickGui.this.animation
                  .get(ClickGui.this.isOn() ? 1.0 : 0.0, (long)ClickGui.this.length.getValueInt(), ClickGui.this.easing.getValue());
            }

            if (ClickGui.this.alphaValue > 0.0
               && !(Wrapper.mc.currentScreen instanceof NewClickGuiScreen)
               && !(Wrapper.mc.currentScreen instanceof SoundSelectScreen)
               && !(Wrapper.mc.currentScreen instanceof HudEditorScreen)) {
               if (!Wrapper.mc.isWindowFocused()) {
                  return;
               }

               event.drawContext.getMatrices().push();
               event.drawContext.getMatrices().translate(0.0F, 0.0F, 5000.0F);
               NewClickGuiScreen.getInstance().render(event.drawContext, 0, 0, event.tickDelta);
               event.drawContext.getMatrices().pop();
            }
         } else {
            ClickGui.this.alphaValue = 1.0;
         }
      }
   }

   public static enum SearchMode {
      Normal,
      Exhibition;
   }

   public static enum Style {
      Normal,
      Rounded,
      Exhibition,
      Thunder,
      Opal;
   }
}
