package noxisclient.mod.modules.impl.render;

import com.mojang.blaze3d.systems.RenderSystem;
import java.awt.Color;
import java.text.DecimalFormat;
import net.minecraft.block.Blocks;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.render.VertexFormat.DrawMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.entity.projectile.thrown.EnderPearlEntity;
import net.minecraft.entity.projectile.thrown.ExperienceBottleEntity;
import net.minecraft.item.BowItem;
import net.minecraft.item.CrossbowItem;
import net.minecraft.item.EggItem;
import net.minecraft.item.EnderPearlItem;
import net.minecraft.item.ExperienceBottleItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.LingeringPotionItem;
import net.minecraft.item.SnowballItem;
import net.minecraft.item.SplashPotionItem;
import net.minecraft.item.TridentItem;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.hit.HitResult.Type;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.RaycastContext.FluidHandling;
import net.minecraft.world.RaycastContext.ShapeType;
import noxisclient.Alien;
import noxisclient.api.utils.math.MathUtil;
import noxisclient.api.utils.render.ColorUtil;
import noxisclient.api.utils.render.Render3DUtil;
import noxisclient.mod.modules.Module;
import noxisclient.mod.modules.impl.player.AutoPearl;
import noxisclient.mod.modules.settings.impl.BooleanSetting;
import noxisclient.mod.modules.settings.impl.ColorSetting;
import noxisclient.mod.modules.settings.impl.SliderSetting;
import org.joml.Matrix4f;
import org.joml.Vector3f;

public class Trajectories extends Module {
   static MatrixStack matrixStack;
   private static final Identifier PEARL_ICON_TEXTURE = Identifier.of("minecraft", "textures/item/ender_pearl.png");
   private final DecimalFormat timeFormat = new DecimalFormat("0.0");
   private final ColorSetting hand = this.add(new ColorSetting("Hand", new Color(255, 255, 255, 255)).injectBoolean(true));
   private final ColorSetting pearl = this.add(new ColorSetting("Pearl", new Color(255, 255, 255, 255)).injectBoolean(true));
   private final ColorSetting arrow = this.add(new ColorSetting("Arrow", new Color(255, 255, 255, 255)).injectBoolean(true));
   private final ColorSetting xp = this.add(new ColorSetting("XP", new Color(255, 255, 255, 255)).injectBoolean(true));
   private final BooleanSetting pearlLanding = this.add(new BooleanSetting("PearlLanding", true).setParent());
   private final SliderSetting pearlLandingScale = this.add(new SliderSetting("Scale", 1.0, 0.0, 3.0, 0.1, this.pearlLanding::isOpen));
   private final BooleanSetting pearlLandingFactor = this.add(new BooleanSetting("Factor", true, this.pearlLanding::isOpen).setParent());
   private final SliderSetting pearlLandingScaling = this.add(new SliderSetting("Scaling", 1.0, 0.0, 3.0, 0.1, this.pearlLandingFactor::isOpen));
   private final SliderSetting pearlLandingDistance = this.add(new SliderSetting("Distance", 6.0, 0.0, 20.0, 0.1, this.pearlLandingFactor::isOpen));

   public Trajectories() {
      super("Trajectories", Module.Category.Render);
      this.setChinese("抛物线预测");
   }

