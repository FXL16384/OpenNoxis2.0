package noxisclient.mod.modules.impl.client;

import java.awt.Color;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map.Entry;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import noxisclient.Alien;
import noxisclient.api.events.impl.PacketEvent;
import noxisclient.api.events.impl.TotemEvent;
import noxisclient.core.impl.FontManager;
import noxisclient.mod.modules.Module;
import noxisclient.mod.modules.impl.misc.PotionTracker;
import noxisclient.mod.modules.settings.impl.BooleanSetting;
import noxisclient.mod.modules.settings.impl.ColorSetting;
import noxisclient.mod.modules.settings.impl.SliderSetting;

public class TextRadar extends Module {
   public static TextRadar INSTANCE;
   private final DecimalFormat df = new DecimalFormat("0.0");
   private final BooleanSetting shadow = this.add(new BooleanSetting("Shadow", true));
   private final SliderSetting x = this.add(new SliderSetting("X", 0, 0, 1500));
   private final SliderSetting y = this.add(new SliderSetting("Y", 100, 0, 1000));
   private final ColorSetting color = this.add(new ColorSetting("Color", new Color(255, 255, 255)));
   private final ColorSetting friend = this.add(new ColorSetting("Friend").injectBoolean(true));
   private final BooleanSetting doubleBlank = this.add(new BooleanSetting("Double", false));
   private final BooleanSetting health = this.add(new BooleanSetting("Health", true));
   private final BooleanSetting pops = this.add(new BooleanSetting("Pops", true));
   public final BooleanSetting red = this.add(new BooleanSetting("Red", false));
   private final BooleanSetting distance = this.add(new BooleanSetting("Distance", true));
   private final BooleanSetting effects = this.add(new BooleanSetting("Effects", true));
   private final BooleanSetting turtle = this.add(new BooleanSetting("Turtle", true));
   private final BooleanSetting strength = this.add(new BooleanSetting("Strength", true));

   private boolean useFont() {
      return Fonts.useTextRadarFont();
   }

   public TextRadar() {
      super("TextRadar", Module.Category.Client);
      this.setChinese("文字雷达");
      INSTANCE = this;
   }

   public void onPacketReceive(PacketEvent.Receive event) {
   }

   public void onTotemPop(TotemEvent event) {
   }

