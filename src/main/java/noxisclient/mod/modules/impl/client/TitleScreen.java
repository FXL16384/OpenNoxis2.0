package noxisclient.mod.modules.impl.client;

import noxisclient.Alien;
import noxisclient.mod.modules.Module;
import noxisclient.mod.modules.settings.impl.BooleanSetting;
import noxisclient.mod.modules.settings.impl.EnumSetting;

public class TitleScreen extends Module {
   public static TitleScreen INSTANCE;
   public final EnumSetting<TitleScreen.MenuShader> shader = this.add(new EnumSetting<>("Shader", TitleScreen.MenuShader.TitleImage));
   public final BooleanSetting mouseFollow = this.add(new BooleanSetting("MouseFollow", false, this::supportsMouseFollow));
   private long shaderStartTime = System.currentTimeMillis();

   public TitleScreen() {
      super("TitleScreen", Module.Category.Client);
      this.setChinese("Main Menu");
      INSTANCE = this;
   }

   public static boolean shouldRender() {
      return INSTANCE != null && INSTANCE.isOn();
   }

   public TitleScreen.MenuShader getShaderMode() {
      TitleScreen.MenuShader mode = this.shader.getValue();
      return mode == null ? TitleScreen.MenuShader.TitleImage : mode;
   }

   public float getRenderTime() {
      return (float)(System.currentTimeMillis() - this.shaderStartTime) / 1000.0F;
   }

   public boolean shouldMouseFollow() {
      return this.mouseFollow.getValue();
   }

   private boolean supportsMouseFollow() {
      TitleScreen.MenuShader mode = this.getShaderMode();
      return mode == TitleScreen.MenuShader.TitleImage || mode == TitleScreen.MenuShader.Heartfelt || mode == TitleScreen.MenuShader.MonolithFlow;
   }

   @Override
   public String getInfo() {
      return this.getShaderMode().name();
   }

   @Override
   public void onToggle() {
      this.shaderStartTime = System.currentTimeMillis();
      if (Alien.SHADER != null) {
         Alien.SHADER.reloadShaders();
      }
   }

   public static enum MenuShader {
      TitleImage,
      Heartfelt,
      MonolithFlow,
      NebulaOrb,
      NetworkMesh,
      PhobosColorful,
      PhobosFract,
      PhobosGrid;
   }
}
