package noxisclient.mod.modules.impl.render;

import com.mojang.blaze3d.systems.RenderSystem;
import java.awt.Color;
import java.text.DecimalFormat;
import java.util.Collections;
import java.util.Map.Entry;
import java.util.concurrent.CopyOnWriteArrayList;
import net.minecraft.client.font.TextRenderer.TextLayerType;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.DiffuseLighting;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.render.VertexConsumerProvider.Immediate;
import net.minecraft.client.render.VertexFormat.DrawMode;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import noxisclient.Alien;
import noxisclient.api.events.impl.Render3DEvent;
import noxisclient.api.utils.math.MathUtil;
import noxisclient.api.utils.render.ColorUtil;
import noxisclient.api.utils.render.Render2DUtil;
import noxisclient.api.utils.render.TextUtil;
import noxisclient.core.impl.FontManager;
import noxisclient.mod.modules.Module;
import noxisclient.mod.modules.impl.client.Fonts;
import noxisclient.mod.modules.impl.client.Socials;
import noxisclient.mod.modules.impl.client.TextRadar;
import noxisclient.mod.modules.impl.misc.PotionTracker;
import noxisclient.mod.modules.settings.impl.BooleanSetting;
import noxisclient.mod.modules.settings.impl.ColorSetting;
import noxisclient.mod.modules.settings.impl.EnumSetting;
import noxisclient.mod.modules.settings.impl.SliderSetting;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.opengl.GL11;

public class NameTags extends Module {
   public static NameTags INSTANCE;
   final ColorSetting colorConfig = this.add(new ColorSetting("Color", new Color(255, 255, 255)));
   final ColorSetting friendConfig = this.add(new ColorSetting("Friend", new Color(155, 155, 255)).injectBoolean(true));
   final ColorSetting invisibleConfig = this.add(new ColorSetting("Invisible", new Color(200, 200, 200)).injectBoolean(true));
   final ColorSetting died = this.add(new ColorSetting("Died", new Color(180, 0, 0)).injectBoolean(true));
   final ColorSetting sneakingConfig = this.add(new ColorSetting("Sneaking", new Color(200, 200, 0)).injectBoolean(true));
   final ColorSetting rectConfig = this.add(new ColorSetting("Rectangle", new Color(0, 0, 0, 100)).injectBoolean(true));
   final BooleanSetting rectBlurConfig = this.add(new BooleanSetting("RectBlur", false, () -> this.rectConfig.booleanValue).setParent());
   final SliderSetting rectBlurRadiusConfig = this.add(
      new SliderSetting("BlurRadius", 10.0, 0.0, 100.0, () -> this.rectConfig.booleanValue && this.rectBlurConfig.isOpen())
   );
   final BooleanSetting armorConfig = this.add(new BooleanSetting("Armor", true).setParent());
   final BooleanSetting drawItemConfig = this.add(new BooleanSetting("DrawItem", true, this.armorConfig::isOpen));
   final SliderSetting offsetConfig = this.add(new SliderSetting("Offset", -20.0, -30.0, 10.0, 0.01, this.armorConfig::isOpen));
   final BooleanSetting enchantmentsConfig = this.add(new BooleanSetting("Enchantments", true).setParent());
   final BooleanSetting protectionOnlyConfig = this.add(new BooleanSetting("Protection", false, this.enchantmentsConfig::isOpen));
   final BooleanSetting durabilityConfig = this.add(new BooleanSetting("Durability", true).setParent());
   final BooleanSetting forceBarConfig = this.add(new BooleanSetting("ForceBar", true, this.durabilityConfig::isOpen));
   final BooleanSetting itemNameConfig = this.add(new BooleanSetting("ItemName", false));
   final BooleanSetting entityIdConfig = this.add(new BooleanSetting("EntityId", false));
   final BooleanSetting gamemodeConfig = this.add(new BooleanSetting("Gamemode", false));
   final BooleanSetting pingConfig = this.add(new BooleanSetting("Ping", true));
   final BooleanSetting healthConfig = this.add(new BooleanSetting("Health", true));
   final BooleanSetting totemsConfig = this.add(new BooleanSetting("Totems", false));
   final BooleanSetting turtleConfig = this.add(new BooleanSetting("TurtleConfig", false).setParent());
   final BooleanSetting showTurtleIcon = this.add(new BooleanSetting("ShowIcon", true, this.turtleConfig::isOpen));
   final BooleanSetting showTurtleTime = this.add(new BooleanSetting("ShowTime", true, this.turtleConfig::isOpen));
   final BooleanSetting turtleColorName = this.add(new BooleanSetting("ColorName", false, this.turtleConfig::isOpen).setParent());
   final BooleanSetting ignoreFriend = this.add(new BooleanSetting("IgnoreFriend", false, this.turtleColorName::isOpen));
   final EnumSetting<NameTags.ScaleMode> scaleModeConfig = this.add(new EnumSetting<>("ScaleMode", NameTags.ScaleMode.Original));
   final SliderSetting scaleConfig = this.add(new SliderSetting("Scale", 1.0, 0.0, 3.0, 0.1));
   final BooleanSetting factorConfig = this.add(new BooleanSetting("Factor", true).setParent());
   final SliderSetting scalingConfig = this.add(new SliderSetting("Scaling", 1.0, 0.0, 3.0, 0.1, this.factorConfig::isOpen));
   final SliderSetting distanceConfig = this.add(new SliderSetting("Distance", 6.0, 0.0, 20.0, 0.1, this.factorConfig::isOpen));
   final SliderSetting heightConfig = this.add(new SliderSetting("Height", 0.0, -3.0, 3.0, 0.01));
   final DecimalFormat df = new DecimalFormat("0.0");

