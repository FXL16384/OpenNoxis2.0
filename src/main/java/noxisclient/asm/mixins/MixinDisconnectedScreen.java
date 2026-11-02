package noxisclient.asm.mixins;

import it.unimi.dsi.fastutil.Pair;
import net.minecraft.client.gui.screen.DisconnectedScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.screen.multiplayer.ConnectScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.DirectionalLayoutWidget;
import net.minecraft.client.gui.widget.ButtonWidget.Builder;
import net.minecraft.client.network.ServerAddress;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.text.Text;
import noxisclient.api.utils.Wrapper;
import noxisclient.mod.modules.impl.misc.AutoReconnect;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({DisconnectedScreen.class})
public abstract class MixinDisconnectedScreen extends Screen {
   @Shadow
   @Final
   private DirectionalLayoutWidget field_44552;
   @Unique
   private ButtonWidget reconnectBtn;
   @Unique
   private double time = AutoReconnect.INSTANCE.delay.getValue() * 20.0;

   protected MixinDisconnectedScreen(Text title) {
      super(title);
   }

   @Inject(
      method = {"init"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/gui/widget/DirectionalLayoutWidget;refreshPositions()V",
         shift = Shift.BEFORE
      )}
   )
   private void addButtons(CallbackInfo ci) {
      if (AutoReconnect.INSTANCE.lastServerConnection != null) {
         this.reconnectBtn = new Builder(Text.literal(this.getText()), button -> this.tryConnecting()).build();
         this.field_44552.add(this.reconnectBtn);
      }
   }

   @Override
   public void tick() {
      if (AutoReconnect.INSTANCE.rejoin() && AutoReconnect.INSTANCE.lastServerConnection != null) {
         if (this.time <= 0.0) {
            this.tryConnecting();
         } else {
            this.time--;
            if (this.reconnectBtn != null) {
               this.reconnectBtn.setMessage(Text.literal(this.getText()));
            }
         }
      }
   }

   @Unique
   private String getText() {
      Object reconnectText = "Reconnect";
      if (AutoReconnect.INSTANCE.rejoin()) {
         reconnectText = (String)reconnectText + " " + String.format("(%.1f)", this.time / 20.0);
      }

      return (String)reconnectText;
   }

   @Unique
   private void tryConnecting() {
      Pair<ServerAddress, ServerInfo> lastServer = AutoReconnect.INSTANCE.lastServerConnection;
      ConnectScreen.connect(new TitleScreen(), Wrapper.mc, lastServer.left(), lastServer.right(), false, null);
   }
}
