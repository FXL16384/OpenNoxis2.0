package noxisclient.mod.modules.impl.render;

import java.awt.Color;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LightningEntity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.MathHelper;
import noxisclient.Alien;
import noxisclient.asm.accessors.IGameRenderer;
import noxisclient.core.impl.ShaderManager;
import noxisclient.mod.modules.Module;
import noxisclient.mod.modules.settings.impl.BooleanSetting;
import noxisclient.mod.modules.settings.impl.ColorSetting;
import noxisclient.mod.modules.settings.impl.EnumSetting;
import noxisclient.mod.modules.settings.impl.SliderSetting;

public class ShaderModule extends Module {
   public static ShaderModule INSTANCE;
   private final EnumSetting<ShaderModule.Page> page = this.add(new EnumSetting<>("Page", ShaderModule.Page.Shader));
   public final EnumSetting<ShaderManager.Shader> mode = this.add(
      new EnumSetting<>("Mode", ShaderManager.Shader.Flow, () -> this.page.getValue() == ShaderModule.Page.Shader)
   );
   public final SliderSetting speed = this.add(
      new SliderSetting(
         "Speed", 4.0, 0.0, 20.0, 0.1, () -> this.page.getValue() == ShaderModule.Page.Shader && this.mode.getValue() != ShaderManager.Shader.Bloom
      )
   );
   public final ColorSetting fill = this.add(
      new ColorSetting("Color", () -> this.page.getValue() == ShaderModule.Page.Shader && this.mode.getValue() != ShaderManager.Shader.Bloom)
   );
   public final SliderSetting maxSample = this.add(
      new SliderSetting(
         "MaxSample", 10.0, 0.0, 20.0, () -> this.page.getValue() == ShaderModule.Page.Shader && this.mode.getValue() != ShaderManager.Shader.Bloom
      )
   );
   public final SliderSetting divider = this.add(
      new SliderSetting(
         "Divider", 150.0, 0.0, 300.0, () -> this.page.getValue() == ShaderModule.Page.Shader && this.mode.getValue() != ShaderManager.Shader.Bloom
      )
   );
   public final SliderSetting radius = this.add(
      new SliderSetting("Radius", 2.0, 0.0, 6.0, () -> this.page.getValue() == ShaderModule.Page.Shader && this.mode.getValue() != ShaderManager.Shader.Bloom)
   );
   public final SliderSetting smoothness = this.add(
      new SliderSetting(
         "Smoothness", 1.0, 0.0, 1.0, 0.01, () -> this.page.getValue() == ShaderModule.Page.Shader && this.mode.getValue() != ShaderManager.Shader.Bloom
      )
   );
   public final SliderSetting alpha = this.add(
      new SliderSetting("GlowAlpha", 255, 0, 255, () -> this.page.getValue() == ShaderModule.Page.Shader && this.mode.getValue() == ShaderManager.Shader.Bloom)
   );
   public final SliderSetting bloomRadius = this.add(
      new SliderSetting("Width", 1, 0, 10, () -> this.page.getValue() == ShaderModule.Page.Shader && this.mode.getValue() == ShaderManager.Shader.Bloom)
   );
   public final SliderSetting bloomIntensity = this.add(
      new SliderSetting(
         "Glow", 1.0, 0.0, 5.0, 0.1, () -> this.page.getValue() == ShaderModule.Page.Shader && this.mode.getValue() == ShaderManager.Shader.Bloom
      )
   );
   public final SliderSetting bloomQuality = this.add(
      new SliderSetting("Quality", 1, 1, 6, () -> this.page.getValue() == ShaderModule.Page.Shader && this.mode.getValue() == ShaderManager.Shader.Bloom)
   );
   public final ColorSetting bloomFill = this.add(
      new ColorSetting(
            "Fill", new Color(189, 153, 255), () -> this.page.getValue() == ShaderModule.Page.Shader && this.mode.getValue() == ShaderManager.Shader.Bloom
         )
         .alphaEnabled(false)
   );
   public final ColorSetting bloomOutline = this.add(
      new ColorSetting(
            "Outline", new Color(189, 153, 255), () -> this.page.getValue() == ShaderModule.Page.Shader && this.mode.getValue() == ShaderManager.Shader.Bloom
         )
         .alphaEnabled(false)
   );
   public final SliderSetting bloomFillAlpha = this.add(
      new SliderSetting("FillAlpha", 120, 0, 255, () -> this.page.getValue() == ShaderModule.Page.Shader && this.mode.getValue() == ShaderManager.Shader.Bloom)
   );
   public final BooleanSetting bloomFastLines = this.add(
      new BooleanSetting("FastLines", false, () -> this.page.getValue() == ShaderModule.Page.Shader && this.mode.getValue() == ShaderManager.Shader.Bloom)
   );
   public final SliderSetting maxRange = this.add(new SliderSetting("MaxRange", 64, 16, 512, () -> this.page.getValue() == ShaderModule.Page.Target));
   public final SliderSetting pulseSpeed = this.add(
      new SliderSetting(
         "PulseSize", 10.0, 0.0, 200.0, 0.1, () -> this.page.getValue() == ShaderModule.Page.Color && this.mode.getValue() != ShaderManager.Shader.Bloom
      )
   );
   public final ColorSetting pulse = this.add(
      new ColorSetting(
         "Pulse", new Color(255, 255, 255), () -> this.page.getValue() == ShaderModule.Page.Color && this.mode.getValue() != ShaderManager.Shader.Bloom
      )
   );
   public final SliderSetting step = this.add(
      new SliderSetting(
         "Step", 0.5, 0.0, 4.0, 0.01F, () -> this.page.getValue() == ShaderModule.Page.Color && this.mode.getValue() != ShaderManager.Shader.Bloom
      )
   );
   public final SliderSetting octaves = this.add(
      new SliderSetting("Octaves", 10, 5, 30, () -> this.page.getValue() == ShaderModule.Page.Color && this.mode.getValue() != ShaderManager.Shader.Bloom)
   );
   public final ColorSetting smoke1 = this.add(
      new ColorSetting(
         "Color1", new Color(255, 255, 255), () -> this.page.getValue() == ShaderModule.Page.Color && this.mode.getValue() != ShaderManager.Shader.Bloom
      )
   );
   public final ColorSetting smoke2 = this.add(
      new ColorSetting(
         "Color2", new Color(255, 255, 255), () -> this.page.getValue() == ShaderModule.Page.Color && this.mode.getValue() != ShaderManager.Shader.Bloom
      )
   );
   public final ColorSetting smoke3 = this.add(
      new ColorSetting(
         "Color3", new Color(255, 255, 255), () -> this.page.getValue() == ShaderModule.Page.Color && this.mode.getValue() != ShaderManager.Shader.Bloom
      )
   );
   public final ColorSetting smoke4 = this.add(
      new ColorSetting(
         "Color4", new Color(255, 255, 255), () -> this.page.getValue() == ShaderModule.Page.Color && this.mode.getValue() != ShaderManager.Shader.Bloom
      )
   );
   private final BooleanSetting hands = this.add(new BooleanSetting("Hands", true, () -> this.page.getValue() == ShaderModule.Page.Target));
   private final BooleanSetting lighting = this.add(new BooleanSetting("Lighting", false, () -> this.page.getValue() == ShaderModule.Page.Target));
   private final BooleanSetting self = this.add(new BooleanSetting("Self", true, () -> this.page.getValue() == ShaderModule.Page.Target));
   private final BooleanSetting players = this.add(new BooleanSetting("Players", true, () -> this.page.getValue() == ShaderModule.Page.Target));
   private final BooleanSetting friends = this.add(new BooleanSetting("Friends", true, () -> this.page.getValue() == ShaderModule.Page.Target));
   private final BooleanSetting crystals = this.add(new BooleanSetting("Crystals", true, () -> this.page.getValue() == ShaderModule.Page.Target));
   private final BooleanSetting creatures = this.add(new BooleanSetting("Creatures", false, () -> this.page.getValue() == ShaderModule.Page.Target));
   private final BooleanSetting monsters = this.add(new BooleanSetting("Monsters", false, () -> this.page.getValue() == ShaderModule.Page.Target));
   private final BooleanSetting ambients = this.add(new BooleanSetting("Ambients", false, () -> this.page.getValue() == ShaderModule.Page.Target));
   private final BooleanSetting items = this.add(new BooleanSetting("Items", true, () -> this.page.getValue() == ShaderModule.Page.Target));
   private final BooleanSetting others = this.add(new BooleanSetting("Others", false, () -> this.page.getValue() == ShaderModule.Page.Target));

