package noxisclient.core.impl;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.GlStateManager.DstFactor;
import com.mojang.blaze3d.platform.GlStateManager.SrcFactor;
import com.mojang.blaze3d.systems.RenderSystem;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.PostEffectProcessor;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.render.VertexFormat.DrawMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import noxisclient.api.interfaces.IShaderEffectHook;
import noxisclient.api.utils.Wrapper;
import noxisclient.api.utils.math.Timer;
import noxisclient.api.utils.render.Render2DUtil;
import noxisclient.mod.modules.impl.client.ClickGui;
import noxisclient.mod.modules.impl.client.TitleScreen;
import noxisclient.mod.modules.impl.render.ShaderModule;
import noxisclient.mod.modules.impl.render.SkyBox;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.ladysnake.satin.api.managed.ManagedShaderEffect;
import org.ladysnake.satin.api.managed.ShaderEffectManager;
import org.lwjgl.opengl.GL11;

public class ShaderManager implements Wrapper {
   static final Timer timer = new Timer();
   private static final String MOD_ID = "noxisclient";
   private static final float TITLE_IMAGE_WIDTH = 2560.0F;
   private static final float TITLE_IMAGE_HEIGHT = 1208.0F;
   private static final List<ShaderManager.RenderTask> tasks = new ArrayList<>();
   public static ManagedShaderEffect DEFAULT_OUTLINE;
   public static ManagedShaderEffect GRADIENT_OUTLINE;
   public static ManagedShaderEffect FLOW_OUTLINE;
   public static ManagedShaderEffect RAINBOW_OUTLINE;
   public static ManagedShaderEffect BLOOM_OUTLINE;
   public static ManagedShaderEffect DEFAULT;
   public static ManagedShaderEffect GRADIENT;
   public static ManagedShaderEffect FLOW;
   public static ManagedShaderEffect RAINBOW;
   public static ManagedShaderEffect BLOOM;
   public static ManagedShaderEffect COSMIC_HORIZON;
   public static ManagedShaderEffect MONOLITH_FLOW;
   public static ManagedShaderEffect NEBULA_ORB;
   public static ManagedShaderEffect NETWORK_MESH;
   public static ManagedShaderEffect SPHERE_TUNNEL;
   public static ManagedShaderEffect ISM_FRACTAL;
   public static ManagedShaderEffect SWIRL_BLOOM;
   public static ManagedShaderEffect PHOBOS_COLORFUL;
   public static ManagedShaderEffect PHOBOS_FRACT;
   public static ManagedShaderEffect PHOBOS_GRID;
   public static ManagedShaderEffect PHOBOS_MARBLE;
   public static ManagedShaderEffect PHOBOS_PLASMA;
   public static ManagedShaderEffect HEARTFELT;
   public static ManagedShaderEffect TITLE_IMAGE;
   public float time = 0.0F;
   private ShaderManager.MyFramebuffer shaderBuffer;
   private ShaderManager.MyFramebuffer skyBuffer;

   public void renderShader(Runnable runnable, ShaderManager.Shader mode) {
      tasks.add(new ShaderManager.RenderTask(runnable, mode));
   }

   public void renderShaders() {
      tasks.forEach(t -> this.applyShader(t.task(), t.shader()));
      tasks.clear();
   }

   public boolean renderTitleScreenMenu(TitleScreen.MenuShader mode, float width, float height, float delta, float titleTime) {
      if (this.fullNullCheck()) {
         return false;
      } else {
         RenderSystem.assertOnRenderThreadOrInit();
         TitleScreen.MenuShader shaderMode = mode == null ? TitleScreen.MenuShader.TitleImage : mode;
         Framebuffer mcBuffer = MinecraftClient.getInstance().getFramebuffer();
         if (mcBuffer == null) {
            return false;
         } else {
            this.ensureFramebufferSize(this.shaderBuffer, mcBuffer.textureWidth, mcBuffer.textureHeight);
            GlStateManager._glBindFramebuffer(36009, this.shaderBuffer.fbo);
            this.shaderBuffer.setClearColor(0.0F, 0.0F, 0.0F, 1.0F);
            this.shaderBuffer.beginWrite(true);
            this.shaderBuffer.clear(false);
            this.shaderBuffer.endWrite();
            this.shaderBuffer.setClearColor(0.0F, 0.0F, 0.0F, 0.0F);
            GlStateManager._glBindFramebuffer(36009, mcBuffer.fbo);
            mcBuffer.beginWrite(false);
            ManagedShaderEffect shader = this.getTitleScreenShader(shaderMode);
            if (shader == null) {
               return false;
            } else {
               PostEffectProcessor effect = shader.getShaderEffect();
               if (effect == null) {
                  return false;
               } else {
                  ((IShaderEffectHook)effect).alienClient$addHook("bufIn", this.shaderBuffer);
                  Framebuffer outBuffer = effect.getSecondaryTarget("bufOut");
                  if (outBuffer == null) {
                     return false;
                  } else {
                     if (shaderMode == TitleScreen.MenuShader.Heartfelt) {
                        TitleScreen titleScreen = TitleScreen.INSTANCE;
                        float[] mouse = this.getTitleScreenMouse(titleScreen != null && titleScreen.shouldMouseFollow(), width, height);
                        shader.setUniformValue("u_Resolution", Math.max(1.0F, width), Math.max(1.0F, height));
                        shader.setUniformValue("u_Time", titleTime);
                        shader.setUniformValue("u_Opacity", 1.0F);
                        shader.setUniformValue("u_Mouse", mouse[0], mouse[1], mouse[2], mouse[3]);
                        shader.render(delta);
                     } else if (shaderMode == TitleScreen.MenuShader.TitleImage) {
                        TitleScreen titleScreen = TitleScreen.INSTANCE;
                        float[] mouse = this.getTitleScreenMouse(titleScreen != null && titleScreen.shouldMouseFollow(), width, height);
                        shader.setUniformValue("u_Resolution", Math.max(1.0F, width), Math.max(1.0F, height));
                        shader.setUniformValue("u_ImageSize", 2560.0F, 1208.0F);
                        shader.setUniformValue("u_Time", titleTime);
                        shader.setUniformValue("u_Opacity", 1.0F);
                        shader.setUniformValue("u_Mouse", mouse[0], mouse[1], mouse[2], mouse[3]);
                        shader.render(delta);
                     } else {
                        ShaderManager.GuiShader guiShader = this.getTitleScreenGuiShader(shaderMode);
                        if (guiShader == null) {
                           return false;
                        }

                        TitleScreen titleScreen = TitleScreen.INSTANCE;
                        this.setupTitleScreenGuiShader(guiShader, shader, 1.0F, titleScreen != null && titleScreen.shouldMouseFollow());
                     }

                     this.shaderBuffer.clear(false);
                     mcBuffer.beginWrite(false);
                     RenderSystem.disableBlend();
                     RenderSystem.backupProjectionMatrix();
                     outBuffer.draw(outBuffer.textureWidth, outBuffer.textureHeight, false);
                     RenderSystem.restoreProjectionMatrix();
                     mcBuffer.beginWrite(false);
                     return true;
                  }
               }
            }
         }
      }
   }

