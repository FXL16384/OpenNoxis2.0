package noxisclient.mod.modules.impl.misc;

import java.awt.Color;
import java.text.DecimalFormat;
import java.util.Objects;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Formatting;
import noxisclient.Alien;
import noxisclient.api.events.impl.DeathEvent;
import noxisclient.api.events.impl.EntitySpawnEvent;
import noxisclient.api.events.impl.PacketEvent;
import noxisclient.api.events.impl.RemoveEntityEvent;
import noxisclient.api.events.impl.TotemEvent;
import noxisclient.api.events.impl.UpdateEvent;
import noxisclient.api.utils.math.Timer;
import noxisclient.api.utils.player.InventoryUtil;
import noxisclient.api.utils.render.TextUtil;
import noxisclient.core.impl.CommandManager;
import noxisclient.mod.modules.Module;
import noxisclient.mod.modules.impl.client.ClientSetting;
import noxisclient.mod.modules.impl.client.Fonts;
import noxisclient.mod.modules.settings.impl.BooleanSetting;
import noxisclient.mod.modules.settings.impl.SliderSetting;

public class Tips extends Module {
   public static Tips INSTANCE;
   public final BooleanSetting visualRange = this.add(new BooleanSetting("VisualRange", false).setParent());
   public final BooleanSetting friends = this.add(new BooleanSetting("Friends", false, this.visualRange::isOpen));
   public final BooleanSetting popCounter = this.add(new BooleanSetting("PopCounter", true));
   public final BooleanSetting deathCoords = this.add(new BooleanSetting("DeathCoords", true));
   public final BooleanSetting serverLag = this.add(new BooleanSetting("ServerLag", true));
   public final BooleanSetting lagBack = this.add(new BooleanSetting("LagBack", true));
   public final BooleanSetting potion = this.add(new BooleanSetting("Potion", true).setParent());
   public final BooleanSetting resistanceLevelCheck = this.add(new BooleanSetting("ResistanceLevelCheck", true, this.potion::isOpen));
   public final BooleanSetting potionWarning = this.add(new BooleanSetting("PotionWarning", true, this.potion::isOpen).setParent());
   private final SliderSetting potionWarningThreshold = this.add(new SliderSetting("WarningThreshold", 3.0, 0.0, 64.0, 1.0, this.potionWarning::isOpen));
   private final SliderSetting potionWarningYOffset = this.add(new SliderSetting("WarningYOffset", 0.0, -200.0, 200.0, 1.0, this.potionWarning::isOpen));
   private final SliderSetting yOffset = this.add(new SliderSetting("YOffset", 0, -200, 200, this.potion::isOpen));
   final DecimalFormat df = new DecimalFormat("0.0");
   final int color = new Color(190, 0, 0).getRGB();
   private final Timer lagTimer = new Timer();
   private final Timer lagBackTimer = new Timer();
   int turtles = 0;

   public Tips() {
      super("Tips", Module.Category.Misc);
      this.setChinese("提示");
      INSTANCE = this;
   }

   public void onAddEntity(EntitySpawnEvent event) {
      if (this.visualRange.getValue() && event.getEntity() instanceof PlayerEntity && event.getEntity().getDisplayName() != null) {
         String playerName = event.getEntity().getDisplayName().getString();
         boolean isFriend = Alien.FRIEND.isFriend(playerName);
         if ((!isFriend || this.friends.getValue()) && event.getEntity() != mc.player) {
            CommandManager.sendMessageId(
               (isFriend ? Formatting.AQUA + playerName : Formatting.WHITE + playerName) + "§f entered your visual range.", event.getEntity().getId() + 777
            );
            mc.world.playSound(mc.player, mc.player.getBlockPos(), SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, SoundCategory.PLAYERS, 100.0F, 1.9F);
         }
      }
   }

   public void onRemoveEntity(RemoveEntityEvent event) {
      if (this.visualRange.getValue() && event.getEntity() instanceof PlayerEntity && event.getEntity().getDisplayName() != null) {
         String playerName = event.getEntity().getDisplayName().getString();
         boolean isFriend = Alien.FRIEND.isFriend(playerName);
         if ((!isFriend || this.friends.getValue()) && event.getEntity() != mc.player) {
            CommandManager.sendMessageId(
               (isFriend ? Formatting.AQUA + playerName : Formatting.WHITE + playerName) + "§f left your visual range.", event.getEntity().getId() + 777
            );
            mc.world.playSound(mc.player, mc.player.getBlockPos(), SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, SoundCategory.PLAYERS, 100.0F, 1.9F);
         }
      }
   }

   public void onUpdate(UpdateEvent event) {
      if (this.potion.getValue()) {
         this.turtles = InventoryUtil.getPotionCount(StatusEffects.RESISTANCE.value());
      }
   }

   public void onPacketEvent(PacketEvent.Receive event) {
      this.lagTimer.reset();
      if (event.getPacket() instanceof PlayerPositionLookS2CPacket) {
         this.lagBackTimer.reset();
      }
   }

