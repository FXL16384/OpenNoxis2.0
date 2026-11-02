package noxisclient.mod.modules.impl.misc;

import com.mojang.blaze3d.systems.RenderSystem;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.render.VertexFormat.DrawMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LightningEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;
import noxisclient.api.events.impl.ClientTickEvent;
import noxisclient.api.events.impl.DeathEvent;
import noxisclient.api.events.impl.Render3DEvent;
import noxisclient.api.utils.math.MathUtil;
import noxisclient.mod.modules.Module;
import noxisclient.mod.modules.impl.render.LogoutSpots;
import noxisclient.mod.modules.settings.impl.BooleanSetting;
import noxisclient.mod.modules.settings.impl.EnumSetting;
import noxisclient.mod.modules.settings.impl.SliderSetting;
import org.joml.Matrix4f;

public class KillEffect extends Module {
   private final EnumSetting<KillEffect.KillEffectMode> mode = this.add(new EnumSetting<>("Mode", KillEffect.KillEffectMode.DEFAULT));
   private final BooleanSetting sound = this.add(new BooleanSetting("Sound", true).setParent());
   private final EnumSetting<LogoutSpots.MioSound> soundPath = this.add(new EnumSetting<>("SoundPath", LogoutSpots.MioSound.DOUBLEKILL, this.sound::isOpen));
   private final BooleanSetting lightning = this.add(new BooleanSetting("Lightning", true, () -> this.mode.getValue() == KillEffect.KillEffectMode.DEFAULT));
   private final BooleanSetting levelUp = this.add(
      new BooleanSetting("LevelUp", true, () -> this.mode.getValue() == KillEffect.KillEffectMode.DEFAULT).setParent()
   );
   private final SliderSetting lMaxPitch = this.add(
      new SliderSetting("LMaxPitch", 1.0, 0.0, 2.0, 0.1, () -> this.mode.getValue() == KillEffect.KillEffectMode.DEFAULT && this.levelUp.isOpen())
   );
   private final SliderSetting lMinPitch = this.add(
      new SliderSetting("LMinPitch", 1.0, 0.0, 2.0, 0.1, () -> this.mode.getValue() == KillEffect.KillEffectMode.DEFAULT && this.levelUp.isOpen())
   );
   private final BooleanSetting trident = this.add(
      new BooleanSetting("Trident", false, () -> this.mode.getValue() == KillEffect.KillEffectMode.DEFAULT).setParent()
   );
   private final SliderSetting tMaxPitch = this.add(
      new SliderSetting("TMaxPitch", 1.0, 0.0, 2.0, 0.1, () -> this.mode.getValue() == KillEffect.KillEffectMode.DEFAULT && this.trident.isOpen())
   );
   private final SliderSetting tMinPitch = this.add(
      new SliderSetting("TMinPitch", 1.0, 0.0, 2.0, 0.1, () -> this.mode.getValue() == KillEffect.KillEffectMode.DEFAULT && this.trident.isOpen())
   );
   private final SliderSetting factor = this.add(
      new SliderSetting("Factor", 1.0, 1.0, 10.0, 1.0, () -> this.mode.getValue() == KillEffect.KillEffectMode.DEFAULT)
   );
   private final SliderSetting duration = this.add(
      new SliderSetting("Duration", 3.0, 1.0, 10.0, 0.1, () -> this.mode.getValue() == KillEffect.KillEffectMode.HereHasALowiqDie)
   );
   private final SliderSetting scale = this.add(
      new SliderSetting("Scale", 1.0, 0.1, 3.0, 0.1, () -> this.mode.getValue() == KillEffect.KillEffectMode.HereHasALowiqDie)
   );
   private final BooleanSetting flash = this.add(new BooleanSetting("Flash", true, () -> this.mode.getValue() == KillEffect.KillEffectMode.HereHasALowiqDie));
   private final List<KillEffect.DeathEffect> deathEffects = new ArrayList<>();

   public KillEffect() {
      super("KillEffect", Module.Category.Misc);
      this.setChinese("击杀效果");
   }

   public void onPlayerDeath(DeathEvent event) {
      if (!nullCheck()) {
         PlayerEntity player = event.getPlayer();
         if (player != null) {
            if (this.sound.getValue()) {
               LogoutSpots.playMioSound(this.soundPath.getValue());
            }

            if (this.mode.getValue() == KillEffect.KillEffectMode.DEFAULT) {
               for (int i = 0; (double)i < this.factor.getValue(); i++) {
                  this.doEffect(player);
               }
            } else if (this.mode.getValue() == KillEffect.KillEffectMode.HereHasALowiqDie) {
               boolean exists = false;

               for (KillEffect.DeathEffect effect : this.deathEffects) {
                  if (Math.abs(effect.x - player.getX()) < 0.5 && Math.abs(effect.y - player.getY()) < 0.5 && Math.abs(effect.z - player.getZ()) < 0.5) {
                     exists = true;
                     break;
                  }
               }

               if (!exists) {
                  this.deathEffects.add(new KillEffect.DeathEffect(player.getX(), player.getY() + 1.0, player.getZ(), System.currentTimeMillis()));
               }
            }
         }
      }
   }

