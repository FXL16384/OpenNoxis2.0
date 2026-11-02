package noxisclient.mod.modules.impl.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import noxisclient.core.impl.FontManager;
import noxisclient.mod.modules.Module;
import noxisclient.mod.modules.settings.impl.BooleanSetting;
import noxisclient.mod.modules.settings.impl.EnumSetting;
import noxisclient.mod.modules.settings.impl.SliderSetting;
import noxisclient.mod.modules.settings.impl.StringSetting;
import org.lwjgl.glfw.GLFW;

public class Fonts extends Module {
   public static Fonts INSTANCE;
   private boolean pendingRefresh;
   private final EnumSetting<Fonts.Page> page = this.add(new EnumSetting<>("Page", Fonts.Page.General));
   public final StringSetting fontName = this.add(new StringSetting("Font", "msyh", () -> this.isPage(Fonts.Page.General)).injectTask(this::refresh));
   public final EnumSetting<Fonts.FontStyle> style = this.add(
      new EnumSetting<>("Style", Fonts.FontStyle.PLAIN, () -> this.isPage(Fonts.Page.General)).injectTask(this::refresh)
   );
   public final BooleanSetting smooth = this.add(new BooleanSetting("Smooth", true, () -> this.isPage(Fonts.Page.General)).injectTask(this::refresh));
   public final EnumSetting<Fonts.AntiAliasMode> antiAliasMode = this.add(
      new EnumSetting<>("AntiAliasMode", Fonts.AntiAliasMode.Default, () -> this.isPage(Fonts.Page.General)).injectTask(this::refresh)
   );
   public final SliderSetting size = this.add(new SliderSetting("Size", 8.0, 1.0, 30.0, 1.0, () -> this.isPage(Fonts.Page.General)).injectTask(this::refresh));
   public final SliderSetting shift = this.add(new SliderSetting("Shift", 0.0, -10.0, 10.0, 1.0, () -> this.isPage(Fonts.Page.General)));
   public final EnumSetting<Fonts.ShadowMode> shadowMode = this.add(
      new EnumSetting<>("ShadowMode", Fonts.ShadowMode.Default, () -> this.isPage(Fonts.Page.General))
   );
   public final SliderSetting shadowOffset = this.add(
      new SliderSetting("ShadowOffset", 1.0, 0.0, 5.0, 0.1, () -> this.isPage(Fonts.Page.General) && this.shadowMode.is(Fonts.ShadowMode.Default))
   );
   public final SliderSetting translate = this.add(new SliderSetting("Translate", 0.0, -10.0, 10.0, 1.0, () -> this.isPage(Fonts.Page.General)));
   public final BooleanSetting all = this.add(new BooleanSetting("All", false, () -> this.isPage(Fonts.Page.Module)).injectTask(this::setAllModuleFonts));
   public final BooleanSetting clickGui = this.add(new BooleanSetting("ClickGui", true, () -> this.isPage(Fonts.Page.Module)));
   public final BooleanSetting hud = this.add(new BooleanSetting("HUD", true, () -> this.isPage(Fonts.Page.Module)));
   public final BooleanSetting nameTags = this.add(new BooleanSetting("NameTags", true, () -> this.isPage(Fonts.Page.Module)));
   public final BooleanSetting textRadar = this.add(new BooleanSetting("TextRadar", true, () -> this.isPage(Fonts.Page.Module)));
   public final BooleanSetting tips = this.add(new BooleanSetting("Tips", true, () -> this.isPage(Fonts.Page.Module)));
   public final BooleanSetting render = this.add(new BooleanSetting("Render", true, () -> this.isPage(Fonts.Page.Module)));

   public Fonts() {
      super("Fonts", Module.Category.Client);
      this.setChinese("字体");
      INSTANCE = this;
   }

   @Override
   public void onEnable() {
      this.refresh();
   }

   public void refresh() {
      if (!this.canReloadNow()) {
         this.pendingRefresh = true;
      } else {
         try {
            this.pendingRefresh = false;
            FontManager.INSTANCE.reload();
         } catch (Exception var2) {
            var2.printStackTrace();
         }
      }
   }

   public void refreshIfPending() {
      if (this.pendingRefresh) {
         this.refresh();
      }
   }

   private boolean canReloadNow() {
      try {
         MinecraftClient mc = MinecraftClient.getInstance();
         return mc != null && mc.getWindow() != null && RenderSystem.isOnRenderThread() && GLFW.glfwGetCurrentContext() != 0L;
      } catch (Throwable var2) {
         return false;
      }
   }

   private boolean isPage(Fonts.Page page) {
      return this.page.getValue() == page;
   }

   private void setAllModuleFonts() {
      boolean enabled = this.all.getValue();
      this.clickGui.setValue(enabled);
      this.hud.setValue(enabled);
      this.nameTags.setValue(enabled);
      this.textRadar.setValue(enabled);
      this.tips.setValue(enabled);
      this.render.setValue(enabled);
   }

   public static boolean isCustomFontEnabled() {
      return INSTANCE != null && INSTANCE.isOn();
   }

   private static boolean useModuleFont(BooleanSetting setting) {
      return isCustomFontEnabled() && setting.getValue();
   }

   public static boolean useClickGuiFont() {
      return INSTANCE != null && useModuleFont(INSTANCE.clickGui);
   }

   public static boolean useHudFont() {
      return INSTANCE != null && useModuleFont(INSTANCE.hud);
   }

   public static boolean useNameTagsFont() {
      return INSTANCE != null && useModuleFont(INSTANCE.nameTags);
   }

   public static boolean useTextRadarFont() {
      return INSTANCE != null && useModuleFont(INSTANCE.textRadar);
   }

   public static boolean useTipsFont() {
      return INSTANCE != null && useModuleFont(INSTANCE.tips);
   }

   public static boolean useRenderFont() {
      return INSTANCE != null && useModuleFont(INSTANCE.render);
   }

   public static enum AntiAliasMode {
      Default,
      Hachimi;
   }

   public static enum FontStyle {
      PLAIN(0),
      BOLD(1),
      ITALIC(2),
      BOLDITALIC(3);

      public final int awtStyle;

      private FontStyle(int awtStyle) {
         this.awtStyle = awtStyle;
      }
   }

   private static enum Page {
      General,
      Module;
   }

   public static enum ShadowMode {
      Default,
      Shoreline;
   }
}
