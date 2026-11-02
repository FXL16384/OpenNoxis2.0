package noxisclient.mod.modules.impl.movement;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.CobwebBlock;
import net.minecraft.block.SweetBerryBushBlock;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.screen.DeathScreen;
import net.minecraft.client.gui.screen.ingame.SignEditScreen;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.ClickSlotC2SPacket;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket.Mode;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket.Action;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import noxisclient.Alien;
import noxisclient.api.events.impl.BlockSlipperinessEvent;
import noxisclient.api.events.impl.MovementSlowdownEvent;
import noxisclient.api.events.impl.PacketEvent;
import noxisclient.api.events.impl.SetCurrentHandEvent;
import noxisclient.api.events.impl.SlowMovementEvent;
import noxisclient.api.events.impl.SteppedOnSlimeBlockEvent;
import noxisclient.api.events.impl.StrafeFixEvent;
import noxisclient.api.events.impl.UpdateEvent;
import noxisclient.api.events.impl.VelocityMultiplierEvent;
import noxisclient.mod.modules.Module;
import noxisclient.mod.modules.settings.impl.BooleanSetting;
import noxisclient.mod.modules.settings.impl.SliderSetting;

public class NoSlow extends Module {
   public static NoSlow INSTANCE;
   private final BooleanSetting strict = this.add(new BooleanSetting("Strict", false));
   private final BooleanSetting airStrict = this.add(new BooleanSetting("AirStrict", false));
   private final BooleanSetting grim = this.add(new BooleanSetting("Grim", false));
   private final BooleanSetting grimNew = this.add(new BooleanSetting("GrimV3", false));
   private final BooleanSetting strafeFix = this.add(new BooleanSetting("StrafeFix", false));
   private final BooleanSetting inventoryMove = this.add(new BooleanSetting("InventoryMove", true));
   private final BooleanSetting arrowMove = this.add(new BooleanSetting("ArrowMove", false, () -> this.inventoryMove.getValue()));
   private final BooleanSetting items = this.add(new BooleanSetting("Items", true));
   private final BooleanSetting sneak = this.add(new BooleanSetting("Sneak", false));
   private final BooleanSetting crawl = this.add(new BooleanSetting("Crawl", false));
   private final BooleanSetting shields = this.add(new BooleanSetting("Shields", true));
   private final BooleanSetting webs = this.add(new BooleanSetting("Webs", false));
   private final BooleanSetting berryBush = this.add(new BooleanSetting("BerryBush", false));
   private final SliderSetting webSpeed = this.add(
      new SliderSetting("WebMultiplier", 1.0, 0.0, 1.0, 0.01, () -> this.webs.getValue() || this.berryBush.getValue())
   );
   private final BooleanSetting soulSand = this.add(new BooleanSetting("SoulSand", false));
   private final BooleanSetting honeyBlock = this.add(new BooleanSetting("HoneyBlock", false));
   private final BooleanSetting slimeBlock = this.add(new BooleanSetting("SlimeBlock", false));
   private boolean sneaking = false;

   public NoSlow() {
      super("NoSlow", Module.Category.Movement);
      this.setChinese("无减");
      INSTANCE = this;
   }

   @Override
   public void onDisable() {
      if (this.airStrict.getValue() && this.sneaking) {
         mc.getNetworkHandler().sendPacket(new ClientCommandC2SPacket(mc.player, Mode.RELEASE_SHIFT_KEY));
      }

      this.sneaking = false;
   }

   public void onSetCurrentHand(SetCurrentHandEvent event) {
      if (this.airStrict.getValue() && !this.sneaking && this.checkSlowed()) {
         this.sneaking = true;
         mc.getNetworkHandler().sendPacket(new ClientCommandC2SPacket(mc.player, Mode.PRESS_SHIFT_KEY));
      }
   }