   @Override
   public void onRender3D(MatrixStack matrixStack) {
      if (!nullCheck()) {
         Trajectories.matrixStack = matrixStack;
         if (this.pearl.booleanValue || this.arrow.booleanValue || this.xp.booleanValue) {
            RenderSystem.disableDepthTest();

            for (Entity en : Alien.THREAD.getEntities()) {
               if (en instanceof EnderPearlEntity && this.pearl.booleanValue) {
                  this.calcTrajectory(en, this.pearl.getValue());
               }

               if (en instanceof ArrowEntity && this.arrow.booleanValue) {
                  this.calcTrajectory(en, this.arrow.getValue());
               }

               if (en instanceof ExperienceBottleEntity && this.xp.booleanValue) {
                  this.calcTrajectory(en, this.xp.getValue());
               }
            }

            RenderSystem.enableDepthTest();
         }

         if (this.hand.booleanValue) {
            if (!mc.options.getPerspective().isFirstPerson()) {
               return;
            }

            ItemStack mainHand = mc.player.getMainHandStack();
            ItemStack offHand = mc.player.getOffHandStack();
            Hand hand;
            if (!(mainHand.getItem() instanceof BowItem)
               && !(mainHand.getItem() instanceof CrossbowItem)
               && !this.isThrowable(mainHand.getItem())
               && !AutoPearl.INSTANCE.isOn()) {
               if (!(offHand.getItem() instanceof BowItem) && !(offHand.getItem() instanceof CrossbowItem) && !this.isThrowable(offHand.getItem())) {
                  return;
               }

               hand = Hand.OFF_HAND;
            } else {
               hand = Hand.MAIN_HAND;
            }

            RenderSystem.disableDepthTest();
            boolean prev_bob = mc.options.getBobView().getValue();
            mc.options.getBobView().setValue(false);
            double x = MathUtil.interpolate(mc.player.prevX, mc.player.getX(), (double)mc.getRenderTickCounter().getTickDelta(true));
            double y = MathUtil.interpolate(mc.player.prevY, mc.player.getY(), (double)mc.getRenderTickCounter().getTickDelta(true));
            double z = MathUtil.interpolate(mc.player.prevZ, mc.player.getZ(), (double)mc.getRenderTickCounter().getTickDelta(true));
            if (offHand.getItem() instanceof CrossbowItem
                  && EnchantmentHelper.getLevel(
                        mc.world.getRegistryManager().get(Enchantments.MULTISHOT.getRegistryRef()).getEntry(Enchantments.MULTISHOT).get(), offHand
                     )
                     != 0
               || mainHand.getItem() instanceof CrossbowItem
                  && EnchantmentHelper.getLevel(
                        mc.world.getRegistryManager().get(Enchantments.MULTISHOT.getRegistryRef()).getEntry(Enchantments.MULTISHOT).get(), mainHand
                     )
                     != 0) {
               this.calcTrajectory(hand == Hand.OFF_HAND ? offHand.getItem() : mainHand.getItem(), mc.player.getYaw() - 10.0F, x, y, z);
               this.calcTrajectory(hand == Hand.OFF_HAND ? offHand.getItem() : mainHand.getItem(), mc.player.getYaw(), x, y, z);
               this.calcTrajectory(hand == Hand.OFF_HAND ? offHand.getItem() : mainHand.getItem(), mc.player.getYaw() + 10.0F, x, y, z);
            } else {
               this.calcTrajectory(hand == Hand.OFF_HAND ? offHand.getItem() : mainHand.getItem(), mc.player.getYaw(), x, y, z);
            }

            mc.options.getBobView().setValue(prev_bob);
            RenderSystem.enableDepthTest();
         }
      }
   }

   private void calcTrajectory(Entity e, Color color) {
      double motionX = e.getVelocity().x;
      double motionY = e.getVelocity().y;
      double motionZ = e.getVelocity().z;
      if (motionX != 0.0 || motionY != 0.0 || motionZ != 0.0) {
         double x = e.getX();
         double y = e.getY();
         double z = e.getZ();

         for (int i = 0; i < 300; i++) {
            Vec3d lastPos = new Vec3d(x, y, z);
            if (mc.world.getBlockState(new BlockPos((int)(x += motionX), (int)(y += motionY), (int)(z += motionZ))).getBlock() == Blocks.WATER) {
               motionX *= 0.8;
               motionY *= 0.8;
               motionZ *= 0.8;
            } else {
               motionX *= 0.99;
               motionY *= 0.99;
               motionZ *= 0.99;
            }

            double var21;
            double var22;
            motionY = e instanceof ArrowEntity ? (var21 = motionY - 0.05F) : (var22 = motionY - 0.03F);
            Vec3d pos = new Vec3d(x, y, z);
            HitResult hitResult = mc.world.raycast(new RaycastContext(lastPos, pos, ShapeType.OUTLINE, FluidHandling.NONE, mc.player));
            if (hitResult != null && hitResult.getType() == Type.BLOCK || y <= -65.0) {
               if (e instanceof EnderPearlEntity && this.pearlLanding.getValue()) {
                  this.renderPearlLanding(hitResult != null && hitResult.getType() == Type.BLOCK ? hitResult.getPos() : pos, i + 1, color);
               }
               break;
            }

            int alpha = (int)MathUtil.clamp(255.0F * ((float)(i + 1) / 10.0F), 0.0F, 255.0F);
            Render3DUtil.drawLine(lastPos, pos, ColorUtil.injectAlpha(color, alpha));
         }
      }
   }

