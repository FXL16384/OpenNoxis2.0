package noxisclient.mod.modules.impl.render;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Comparator;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.block.entity.EndPortalBlockEntity;
import net.minecraft.block.entity.EnderChestBlockEntity;
import net.minecraft.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.thrown.EnderPearlEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import noxisclient.Alien;
import noxisclient.api.events.impl.EntitySpawnedEvent;
import noxisclient.api.utils.math.MathUtil;
import noxisclient.api.utils.render.Render2DUtil;
import noxisclient.api.utils.render.Render3DUtil;
import noxisclient.api.utils.render.TextUtil;
import noxisclient.api.utils.world.BlockUtil;
import noxisclient.asm.accessors.IEntity;
import noxisclient.mod.modules.Module;
import noxisclient.mod.modules.settings.impl.BooleanSetting;
import noxisclient.mod.modules.settings.impl.ColorSetting;
import noxisclient.mod.modules.settings.impl.EnumSetting;
import noxisclient.mod.modules.settings.impl.SliderSetting;

public class ESP extends Module {
   public static ESP INSTANCE;
   public final EnumSetting<ESP.RenderMode> mode = this.add(new EnumSetting<>("Mode", ESP.RenderMode.ThreeD));
   private final SliderSetting rangeConfig = this.add(new SliderSetting("Range", 64.0, 0.0, 256.0, 1.0));
   private final BooleanSetting twoDOutline = this.add(new BooleanSetting("2DOutline", true, () -> this.mode.getValue() == ESP.RenderMode.TwoD));
   private final BooleanSetting twoDHealth = this.add(new BooleanSetting("2DHealth", true, () -> this.mode.getValue() == ESP.RenderMode.TwoD));
   private final ColorSetting hHealth = this.add(new ColorSetting("HighHealth", new Color(0, 255, 0, 255), () -> this.mode.getValue() == ESP.RenderMode.TwoD));
   private final ColorSetting mHealth = this.add(new ColorSetting("MidHealth", new Color(255, 255, 0, 255), () -> this.mode.getValue() == ESP.RenderMode.TwoD));
   private final ColorSetting lHealth = this.add(new ColorSetting("LowHealth", new Color(255, 0, 0, 255), () -> this.mode.getValue() == ESP.RenderMode.TwoD));
   private final BooleanSetting twoDArmorDura = this.add(new BooleanSetting("2DArmorDura", true, () -> this.mode.getValue() == ESP.RenderMode.TwoD));
   private final ColorSetting armorDuraColor = this.add(
      new ColorSetting("ArmorDuraColor", new Color(3145472), () -> this.mode.getValue() == ESP.RenderMode.TwoD && this.twoDArmorDura.getValue())
   );
   private final SliderSetting duraScale = this.add(
      new SliderSetting("DuraScale", 1.0, 0.0, 2.0, 0.1, () -> this.mode.getValue() == ESP.RenderMode.TwoD && this.twoDArmorDura.getValue())
   );
   public final BooleanSetting target = this.add(new BooleanSetting("Target", true).setParent());
   public final BooleanSetting self = this.add(new BooleanSetting("Self", false, this.target::isOpen));
   private final ColorSetting endPortalFill = this.add(
      new ColorSetting("EndPortalFill", new Color(255, 243, 129, 100), () -> this.mode.getValue() != ESP.RenderMode.TwoD && this.target.isOpen())
         .injectBoolean(false)
   );
   private final ColorSetting endPortalOutline = this.add(
      new ColorSetting("EndPortalOutline", new Color(255, 243, 129, 100), () -> this.mode.getValue() != ESP.RenderMode.TwoD && this.target.isOpen())
         .injectBoolean(false)
   );
   private final ColorSetting itemFill = this.add(new ColorSetting("ItemFill", new Color(255, 255, 255, 100), this.target::isOpen).injectBoolean(true));
   private final ColorSetting itemOutline = this.add(new ColorSetting("ItemOutline", new Color(255, 255, 255, 100), this.target::isOpen).injectBoolean(true));
   private final ColorSetting playerFill = this.add(new ColorSetting("PlayerFill", new Color(255, 255, 255, 100), this.target::isOpen).injectBoolean(true));
   private final ColorSetting playerOutline = this.add(
      new ColorSetting("PlayerOutline", new Color(255, 255, 255, 100), this.target::isOpen).injectBoolean(true)
   );
   private final ColorSetting chestFill = this.add(
      new ColorSetting("ChestFill", new Color(255, 198, 123, 100), () -> this.mode.getValue() != ESP.RenderMode.TwoD && this.target.isOpen())
         .injectBoolean(false)
   );
   private final ColorSetting chestOutline = this.add(
      new ColorSetting("ChestOutline", new Color(255, 198, 123, 100), () -> this.mode.getValue() != ESP.RenderMode.TwoD && this.target.isOpen())
         .injectBoolean(false)
   );
   private final ColorSetting enderChestFill = this.add(
      new ColorSetting("EnderChestFill", new Color(255, 100, 255, 100), () -> this.mode.getValue() != ESP.RenderMode.TwoD && this.target.isOpen())
         .injectBoolean(false)
   );
   private final ColorSetting enderChestOutline = this.add(
      new ColorSetting("EnderChestOutline", new Color(255, 100, 255, 100), () -> this.mode.getValue() != ESP.RenderMode.TwoD && this.target.isOpen())
         .injectBoolean(false)
   );
   private final ColorSetting shulkerBoxFill = this.add(
      new ColorSetting("ShulkerBoxFill", new Color(15, 255, 255, 100), () -> this.mode.getValue() != ESP.RenderMode.TwoD && this.target.isOpen())
         .injectBoolean(false)
   );
   private final ColorSetting shulkerBoxOutline = this.add(
      new ColorSetting("ShulkerBoxOutline", new Color(15, 255, 255, 100), () -> this.mode.getValue() != ESP.RenderMode.TwoD && this.target.isOpen())
         .injectBoolean(false)
   );
   public final BooleanSetting item = this.add(new BooleanSetting("ItemName", false).setParent());
   public final BooleanSetting customName = this.add(new BooleanSetting("CustomName", false, this.item::isOpen));
   public final BooleanSetting count = this.add(new BooleanSetting("Count", true, this.item::isOpen));
   private final ColorSetting text = this.add(new ColorSetting("Text", new Color(255, 255, 255, 255), this.item::isOpen));
   public final BooleanSetting pearl = this.add(new BooleanSetting("PearlOwner", true));