   public void onUpdate(UpdateEvent event) {
      if (this.grim.getValue() && mc.player.isUsingItem() && !mc.player.isSneaking() && this.items.getValue()) {
         if (mc.player.getActiveHand() == Hand.OFF_HAND && this.checkStack(mc.player.getMainHandStack())) {
            sendSequencedPacket(id -> new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, id, mc.player.getYaw(), mc.player.getPitch()));
         } else if (this.checkStack(mc.player.getOffHandStack())) {
            sendSequencedPacket(id -> new PlayerInteractItemC2SPacket(Hand.OFF_HAND, id, mc.player.getYaw(), mc.player.getPitch()));
         }
      }

      if (this.airStrict.getValue() && !mc.player.isUsingItem()) {
         this.sneaking = false;
         mc.getNetworkHandler().sendPacket(new ClientCommandC2SPacket(mc.player, Mode.RELEASE_SHIFT_KEY));
      }

      if (this.inventoryMove.getValue() && this.checkScreen()) {
         long handle = mc.getWindow().getHandle();
         KeyBinding[] keys = new KeyBinding[]{mc.options.jumpKey, mc.options.forwardKey, mc.options.backKey, mc.options.rightKey, mc.options.leftKey};

         for (KeyBinding binding : keys) {
            binding.setPressed(InputUtil.isKeyPressed(handle, binding.getDefaultKey().getCode()));
         }

         if (this.arrowMove.getValue()) {
            float yaw = mc.player.getYaw();
            float pitch = mc.player.getPitch();
            if (InputUtil.isKeyPressed(handle, 265)) {
               pitch -= 3.0F;
            } else if (InputUtil.isKeyPressed(handle, 264)) {
               pitch += 3.0F;
            } else if (InputUtil.isKeyPressed(handle, 263)) {
               yaw -= 3.0F;
            } else if (InputUtil.isKeyPressed(handle, 262)) {
               yaw += 3.0F;
            }

            mc.player.setYaw(yaw);
            mc.player.setPitch(MathHelper.clamp(pitch, -90.0F, 90.0F));
         }
      }

