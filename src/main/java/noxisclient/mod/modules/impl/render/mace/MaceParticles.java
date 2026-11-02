package noxisclient.mod.modules.impl.render.mace;

import net.fabricmc.fabric.api.client.particle.v1.ParticleFactoryRegistry;
import net.fabricmc.fabric.api.particle.v1.FabricParticleTypes;
import net.minecraft.particle.SimpleParticleType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public final class MaceParticles {
   public static final SimpleParticleType SMASH = FabricParticleTypes.simple();
   public static final SimpleParticleType FLICK = FabricParticleTypes.simple();
   public static final SimpleParticleType FLASH = FabricParticleTypes.simple();
   private static boolean registered;

   private MaceParticles() {
   }

   public static void register() {
      if (!registered) {
         registered = true;
         Registry.register(Registries.PARTICLE_TYPE, Identifier.of("noxisclient", "mace_smash"), SMASH);
         Registry.register(Registries.PARTICLE_TYPE, Identifier.of("noxisclient", "mace_flick"), FLICK);
         Registry.register(Registries.PARTICLE_TYPE, Identifier.of("noxisclient", "mace_flash"), FLASH);
         ParticleFactoryRegistry registry = ParticleFactoryRegistry.getInstance();
         registry.register(SMASH, MaceSmashParticle.Factory::new);
         registry.register(FLICK, MaceFlickParticle.Factory::new);
         registry.register(FLASH, MaceFlashParticle.Factory::new);
      }
   }
}
