package noxisclient.asm.mixins;

import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import noxisclient.Vitality;
import noxisclient.api.events.impl.RenderEntityEvent;
import noxisclient.mod.modules.impl.render.Stickman;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({EntityRenderDispatcher.class})
public class MixinEntityRenderDispatcher {
   @Inject(
      method = {"render"},
      at = {@At("HEAD")},
      cancellable = true
   )
   public <E extends Entity> void onRender(
      E entity,
      double x,
      double y,
      double z,
      float yaw,
      float tickDelta,
      MatrixStack matrices,
      VertexConsumerProvider vertexConsumers,
      int light,
      CallbackInfo ci
   ) {
      RenderEntityEvent event = RenderEntityEvent.get(entity);
      Vitality.EVENT_BUS.post(event);
      if (event.isCancelled()) {
         ci.cancel();
      } else {
         if (entity instanceof PlayerEntity && Stickman.INSTANCE != null && Stickman.INSTANCE.shouldReplace((PlayerEntity)entity)) {
            Stickman.INSTANCE.renderPlayer(matrices, (PlayerEntity)entity, x, y, z, tickDelta);
            ci.cancel();
         }
      }
   }
}
