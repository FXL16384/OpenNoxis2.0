package noxisclient.license;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import net.fabricmc.loader.api.FabricLoader;

public final class GateRuntime {
   private static final int MAX_CONSECUTIVE_PULSE_FAILURES = 3;
   private static final int HEARTBEAT_INTERVAL_SECONDS = 60;
   private static final GateChannel.BuildBinding LICENSE_ONLY_BUILD = new GateChannel.BuildBinding("license-only", "license-only", null);
   private static final AtomicReference<GateChannel.Lease> ACTIVE = new AtomicReference<>();
   private static final AtomicInteger PULSE_FAILURES = new AtomicInteger();
   private static final ScheduledExecutorService WATCHDOG = Executors.newSingleThreadScheduledExecutor(runnable -> {
      Thread thread = new Thread(runnable, "Noxis-Gate-Watchdog");
      thread.setDaemon(true);
      return thread;
   });
   private static volatile boolean armed;

   private GateRuntime() {
   }

   public static void bootstrap() {
   }

   public static void requireActive() {
   }

   public static synchronized void arm() {
   }

   static boolean isActiveForTests() {
      GateChannel.Lease lease = ACTIVE.get();
      return lease != null && lease.expiresAt() > Instant.now().getEpochSecond();
   }

   private static void pulse() {
      try {
         requireActive();
         GateChannel.Lease refreshed = channel().pulse(ACTIVE.get(), buildBinding());
         ACTIVE.set(refreshed);
         PULSE_FAILURES.set(0);
      } catch (Exception var1) {
         if (PULSE_FAILURES.incrementAndGet() >= 3) {
            ACTIVE.set(null);
            Runtime.getRuntime().halt(23);
         }
      }
   }

   private static GateChannel channel() {
      return GateRuntime.GateChannelHolder.INSTANCE;
   }

   private static GateChannel.BuildBinding buildBinding() {
      if (!ClientTrust.isIntegrityVerificationEnabled()) {
         return LICENSE_ONLY_BUILD;
      } else {
         IntegrityFingerprint.Snapshot integrity = IntegrityFingerprint.current();
         return new GateChannel.BuildBinding(integrity.buildId(), integrity.hash(), integrity);
      }
   }

   private static Optional<String> readSavedKey() {
      Path path = activationFile();

      try {
         if (!Files.isRegularFile(path)) {
            return Optional.empty();
         } else {
            String encoded = Files.readString(path, StandardCharsets.US_ASCII).trim();
            byte[] decoded = Base64.getUrlDecoder().decode(encoded);
            String key = new String(decoded, StandardCharsets.UTF_8).trim();
            return !key.isBlank() && key.length() <= 1024 ? Optional.of(key) : Optional.empty();
         }
      } catch (Exception var4) {
         return Optional.empty();
      }
   }

   private static void saveKey(String key) {
      try {
         Path path = activationFile();
         Files.createDirectories(path.getParent());
         Files.writeString(path, Base64.getUrlEncoder().withoutPadding().encodeToString(key.getBytes(StandardCharsets.UTF_8)), StandardCharsets.US_ASCII);
      } catch (IOException var2) {
      }
   }

   private static Path activationFile() {
      return FabricLoader.getInstance().getGameDir().resolve("config").resolve("noxis").resolve("gate.dat");
   }

   private static String deviceId() {
      String source = System.getProperty("os.name", "")
         + "|"
         + System.getProperty("os.arch", "")
         + "|"
         + System.getProperty("user.name", "")
         + "|"
         + System.getenv().getOrDefault("COMPUTERNAME", "")
         + "|"
         + System.getenv().getOrDefault("PROCESSOR_IDENTIFIER", "");

      try {
         return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(source.getBytes(StandardCharsets.UTF_8)));
      } catch (Exception var2) {
         throw new SecurityException("Device identity is unavailable", var2);
      }
   }

   private static String message(Exception error) {
      return error instanceof GateChannel.GateRejected && error.getMessage() != null
         ? error.getMessage()
         : "Authorization server is unavailable or rejected this client.";
   }

   private static final class GateChannelHolder {
      private static final GateChannel INSTANCE = new GateChannel();
   }
}
