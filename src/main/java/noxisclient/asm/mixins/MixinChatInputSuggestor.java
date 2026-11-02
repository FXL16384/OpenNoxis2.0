package noxisclient.asm.mixins;

import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ChatInputSuggestor;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import noxisclient.Vitality;
import noxisclient.api.utils.render.Render2DUtil;
import noxisclient.mod.commands.Command;
import noxisclient.mod.modules.impl.client.ClientSetting;
import org.apache.commons.lang3.StringUtils;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({ChatInputSuggestor.class})
public abstract class MixinChatInputSuggestor {
   @Final
   @Shadow
   TextFieldWidget field_21599;
   @Shadow
   private CompletableFuture<Suggestions> field_21611;
   @Final
   @Shadow
   private List<OrderedText> field_21607;
   @Unique
   private boolean showOutline = false;

   @Shadow
   public abstract void method_23920(boolean var1);

   @Inject(
      at = {@At("HEAD")},
      method = {"render"}
   )
   private void onRender(DrawContext context, int mouseX, int mouseY, CallbackInfo ci) {
      if (this.showOutline && ClientSetting.INSTANCE != null) {
         int x = this.field_21599.getX() - 3;
         int y = this.field_21599.getY() - 3;
         Render2DUtil.drawRect(
            context.getMatrices(), (float)x, (float)y, (float)(this.field_21599.getWidth() + 1), 1.0F, ClientSetting.INSTANCE.color.getValue().getRGB()
         );
         Render2DUtil.drawRect(
            context.getMatrices(),
            (float)x,
            (float)(y + this.field_21599.getHeight() + 1),
            (float)(this.field_21599.getWidth() + 1),
            1.0F,
            ClientSetting.INSTANCE.color.getValue().getRGB()
         );
         Render2DUtil.drawRect(
            context.getMatrices(), (float)x, (float)y, 1.0F, (float)(this.field_21599.getHeight() + 1), ClientSetting.INSTANCE.color.getValue().getRGB()
         );
         Render2DUtil.drawRect(
            context.getMatrices(),
            (float)(x + this.field_21599.getWidth() + 1),
            (float)y,
            1.0F,
            (float)(this.field_21599.getHeight() + 2),
            ClientSetting.INSTANCE.color.getValue().getRGB()
         );
      }
   }

   @Inject(
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/gui/widget/TextFieldWidget;getCursor()I",
         ordinal = 0
      )},
      method = {"refresh()V"}
   )
   private void onRefresh(CallbackInfo ci) {
      String prefix = Vitality.getPrefix();
      if (Vitality.COMMAND == null) {
         this.showOutline = false;
      } else {
         String string = this.field_21599.getText();
         this.showOutline = string.startsWith(prefix);
         String string2;
         if (!string.isEmpty() && (prefix.startsWith(string2 = string.substring(0, this.field_21599.getCursor())) || string2.startsWith(prefix))) {
            int j = 0;
            Matcher matcher = Pattern.compile("(\\s+)").matcher(string2);

            while (matcher.find()) {
               j = matcher.end();
            }

            SuggestionsBuilder builder = new SuggestionsBuilder(string2, j);
            if (string2.length() < prefix.length()) {
               if (!prefix.startsWith(string2)) {
                  return;
               }

               builder.suggest(prefix);
            } else {
               if (!string2.startsWith(prefix)) {
                  return;
               }

               int count = StringUtils.countMatches(string2, " ");
               List<String> seperated = Arrays.asList(string2.split(" ", -1));
               if (count == 0) {
                  ArrayList<String> commands = new ArrayList<>(Vitality.COMMAND.getCommands().keySet());
                  commands.sort(String::compareToIgnoreCase);

                  for (String str : commands) {
                     builder.suggest(Vitality.getPrefix() + str + " ");
                  }
               } else {
                  if (seperated.isEmpty()) {
                     return;
                  }

                  Command c = Vitality.COMMAND.getCommandBySyntax(seperated.getFirst().substring(prefix.length()));
                  if (c == null) {
                     this.field_21607.add(Text.of(" §4no commands found: §f" + seperated.getFirst().substring(prefix.length())).asOrderedText());
                     return;
                  }

                  String[] suggestions = c.getAutocorrect(count, seperated);
                  if (suggestions == null || suggestions.length == 0) {
                     return;
                  }

                  for (String str : suggestions) {
                     builder.suggest(str + " ");
                  }
               }
            }

            this.field_21611 = builder.buildFuture();
            this.field_21611.thenRun(() -> {
               if (this.field_21611 != null && this.field_21611.isDone()) {
                  this.method_23920(false);
               }
            });
         }
      }
   }
}
