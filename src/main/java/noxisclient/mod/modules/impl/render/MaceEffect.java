package noxisclient.mod.modules.impl.render;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket.InteractType;
import net.minecraft.network.packet.s2c.play.WorldEventS2CPacket;
import net.minecraft.util.math.Vec3d;
import noxisclient.api.events.impl.PacketEvent;
import noxisclient.mod.modules.Module;
import noxisclient.mod.modules.impl.combat.Criticals;
import noxisclient.mod.modules.impl.render.mace.MaceParticles;
import noxisclient.mod.modules.settings.impl.BooleanSetting;
import noxisclient.mod.modules.settings.impl.SliderSetting;

public class MaceEffect extends Module {
   public static MaceEffect INSTANCE;
   private static final int MACE_SMASH_WORLD_EVENT = 2013;
   private final BooleanSetting maceShockwave = this.add(new BooleanSetting("MaceShockwave", true));
   private final SliderSetting maceShockwaveSize = this.add(new SliderSetting("MaceShockwaveSize", 1.0, 0.1, 3.0, 0.05));
   private final SliderSetting maceShockwaveOpacity = this.add(new SliderSetting("MaceShockwaveOpacity", 1.0, 0.0, 1.0, 0.01));
   private final BooleanSetting maceSpark = this.add(new BooleanSetting("MaceSpark", true));
   private final BooleanSetting maceFlash = this.add(new BooleanSetting("MaceFlash", true));
   private final BooleanSetting doubleWave = this.add(new BooleanSetting("DoubleWave", true));
   private final SliderSetting sparkCount = this.add(new SliderSetting("SparkCount", 1, 0, 8));
   private final SliderSetting sparkSpread = this.add(new SliderSetting("SparkSpread", 1.0, 0.1, 2.5, 0.1));

   public MaceEffect() {
      super("MaceEffect", Module.Category.Render);
      this.setChinese("重锤特效");
      INSTANCE = this;
   }

   public static void playSlam(Vec3d pos) {
      if (INSTANCE != null && !INSTANCE.isOff() && !Module.nullCheck()) {
         INSTANCE.spawnSlamEffect(pos);
      }
   }

   public void onPacketReceive(PacketEvent.Receive event) {
      if (!Module.nullCheck() && !event.isCancelled()) {
         if (event.getPacket() instanceof WorldEventS2CPacket packet && packet.getEventId() == 2013) {
            playSlam(Vec3d.ofCenter(packet.getPos()).add(0.0, 0.5, 0.0));
         }
      }
   }

   public void onPacketSend(PacketEvent.Send event) {
      if (!Module.nullCheck() && !event.isCancelled()) {
         if (mc.player.getMainHandStack().isOf(Items.MACE)) {
            Packet<?> rawPacket = event.getPacket();
            PlayerInteractEntityC2SPacket packet;
            if (rawPacket instanceof PlayerInteractEntityC2SPacket
               && Criticals.getInteractType(packet = (PlayerInteractEntityC2SPacket)rawPacket) == InteractType.ATTACK) {
               if (Criticals.getEntity(packet) instanceof PlayerEntity player) {
                  if (this.isFallingMaceSmash()) {
                     playSlam(player.getPos());
                  }
               }
            }
         }
      }
   }

   private boolean isFallingMaceSmash() {
      return mc.player.fallDistance > 1.5F
         && !mc.player.isOnGround()
         && !mc.player.getAbilities().flying
         && !mc.player.isFallFlying()
         && !mc.player.isClimbing()
         && !mc.player.isTouchingWater()
         && !mc.player.isSubmergedInWater()
         && !mc.player.isInLava()
         && !mc.player.hasVehicle();
   }

   private void spawnSlamEffect(Vec3d pos) {
      if (mc.world != null) {
         if (this.maceShockwave.getValue()) {
            mc.world.addParticle(MaceParticles.SMASH, pos.x, pos.y, pos.z, 0.8, this.maceShockwaveOpacity.getValue(), 1.5 * this.maceShockwaveSize.getValue());
            if (this.doubleWave.getValue()) {
               mc.world
                  .addParticle(MaceParticles.SMASH, pos.x, pos.y, pos.z, 0.4, this.maceShockwaveOpacity.getValue(), 0.35 * this.maceShockwaveSize.getValue());
            }
         }

         if (this.maceSpark.getValue()) {
            this.spawnSparks(pos);
         }

         if (this.maceFlash.getValue()) {
            mc.world.addParticle(MaceParticles.FLASH, pos.x, pos.y, pos.z, 0.0, 0.0, 0.0);
         }
      }
   }

   private void spawnSparks(Vec3d pos) {
      int count = this.sparkCount.getValueInt();
      double spread = this.sparkSpread.getValue();

      for (int i = 0; i < count; i++) {
         Vec3d offset = new Vec3d(this.randomRange(-0.6, 0.6) * spread, this.randomRange(-1.2, 1.2) * spread, this.randomRange(-0.6, 0.6) * spread);
         double x = pos.x + offset.x;
         double y = pos.y + offset.y + 0.9;
         double z = pos.z + offset.z;
         mc.world.addParticle(MaceParticles.FLICK, x, y, z, 0.5, 0.0, 0.0);
         mc.world.addParticle(MaceParticles.FLICK, x, y, z, 0.2, 0.0, 0.0);
      }
   }

   private double randomRange(double min, double max) {
      return min + mc.world.random.nextDouble() * (max - min);
   }
}
