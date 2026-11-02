package noxisclient.mod.modules.impl.render.mace;

import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleFactory;
import net.minecraft.client.particle.ParticleTextureSheet;
import net.minecraft.client.particle.SpriteBillboardParticle;
import net.minecraft.client.particle.SpriteProvider;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particle.SimpleParticleType;

public class MaceFlashParticle extends SpriteBillboardParticle {
   private final SpriteProvider spriteProvider;

   protected MaceFlashParticle(ClientWorld world, double x, double y, double z, SpriteProvider spriteProvider) {
      super(world, x, y, z, 0.0, 0.0, 0.0);
      this.spriteProvider = spriteProvider;
      this.maxAge = 8;
      this.scale = 1.8F;
      this.gravityStrength = 0.0F;
      this.alpha = 0.85F;
      this.setSprite(spriteProvider);
   }

   @Override
   public void tick() {
      super.tick();
      this.setSpriteForAge(this.spriteProvider);
      this.alpha = Math.max(0.0F, 0.85F * (1.0F - (float)this.age / (float)this.maxAge));
      this.scale += 0.08F;
   }

   @Override
   public ParticleTextureSheet getType() {
      return ParticleTextureSheet.PARTICLE_SHEET_TRANSLUCENT;
   }

   public static class Factory implements ParticleFactory<SimpleParticleType> {
      private final SpriteProvider spriteProvider;

      public Factory(SpriteProvider spriteProvider) {
         this.spriteProvider = spriteProvider;
      }

      public Particle createParticle(
         SimpleParticleType parameters, ClientWorld world, double x, double y, double z, double velocityX, double velocityY, double velocityZ
      ) {
         return new MaceFlashParticle(world, x, y, z, this.spriteProvider);
      }
   }
}