   public void applyShaderRects(List<float[]> rects, ShaderManager.Shader mode, MatrixStack matrices) {
      if (!this.fullNullCheck()) {
         RenderSystem.assertOnRenderThreadOrInit();
         boolean scissorTest = GL11.glIsEnabled(3089);
         boolean depthTest = GL11.glIsEnabled(2929);
         Framebuffer mcBuffer = MinecraftClient.getInstance().getFramebuffer();
         if (this.shaderBuffer.textureWidth != mcBuffer.textureWidth || this.shaderBuffer.textureHeight != mcBuffer.textureHeight) {
            this.shaderBuffer.resize(mcBuffer.textureWidth, mcBuffer.textureHeight, false);
         }

         GlStateManager._glBindFramebuffer(36009, this.shaderBuffer.fbo);
         this.shaderBuffer.setClearColor(0.0F, 0.0F, 0.0F, 0.0F);
         this.shaderBuffer.beginWrite(true);
         this.shaderBuffer.clear(false);
         GL11.glDisable(3089);
         RenderSystem.disableDepthTest();
         RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
         prepareMaskBlend();

         for (float[] r : rects) {
            float rectOpacity = r.length > 4 ? Math.max(0.0F, Math.min(1.0F, r[4])) : 1.0F;
            if (!(rectOpacity <= 0.0F)) {
               Render2DUtil.drawRect(matrices, r[0], r[1], r[2], r[3], new Color(255, 255, 255, Math.round(255.0F * rectOpacity)).getRGB());
            }
         }

         this.shaderBuffer.endWrite();
         RenderSystem.defaultBlendFunc();
         RenderSystem.disableBlend();
         GlStateManager._glBindFramebuffer(36009, mcBuffer.fbo);
         mcBuffer.beginWrite(false);
         ManagedShaderEffect shader = this.getShader(mode);
         if (shader == null) {
            shader = DEFAULT;
         }

         if (shader == null) {
            restoreDepthAndScissor(depthTest, scissorTest);
         } else {
            PostEffectProcessor effect = shader.getShaderEffect();
            if (effect == null) {
               restoreDepthAndScissor(depthTest, scissorTest);
            } else {
               ((IShaderEffectHook)effect).alienClient$addHook("bufIn", this.shaderBuffer);
               Framebuffer outBuffer = effect.getSecondaryTarget("bufOut");
               if (outBuffer == null) {
                  restoreDepthAndScissor(depthTest, scissorTest);
               } else {
                  this.setupShader(mode, shader);
                  this.shaderBuffer.clear(false);
                  mcBuffer.beginWrite(false);
                  GL11.glDisable(3089);
                  RenderSystem.disableDepthTest();
                  RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
                  RenderSystem.enableBlend();
                  RenderSystem.blendFuncSeparate(SrcFactor.SRC_ALPHA, DstFactor.ONE_MINUS_SRC_ALPHA, SrcFactor.ZERO, DstFactor.ONE);
                  RenderSystem.backupProjectionMatrix();
                  outBuffer.draw(outBuffer.textureWidth, outBuffer.textureHeight, false);
                  RenderSystem.restoreProjectionMatrix();
                  RenderSystem.defaultBlendFunc();
                  RenderSystem.disableBlend();
                  restoreDepthAndScissor(depthTest, scissorTest);
                  mcBuffer.beginWrite(false);
               }
            }
         }
      }
   }

   public void applyGuiShaderRects(List<float[]> rects, ShaderManager.GuiShader mode, MatrixStack matrices, float opacity) {
      if (rects != null && !rects.isEmpty() && !this.fullNullCheck()) {
         RenderSystem.assertOnRenderThreadOrInit();
         boolean scissorTest = GL11.glIsEnabled(3089);
         boolean depthTest = GL11.glIsEnabled(2929);
         Framebuffer mcBuffer = MinecraftClient.getInstance().getFramebuffer();
         if (this.shaderBuffer.textureWidth != mcBuffer.textureWidth || this.shaderBuffer.textureHeight != mcBuffer.textureHeight) {
            this.shaderBuffer.resize(mcBuffer.textureWidth, mcBuffer.textureHeight, false);
         }

         ManagedShaderEffect shader = this.getGuiShader(mode);
         if (shader == null) {
            shader = DEFAULT;
         }

         if (shader == null) {
            restoreDepthAndScissor(depthTest, scissorTest);
         } else {
            PostEffectProcessor effect = shader.getShaderEffect();
            if (effect == null) {
               restoreDepthAndScissor(depthTest, scissorTest);
            } else {
               ((IShaderEffectHook)effect).alienClient$addHook("bufIn", this.shaderBuffer);
               Framebuffer outBuffer = effect.getSecondaryTarget("bufOut");
               if (outBuffer == null) {
                  restoreDepthAndScissor(depthTest, scissorTest);
               } else {
                  GlStateManager._glBindFramebuffer(36009, this.shaderBuffer.fbo);
                  this.shaderBuffer.setClearColor(1.0F, 1.0F, 1.0F, 1.0F);
                  this.shaderBuffer.beginWrite(true);
                  this.shaderBuffer.clear(false);
                  this.shaderBuffer.endWrite();
                  this.shaderBuffer.setClearColor(0.0F, 0.0F, 0.0F, 0.0F);
                  GlStateManager._glBindFramebuffer(36009, mcBuffer.fbo);
                  mcBuffer.beginWrite(false);
                  this.setupGuiShader(mode, shader, 1.0F);
                  this.shaderBuffer.clear(false);
                  mcBuffer.beginWrite(true);
                  GL11.glDisable(3089);
                  RenderSystem.disableDepthTest();
                  RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
                  this.drawGuiShaderRects(outBuffer, matrices, rects, opacity);
                  restoreDepthAndScissor(depthTest, scissorTest);
                  mcBuffer.beginWrite(false);
               }
            }
         }
      }
   }

   public void applyShader(Runnable runnable, ShaderManager.Shader mode) {
      if (!this.fullNullCheck()) {
         RenderSystem.assertOnRenderThreadOrInit();
         Framebuffer mcBuffer = MinecraftClient.getInstance().getFramebuffer();
         if (this.shaderBuffer.textureWidth != mcBuffer.textureWidth || this.shaderBuffer.textureHeight != mcBuffer.textureHeight) {
            this.shaderBuffer.resize(mcBuffer.textureWidth, mcBuffer.textureHeight, false);
         }

         GlStateManager._glBindFramebuffer(36009, this.shaderBuffer.fbo);
         this.shaderBuffer.beginWrite(false);
         RenderSystem.defaultBlendFunc();
         runnable.run();
         this.shaderBuffer.endWrite();
         GlStateManager._glBindFramebuffer(36009, mcBuffer.fbo);
         mcBuffer.beginWrite(false);
         ManagedShaderEffect shader = this.getShader(mode);
         PostEffectProcessor effect = shader.getShaderEffect();
         if (effect != null) {
            ((IShaderEffectHook)effect).alienClient$addHook("bufIn", this.shaderBuffer);
            Framebuffer outBuffer = effect.getSecondaryTarget("bufOut");
            if (outBuffer != null) {
               this.setupShader(mode, shader);
               this.shaderBuffer.clear(false);
               mcBuffer.beginWrite(false);
               RenderSystem.enableBlend();
               RenderSystem.blendFuncSeparate(SrcFactor.SRC_ALPHA, DstFactor.ONE_MINUS_SRC_ALPHA, SrcFactor.ZERO, DstFactor.ONE);
               RenderSystem.backupProjectionMatrix();
               outBuffer.draw(outBuffer.textureWidth, outBuffer.textureHeight, false);
               RenderSystem.restoreProjectionMatrix();
               RenderSystem.defaultBlendFunc();
               RenderSystem.disableBlend();
            }
         }
      }
   }

