package noxisclient.mod.modules.impl.combat;

import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket.Handler;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket.InteractType;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.PositionAndOnGround;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import noxisclient.api.events.impl.PacketEvent;
import noxisclient.api.events.impl.UpdateEvent;
import noxisclient.api.utils.player.EntityUtil;
import noxisclient.api.utils.player.MovementUtil;
import noxisclient.api.utils.world.BlockUtil;
import noxisclient.asm.accessors.IPlayerInteractEntityC2SPacket;
import noxisclient.asm.accessors.IPlayerMoveC2SPacket;
import noxisclient.mod.modules.Module;
import noxisclient.mod.modules.impl.exploit.Blink;
import noxisclient.mod.modules.impl.exploit.BowBomb;
import noxisclient.mod.modules.impl.exploit.Phase;
import noxisclient.mod.modules.impl.player.AutoPearl;
import noxisclient.mod.modules.settings.impl.BooleanSetting;
import noxisclient.mod.modules.settings.impl.EnumSetting;
import noxisclient.mod.modules.settings.impl.SliderSetting;

public class Criticals extends Module {
   public static Criticals INSTANCE;
   public final EnumSetting<Criticals.Mode> mode = this.add(new EnumSetting<>("Mode", Criticals.Mode.OldNCP));
   public final BooleanSetting onlyGround = this.add(new BooleanSetting("OnlyGround", true, () -> !this.mode.is(Criticals.Mode.Ground)));
   private final BooleanSetting setOnGround = this.add(new BooleanSetting("SetNoGround", false, () -> this.mode.is(Criticals.Mode.Ground)));
   private final BooleanSetting blockCheck = this.add(new BooleanSetting("BlockCheck", true, () -> this.mode.is(Criticals.Mode.Ground)));
   private final BooleanSetting autoJump = this.add(new BooleanSetting("AutoJump", true, () -> this.mode.is(Criticals.Mode.Ground)).setParent());
   private final BooleanSetting mini = this.add(new BooleanSetting("Mini", true, () -> this.mode.is(Criticals.Mode.Ground) && this.autoJump.isOpen()));
   private final SliderSetting y = this.add(
      new SliderSetting("MotionY", 0.05, 0.0, 1.0, 1.0E-10, () -> this.mode.is(Criticals.Mode.Ground) && this.autoJump.isOpen())
   );
   private final BooleanSetting autoDisable = this.add(new BooleanSetting("AutoDisable", true, () -> this.mode.is(Criticals.Mode.Ground)));
   private final BooleanSetting crawlingDisable = this.add(new BooleanSetting("CrawlingDisable", true, () -> this.mode.is(Criticals.Mode.Ground)));
   private final BooleanSetting flight = this.add(new BooleanSetting("Flight", false, () -> this.mode.is(Criticals.Mode.Ground)));
   boolean requireJump = false;

   public Criticals() {
      super("Criticals", Module.Category.Combat);
      this.setChinese("刀刀暴击");
      INSTANCE = this;
   }

   @Override
   public String getInfo() {
      return this.mode.getValue().name();
   }

   public void onPacketSend(PacketEvent.Send event) {
      if (!event.isCancelled()) {
         if (!Blink.INSTANCE.isOn() || !Blink.INSTANCE.pauseModule.getValue()) {
            if (this.mode.is(Criticals.Mode.Ground)) {
               if (!BowBomb.send) {
                  if (!AutoPearl.throwing && !Phase.INSTANCE.isOn()) {
                     if (this.setOnGround.getValue()) {
                        if (event.getPacket() instanceof PlayerMoveC2SPacket) {
                           ((IPlayerMoveC2SPacket)event.getPacket()).setOnGround(false);
                        }
                     }
                  }
               }
            } else {
               Packet<?> packet2 = event.getPacket();
               Entity entity;
               PlayerInteractEntityC2SPacket packet;
               if (packet2 instanceof PlayerInteractEntityC2SPacket
                  && getInteractType(packet = (PlayerInteractEntityC2SPacket)packet2) == InteractType.ATTACK
                  && !((entity = getEntity(packet)) instanceof EndCrystalEntity)
                  && (!this.onlyGround.getValue() || mc.player.isOnGround() || mc.player.getAbilities().flying)
                  && !mc.player.isInLava()
                  && !mc.player.isTouchingWater()
                  && entity != null) {
                  this.doCrit(entity);
               }
            }
         }
      }
   }

