package noxisclient.mod.modules.impl.render;

import java.awt.Color;
import noxisclient.Alien;
import noxisclient.core.impl.ShaderManager;
import noxisclient.mod.modules.Module;
import noxisclient.mod.modules.settings.impl.BooleanSetting;
import noxisclient.mod.modules.settings.impl.ColorSetting;
import noxisclient.mod.modules.settings.impl.EnumSetting;
import noxisclient.mod.modules.settings.impl.SliderSetting;

public class SkyBox extends Module {
   public static SkyBox INSTANCE;
   public final EnumSetting<ShaderManager.SkyShader> mode = this.add(new EnumSetting<>("Mode", ShaderManager.SkyShader.CosmicHorizon));
   public final SliderSetting speed = this.add(new SliderSetting("Speed", 4.0, 0.0, 20.0, 0.1, () -> !this.mode.is(ShaderManager.SkyShader.SolidColor)));
   public final ColorSetting color = this.add(new ColorSetting("Color", new Color(255, 255, 255, 255)));
   public final ColorSetting secondColor = this.add(
      new ColorSetting(
         "SecondColor",
         new Color(255, 255, 255, 255),
         () -> this.mode.is(ShaderManager.SkyShader.GradientFade) || this.mode.is(ShaderManager.SkyShader.MonolithFlow)
      )
   );
   public final SliderSetting step = this.add(new SliderSetting("Step", 0.5, 0.0, 4.0, 0.01F, () -> this.mode.is(ShaderManager.SkyShader.GradientFade)));
   public final SliderSetting resolutionScale = this.add(
      new SliderSetting("ResolutionScale", 0.75, 0.25, 1.0, 0.05, () -> !this.mode.is(ShaderManager.SkyShader.SolidColor))
   );
   public final SliderSetting shaderScale = this.add(
      new SliderSetting("ShaderScale", 1.0, 0.1, 3.0, 0.01F, () -> this.mode.is(ShaderManager.SkyShader.MonolithFlow))
   );
   public final SliderSetting shaderIntensity = this.add(
      new SliderSetting("ShaderIntensity", 0.005, 0.001, 0.03, 5.0E-4F, () -> this.mode.is(ShaderManager.SkyShader.MonolithFlow))
   );
   public final BooleanSetting mouseFollow = this.add(new BooleanSetting("MouseFollow", false, () -> this.mode.is(ShaderManager.SkyShader.MonolithFlow)));

   public SkyBox() {
      super("SkyBox", "Render shader skybox.", Module.Category.Render);
      this.setChinese("天空");
      INSTANCE = this;
   }

   @Override
   public String getInfo() {
      return this.mode.getValue().name();
   }

   @Override
   public void onToggle() {
      Alien.SHADER.reloadShaders();
   }

   @Override
   public void onLogin() {
      Alien.SHADER.reloadShaders();
   }
}