   public NameTags() {
      super("NameTags", "Renders info on player NameTags", Module.Category.Render);
      this.setChinese("名字标签");
      INSTANCE = this;
   }

   public void onRender3D(Render3DEvent event) {
      if (mc.gameRenderer != null && mc.getCameraEntity() != null) {
         Camera camera = mc.gameRenderer.getCamera();
         RenderSystem.enableBlend();
         GL11.glDepthFunc(519);
         MatrixStack matrixStack = new MatrixStack();

         for (PlayerEntity playerEntity : Alien.THREAD.getPlayers()) {
            if ((this.died.booleanValue || playerEntity.isAlive())
               && (playerEntity != mc.player || !mc.options.getPerspective().isFirstPerson())
               && (this.invisibleConfig.booleanValue || !playerEntity.isInvisible())) {
               String info = this.getNametagInfo(playerEntity);
               boolean hasTurtle = false;
               Sprite turtleSprite = null;
               if (this.turtleConfig.getValue() && PotionTracker.INSTANCE != null && PotionTracker.INSTANCE.hasTurtle(playerEntity)) {
                  hasTurtle = true;
                  if (this.showTurtleIcon.getValue()) {
                     turtleSprite = mc.getStatusEffectSpriteManager().getSprite(StatusEffects.RESISTANCE);
                  }
               }

               Vec3d renderPosition = MathUtil.getRenderPosition(playerEntity, event.tickDelta);
               double x = renderPosition.getX();
               double y = renderPosition.getY();
               double z = renderPosition.getZ();
               float hwidth = this.getTextWidth(info) / 2.0F;
               this.renderInfo(info, hwidth, playerEntity, x, y, z, camera, matrixStack, hasTurtle, turtleSprite);
            }
         }

         GL11.glDepthFunc(515);
         RenderSystem.disableBlend();
      }
   }