   public ManagedShaderEffect getShader(@NotNull ShaderManager.Shader mode) {
      return switch (mode) {
         case Gradient -> {
            if (GRADIENT == null) {
               GRADIENT = manage("gradient");
            }

            yield GRADIENT;
         }
         case Flow -> {
            if (FLOW == null) {
               FLOW = manage("glowing");
            }

            yield FLOW;
         }
         case Rainbow -> {
            if (RAINBOW == null) {
               RAINBOW = manage("rainbow");
            }

            yield RAINBOW;
         }
         case Bloom -> {
            if (BLOOM == null) {
               BLOOM = manage("bloom");
            }

            yield BLOOM;
         }
         default -> {
            if (DEFAULT == null) {
               DEFAULT = manage("outline");
            }

            yield DEFAULT;
         }
      };
   }

   public ManagedShaderEffect getShaderOutline(@NotNull ShaderManager.Shader mode) {
      return switch (mode) {
         case Gradient -> {
            if (GRADIENT_OUTLINE == null) {
               GRADIENT_OUTLINE = manageEntityOutline("gradient");
            }

            yield GRADIENT_OUTLINE;
         }
         case Flow -> {
            if (FLOW_OUTLINE == null) {
               FLOW_OUTLINE = manageEntityOutline("glowing");
            }

            yield FLOW_OUTLINE;
         }
         case Rainbow -> {
            if (RAINBOW_OUTLINE == null) {
               RAINBOW_OUTLINE = manageEntityOutline("rainbow");
            }

            yield RAINBOW_OUTLINE;
         }
         case Bloom -> {
            if (BLOOM_OUTLINE == null) {
               BLOOM_OUTLINE = manageEntityOutline("bloom");
            }

            yield BLOOM_OUTLINE;
         }
         default -> {
            if (DEFAULT_OUTLINE == null) {
               DEFAULT_OUTLINE = manageEntityOutline("outline");
            }

            yield DEFAULT_OUTLINE;
         }
      };
   }

   public void setupShader(ShaderManager.Shader shader, ManagedShaderEffect effect) {
      ShaderModule module = ShaderModule.INSTANCE;
      if (module != null) {
         this.setupShader(shader, effect, module, false);
      }
   }

   public void applySky(Runnable runnable) {
      SkyBox module = SkyBox.INSTANCE;
      if (module != null && module.isOn() && !this.fullNullCheck()) {
         ShaderManager.SkyShader shaderMode = module.mode.getValue();
         if (shaderMode == null) {
            shaderMode = ShaderManager.SkyShader.CosmicHorizon;
         }

         RenderSystem.assertOnRenderThreadOrInit();
         Framebuffer mcBuffer = MinecraftClient.getInstance().getFramebuffer();
         float skyScale = Math.max(0.25F, Math.min(1.0F, module.resolutionScale.getValueFloat()));
         int scaledWidth = Math.max(1, Math.round((float)mcBuffer.textureWidth * skyScale));
         int scaledHeight = Math.max(1, Math.round((float)mcBuffer.textureHeight * skyScale));
         this.ensureFramebufferSize(this.skyBuffer, scaledWidth, scaledHeight);
         GlStateManager._glBindFramebuffer(36009, this.skyBuffer.fbo);
         this.skyBuffer.setClearColor(1.0F, 1.0F, 1.0F, 1.0F);
         this.skyBuffer.beginWrite(true);
         this.skyBuffer.clear(false);
         this.skyBuffer.endWrite();
         this.skyBuffer.setClearColor(0.0F, 0.0F, 0.0F, 0.0F);
         GlStateManager._glBindFramebuffer(36009, mcBuffer.fbo);
         mcBuffer.beginWrite(false);
         ManagedShaderEffect shader = this.getSkyShader(shaderMode);
         if (shader == null) {
            shader = COSMIC_HORIZON;
         }

         PostEffectProcessor effect = shader.getShaderEffect();
         if (effect != null) {
            ((IShaderEffectHook)effect).alienClient$addHook("bufIn", this.skyBuffer);
            Framebuffer outBuffer = effect.getSecondaryTarget("bufOut");
            if (outBuffer != null) {
               this.setupSkyShader(shaderMode, shader, module);
               this.skyBuffer.clear(false);
               mcBuffer.beginWrite(false);
               RenderSystem.disableBlend();
               RenderSystem.backupProjectionMatrix();
               outBuffer.draw(outBuffer.textureWidth, outBuffer.textureHeight, false);
               RenderSystem.restoreProjectionMatrix();
               RenderSystem.disableBlend();
            }
         }
      }
   }

   public ManagedShaderEffect getSkyShader(@NotNull ShaderManager.SkyShader mode) {
      return switch (mode) {
         case SolidColor -> this.getShader(ShaderManager.Shader.Solid);
         case GradientFade -> this.getShader(ShaderManager.Shader.Gradient);
         case GlowingFlow -> this.getShader(ShaderManager.Shader.Flow);
         case RainbowAura -> this.getShader(ShaderManager.Shader.Rainbow);
         case CosmicHorizon -> {
            if (COSMIC_HORIZON == null) {
               COSMIC_HORIZON = manage("cosmic_horizon");
            }

            yield COSMIC_HORIZON;
         }
         case MonolithFlow -> {
            if (MONOLITH_FLOW == null) {
               MONOLITH_FLOW = manage("monolith_flow");
            }

            yield MONOLITH_FLOW;
         }
         case NebulaOrb -> {
            if (NEBULA_ORB == null) {
               NEBULA_ORB = manage("nebula_orb");
            }

            yield NEBULA_ORB;
         }
         case NetworkMesh -> {
            if (NETWORK_MESH == null) {
               NETWORK_MESH = manage("network_mesh");
            }

            yield NETWORK_MESH;
         }
         case SphereTunnel -> {
            if (SPHERE_TUNNEL == null) {
               SPHERE_TUNNEL = manage("sphere_tunnel");
            }

            yield SPHERE_TUNNEL;
         }
         case ISMFractal -> {
            if (ISM_FRACTAL == null) {
               ISM_FRACTAL = manage("ism_fractal");
            }

            yield ISM_FRACTAL;
         }
         case SwirlBloom -> {
            if (SWIRL_BLOOM == null) {
               SWIRL_BLOOM = manage("swirl_bloom");
            }

            yield SWIRL_BLOOM;
         }
         case PhobosColorful -> {
            if (PHOBOS_COLORFUL == null) {
               PHOBOS_COLORFUL = manage("phobos_colorful");
            }

            yield PHOBOS_COLORFUL;
         }
         case PhobosFract -> {
            if (PHOBOS_FRACT == null) {
               PHOBOS_FRACT = manage("phobos_fract");
            }

            yield PHOBOS_FRACT;
         }
         case PhobosGrid -> {
            if (PHOBOS_GRID == null) {
               PHOBOS_GRID = manage("phobos_grid");
            }

            yield PHOBOS_GRID;
         }
         case PhobosMarble -> {
            if (PHOBOS_MARBLE == null) {
               PHOBOS_MARBLE = manage("phobos_marble");
            }

            yield PHOBOS_MARBLE;
         }
         case PhobosPlasma -> {
            if (PHOBOS_PLASMA == null) {
               PHOBOS_PLASMA = manage("phobos_plasma");
            }

            yield PHOBOS_PLASMA;
         }
      };
   }

