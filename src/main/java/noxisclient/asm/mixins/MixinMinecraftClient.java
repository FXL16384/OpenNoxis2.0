package noxisclient.asm.mixins;

import com.mojang.blaze3d.systems.RenderSystem;
import java.awt.Color;
import net.minecraft.SharedConstants;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.RunArgs;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.gui.screen.DownloadingTerrainScreen;
import net.minecraft.client.gui.screen.ProgressScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.screen.multiplayer.ConnectScreen;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.client.particle.ParticleManager;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.client.util.Window;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.server.integrated.IntegratedServer;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.hit.HitResult.Type;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.thread.ReentrantThreadExecutor;
import noxisclient.Alien;
import noxisclient.api.events.Event;
import noxisclient.api.events.impl.ClientTickEvent;
import noxisclient.api.events.impl.DoAttackEvent;
import noxisclient.api.events.impl.GameLeftEvent;
import noxisclient.api.events.impl.OpenScreenEvent;
import noxisclient.api.events.impl.ResizeEvent;
import noxisclient.api.utils.render.Render2DUtil;
import noxisclient.core.impl.CommandManager;
import noxisclient.core.impl.FontManager;
import noxisclient.core.impl.ShaderManager;
import noxisclient.mod.modules.impl.client.ClientSetting;
import noxisclient.mod.modules.impl.client.Fonts;
import noxisclient.mod.modules.impl.player.InteractTweaks;
import noxisclient.mod.modules.impl.render.SpearFix;
import org.ladysnake.satin.api.managed.ManagedShaderEffect;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({MinecraftClient.class})
public abstract class MixinMinecraftClient extends ReentrantThreadExecutor<Runnable> {
   @Shadow
   @Final
   public InGameHud field_1705;
   @Shadow
   public int field_1771;
   @Shadow
   public ClientPlayerEntity field_1724;
   @Shadow
   public HitResult field_1765;
   @Shadow
   public ClientPlayerInteractionManager field_1761;
   @Final
   @Shadow
   public ParticleManager field_1713;
   @Shadow
   public ClientWorld field_1687;
   @Shadow
   private IntegratedServer field_1766;
   @Shadow
   public Screen field_1755;
   @Shadow
   @Final
   private Window field_1704;
   @Unique
   private static long veyraStartTs = 0L;

   public MixinMinecraftClient(String string) {
      super(string);
   }

   @Inject(
      method = {"onResolutionChanged"},
      at = {@At("TAIL")}
   )
   private void captureResize(CallbackInfo ci) {
      Alien.EVENT_BUS.post(new ResizeEvent(this.field_1704));
   }

   @Redirect(
      method = {"render"},
      at = @At(
         value = "INVOKE",
         target = "Lcom/mojang/blaze3d/systems/RenderSystem;limitDisplayFPS(I)V"
      ),
      require = 0
   )
   public void fpsHook(int fps) {
      if (ClientSetting.INSTANCE == null || !ClientSetting.INSTANCE.fuckFPSLimit.getValue()) {
         RenderSystem.limitDisplayFPS(fps);
      }
   }

   @Inject(
      method = {"<init>"},
      at = {@At("TAIL")}
   )
   void postWindowInit(RunArgs args, CallbackInfo ci) {
      FontManager.init();
      if (Fonts.INSTANCE != null && Fonts.INSTANCE.isOn()) {
         Fonts.INSTANCE.refresh();
      }

      veyraStartTs = System.currentTimeMillis();
   }