   private void calcTrajectory(Item item, float yaw, double x, double y, double z) {
      y = y + (double)mc.player.getEyeHeight(mc.player.getPose()) - 0.1000000014901161;
      if (item == mc.player.getMainHandStack().getItem()) {
         x -= (double)(MathHelper.cos(yaw / 180.0F * (float) Math.PI) * 0.16F);
         z -= (double)(MathHelper.sin(yaw / 180.0F * (float) Math.PI) * 0.16F);
      } else {
         x += (double)(MathHelper.cos(yaw / 180.0F * (float) Math.PI) * 0.16F);
         z += (double)(MathHelper.sin(yaw / 180.0F * (float) Math.PI) * 0.16F);
      }

      float maxDist = this.getDistance(item);
      double motionX = (double)(-MathHelper.sin(yaw / 180.0F * (float) Math.PI) * MathHelper.cos(mc.player.getPitch() / 180.0F * (float) Math.PI) * maxDist);
      double motionY = (double)(-MathHelper.sin((mc.player.getPitch() - (float)this.getThrowPitch(item)) / 180.0F * 3.141593F) * maxDist);
      double motionZ = (double)(MathHelper.cos(yaw / 180.0F * (float) Math.PI) * MathHelper.cos(mc.player.getPitch() / 180.0F * (float) Math.PI) * maxDist);
      float power = (float)mc.player.getItemUseTime() / 20.0F;
      power = (power * power + power * 2.0F) / 3.0F;
      if (power > 1.0F) {
         power = 1.0F;
      }

      float getDistance = MathHelper.sqrt((float)(motionX * motionX + motionY * motionY + motionZ * motionZ));
      motionX /= (double)getDistance;
      motionY /= (double)getDistance;
      motionZ /= (double)getDistance;
      float pow = (item instanceof BowItem ? power * 2.0F : (item instanceof CrossbowItem ? 2.2F : 1.0F)) * this.getThrowVelocity(item);
      motionX *= (double)pow;
      motionY *= (double)pow;
      motionZ *= (double)pow;
      motionX += mc.player.getVelocity().getX();
      motionY += mc.player.getVelocity().getY();
      motionZ += mc.player.getVelocity().getZ();

      for (int i = 0; i < 300; i++) {
         Vec3d lastPos = new Vec3d(x, y, z);
         if (mc.world.getBlockState(new BlockPos((int)(x += motionX), (int)(y += motionY), (int)(z += motionZ))).getBlock() == Blocks.WATER) {
            motionX *= 0.8;
            motionY *= 0.8;
            motionZ *= 0.8;
         } else {
            motionX *= 0.99;
            motionY *= 0.99;
            motionZ *= 0.99;
         }

         double var35;
         double var36;
         double var37;
         motionY = item instanceof BowItem
            ? (var35 = motionY - 0.05F)
            : (mc.player.getMainHandStack().getItem() instanceof CrossbowItem ? (var36 = motionY - 0.05F) : (var37 = motionY - 0.03F));
         Vec3d pos = new Vec3d(x, y, z);

         for (Entity ent : Alien.THREAD.getEntities()) {
            if (!(ent instanceof ArrowEntity)
               && !ent.equals(mc.player)
               && ent.getBoundingBox().intersects(new Box(x - 0.3, y - 0.3, z - 0.3, x + 0.3, y + 0.3, z + 0.3))) {
               Render3DUtil.drawBox(matrixStack, ent.getBoundingBox(), this.hand.getValue());
               break;
            }
         }

         BlockHitResult bhr;
         if ((bhr = mc.world.raycast(new RaycastContext(lastPos, pos, ShapeType.OUTLINE, FluidHandling.NONE, mc.player))) != null
            && bhr.getType() == Type.BLOCK) {
            Render3DUtil.drawBox(matrixStack, new Box(bhr.getBlockPos()), this.hand.getValue());
            if (item instanceof EnderPearlItem && this.pearlLanding.getValue()) {
               this.renderPearlLanding(bhr.getPos(), i + 1, this.pearl.getValue());
            }
            break;
         }

         if (y <= -65.0) {
            break;
         }

         if (motionX != 0.0 || motionY != 0.0 || motionZ != 0.0) {
            Render3DUtil.drawLine(lastPos, pos, this.hand.getValue());
         }
      }
   }