   @Override
   public void onLogout() {
      if (this.mode.is(Criticals.Mode.Ground) && this.autoDisable.getValue()) {
         this.disable();
      }
   }

   @Override
   public void onEnable() {
      if (!Blink.INSTANCE.isOn() || !Blink.INSTANCE.pauseModule.getValue()) {
         this.requireJump = true;
         if (this.mode.is(Criticals.Mode.Ground)) {
            if (nullCheck()) {
               if (this.autoDisable.getValue()) {
                  this.disable();
               }
            } else if (MovementUtil.isMoving() && this.autoDisable.getValue()) {
               this.disable();
            } else if (this.crawlingDisable.getValue() && mc.player.isCrawling()) {
               this.disable();
            } else if (mc.player.isOnGround()
               && this.autoJump.getValue()
               && (!this.blockCheck.getValue() || BlockUtil.canCollide(mc.player, new Box(EntityUtil.getPlayerPos(true).up(2))))) {
               this.jump();
            }
         }
      }
   }

   public void jump() {
      if (this.mini.getValue()) {
         MovementUtil.setMotionY(this.y.getValue());
      } else {
         mc.player.jump();
      }
   }

   public void onUpdate(UpdateEvent event) {
      if (!Blink.INSTANCE.isOn() || !Blink.INSTANCE.pauseModule.getValue()) {
         if (this.mode.is(Criticals.Mode.Ground)) {
            if (this.crawlingDisable.getValue() && mc.player.isCrawling()) {
               this.disable();
            } else if (MovementUtil.isMoving() && this.autoDisable.getValue()) {
               this.disable();
            } else if (this.flight.getValue() && mc.player.fallDistance > 0.0F) {
               MovementUtil.setMotionY(0.0);
               MovementUtil.setMotionX(0.0);
               MovementUtil.setMotionZ(0.0);
               this.requireJump = false;
            } else if (this.blockCheck.getValue() && !BlockUtil.canCollide(mc.player, new Box(EntityUtil.getPlayerPos(true).up(2)))) {
               this.requireJump = true;
            } else if (mc.player.isOnGround() && this.autoJump.getValue() && (this.flight.getValue() || this.requireJump)) {
               this.jump();
               this.requireJump = false;
            }
         }
      }
   }