   public ESP() {
      super("ESP", Module.Category.Render);
      this.setChinese("透视");
      INSTANCE = this;
   }

   @Override
   public void onRender3D(MatrixStack matrixStack) {
      if (this.mode.getValue() != ESP.RenderMode.TwoD) {
         double rangeSq = (double)this.rangeConfig.getValueFloat();
         rangeSq *= rangeSq;
         if (this.item.getValue()) {
            for (Entity entity : Alien.THREAD.getEntities()) {
               if (entity instanceof ItemEntity && !(entity.squaredDistanceTo(mc.player) > rangeSq)) {
                  ItemEntity itemEntity = (ItemEntity)entity;
                  int itemCount = itemEntity.getStack().getCount();
                  String s = this.count.getValue() && itemCount > 1 ? " x" + itemCount : "";
                  String name = (this.customName.getValue() ? itemEntity.getStack().getName() : itemEntity.getStack().getItem().getName()).getString();
                  Render3DUtil.drawText3D(
                     name + s,
                     ((IEntity)itemEntity)
                        .getDimensions()
                        .getBoxAt(
                           new Vec3d(
                              MathUtil.interpolate(itemEntity.lastRenderX, itemEntity.getX(), (double)mc.getRenderTickCounter().getTickDelta(true)),
                              MathUtil.interpolate(itemEntity.lastRenderY, itemEntity.getY(), (double)mc.getRenderTickCounter().getTickDelta(true)),
                              MathUtil.interpolate(itemEntity.lastRenderZ, itemEntity.getZ(), (double)mc.getRenderTickCounter().getTickDelta(true))
                           )
                        )
                        .expand(0.0, 0.1, 0.0)
                        .getCenter()
                        .add(0.0, 0.5, 0.0),
                     this.text.getValue()
                  );
               }
            }
         }

         if (this.target.getValue()) {
            if (this.itemFill.booleanValue || this.playerFill.booleanValue) {
               for (Entity entityx : Alien.THREAD.getEntities()) {
                  if (!(entityx.squaredDistanceTo(mc.player) > rangeSq)) {
                     if (!(entityx instanceof ItemEntity) || !this.itemFill.booleanValue && !this.itemOutline.booleanValue) {
                        if ((entityx != mc.player || this.self.getValue() && !mc.options.getPerspective().isFirstPerson())
                           && entityx instanceof PlayerEntity
                           && (this.playerFill.booleanValue || this.playerOutline.booleanValue)) {
                           Color color = this.playerFill.getValue();
                           Render3DUtil.draw3DBox(
                              matrixStack,
                              ((IEntity)entityx)
                                 .getDimensions()
                                 .getBoxAt(
                                    new Vec3d(
                                       MathUtil.interpolate(entityx.lastRenderX, entityx.getX(), (double)mc.getRenderTickCounter().getTickDelta(true)),
                                       MathUtil.interpolate(entityx.lastRenderY, entityx.getY(), (double)mc.getRenderTickCounter().getTickDelta(true)),
                                       MathUtil.interpolate(entityx.lastRenderZ, entityx.getZ(), (double)mc.getRenderTickCounter().getTickDelta(true))
                                    )
                                 )
                                 .expand(0.0, 0.1, 0.0),
                              color,
                              this.playerOutline.getValue(),
                              this.playerOutline.booleanValue,
                              this.playerFill.booleanValue
                           );
                        }
                     } else {
                        Color color = this.itemFill.getValue();
                        Render3DUtil.draw3DBox(
                           matrixStack,
                           ((IEntity)entityx)
                              .getDimensions()
                              .getBoxAt(
                                 new Vec3d(
                                    MathUtil.interpolate(entityx.lastRenderX, entityx.getX(), (double)mc.getRenderTickCounter().getTickDelta(true)),
                                    MathUtil.interpolate(entityx.lastRenderY, entityx.getY(), (double)mc.getRenderTickCounter().getTickDelta(true)),
                                    MathUtil.interpolate(entityx.lastRenderZ, entityx.getZ(), (double)mc.getRenderTickCounter().getTickDelta(true))
                                 )
                              ),
                           color,
                           this.itemOutline.getValue(),
                           this.itemOutline.booleanValue,
                           this.itemFill.booleanValue
                        );
                     }
                  }
               }
            }

            for (BlockEntity blockEntity : BlockUtil.getTileEntities()) {
               if (!(mc.player.squaredDistanceTo(blockEntity.getPos().toCenterPos()) > rangeSq)) {
                  if (!(blockEntity instanceof ChestBlockEntity) || !this.chestFill.booleanValue && !this.chestOutline.booleanValue) {
                     if (!(blockEntity instanceof EnderChestBlockEntity) || !this.enderChestFill.booleanValue && !this.enderChestOutline.booleanValue) {
                        if (!(blockEntity instanceof ShulkerBoxBlockEntity) || !this.shulkerBoxFill.booleanValue && !this.shulkerBoxOutline.booleanValue) {
                           if (blockEntity instanceof EndPortalBlockEntity && (this.endPortalFill.booleanValue || this.endPortalOutline.booleanValue)) {
                              Box box = new Box(blockEntity.getPos());
                              Render3DUtil.draw3DBox(
                                 matrixStack,
                                 box,
                                 this.endPortalFill.getValue(),
                                 this.endPortalOutline.getValue(),
                                 this.endPortalOutline.booleanValue,
                                 this.endPortalFill.booleanValue
                              );
                           }
                        } else {
                           Box box = new Box(blockEntity.getPos());
                           Render3DUtil.draw3DBox(
                              matrixStack,
                              box,
                              this.shulkerBoxFill.getValue(),
                              this.shulkerBoxOutline.getValue(),
                              this.shulkerBoxOutline.booleanValue,
                              this.shulkerBoxFill.booleanValue
                           );
                        }
                     } else {
                        Box box = new Box(blockEntity.getPos());
                        Render3DUtil.draw3DBox(
                           matrixStack,
                           box,
                           this.enderChestFill.getValue(),
                           this.enderChestOutline.getValue(),
                           this.enderChestOutline.booleanValue,
                           this.enderChestFill.booleanValue
                        );
                     }
                  } else {
                     Box box = new Box(blockEntity.getPos());
                     Render3DUtil.draw3DBox(
                        matrixStack, box, this.chestFill.getValue(), this.chestOutline.getValue(), this.chestOutline.booleanValue, this.chestFill.booleanValue
                     );
                  }
               }
            }
         }
      }
   }