   public void onTick(ClientTickEvent event) {
      if (mc.world != null && this.mode.getValue() == KillEffect.KillEffectMode.HereHasALowiqDie) {
         this.deathEffects.removeIf(effect -> (double)(System.currentTimeMillis() - effect.timestamp) > this.duration.getValue() * 1000.0);
      }
   }

   public void onRender3D(Render3DEvent event) {
      if (mc.world != null && this.mode.getValue() == KillEffect.KillEffectMode.HereHasALowiqDie) {
         for (KillEffect.DeathEffect effect : this.deathEffects) {
            if (this.flash.getValue()) {
               if (System.currentTimeMillis() / 200L % 2L == 0L) {
                  this.drawEffect(event.matrixStack, effect.x, effect.y, effect.z, 1.0F);
               }
            } else {
               this.drawEffect(event.matrixStack, effect.x, effect.y, effect.z, 1.0F);
            }
         }
      }
   }

   private void doEffect(PlayerEntity player) {
      double x = player.getX();
      double y = player.getY();
      double z = player.getZ();
      if (this.lightning.getValue()) {
         LightningEntity lightningEntity = new LightningEntity(EntityType.LIGHTNING_BOLT, mc.world);
         lightningEntity.updatePosition(x, y, z);
         lightningEntity.refreshPositionAfterTeleport(x, y, z);
         mc.world.addEntity(lightningEntity);
      }

      if (this.levelUp.getValue()) {
         mc.world
            .playSound(
               mc.player,
               x,
               y,
               z,
               SoundEvents.ENTITY_PLAYER_LEVELUP,
               SoundCategory.PLAYERS,
               100.0F,
               MathUtil.random(this.lMinPitch.getValueFloat(), this.lMaxPitch.getValueFloat())
            );
      }

      if (this.trident.getValue()) {
         mc.world
            .playSound(
               mc.player,
               x,
               y,
               z,
               SoundEvents.ITEM_TRIDENT_THUNDER,
               SoundCategory.MASTER,
               999.0F,
               MathUtil.random(this.tMinPitch.getValueFloat(), this.tMaxPitch.getValueFloat())
            );
      }
   }

   private void drawEffect(MatrixStack matrixStack, double x, double y, double z, float alpha) {
      matrixStack.push();
      double cameraX = mc.gameRenderer.getCamera().getPos().x;
      double cameraY = mc.gameRenderer.getCamera().getPos().y;
      double cameraZ = mc.gameRenderer.getCamera().getPos().z;
      matrixStack.translate(x - cameraX, y - cameraY, z - cameraZ);
      matrixStack.multiply(mc.gameRenderer.getCamera().getRotation());
      double scaleValue = 0.0245 * this.scale.getValue();
      matrixStack.scale((float)scaleValue, (float)scaleValue, (float)scaleValue);
      Identifier yuanquanTexture = Identifier.of("noxisclient", "textures/killeffect/yuanquan.png");
      RenderSystem.setShaderTexture(0, yuanquanTexture);
      RenderSystem.enableBlend();
      RenderSystem.defaultBlendFunc();
      RenderSystem.disableCull();
      RenderSystem.disableDepthTest();
      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, alpha);
      this.drawTexture(matrixStack, -32.0F, -32.0F, 64.0F, 64.0F);
      Identifier jiantouTexture = Identifier.of("noxisclient", "textures/killeffect/jiantou.png");
      RenderSystem.setShaderTexture(0, jiantouTexture);
      this.drawTexture(matrixStack, -5.0F, -5.0F, 64.0F, 64.0F);
      RenderSystem.enableCull();
      RenderSystem.enableDepthTest();
      matrixStack.pop();
   }

   private void drawTexture(MatrixStack matrices, float x, float y, float width, float height) {
      RenderSystem.setShader(GameRenderer::getPositionTexProgram);
      BufferBuilder bufferBuilder = Tessellator.getInstance().begin(DrawMode.QUADS, VertexFormats.POSITION_TEXTURE);
      Matrix4f matrix = matrices.peek().getPositionMatrix();
      bufferBuilder.vertex(matrix, x, y + height, 0.0F).texture(0.0F, 0.0F);
      bufferBuilder.vertex(matrix, x + width, y + height, 0.0F).texture(1.0F, 0.0F);
      bufferBuilder.vertex(matrix, x + width, y, 0.0F).texture(1.0F, 1.0F);
      bufferBuilder.vertex(matrix, x, y, 0.0F).texture(0.0F, 1.0F);
      BufferRenderer.drawWithGlobalProgram(bufferBuilder.end());
   }

   private static class DeathEffect {
      public final double x;
      public final double y;
      public final double z;
      public final long timestamp;

      public DeathEffect(double x, double y, double z, long timestamp) {
         this.x = x;
         this.y = y;
         this.z = z;
         this.timestamp = timestamp;
      }
   }

   public static enum KillEffectMode {
      DEFAULT,
      HereHasALowiqDie;
   }
}
