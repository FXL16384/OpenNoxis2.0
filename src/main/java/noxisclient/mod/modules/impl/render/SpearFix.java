package noxisclient.mod.modules.impl.render;

import java.util.Locale;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtInt;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Arm;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import noxisclient.mod.modules.Module;
import noxisclient.mod.modules.settings.impl.BooleanSetting;

public class SpearFix extends Module {
   public static SpearFix INSTANCE;
   public final BooleanSetting itemId = this.add(new BooleanSetting("ItemId", true));
   public final BooleanSetting itemName = this.add(new BooleanSetting("ItemName", true));
   public final BooleanSetting grimSafe = this.add(new BooleanSetting("GrimSafe", true));
   private static final int VIA_WOODEN_SPEAR_ITEM_ID = 1296;
   private static final String VIA_ORIGINAL_HASHES_KEY = "VV|original_hashes";
   private static final String VIA_1_21_11_ITEM_ID_KEY = "VB|Protocol1_21_11To1_21_9|id";
   private static final Identifier SPEAR_ATTACK_ID = Identifier.of("minecraft", "item.spear.attack");
   private static final Identifier SPEAR_HIT_ID = Identifier.of("minecraft", "item.spear.hit");
   private static final Identifier SPEAR_USE_ID = Identifier.of("minecraft", "item.spear.use");
   private static final Identifier SPEAR_LUNGE_ID = Identifier.of("minecraft", "item.spear.lunge");
   private static final Identifier SPEAR_WOOD_ATTACK_ID = Identifier.of("minecraft", "item.spear_wood.attack");
   private static final Identifier SPEAR_WOOD_HIT_ID = Identifier.of("minecraft", "item.spear_wood.hit");
   private static final Identifier SPEAR_WOOD_USE_ID = Identifier.of("minecraft", "item.spear_wood.use");
   private static SoundEvent SPEAR_ATTACK;
   private static SoundEvent SPEAR_HIT;
   private static SoundEvent SPEAR_USE;
   private static SoundEvent SPEAR_LUNGE;
   private static SoundEvent SPEAR_WOOD_ATTACK;
   private static SoundEvent SPEAR_WOOD_HIT;
   private static SoundEvent SPEAR_WOOD_USE;
   private long lastAttackSound;
   private long lastUseSound;

   public SpearFix() {
      super("SpearFix", "Applies spear stab animations to spear-named items.", Module.Category.Render);
      this.setChinese("长矛修复");
      INSTANCE = this;
   }

   public static void registerSounds() {
      SPEAR_ATTACK = registerSound(SPEAR_ATTACK_ID);
      SPEAR_HIT = registerSound(SPEAR_HIT_ID);
      SPEAR_USE = registerSound(SPEAR_USE_ID);
      SPEAR_LUNGE = registerSound(SPEAR_LUNGE_ID);
      SPEAR_WOOD_ATTACK = registerSound(SPEAR_WOOD_ATTACK_ID);
      SPEAR_WOOD_HIT = registerSound(SPEAR_WOOD_HIT_ID);
      SPEAR_WOOD_USE = registerSound(SPEAR_WOOD_USE_ID);
   }

   private static SoundEvent registerSound(Identifier id) {
      try {
         return Registry.register(Registries.SOUND_EVENT, id, SoundEvent.of(id));
      } catch (RuntimeException var3) {
         SoundEvent existing = Registries.SOUND_EVENT.get(id);
         return existing == null ? SoundEvent.of(id) : existing;
      }
   }

   public boolean shouldAnimate(ItemStack stack) {
      return this.isOn() && this.matchesSpear(stack);
   }

   public static boolean isSpearItem(ItemStack stack) {
      return INSTANCE != null && INSTANCE.matchesSpear(stack);
   }

   private boolean matchesSpear(ItemStack stack) {
      if (stack != null && !stack.isEmpty()) {
         if (this.itemId.getValue()) {
            if (this.isViaSpear(stack)) {
               return true;
            }

            String path = Registries.ITEM.getId(stack.getItem()).getPath().toLowerCase(Locale.ROOT);
            if (path.contains("spear")) {
               return true;
            }
         }

         if (this.itemName.getValue()) {
            String name = stack.getName().getString().toLowerCase(Locale.ROOT);
            return name.contains("spear");
         } else {
            return false;
         }
      } else {
         return false;
      }
   }

   private boolean isViaSpear(ItemStack stack) {
      if (!stack.isIn(ItemTags.SWORDS)) {
         return false;
      } else {
         Integer viaItemId = this.getOptionalViaItemId(stack);
         return viaItemId != null && viaItemId >= 1296;
      }
   }

