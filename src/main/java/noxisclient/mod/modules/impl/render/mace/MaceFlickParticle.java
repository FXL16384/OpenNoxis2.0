package noxisclient.mod.modules.impl.render.mace;

import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleFactory;
import net.minecraft.client.particle.ParticleTextureSheet;
import net.minecraft.client.particle.SpriteBillboardParticle;
import net.minecraft.client.particle.SpriteProvider;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particle.SimpleParticleType;
import net.minecraft.util.math.MathHelper;

public class MaceFlickParticle extends SpriteBillboardParticle {
   private float time;
   private final float scaleControl;

   protected MaceFlickParticle(ClientWorld world, double x, double y, double z, SpriteProvider spriteProvider, double scale, double velocityY, double velocityZ) {
      super(world, x, y, z, 0.0, velocityY, velocityZ);
      this.alpha = 1.0F;
      this.scaleControl = (float)scale;
      this.maxAge = 20;
      this.time = 0.0F;
      this.gravityStrength = 0.0F;
      this.setSprite(spriteProvider);
   }

   @Override
   public void tick() {
      super.tick();
      this.alpha = MathHelper.clamp((1.0F - this.time) * 1.5F, 0.2F, 1.0F) - 0.2F;
      this.time = this.time + 1.0F / (float)this.maxAge;
      this.scale = this.alpha * this.scaleControl;
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
         return new MaceFlickParticle(world, x, y, z, this.spriteProvider, velocityX, velocityY, velocityZ);
      }
   }
}