   private boolean isThrowable(Item item) {
      return item instanceof EnderPearlItem
         || item instanceof TridentItem
         || item instanceof ExperienceBottleItem
         || item instanceof SnowballItem
         || item instanceof EggItem
         || item instanceof SplashPotionItem
         || item instanceof LingeringPotionItem;
   }

   private void renderPearlLanding(Vec3d pos, int ticks, Color color) {
      this.drawPearlIcon(pos);
      String time = this.timeFormat.format((double)ticks / 20.0) + "s";
      Render3DUtil.drawText3D(Text.of(time), this.getPearlTextPos(pos), 0.0, 0.0, this.getPearlTextScale(pos), color);
   }

   private Vec3d getPearlTextPos(Vec3d pos) {
      Camera camera = mc.gameRenderer.getCamera();
      Vector3f offset = new Vector3f(0.0F, 14.0F * this.getPearlLandingScale(pos), 0.0F);
      offset.rotate(camera.getRotation());
      return pos.add((double)offset.x, (double)offset.y, (double)offset.z);
   }

   private float getPearlLandingScale(Vec3d pos) {
      Vec3d cameraPos = mc.gameRenderer.getCamera().getPos();
      double distanceSq = cameraPos.squaredDistanceTo(pos);
      float scale = -0.025F * this.pearlLandingScale.getValueFloat();
      if (this.pearlLandingFactor.getValue() && distanceSq > (double)(this.pearlLandingDistance.getValueFloat() * this.pearlLandingDistance.getValueFloat())) {
         scale += (float)(
            (Math.sqrt(distanceSq) - (double)this.pearlLandingDistance.getValueFloat()) * -0.0025F * (double)this.pearlLandingScaling.getValueFloat()
         );
      }

      return scale;
   }

   private double getPearlTextScale(Vec3d pos) {
      return Math.max(0.1, (double)(Math.abs(this.getPearlLandingScale(pos)) / 0.025F));
   }

   private void drawPearlIcon(Vec3d pos) {
      Camera camera = mc.gameRenderer.getCamera();
      Vec3d cameraPos = camera.getPos();
      matrixStack.push();
      matrixStack.translate(pos.x - cameraPos.x, pos.y - cameraPos.y, pos.z - cameraPos.z);
      matrixStack.multiply(camera.getRotation());
      float scale = this.getPearlLandingScale(pos);
      matrixStack.scale(scale, scale, -scale);
      RenderSystem.setShaderTexture(0, PEARL_ICON_TEXTURE);
      RenderSystem.enableBlend();
      RenderSystem.defaultBlendFunc();
      RenderSystem.disableCull();
      RenderSystem.disableDepthTest();
      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
      this.drawTexture(matrixStack, -8.0F, -8.0F, 16.0F, 16.0F);
      RenderSystem.enableCull();
      RenderSystem.disableBlend();
      RenderSystem.enableDepthTest();
      matrixStack.pop();
   }

   private void drawTexture(MatrixStack matrices, float x, float y, float width, float height) {
      RenderSystem.setShader(GameRenderer::getPositionTexProgram);
      BufferBuilder bufferBuilder = Tessellator.getInstance().begin(DrawMode.QUADS, VertexFormats.POSITION_TEXTURE);
      Matrix4f matrix = matrices.peek().getPositionMatrix();
      bufferBuilder.vertex(matrix, x, y + height, 0.0F).texture(0.0F, 1.0F);
      bufferBuilder.vertex(matrix, x + width, y + height, 0.0F).texture(1.0F, 1.0F);
      bufferBuilder.vertex(matrix, x + width, y, 0.0F).texture(1.0F, 0.0F);
      bufferBuilder.vertex(matrix, x, y, 0.0F).texture(0.0F, 0.0F);
      BufferRenderer.drawWithGlobalProgram(bufferBuilder.end());
   }

   private float getDistance(Item item) {
      return item instanceof BowItem ? 1.0F : 0.4F;
   }

   private float getThrowVelocity(Item item) {
      if (item instanceof SplashPotionItem || item instanceof LingeringPotionItem) {
         return 0.5F;
      } else if (item instanceof ExperienceBottleItem) {
         return 0.59F;
      } else {
         return item instanceof TridentItem ? 2.0F : 1.5F;
      }
   }

   private int getThrowPitch(Item item) {
      return !(item instanceof SplashPotionItem) && !(item instanceof LingeringPotionItem) && !(item instanceof ExperienceBottleItem) ? 0 : 20;
   }
}
