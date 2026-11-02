package noxisclient.asm.mixins;

import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.ref.LocalIntRef;
import java.util.Comparator;
import java.util.List;
import net.minecraft.client.gui.hud.PlayerListHud;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.text.Text;
import noxisclient.Alien;
import noxisclient.api.utils.Wrapper;
import noxisclient.mod.modules.impl.client.Socials;
import noxisclient.mod.modules.impl.misc.ExtraTab;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({PlayerListHud.class})
public abstract class MixinPlayerListHud {
   @Final
   @Shadow
   private static Comparator<PlayerListEntry> field_2156;

   @Shadow
   protected abstract List<PlayerListEntry> method_48213();

   @Inject(
      method = {"collectPlayerEntries"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void collectPlayerEntriesHook(CallbackInfoReturnable<List<PlayerListEntry>> cir) {
      if (ExtraTab.INSTANCE != null && ExtraTab.INSTANCE.isOn()) {
         cir.setReturnValue(
            Wrapper.mc
               .player
               .networkHandler
               .getListedPlayerListEntries()
               .stream()
               .filter(ExtraTab.INSTANCE::shouldInclude)
               .sorted(field_2156)
               .limit((long)ExtraTab.INSTANCE.size.getValueInt())
               .toList()
         );
      }
   }

   @Inject(
      method = {"getPlayerName"},
      at = {@At("RETURN")},
      cancellable = true
   )
   private void colorSocialName(PlayerListEntry entry, CallbackInfoReturnable<Text> cir) {
      if (Socials.INSTANCE != null && cir.getReturnValue() != null) {
         String name = entry.getProfile().getName();
         Integer color = null;
         if (Socials.INSTANCE.isFriendsEnabled() && Alien.FRIEND != null && Alien.FRIEND.friendList.contains(name)) {
            color = Socials.INSTANCE.friendsColor.getValue().getRGB();
         } else if (Socials.INSTANCE.isEnemysEnabled() && Alien.ENEMY != null && Alien.ENEMY.enemyList.contains(name)) {
            color = Socials.INSTANCE.enemysColor.getValue().getRGB();
         }

         if (color != null) {
            int rgb = color & 16777215;
            cir.setReturnValue(cir.getReturnValue().copy().styled(style -> style.withColor(rgb)));
         }
      }
   }

   @Inject(
      method = {"render"},
      at = {@At(
         value = "INVOKE",
         target = "Ljava/lang/Math;min(II)I",
         shift = Shift.BEFORE
      )}
   )
   private void hookRender(CallbackInfo ci, @Local(ordinal = 5) LocalIntRef o, @Local(ordinal = 6) LocalIntRef p) {
      if (ExtraTab.INSTANCE != null && ExtraTab.INSTANCE.isOn()) {
         int totalPlayers = this.method_48213().size();
         int newP = 1;

         int newO;
         for (newO = (totalPlayers + newP - 1) / newP; newO > ExtraTab.INSTANCE.columns.getValueInt(); newO = (totalPlayers + newP - 1) / newP) {
            newP++;
         }

         o.set(newO);
         p.set(newP);
      }
   }
}