   public ManagedShaderEffect getGuiShader(@NotNull ShaderManager.GuiShader mode) {
      return switch (mode) {
         case Solid, SolidColor -> this.getShader(ShaderManager.Shader.Solid);
         case Gradient, GradientFade -> this.getShader(ShaderManager.Shader.Gradient);
         case Flow, GlowingFlow -> this.getShader(ShaderManager.Shader.Flow);
         case Rainbow, RainbowAura -> this.getShader(ShaderManager.Shader.Rainbow);
         case CosmicHorizon -> this.getSkyShader(ShaderManager.SkyShader.CosmicHorizon);
         case MonolithFlow -> this.getSkyShader(ShaderManager.SkyShader.MonolithFlow);
         case NebulaOrb -> this.getSkyShader(ShaderManager.SkyShader.NebulaOrb);
         case NetworkMesh -> this.getSkyShader(ShaderManager.SkyShader.NetworkMesh);
         case SphereTunnel -> this.getSkyShader(ShaderManager.SkyShader.SphereTunnel);
         case ISMFractal -> this.getSkyShader(ShaderManager.SkyShader.ISMFractal);
         case SwirlBloom -> this.getSkyShader(ShaderManager.SkyShader.SwirlBloom);
         case PhobosColorful -> this.getSkyShader(ShaderManager.SkyShader.PhobosColorful);
         case PhobosFract -> this.getSkyShader(ShaderManager.SkyShader.PhobosFract);
         case PhobosGrid -> this.getSkyShader(ShaderManager.SkyShader.PhobosGrid);
         case PhobosMarble -> this.getSkyShader(ShaderManager.SkyShader.PhobosMarble);
         case PhobosPlasma -> this.getSkyShader(ShaderManager.SkyShader.PhobosPlasma);
      };
   }

   public ManagedShaderEffect getTitleScreenShader(@NotNull TitleScreen.MenuShader mode) {
      if (mode == TitleScreen.MenuShader.TitleImage) {
         if (TITLE_IMAGE == null) {
            TITLE_IMAGE = manage("title_image");
         }

         return TITLE_IMAGE;
      } else if (mode == TitleScreen.MenuShader.Heartfelt) {
         if (HEARTFELT == null) {
            HEARTFELT = manage("heartfelt");
         }

         return HEARTFELT;
      } else {
         ShaderManager.GuiShader guiShader = this.getTitleScreenGuiShader(mode);
         if (guiShader != null) {
            return this.getGuiShader(guiShader);
         } else {
            if (HEARTFELT == null) {
               HEARTFELT = manage("heartfelt");
            }

            return HEARTFELT;
         }
      }
   }

   private ShaderManager.GuiShader getTitleScreenGuiShader(@NotNull TitleScreen.MenuShader mode) {
      return switch (mode) {
         case TitleImage -> null;
         case Heartfelt -> null;
         case MonolithFlow -> ShaderManager.GuiShader.MonolithFlow;
         case NebulaOrb -> ShaderManager.GuiShader.NebulaOrb;
         case NetworkMesh -> ShaderManager.GuiShader.NetworkMesh;
         case PhobosColorful -> ShaderManager.GuiShader.PhobosColorful;
         case PhobosFract -> ShaderManager.GuiShader.PhobosFract;
         case PhobosGrid -> ShaderManager.GuiShader.PhobosGrid;
      };
   }

   private void ensureFramebufferSize(ShaderManager.MyFramebuffer framebuffer, int width, int height) {
      if (framebuffer.textureWidth != width || framebuffer.textureHeight != height) {
         framebuffer.resize(width, height, false);
      }
   }

   private void setupShader(ShaderManager.Shader shader, ManagedShaderEffect effect, ShaderModule module, boolean skyMode) {
      Color color = module.fill.getValue();
      this.time = (float)timer.getMs() / 5.0F * module.speed.getValueFloat() * 0.004F;
      switch (shader) {
         case Solid:
            this.setupSolidShader(effect, module, color);
            break;
         case Gradient:
            this.setupGradientShader(effect, module, color);
            break;
         case Flow:
         default:
            this.setupGlowingShader(effect, module, color);
            break;
         case Rainbow:
            this.setupRainbowShader(effect, module, color);
            break;
         case Bloom:
            this.setupBloomShader(effect, module);
      }
   }

   private void setupSolidShader(ManagedShaderEffect effect, ShaderModule module, Color color) {
      this.setupOutlineUniforms(effect, module, color);
      effect.setUniformValue("sobel", 0);
      effect.setUniformValue("dots", 0);
      effect.setUniformValue("dotRadius", 1);
      effect.render(mc.getRenderTickCounter().getTickDelta(true));
   }

   private void setupGradientShader(ManagedShaderEffect effect, ShaderModule module, Color color) {
      this.setupOutlineUniforms(effect, module, color);
      Color secondColor = module.pulse.getValue();
      effect.setUniformValue(
         "color1",
         (float)secondColor.getRed() / 255.0F,
         (float)secondColor.getGreen() / 255.0F,
         (float)secondColor.getBlue() / 255.0F,
         (float)color.getAlpha() / 255.0F
      );
      effect.setUniformValue("factor", Math.max(1.0F, module.step.getValueFloat() * 300.0F));
      effect.setUniformValue("time", this.time * 300.0F);
      effect.render(mc.getRenderTickCounter().getTickDelta(true));
   }

   private void setupRainbowShader(ManagedShaderEffect effect, ShaderModule module, Color color) {
      this.setupOutlineUniforms(effect, module, color);
      effect.setUniformValue("oct", Math.max(1, Math.round(module.octaves.getValueFloat())));
      effect.setUniformValue("alpha0", (float)color.getAlpha() / 255.0F);
      effect.render(mc.getRenderTickCounter().getTickDelta(true));
   }

   private void setupGlowingShader(ManagedShaderEffect effect, ShaderModule module, Color color) {
      this.setupOutlineUniforms(effect, module, color);
      effect.setUniformValue("sobel", 1);
      effect.setUniformValue("resolution", (float)mc.getWindow().getScaledWidth(), (float)mc.getWindow().getScaledHeight());
      effect.setUniformValue("time", this.time);
      effect.render(mc.getRenderTickCounter().getTickDelta(true));
   }

   private void setupBloomShader(ManagedShaderEffect effect, ShaderModule module) {
      Color fillColor = module.bloomFill.getValue();
      Color outlineColor = module.bloomOutline.getValue();
      effect.setUniformValue("Width", Math.max(0, Math.min(10, module.bloomRadius.getValueInt())));
      effect.setUniformValue("FastLines", module.bloomFastLines.getValue() ? 1 : 0);
      effect.setUniformValue("GlowMultiplier", Math.max(0.0F, Math.min(5.0F, module.bloomIntensity.getValueFloat())));
      effect.setUniformValue("GlowQuality", Math.max(1, Math.min(6, module.bloomQuality.getValueInt())));
      effect.setUniformValue(
         "FillColor",
         (float)fillColor.getRed() / 255.0F,
         (float)fillColor.getGreen() / 255.0F,
         (float)fillColor.getBlue() / 255.0F,
         Math.max(0.0F, Math.min(1.0F, module.bloomFillAlpha.getValueFloat() / 255.0F))
      );
      effect.setUniformValue(
         "OutlineColor",
         (float)outlineColor.getRed() / 255.0F,
         (float)outlineColor.getGreen() / 255.0F,
         (float)outlineColor.getBlue() / 255.0F,
         Math.max(0.0F, Math.min(1.0F, module.alpha.getValueFloat() / 255.0F))
      );
      effect.render(mc.getRenderTickCounter().getTickDelta(true));
   }