      if ((this.grim.getValue() || this.grimNew.getValue()) && this.webs.getValue()) {
         Box bb = this.grim.getValue() ? mc.player.getBoundingBox().expand(1.0) : mc.player.getBoundingBox();

         for (BlockPos pos : this.getIntersectingWebs(bb)) {
            mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(Action.STOP_DESTROY_BLOCK, pos, Direction.DOWN));
         }
      }
   }

   public void onStrafeFix(StrafeFixEvent event) {
      if (this.strafeFix.getValue()) {
         float yaw = Alien.ROTATION.getLastYaw();
         float pitch = Alien.ROTATION.getLastPitch();
         event.cancel();
         event.setYaw(yaw);
         event.setPitch(pitch);
      }
   }

   public void onSlowMovement(SlowMovementEvent event) {
      Block block = event.getState().getBlock();
      if (block instanceof CobwebBlock && this.webs.getValue() || block instanceof SweetBerryBushBlock && this.berryBush.getValue()) {
         float multiplier = this.webSpeed.getValueFloat();
         if (this.webSpeed.getValueFloat() == 1.0F) {
            multiplier = 0.0F;
         }

         event.cancel();
         event.setMultiplier(multiplier);
      }
   }

   public void onMovementSlowdown(MovementSlowdownEvent event) {
      if (this.sneak.getValue() && mc.player.isSneaking() || this.crawl.getValue() && mc.player.isCrawling()) {
         float f = 1.0F / (float)mc.player.getAttributeValue(EntityAttributes.PLAYER_SNEAKING_SPEED);
         event.input.movementForward *= f;
         event.input.movementSideways *= f;
      }

      if (this.checkSlowed()) {
         event.input.movementForward *= 5.0F;
         event.input.movementSideways *= 5.0F;
      }
   }

   public void onVelocityMultiplier(VelocityMultiplierEvent event) {
      if (event.getBlock() == Blocks.SOUL_SAND && this.soulSand.getValue() || event.getBlock() == Blocks.HONEY_BLOCK && this.honeyBlock.getValue()) {
         event.cancel();
      }
   }

   public void onSteppedOnSlimeBlock(SteppedOnSlimeBlockEvent event) {
      if (this.slimeBlock.getValue()) {
         event.cancel();
      }
   }

   public void onBlockSlipperiness(BlockSlipperinessEvent event) {
      if (event.getBlock() == Blocks.SLIME_BLOCK && this.slimeBlock.getValue()) {
         event.cancel();
         event.setSlipperiness(0.6F);
      }
   }

   public void onPacketOutbound(PacketEvent.Send event) {
      if (mc.player != null && mc.world != null && !mc.isInSingleplayer()) {
         if (event.getPacket() instanceof PlayerMoveC2SPacket packet && packet.changesPosition() && this.strict.getValue() && this.checkSlowed()) {
            mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(mc.player.getInventory().selectedSlot));
            return;
         }

         if (event.getPacket() instanceof ClickSlotC2SPacket && this.strict.getValue()) {
            if (mc.player.isUsingItem()) {
               mc.player.stopUsingItem();
            }

            if (this.sneaking || mc.player.isSneaking()) {
               mc.getNetworkHandler().sendPacket(new ClientCommandC2SPacket(mc.player, Mode.RELEASE_SHIFT_KEY));
            }

            if (mc.player.isSprinting()) {
               mc.getNetworkHandler().sendPacket(new ClientCommandC2SPacket(mc.player, Mode.STOP_SPRINTING));
            }
         }
      }
   }

   private boolean checkGrimNew() {
      return this.isUsingFood() && mc.player.getItemUseTimeLeft() <= 5
         ? false
         : !mc.player.isSneaking()
            && !mc.player.isCrawling()
            && !mc.player.isRiding()
            && (mc.player.getItemUseTimeLeft() < 5 || mc.player.getItemUseTime() > 1 && mc.player.getItemUseTime() % 2 != 0);
   }

   public boolean checkSlowed() {
      return this.grimNew.getValue() && !this.checkGrimNew()
         ? false
         : !mc.player.isRiding()
            && !mc.player.isSneaking()
            && (
               mc.player.isUsingItem() && this.items.getValue()
                  || mc.player.isBlocking() && this.shields.getValue() && !this.grimNew.getValue() && !this.grim.getValue()
            );
   }

   private boolean checkStack(ItemStack stack) {
      return !stack.getComponents().contains(DataComponentTypes.FOOD)
         && stack.getItem() != Items.BOW
         && stack.getItem() != Items.CROSSBOW
         && stack.getItem() != Items.SHIELD;
   }

   private boolean isUsingFood() {
      ItemStack stack = mc.player.getActiveItem();
      return !stack.isEmpty() && stack.getComponents().contains(DataComponentTypes.FOOD);
   }

   public boolean checkScreen() {
      return mc.currentScreen != null
         && !(mc.currentScreen instanceof ChatScreen)
         && !(mc.currentScreen instanceof SignEditScreen)
         && !(mc.currentScreen instanceof DeathScreen);
   }

   public List<BlockPos> getIntersectingWebs(Box boundingBox) {
      List<BlockPos> blocks = new ArrayList<>();
      int minX = (int)Math.floor(boundingBox.minX);
      int maxX = (int)Math.floor(boundingBox.maxX);
      int minY = (int)Math.floor(boundingBox.minY);
      int maxY = (int)Math.floor(boundingBox.maxY);
      int minZ = (int)Math.floor(boundingBox.minZ);
      int maxZ = (int)Math.floor(boundingBox.maxZ);

      for (int x = minX; x <= maxX; x++) {
         for (int y = minY; y <= maxY; y++) {
            for (int z = minZ; z <= maxZ; z++) {
               BlockPos pos = new BlockPos(x, y, z);
               if (mc.world.getBlockState(pos).getBlock() instanceof CobwebBlock) {
                  blocks.add(pos);
               }
            }
         }
      }

      return blocks;
   }

   public boolean noSlow() {
      return this.isOn() && this.checkSlowed();
   }

   public boolean getStrafeFix() {
      return this.isOn() && this.strafeFix.getValue();
   }
}