   public ShaderModule() {
      super("Shader", Module.Category.Render);
      this.setChinese("着色器");
      INSTANCE = this;
   }

   @Override
   public String getInfo() {
      return this.mode.getValue().name();
   }

   public boolean shouldRenderLighting() {
      return this.isOn() && this.lighting.getValue();
   }

   public boolean shouldRender(Entity entity) {
      if (entity == null) {
         return false;
      } else if (mc.player == null) {
         return false;
      } else if ((double)MathHelper.sqrt((float)mc.player.squaredDistanceTo(entity.getPos())) > this.maxRange.getValue()) {
         return false;
      } else if (entity instanceof PlayerEntity playerEntity) {
         if (entity == mc.player) {
            return this.self.getValue();
         } else {
            return Alien.FRIEND.isFriend(playerEntity) ? this.friends.getValue() : this.players.getValue();
         }
      } else if (entity instanceof EndCrystalEntity) {
         return this.crystals.getValue();
      } else if (entity instanceof LightningEntity) {
         return this.lighting.getValue();
      } else if (entity instanceof ItemEntity) {
         return this.items.getValue();
      } else {
         return switch (entity.getType().getSpawnGroup()) {
            case CREATURE, WATER_CREATURE -> this.creatures.getValue();
            case MONSTER -> this.monsters.getValue();
            case AMBIENT, WATER_AMBIENT -> this.ambients.getValue();
            default -> this.others.getValue();
         };
      }
   }

   @Override
   public void onRender3D(MatrixStack matrixStack) {
      if (this.hands.getValue()) {
         Alien.SHADER
            .renderShader(
               () -> ((IGameRenderer)mc.gameRenderer)
                     .IRenderHand(mc.gameRenderer.getCamera(), mc.getRenderTickCounter().getTickDelta(true), matrixStack.peek().getPositionMatrix()),
               this.mode.getValue()
            );
      }
   }

   @Override
   public void onToggle() {
      Alien.SHADER.reloadShaders();
   }

   @Override
   public void onLogin() {
      Alien.SHADER.reloadShaders();
   }

   private static enum Page {
      Shader,
      Target,
      Color;
   }
}