   @Inject(
      method = {"setScreen"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void onSetScreen(Screen screen, CallbackInfo info) {
      if (Alien.EVENT_BUS != null) {
         OpenScreenEvent event = OpenScreenEvent.get(screen);
         Alien.EVENT_BUS.post(event);
         if (event.isCancelled()) {
            info.cancel();
         }
      }
   }

   @Inject(
      method = {"render"},
      at = {@At("TAIL")}
   )
   private void veyraMagentaOverlay(CallbackInfo ci) {
      if (Fonts.INSTANCE != null) {
         Fonts.INSTANCE.refreshIfPending();
      }

      if (FontManager.ui == null) {
         FontManager.init();
      }

      Screen screen = this.field_1755;
      if (screen != null) {
         boolean isMainMenu = screen instanceof TitleScreen;
         boolean isLoading = screen instanceof ProgressScreen || screen instanceof DownloadingTerrainScreen || screen instanceof ConnectScreen;
         if (isMainMenu || isLoading) {
            if (!isMainMenu || !noxisclient.mod.modules.impl.client.TitleScreen.shouldRender()) {
               if (Alien.SHADER != null && !Alien.SHADER.fullNullCheck()) {
                  float w = (float)this.field_1704.getScaledWidth();
                  float h = (float)this.field_1704.getScaledHeight();
                  float t = (float)(System.currentTimeMillis() % 100000L) / 1000.0F;
                  ManagedShaderEffect gradient = Alien.SHADER.getShader(ShaderManager.Shader.Gradient);
                  if (gradient != null) {
                     gradient.setUniformValue("texelSize", 1.0F / Math.max(1.0F, w), 1.0F / Math.max(1.0F, h));
                     gradient.setUniformValue("samples", 10);
                     gradient.setUniformValue("steps", 5);
                     gradient.setUniformValue("color", 0.78F, 0.05F, 0.59F, 0.85F);
                     gradient.setUniformValue("color1", 0.56F, 0.06F, 0.68F, 0.85F);
                     gradient.setUniformValue("factor", 180.0F);
                     gradient.setUniformValue("radius", 2.0F);
                     gradient.setUniformValue("fastOutline", 1);
                     gradient.setUniformValue("glow", 1);
                     gradient.setUniformValue("glowRadius", 2.0F);
                     gradient.setUniformValue("time", t * 300.0F);
                     gradient.render((((MinecraftClient)(Object)this)).getRenderTickCounter().getTickDelta(true));
                     ManagedShaderEffect pulse = Alien.SHADER.getShader(ShaderManager.Shader.Gradient);
                     if (pulse != null) {
                        pulse.setUniformValue("texelSize", 1.0F / Math.max(1.0F, w), 1.0F / Math.max(1.0F, h));
                        pulse.setUniformValue("samples", 10);
                        pulse.setUniformValue("steps", 5);
                        pulse.setUniformValue("color", 0.85F, 0.05F, 0.66F, 0.65F);
                        pulse.setUniformValue("color1", 0.56F, 0.06F, 0.68F, 0.65F);
                        pulse.setUniformValue("factor", 120.0F);
                        pulse.setUniformValue("radius", 2.0F);
                        pulse.setUniformValue("fastOutline", 1);
                        pulse.setUniformValue("glow", 1);
                        pulse.setUniformValue("glowRadius", 2.0F);
                        pulse.setUniformValue("time", t * 180.0F);
                        pulse.render((((MinecraftClient)(Object)this)).getRenderTickCounter().getTickDelta(true));
                     }

                     MatrixStack m = new MatrixStack();
                     w = (float)this.field_1704.getScaledWidth();
                     h = (float)this.field_1704.getScaledHeight();
                     long now = System.currentTimeMillis();
                     long elapsed = now - veyraStartTs;
                     if (elapsed < 2400L) {
                        float p = Math.max(0.0F, Math.min(1.0F, (float)elapsed / 2400.0F));
                        int aTop = (int)(180.0 * Math.sin((double)p * Math.PI));
                        Color c1 = new Color(28, 60, 110, aTop);
                        Color c2 = new Color(190, 50, 160, aTop);
                        Render2DUtil.verticalGradient(m, 0.0F, 0.0F, w, h, c1, c2);
                        float r = Math.min(w, h) * (0.12F + 0.18F * p);
                        Render2DUtil.drawCircle(m, w / 2.0F, h / 2.0F, r, new Color(255, 255, 255, (int)(70.0F * p)), 80);
                        Render2DUtil.drawCircle(m, w / 2.0F, h / 2.0F, r * 1.2F, new Color(120, 220, 255, (int)(50.0F * p)), 80);
                     } else {
                        float phase = (float)(now % 6000L) / 6000.0F;
                        float angle = 0.523599F;
                        float dx = (float)Math.tan((double)angle) * h;
                        float base = -h;
                        float spacing = 28.0F;
                        float shift = phase * spacing * 5.2F;
                        int cA1 = new Color(230, 60, 170, 64).getRGB();
                        int cA2 = new Color(160, 40, 130, 48).getRGB();
                        int nodeC = new Color(255, 255, 255, 42).getRGB();

                        for (float i = base; i < w; i += spacing) {
                           float x0 = i + shift;
                           Render2DUtil.drawLine(m, x0, 0.0F, x0 + dx, h, cA1);
                           float nx = x0 + dx * 0.25F;
                           float ny = h * 0.25F;
                           Render2DUtil.drawCircle(m, nx, ny, 2.5F, new Color(nodeC, true), 32);
                        }

                        for (float var32 = base + spacing / 2.0F; var32 < w; var32 += spacing) {
                           float x0 = var32 + shift * 0.85F;
                           Render2DUtil.drawLine(m, x0, 0.0F, x0 + dx, h, cA2);
                           float nx = x0 + dx * 0.65F;
                           float ny = h * 0.6F;
                           Render2DUtil.drawCircle(m, nx, ny, 2.0F, new Color(nodeC, true), 28);
                        }
                     }
                  }
               }
            }
         }
      }
   }

   @Inject(
      method = {"doAttack"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/util/hit/HitResult;getType()Lnet/minecraft/util/hit/HitResult$Type;",
         shift = Shift.BEFORE
      )}
   )
   public void onAttack(CallbackInfoReturnable<Boolean> cir) {
      Alien.EVENT_BUS.post(DoAttackEvent.getPre());
   }

   @Inject(
      method = {"doAttack"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/network/ClientPlayerEntity;swingHand(Lnet/minecraft/util/Hand;)V",
         shift = Shift.AFTER
      )}
   )
   public void onAttackPost(CallbackInfoReturnable<Boolean> cir) {
      if (Alien.EVENT_BUS != null) {
         Alien.EVENT_BUS.post(DoAttackEvent.getPost());
      }
   }

