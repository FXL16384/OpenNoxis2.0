package noxisclient.mod.modules.impl.render.mace;

import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleFactory;
import net.minecraft.client.particle.ParticleTextureSheet;
import net.minecraft.client.particle.SpriteBillboardParticle;
import net.minecraft.client.particle.SpriteProvider;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particle.SimpleParticleType;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public class MaceSmashParticle extends SpriteBillboardParticle {
   private static final Quaternionf FLAT_ROTATION = new Quaternionf(0.0F, -0.7F, 0.7F, 0.0F);
   private float expansion;
   private final float sizeStep;
   private float alphaControl;

   protected MaceSmashParticle(ClientWorld world, double x, double y, double z, SpriteProvider spriteProvider, double expansion, double alpha, double sizeStep) {
      super(world, x, y, z, expansion, alpha, sizeStep);
      this.expansion = (float)expansion;
      this.alphaControl = (float)alpha;
      this.sizeStep = (float)sizeStep;
      this.alpha = MathHelper.clamp(this.alphaControl, 0.0F, 1.0F);
      this.scale = 0.5F;
      this.maxAge = 40;
      this.gravityStrength = 0.0F;
      this.setSprite(spriteProvider);
   }

   @Override
   public void buildGeometry(VertexConsumer vertexConsumer, Camera camera, float tickDelta) {
      Vec3d cameraPos = camera.getPos();
      float x = (float)(MathHelper.lerp((double)tickDelta, this.prevPosX, this.x) - cameraPos.getX());
      float y = (float)(MathHelper.lerp((double)tickDelta, this.prevPosY, this.y) - cameraPos.getY());
      float z = (float)(MathHelper.lerp((double)tickDelta, this.prevPosZ, this.z) - cameraPos.getZ());
      float size = this.getSize(tickDelta);
      Vector3f[] top = new Vector3f[]{
         new Vector3f(-1.0F, -1.0F, 0.0F), new Vector3f(-1.0F, 1.0F, 0.0F), new Vector3f(1.0F, 1.0F, 0.0F), new Vector3f(1.0F, -1.0F, 0.0F)
      };
      Vector3f[] bottom = new Vector3f[]{
         new Vector3f(-1.0F, -1.0F, 0.0F), new Vector3f(1.0F, -1.0F, 0.0F), new Vector3f(1.0F, -1.0F, 0.0F), new Vector3f(-1.0F, -1.0F, 0.0F)
      };

      for (int i = 0; i < 4; i++) {
         top[i].rotate(FLAT_ROTATION).mul(size).add(x, y, z);
         bottom[i].rotate(FLAT_ROTATION).mul(size).add(x, y - 0.1F, z);
      }

      float minU = this.getMinU();
      float maxU = this.getMaxU();
      float minV = this.getMinV();
      float maxV = this.getMaxV();
      int light = this.getBrightness(tickDelta);
      this.vertex(vertexConsumer, top[0], maxU, maxV, light);
      this.vertex(vertexConsumer, top[1], maxU, minV, light);
      this.vertex(vertexConsumer, top[2], minU, minV, light);
      this.vertex(vertexConsumer, top[3], minU, maxV, light);
      this.vertex(vertexConsumer, bottom[3], minU, maxV, light);
      this.vertex(vertexConsumer, bottom[2], minU, minV, light);
      this.vertex(vertexConsumer, bottom[1], maxU, minV, light);
      this.vertex(vertexConsumer, bottom[0], maxU, maxV, light);
   }

   private void vertex(VertexConsumer vertexConsumer, Vector3f pos, float u, float v, int light) {
      vertexConsumer.vertex(pos.x(), pos.y(), pos.z()).texture(u, v).color(this.red, this.green, this.blue, this.alpha).light(light);
   }

   @Override
   public void tick() {
      super.tick();
      this.scale = this.scale + this.expansion * this.sizeStep;
      this.expansion = MathHelper.clamp(this.expansion - 1.0F / (float)this.maxAge, 0.0F, 1.0F);
      this.alphaControl = this.alphaControl - 1.0F / (float)this.maxAge;
      this.alpha = MathHelper.clamp(this.alphaControl, 0.0F, 1.0F);
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
         return new MaceSmashParticle(world, x, y, z, this.spriteProvider, velocityX, velocityY, velocityZ);
      }
   }
}