   @Override
   public void onRender2D(DrawContext drawContext, float tickDelta) {
      int currentY = this.y.getValueInt();
      List<AbstractClientPlayerEntity> players = new ArrayList<>(mc.world.getPlayers());
      players.sort(Comparator.comparingDouble(playerx -> (double)mc.player.distanceTo(playerx)));

      for (PlayerEntity player : players) {
         if (player != mc.player) {
            StringBuilder stringBuilder = new StringBuilder();
            String blank = this.doubleBlank.getValue() ? "  " : " ";
            if (this.health.getValue()) {
               stringBuilder.append(getHealthColor(player));
               stringBuilder.append(this.df.format((double)(player.getHealth() + player.getAbsorptionAmount())));
               stringBuilder.append(blank);
            }

            stringBuilder.append(Formatting.RESET);
            stringBuilder.append(player.getName().getString());
            if (this.distance.getValue()) {
               stringBuilder.append(blank);
               stringBuilder.append(Formatting.WHITE);
               stringBuilder.append(this.df.format((double)mc.player.distanceTo(player)));
               stringBuilder.append("m");
            }

            if (this.effects.getValue()) {
               if (player.hasStatusEffect(StatusEffects.SLOWNESS)) {
                  StatusEffectInstance effect = player.getStatusEffect(StatusEffects.SLOWNESS);
                  if (effect != null) {
                     stringBuilder.append(blank);
                     stringBuilder.append(Formatting.GRAY);
                     stringBuilder.append("Slow");
                     stringBuilder.append(" ");
                     stringBuilder.append("Lv.");
                     stringBuilder.append(effect.getAmplifier() + 1);
                     stringBuilder.append(" ");
                     stringBuilder.append(effect.getDuration() / 20 + 1);
                     stringBuilder.append("s");
                  }
               }

               if (player.hasStatusEffect(StatusEffects.SPEED)) {
                  StatusEffectInstance effect = player.getStatusEffect(StatusEffects.SPEED);
                  if (effect != null) {
                     stringBuilder.append(blank);
                     stringBuilder.append(Formatting.AQUA);
                     stringBuilder.append("Speed");
                     stringBuilder.append(" ");
                     stringBuilder.append("Lv.");
                     stringBuilder.append(effect.getAmplifier() + 1);
                     stringBuilder.append(" ");
                     stringBuilder.append(effect.getDuration() / 20 + 1);
                     stringBuilder.append("s");
                  }
               }

               if (player.hasStatusEffect(StatusEffects.STRENGTH)) {
                  StatusEffectInstance effect = player.getStatusEffect(StatusEffects.STRENGTH);
                  if (effect != null) {
                     stringBuilder.append(blank);
                     stringBuilder.append(Formatting.DARK_RED);
                     stringBuilder.append("Strength");
                     stringBuilder.append(" ");
                     stringBuilder.append("Lv.");
                     stringBuilder.append(effect.getAmplifier() + 1);
                     stringBuilder.append(" ");
                     stringBuilder.append(effect.getDuration() / 20 + 1);
                     stringBuilder.append("s");
                  }
               }

               if (player.hasStatusEffect(StatusEffects.RESISTANCE)) {
                  StatusEffectInstance effect = player.getStatusEffect(StatusEffects.RESISTANCE);
                  if (effect != null) {
                     stringBuilder.append(blank);
                     stringBuilder.append(Formatting.BLUE);
                     stringBuilder.append("Resist");
                     stringBuilder.append(" ");
                     stringBuilder.append("Lv.");
                     stringBuilder.append(effect.getAmplifier() + 1);
                     stringBuilder.append(" ");
                     stringBuilder.append(effect.getDuration() / 20 + 1);
                     stringBuilder.append("s");
                  }
               }

               if (player.hasStatusEffect(StatusEffects.REGENERATION)) {
                  StatusEffectInstance effect = player.getStatusEffect(StatusEffects.REGENERATION);
                  if (effect != null) {
                     stringBuilder.append(blank);
                     stringBuilder.append(Formatting.GREEN);
                     stringBuilder.append("Regen");
                     stringBuilder.append(" ");
                     stringBuilder.append("Lv.");
                     stringBuilder.append(effect.getAmplifier() + 1);
                     stringBuilder.append(" ");
                     stringBuilder.append(effect.getDuration() / 20 + 1);
                     stringBuilder.append("s");
                  }
               }

               if (player.hasStatusEffect(StatusEffects.INVISIBILITY)) {
                  StatusEffectInstance effect = player.getStatusEffect(StatusEffects.INVISIBILITY);
                  if (effect != null) {
                     stringBuilder.append(blank);
                     stringBuilder.append(Formatting.WHITE);
                     stringBuilder.append("Invis");
                     stringBuilder.append(" ");
                     stringBuilder.append("Lv.");
                     stringBuilder.append(effect.getAmplifier() + 1);
                     stringBuilder.append(" ");
                     stringBuilder.append(effect.getDuration() / 20 + 1);
                     stringBuilder.append("s");
                  }
               }

               if (player.hasStatusEffect(StatusEffects.JUMP_BOOST)) {
                  StatusEffectInstance effect = player.getStatusEffect(StatusEffects.JUMP_BOOST);
                  if (effect != null) {
                     stringBuilder.append(blank);
                     stringBuilder.append(Formatting.LIGHT_PURPLE);
                     stringBuilder.append("Jump");
                     stringBuilder.append(" ");
                     stringBuilder.append("Lv.");
                     stringBuilder.append(effect.getAmplifier() + 1);
                     stringBuilder.append(" ");
                     stringBuilder.append(effect.getDuration() / 20 + 1);
                     stringBuilder.append("s");
                  }
               }

               if (player.hasStatusEffect(StatusEffects.FIRE_RESISTANCE)) {
                  StatusEffectInstance effect = player.getStatusEffect(StatusEffects.FIRE_RESISTANCE);
                  if (effect != null) {
                     stringBuilder.append(blank);
                     stringBuilder.append(Formatting.RED);
                     stringBuilder.append("FireRes");
                     stringBuilder.append(" ");
                     stringBuilder.append("Lv.");
                     stringBuilder.append(effect.getAmplifier() + 1);
                     stringBuilder.append(" ");
                     stringBuilder.append(effect.getDuration() / 20 + 1);
                     stringBuilder.append("s");
                  }
               }

               if (player.hasStatusEffect(StatusEffects.WATER_BREATHING)) {
                  StatusEffectInstance effect = player.getStatusEffect(StatusEffects.WATER_BREATHING);
                  if (effect != null) {
                     stringBuilder.append(blank);
                     stringBuilder.append(Formatting.DARK_AQUA);
                     stringBuilder.append("WaterBreath");
                     stringBuilder.append(" ");
                     stringBuilder.append("Lv.");
                     stringBuilder.append(effect.getAmplifier() + 1);
                     stringBuilder.append(" ");
                     stringBuilder.append(effect.getDuration() / 20 + 1);
                     stringBuilder.append("s");
                  }
               }

               if (player.hasStatusEffect(StatusEffects.NIGHT_VISION)) {
                  StatusEffectInstance effect = player.getStatusEffect(StatusEffects.NIGHT_VISION);
                  if (effect != null) {
                     stringBuilder.append(blank);
                     stringBuilder.append(Formatting.GREEN);
                     stringBuilder.append("NightVision");
                     stringBuilder.append(" ");
                     stringBuilder.append("Lv.");
                     stringBuilder.append(effect.getAmplifier() + 1);
                     stringBuilder.append(" ");
                     stringBuilder.append(effect.getDuration() / 20 + 1);
                     stringBuilder.append("s");
                  }
               }
            }

            if (this.pops.getValue()) {
               int totemPopped = Alien.POP.getPop(player);
               if (totemPopped > 0) {
                  stringBuilder.append(blank);
                  stringBuilder.append(getPopColor(totemPopped));
                  stringBuilder.append("-");
                  stringBuilder.append(totemPopped);
               }
            }

            boolean isFriend = Alien.FRIEND.isFriend(player);
            if (!isFriend || this.friend.booleanValue) {
               int textColor = isFriend ? this.friend.getValue().getRGB() : this.color.getValue().getRGB();
               if (this.useFont()) {
                  FontManager.ui
                     .drawString(
                        drawContext.getMatrices(), stringBuilder.toString(), (double)this.x.getValueInt(), (double)currentY, textColor, this.shadow.getValue()
                     );
               } else {
                  drawContext.drawText(mc.textRenderer, stringBuilder.toString(), this.x.getValueInt(), currentY, textColor, this.shadow.getValue());
               }

               int textWidth = this.useFont() ? (int)FontManager.ui.getWidth(stringBuilder.toString()) : mc.textRenderer.getWidth(stringBuilder.toString());
               int currentX = this.x.getValueInt() + textWidth + 2;
               if (this.turtle.getValue() && PotionTracker.INSTANCE != null) {
                  int turtleLevel = PotionTracker.INSTANCE.getTurtleLevel(player);
                  if (turtleLevel > 0) {
                     Entry<Long, Integer> entry = PotionTracker.INSTANCE.getTurtlePlayers().get(player.getId());
                     if (entry != null) {
                        long expiry = entry.getKey();
                        if (System.currentTimeMillis() < expiry) {
                           long remaining = expiry - System.currentTimeMillis();
                           int seconds = (int)(remaining / 1000L);
                           String time = String.format("%d:%02d", seconds / 60, seconds % 60);
                           String levelRoman = this.toRomanNumeral(turtleLevel);
                           String displayText = levelRoman + " " + time;
                           int iconSize = 8;
                           Identifier texture = Identifier.of("minecraft", "textures/mob_effect/resistance.png");
                           drawContext.drawTexture(texture, currentX, currentY, 0.0F, 0.0F, iconSize, iconSize, iconSize, iconSize);
                           int textX = currentX + iconSize + 2;
                           int colorTurtle = -11184641;
                           if (this.useFont()) {
                              FontManager.ui
                                 .drawString(drawContext.getMatrices(), displayText, (float)textX, (float)currentY, colorTurtle, this.shadow.getValue());
                           } else {
                              drawContext.drawText(mc.textRenderer, displayText, textX, currentY, colorTurtle, this.shadow.getValue());
                           }

                           int textWidthEffect = this.useFont() ? (int)FontManager.ui.getWidth(displayText) : mc.textRenderer.getWidth(displayText);
                           currentX = textX + textWidthEffect + 4;
                        }
                     }
                  }
               }

               if (this.strength.getValue() && PotionTracker.INSTANCE != null) {
                  int strengthLevel = PotionTracker.INSTANCE.getStrengthLevel(player);
                  if (strengthLevel > 0) {
                     Entry<Long, Integer> entry = PotionTracker.INSTANCE.getStrengthPlayers().get(player.getId());
                     if (entry != null) {
                        long expiry = entry.getKey();
                        if (System.currentTimeMillis() < expiry) {
                           long remaining = expiry - System.currentTimeMillis();
                           int seconds = (int)(remaining / 1000L);
                           String time = String.format("%d:%02d", seconds / 60, seconds % 60);
                           String levelRoman = this.toRomanNumeral(strengthLevel);
                           String displayText = levelRoman + " " + time;
                           int iconSize = 8;
                           Identifier texture = Identifier.of("minecraft", "textures/mob_effect/strength.png");
                           drawContext.drawTexture(texture, currentX, currentY, 0.0F, 0.0F, iconSize, iconSize, iconSize, iconSize);
                           int textX = currentX + iconSize + 2;
                           int colorStrength = -43691;
                           if (this.useFont()) {
                              FontManager.ui
                                 .drawString(drawContext.getMatrices(), displayText, (float)textX, (float)currentY, colorStrength, this.shadow.getValue());
                           } else {
                              drawContext.drawText(mc.textRenderer, displayText, textX, currentY, colorStrength, this.shadow.getValue());
                           }
                        }
                     }
                  }
               }

               currentY += this.useFont() ? (int)FontManager.ui.getFontHeight() + 2 : 9 + 2;
            }
         }
      }
   }