   @Inject(
      method = {"doAttack"},
      at = {@At("HEAD")},
      cancellable = true
   )
   public void doAttackHook(CallbackInfoReturnable<Boolean> cir) {
      if (Alien.EVENT_BUS != null) {
         DoAttackEvent event = new DoAttackEvent();
         Alien.EVENT_BUS.post(event);
         if (event.isCancelled()) {
            cir.setReturnValue(false);
            return;
         }
      }

      if (SpearFix.INSTANCE != null && this.field_1724 != null) {
         SpearFix.INSTANCE.playAttackSound(this.field_1724.getMainHandStack(), this.field_1765 instanceof EntityHitResult);
      }
   }

   @Inject(
      method = {"disconnect(Lnet/minecraft/client/gui/screen/Screen;)V"},
      at = {@At("HEAD")}
   )
   private void onDisconnect(Screen screen, CallbackInfo info) {
      if (Alien.EVENT_BUS != null) {
         Alien.EVENT_BUS.post(GameLeftEvent.INSTANCE);
      }

      if (Alien.MODULE != null) {
         Alien.MODULE.onLogout();
      }
   }

   @Inject(
      method = {"disconnect(Lnet/minecraft/client/gui/screen/Screen;)V"},
      at = {@At("HEAD")}
   )
   private void clearTitleMixin(Screen screen, CallbackInfo info) {
      if (ClientSetting.INSTANCE != null && ClientSetting.INSTANCE.titleFix.getValue()) {
         this.field_1705.clearTitle();
         this.field_1705.setDefaultTitleFade();
      }
   }

