package noxisclient.asm.mixins;

import com.mojang.serialization.MapCodec;
import it.unimi.dsi.fastutil.objects.Reference2ObjectArrayMap;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.AbstractBlock.AbstractBlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.state.property.Property;
import net.minecraft.util.ActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.world.World;
import noxisclient.Vitality;
import noxisclient.api.events.impl.BlockActivateEvent;
import org.spongepowered.asm.mixin.Mixin;

@Mixin({BlockState.class})
public abstract class MixinBlockState extends AbstractBlockState {
   public MixinBlockState(Block block, Reference2ObjectArrayMap<Property<?>, Comparable<?>> propertyMap, MapCodec<BlockState> mapCodec) {
      super(block, propertyMap, mapCodec);
   }

   @Override
   public ActionResult onUse(World world, PlayerEntity player, BlockHitResult hit) {
      Vitality.EVENT_BUS.post(BlockActivateEvent.get(BlockState.class.cast(this)));
      return super.onUse(world, player, hit);
   }
}