   private String toRomanNumeral(int number) {
      switch (number) {
         case 1:
            return "I";
         case 2:
            return "II";
         case 3:
            return "III";
         case 4:
            return "IV";
         case 5:
            return "V";
         case 6:
            return "VI";
         case 7:
            return "VII";
         case 8:
            return "VIII";
         case 9:
            return "IX";
         case 10:
            return "X";
         default:
            return String.valueOf(number);
      }
   }

   public static Formatting getHealthColor(PlayerEntity player) {
      double health = (double)(player.getHealth() + player.getAbsorptionAmount());
      if (health > 18.0) {
         return Formatting.GREEN;
      } else if (health > 16.0) {
         return Formatting.DARK_GREEN;
      } else if (health > 12.0) {
         return Formatting.YELLOW;
      } else if (health > 8.0) {
         return Formatting.GOLD;
      } else {
         return health > 4.0 ? Formatting.RED : Formatting.DARK_RED;
      }
   }

   public static Formatting getPopColor(int totems) {
      if (INSTANCE.red.getValue()) {
         return Formatting.RED;
      } else if (totems > 10) {
         return Formatting.DARK_RED;
      } else if (totems > 8) {
         return Formatting.RED;
      } else if (totems > 6) {
         return Formatting.GOLD;
      } else if (totems > 4) {
         return Formatting.YELLOW;
      } else {
         return totems > 2 ? Formatting.DARK_GREEN : Formatting.GREEN;
      }
   }
}
