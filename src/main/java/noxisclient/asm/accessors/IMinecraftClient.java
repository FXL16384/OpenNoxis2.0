package noxisclient.asm.accessors;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.session.Session;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin({MinecraftClient.class})
public interface IMinecraftClient {
   @Mutable
   @Accessor("session")
   void setSession(Session var1);

   @Invoker("doItemUse")
   void invokeDoItemUse();
}
