package noxisclient.asm.mixins;

import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleManager;
import noxisclient.Alien;
import noxisclient.api.events.impl.ParticleEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({ParticleManager.class})
public class MixinParticleManager {
   @Inject(
      at = {@At("HEAD")},
      method = {"addParticle(Lnet/minecraft/client/particle/Particle;)V"},
      cancellable = true
   )
   public void onAddParticle(Particle particle, CallbackInfo ci) {
      ParticleEvent event = ParticleEvent.get(particle);
      Alien.EVENT_BUS.post(event);
      if (event.isCancelled()) {
         ci.cancel();
      }
   }
}