   private void setupSkyShader(ShaderManager.SkyShader shader, ManagedShaderEffect effect, SkyBox module) {
      this.time = (float)timer.getMs() / 5.0F * module.speed.getValueFloat() * 0.004F;
      switch (shader) {
         case SolidColor:
            this.setupSkySolidShader(effect, module);
            break;
         case GradientFade:
            this.setupSkyGradientShader(effect, module);
            break;
         case GlowingFlow:
            this.setupSkyFlowShader(effect, module);
            break;
         case RainbowAura:
            this.setupSkyRainbowShader(effect, module);
            break;
         case CosmicHorizon:
            this.setupCosmicHorizonShader(effect, module);
            break;
         case MonolithFlow:
            this.setupMonolithSkyShader(effect, module);
            break;
         case NebulaOrb:
         case NetworkMesh:
         case SphereTunnel:
         case ISMFractal:
            this.setupGuiProceduralShader(effect, module.color.getValue(), this.time);
            break;
         case SwirlBloom:
            this.setupScaledProceduralShader(effect, module.color.getValue(), this.time, module.shaderScale.getValueFloat());
            break;
         case PhobosColorful:
         case PhobosFract:
         case PhobosGrid:
         case PhobosMarble:
         case PhobosPlasma:
            this.setupPhobosThemeShader(effect, module.color.getValue(), this.time);
      }
   }

   private void setupGuiShader(ShaderManager.GuiShader shader, ManagedShaderEffect effect, float opacity) {
      ShaderModule shaderModule = ShaderModule.INSTANCE;
      SkyBox skyBox = SkyBox.INSTANCE;
      ClickGui clickGui = ClickGui.getInstance();
      float speed = clickGui == null ? 4.0F : clickGui.shaderSpeed.getValueFloat();
      this.time = (float)timer.getMs() / 5.0F * speed * 0.004F;
      float guiOpacity = Math.max(0.0F, Math.min(1.0F, opacity));
      switch (shader) {
         case Solid:
            this.setupGuiSolidShader(effect, this.withOpacity(shaderModule == null ? Color.WHITE : shaderModule.fill.getValue(), guiOpacity));
            break;
         case Gradient:
            this.setupGuiGradientShader(
               effect,
               this.withOpacity(shaderModule == null ? Color.WHITE : shaderModule.fill.getValue(), guiOpacity),
               this.withOpacity(shaderModule == null ? Color.WHITE : shaderModule.pulse.getValue(), guiOpacity),
               shaderModule == null ? 0.5F : shaderModule.step.getValueFloat(),
               this.time
            );
            break;
         case Flow:
            this.setupGuiFlowShader(effect, this.withOpacity(shaderModule == null ? Color.WHITE : shaderModule.fill.getValue(), guiOpacity), this.time);
            break;
         case Rainbow:
            this.setupGuiRainbowShader(effect, this.withOpacity(shaderModule == null ? Color.WHITE : shaderModule.fill.getValue(), guiOpacity), this.time);
            break;
         case SolidColor:
            this.setupGuiSolidShader(effect, this.withOpacity(skyBox == null ? Color.WHITE : skyBox.color.getValue(), guiOpacity));
            break;
         case GradientFade:
            this.setupGuiGradientShader(
               effect,
               this.withOpacity(skyBox == null ? Color.WHITE : skyBox.color.getValue(), guiOpacity),
               this.withOpacity(skyBox == null ? Color.WHITE : skyBox.secondColor.getValue(), guiOpacity),
               skyBox == null ? 0.5F : skyBox.step.getValueFloat(),
               this.time
            );
            break;
         case GlowingFlow:
            this.setupGuiFlowShader(effect, this.withOpacity(skyBox == null ? Color.WHITE : skyBox.color.getValue(), guiOpacity), this.time);
            break;
         case RainbowAura:
            this.setupGuiRainbowShader(effect, this.withOpacity(skyBox == null ? Color.WHITE : skyBox.color.getValue(), guiOpacity), this.time);
            break;
         case CosmicHorizon:
            if (skyBox == null) {
               this.setupCosmicHorizonShader(effect, this.withOpacity(Color.WHITE, guiOpacity));
            } else {
               this.setupCosmicHorizonShader(effect, this.withOpacity(skyBox.color.getValue(), guiOpacity));
            }
            break;
         case MonolithFlow:
            float guiScale = clickGui == null ? 1.0F : clickGui.shaderSize.getValueFloat();
            if (skyBox == null) {
               this.setupMonolithShader(
                  effect, this.withOpacity(Color.WHITE, guiOpacity), this.withOpacity(Color.WHITE, guiOpacity), 0.0F, 0.0F, guiScale, 0.005F
               );
            } else {
               this.setupMonolithShader(
                  effect,
                  this.withOpacity(skyBox.color.getValue(), guiOpacity),
                  this.withOpacity(skyBox.secondColor.getValue(), guiOpacity),
                  this.getMonolithMouseX(skyBox),
                  this.getMonolithMouseY(skyBox),
                  guiScale,
                  skyBox.shaderIntensity.getValueFloat()
               );
            }
            break;
         case NebulaOrb:
         case NetworkMesh:
         case SphereTunnel:
         case ISMFractal:
            this.setupGuiProceduralShader(effect, this.withOpacity(shaderModule == null ? Color.WHITE : shaderModule.fill.getValue(), guiOpacity), this.time);
            break;
         case SwirlBloom:
            this.setupScaledProceduralShader(
               effect,
               this.withOpacity(shaderModule == null ? Color.WHITE : shaderModule.fill.getValue(), guiOpacity),
               this.time,
               clickGui == null ? 1.0F : clickGui.shaderSize.getValueFloat()
            );
            break;
         case PhobosColorful:
         case PhobosFract:
         case PhobosGrid:
         case PhobosMarble:
         case PhobosPlasma:
            this.setupPhobosThemeShader(effect, this.withOpacity(shaderModule == null ? Color.WHITE : shaderModule.fill.getValue(), guiOpacity), this.time);
      }
   }

   private static void restoreDepthAndScissor(boolean depthTest, boolean scissorTest) {
      if (depthTest) {
         RenderSystem.enableDepthTest();
      } else {
         RenderSystem.disableDepthTest();
      }

      if (scissorTest) {
         GL11.glEnable(3089);
      } else {
         GL11.glDisable(3089);
      }
   }

   private static void prepareMaskBlend() {
      RenderSystem.enableBlend();
      RenderSystem.blendFuncSeparate(SrcFactor.SRC_ALPHA, DstFactor.ONE_MINUS_SRC_ALPHA, SrcFactor.ONE, DstFactor.ZERO);
   }

