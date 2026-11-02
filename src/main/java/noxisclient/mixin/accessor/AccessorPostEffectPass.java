package noxisclient.mixin.accessor;

import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.PostEffectPass;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin({PostEffectPass.class})
public interface AccessorPostEffectPass {
   @Accessor("input")
   Framebuffer hookGetInput();

   @Accessor("input")
   void hookSetInput(Framebuffer var1);

   @Accessor("output")
   Framebuffer hookGetOutput();

   @Accessor("output")
   void hookSetOutput(Framebuffer var1);
}
