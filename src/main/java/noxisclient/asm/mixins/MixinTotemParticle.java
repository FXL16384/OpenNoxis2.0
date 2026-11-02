package noxisclient.asm.mixins;

import java.awt.Color;
import net.minecraft.client.particle.SpriteProvider;
import net.minecraft.client.particle.TotemParticle;
import net.minecraft.client.world.ClientWorld;
import noxisclient.Alien;
import noxisclient.api.events.impl.TotemParticleEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({TotemParticle.class})
public abstract class MixinTotemParticle extends MixinParticle {
   @Inject(
      method = {"<init>"},
      at = {@At("TAIL")}
   )
   private void hookInit(
      ClientWorld world, double x, double y, double z, double velocityX, double velocityY, double velocityZ, SpriteProvider spriteProvider, CallbackInfo ci
   ) {
      TotemParticleEvent event = TotemParticleEvent.get(velocityX, velocityY, velocityZ);
      Alien.EVENT_BUS.post(event);
      if (event.isCancelled()) {
         this.field_3852 = event.velocityX;
         this.field_3869 = event.velocityY;
         this.field_3850 = event.velocityZ;
         Color color = event.color;
         if (color != null) {
            this.method_3084((float)color.getRed() / 255.0F, (float)color.getGreen() / 255.0F, (float)color.getBlue() / 255.0F);
            this.method_3083((float)color.getAlpha() / 255.0F);
         }
      }
   }
}