   private void drawGuiShaderRects(Framebuffer source, MatrixStack matrices, List<float[]> rects, float opacity) {
      float globalOpacity = clamp01(opacity);
      if (source != null && !(globalOpacity <= 0.0F) && mc.getWindow() != null) {
         boolean hasVisibleRect = false;

         for (float[] rect : rects) {
            if (rect != null && rect.length >= 4 && !(rect[2] <= 0.0F) && !(rect[3] <= 0.0F)) {
               float rectOpacity = rect.length > 4 ? clamp01(rect[4]) : 1.0F;
               if (rectOpacity * globalOpacity > 0.0F) {
                  hasVisibleRect = true;
                  break;
               }
            }
         }

         if (hasVisibleRect) {
            Matrix4f matrix = matrices.peek().getPositionMatrix();
            float screenWidth = Math.max(1.0F, (float)mc.getWindow().getScaledWidth());
            float screenHeight = Math.max(1.0F, (float)mc.getWindow().getScaledHeight());
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.setShader(GameRenderer::getPositionTexColorProgram);
            RenderSystem.setShaderTexture(0, source.getColorAttachment());
            BufferBuilder buffer = Tessellator.getInstance().begin(DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);

            for (float[] rectx : rects) {
               if (rectx != null && rectx.length >= 4) {
                  float x = rectx[0];
                  float y = rectx[1];
                  float width = rectx[2];
                  float height = rectx[3];
                  if (!(width <= 0.0F) && !(height <= 0.0F)) {
                     float rectOpacity = rectx.length > 4 ? clamp01(rectx[4]) : 1.0F;
                     float alpha = clamp01(rectOpacity * globalOpacity);
                     if (!(alpha <= 0.0F)) {
                        Vector3f topLeft = matrix.transformPosition(x, y, 0.0F, new Vector3f());
                        Vector3f bottomRight = matrix.transformPosition(x + width, y + height, 0.0F, new Vector3f());
                        float left = Math.min(topLeft.x, bottomRight.x);
                        float right = Math.max(topLeft.x, bottomRight.x);
                        float top = Math.min(topLeft.y, bottomRight.y);
                        float bottom = Math.max(topLeft.y, bottomRight.y);
                        float u1 = clamp01(left / screenWidth);
                        float u2 = clamp01(right / screenWidth);
                        float v1 = clamp01(top / screenHeight);
                        float v2 = clamp01(bottom / screenHeight);
                        int color = Math.round(alpha * 255.0F) << 24 | 16777215;
                        buffer.vertex(matrix, x, y + height, 0.0F).texture(u1, v2).color(color);
                        buffer.vertex(matrix, x + width, y + height, 0.0F).texture(u2, v2).color(color);
                        buffer.vertex(matrix, x + width, y, 0.0F).texture(u2, v1).color(color);
                        buffer.vertex(matrix, x, y, 0.0F).texture(u1, v1).color(color);
                     }
                  }
               }
            }

            BufferRenderer.drawWithGlobalProgram(buffer.end());
            RenderSystem.setShaderTexture(0, 0);
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            RenderSystem.defaultBlendFunc();
            RenderSystem.disableBlend();
         }
      }
   }

   private static float clamp01(float value) {
      return Math.max(0.0F, Math.min(1.0F, value));
   }

   private void setupTitleScreenGuiShader(ShaderManager.GuiShader shader, ManagedShaderEffect effect, float opacity, boolean mouseFollow) {
      if (shader != ShaderManager.GuiShader.MonolithFlow) {
         this.setupGuiShader(shader, effect, opacity);
      } else {
         SkyBox skyBox = SkyBox.INSTANCE;
         ClickGui clickGui = ClickGui.getInstance();
         float speed = clickGui == null ? 4.0F : clickGui.shaderSpeed.getValueFloat();
         this.time = (float)timer.getMs() / 5.0F * speed * 0.004F;
         float guiOpacity = Math.max(0.0F, Math.min(1.0F, opacity));
         float guiScale = clickGui == null ? 1.0F : clickGui.shaderSize.getValueFloat();
         float mouseX = this.getTitleScreenMonolithMouseX(mouseFollow);
         float mouseY = this.getTitleScreenMonolithMouseY(mouseFollow);
         if (skyBox == null) {
            this.setupMonolithShader(
               effect, this.withOpacity(Color.WHITE, guiOpacity), this.withOpacity(Color.WHITE, guiOpacity), mouseX, mouseY, guiScale, 0.005F
            );
         } else {
            this.setupMonolithShader(
               effect,
               this.withOpacity(skyBox.color.getValue(), guiOpacity),
               this.withOpacity(skyBox.secondColor.getValue(), guiOpacity),
               mouseX,
               mouseY,
               guiScale,
               skyBox.shaderIntensity.getValueFloat()
            );
         }
      }
   }

   private Color withOpacity(Color color, float opacity) {
      int alpha = Math.round(255.0F * Math.max(0.0F, Math.min(1.0F, opacity)));
      return new Color(color.getRed(), color.getGreen(), color.getBlue(), Math.max(0, Math.min(255, alpha)));
   }

   private void setupGuiSolidShader(ManagedShaderEffect effect, Color color) {
      this.setupGuiFillUniforms(effect, color);
      effect.setUniformValue("sobel", 0);
      effect.setUniformValue("dots", 0);
      effect.setUniformValue("dotRadius", 1);
      effect.render(mc.getRenderTickCounter().getTickDelta(true));
   }

   private void setupGuiGradientShader(ManagedShaderEffect effect, Color color, Color secondColor, float step, float shaderTime) {
      this.setupGuiFillUniforms(effect, color);
      effect.setUniformValue("sobel", 0);
      effect.setUniformValue(
         "color1",
         (float)secondColor.getRed() / 255.0F,
         (float)secondColor.getGreen() / 255.0F,
         (float)secondColor.getBlue() / 255.0F,
         (float)secondColor.getAlpha() / 255.0F
      );
      effect.setUniformValue("factor", Math.max(1.0F, step * 300.0F));
      effect.setUniformValue("time", shaderTime * 300.0F);
      effect.render(mc.getRenderTickCounter().getTickDelta(true));
   }

   private void setupGuiFlowShader(ManagedShaderEffect effect, Color color, float shaderTime) {
      this.setupGuiFillUniforms(effect, color);
      effect.setUniformValue("sobel", 0);
      effect.setUniformValue("resolution", (float)mc.getWindow().getScaledWidth(), (float)mc.getWindow().getScaledHeight());
      effect.setUniformValue("time", shaderTime);
      effect.render(mc.getRenderTickCounter().getTickDelta(true));
   }

   private void setupGuiRainbowShader(ManagedShaderEffect effect, Color color, float shaderTime) {
      this.setupGuiFillUniforms(effect, color);
      effect.setUniformValue("resolution", (float)mc.getWindow().getScaledWidth(), (float)mc.getWindow().getScaledHeight());
      effect.setUniformValue("time", shaderTime);
      effect.render(mc.getRenderTickCounter().getTickDelta(true));
   }

   private void setupGuiProceduralShader(ManagedShaderEffect effect, Color color, float shaderTime) {
      effect.setUniformValue("u_Resolution", (float)mc.getWindow().getScaledWidth(), (float)mc.getWindow().getScaledHeight());
      effect.setUniformValue("u_Time", shaderTime);
      effect.setUniformValue("u_Opacity", (float)color.getAlpha() / 255.0F);
      effect.render(mc.getRenderTickCounter().getTickDelta(true));
   }