   @Override
   public void onRender2D(DrawContext drawContext, float tickDelta) {
      if (this.mode.getValue() == ESP.RenderMode.TwoD) {
         if (!nullCheck()) {
            double rangeSq = (double)this.rangeConfig.getValueFloat();
            rangeSq *= rangeSq;
            if (this.target.getValue()) {
               for (Entity entity : Alien.THREAD.getEntities()) {
                  if (!(entity.squaredDistanceTo(mc.player) > rangeSq)) {
                     if (entity instanceof ItemEntity) {
                        boolean outline = this.itemOutline.booleanValue;
                        boolean fill = this.itemFill.booleanValue;
                        if (outline || fill) {
                           this.draw2DForEntity(
                              drawContext.getMatrices(), entity, this.itemOutline.getValue(), this.itemFill.getValue(), outline, fill, tickDelta, false
                           );
                        }
                     } else if ((entity != mc.player || this.self.getValue() && !mc.options.getPerspective().isFirstPerson()) && entity instanceof PlayerEntity
                        )
                      {
                        boolean outline = this.playerOutline.booleanValue;
                        boolean fill = this.playerFill.booleanValue;
                        if (outline || fill) {
                           this.draw2DForEntity(
                              drawContext.getMatrices(), entity, this.playerOutline.getValue(), this.playerFill.getValue(), outline, fill, tickDelta, true
                           );
                        }
                     }
                  }
               }
            }

            if (this.item.getValue()) {
               for (Entity entityx : Alien.THREAD.getEntities()) {
                  if (entityx instanceof ItemEntity && !(entityx.squaredDistanceTo(mc.player) > rangeSq)) {
                     this.draw2DItemText(drawContext, (ItemEntity)entityx, tickDelta);
                  }
               }
            }
         }
      }
   }

