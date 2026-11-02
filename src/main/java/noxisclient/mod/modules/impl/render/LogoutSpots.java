package noxisclient.mod.modules.impl.render;

import com.google.common.collect.Maps;
import com.mojang.blaze3d.systems.RenderSystem;
import java.awt.Color;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicBoolean;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer.TextLayerType;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.VertexConsumerProvider.Immediate;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.s2c.play.PlayerListS2CPacket;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Direction.Axis;
import noxisclient.Alien;
import noxisclient.api.events.impl.PacketEvent;
import noxisclient.api.events.impl.RemoveEntityEvent;
import noxisclient.api.events.impl.UpdateEvent;
import noxisclient.api.utils.render.ModelPlayer;
import noxisclient.api.utils.render.Render3DUtil;
import noxisclient.mod.modules.Module;
import noxisclient.mod.modules.impl.client.TextRadar;
import noxisclient.mod.modules.settings.impl.BooleanSetting;
import noxisclient.mod.modules.settings.impl.ColorSetting;
import noxisclient.mod.modules.settings.impl.EnumSetting;
import noxisclient.mod.modules.settings.impl.SliderSetting;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;

public class LogoutSpots extends Module {
   private static boolean renderingTexturedModel;
   private static final Map<LogoutSpots.MioSound, SoundEvent> SOUND_EVENTS = Maps.newEnumMap(LogoutSpots.MioSound.class);
   private final SliderSetting distance = this.add(new SliderSetting("Distance", 260.0, 16.0, 260.0, 1.0).setSuffix("m"));
   private final EnumSetting<LogoutSpots.ModelMode> model = this.add(new EnumSetting<>("Model", LogoutSpots.ModelMode.BOTH));
   private final BooleanSetting textured = this.add(new BooleanSetting("Textured", false, this::rendersComplexModel));
   private final SliderSetting alpha = this.add(new SliderSetting("Alpha", 1.0, 0.0, 1.0, 0.01, this::rendersComplexModel));
   private final SliderSetting width = this.add(new SliderSetting("Width", 1.5, 0.1, 5.0, 0.1, this::rendersModel));
   private final BooleanSetting tracer = this.add(new BooleanSetting("Tracer", false));
   private final SliderSetting tracerWidth = this.add(new SliderSetting("Width", 1.0, 0.1, 5.0, 0.1, this.tracer::getValue));
   private final BooleanSetting nameTag = this.add(new BooleanSetting("NameTag", true).setParent());
   private final SliderSetting textScale = this.add(new SliderSetting("TextScale", 1.0, 0.5, 3.0, 0.1, this.nameTag::isOpen));
   private final EnumSetting<LogoutSpots.PositionMode> position = this.add(
      new EnumSetting<>("Position", LogoutSpots.PositionMode.COORDINATES, this.nameTag::isOpen)
   );
   private final BooleanSetting time = this.add(new BooleanSetting("Time", true, this.nameTag::isOpen));
   private final BooleanSetting health = this.add(new BooleanSetting("Health", true, this.nameTag::isOpen));
   private final BooleanSetting totems = this.add(new BooleanSetting("Totems", true, this.nameTag::isOpen));
   private final BooleanSetting sounds = this.add(new BooleanSetting("Sounds", true).setParent());
   private final BooleanSetting login = this.add(new BooleanSetting("Login", true, this.sounds::isOpen).setParent());
   private final EnumSetting<LogoutSpots.MioSound> loginSoundPath = this.add(
      new EnumSetting<>("LoginSoundPath", LogoutSpots.MioSound.DOUBLEKILL, this.login::isOpen)
   );
   private final BooleanSetting logout = this.add(new BooleanSetting("Logout", true, this.sounds::isOpen).setParent());
   private final EnumSetting<LogoutSpots.MioSound> logoutSoundPath = this.add(
      new EnumSetting<>("LogoutSoundPath", LogoutSpots.MioSound.DOMINATING, this.logout::isOpen)
   );
   private final BooleanSetting colors = this.add(new BooleanSetting("Colors", true).setParent());
   private final ColorSetting modelFill = this.add(new ColorSetting("ModelFill", new Color(255, 255, 255, 90), this.colors::isOpen).injectBoolean(true));
   private final ColorSetting modelLine = this.add(new ColorSetting("ModelLine", new Color(255, 255, 255, 150), this.colors::isOpen).injectBoolean(true));
   private final ColorSetting boxFill = this.add(new ColorSetting("BoxFill", new Color(255, 255, 255, 60), this.colors::isOpen).injectBoolean(true));
   private final ColorSetting boxLine = this.add(new ColorSetting("BoxLine", new Color(255, 255, 255, 180), this.colors::isOpen).injectBoolean(true));
   private final ColorSetting nameTagColor = this.add(new ColorSetting("NameTagColor", new Color(255, 255, 255, 255), this.colors::isOpen));
   private final ColorSetting fillColor = this.add(new ColorSetting("FillColor", new Color(0, 0, 0, 80), this.colors::isOpen));
   private final BooleanSetting filter = this.add(new BooleanSetting("Filter", false).setParent());
   private final BooleanSetting ignoreFriends = this.add(new BooleanSetting("IgnoreFriends", false, this.filter::isOpen));
   private final BooleanSetting ignoreNakeds = this.add(new BooleanSetting("IgnoreNakeds", false, this.filter::isOpen));
   private final Map<String, LogoutSpots.LogoutSpot> logoutSpots = Maps.newConcurrentMap();
   private final List<LogoutSpots.PendingLogout> pendingLogouts = new ArrayList<>();

