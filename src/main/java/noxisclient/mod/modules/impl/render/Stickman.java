package noxisclient.mod.modules.impl.render;

import com.mojang.blaze3d.systems.RenderSystem;
import java.awt.Color;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.BuiltBuffer;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.render.VertexFormat.DrawMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;
import noxisclient.mod.modules.Module;
import noxisclient.mod.modules.settings.impl.BooleanSetting;
import noxisclient.mod.modules.settings.impl.ColorSetting;
import noxisclient.mod.modules.settings.impl.SliderSetting;
import org.joml.Matrix4f;

public class Stickman extends Module {
   public static Stickman INSTANCE;
   private final BooleanSetting self = this.add(new BooleanSetting("Self", false));
   private final BooleanSetting invisible = this.add(new BooleanSetting("Invisible", false));
   private final BooleanSetting throughWalls = this.add(new BooleanSetting("ThroughWalls", false));
   private final SliderSetting range = this.add(new SliderSetting("Range", 64.0, 8.0, 256.0, 1.0));
   private final SliderSetting scale = this.add(new SliderSetting("Scale", 1.0, 0.5, 1.5, 0.05));
   private final SliderSetting lineWidth = this.add(new SliderSetting("LineWidth", 2.0, 1.0, 6.0, 0.1));
   private final ColorSetting color = this.add(new ColorSetting("Color", new Color(255, 255, 255, 255)));

   public Stickman() {
      super("Stickman", "Renders players as stick figures.", Module.Category.Render);
      this.setChinese("火柴");
      INSTANCE = this;
   }

   @Override
   public void onRender3D(MatrixStack matrixStack) {
   }

   public boolean shouldReplace(PlayerEntity player) {
      if (!this.isOn() || mc.player == null || player == null || player.isSpectator()) {
         return false;
      } else {
         return !this.invisible.getValue() && player.isInvisible()
            ? false
            : (player == mc.player || player.squaredDistanceTo(mc.player) <= this.range.getValue() * this.range.getValue())
               && (player != mc.player || this.self.getValue() && !mc.options.getPerspective().isFirstPerson());
      }
   }

   public void renderPlayer(MatrixStack matrices, PlayerEntity player, double x, double y, double z, float tickDelta) {
      double s = this.scale.getValue() * Math.max(0.55, (double)player.getHeight() / 1.8);
      float bodyYaw = lerpAngle(player.prevBodyYaw, player.bodyYaw, tickDelta);
      double yaw = Math.toRadians((double)bodyYaw);
      Vec3d right = new Vec3d(Math.cos(yaw), 0.0, Math.sin(yaw));
      Vec3d forward = new Vec3d(-Math.sin(yaw), 0.0, Math.cos(yaw));
      double crouch = player.isInSneakingPose() ? 0.16 * s : 0.0;
      Vec3d base = new Vec3d(x, y, z);
      Vec3d hip = base.add(0.0, 0.68 * s - crouch * 0.5, 0.0);
      Vec3d neck = base.add(0.0, 1.3 * s - crouch, 0.0);
      Vec3d head = base.add(0.0, 1.55 * s - crouch, 0.0);
      float limbSwing = player.limbAnimator.getPos();
      float limbSpeed = Math.min(player.limbAnimator.getSpeed(), 1.0F);
      double swing = Math.sin((double)(limbSwing * 0.6662F)) * (double)limbSpeed * s;
      Vec3d leftHand = neck.add(right.multiply(0.46 * s)).add(forward.multiply(-swing * 0.2)).add(0.0, -0.7 * s, 0.0);
      Vec3d rightHand = neck.add(right.multiply(-0.46 * s)).add(forward.multiply(swing * 0.2)).add(0.0, -0.7 * s, 0.0);
      Vec3d leftArmBend = neck.add(right.multiply(0.3 * s)).add(forward.multiply(-swing * 0.1)).add(0.0, -0.3 * s, 0.0);
      Vec3d rightArmBend = neck.add(right.multiply(-0.3 * s)).add(forward.multiply(swing * 0.1)).add(0.0, -0.3 * s, 0.0);
      Vec3d leftFoot = hip.add(right.multiply(0.24 * s)).add(forward.multiply(swing * 0.3)).add(0.0, -0.82 * s, 0.0);
      Vec3d rightFoot = hip.add(right.multiply(-0.24 * s)).add(forward.multiply(-swing * 0.3)).add(0.0, -0.82 * s, 0.0);
      Color lineColor = this.color.getValue();
      Stickman.LineBatch batch = new Stickman.LineBatch(matrices, lineColor, this.lineWidth.getValueFloat(), this.throughWalls.getValue());
      batch.begin();
      this.oval(batch, head, 0.17 * s, 0.28 * s);
      batch.line(neck, hip);
      batch.curve(neck, leftArmBend, leftHand, 12);
      batch.curve(neck, rightArmBend, rightHand, 12);
      batch.line(hip, leftFoot);
      batch.line(hip, rightFoot);
      batch.end();
   }