   public void doCrit(Entity entity) {
      switch ((Criticals.Mode)this.mode.getValue()) {
         case UpdatedNCP:
            mc.player.addCritParticles(entity);
            mc.getNetworkHandler().sendPacket(new PositionAndOnGround(mc.player.getX(), mc.player.getY() + 2.71875E-7, mc.player.getZ(), false));
            mc.getNetworkHandler().sendPacket(new PositionAndOnGround(mc.player.getX(), mc.player.getY(), mc.player.getZ(), false));
            break;
         case Strict:
            mc.player.addCritParticles(entity);
            mc.getNetworkHandler().sendPacket(new PositionAndOnGround(mc.player.getX(), mc.player.getY() + 0.062600301692775, mc.player.getZ(), false));
            mc.getNetworkHandler().sendPacket(new PositionAndOnGround(mc.player.getX(), mc.player.getY() + 0.07260029960661, mc.player.getZ(), false));
            mc.getNetworkHandler().sendPacket(new PositionAndOnGround(mc.player.getX(), mc.player.getY(), mc.player.getZ(), false));
            mc.getNetworkHandler().sendPacket(new PositionAndOnGround(mc.player.getX(), mc.player.getY(), mc.player.getZ(), false));
            break;
         case NCP:
            mc.player.addCritParticles(entity);
            mc.getNetworkHandler().sendPacket(new PositionAndOnGround(mc.player.getX(), mc.player.getY() + 0.0625, mc.player.getZ(), false));
            mc.getNetworkHandler().sendPacket(new PositionAndOnGround(mc.player.getX(), mc.player.getY(), mc.player.getZ(), false));
            break;
         case OldNCP:
            mc.player.addCritParticles(entity);
            mc.getNetworkHandler().sendPacket(new PositionAndOnGround(mc.player.getX(), mc.player.getY() + 1.058293536E-5, mc.player.getZ(), false));
            mc.getNetworkHandler().sendPacket(new PositionAndOnGround(mc.player.getX(), mc.player.getY() + 9.16580235E-6, mc.player.getZ(), false));
            mc.getNetworkHandler().sendPacket(new PositionAndOnGround(mc.player.getX(), mc.player.getY() + 1.0371854E-7, mc.player.getZ(), false));
            break;
         case Hypixel2K22:
            mc.player.addCritParticles(entity);
            mc.getNetworkHandler().sendPacket(new PositionAndOnGround(mc.player.getX(), mc.player.getY() + 0.0045, mc.player.getZ(), true));
            mc.getNetworkHandler().sendPacket(new PositionAndOnGround(mc.player.getX(), mc.player.getY() + 1.52121E-4, mc.player.getZ(), false));
            mc.getNetworkHandler().sendPacket(new PositionAndOnGround(mc.player.getX(), mc.player.getY() + 0.3, mc.player.getZ(), false));
            mc.getNetworkHandler().sendPacket(new PositionAndOnGround(mc.player.getX(), mc.player.getY() + 0.025, mc.player.getZ(), false));
            break;
         case Packet:
            mc.player.addCritParticles(entity);
            mc.getNetworkHandler().sendPacket(new PositionAndOnGround(mc.player.getX(), mc.player.getY() + 5.0E-4, mc.player.getZ(), false));
            mc.getNetworkHandler().sendPacket(new PositionAndOnGround(mc.player.getX(), mc.player.getY() + 1.0E-4, mc.player.getZ(), false));
         case Ground:
         default:
            break;
         case BBTT:
            if (MovementUtil.isMoving() || !MovementUtil.isStatic()) {
               return;
            }

            mc.getNetworkHandler().sendPacket(new PositionAndOnGround(mc.player.getX(), mc.player.getY(), mc.player.getZ(), true));
            mc.getNetworkHandler().sendPacket(new PositionAndOnGround(mc.player.getX(), mc.player.getY() + 0.0625, mc.player.getZ(), false));
            mc.getNetworkHandler().sendPacket(new PositionAndOnGround(mc.player.getX(), mc.player.getY() + 0.045, mc.player.getZ(), false));
      }
   }

   public static Entity getEntity(PlayerInteractEntityC2SPacket packet) {
      return mc.world == null ? null : mc.world.getEntityById(((IPlayerInteractEntityC2SPacket)packet).getEntityId());
   }

   public static InteractType getInteractType(PlayerInteractEntityC2SPacket packet) {
      final InteractType[] result = new InteractType[1];
      packet.handle(new Handler() {
         @Override
         public void interact(Hand hand) {
            result[0] = InteractType.INTERACT;
         }

         @Override
         public void interactAt(Hand hand, Vec3d pos) {
            result[0] = InteractType.INTERACT_AT;
         }

         @Override
         public void attack() {
            result[0] = InteractType.ATTACK;
         }
      });
      return result[0];
   }

   public static enum Mode {
      UpdatedNCP,
      Strict,
      NCP,
      OldNCP,
      Hypixel2K22,
      Packet,
      Ground,
      BBTT;
   }
}
