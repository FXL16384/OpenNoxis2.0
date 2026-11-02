package noxisclient.api.utils.render;

import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.block.BlockState;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.BakedQuad;
import net.minecraft.client.render.model.json.ModelOverrideList;
import net.minecraft.client.render.model.json.ModelTransformation;
import net.minecraft.client.texture.Sprite;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import org.jetbrains.annotations.Nullable;

public class SimpleItemModel implements BakedModel {
   private BakedModel flattenedItem;
   private final List<BakedQuad> nullQuadList = new ArrayList<>();

   public void setItem(BakedModel model) {
      this.flattenedItem = model;
   }

   private boolean isCorrectDirectionForType(Direction direction) {
      return direction == Direction.SOUTH;
   }

   @Override
   public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction face, Random random) {
      if (face != null) {
         return (List<BakedQuad>)(this.isCorrectDirectionForType(face) ? this.flattenedItem.getQuads(state, face, random) : ImmutableList.of());
      } else {
         this.nullQuadList.clear();

         for (BakedQuad quad : this.flattenedItem.getQuads(state, null, random)) {
            if (this.isCorrectDirectionForType(quad.getFace())) {
               this.nullQuadList.add(quad);
            }
         }

         return this.nullQuadList;
      }
   }

   @Override
   public boolean useAmbientOcclusion() {
      return this.flattenedItem.useAmbientOcclusion();
   }

   @Override
   public boolean hasDepth() {
      return this.flattenedItem.hasDepth();
   }

   @Override
   public boolean isSideLit() {
      return this.flattenedItem.isSideLit();
   }

   @Override
   public boolean isBuiltin() {
      return this.flattenedItem.isBuiltin();
   }

   @Override
   public Sprite getParticleSprite() {
      return this.flattenedItem.getParticleSprite();
   }

   @Override
   public ModelTransformation getTransformation() {
      return this.flattenedItem.getTransformation();
   }

   @Override
   public ModelOverrideList getOverrides() {
      return this.flattenedItem.getOverrides();
   }
}