   private void draw2DForEntity(
      MatrixStack matrices, Entity entity, Color outlineColor, Color fillColor, boolean outline, boolean fill, float tickDelta, boolean isPlayer
   ) {
      double[] box = this.getEntity2DBox(entity, tickDelta);
      if (box != null) {
         float minX = (float)box[0];
         float minY = (float)box[1];
         float maxX = (float)box[2];
         float maxY = (float)box[3];
         if (!(maxX - minX <= 0.0F) && !(maxY - minY <= 0.0F)) {
            if (fill) {
               Render2DUtil.drawRect(matrices, minX, minY, maxX - minX, maxY - minY, fillColor);
            }

            if (outline) {
               this.draw2DBox(matrices, minX, minY, maxX, maxY, outlineColor);
            }

            if (this.twoDHealth.getValue() && entity instanceof LivingEntity living && living.getMaxHealth() > 0.0F && living.getHealth() > 0.0F) {
               this.draw2DHealthBar(matrices, minX, minY, maxY, living);
            }

            if (isPlayer && this.twoDArmorDura.getValue() && entity instanceof PlayerEntity) {
               this.draw2DArmorDura(matrices, maxX, minY, maxY, (PlayerEntity)entity);
            }
         }
      }
   }

   private void draw2DBox(MatrixStack matrices, float posX, float posY, float endPosX, float endPosY, Color col) {
      if (this.twoDOutline.getValue()) {
         Render2DUtil.drawRect(matrices, posX - 1.0F, posY, 1.5F, endPosY - posY + 0.5F, Color.BLACK);
         Render2DUtil.drawRect(matrices, posX - 1.0F, posY - 0.5F, endPosX - posX + 1.5F, 1.5F, Color.BLACK);
         Render2DUtil.drawRect(matrices, endPosX - 1.0F, posY, 1.5F, endPosY - posY + 0.5F, Color.BLACK);
         Render2DUtil.drawRect(matrices, posX - 1.0F, endPosY - 1.0F, endPosX - posX + 1.5F, 1.5F, Color.BLACK);
      }

      Render2DUtil.drawRect(matrices, posX - 0.5F, posY, 0.5F, endPosY - posY, col);
      Render2DUtil.drawRect(matrices, posX, endPosY - 0.5F, endPosX - posX, 0.5F, col);
      Render2DUtil.drawRect(matrices, posX - 0.5F, posY, endPosX - posX + 0.5F, 0.5F, col);
      Render2DUtil.drawRect(matrices, endPosX - 0.5F, posY, 0.5F, endPosY - posY, col);
   }