   private void renderInfo(
      String info, float width, PlayerEntity entity, double x, double y, double z, Camera camera, MatrixStack matrices, boolean hasTurtle, Sprite turtleSprite
   ) {
      Vec3d pos = camera.getPos();
      double eyeY = y + (double)entity.getHeight() + (double)(entity.isSneaking() ? 0.4F : 0.43F) + (double)this.heightConfig.getValueFloat();
      float scale = this.getNameTagScale(pos, x, eyeY, z);
      matrices.push();
      matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(camera.getPitch()));
      matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(camera.getYaw() + 180.0F));
      matrices.translate(x - pos.getX(), eyeY - pos.getY() + (double)((scale / -0.025F - 1.0F) / 4.0F), z - pos.getZ());
      matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-camera.getYaw()));
      matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(camera.getPitch()));
      matrices.scale(scale, scale, -1.0F);
      float spriteOffset = 4.0F;
      if (hasTurtle && turtleSprite != null) {
         spriteOffset = 10.0F;
      }

      if (this.rectConfig.booleanValue) {
         float f = -width - 2.0F - spriteOffset;
         float rectWidth = width * 2.0F + 3.0F + spriteOffset;
         float rectHeight = this.getTextHeight() + 1.0F;
         if (this.rectBlurConfig.getValue()) {
            this.applyRectBlur(matrices, f, -1.0F, rectWidth, rectHeight);
         }

         Render2DUtil.drawRect(matrices, f, -1.0F, rectWidth, rectHeight, this.rectConfig.getValue());
      }

      int nameColor;
      if (!hasTurtle || !this.turtleColorName.getValue()) {
         nameColor = this.getNametagColor(entity);
      } else if (this.ignoreFriend.getValue() && Alien.FRIEND.isFriend(entity)) {
         nameColor = this.getNametagColor(entity);
      } else {
         Integer blueColor = Formatting.BLUE.getColorValue();
         nameColor = blueColor != null ? 0xFF000000 | blueColor : -11184641;
      }

      this.drawWithShadow(matrices, info, -width, 0.0F, nameColor);
      if (turtleSprite != null) {
         this.renderIconSprite(matrices, turtleSprite, -width - spriteOffset, -1.0F, 10.0F);
      }

      if (this.armorConfig.getValue()) {
         this.renderItems(matrices, entity);
      }

      matrices.pop();
   }

   private float getNameTagScale(Vec3d cameraPos, double x, double y, double z) {
      if (this.scaleModeConfig.getValue() == NameTags.ScaleMode.Current) {
         double setting = Math.max(0.001, this.scaleConfig.getValue());
         double distance = cameraPos.distanceTo(new Vec3d(x, y, z));
         double fov = (double)mc.options.getFov().getValue().intValue() / 110.0;
         if (fov < 1.0) {
            fov = 1.0;
         }

         double currentScale = distance <= 10.0 / setting ? Math.min(1.0, setting) * fov : distance * setting * 0.1 * fov;
         return (float)(-0.025 * currentScale);
      } else {
         return (float)(
            (double)(-0.025F * this.scaleConfig.getValueFloat())
               + (
                  this.factorConfig.getValue()
                        && cameraPos.squaredDistanceTo(x, y, z) > (double)(this.distanceConfig.getValueFloat() * this.distanceConfig.getValueFloat())
                     ? (Math.sqrt(cameraPos.squaredDistanceTo(x, y, z)) - (double)this.distanceConfig.getValueFloat())
                        * -0.0025F
                        * (double)this.scalingConfig.getValueFloat()
                     : 0.0
               )
         );
      }
   }

   private void applyRectBlur(MatrixStack matrices, float x, float y, float width, float height) {
      double[] box = this.getScreenRect(matrices, x, y, width, height);
      if (box != null) {
         float blurX = (float)box[0];
         float blurY = (float)box[1];
         float blurWidth = (float)(box[2] - box[0]);
         float blurHeight = (float)(box[3] - box[1]);
         if (!(blurWidth <= 0.5F) && !(blurHeight <= 0.5F)) {
            Alien.BLUR.applyBlur(this.rectBlurRadiusConfig.getValueFloat(), blurX, blurY, blurWidth, blurHeight);
         }
      }
   }

   private double[] getScreenRect(MatrixStack matrices, float x, float y, float width, float height) {
      Vec3d[] corners = new Vec3d[]{
         this.localToScreen(matrices, x, y),
         this.localToScreen(matrices, x + width, y),
         this.localToScreen(matrices, x, y + height),
         this.localToScreen(matrices, x + width, y + height)
      };
      double minX = Double.POSITIVE_INFINITY;
      double minY = Double.POSITIVE_INFINITY;
      double maxX = Double.NEGATIVE_INFINITY;
      double maxY = Double.NEGATIVE_INFINITY;
      boolean visible = false;

      for (Vec3d corner : corners) {
         if (corner != null && !(corner.z <= 0.0) && !(corner.z >= 1.0)) {
            visible = true;
            minX = Math.min(minX, corner.x);
            minY = Math.min(minY, corner.y);
            maxX = Math.max(maxX, corner.x);
            maxY = Math.max(maxY, corner.y);
         }
      }

      if (!visible) {
         return null;
      } else {
         double screenWidth = (double)mc.getWindow().getScaledWidth();
         double screenHeight = (double)mc.getWindow().getScaledHeight();
         minX = MathUtil.clamp(minX, 0.0, screenWidth);
         minY = MathUtil.clamp(minY, 0.0, screenHeight);
         maxX = MathUtil.clamp(maxX, 0.0, screenWidth);
         maxY = MathUtil.clamp(maxY, 0.0, screenHeight);
         return new double[]{minX, minY, maxX, maxY};
      }
   }

   private Vec3d localToScreen(MatrixStack matrices, float x, float y) {
      int displayHeight = mc.getWindow().getHeight();
      int[] viewport = new int[4];
      GL11.glGetIntegerv(2978, viewport);
      Vector3f target = new Vector3f();
      Vector4f transformedCoordinates = new Vector4f(x, y, 0.0F, 1.0F).mul(matrices.peek().getPositionMatrix());
      Matrix4f matrixProj = new Matrix4f(TextUtil.lastProjMat);
      Matrix4f matrixModel = new Matrix4f(TextUtil.lastModMat);
      matrixProj.mul(matrixModel).project(transformedCoordinates.x(), transformedCoordinates.y(), transformedCoordinates.z(), viewport, target);
      return new Vec3d(
         (double)target.x / mc.getWindow().getScaleFactor(), (double)((float)displayHeight - target.y) / mc.getWindow().getScaleFactor(), (double)target.z
      );
   }

   private void drawWithShadow(MatrixStack matrices, String info, float x, float y, int color) {
      if (this.useFont()) {
         FontManager.drawNameTagsString(matrices, info, x, y, color, true);
      } else {
         Immediate i = mc.getBufferBuilders().getEntityVertexConsumers();
         Matrix4f matrix = matrices.peek().getPositionMatrix();
         float shadowX = x + 0.5F;
         float shadowY = y + 0.5F;
         int currentColor = color;
         StringBuilder segment = new StringBuilder();

         for (int j = 0; j < info.length(); j++) {
            char c = info.charAt(j);
            if (c == 167) {
               c = 25602;
            }

            if (c == 25602) {
               c = 167;
            }

            if (c == 167 && j + 1 < info.length()) {
               char code = info.charAt(j + 1);
               Formatting fmt = Formatting.byCode(code);
               if (fmt != null && fmt.isColor()) {
                  if (segment.length() > 0) {
                     int shadowColor = (currentColor & 16579836) >> 2 | currentColor & 0xFF000000;
                     mc.textRenderer.draw(segment.toString(), shadowX, shadowY, shadowColor, false, matrix, i, TextLayerType.SEE_THROUGH, 0, 15728880);
                     shadowX += this.getTextWidth(segment.toString());
                     segment = new StringBuilder();
                  }

                  Integer rgb = fmt.getColorValue();
                  if (rgb != null) {
                     currentColor = 0xFF000000 | rgb;
                  }
               } else if (fmt == Formatting.RESET) {
                  if (segment.length() > 0) {
                     int shadowColor = (currentColor & 16579836) >> 2 | currentColor & 0xFF000000;
                     mc.textRenderer.draw(segment.toString(), shadowX, shadowY, shadowColor, false, matrix, i, TextLayerType.SEE_THROUGH, 0, 15728880);
                     shadowX += this.getTextWidth(segment.toString());
                     segment = new StringBuilder();
                  }

                  currentColor = color;
               } else {
                  segment.append(c);
                  segment.append(code);
               }

               j++;
            } else {
               segment.append(c);
            }
         }

         if (segment.length() > 0) {
            int shadowColor = (currentColor & 16579836) >> 2 | currentColor & 0xFF000000;
            mc.textRenderer.draw(segment.toString(), shadowX, shadowY, shadowColor, false, matrix, i, TextLayerType.SEE_THROUGH, 0, 15728880);
         }

         i.draw();
         mc.textRenderer.draw(this.normalizeFormattingPrefixes(info), x, y, color, false, matrix, i, TextLayerType.SEE_THROUGH, 0, 15728880);
         i.draw();
      }
   }

   private String normalizeFormattingPrefixes(String text) {
      return text == null ? "" : text.replace('搂', '§');
   }

   private void renderItems(MatrixStack matrixStack, PlayerEntity player) {
      CopyOnWriteArrayList<ItemStack> displayItems = new CopyOnWriteArrayList<>();
      if (!player.getOffHandStack().isEmpty()) {
         displayItems.add(player.getOffHandStack());
      }

      player.getInventory().armor.forEach(armorStack -> {
         if (!armorStack.isEmpty()) {
            displayItems.add(armorStack);
         }
      });
      if (!player.getMainHandStack().isEmpty()) {
         displayItems.add(player.getMainHandStack());
      }

      Collections.reverse(displayItems);
      float x = 0.0F;
      int n11 = 0;

      for (ItemStack itemStack : displayItems) {
         x -= 8.0F;
         if (itemStack.getEnchantments().getSize() > n11) {
            n11 = itemStack.getEnchantments().getSize();
         }
      }

      int enchantDisplayCount = n11;
      if (this.enchantmentsConfig.getValue() && this.protectionOnlyConfig.getValue()) {
         enchantDisplayCount = 0;

         for (ItemStack stack : displayItems) {
            int count = 0;

            for (RegistryEntry<Enchantment> ench : EnchantmentHelper.getEnchantments(stack).getEnchantments()) {
               String path = ench.getKey().map(k -> k.getValue().getPath()).orElse("");
               if (path.contains("protection")) {
                  count++;
               }
            }

            if (count > enchantDisplayCount) {
               enchantDisplayCount = count;
            }
         }
      }

      float y = this.offsetConfig.getValueFloat();

      for (ItemStack stack : displayItems) {
         GL11.glDepthFunc(519);
         if (this.drawItemConfig.getValue()) {
            this.renderItemStack(matrixStack, stack, x, y + 1.0F);
         }

         this.renderItemOverlay(matrixStack, stack, x, y + 2.5F);
         matrixStack.scale(0.5F, 0.5F, 0.5F);
         float aboveY = y - 3.0F;
         if (this.enchantmentsConfig.getValue() && this.durabilityConfig.getValue()) {
            this.renderEnchants(matrixStack, stack, x + 2.0F, aboveY - 5.0F);
            this.renderDurability(matrixStack, stack, x + 2.0F, aboveY);
         } else if (this.enchantmentsConfig.getValue()) {
            this.renderEnchants(matrixStack, stack, x + 2.0F, aboveY);
         } else if (this.durabilityConfig.getValue()) {
            this.renderDurability(matrixStack, stack, x + 2.0F, aboveY);
         }

         matrixStack.scale(2.0F, 2.0F, 2.0F);
         x += 16.0F;
         GL11.glDepthFunc(515);
      }

      ItemStack itemStackx = player.getMainHandStack();
      if (!itemStackx.isEmpty()) {
         matrixStack.scale(0.5F, 0.5F, 0.5F);
         if (this.itemNameConfig.getValue()) {
            float itemNameY = y - 4.5F + this.enchantOffset(n11);
            if (this.durabilityConfig.getValue() || this.enchantmentsConfig.getValue()) {
               float topY = y - 3.0F;
               if (this.enchantmentsConfig.getValue() && enchantDisplayCount > 0) {
                  if (this.durabilityConfig.getValue()) {
                     topY -= 5.0F;
                  }

                  topY -= 4.5F * (float)(enchantDisplayCount - 1);
               }

               itemNameY = topY - 5.0F;
            }

            this.renderItemName(matrixStack, itemStackx, itemNameY);
         }

         matrixStack.scale(2.0F, 2.0F, 2.0F);
      }
   }

   private void renderItemStack(MatrixStack matrixStack, ItemStack stack, float x, float y) {
      matrixStack.push();
      matrixStack.translate(x, y, 0.0F);
      matrixStack.translate(8.0F, 8.0F, 0.0F);
      matrixStack.scale(16.0F, 16.0F, 1.0E-8F);
      matrixStack.multiplyPositionMatrix(new Matrix4f().scaling(1.0F, -1.0F, 1.0F));
      DiffuseLighting.disableGuiDepthLighting();
      BakedModel model = mc.getItemRenderer().getModel(stack, mc.world, null, 0);
      Immediate i = mc.getBufferBuilders().getEntityVertexConsumers();
      mc.getItemRenderer().renderItem(stack, ModelTransformationMode.GUI, false, matrixStack, i, 16711680, OverlayTexture.DEFAULT_UV, model);
      i.draw();
      DiffuseLighting.enableGuiDepthLighting();
      matrixStack.pop();
   }

   private void renderItemOverlay(MatrixStack matrixStack, ItemStack stack, float x, float y) {
      matrixStack.push();
      if (stack.getCount() != 1) {
         String string = String.valueOf(stack.getCount());
         this.drawWithShadow(matrixStack, string, x + 17.0F - this.getTextWidth(string), y + 9.0F, -1);
      }

      if (stack.isItemBarVisible() || stack.isDamageable() && this.forceBarConfig.getValue()) {
         int i = stack.getItemBarStep();
         int j = stack.getItemBarColor();
         float k = x + 2.0F;
         float l = y + 13.0F;
         Render2DUtil.drawRect(matrixStack, k, l, 13.0F, 2.0F, -16777216);
         Render2DUtil.drawRect(matrixStack, k, l, (float)i, 1.0F, j | 0xFF000000);
      }

      matrixStack.pop();
   }

   private void renderDurability(MatrixStack matrixStack, ItemStack itemStack, float x, float y) {
      if (itemStack.isDamageable()) {
         int n = itemStack.getMaxDamage();
         int n2 = itemStack.getDamage();
         int durability = (int)((float)(n - n2) / (float)n * 100.0F);
         this.drawWithShadow(
            matrixStack, durability + "%", x * 2.0F, y * 2.0F, ColorUtil.hslToColor((float)(n - n2) / (float)n * 120.0F, 100.0F, 50.0F, 1.0F).getRGB()
         );
      }
   }

   private void renderEnchants(MatrixStack matrixStack, ItemStack itemStack, float x, float y) {
      if (itemStack.getItem() == Items.ENCHANTED_GOLDEN_APPLE) {
         this.drawWithShadow(matrixStack, "God", x * 2.0F, y * 2.0F, -3977663);
      } else if (itemStack.hasEnchantments()) {
         ItemEnchantmentsComponent enchants = EnchantmentHelper.getEnchantments(itemStack);
         float n2 = 0.0F;

         for (RegistryEntry<Enchantment> enchantment : enchants.getEnchantments()) {
            if (this.protectionOnlyConfig.getValue()) {
               String path = enchantment.getKey().map(key -> key.getValue().getPath()).orElse("");
               if (!path.contains("protection")) {
                  continue;
               }
            }

            int lvl = enchants.getLevel(enchantment);
            StringBuilder enchantString = new StringBuilder();
            String translatedName = enchantment.value().toString().replace("Enchantment ", "");
            if (translatedName.contains("Vanish")) {
               enchantString.append("§cVan");
            } else if (translatedName.contains("Bind")) {
               enchantString.append("§cBind");
            } else {
               int maxLen = lvl > 1 ? 2 : 3;
               if (translatedName.length() > maxLen) {
                  translatedName = translatedName.substring(0, maxLen);
               }

               enchantString.append(translatedName);
               enchantString.append(lvl);
            }

            this.drawWithShadow(matrixStack, enchantString.toString(), x * 2.0F, (y + n2) * 2.0F, -1);
            n2 -= 4.5F;
         }
      }
   }

   private float enchantOffset(int n) {
      if (this.enchantmentsConfig.getValue() && n > 2) {
         float value = -2.0F;
         float var3;
         return var3 = value - (float)(n - 3) * 4.5F;
      } else {
         return 0.0F;
      }
   }

   private void renderItemName(MatrixStack matrixStack, ItemStack itemStack, float y) {
      String itemName = itemStack.getName().getString();
      float width = this.getTextWidth(itemName) / 4.0F;
      this.drawWithShadow(matrixStack, itemName, (0.0F - width) * 2.0F, y * 2.0F, -1);
   }

   private boolean useFont() {
      return Fonts.useNameTagsFont() && FontManager.nameTags() != null;
   }

   private float getTextWidth(String text) {
      return this.useFont() ? FontManager.getNameTagsWidth(text) : (float)mc.textRenderer.getWidth(this.normalizeFormattingPrefixes(text));
   }

   private float getTextHeight() {
      return this.useFont() ? FontManager.getNameTagsHeight() : 9.0F;
   }

   private String getNametagInfo(PlayerEntity player) {
      StringBuilder info = new StringBuilder();
      if (this.gamemodeConfig.getValue()) {
         if (player.isCreative()) {
            info.append(Formatting.GOLD);
            info.append("[C] ");
         } else if (player.isSpectator()) {
            info.append(Formatting.GRAY);
            info.append("[I] ");
         } else {
            info.append(Formatting.BOLD);
            info.append("[S] ");
         }
      }

      if (this.pingConfig.getValue()) {
         info.append(this.getEntityPing(player));
         info.append("ms ");
         info.append(Formatting.RESET);
      }

      info.append(player.getName().getString());
      info.append(" ");
      if (this.entityIdConfig.getValue()) {
         info.append("ID: ");
         info.append(player.getId());
         info.append(" ");
      }

      if (this.healthConfig.getValue()) {
         double health = (double)(player.getHealth() + player.getAbsorptionAmount());
         Formatting hcolor = health > 18.0
            ? Formatting.GREEN
            : (
               health > 16.0
                  ? Formatting.DARK_GREEN
                  : (health > 12.0 ? Formatting.YELLOW : (health > 8.0 ? Formatting.GOLD : (health > 4.0 ? Formatting.RED : Formatting.DARK_RED)))
            );
         String phealth = this.df.format(health);
         info.append(hcolor);
         info.append(phealth);
         info.append(" ");
      }

      if (this.turtleConfig.getValue() && this.showTurtleTime.getValue() && PotionTracker.INSTANCE != null) {
         int turtleLevel = PotionTracker.INSTANCE.getTurtleLevel(player);
         if (turtleLevel > 0) {
            Entry<Long, Integer> entry = PotionTracker.INSTANCE.getTurtlePlayers().get(player.getId());
            if (entry != null) {
               long expiry = entry.getKey();
               long sec = (expiry - System.currentTimeMillis()) / 1000L;
               if (sec > 0L) {
                  info.append(Formatting.BLUE);
                  info.append(String.format("[%d:%02d]", sec / 60L, sec % 60L));
                  info.append(Formatting.RESET);
                  info.append(" ");
               }
            }
         }
      }

      int totems;
      if (this.totemsConfig.getValue() && (totems = Alien.POP.getPop(player)) > 0) {
         Formatting c = TextRadar.getPopColor(totems);
         info.append(c);
         info.append(-totems);
         info.append(" ");
      }

      return info.toString().trim();
   }

   private String getEntityPing(PlayerEntity entity) {
      if (mc.getNetworkHandler() == null) {
         return "§7-1";
      } else {
         PlayerListEntry playerListEntry = mc.getNetworkHandler().getPlayerListEntry(entity.getUuid());
         if (playerListEntry == null) {
            return "§7-1";
         } else {
            int ping = playerListEntry.getLatency();
            Formatting color = ping >= 200 ? Formatting.RED : (ping >= 100 ? Formatting.YELLOW : Formatting.GREEN);
            return color.toString() + ping;
         }
      }
   }

   private void renderIconSprite(MatrixStack matrices, Sprite sprite, float x, float y, float size) {
      RenderSystem.enableBlend();
      RenderSystem.defaultBlendFunc();
      RenderSystem.setShader(GameRenderer::getPositionTexProgram);
      RenderSystem.setShaderTexture(0, sprite.getAtlasId());
      Matrix4f matrix = matrices.peek().getPositionMatrix();
      BufferBuilder buffer = Tessellator.getInstance().begin(DrawMode.QUADS, VertexFormats.POSITION_TEXTURE);
      float u1 = sprite.getMinU();
      float u2 = sprite.getMaxU();
      float v1 = sprite.getMinV();
      float v2 = sprite.getMaxV();
      buffer.vertex(matrix, x, y + size, 0.0F).texture(u1, v2);
      buffer.vertex(matrix, x + size, y + size, 0.0F).texture(u2, v2);
      buffer.vertex(matrix, x + size, y, 0.0F).texture(u2, v1);
      buffer.vertex(matrix, x, y, 0.0F).texture(u1, v1);
      BufferRenderer.drawWithGlobalProgram(buffer.end());
      RenderSystem.disableBlend();
   }

   private int getNametagColor(PlayerEntity player) {
      String name = player.getGameProfile().getName();
      if (Socials.INSTANCE != null) {
         if (Socials.INSTANCE.isFriendsEnabled() && Alien.FRIEND != null && Alien.FRIEND.friendList.contains(name)) {
            return Socials.INSTANCE.friendsColor.getValue().getRGB();
         }

         if (Socials.INSTANCE.isEnemysEnabled() && Alien.ENEMY != null && Alien.ENEMY.enemyList.contains(name)) {
            return Socials.INSTANCE.enemysColor.getValue().getRGB();
         }
      }

      if (this.friendConfig.booleanValue && player.getDisplayName() != null && Alien.FRIEND.isFriend(player)) {
         return this.friendConfig.getValue().getRGB();
      } else if (this.invisibleConfig.booleanValue && player.isInvisible()) {
         return this.invisibleConfig.getValue().getRGB();
      } else if (this.sneakingConfig.booleanValue && player.isSneaking()) {
         return this.sneakingConfig.getValue().getRGB();
      } else {
         return !player.isAlive() ? this.died.getValue().getRGB() : this.colorConfig.getValue().getRGB();
      }
   }

   public static enum ScaleMode {
      Original,
      Current;
   }
}