   private void setupScaledProceduralShader(ManagedShaderEffect effect, Color color, float shaderTime, float scale) {
      effect.setUniformValue("u_Resolution", (float)mc.getWindow().getScaledWidth(), (float)mc.getWindow().getScaledHeight());
      effect.setUniformValue("u_Time", shaderTime);
      effect.setUniformValue("u_Opacity", (float)color.getAlpha() / 255.0F);
      effect.setUniformValue("u_Scale", Math.max(0.1F, scale));
      effect.render(mc.getRenderTickCounter().getTickDelta(true));
   }

   private void setupPhobosThemeShader(ManagedShaderEffect effect, Color color, float shaderTime) {
      effect.setUniformValue("u_Resolution", (float)mc.getWindow().getScaledWidth(), (float)mc.getWindow().getScaledHeight());
      effect.setUniformValue("u_Time", shaderTime);
      effect.setUniformValue("u_Opacity", (float)color.getAlpha() / 255.0F);
      effect.setUniformValue("u_Color", (float)color.getRed() / 255.0F, (float)color.getGreen() / 255.0F, (float)color.getBlue() / 255.0F, 1.0F);
      effect.render(mc.getRenderTickCounter().getTickDelta(true));
   }

   private void setupGuiFillUniforms(ManagedShaderEffect effect, Color color) {
      float width = Math.max(1.0F, (float)mc.getWindow().getScaledWidth());
      float height = Math.max(1.0F, (float)mc.getWindow().getScaledHeight());
      effect.setUniformValue("texelSize", 1.0F / width, 1.0F / height);
      effect.setUniformValue("samples", 1);
      effect.setUniformValue("steps", 1);
      effect.setUniformValue(
         "color", (float)color.getRed() / 255.0F, (float)color.getGreen() / 255.0F, (float)color.getBlue() / 255.0F, (float)color.getAlpha() / 255.0F
      );
      effect.setUniformValue("fastOutline", 1);
      effect.setUniformValue("radius", 0.0F);
      effect.setUniformValue("glow", 0);
      effect.setUniformValue("glowRadius", 1.0F);
   }

   private void setupSkySolidShader(ManagedShaderEffect effect, SkyBox module) {
      this.setupSkyFillUniforms(effect, module);
      effect.setUniformValue("sobel", 0);
      effect.setUniformValue("dots", 0);
      effect.setUniformValue("dotRadius", 1);
      effect.render(mc.getRenderTickCounter().getTickDelta(true));
   }

   private void setupSkyGradientShader(ManagedShaderEffect effect, SkyBox module) {
      this.setupSkyFillUniforms(effect, module);
      Color secondColor = module.secondColor.getValue();
      effect.setUniformValue("sobel", 0);
      effect.setUniformValue(
         "color1",
         (float)secondColor.getRed() / 255.0F,
         (float)secondColor.getGreen() / 255.0F,
         (float)secondColor.getBlue() / 255.0F,
         (float)secondColor.getAlpha() / 255.0F
      );
      effect.setUniformValue("factor", Math.max(1.0F, module.step.getValueFloat() * 300.0F));
      effect.setUniformValue("time", this.time * 300.0F);
      effect.render(mc.getRenderTickCounter().getTickDelta(true));
   }

   private void setupSkyFlowShader(ManagedShaderEffect effect, SkyBox module) {
      this.setupSkyFillUniforms(effect, module);
      effect.setUniformValue("sobel", 0);
      effect.setUniformValue("resolution", (float)mc.getWindow().getScaledWidth(), (float)mc.getWindow().getScaledHeight());
      effect.setUniformValue("time", this.time);
      effect.render(mc.getRenderTickCounter().getTickDelta(true));
   }

   private void setupSkyRainbowShader(ManagedShaderEffect effect, SkyBox module) {
      this.setupSkyFillUniforms(effect, module);
      effect.setUniformValue("resolution", (float)mc.getWindow().getScaledWidth(), (float)mc.getWindow().getScaledHeight());
      effect.setUniformValue("time", this.time);
      effect.render(mc.getRenderTickCounter().getTickDelta(true));
   }

   private void setupCosmicHorizonShader(ManagedShaderEffect effect, SkyBox module) {
      this.setupCosmicHorizonShader(effect, module.color.getValue());
   }

   private void setupCosmicHorizonShader(ManagedShaderEffect effect, Color color) {
      effect.setUniformValue("u_Resolution", (float)mc.getWindow().getScaledWidth(), (float)mc.getWindow().getScaledHeight());
      effect.setUniformValue("u_Time", this.time);
      effect.setUniformValue("u_Opacity", (float)color.getAlpha() / 255.0F);
      effect.render(mc.getRenderTickCounter().getTickDelta(true));
   }

   private void setupMonolithSkyShader(ManagedShaderEffect effect, SkyBox module) {
      this.setupMonolithShader(
         effect,
         module.color.getValue(),
         module.secondColor.getValue(),
         this.getMonolithMouseX(module),
         this.getMonolithMouseY(module),
         module.shaderScale.getValueFloat(),
         module.shaderIntensity.getValueFloat()
      );
   }

   private void setupMonolithShader(ManagedShaderEffect effect, Color color, Color secondColor, float mouseX, float mouseY, float scale, float intensity) {
      effect.setUniformValue("u_Color", (float)color.getRed() / 255.0F, (float)color.getGreen() / 255.0F, (float)color.getBlue() / 255.0F, 1.0F);
      effect.setUniformValue(
         "u_Color2", (float)secondColor.getRed() / 255.0F, (float)secondColor.getGreen() / 255.0F, (float)secondColor.getBlue() / 255.0F, 1.0F
      );
      effect.setUniformValue("u_Resolution", (float)mc.getWindow().getScaledWidth(), (float)mc.getWindow().getScaledHeight());
      effect.setUniformValue("u_Mouse", mouseX, mouseY);
      effect.setUniformValue("u_Scale", scale);
      effect.setUniformValue("u_Time", this.time);
      effect.setUniformValue("u_Intensity", intensity);
      effect.setUniformValue("u_Opacity", (float)color.getAlpha() / 255.0F);
      effect.render(mc.getRenderTickCounter().getTickDelta(true));
   }

   private float getMonolithMouseX(SkyBox module) {
      if (module.mouseFollow.getValue() && mc.getWindow() != null) {
         double width = Math.max(1.0, (double)mc.getWindow().getWidth());
         return (float)((mc.mouse.getX() / width - 0.5) * 0.4);
      } else {
         return 0.0F;
      }
   }

   private float getMonolithMouseY(SkyBox module) {
      if (module.mouseFollow.getValue() && mc.getWindow() != null) {
         double height = Math.max(1.0, (double)mc.getWindow().getHeight());
         return (float)((mc.mouse.getY() / height - 0.5) * 0.4);
      } else {
         return 0.0F;
      }
   }

