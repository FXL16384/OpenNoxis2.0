package noxisclient.asm.mixins;

import java.awt.Color;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.dimension.DimensionType;
import noxisclient.mod.modules.impl.render.Ambience;
import noxisclient.mod.modules.impl.render.NoRender;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({LightmapTextureManager.class})
public class MixinLightmapTextureManager {
   @Final
   @Shadow
   private NativeImageBackedTexture field_4138;
   @Final
   @Shadow
   private NativeImage field_4133;
   @Shadow
   private boolean field_4135;
   @Shadow
   private float field_21528;
   @Final
   @Shadow
   private GameRenderer field_4134;
   @Final
   @Shadow
   private MinecraftClient field_4137;

   @Shadow
   private static void method_46557(Vector3f vec) {
      vec.set(MathHelper.clamp(vec.x, 0.0F, 1.0F), MathHelper.clamp(vec.y, 0.0F, 1.0F), MathHelper.clamp(vec.z, 0.0F, 1.0F));
   }

   @Shadow
   public static float method_23284(DimensionType type, int lightLevel) {
      float f = (float)lightLevel / 15.0F;
      float g = f / (4.0F - 3.0F * f);
      return MathHelper.lerp(type.ambientLight(), g, 1.0F);
   }

   @Redirect(
      method = {"update"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/network/ClientPlayerEntity;hasStatusEffect(Lnet/minecraft/registry/entry/RegistryEntry;)Z",
         ordinal = 0,
         remap = false
      ),
      require = 0
   )
   private boolean nightVisionHook(ClientPlayerEntity instance, RegistryEntry<StatusEffect> registryEntry) {
      return Ambience.INSTANCE.isOn() && Ambience.INSTANCE.fullBright.getValue() ? true : instance.hasStatusEffect(registryEntry);
   }

   @Inject(
      method = {"update"},
      at = {@At("HEAD")},
      cancellable = true
   )
   public void updateHook(float delta, CallbackInfo ci) {
      if (Ambience.INSTANCE.isOn() && Ambience.INSTANCE.worldColor.booleanValue) {
         ci.cancel();
         if (this.field_4135) {
            this.field_4135 = false;
            this.field_4137.getProfiler().push("lightTex");
            ClientWorld clientWorld = this.field_4137.world;
            if (clientWorld != null) {
               float f = clientWorld.getSkyBrightness(1.0F);
               float g = clientWorld.getLightningTicksLeft() > 0 ? 1.0F : f * 0.95F + 0.05F;
               float h = this.field_4137.options.getDarknessEffectScale().getValue().floatValue();
               float i = this.method_42597(delta) * h;
               float j = this.method_42596(this.field_4137.player, i, delta) * h;
               Vector3f vector3f = new Vector3f(f, f, 1.0F).lerp(new Vector3f(1.0F, 1.0F, 1.0F), 0.35F);
               float m = this.field_21528 + 1.5F;
               Vector3f vector3f2 = new Vector3f();

               for (int n = 0; n < 16; n++) {
                  for (int o = 0; o < 16; o++) {
                     float p = method_23284(clientWorld.getDimension(), n) * g;
                     float q = method_23284(clientWorld.getDimension(), o) * m;
                     float s = q * ((q * 0.6F + 0.4F) * 0.6F + 0.4F);
                     float t = q * (q * q * 0.6F + 0.4F);
                     vector3f2.set(q, s, t);
                     boolean bl = clientWorld.getDimensionEffects().shouldBrightenLighting();
                     if (bl) {
                        vector3f2.lerp(new Vector3f(0.99F, 1.12F, 1.0F), 0.25F);
                        method_46557(vector3f2);
                     } else {
                        Vector3f vector3f3 = new Vector3f(vector3f).mul(p);
                        vector3f2.add(vector3f3);
                        vector3f2.lerp(new Vector3f(0.75F, 0.75F, 0.75F), 0.04F);
                        if (this.field_4134.getSkyDarkness(delta) > 0.0F) {
                           float u = this.field_4134.getSkyDarkness(delta);
                           Vector3f vector3f4 = new Vector3f(vector3f2).mul(0.7F, 0.6F, 0.6F);
                           vector3f2.lerp(vector3f4, u);
                        }
                     }

                     float v = Math.max(vector3f2.x(), Math.max(vector3f2.y(), vector3f2.z()));
                     if (v < 1.0F) {
                        this.field_4133
                           .setColor(
                              o,
                              n,
                              new Color(
                                    Ambience.INSTANCE.worldColor.getValue().getBlue(),
                                    Ambience.INSTANCE.worldColor.getValue().getGreen(),
                                    Ambience.INSTANCE.worldColor.getValue().getRed(),
                                    Ambience.INSTANCE.worldColor.getValue().getAlpha()
                                 )
                                 .getRGB()
                           );
                     } else {
                        if (!bl) {
                           if (j > 0.0F) {
                              vector3f2.add(-j, -j, -j);
                           }

                           method_46557(vector3f2);
                        }

                        v = this.field_4137.options.getGamma().getValue().floatValue();
                        Vector3f vector3f5 = new Vector3f(this.method_23795(vector3f2.x), this.method_23795(vector3f2.y), this.method_23795(vector3f2.z));
                        vector3f2.lerp(vector3f5, Math.max(0.0F, v - i));
                        vector3f2.lerp(new Vector3f(0.75F, 0.75F, 0.75F), 0.04F);
                        method_46557(vector3f2);
                        vector3f2.mul(255.0F);
                        int x = (int)vector3f2.x();
                        int y = (int)vector3f2.y();
                        int z = (int)vector3f2.z();
                        this.field_4133.setColor(o, n, 0xFF000000 | z << 16 | y << 8 | x);
                     }
                  }
               }

               this.field_4138.upload();
               this.field_4137.getProfiler().pop();
            }
         }
      }
   }

   @Inject(
      method = {"getDarknessFactor(F)F"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void getDarknessFactor(float tickDelta, CallbackInfoReturnable<Float> info) {
      if (NoRender.INSTANCE.isOn() && NoRender.INSTANCE.darkness.getValue()) {
         info.setReturnValue(0.0F);
      }
   }

   @Shadow
   private float method_23795(float x) {
      float f = 1.0F - x;
      return 1.0F - f * f * f * f;
   }

   @Shadow
   private float method_42597(float delta) {
      StatusEffectInstance statusEffectInstance = this.field_4137.player.getStatusEffect(StatusEffects.DARKNESS);
      return statusEffectInstance != null ? statusEffectInstance.getFadeFactor(this.field_4137.player, delta) : 0.0F;
   }

   @Shadow
   private float method_42596(LivingEntity entity, float factor, float delta) {
      float f = 0.45F * factor;
      return Math.max(0.0F, MathHelper.cos(((float)entity.age - delta) * (float) Math.PI * 0.025F) * f);
   }
}