   @Inject(
      method = {"handleBlockBreaking"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void handleBlockBreaking(boolean breaking, CallbackInfo ci) {
      if (InteractTweaks.INSTANCE != null && this.field_1771 <= 0 && this.field_1724.isUsingItem() && InteractTweaks.INSTANCE.multiTask()) {
         if (breaking && this.field_1765 != null && this.field_1765.getType() == Type.BLOCK) {
            BlockHitResult blockHitResult = (BlockHitResult)(Object)this.field_1765;
            BlockPos blockPos = blockHitResult.getBlockPos();
            Direction direction;
            if (!this.field_1687.getBlockState(blockPos).isAir() && this.field_1761.updateBlockBreakingProgress(blockPos, direction = blockHitResult.getSide())
               )
             {
               this.field_1713.addBlockBreakingParticles(blockPos, direction);
               this.field_1724.swingHand(Hand.MAIN_HAND);
            }
         } else {
            this.field_1761.cancelBlockBreaking();
         }

         ci.cancel();
      }
   }

   @Inject(
      at = {@At("HEAD")},
      method = {"tick()V"}
   )
   public void tickHead(CallbackInfo info) {
      try {
         if (Alien.EVENT_BUS != null) {
            Alien.EVENT_BUS.post(ClientTickEvent.get(Event.Stage.Pre));
         }
      } catch (Exception var3) {
         var3.printStackTrace();
         if (ClientSetting.INSTANCE != null && ClientSetting.INSTANCE.debug.getValue()) {
            CommandManager.sendMessage("§4An error has occurred (MinecraftClient.tick() [HEAD]) Message: [" + var3.getMessage() + "]");
         }
      }
   }

   @Inject(
      at = {@At("TAIL")},
      method = {"tick()V"}
   )
   public void tickTail(CallbackInfo info) {
      try {
         if (Alien.EVENT_BUS != null) {
            Alien.EVENT_BUS.post(ClientTickEvent.get(Event.Stage.Post));
         }
      } catch (Exception var3) {
         var3.printStackTrace();
         if (ClientSetting.INSTANCE != null && ClientSetting.INSTANCE.debug.getValue()) {
            CommandManager.sendMessage("§4An error has occurred (MinecraftClient.tick() [TAIL]) Message: [" + var3.getMessage() + "]");
         }
      }
   }

   @Overwrite
   private String method_24287() {
      if (ClientSetting.INSTANCE == null) {
         return "Noxis 2.0: Loading..";
      } else if (ClientSetting.INSTANCE.titleOverride.getValue()) {
         return ClientSetting.INSTANCE.windowTitle.getValue();
      } else {
         StringBuilder stringBuilder = new StringBuilder("Noxis 2.0");
         stringBuilder.append(" ");
         stringBuilder.append(SharedConstants.getGameVersion().getName());
         ClientPlayNetworkHandler clientPlayNetworkHandler = this.method_1562();
         if (clientPlayNetworkHandler != null && clientPlayNetworkHandler.getConnection().isOpen()) {
            stringBuilder.append(" - ");
            ServerInfo serverInfo = this.method_1558();
            if (this.field_1766 != null && !this.field_1766.isRemote()) {
               stringBuilder.append(I18n.translate("title.singleplayer"));
            } else if (serverInfo != null && serverInfo.isRealm()) {
               stringBuilder.append(I18n.translate("title.multiplayer.realms"));
            } else if (this.field_1766 != null || serverInfo != null && serverInfo.isLocal()) {
               stringBuilder.append(I18n.translate("title.multiplayer.lan"));
            } else {
               stringBuilder.append(I18n.translate("title.multiplayer.other"));
            }
         }

         return stringBuilder.toString();
      }
   }

   @Shadow
   public ClientPlayNetworkHandler method_1562() {
      return null;
   }

   @Shadow
   public ServerInfo method_1558() {
      return null;
   }
}
