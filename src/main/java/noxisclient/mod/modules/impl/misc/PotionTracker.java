package noxisclient.mod.modules.impl.misc;

import java.util.Map;
import java.util.AbstractMap.SimpleEntry;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.entity.projectile.thrown.PotionEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.s2c.play.WorldEventS2CPacket;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import noxisclient.api.events.impl.PacketEvent;
import noxisclient.api.events.impl.TotemEvent;
import noxisclient.mod.modules.Module;
import noxisclient.mod.modules.settings.impl.BooleanSetting;

public class PotionTracker extends Module {
   public static PotionTracker INSTANCE;
   public final BooleanSetting version1_21_6Config = this.add(new BooleanSetting("1.21.6+", false));
   public final BooleanSetting turtleConfig = this.add(new BooleanSetting("Turtle", true));
   public final BooleanSetting strengthConfig = this.add(new BooleanSetting("Strength", true));
   private final Map<Integer, Entry<Long, Integer>> turtlePlayers = new ConcurrentHashMap<>();
   private final Map<Integer, Entry<Long, Integer>> strengthPlayers = new ConcurrentHashMap<>();

   public PotionTracker() {
      super("PotionTracker", "Calculates and tracks potion effects", Module.Category.Misc);
      this.setChinese("药水计算");
      INSTANCE = this;
   }

   public void onPacketReceive(PacketEvent.Receive event) {
      if (event.getPacket() instanceof WorldEventS2CPacket packet) {
         if (packet.getEventId() == 2002) {
            Vec3d splashPos = Vec3d.ofCenter(packet.getPos());
            PotionEntity potion = this.findPotion(splashPos);
            if (potion != null) {
               ItemStack stack = potion.getStack();
               PotionContentsComponent contents = stack.getOrDefault(DataComponentTypes.POTION_CONTENTS, null);
               if (contents != null) {
                  Box affectedBox = potion.getBoundingBox().expand(4.0, 2.0, 4.0);
                  Box impactBox = null;
                  float entityAgeScale = 0.0F;
                  if (this.version1_21_6Config.getValue()) {
                     HitResult hitResult = ProjectileUtil.getCollision(potion, Entity::canHit);
                     impactBox = potion.getBoundingBox().offset(hitResult.getPos().subtract(potion.getPos()));
                     entityAgeScale = getEntityAgeScale(potion);
                  }

                  for (PlayerEntity player : mc.world.getPlayers()) {
                     if (player != null && !player.isSpectator() && !player.isDead() && affectedBox.intersects(player.getBoundingBox())) {
                        double distanceSq;
                        if (this.version1_21_6Config.getValue()) {
                           distanceSq = this.distanceBetweenBoxes(impactBox, player.getBoundingBox().expand((double)entityAgeScale));
                        } else {
                           distanceSq = potion.squaredDistanceTo(player);
                        }

                        if (!(distanceSq >= 16.0)) {
                           for (StatusEffectInstance effect : contents.getEffects()) {
                              int amplifier = effect.getAmplifier();
                              int originalDuration = effect.getDuration();
                              if (originalDuration > 0) {
                                 double distanceFactor = 1.0 - Math.sqrt(distanceSq) / 4.0;
                                 int durationTicks = effect.mapDuration(duration -> (int)(distanceFactor * (double)duration + 0.5));
                                 if (durationTicks >= 20) {
                                    long expiry = System.currentTimeMillis() + (long)durationTicks * 50L;
                                    if (effect.getEffectType().value() == StatusEffects.RESISTANCE.value()) {
                                       if (this.turtleConfig.getValue() && (amplifier == 2 || amplifier == 3)) {
                                          int level = amplifier == 2 ? 3 : 4;
                                          this.turtlePlayers
                                             .merge(
                                                player.getId(),
                                                new SimpleEntry<>(expiry, level),
                                                (old, newVal) -> newVal.getKey() > old.getKey() ? newVal : old
                                             );
                                       }
                                    } else if (effect.getEffectType().value() == StatusEffects.STRENGTH.value()
                                       && this.strengthConfig.getValue()
                                       && (amplifier == 0 || amplifier == 1)) {
                                       int level = amplifier + 1;
                                       this.strengthPlayers
                                          .merge(
                                             player.getId(), new SimpleEntry<>(expiry, level), (old, newVal) -> newVal.getKey() > old.getKey() ? newVal : old
                                          );
                                    }
                                 }
                              }
                           }
                        }
                     }
                  }
               }
            }
         }
      }
   }

   private PotionEntity findPotion(Vec3d splashPos) {
      PotionEntity closest = null;
      double closestDistanceSq = 4.0;

      for (Entity entity : mc.world.getEntities()) {
         if (entity instanceof PotionEntity) {
            PotionEntity potion = (PotionEntity)entity;
            double distanceSq = potion.getPos().squaredDistanceTo(splashPos);
            if (distanceSq < closestDistanceSq) {
               closest = potion;
               closestDistanceSq = distanceSq;
            }
         }
      }

      return closest;
   }

   private double distanceBetweenBoxes(Box boxA, Box boxB) {
      double dx = Math.max(Math.max(boxA.minX - boxB.maxX, boxB.minX - boxA.maxX), 0.0);
      double dy = Math.max(Math.max(boxA.minY - boxB.maxY, boxB.minY - boxA.maxY), 0.0);
      double dz = Math.max(Math.max(boxA.minZ - boxB.maxZ, boxB.minZ - boxA.maxZ), 0.0);
      return dx * dx + dy * dy + dz * dz;
   }

   private static float getEntityAgeScale(Entity entity) {
      return Math.max(0.0F, Math.min(1.0F, (float)(entity.age - 2) / 2.0F));
   }

   public void onTotemPop(TotemEvent event) {
      if (event.getPlayer() != null) {
         this.turtlePlayers.remove(event.getPlayer().getId());
         this.strengthPlayers.remove(event.getPlayer().getId());
      }
   }

   public void onUpdate() {
      this.turtlePlayers.entrySet().removeIf(entry -> System.currentTimeMillis() >= entry.getValue().getKey());
      this.strengthPlayers.entrySet().removeIf(entry -> System.currentTimeMillis() >= entry.getValue().getKey());
   }

   public boolean hasTurtle(PlayerEntity player) {
      if (this.isOn() && this.turtleConfig.getValue()) {
         Entry<Long, Integer> entry = this.turtlePlayers.get(player.getId());
         return entry != null && System.currentTimeMillis() < entry.getKey();
      } else {
         return false;
      }
   }

   public boolean hasStrength(PlayerEntity player) {
      if (this.isOn() && this.strengthConfig.getValue()) {
         Entry<Long, Integer> entry = this.strengthPlayers.get(player.getId());
         return entry != null && System.currentTimeMillis() < entry.getKey();
      } else {
         return false;
      }
   }

   public int getTurtleLevel(PlayerEntity player) {
      Entry<Long, Integer> entry = this.turtlePlayers.get(player.getId());
      return entry != null && System.currentTimeMillis() < entry.getKey() ? entry.getValue() : -1;
   }

   public int getStrengthLevel(PlayerEntity player) {
      Entry<Long, Integer> entry = this.strengthPlayers.get(player.getId());
      return entry != null && System.currentTimeMillis() < entry.getKey() ? entry.getValue() : -1;
   }

   public Map<Integer, Entry<Long, Integer>> getTurtlePlayers() {
      return this.turtlePlayers;
   }

   public Map<Integer, Entry<Long, Integer>> getStrengthPlayers() {
      return this.strengthPlayers;
   }
}
