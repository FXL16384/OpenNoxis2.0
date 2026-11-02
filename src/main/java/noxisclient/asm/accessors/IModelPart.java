package noxisclient.asm.accessors;

import java.util.List;
import java.util.Map;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.model.ModelPart.Cuboid;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin({ModelPart.class})
public interface IModelPart {
   @Accessor("cuboids")
   List<Cuboid> getCuboids();

   @Accessor("children")
   Map<String, ModelPart> getChildren();
}
