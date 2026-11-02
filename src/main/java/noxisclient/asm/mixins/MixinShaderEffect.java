package noxisclient.asm.mixins;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.PostEffectPass;
import net.minecraft.client.gl.PostEffectProcessor;
import noxisclient.api.interfaces.IShaderEffectHook;
import noxisclient.asm.accessors.IPostProcessShader;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({PostEffectProcessor.class})
public class MixinShaderEffect implements IShaderEffectHook {
   @Unique
   private List<String> fakedBufferNames;
   @Shadow
   @Final
   private Map<String, Framebuffer> field_1495;
   @Shadow
   @Final
   private List<PostEffectPass> field_1497;

   @Override
   public void alienClient$addHook(String name, Framebuffer buffer) {
      this.ensureFakeBuffers();
      Framebuffer previousFramebuffer = this.field_1495.get(name);
      if (previousFramebuffer != buffer) {
         if (previousFramebuffer != null) {
            for (PostEffectPass pass : this.field_1497) {
               if (pass.input == previousFramebuffer) {
                  ((IPostProcessShader)pass).setInput(buffer);
               }

               if (pass.output == previousFramebuffer) {
                  ((IPostProcessShader)pass).setOutput(buffer);
               }
            }

            this.field_1495.remove(name);
            this.fakedBufferNames.remove(name);
         }

         this.field_1495.put(name, buffer);
         this.fakedBufferNames.add(name);
      }
   }

   @Inject(
      method = {"close"},
      at = {@At("HEAD")}
   )
   void deleteFakeBuffersHook(CallbackInfo ci) {
      this.ensureFakeBuffers();

      for (String fakedBufferName : this.fakedBufferNames) {
         this.field_1495.remove(fakedBufferName);
      }
   }

   @Unique
   private void ensureFakeBuffers() {
      if (this.fakedBufferNames == null) {
         this.fakedBufferNames = new ArrayList<>();
      }
   }
}