   private void oval(Stickman.LineBatch batch, Vec3d center, double radiusX, double radiusY) {
      double yaw = Math.toRadians((double)mc.gameRenderer.getCamera().getYaw());
      Vec3d right = new Vec3d(Math.cos(yaw), 0.0, Math.sin(yaw));
      Vec3d up = new Vec3d(0.0, 1.0, 0.0);
      batch.ring(center, right, up, radiusX, radiusY);
   }

   private static float lerpAngle(float from, float to, float delta) {
      float diff = (to - from) % 360.0F;
      if (diff >= 180.0F) {
         diff -= 360.0F;
      }

      if (diff < -180.0F) {
         diff += 360.0F;
      }

      return from + diff * delta;
   }

   private static class LineBatch {
      private final MatrixStack matrices;
      private final Color color;
      private final float width;
      private final boolean throughWalls;
      private BufferBuilder buffer;
      private Matrix4f matrix;

      private LineBatch(MatrixStack matrices, Color color, float width, boolean throughWalls) {
         this.matrices = matrices;
         this.color = color;
         this.width = width;
         this.throughWalls = throughWalls;
      }

      private void begin() {
         RenderSystem.enableBlend();
         RenderSystem.disableCull();
         if (this.throughWalls) {
            RenderSystem.disableDepthTest();
         }

         RenderSystem.setShader(GameRenderer::getPositionColorProgram);
         this.matrix = this.matrices.peek().getPositionMatrix();
         this.buffer = Tessellator.getInstance().begin(DrawMode.QUADS, VertexFormats.POSITION_COLOR);
      }

      private void line(Vec3d start, Vec3d end) {
         Vec3d direction = end.subtract(start);
         if (!(direction.lengthSquared() < 1.0E-6)) {
            Vec3d cameraOffset = start.add(end).multiply(0.5);
            Vec3d side = direction.normalize().crossProduct(cameraOffset.normalize());
            if (side.lengthSquared() < 1.0E-6) {
               side = direction.normalize().crossProduct(new Vec3d(0.0, 1.0, 0.0));
            }

            if (side.lengthSquared() < 1.0E-6) {
               side = new Vec3d(1.0, 0.0, 0.0);
            }

            double halfWidth = (double)this.width * 0.006;
            Vec3d offset = side.normalize().multiply(halfWidth);
            this.vertex(start.add(offset));
            this.vertex(end.add(offset));
            this.vertex(end.subtract(offset));
            this.vertex(start.subtract(offset));
         }
      }

      private void curve(Vec3d start, Vec3d control, Vec3d end, int segments) {
         Vec3d prev = start;

         for (int i = 1; i <= segments; i++) {
            double t = (double)i / (double)segments;
            double inv = 1.0 - t;
            Vec3d point = start.multiply(inv * inv).add(control.multiply(2.0 * inv * t)).add(end.multiply(t * t));
            this.line(prev, point);
            prev = point;
         }
      }

      private void ring(Vec3d center, Vec3d right, Vec3d up, double radiusX, double radiusY) {
         int segments = 96;
         double halfWidth = (double)this.width * 0.006;
         double innerRadiusX = Math.max(radiusX - halfWidth, radiusX * 0.65);
         double outerRadiusX = radiusX + halfWidth;
         double innerRadiusY = Math.max(radiusY - halfWidth, radiusY * 0.65);
         double outerRadiusY = radiusY + halfWidth;

         for (int i = 0; i < segments; i++) {
            double angle = (Math.PI * 2) * (double)i / (double)segments;
            double nextAngle = (Math.PI * 2) * (double)(i + 1) / (double)segments;
            Vec3d outer1 = this.ellipsePoint(center, right, up, outerRadiusX, outerRadiusY, angle);
            Vec3d outer2 = this.ellipsePoint(center, right, up, outerRadiusX, outerRadiusY, nextAngle);
            Vec3d inner2 = this.ellipsePoint(center, right, up, innerRadiusX, innerRadiusY, nextAngle);
            Vec3d inner1 = this.ellipsePoint(center, right, up, innerRadiusX, innerRadiusY, angle);
            this.vertex(outer1);
            this.vertex(outer2);
            this.vertex(inner2);
            this.vertex(inner1);
         }
      }

      private Vec3d ellipsePoint(Vec3d center, Vec3d right, Vec3d up, double radiusX, double radiusY, double angle) {
         return center.add(right.multiply(Math.cos(angle) * radiusX)).add(up.multiply(Math.sin(angle) * radiusY));
      }

      private void vertex(Vec3d vec) {
         this.buffer
            .vertex(this.matrix, (float)vec.x, (float)vec.y, (float)vec.z)
            .color(this.color.getRed(), this.color.getGreen(), this.color.getBlue(), this.color.getAlpha());
      }

      private void end() {
         BuiltBuffer builtBuffer = this.buffer.end();
         if (builtBuffer != null) {
            BufferRenderer.drawWithGlobalProgram(builtBuffer);
         }

         if (this.throughWalls) {
            RenderSystem.enableDepthTest();
         }

         RenderSystem.enableCull();
         RenderSystem.disableBlend();
      }
   }
}