   private void draw2DHealthBar(MatrixStack matrices, float minX, float minY, float maxY, LivingEntity living) {
      float height = maxY - minY;
      float health = living.getHealth();
      float maxHealth = living.getMaxHealth();
      float ratio = Math.max(0.0F, Math.min(1.0F, health / maxHealth));
      Render2DUtil.drawRect(matrices, minX - 4.0F, minY, 1.0F, height, Color.BLACK);
      Render2DUtil.drawRect(matrices, minX - 4.0F, maxY - height * ratio, 1.0F, height * ratio, this.getHealthColor(health));
   }

   private Color getHealthColor(float health) {
      if (health >= 20.0F) {
         return this.hHealth.getValue();
      } else {
         return health > 10.0F ? this.mHealth.getValue() : this.lHealth.getValue();
      }
   }

   private void draw2DArmorDura(MatrixStack matrices, float maxX, float minY, float maxY, PlayerEntity player) {
      float height = maxY - minY;
      float piece = height / 4.0F;
      ArrayList<ItemStack> stacks = new ArrayList<>();
      stacks.add(player.getInventory().armor.get(3));
      stacks.add(player.getInventory().armor.get(2));
      stacks.add(player.getInventory().armor.get(1));
      stacks.add(player.getInventory().armor.get(0));

      for (int i = 0; i < stacks.size(); i++) {
         ItemStack armor = stacks.get(i);
         if (!armor.isEmpty() && armor.getMaxDamage() > 0) {
            float ratio = (float)(armor.getMaxDamage() - armor.getDamage()) / (float)armor.getMaxDamage();
            ratio = Math.max(0.0F, Math.min(1.0F, ratio));
            float segmentTop = minY + piece * (float)i;
            float segmentBottom = segmentTop + piece;
            float filled = piece * ratio * this.duraScale.getValueFloat();
            filled = Math.min(piece, filled);
            Render2DUtil.drawRect(matrices, maxX + 1.5F, segmentBottom - filled, 1.5F, filled, this.armorDuraColor.getValue());
         }
      }
   }

