package noxisclient.asm.mixins;

import net.fabricmc.fabric.api.client.model.loading.v1.FabricBakedModelManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import noxisclient.api.utils.render.SimpleItemModel;
import noxisclient.mod.modules.impl.render.NoRender;
import noxisclient.mod.modules.impl.render.SpearFix;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({ItemRenderer.class})
public class MixinItemRenderer {
   @Unique
   private SimpleItemModel flattenedModel;
   @Unique
   private ModelTransformationMode renderMode;

   @Inject(
      method = {"renderItem*"},
      at = {@At("HEAD")}
   )
   private void getRenderType(
      ItemStack itemStack,
      ModelTransformationMode transformationMode,
      boolean leftHand,
      MatrixStack matrices,
      VertexConsumerProvider vertexConsumers,
      int light,
      int overlay,
      BakedModel model,
      CallbackInfo ci
   ) {
      this.renderMode = transformationMode;
   }

   @ModifyVariable(
      method = {"renderItem(Lnet/minecraft/item/ItemStack;Lnet/minecraft/client/render/model/json/ModelTransformationMode;ZLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;IILnet/minecraft/client/render/model/BakedModel;)V"},
      at = @At("HEAD"),
      index = 8,
      argsOnly = true,
      require = 0
   )
   private BakedModel spearFix$replaceRenderModel(
      BakedModel model,
      ItemStack stack,
      ModelTransformationMode transformationMode,
      boolean leftHand,
      MatrixStack matrices,
      VertexConsumerProvider vertexConsumers,
      int light,
      int overlay,
      BakedModel originalModel
   ) {
      BakedModel spearModel = this.spearFix$getSpearModel(stack, transformationMode);
      return spearModel == null ? model : spearModel;
   }

   @ModifyVariable(
      method = {"renderBakedItemModel"},
      at = @At("HEAD"),
      index = 1,
      argsOnly = true,
      require = 0
   )
   private BakedModel replaceItemModelClass(
      BakedModel model, BakedModel arg, ItemStack stack, int light, int overlay, MatrixStack matrices, VertexConsumer vertices
   ) {
      BakedModel spearModel = this.spearFix$getSpearModel(stack, this.renderMode);
      if (spearModel != null) {
         return spearModel;
      } else if (NoRender.INSTANCE.isOn()
         && NoRender.INSTANCE.fastItem.getValue()
         && !NoRender.INSTANCE.renderSidesOfItems.getValue()
         && !stack.isEmpty()
         && !model.hasDepth()
         && this.renderMode == ModelTransformationMode.GROUND) {
         this.ensureFlattenedModel();
         this.flattenedModel.setItem(model);
         return this.flattenedModel;
      } else {
         return model;
      }
   }

   @Unique
   private BakedModel spearFix$getSpearModel(ItemStack stack, ModelTransformationMode mode) {
      return SpearFix.INSTANCE != null && SpearFix.INSTANCE.shouldReplaceModel(stack) && mode != null
         ? ((FabricBakedModelManager)MinecraftClient.getInstance().getBakedModelManager()).getModel(SpearFix.INSTANCE.getModelId(stack, mode))
         : null;
   }

   @Unique
   private void ensureFlattenedModel() {
      if (this.flattenedModel == null) {
         this.flattenedModel = new SimpleItemModel();
      }
   }
}
