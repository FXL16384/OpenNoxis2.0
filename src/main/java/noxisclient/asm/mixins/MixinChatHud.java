package noxisclient.asm.mixins;

import com.llamalad7.mixinextras.sugar.Local;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.client.gui.hud.ChatHudLine;
import net.minecraft.client.gui.hud.MessageIndicator;
import net.minecraft.client.gui.hud.ChatHudLine.Visible;
import net.minecraft.network.message.MessageSignatureData;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import noxisclient.api.interfaces.IChatHudHook;
import noxisclient.api.interfaces.IChatHudLineHook;
import noxisclient.api.utils.math.FadeUtils;
import noxisclient.api.utils.render.ColorUtil;
import noxisclient.asm.accessors.IChatHud;
import noxisclient.mod.modules.impl.client.ClientSetting;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({ChatHud.class})
public abstract class MixinChatHud implements IChatHudHook {
   @Unique
   private int nextMessageId = 0;
   @Unique
   private boolean nextSync;
   @Unique
   private int chatLineIndex;
   @Final
   @Shadow
   private List<Visible> field_2064;
   @Shadow
   @Final
   private List<ChatHudLine> field_2061;

   @Inject(
      method = {"<init>"},
      at = {@At("TAIL")}
   )
   public void onInit(MinecraftClient client, CallbackInfo ci) {
      (((IChatHud)(Object)this)).setMessages(new CopyOnWriteArrayList<>());
      (((IChatHud)(Object)this)).setVisibleMessages(new CopyOnWriteArrayList<>());
   }

   @Override
   public void alienClient$addMessage(Text message, int id) {
      this.nextMessageId = id;
      this.nextSync = true;
      this.method_1812(message);
      this.nextSync = false;
      this.nextMessageId = 0;
   }

   @Override
   public void alienClient$addMessage(Text message) {
      this.nextSync = true;
      this.method_1812(message);
      this.nextSync = false;
   }

   @Override
   public void alienClient$addMessageOutSync(Text message, int id) {
      this.nextMessageId = id;
      this.method_1812(message);
      this.nextMessageId = 0;
   }

   @Inject(
      method = {"addVisibleMessage"},
      at = {@At(
         value = "INVOKE",
         target = "Ljava/util/List;add(ILjava/lang/Object;)V",
         ordinal = 0,
         shift = Shift.AFTER
      )}
   )
   private void onAddMessageAfterNewChatHudLineVisible(ChatHudLine message, CallbackInfo ci) {
      IChatHudLineHook line = (IChatHudLineHook)(Object)this.field_2064.getFirst();
      if (line != null) {
         line.alienClient$setMessageId(this.nextMessageId);
         line.alienClient$setSync(this.nextSync);
         int animationTime = ClientSetting.INSTANCE == null ? 250 : ClientSetting.INSTANCE.animationTime.getValueInt();
         line.alienClient$setFade(new FadeUtils((long)animationTime));
      }
   }

   @Inject(
      method = {"addMessage(Lnet/minecraft/client/gui/hud/ChatHudLine;)V"},
      at = {@At(
         value = "INVOKE",
         target = "Ljava/util/List;add(ILjava/lang/Object;)V",
         ordinal = 0,
         shift = Shift.AFTER
      )}
   )
   private void onAddMessageAfterNewChatHudLine(ChatHudLine message, CallbackInfo ci) {
      IChatHudLineHook line = (IChatHudLineHook)(Object)this.field_2061.getFirst();
      if (line != null) {
         line.alienClient$setMessageId(this.nextMessageId);
         line.alienClient$setSync(this.nextSync);
         int animationTime = ClientSetting.INSTANCE == null ? 250 : ClientSetting.INSTANCE.animationTime.getValueInt();
         line.alienClient$setFade(new FadeUtils((long)animationTime));
      }
   }