   public LogoutSpots() {
      super("LogoutSpots", "Highlights log out spots.", Module.Category.Render);
      this.setChinese("退出记录");
      this.drawn.setValue(false);
   }

   @Override
   public void onToggle() {
      this.logoutSpots.clear();
   }

   @Override
   public void onDisable() {
      this.clear();
   }

   @Override
   public void onLogin() {
      this.clear();
   }

   @Override
   public void onLogout() {
      this.clear();
   }

   public void onPacketReceive(PacketEvent.Receive event) {
      if (!nullCheck() && event.getPacket() instanceof PlayerListS2CPacket packet) {
         for (net.minecraft.network.packet.s2c.play.PlayerListS2CPacket.Entry entry : packet.getPlayerAdditionEntries()) {
            mc.execute(() -> {
               if (entry.profile() != null) {
                  String name = entry.profile().getName();
                  if (this.sounds.getValue() && this.login.getValue() && this.logoutSpots.containsKey(name)) {
                     this.playSound(this.loginSoundPath.getValue());
                  }

                  this.logoutSpots.remove(name);
               }
            });
         }
      }
   }

   public void onRemoveEntity(RemoveEntityEvent event) {
      if (!nullCheck() && event.getEntity() instanceof PlayerEntity player && player != mc.player) {
         if (this.canLog(player)) {
            synchronized (this.pendingLogouts) {
               this.pendingLogouts.add(new LogoutSpots.PendingLogout(player));
            }
         }
      }
   }

   public void onUpdate(UpdateEvent event) {
      if (nullCheck()) {
         this.clear();
      } else {
         synchronized (this.pendingLogouts) {
            Iterator<LogoutSpots.PendingLogout> iterator = this.pendingLogouts.iterator();

            while (iterator.hasNext()) {
               LogoutSpots.PendingLogout pending = iterator.next();
               PlayerEntity player = pending.player();
               if (pending.isExpired()) {
                  iterator.remove();
               } else if (pending.marked().get()) {
                  iterator.remove();
               } else if (this.isLoggedOut(player)) {
                  pending.marked().set(true);
                  this.addLogoutSpot(player);
                  iterator.remove();
               }
            }
         }
      }
   }