   @Override
   public void onRender2D(DrawContext drawContext, float tickDelta) {
      if (this.serverLag.getValue() && this.lagTimer.passedS(1.4)) {
         String text = "Server not responding (" + this.df.format((double)this.lagTimer.getMs() / 1000.0) + "s)";
         int n = mc.getWindow().getScaledWidth() / 2 - mc.textRenderer.getWidth(text) / 2;
         Objects.requireNonNull(mc.textRenderer);
         TextUtil.drawString(drawContext, text, (double)n, 19.0, this.color, false, true);
      }

      if (this.lagBack.getValue() && !this.lagBackTimer.passedS(1.5)) {
         String text = "Lagback (" + this.df.format((double)(1500L - this.lagBackTimer.getMs()) / 1000.0) + "s)";
         int n = mc.getWindow().getScaledWidth() / 2 - mc.textRenderer.getWidth(text) / 2;
         Objects.requireNonNull(mc.textRenderer);
         TextUtil.drawString(drawContext, text, (double)n, 28.0, this.color, false, true);
      }

      if (this.potion.getValue()) {
         StringBuilder stringBuilder = new StringBuilder();
         boolean shouldWarn = this.potionWarning.getValue() && this.turtles <= this.potionWarningThreshold.getValueInt();
         if (shouldWarn) {
            String warning = "§c警告:你的§b神龟药水§c还剩§e" + this.turtles + "§c瓶";
            int warningX = mc.getWindow().getScaledWidth() / 2 - mc.textRenderer.getWidth(warning) / 2;
            int warningY = mc.getWindow().getScaledHeight() / 2 + 9 - this.yOffset.getValueInt() - 9 + this.potionWarningYOffset.getValueInt();
            TextUtil.drawString(drawContext, warning, (double)warningX, (double)warningY, -1, Fonts.useTipsFont(), true);
         }

         if (this.turtles > 0) {
            stringBuilder.append("§e").append(this.turtles);
         }

         if (mc.player.hasStatusEffect(StatusEffects.RESISTANCE)
            && (!this.resistanceLevelCheck.getValue() || mc.player.getStatusEffect(StatusEffects.RESISTANCE).getAmplifier() > 0)) {
            if (!stringBuilder.isEmpty()) {
               stringBuilder.append(" ");
            }

            stringBuilder.append("§9").append(mc.player.getStatusEffect(StatusEffects.RESISTANCE).getDuration() / 20 + 1);
         }

         if (mc.player.hasStatusEffect(StatusEffects.STRENGTH)) {
            if (!stringBuilder.isEmpty()) {
               stringBuilder.append(" ");
            }

            stringBuilder.append("§4").append(mc.player.getStatusEffect(StatusEffects.STRENGTH).getDuration() / 20 + 1);
         }

         if (mc.player.hasStatusEffect(StatusEffects.SPEED)) {
            if (!stringBuilder.isEmpty()) {
               stringBuilder.append(" ");
            }

            stringBuilder.append("§b").append(mc.player.getStatusEffect(StatusEffects.SPEED).getDuration() / 20 + 1);
         }

         String string = stringBuilder.toString();
         int n = mc.getWindow().getScaledWidth() / 2 - mc.textRenderer.getWidth(stringBuilder.toString()) / 2;
         int n2 = mc.getWindow().getScaledHeight() / 2;
         Objects.requireNonNull(mc.textRenderer);
         TextUtil.drawString(drawContext, string, (double)n, (double)(n2 + 9 - this.yOffset.getValueInt()), -1, Fonts.useTipsFont(), true);
      }
   }

   public void onPlayerDeath(DeathEvent event) {
      PlayerEntity player = event.getPlayer();
      if (this.popCounter.getValue()) {
         if (Alien.POP.popContainer.containsKey(player.getName().getString())) {
            int l_Count = Alien.POP.popContainer.get(player.getName().getString());
            if (l_Count == 1) {
               if (player.equals(mc.player)) {
                  this.sendMessage("§fYou§r died after popping §f" + l_Count + "§r totem.", player.getId());
               } else {
                  this.sendMessage("§f" + player.getName().getString() + "§r died after popping §f" + l_Count + "§r totem.", player.getId());
               }
            } else if (player.equals(mc.player)) {
               this.sendMessage("§fYou§r died after popping §f" + l_Count + "§r totems.", player.getId());
            } else {
               this.sendMessage("§f" + player.getName().getString() + "§r died after popping §f" + l_Count + "§r totems.", player.getId());
            }
         } else if (player.equals(mc.player)) {
            this.sendMessage("§fYou§r died.", player.getId());
         } else {
            this.sendMessage("§f" + player.getName().getString() + "§r died.", player.getId());
         }
      }

      if (this.deathCoords.getValue() && player == mc.player) {
         this.sendMessage("§4You died at " + player.getBlockX() + ", " + player.getBlockY() + ", " + player.getBlockZ());
      }
   }

   public void onTotem(TotemEvent event) {
      if (this.popCounter.getValue()) {
         PlayerEntity player = event.getPlayer();
         int l_Count = 1;
         if (Alien.POP.popContainer.containsKey(player.getName().getString())) {
            l_Count = Alien.POP.popContainer.get(player.getName().getString());
         }

         if (l_Count == 1) {
            if (player.equals(mc.player)) {
               this.sendMessage("§fYou§r popped §f" + l_Count + "§r totem.", player.getId());
            } else {
               this.sendMessage("§f" + player.getName().getString() + " §rpopped §f" + l_Count + "§r totems.", player.getId());
            }
         } else if (player.equals(mc.player)) {
            this.sendMessage("§fYou§r popped §f" + l_Count + "§r totem.", player.getId());
         } else {
            this.sendMessage("§f" + player.getName().getString() + " §rhas popped §f" + l_Count + "§r totems.", player.getId());
         }
      }
   }

   public void sendMessage(String message, int id) {
      if (!nullCheck()) {
         if (ClientSetting.INSTANCE.messageStyle.getValue() == ClientSetting.Style.Moon) {
            CommandManager.sendMessageId("§f[§3" + this.getName() + "§f] " + message, id);
            return;
         }

         CommandManager.sendMessageId(message, id);
      }
   }
}
