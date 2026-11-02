package noxisclient.mod.modules.impl.player;

import net.minecraft.block.AnvilBlock;
import net.minecraft.block.Block;
import net.minecraft.block.ChestBlock;
import net.minecraft.block.EnderChestBlock;
import net.minecraft.client.gui.screen.DeathScreen;
import net.minecraft.item.PickaxeItem;
import net.minecraft.item.SwordItem;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import noxisclient.api.events.impl.PacketEvent;
import noxisclient.api.events.impl.UpdateEvent;
import noxisclient.mod.modules.Module;
import noxisclient.mod.modules.settings.impl.BooleanSetting;
import noxisclient.mod.modules.settings.impl.SliderSetting;

public class InteractTweaks extends Module {
   public static InteractTweaks INSTANCE;
   public final BooleanSetting noEntityTrace = this.add(new BooleanSetting("NoEntityTrace", true).setParent());
   public final BooleanSetting onlyPickaxe = this.add(new BooleanSetting("OnlyPickaxe", true, this.noEntityTrace::isOpen));
   public final BooleanSetting multiTask = this.add(new BooleanSetting("MultiTask", true));
   public final BooleanSetting respawn = this.add(new BooleanSetting("Respawn", true));
   public final BooleanSetting ghostHand = this.add(new BooleanSetting("IgnoreBedrock", false));
   private final BooleanSetting noAbort = this.add(new BooleanSetting("NoMineAbort", false));
   private final BooleanSetting noReset = this.add(new BooleanSetting("NoMineReset", false));
   private final BooleanSetting noDelay = this.add(new BooleanSetting("NoMineDelay", false));
   private final BooleanSetting noInteract = this.add(new BooleanSetting("NoInteract", false));
   private final BooleanSetting reach = this.add(new BooleanSetting("Reach", false));
   public final SliderSetting blockRange = this.add(new SliderSetting("BlockRange", 5.0, 0.0, 15.0, 0.1, this.reach::getValue));
   public final SliderSetting entityRange = this.add(new SliderSetting("EntityRange", 5.0, 0.0, 15.0, 0.1, this.reach::getValue));
   private final SliderSetting delay = this.add(new SliderSetting("UseDelay", 4.0, 0.0, 4.0, 1.0));
   public boolean isActive;

   public InteractTweaks() {
      super("InteractTweaks", Module.Category.Player);
      this.setChinese("交互调整");
      INSTANCE = this;
   }

   public void onUpdate(UpdateEvent event) {
      if (this.respawn.getValue() && mc.currentScreen instanceof DeathScreen) {
         mc.player.requestRespawn();
         mc.setScreen(null);
      }

      if (mc.itemUseCooldown <= 4 - this.delay.getValueInt()) {
         mc.itemUseCooldown = 0;
      }
   }

   public void onPacket(PacketEvent.Send event) {
      Packet<?> packet;
      if (!nullCheck() && this.noInteract.getValue() && (packet = event.getPacket()) instanceof PlayerInteractBlockC2SPacket) {
         PlayerInteractBlockC2SPacket packet2 = (PlayerInteractBlockC2SPacket)packet;
         Block block = mc.world.getBlockState(packet2.getBlockHitResult().getBlockPos()).getBlock();
         if (!mc.player.isSneaking() && (block instanceof ChestBlock || block instanceof EnderChestBlock || block instanceof AnvilBlock)) {
            event.cancel();
         }
      }
   }

   @Override
   public void onDisable() {
      this.isActive = false;
   }

   public boolean reach() {
      return this.isOn() && this.reach.getValue();
   }

   public boolean noAbort() {
      return this.isOn() && this.noAbort.getValue() && !mc.options.useKey.isPressed();
   }

   public boolean noReset() {
      return this.isOn() && this.noReset.getValue();
   }

   public boolean noDelay() {
      return this.isOn() && this.noDelay.getValue();
   }

   public boolean multiTask() {
      return this.isOn() && this.multiTask.getValue();
   }

   public boolean noEntityTrace() {
      if (this.isOff() || !this.noEntityTrace.getValue()) {
         return false;
      } else {
         return !this.onlyPickaxe.getValue()
            ? true
            : mc.player.getMainHandStack().getItem() instanceof PickaxeItem
               || mc.player.isUsingItem() && !(mc.player.getMainHandStack().getItem() instanceof SwordItem);
      }
   }

   public boolean ghostHand() {
      return this.isOn() && this.ghostHand.getValue() && !mc.options.useKey.isPressed() && !mc.options.sneakKey.isPressed();
   }
}