   @Override
   public void onRender3D(MatrixStack matrixStack) {
      if (!nullCheck()) {
         Vec3d cameraPos = mc.gameRenderer.getCamera().getPos();

         for (Entry<String, LogoutSpots.LogoutSpot> entry : this.logoutSpots.entrySet()) {
            LogoutSpots.LogoutSpot spot = entry.getValue();
            if (spot.worldId().equals(this.worldId()) && this.inRange(spot, cameraPos)) {
               if (this.model.getValue() == LogoutSpots.ModelMode.SIMPLE || this.model.getValue() == LogoutSpots.ModelMode.BOTH) {
                  if (this.boxFill.booleanValue) {
                     Render3DUtil.drawFill(matrixStack, spot.box(), this.boxFill.getValue());
                  }

                  if (this.boxLine.booleanValue) {
                     Render3DUtil.drawBox(matrixStack, spot.box(), this.boxLine.getValue(), this.width.getValueFloat());
                  }
               }

               if ((this.model.getValue() == LogoutSpots.ModelMode.COMPLEX || this.model.getValue() == LogoutSpots.ModelMode.BOTH)
                  && (this.modelFill.booleanValue || this.modelLine.booleanValue)) {
                  renderingTexturedModel = this.textured.getValue();
                  spot.model()
                     .render(matrixStack, this.modelFill, this.modelLine, this.alpha.getValue(), 0.0, 1.0, 0.0, false, false, this.textured.getValue());
                  renderingTexturedModel = false;
               }

               if (this.tracer.getValue()) {
                  Vec3d start = this.getTracerStart();
                  Vec3d end = spot.box().getCenter();
                  Render3DUtil.drawLine(start.x, start.y, start.z, end.x, end.y, end.z, this.modelLine.getValue(), this.tracerWidth.getValueFloat());
               }

               if (this.nameTag.getValue()) {
                  Vec3d pos = spot.box().getCenter().withAxis(Axis.Y, spot.box().maxY + 0.5);
                  this.renderNameTag(this.getNameTag(entry.getKey(), spot), pos);
               }
            }
         }
      }
   }

