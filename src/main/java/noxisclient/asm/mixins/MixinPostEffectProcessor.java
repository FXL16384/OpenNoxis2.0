package noxisclient.asm.mixins;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.PostEffectPass;
import net.minecraft.client.gl.PostEffectProcessor;
import net.minecraft.util.Identifier;
import noxisclient.asm.accessors.IPostProcessShader;
import noxisclient.impl.imixin.IPostEffectProcessor;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({PostEffectProcessor.class})
public class MixinPostEffectProcessor implements IPostEffectProcessor {
   @Shadow
   @Final
   private List<PostEffectPass> field_1497;
   @Shadow
   @Final
   private Map<String, Framebuffer> field_1495;
   @Unique
   private List<String> bufferTargets;

   @WrapOperation(
      method = {"parsePass"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/util/Identifier;ofVanilla(Ljava/lang/String;)Lnet/minecraft/util/Identifier;"
      )}
   )
   private Identifier resolveNamespacedAuxTexture(String path, Operation<Identifier> original) {
      int namespaceSeparator = path.indexOf(58);
      if (namespaceSeparator < 0) {
         return original.call(path);
      } else {
         String namespacePath = path.substring(0, namespaceSeparator);
         int resourcePathStart = namespacePath.lastIndexOf(47) + 1;
         String namespace = namespacePath.substring(resourcePathStart);
         String resourcePath = namespacePath.substring(0, resourcePathStart) + path.substring(namespaceSeparator + 1);
         return Identifier.of(namespace, resourcePath);
      }
   }

   @Override
   public void overwriteBuffer(String name, Framebuffer buffer) {
      this.ensureBufferTargets();
      Framebuffer target = this.field_1495.get(name);
      if (target != buffer) {
         if (target != null) {
            for (PostEffectPass pass : this.field_1497) {
               if (pass.input == target) {
                  ((IPostProcessShader)pass).setInput(buffer);
               }

               if (pass.output == target) {
                  ((IPostProcessShader)pass).setOutput(buffer);
               }
            }

            this.field_1495.remove(name);
            this.bufferTargets.remove(name);
         }

         this.field_1495.put(name, buffer);
         this.bufferTargets.add(name);
      }
   }

   @Inject(
      method = {"close"},
      at = {@At("HEAD")}
   )
   private void hookClose(CallbackInfo ci) {
      this.ensureBufferTargets();

      for (String fakedBufferName : this.bufferTargets) {
         this.field_1495.remove(fakedBufferName);
      }
   }

   @Unique
   private void ensureBufferTargets() {
      if (this.bufferTargets == null) {
         this.bufferTargets = new CopyOnWriteArrayList<>();
      }
   }
}