   private Integer getOptionalViaItemId(ItemStack stack) {
      NbtComponent customData = stack.get(DataComponentTypes.CUSTOM_DATA);
      if (customData == null) {
         return null;
      } else {
         NbtCompound nbt = customData.getNbt();
         if (nbt.get("VV|original_hashes") instanceof NbtCompound originalHashes && originalHashes.get("id") instanceof NbtInt originalId) {
            return originalId.intValue();
         }

         return nbt.get("VB|Protocol1_21_11To1_21_9|id") instanceof NbtInt viaBackwardsId ? viaBackwardsId.intValue() : null;
      }
   }

   public boolean shouldAnimateFirstPerson(ItemStack stack) {
      return this.shouldAnimate(stack);
   }

   public boolean shouldAnimateThirdPerson(ItemStack stack) {
      return this.shouldAnimate(stack);
   }

   public boolean shouldSuppressEquip(ItemStack stack, float swingProgress) {
      return this.shouldAnimateFirstPerson(stack) && swingProgress > 0.0F;
   }

   public boolean shouldUseAnimate(ItemStack stack) {
      return this.shouldAnimateFirstPerson(stack);
   }

   public boolean isUsingSpear(LivingEntity entity) {
      return entity != null && entity.isUsingItem() && this.shouldAnimate(entity.getActiveItem());
   }

   public boolean shouldTransformHeldItem(ItemStack stack) {
      return this.shouldAnimateThirdPerson(stack);
   }

   public int getSwingTicks(LivingEntity entity) {
      return entity != null && this.shouldAnimate(entity.getMainHandStack()) ? Math.max(1, this.getSpearProfile(entity.getMainHandStack()).swingTicks()) : -1;
   }

   public boolean shouldFixUseMovement(LivingEntity entity) {
      return !this.grimSafe.getValue() && this.isUsingSpear(entity);
   }

   public boolean shouldAllowSprint(LivingEntity entity) {
      return !this.grimSafe.getValue() && this.isUsingSpear(entity);
   }

   public boolean shouldReplaceModel(ItemStack stack) {
      return this.shouldAnimate(stack);
   }

   public Identifier getModelId(ItemStack stack, ModelTransformationMode mode) {
      String material = this.getSpearMaterial(stack);
      boolean inHand = mode == ModelTransformationMode.FIRST_PERSON_LEFT_HAND
         || mode == ModelTransformationMode.FIRST_PERSON_RIGHT_HAND
         || mode == ModelTransformationMode.THIRD_PERSON_LEFT_HAND
         || mode == ModelTransformationMode.THIRD_PERSON_RIGHT_HAND;
      return Identifier.of("minecraft", "item/" + material + "_spear" + (inHand ? "_in_hand" : ""));
   }

   public String getSpearMaterial(ItemStack stack) {
      String target = "";
      if (stack != null && !stack.isEmpty()) {
         target = Registries.ITEM.getId(stack.getItem()).getPath() + " " + stack.getName().getString();
      }

      target = target.toLowerCase(Locale.ROOT);
      if (target.contains("wood")) {
         return "wooden";
      } else if (target.contains("stone")) {
         return "stone";
      } else if (target.contains("copper")) {
         return "copper";
      } else if (target.contains("gold")) {
         return "golden";
      } else if (target.contains("diamond")) {
         return "diamond";
      } else {
         return target.contains("netherite") ? "netherite" : "iron";
      }
   }

   public void playAttackSound(ItemStack stack, boolean hit) {
      if (this.shouldAnimate(stack)) {
         long now = System.currentTimeMillis();
         if (now - this.lastAttackSound >= 120L) {
            this.lastAttackSound = now;
            boolean wooden = this.getSpearMaterial(stack).equals("wooden");
            this.playSound(wooden ? SPEAR_WOOD_ATTACK : SPEAR_ATTACK, 1.0F);
            if (hit) {
               this.playSound(wooden ? SPEAR_WOOD_HIT : SPEAR_HIT, 1.0F);
            }
         }
      }
   }

   public void playUseSound(ItemStack stack) {
      if (this.shouldAnimate(stack)) {
         long now = System.currentTimeMillis();
         if (now - this.lastUseSound >= 250L) {
            this.lastUseSound = now;
            this.playSound(this.getSpearMaterial(stack).equals("wooden") ? SPEAR_WOOD_USE : SPEAR_USE, 1.0F);
         }
      }
   }

