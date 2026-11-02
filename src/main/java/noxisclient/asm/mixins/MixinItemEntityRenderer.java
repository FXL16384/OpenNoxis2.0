package noxisclient.asm.mixins;

import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.ItemEntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory.Context;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.random.Random;
import noxisclient.mod.modules.impl.render.NoRender;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({ItemEntityRenderer.class})
public abstract class MixinItemEntityRenderer extends EntityRenderer<ItemEntity> {
   @Final
   @Shadow
   private ItemRenderer field_4726;
   @Final
   @Shadow
   private Random field_4725;

   protected MixinItemEntityRenderer(Context ctx) {
      super(ctx);
   }

   @Unique
   private static int getRenderedAmount(ItemStack stackSize) {
      int count = stackSize.getCount();
      if (count <= 1) {
         return 1;
      } else if (count <= 16) {
         return 2;
      } else if (count <= 32) {
         return 3;
      } else {
         return count <= 48 ? 4 : 5;
      }
   }

   @Inject(
      method = {"render*"},
      at = {@At("HEAD")},
      cancellable = true
   )
   public void render(ItemEntity itemEntity, float f, float g, MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, int i, CallbackInfo ci) {
      if (NoRender.INSTANCE.isOn() && NoRender.INSTANCE.fastItem.getValue()) {
         matrixStack.push();
         ItemStack itemStack = itemEntity.getStack();
         long j = itemStack.isEmpty() ? 187L : (long)(Item.getRawId(itemStack.getItem()) + itemStack.getDamage());
         this.field_4725.setSeed(j);
         BakedModel bakedModel = this.field_4726.getModel(itemStack, itemEntity.getWorld(), null, itemEntity.getId());
         boolean hasDepth = bakedModel.hasDepth();
         this.shadowRadius = NoRender.INSTANCE.castShadow.getValue() ? 0.15F : 0.0F;
         float l = MathHelper.sin(((float)itemEntity.getItemAge() + g) / 10.0F + itemEntity.uniqueOffset) * 0.1F + 0.1F;
         float m = bakedModel.getTransformation().getTransformation(ModelTransformationMode.GROUND).scale.y();
         matrixStack.translate(0.0F, l + 0.25F * m, 0.0F);
         matrixStack.multiply(this.dispatcher.getRotation());
         float o = bakedModel.getTransformation().ground.scale.x();
         float p = bakedModel.getTransformation().ground.scale.y();
         float q = bakedModel.getTransformation().ground.scale.z();
         int renderedAmount = getRenderedAmount(itemStack);
         if (!hasDepth) {
            float r = -0.0F * (float)(renderedAmount - 1) * 0.5F * o;
            float s = -0.0F * (float)(renderedAmount - 1) * 0.5F * p;
            float t = -0.09375F * (float)(renderedAmount - 1) * 0.5F * q;
            matrixStack.translate(r, s, t);
         }

         for (int u = 0; u < renderedAmount; u++) {
            matrixStack.push();
            if (u > 0) {
               if (hasDepth) {
                  float s = (this.field_4725.nextFloat() * 2.0F - 1.0F) * 0.15F;
                  float t = (this.field_4725.nextFloat() * 2.0F - 1.0F) * 0.15F;
                  float v = (this.field_4725.nextFloat() * 2.0F - 1.0F) * 0.15F;
                  matrixStack.translate(s, t, v);
               } else {
                  float s = (this.field_4725.nextFloat() * 2.0F - 1.0F) * 0.15F * 0.5F;
                  float t = (this.field_4725.nextFloat() * 2.0F - 1.0F) * 0.15F * 0.5F;
                  matrixStack.translate(s, t, 0.0F);
               }
            }

            this.field_4726
               .renderItem(itemStack, ModelTransformationMode.GROUND, false, matrixStack, vertexConsumerProvider, i, OverlayTexture.DEFAULT_UV, bakedModel);
            matrixStack.pop();
            if (!hasDepth) {
               matrixStack.translate(0.0F * o, 0.0F * p, 0.0425F * q);
            }
         }

         matrixStack.pop();
         super.render(itemEntity, f, g, matrixStack, vertexConsumerProvider, i);
         ci.cancel();
      }
   }
}