   private void renderNameTag(String text, Vec3d pos) {
      Camera camera = mc.gameRenderer.getCamera();
      Vec3d cameraPos = camera.getPos();
      MatrixStack matrices = new MatrixStack();
      matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(camera.getPitch()));
      matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(camera.getYaw() + 180.0F));
      matrices.translate(pos.x - cameraPos.x, pos.y - cameraPos.y, pos.z - cameraPos.z);
      matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-camera.getYaw()));
      matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(camera.getPitch()));
      float scale = -0.025F * this.textScale.getValueFloat();
      matrices.scale(scale, scale, -1.0F);
      RenderSystem.enableBlend();
      GL11.glDepthFunc(519);
      float halfWidth = (float)mc.textRenderer.getWidth(text) / 2.0F;
      this.drawNameTagText(matrices, text, -halfWidth, 0.0F, this.nameTagColor.getValue().getRGB());
      GL11.glDepthFunc(515);
      RenderSystem.disableBlend();
   }

   private void drawNameTagText(MatrixStack matrices, String text, float x, float y, int color) {
      Immediate immediate = mc.getBufferBuilders().getEntityVertexConsumers();
      Matrix4f matrix = matrices.peek().getPositionMatrix();
      float shadowX = x + 0.5F;
      float shadowY = y + 0.5F;
      int currentColor = color;
      StringBuilder segment = new StringBuilder();

      for (int i = 0; i < text.length(); i++) {
         char c = text.charAt(i);
         if (c == 167 && i + 1 < text.length()) {
            char code = text.charAt(i + 1);
            Formatting formatting = Formatting.byCode(code);
            if (formatting != null && formatting.isColor()) {
               if (!segment.isEmpty()) {
                  int shadowColor = (currentColor & 16579836) >> 2 | currentColor & 0xFF000000;
                  mc.textRenderer.draw(segment.toString(), shadowX, shadowY, shadowColor, false, matrix, immediate, TextLayerType.SEE_THROUGH, 0, 15728880);
                  shadowX += (float)mc.textRenderer.getWidth(segment.toString());
                  segment = new StringBuilder();
               }

               Integer rgb = formatting.getColorValue();
               if (rgb != null) {
                  currentColor = 0xFF000000 | rgb;
               }
            } else if (formatting == Formatting.RESET) {
               if (!segment.isEmpty()) {
                  int shadowColor = (currentColor & 16579836) >> 2 | currentColor & 0xFF000000;
                  mc.textRenderer.draw(segment.toString(), shadowX, shadowY, shadowColor, false, matrix, immediate, TextLayerType.SEE_THROUGH, 0, 15728880);
                  shadowX += (float)mc.textRenderer.getWidth(segment.toString());
                  segment = new StringBuilder();
               }

               currentColor = color;
            } else {
               segment.append(c);
               segment.append(code);
            }

            i++;
         } else {
            segment.append(c);
         }
      }

      if (!segment.isEmpty()) {
         int shadowColor = (currentColor & 16579836) >> 2 | currentColor & 0xFF000000;
         mc.textRenderer.draw(segment.toString(), shadowX, shadowY, shadowColor, false, matrix, immediate, TextLayerType.SEE_THROUGH, 0, 15728880);
      }

      immediate.draw();
      mc.textRenderer.draw(text, x, y, color, false, matrix, immediate, TextLayerType.SEE_THROUGH, 0, 15728880);
      immediate.draw();
   }

   private void addLogoutSpot(PlayerEntity player) {
      if (this.canLog(player)) {
         String name = player.getGameProfile().getName();
         if (!this.logoutSpots.containsKey(name)) {
            LogoutSpots.LogoutSpot spot = new LogoutSpots.LogoutSpot(
               System.currentTimeMillis(),
               player.getBoundingBox(),
               Math.round(player.getHealth() + player.getAbsorptionAmount()),
               Alien.POP.getPop(player),
               player.getPos(),
               new ModelPlayer(player),
               this.worldId()
            );
            this.logoutSpots.put(name, spot);
            if (this.sounds.getValue() && this.logout.getValue()) {
               this.playSound(this.logoutSoundPath.getValue());
            }
         }
      }
   }

   private String getNameTag(String name, LogoutSpots.LogoutSpot spot) {
      StringBuilder builder = new StringBuilder();
      if (this.position.getValue() == LogoutSpots.PositionMode.COORDINATES) {
         builder.append(String.format(Locale.US, "X: %.1f, Y: %.1f, Z: %.1f", spot.pos().x, spot.pos().y, spot.pos().z));
      }

      if (this.position.getValue() == LogoutSpots.PositionMode.DISTANCE) {
         builder.append(String.format(Locale.US, "%.1fm", mc.player.getPos().distanceTo(spot.pos())));
      }

      if (this.time.getValue()) {
         this.appendSpace(builder);
         builder.append(new SimpleDateFormat("HH:mm").format(new Date(spot.time())));
      }

      if (this.health.getValue()) {
         this.appendSpace(builder);
         builder.append(this.getHealthColor(spot.health())).append(spot.health()).append("hp");
      }

      if (spot.totems() > 0 && this.totems.getValue()) {
         this.appendSpace(builder);
         builder.append(TextRadar.getPopColor(spot.totems())).append("-").append(spot.totems());
      }

      if (!builder.isEmpty()) {
         builder.append(" ");
      }

      builder.append(name).append(" logout spot");
      return builder.toString();
   }

   private void appendSpace(StringBuilder builder) {
      if (!builder.isEmpty()) {
         builder.append(" ");
      }
   }

   private boolean canLog(PlayerEntity player) {
      if (player == null || player == mc.player || player.deathTime > 0 || player.getHealth() + player.getAbsorptionAmount() <= 0.0F) {
         return false;
      } else {
         return this.ignoreFriends.getValue() && Alien.FRIEND.isFriend(player) ? false : !this.ignoreNakeds.getValue() || this.hasArmor(player);
      }
   }

   private boolean hasArmor(LivingEntity entity) {
      for (ItemStack stack : entity.getArmorItems()) {
         if (!stack.isEmpty()) {
            return true;
         }
      }

      return false;
   }

   private boolean isLoggedOut(PlayerEntity player) {
      if (mc.player != null && mc.player.networkHandler != null && player != null) {
         String name = player.getGameProfile().getName();
         return mc.player.networkHandler.getPlayerList().stream().noneMatch(entry -> entry.getProfile().getName().equals(name));
      } else {
         return false;
      }
   }

   private boolean inRange(LogoutSpots.LogoutSpot spot, Vec3d cameraPos) {
      return this.distance.getValue() >= this.distance.getMax() || spot.box().getCenter().distanceTo(cameraPos) <= this.distance.getValue();
   }

   private Vec3d getTracerStart() {
      return mc.gameRenderer
         .getCamera()
         .getPos()
         .add(
            Vec3d.fromPolar(mc.player.getPitch(mc.getRenderTickCounter().getTickDelta(true)), mc.player.getYaw(mc.getRenderTickCounter().getTickDelta(true)))
               .multiply(0.2)
         );
   }

   private String worldId() {
      if (mc.world == null) {
         return "";
      } else {
         return !mc.isInSingleplayer() && mc.getCurrentServerEntry() != null ? mc.getCurrentServerEntry().address : "singleplayer";
      }
   }

   private Formatting getHealthColor(int health) {
      if (health > 18) {
         return Formatting.GREEN;
      } else if (health > 16) {
         return Formatting.DARK_GREEN;
      } else if (health > 12) {
         return Formatting.YELLOW;
      } else if (health > 8) {
         return Formatting.GOLD;
      } else {
         return health > 4 ? Formatting.RED : Formatting.DARK_RED;
      }
   }

   private boolean rendersModel() {
      return this.model.getValue() != LogoutSpots.ModelMode.NONE;
   }

   private boolean rendersComplexModel() {
      return this.model.getValue() == LogoutSpots.ModelMode.COMPLEX || this.model.getValue() == LogoutSpots.ModelMode.BOTH;
   }

   public static void registerSounds() {
      for (LogoutSpots.MioSound sound : LogoutSpots.MioSound.values()) {
         SOUND_EVENTS.put(sound, registerSound(sound.id()));
      }
   }

   private static SoundEvent registerSound(Identifier id) {
      try {
         return Registry.register(Registries.SOUND_EVENT, id, SoundEvent.of(id));
      } catch (RuntimeException var3) {
         SoundEvent existing = Registries.SOUND_EVENT.get(id);
         return existing == null ? SoundEvent.of(id) : existing;
      }
   }

   public static void playMioSound(LogoutSpots.MioSound sound) {
      if (sound != null && MinecraftClient.getInstance().getSoundManager() != null) {
         SoundEvent event = SOUND_EVENTS.get(sound);
         if (event == null) {
            event = registerSound(sound.id());
            SOUND_EVENTS.put(sound, event);
         }

         if (event != null) {
            MinecraftClient.getInstance().getSoundManager().play(PositionedSoundInstance.master(Registries.SOUND_EVENT.getEntry(event), 1.0F));
         }
      }
   }

   private void playSound(LogoutSpots.MioSound sound) {
      playMioSound(sound);
   }

   private void clear() {
      this.logoutSpots.clear();
      synchronized (this.pendingLogouts) {
         this.pendingLogouts.clear();
      }
   }

   public static boolean method_708() {
      return renderingTexturedModel;
   }

   private static record LogoutSpot(long time, Box box, int health, int totems, Vec3d pos, ModelPlayer model, String worldId) {
   }

   public static enum MioSound {
      KABAN("kaban"),
      NEVERLOSE("neverlose"),
      CSS("css"),
      COD("cod"),
      QUAKE("quake"),
      TOOLBOX("toolbox"),
      WARNING("warning"),
      STEAM("steam"),
      WHATSAPP("whatsapp"),
      VK("vk"),
      ICQ("icq"),
      STALKER("stalker"),
      HOVER("hover"),
      CLICK("click"),
      RCLICK("rclick"),
      BODYSPLAT("bodysplat"),
      DOMINATING("dominating"),
      DOUBLEKILL("doublekill"),
      GODLIKE("godlike"),
      HEADHUNTER("headhunter"),
      HUMILIATION("humiliation"),
      IMPRESSIVE("impressive"),
      KILLINGSPREE("killingspree"),
      LUDICROUSKILL("ludicrouskill"),
      MEGAKILL("megakill"),
      MONSTERKILL("monsterkill"),
      MULTIKILL("multikill"),
      PERFECT("perfect"),
      RAMPAGE("rampage"),
      TRIPLEKILL("triplekill"),
      ULTRAKILL("ultrakill"),
      UNSTOPPABLE("unstoppable"),
      WICKEDSICK("wickedsick");

      private final String name;

      private MioSound(String name) {
         this.name = name;
      }

      private Identifier id() {
         return Identifier.of("noxisclient", "mio." + this.name);
      }

      @Override
      public String toString() {
         return this.name;
      }
   }

   private static enum ModelMode {
      NONE,
      SIMPLE,
      COMPLEX,
      BOTH;
   }

   private static record PendingLogout(PlayerEntity player, long time, AtomicBoolean marked) {
      private PendingLogout(PlayerEntity player) {
         this(player, System.currentTimeMillis(), new AtomicBoolean());
      }

      private boolean isExpired() {
         return System.currentTimeMillis() - this.time >= 300L;
      }
   }

   private static enum PositionMode {
      NONE,
      COORDINATES,
      DISTANCE;
   }
}
