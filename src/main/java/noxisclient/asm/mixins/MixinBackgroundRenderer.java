package noxisclient.asm.mixins;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.render.BackgroundRenderer;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.BackgroundRenderer.FogType;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.registry.entry.RegistryEntry;
import noxisclient.mod.modules.impl.render.Ambience;
import noxisclient.mod.modules.impl.render.NoRender;
import noxisclient.mod.modules.impl.render.Xray;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({BackgroundRenderer.class})
public class MixinBackgroundRenderer {
   @Redirect(
      method = {"render"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/entity/LivingEntity;hasStatusEffect(Lnet/minecraft/registry/entry/RegistryEntry;)Z",
         ordinal = 0,
         remap = false
      ),
      require = 0
   )
   private static boolean nightVisionHook(LivingEntity instance, RegistryEntry<StatusEffect> effect) {
      return Ambience.INSTANCE.isOn() && Ambience.INSTANCE.fullBright.getValue() ? true : instance.hasStatusEffect(effect);
   }

   @Inject(
      method = {"applyFog"},
      at = {@At("HEAD")}
   )
   private static void onApplyFog(Camera camera, FogType fogType, float viewDistance, boolean thickFog, float tickDelta, CallbackInfo info) {
      if (Ambience.INSTANCE.isOn()) {
         if (Ambience.INSTANCE.fog.booleanValue) {
            RenderSystem.setShaderFogColor(
               (float)Ambience.INSTANCE.fog.getValue().getRed() / 255.0F,
               (float)Ambience.INSTANCE.fog.getValue().getGreen() / 255.0F,
               (float)Ambience.INSTANCE.fog.getValue().getBlue() / 255.0F,
               (float)Ambience.INSTANCE.fog.getValue().getAlpha() / 255.0F
            );
         }

         if (Ambience.INSTANCE.fogDistance.getValue()) {
            RenderSystem.setShaderFogStart(Ambience.INSTANCE.fogStart.getValueFloat());
            RenderSystem.setShaderFogEnd(Ambience.INSTANCE.fogEnd.getValueFloat());
         }
      }

      if ((NoRender.INSTANCE.isOn() && NoRender.INSTANCE.fog.getValue() || Xray.INSTANCE.isOn()) && fogType == FogType.FOG_TERRAIN) {
         RenderSystem.setShaderFogStart(viewDistance * 4.0F);
         RenderSystem.setShaderFogEnd(viewDistance * 4.25F);
      }
   }

   @Inject(
      method = {"getFogModifier(Lnet/minecraft/entity/Entity;F)Lnet/minecraft/client/render/BackgroundRenderer$StatusEffectFogModifier;"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private static void onGetFogModifier(Entity entity, float tickDelta, CallbackInfoReturnable<Object> info) {
      if (NoRender.INSTANCE.isOn() && NoRender.INSTANCE.blindness.getValue()) {
         info.setReturnValue(null);
      }
   }
}
