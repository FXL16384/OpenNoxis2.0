package noxisclient.asm.mixins;

import net.minecraft.client.render.RenderTickCounter.Dynamic;
import noxisclient.Alien;
import noxisclient.api.events.impl.TimerEvent;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({Dynamic.class})
public class MixinRenderTickCounter {
   @Shadow
   private float field_51958;
   @Shadow
   private float field_51959;
   @Shadow
   private long field_51962;
   @Final
   @Shadow
   private float field_51964;

   @Inject(
      method = {"beginRenderTick(J)I"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void beginRenderTickHook(long timeMillis, CallbackInfoReturnable<Integer> cir) {
      TimerEvent event = TimerEvent.getEvent();
      Alien.EVENT_BUS.post(event);
      if (!event.isCancelled()) {
         float timer = event.isModified() ? event.get() : Alien.TIMER.get();
         if (timer == 1.0F) {
            return;
         }

         this.field_51958 = (float)(timeMillis - this.field_51962) / this.field_51964 * timer;
         this.field_51962 = timeMillis;
         this.field_51959 = this.field_51959 + this.field_51958;
         int i = (int)this.field_51959;
         this.field_51959 -= (float)i;
         cir.setReturnValue(i);
      }
   }
}