   private float[] getTitleScreenMouse(boolean mouseFollow, float width, float height) {
      if (mouseFollow && mc.getWindow() != null) {
         double windowWidth = Math.max(1.0, (double)mc.getWindow().getWidth());
         double windowHeight = Math.max(1.0, (double)mc.getWindow().getHeight());
         float scaledWidth = Math.max(1.0F, width);
         float scaledHeight = Math.max(1.0F, height);
         float mouseX = (float)(mc.mouse.getX() / windowWidth * (double)scaledWidth);
         float mouseY = (float)((1.0 - mc.mouse.getY() / windowHeight) * (double)scaledHeight);
         return new float[]{mouseX, mouseY, 1.0F, 0.0F};
      } else {
         return new float[]{0.0F, 0.0F, 0.0F, 0.0F};
      }
   }

   private float getTitleScreenMonolithMouseX(boolean mouseFollow) {
      if (mouseFollow && mc.getWindow() != null) {
         double width = Math.max(1.0, (double)mc.getWindow().getWidth());
         return (float)((mc.mouse.getX() / width - 0.5) * 0.4);
      } else {
         return 0.0F;
      }
   }

   private float getTitleScreenMonolithMouseY(boolean mouseFollow) {
      if (mouseFollow && mc.getWindow() != null) {
         double height = Math.max(1.0, (double)mc.getWindow().getHeight());
         return (float)((mc.mouse.getY() / height - 0.5) * 0.4);
      } else {
         return 0.0F;
      }
   }

   private void setupSkyFillUniforms(ManagedShaderEffect effect, SkyBox module) {
      Color color = module.color.getValue();
      float width = Math.max(1.0F, (float)mc.getWindow().getScaledWidth());
      float height = Math.max(1.0F, (float)mc.getWindow().getScaledHeight());
      effect.setUniformValue("texelSize", 1.0F / width, 1.0F / height);
      effect.setUniformValue("samples", 1);
      effect.setUniformValue("steps", 1);
      effect.setUniformValue(
         "color", (float)color.getRed() / 255.0F, (float)color.getGreen() / 255.0F, (float)color.getBlue() / 255.0F, (float)color.getAlpha() / 255.0F
      );
      effect.setUniformValue("fastOutline", 1);
      effect.setUniformValue("radius", 0.0F);
      effect.setUniformValue("glow", 0);
      effect.setUniformValue("glowRadius", 1.0F);
   }

   private void setupOutlineUniforms(ManagedShaderEffect effect, ShaderModule module, Color color) {
      float width = Math.max(1.0F, (float)mc.getWindow().getScaledWidth());
      float height = Math.max(1.0F, (float)mc.getWindow().getScaledHeight());
      effect.setUniformValue("texelSize", 1.0F / width, 1.0F / height);
      effect.setUniformValue("samples", Math.max(1, Math.round(module.maxSample.getValueFloat())));
      effect.setUniformValue("steps", Math.max(1, Math.round(module.divider.getValueFloat() / 30.0F)));
      effect.setUniformValue(
         "color", (float)color.getRed() / 255.0F, (float)color.getGreen() / 255.0F, (float)color.getBlue() / 255.0F, (float)color.getAlpha() / 255.0F
      );
      effect.setUniformValue("fastOutline", 1);
      effect.setUniformValue("radius", module.radius.getValueFloat());
      effect.setUniformValue("glow", 1);
      effect.setUniformValue("glowRadius", Math.max(0.1F, module.smoothness.getValueFloat() * 2.0F));
   }

   public void reloadShaders() {
      if (TitleScreen.shouldRender() && TitleScreen.INSTANCE != null) {
         this.getTitleScreenShader(TitleScreen.INSTANCE.getShaderMode());
      }

      if (ShaderModule.INSTANCE != null && ShaderModule.INSTANCE.isOn()) {
         ShaderManager.Shader mode = ShaderModule.INSTANCE.mode.getValue();
         if (mode != null) {
            this.getShader(mode);
            this.getShaderOutline(mode);
         }
      }

      if (SkyBox.INSTANCE != null && SkyBox.INSTANCE.isOn()) {
         ShaderManager.SkyShader mode = SkyBox.INSTANCE.mode.getValue();
         if (mode != null) {
            this.getSkyShader(mode);
         }
      }

      ClickGui clickGui = ClickGui.getInstance();
      if (clickGui != null && clickGui.shader.getValue()) {
         ShaderManager.GuiShader mode = clickGui.shaderMode.getValue();
         if (mode != null) {
            this.getGuiShader(mode);
         }
      }
   }

   private static ManagedShaderEffect manage(String shaderName) {
      return ShaderEffectManager.getInstance().manage(shaderId(shaderName));
   }

   private static ManagedShaderEffect manageOutline(String shaderName, Consumer<ManagedShaderEffect> initCallback) {
      return ShaderEffectManager.getInstance().manage(shaderId(shaderName), initCallback);
   }

   private static ManagedShaderEffect manageEntityOutline(String shaderName) {
      return manageOutline(shaderName, managedShaderEffect -> {
         PostEffectProcessor effect = managedShaderEffect.getShaderEffect();
         if (effect != null && mc.worldRenderer != null) {
            Framebuffer outlines = mc.worldRenderer.getEntityOutlinesFramebuffer();
            if (outlines != null) {
               ((IShaderEffectHook)effect).alienClient$addHook("bufIn", outlines);
               ((IShaderEffectHook)effect).alienClient$addHook("bufOut", outlines);
            }
         }
      });
   }

   private static Identifier shaderId(String shaderName) {
      return Identifier.of("noxisclient", "shaders/post/" + shaderName + ".json");
   }

   public boolean fullNullCheck() {
      if (this.shaderBuffer != null && this.skyBuffer != null) {
         return false;
      } else if (mc.getFramebuffer() == null) {
         return true;
      } else {
         this.shaderBuffer = new ShaderManager.MyFramebuffer(mc.getFramebuffer().textureWidth, mc.getFramebuffer().textureHeight);
         this.skyBuffer = new ShaderManager.MyFramebuffer(mc.getFramebuffer().textureWidth, mc.getFramebuffer().textureHeight);
         return false;
      }
   }

   public static enum GuiShader {
      Solid,
      Gradient,
      Flow,
      Rainbow,
      SolidColor,
      GradientFade,
      GlowingFlow,
      RainbowAura,
      CosmicHorizon,
      MonolithFlow,
      NebulaOrb,
      NetworkMesh,
      SphereTunnel,
      ISMFractal,
      SwirlBloom,
      PhobosColorful,
      PhobosFract,
      PhobosGrid,
      PhobosMarble,
      PhobosPlasma;
   }

   public static class MyFramebuffer extends Framebuffer {
      public MyFramebuffer(int width, int height) {
         super(false);
         RenderSystem.assertOnRenderThreadOrInit();
         this.resize(width, height, true);
         this.setClearColor(0.0F, 0.0F, 0.0F, 0.0F);
      }
   }

   public static record RenderTask(Runnable task, ShaderManager.Shader shader) {
   }

   public static enum Shader {
      Solid,
      Gradient,
      Flow,
      Rainbow,
      Bloom;
   }

   public static enum SkyShader {
      SolidColor,
      GradientFade,
      GlowingFlow,
      RainbowAura,
      CosmicHorizon,
      MonolithFlow,
      NebulaOrb,
      NetworkMesh,
      SphereTunnel,
      ISMFractal,
      SwirlBloom,
      PhobosColorful,
      PhobosFract,
      PhobosGrid,
      PhobosMarble,
      PhobosPlasma;
   }
}