   private double[] getEntity2DBox(Entity ent, float tickDelta) {
      double x = MathUtil.interpolate(ent.lastRenderX, ent.getX(), (double)tickDelta);
      double y = MathUtil.interpolate(ent.lastRenderY, ent.getY(), (double)tickDelta);
      double z = MathUtil.interpolate(ent.lastRenderZ, ent.getZ(), (double)tickDelta);
      Box axisAlignedBB2 = ent.getBoundingBox();
      Box axisAlignedBB = new Box(
         axisAlignedBB2.minX - ent.getX() + x - 0.05,
         axisAlignedBB2.minY - ent.getY() + y,
         axisAlignedBB2.minZ - ent.getZ() + z - 0.05,
         axisAlignedBB2.maxX - ent.getX() + x + 0.05,
         axisAlignedBB2.maxY - ent.getY() + y + 0.15,
         axisAlignedBB2.maxZ - ent.getZ() + z + 0.05
      );
      Vec3d[] vectors = new Vec3d[]{
         new Vec3d(axisAlignedBB.minX, axisAlignedBB.minY, axisAlignedBB.minZ),
         new Vec3d(axisAlignedBB.minX, axisAlignedBB.maxY, axisAlignedBB.minZ),
         new Vec3d(axisAlignedBB.maxX, axisAlignedBB.minY, axisAlignedBB.minZ),
         new Vec3d(axisAlignedBB.maxX, axisAlignedBB.maxY, axisAlignedBB.minZ),
         new Vec3d(axisAlignedBB.minX, axisAlignedBB.minY, axisAlignedBB.maxZ),
         new Vec3d(axisAlignedBB.minX, axisAlignedBB.maxY, axisAlignedBB.maxZ),
         new Vec3d(axisAlignedBB.maxX, axisAlignedBB.minY, axisAlignedBB.maxZ),
         new Vec3d(axisAlignedBB.maxX, axisAlignedBB.maxY, axisAlignedBB.maxZ)
      };
      double minX = Double.POSITIVE_INFINITY;
      double minY = Double.POSITIVE_INFINITY;
      double maxX = Double.NEGATIVE_INFINITY;
      double maxY = Double.NEGATIVE_INFINITY;
      boolean found = false;

      for (Vec3d vector : vectors) {
         Vec3d screen = TextUtil.worldSpaceToScreenSpace(new Vec3d(vector.x, vector.y, vector.z));
         if (screen.z > 0.0 && screen.z < 1.0) {
            found = true;
            minX = Math.min(screen.x, minX);
            minY = Math.min(screen.y, minY);
            maxX = Math.max(screen.x, maxX);
            maxY = Math.max(screen.y, maxY);
         }
      }

      return !found ? null : new double[]{minX, minY, maxX, maxY};
   }

   private void draw2DItemText(DrawContext drawContext, ItemEntity itemEntity, float tickDelta) {
      double[] box = this.getEntity2DBox(itemEntity, tickDelta);
      if (box != null) {
         int itemCount = itemEntity.getStack().getCount();
         String s = this.count.getValue() && itemCount > 1 ? " x" + itemCount : "";
         String name = (this.customName.getValue() ? itemEntity.getStack().getName() : itemEntity.getStack().getItem().getName()).getString();
         String text = name + s;
         int width = mc.textRenderer.getWidth(text);
         float x = (float)(box[0] + (box[2] - box[0]) / 2.0 - (double)width / 2.0);
         drawContext.drawText(mc.textRenderer, text, (int)x, (int)(box[1] - 10.0), this.text.getValue().getRGB(), false);
      }
   }

   public void onReceivePacket(EntitySpawnedEvent event) {
      if (!nullCheck()) {
         Entity entity;
         if (this.pearl.getValue() && (entity = event.getEntity()) instanceof EnderPearlEntity) {
            EnderPearlEntity pearlEntity = (EnderPearlEntity)entity;
            if (pearlEntity.getOwner() != null) {
               pearlEntity.setCustomName(pearlEntity.getOwner().getName());
               pearlEntity.setCustomNameVisible(true);
            } else {
               mc.world
                  .getPlayers()
                  .stream()
                  .min(Comparator.comparingDouble(p -> p.getPos().distanceTo(new Vec3d(pearlEntity.getX(), pearlEntity.getY(), pearlEntity.getZ()))))
                  .ifPresent(player -> {
                     pearlEntity.setCustomName(player.getName());
                     pearlEntity.setCustomNameVisible(true);
                  });
            }
         }
      }
   }

   private static enum RenderMode {
      ThreeD,
      TwoD;
   }
}
