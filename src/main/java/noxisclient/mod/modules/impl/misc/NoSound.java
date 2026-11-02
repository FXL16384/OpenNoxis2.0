package noxisclient.mod.modules.impl.misc;

import java.util.ArrayList;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import noxisclient.api.events.impl.PlaySoundEvent;
import noxisclient.mod.modules.Module;
import noxisclient.mod.modules.impl.movement.ElytraFly;
import noxisclient.mod.modules.settings.impl.BooleanSetting;

public class NoSound extends Module {
   static final ArrayList<SoundEvent> armor = new ArrayList<>();
   public static NoSound INSTANCE;
   private final BooleanSetting equip = this.add(new BooleanSetting("ArmorEquip", true));
   private final BooleanSetting explode = this.add(new BooleanSetting("Explode", true));
   private final BooleanSetting attack = this.add(new BooleanSetting("Attack", true));
   private final BooleanSetting teleport = this.add(new BooleanSetting("Teleport", true));
   private final BooleanSetting throwConfig = this.add(new BooleanSetting("Throw", true));
   private final BooleanSetting potion = this.add(new BooleanSetting("Potion", true));
   private final BooleanSetting elytra = this.add(new BooleanSetting("Elytra", true));

   public NoSound() {
      super("NoSound", Module.Category.Misc);
      this.setChinese("去除声音");
      INSTANCE = this;
   }

   public void onPlaySound(PlaySoundEvent event) {
      if (this.equip.getValue() || ElytraFly.INSTANCE != null && ElytraFly.INSTANCE.shouldVisualSpoof()) {
         for (SoundEvent se : armor) {
            if (event.sound.getId() == se.getId()) {
               event.cancel();
               return;
            }
         }
      }

      if (!this.explode.getValue()
         || event.sound.getId() != SoundEvents.ENTITY_GENERIC_EXPLODE.value().getId()
            && event.sound.getId() != SoundEvents.ENTITY_DRAGON_FIREBALL_EXPLODE.getId()) {
         if (this.attack.getValue()
            && (
               event.sound.getId() == SoundEvents.ENTITY_PLAYER_ATTACK_WEAK.getId()
                  || event.sound.getId() == SoundEvents.ENTITY_PLAYER_ATTACK_KNOCKBACK.getId()
                  || event.sound.getId() == SoundEvents.ENTITY_PLAYER_ATTACK_STRONG.getId()
            )) {
            event.cancel();
         }

         if (this.teleport.getValue() && event.sound.getId() == SoundEvents.ENTITY_PLAYER_TELEPORT.getId()) {
            event.cancel();
         }

         if (this.potion.getValue() && event.sound.getId() == SoundEvents.ENTITY_SPLASH_POTION_BREAK.getId()) {
            event.cancel();
         }

         if (this.elytra.getValue() && event.sound.getId() == SoundEvents.ITEM_ELYTRA_FLYING.getId()) {
            event.cancel();
         }

         if (this.throwConfig.getValue()
            && (
               event.sound.getId() == SoundEvents.ENTITY_ENDER_PEARL_THROW.getId()
                  || event.sound.getId() == SoundEvents.ENTITY_EGG_THROW.getId()
                  || event.sound.getId() == SoundEvents.ENTITY_EXPERIENCE_BOTTLE_THROW.getId()
                  || event.sound.getId() == SoundEvents.ENTITY_SNOWBALL_THROW.getId()
                  || event.sound.getId() == SoundEvents.ENTITY_SPLASH_POTION_THROW.getId()
            )) {
            event.cancel();
         }
      } else {
         event.cancel();
      }
   }

   static {
      armor.add(SoundEvents.ITEM_ARMOR_EQUIP_NETHERITE.value());
      armor.add(SoundEvents.ITEM_ARMOR_EQUIP_TURTLE.value());
      armor.add(SoundEvents.ITEM_ARMOR_EQUIP_CHAIN.value());
      armor.add(SoundEvents.ITEM_ARMOR_EQUIP_ELYTRA.value());
      armor.add(SoundEvents.ITEM_ARMOR_EQUIP_DIAMOND.value());
      armor.add(SoundEvents.ITEM_ARMOR_EQUIP_GOLD.value());
      armor.add(SoundEvents.ITEM_ARMOR_EQUIP_IRON.value());
      armor.add(SoundEvents.ITEM_ARMOR_EQUIP_LEATHER.value());
      armor.add(SoundEvents.ITEM_ARMOR_EQUIP_GENERIC.value());
   }
}