   @Inject(
      at = {@At("HEAD")},
      method = {"addMessage(Lnet/minecraft/text/Text;Lnet/minecraft/network/message/MessageSignatureData;Lnet/minecraft/client/gui/hud/MessageIndicator;)V"}
   )
   private void onAddMessage(Text message, MessageSignatureData signatureData, MessageIndicator indicator, CallbackInfo ci) {
      if (this.nextMessageId != 0) {
         this.field_2064.removeIf(msg -> ((IChatHudLineHook)(Object)msg).alienClient$getMessageId() == this.nextMessageId);
         this.field_2061.removeIf(msg -> ((IChatHudLineHook)(Object)msg).alienClient$getMessageId() == this.nextMessageId);
      }
   }

   @Redirect(
      method = {"addVisibleMessage"},
      at = @At(
         value = "INVOKE",
         target = "Ljava/util/List;size()I",
         ordinal = 2,
         remap = false
      ),
      require = 0
   )
   public int chatLinesSize(List<Visible> list) {
      return ClientSetting.INSTANCE != null && ClientSetting.INSTANCE.isOn() && ClientSetting.INSTANCE.infiniteChat.getValue() ? -2147483647 : list.size();
   }

   @Redirect(
      method = {"render"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/gui/DrawContext;drawTextWithShadow(Lnet/minecraft/client/font/TextRenderer;Lnet/minecraft/text/OrderedText;III)I"
      ),
      require = 0
   )
   private int drawStringWithShadow(DrawContext drawContext, TextRenderer textRenderer, OrderedText text, int x, int y, int color) {
      if (ClientSetting.INSTANCE == null) {
         return drawContext.drawTextWithShadow(textRenderer, text, x, y, color);
      } else if (this.chatLineIndex >= 0 && this.field_2064 != null && this.chatLineIndex < this.field_2064.size()) {
         try {
            IChatHudLineHook line = (IChatHudLineHook)(Object)this.field_2064.get(this.chatLineIndex);
            if (line != null && line.alienClient$getFade() != null) {
               FadeUtils fadeUtils = line.alienClient$getFade();
               double ease = fadeUtils.ease(ClientSetting.INSTANCE.ease.getValue());
               double fade = 1.0 - ease;
               x += (int)(fade * ClientSetting.INSTANCE.animateOffset.getValue());
               double c = Math.max(10.0, (double)(color >> 24 & 0xFF) * ease);
               if (line.alienClient$getSync()) {
                  return drawContext.drawTextWithShadow(
                     textRenderer,
                     text,
                     x,
                     y,
                     ColorUtil.injectAlpha(ClientSetting.INSTANCE.color.getValue(), ClientSetting.INSTANCE.fade.getValue() ? (int)c : color >> 24 & 0xFF)
                        .getRGB()
                  );
               }

               return drawContext.drawTextWithShadow(
                  textRenderer, text, x, y, ColorUtil.injectAlpha(color, ClientSetting.INSTANCE.fade.getValue() ? (int)c : color >> 24 & 0xFF)
               );
            }
         } catch (Exception var15) {
         }

         return drawContext.drawTextWithShadow(textRenderer, text, x, y, color);
      } else {
         return drawContext.drawTextWithShadow(textRenderer, text, x, y, color);
      }
   }

   @Inject(
      method = {"render"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/gui/hud/ChatHudLine$Visible;addedTime()I"
      )}
   )
   public void getChatLineIndex(CallbackInfo ci, @Local(ordinal = 13) int chatLineIndex) {
      this.chatLineIndex = chatLineIndex;
   }

   @ModifyVariable(
      method = {"render"},
      at = @At("STORE")
   )
   private MessageIndicator removeMessageIndicator(MessageIndicator messageIndicator) {
      return ClientSetting.INSTANCE != null && ClientSetting.INSTANCE.hideIndicator.getValue() ? null : messageIndicator;
   }

   @Shadow
   public abstract void method_1812(Text var1);

   @Shadow
   public int method_1811() {
      return 0;
   }
}
