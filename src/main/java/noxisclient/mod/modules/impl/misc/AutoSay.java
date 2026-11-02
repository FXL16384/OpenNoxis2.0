package noxisclient.mod.modules.impl.misc;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import net.minecraft.entity.player.PlayerEntity;
import noxisclient.Alien;
import noxisclient.api.events.impl.DeathEvent;
import noxisclient.api.events.impl.UpdateEvent;
import noxisclient.mod.modules.settings.impl.BooleanSetting;
import noxisclient.mod.modules.settings.impl.EnumSetting;
import noxisclient.mod.modules.settings.impl.SliderSetting;
import noxisclient.mod.modules.settings.impl.StringSetting;

public final class AutoSay extends AiChatFeature {
   private static final String RANDOM_CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
   private final BooleanSetting autoEz = this.add(new BooleanSetting("AutoEZ", false).setParent());
   private final EnumSetting<AutoSay.EzType> ezType = this.add(new EnumSetting<>("EZType", AutoSay.EzType.Bot, this.autoEz::isOpen));
   private final StringSetting ezMessage = this.add(
      new StringSetting("EZMessage", "%player%", () -> this.autoEz.isOpen() && this.ezType.getValue() == AutoSay.EzType.Custom)
   );
   private final SliderSetting ezRange = this.add(new SliderSetting("EZRange", 10.0, 0.0, 20.0, 0.1, this.autoEz::isOpen));
   private final SliderSetting randomLength = this.add(new SliderSetting("RandomLength", 3.0, 0.0, 20.0, 1.0, this.autoEz::isOpen));
   private final BooleanSetting autoCommand = this.add(new BooleanSetting("AutoCommand", false).setParent());
   private final StringSetting command = this.add(new StringSetting("Command", "kit 1", this.autoCommand::isOpen));
   private final BooleanSetting commandOnJoin = this.add(new BooleanSetting("OnJoin", true, this.autoCommand::isOpen));
   private final BooleanSetting commandOnDeath = this.add(new BooleanSetting("OnDeath", true, this.autoCommand::isOpen));
   private final BooleanSetting commandOnRespawn = this.add(new BooleanSetting("OnRespawn", false, this.autoCommand::isOpen));
   private final SliderSetting commandDelay = this.add(new SliderSetting("CommandDelay", 2.0, 0.0, 10.0, 0.1, this.autoCommand::isOpen));
   private final Random random = new Random();
   private final List<AutoSay.PendingCommand> pendingCommands = new ArrayList<>();
   private boolean wasDead;

   public AutoSay() {
      super("AutoSay");
      this.setChinese("自动发言");
   }

   @Override
   public void onEnable() {
      this.pendingCommands.clear();
      this.wasDead = mc.player != null && this.isDead(mc.player);
      super.onEnable();
   }

   @Override
   public void onDisable() {
      this.pendingCommands.clear();
      super.onDisable();
   }

   @Override
   public void onLogin() {
      this.pendingCommands.clear();
      this.wasDead = false;
      if (this.autoCommand.getValue() && this.commandOnJoin.getValue()) {
         this.queueCommand();
      }
   }

   @Override
   public void onLogout() {
      this.pendingCommands.clear();
      this.wasDead = false;
   }

   public void onDeath(DeathEvent event) {
      PlayerEntity player = event.getPlayer();
      if (player != null && mc.player != null) {
         if (player == mc.player) {
            this.wasDead = true;
            if (this.autoCommand.getValue() && this.commandOnDeath.getValue()) {
               this.queueCommand();
            }
         } else {
            if (this.autoEz.getValue()) {
               this.sendAutoEz(player);
            }
         }
      }
   }

   public void onUpdate(UpdateEvent event) {
      if (mc.player != null && mc.getNetworkHandler() != null) {
         boolean dead = this.isDead(mc.player);
         if (this.wasDead && !dead && this.autoCommand.getValue() && this.commandOnRespawn.getValue()) {
            this.queueCommand();
         }

         this.wasDead = dead;
         if (!this.autoCommand.getValue()) {
            this.pendingCommands.clear();
         } else {
            long now = System.currentTimeMillis();
            Iterator<AutoSay.PendingCommand> iterator = this.pendingCommands.iterator();

            while (iterator.hasNext()) {
               AutoSay.PendingCommand pending = iterator.next();
               if (now >= pending.executeAt) {
                  iterator.remove();
                  mc.getNetworkHandler().sendChatCommand(pending.command);
               }
            }
         }
      }
   }

   private void sendAutoEz(PlayerEntity player) {
      if (Alien.FRIEND == null || !Alien.FRIEND.isFriend(player)) {
         if (!(this.ezRange.getValue() > 0.0) || !((double)mc.player.distanceTo(player) > this.ezRange.getValue())) {
            String playerName = player.getName().getString();
            String suffix = this.generateRandomString(this.randomLength.getValueInt());
            if (!suffix.isEmpty()) {
               suffix = " " + suffix;
            }
            String message = switch ((AutoSay.EzType)this.ezType.getValue()) {
               case Bot -> this.randomMessage(AutoEzMessages.BOT) + " " + playerName;
               case Custom -> this.ezMessage.getValue().replace("%player%", playerName);
               case AutoSex -> this.randomMessage(AutoEzMessages.SEX) + " " + playerName;
            };
            mc.getNetworkHandler().sendChatMessage(message + suffix);
         }
      }
   }

   private String randomMessage(List<String> messages) {
      return messages.get(this.random.nextInt(messages.size()));
   }

   private String generateRandomString(int length) {
      StringBuilder result = new StringBuilder(length);

      for (int i = 0; i < length; i++) {
         result.append(
            "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz".charAt(this.random.nextInt("ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz".length()))
         );
      }

      return result.toString();
   }

   private void queueCommand() {
      String normalized = this.command.getValue() == null ? "" : this.command.getValue().trim();

      while (normalized.startsWith("/")) {
         normalized = normalized.substring(1).trim();
      }

      if (!normalized.isEmpty()) {
         long executeAt = System.currentTimeMillis() + (long)(this.commandDelay.getValue() * 1000.0);
         this.pendingCommands.add(new AutoSay.PendingCommand(normalized, executeAt));
      }
   }

   private boolean isDead(PlayerEntity player) {
      return player.isDead() || player.getHealth() <= 0.0F;
   }

   private static enum EzType {
      Bot,
      Custom,
      AutoSex;
   }

   private static final class PendingCommand {
      private final String command;
      private final long executeAt;

      private PendingCommand(String command, long executeAt) {
         this.command = command;
         this.executeAt = executeAt;
      }
   }
}