   private void playSound(SoundEvent sound, float pitch) {
      MinecraftClient mc = MinecraftClient.getInstance();
      if (sound != null && mc.player != null && mc.world != null) {
         mc.world.playSound(mc.player, mc.player.getX(), mc.player.getY(), mc.player.getZ(), sound, mc.player.getSoundCategory(), 1.0F, pitch);
      }
   }

   public void applyStabAnimation(MatrixStack matrices, float swingProgress, ItemStack stack) {
      float strength = 1.0F;
      float g = -(MathHelper.cos((float) Math.PI * MathHelper.clamp(MathHelper.getLerpProgress(swingProgress, 0.0F, 0.05F), 0.0F, 1.0F)) - 1.0F) / 2.0F;
      float h = MathHelper.clamp(MathHelper.getLerpProgress(swingProgress, 0.05F, 0.2F), 0.0F, 1.0F);
      h *= h;
      float j = MathHelper.clamp(MathHelper.getLerpProgress(swingProgress, 0.4F, 1.0F), 0.0F, 1.0F);
      if (j < 0.5F) {
         j = j == 0.0F ? 0.0F : (float)(Math.pow(2.0, (double)(20.0F * j - 10.0F)) / 2.0);
      } else {
         j = j == 1.0F ? 1.0F : (float)((2.0 - Math.pow(2.0, (double)(-20.0F * j + 10.0F))) / 2.0);
      }

      boolean trident = stack != null && stack.isOf(Items.TRIDENT);
      matrices.translate(strength * j * 0.1F * (g - h), strength * -0.075F * (g - j), strength * ((trident ? -0.4F * (g - j) : 0.0F) + 0.65F * (g - h)));
      matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(strength * -(trident ? 50.0F : 70.0F) * (g - j)));
      matrices.translate(0.0F, 0.0F, strength * -0.25F * (j - h));
   }

   public float getThirdPersonSwing(float swingProgress) {
      float g = -(MathHelper.cos((float) Math.PI * MathHelper.clamp(MathHelper.getLerpProgress(swingProgress, 0.0F, 0.05F), 0.0F, 1.0F)) - 1.0F) / 2.0F;
      float h = MathHelper.clamp(MathHelper.getLerpProgress(swingProgress, 0.05F, 0.2F), 0.0F, 1.0F);
      h *= h;
      float i = MathHelper.clamp(MathHelper.getLerpProgress(swingProgress, 0.4F, 1.0F), 0.0F, 1.0F);
      if (i < 0.5F) {
         i = i == 0.0F ? 0.0F : (float)(Math.pow(2.0, (double)(20.0F * i - 10.0F)) / 2.0);
      } else {
         i = i == 1.0F ? 1.0F : (float)((2.0 - Math.pow(2.0, (double)(-20.0F * i + 10.0F))) / 2.0);
      }

      return (90.0F * g - 120.0F * h + 30.0F * i) * (float) (Math.PI / 180.0);
   }

   public void applyFirstPersonUseAnimation(MatrixStack matrices, LivingEntity entity, Arm arm, ItemStack stack, float useTicks) {
      float strength = 1.0F;
      if (entity != null) {
         int side = arm == Arm.RIGHT ? 1 : -1;
         SpearFix.HoldUpAnimation animation = this.getHoldUpAnimation(useTicks, stack);
         matrices.translate((float)side * 0.56F, -0.52F, -0.72F);
         matrices.translate(
            strength
               * (float)side
               * (
                  animation.raiseProgress() * 0.15F
                     + animation.raiseProgressEnd() * -0.05F
                     + animation.swayProgress() * -0.1F
                     + animation.swayScaleSlow() * 0.005F
               ),
            strength * (animation.raiseProgress() * -0.075F + animation.raiseProgressMiddle() * 0.075F + animation.swayScaleFast() * 0.01F),
            strength * (animation.raiseProgressStart() * 0.05F + animation.raiseProgressEnd() * -0.05F + animation.swayScaleSlow() * 0.005F)
         );
         float f = animation.raiseProgress();
         if (f < 0.5F) {
            f = 4.0F * f * f * (7.189819F * f - 2.5949094F) / 2.0F;
         } else {
            float g = 2.0F * f - 2.0F;
            f = (g * g * (3.5949094F * g + 2.5949094F) + 2.0F) / 2.0F;
         }

         matrices.multiply(
            RotationAxis.POSITIVE_X
               .rotationDegrees(
                  strength
                     * (-65.0F * f - 35.0F * (1.0F - animation.lowerProgress()) + 100.0F * animation.raiseBackProgress() + -0.5F * animation.swayScaleFast())
               ),
            0.0F,
            0.1F,
            0.0F
         );
         matrices.multiply(
            RotationAxis.NEGATIVE_Y
               .rotationDegrees(
                  strength
                     * (float)side
                     * (
                        -90.0F * MathHelper.clamp(MathHelper.getLerpProgress(animation.raiseProgress(), 0.5F, 0.55F), 0.0F, 1.0F)
                           + 90.0F * animation.swayProgress()
                           + 2.0F * animation.swayScaleSlow()
                     )
               ),
            (float)side * 0.15F,
            0.0F,
            0.0F
         );
      }
   }

   public void applyThirdPersonHeldItemAnimation(MatrixStack matrices, LivingEntity entity, ItemStack stack, Arm arm) {
      if (this.shouldTransformHeldItem(stack) && entity != null) {
         float strength = 1.0F;
         float tickDelta = this.getTickDelta();
         if (entity.handSwingProgress > 0.0F && entity.getMainArm() == arm) {
            float g = 0.125F;
            float h = entity.isUsingItem() ? 1.0F : entity.getHandSwingProgress(tickDelta);
            float i = MathHelper.clamp(MathHelper.getLerpProgress(h, 0.05F, 0.2F), 0.0F, 1.0F);
            i *= i;
            float j = easeOutExpo(MathHelper.clamp(MathHelper.getLerpProgress(h, 0.4F, 1.0F), 0.0F, 1.0F));
            matrices.multiply(RotationAxis.NEGATIVE_X.rotationDegrees(strength * 70.0F * (i - j)), 0.0F, -g, g);
            matrices.translate(0.0F, strength * this.getSpearProfile(stack).forwardMovement() * (i - j), 0.0F);
         }

         if (entity.isUsingItem() && ItemStack.areEqual(entity.getActiveItem(), stack)) {
            float useTicks = Math.max(0.0F, (float)entity.getItemUseTime() + tickDelta - 1.0F);
            if (useTicks <= 0.0F) {
               return;
            }

            SpearFix.HoldUpAnimation animation = this.getHoldUpAnimation(useTicks, stack);
            int side = arm == Arm.RIGHT ? 1 : -1;
            float g = 1.0F - animation.raiseProgress() - 1.0F;
            g = 1.0F - (1.0F + 2.70158F * g * g * g + 1.70158F * MathHelper.square(g));
            matrices.translate(0.0F, 0.0F, strength * -this.getSpearProfile(stack).forwardMovement() * (g - animation.raiseBackProgress()));
            matrices.multiply(
               RotationAxis.NEGATIVE_X.rotationDegrees(strength * (animation.raiseProgress() * 70.0F - animation.raiseBackProgress() * 70.0F)),
               0.0F,
               -0.03125F,
               0.125F
            );
            matrices.multiply(
               RotationAxis.POSITIVE_Y
                  .rotationDegrees(strength * (animation.raiseProgress() * (float)side * 90.0F - animation.swayProgress() * (float)side * 90.0F)),
               0.0F,
               0.0F,
               0.125F
            );
         }
      }
   }

   public SpearFix.HoldUpAnimation getHoldUpAnimation(float ticks) {
      SpearFix.SpearProfile profile = SpearFix.SpearProfile.IRON;
      return this.getHoldUpAnimation(ticks, profile);
   }

   public SpearFix.HoldUpAnimation getHoldUpAnimation(float ticks, ItemStack stack) {
      return this.getHoldUpAnimation(ticks, this.getSpearProfile(stack));
   }

   private SpearFix.HoldUpAnimation getHoldUpAnimation(float ticks, SpearFix.SpearProfile profile) {
      float delay = profile.delayTicks();
      float dismountEnd = delay + profile.dismountTicks();
      float knockbackEnd = delay + profile.knockbackTicks();
      float damageEnd = delay + profile.damageTicks();
      float g = delay <= 0.0F ? 1.0F : MathHelper.clamp(MathHelper.getLerpProgress(ticks, 0.0F, delay), 0.0F, 1.0F);
      float h = MathHelper.clamp(MathHelper.getLerpProgress(g, 0.0F, 0.5F), 0.0F, 1.0F);
      float m = MathHelper.clamp(MathHelper.getLerpProgress(g, 0.5F, 0.8F), 0.0F, 1.0F);
      float n = MathHelper.clamp(MathHelper.getLerpProgress(g, 0.8F, 1.0F), 0.0F, 1.0F);
      float o = MathHelper.clamp(MathHelper.getLerpProgress(ticks, dismountEnd, knockbackEnd), 0.0F, 1.0F);
      float p = MathHelper.clamp(MathHelper.getLerpProgress(ticks, knockbackEnd, damageEnd - 5.0F), 0.0F, 1.0F);
      if (p > 0.0F && p < 1.0F) {
         double d = Math.sin((20.0 * (double)p - 11.125) * Math.PI * 4.0 / 9.0);
         p = p < 0.5F ? (float)(-(Math.pow(2.0, 20.0 * (double)p - 10.0) * d) / 2.0) : (float)(Math.pow(2.0, -20.0 * (double)p + 10.0) * d / 2.0 + 1.0);
      }

      p = 1.0F - p * p * p;
      float q = MathHelper.clamp(MathHelper.getLerpProgress(ticks, damageEnd - 5.0F, damageEnd), 0.0F, 1.0F);
      float r = 2.0F * (float)Math.sqrt((double)(1.0F + MathHelper.square(1.0F - o))) - 2.0F * (-((float)Math.sqrt((double)(1.0F - o * o))) + 1.0F);
      float s = MathHelper.sin(ticks * 19.0F * (float) (Math.PI / 180.0));
      float t = MathHelper.sin(ticks * 30.0F * (float) (Math.PI / 180.0));
      float u = Math.clamp((1.0F - ticks / Math.max(Math.max(dismountEnd, knockbackEnd), damageEnd)) * 20.0F, 0.0F, 1.0F) * (2.9F - r);
      return new SpearFix.HoldUpAnimation(g, h, m, n, o, p, q, r, s * u, t * u);
   }

   private SpearFix.SpearProfile getSpearProfile(ItemStack stack) {
      String var2 = this.getSpearMaterial(stack);

      return switch (var2) {
         case "wooden" -> SpearFix.SpearProfile.WOODEN;
         case "stone" -> SpearFix.SpearProfile.STONE;
         case "copper" -> SpearFix.SpearProfile.COPPER;
         case "golden" -> SpearFix.SpearProfile.GOLDEN;
         case "diamond" -> SpearFix.SpearProfile.DIAMOND;
         case "netherite" -> SpearFix.SpearProfile.NETHERITE;
         default -> SpearFix.SpearProfile.IRON;
      };
   }

   private static float easeOutExpo(float value) {
      if (value < 0.5F) {
         return value == 0.0F ? 0.0F : (float)(Math.pow(2.0, (double)(20.0F * value - 10.0F)) / 2.0);
      } else {
         return value == 1.0F ? 1.0F : (float)((2.0 - Math.pow(2.0, (double)(-20.0F * value + 10.0F))) / 2.0);
      }
   }

   public float getTickDelta() {
      return MinecraftClient.getInstance().getRenderTickCounter().getTickDelta(true);
   }

   public static record HoldUpAnimation(
      float raiseProgress,
      float raiseProgressStart,
      float raiseProgressMiddle,
      float raiseProgressEnd,
      float swayProgress,
      float lowerProgress,
      float raiseBackProgress,
      float swayIntensity,
      float swayScaleSlow,
      float swayScaleFast
   ) {
   }

   private static record SpearProfile(int swingTicks, float delayTicks, float dismountTicks, float knockbackTicks, float damageTicks, float forwardMovement) {
      private static final SpearFix.SpearProfile WOODEN = new SpearFix.SpearProfile(13, 15.0F, 100.0F, 120.0F, 300.0F, 0.38F);
      private static final SpearFix.SpearProfile STONE = new SpearFix.SpearProfile(15, 14.0F, 90.0F, 110.0F, 275.0F, 0.38F);
      private static final SpearFix.SpearProfile COPPER = new SpearFix.SpearProfile(17, 13.0F, 80.0F, 100.0F, 250.0F, 0.38F);
      private static final SpearFix.SpearProfile GOLDEN = new SpearFix.SpearProfile(19, 14.0F, 70.0F, 110.0F, 275.0F, 0.38F);
      private static final SpearFix.SpearProfile IRON = new SpearFix.SpearProfile(19, 12.0F, 50.0F, 90.0F, 225.0F, 0.38F);
      private static final SpearFix.SpearProfile DIAMOND = new SpearFix.SpearProfile(21, 10.0F, 60.0F, 80.0F, 200.0F, 0.38F);
      private static final SpearFix.SpearProfile NETHERITE = new SpearFix.SpearProfile(23, 8.0F, 50.0F, 70.0F, 175.0F, 0.38F);
   }
}
