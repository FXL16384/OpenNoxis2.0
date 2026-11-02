package noxisclient.license;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.CodeSource;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.security.PublicKey;
import java.security.Signature;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Comparator;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import net.fabricmc.loader.api.FabricLoader;
import noxisclient.Vitality;

final class IntegrityFingerprint {
   static final String SIGNATURE_RESOURCE = "META-INF/noxis-integrity.properties";
   private static final String CANONICALIZATION = "ZS-JAR-V2";
   private static final byte[] DIGEST_DOMAIN = "SlipperyBanana-Jar-Entries-V2\u0000".getBytes(StandardCharsets.US_ASCII);
   private static final int MAX_ENTRIES = 100000;
   private static final long MAX_ENTRY_BYTES = 268435456L;
   private static final long MAX_TOTAL_BYTES = 1073741824L;
   private static final Set<String> MANIFEST_KEYS = Set.of(
      "formatVersion", "buildId", "keyId", "digestAlgorithm", "signatureAlgorithm", "canonicalization", "entryCount", "hash", "signature"
   );
   private static volatile IntegrityFingerprint.Snapshot cached;

   private IntegrityFingerprint() {
   }

   static IntegrityFingerprint.Snapshot verifyBeforeLaunch() {
      IntegrityFingerprint.Snapshot snapshot = cached;
      if (snapshot != null) {
         return snapshot;
      } else {
         synchronized (IntegrityFingerprint.class) {
            if (cached != null) {
               return cached;
            } else {
               IntegrityFingerprint.Snapshot var10000;
               try {
                  ClientTrust.Config trust = ClientTrust.current();
                  if (ClientTrust.isDevelopmentMode()) {
                     Path codeSource = resolveCodeSource();
                     if (codeSource == null || !Files.isDirectory(codeSource)) {
                        throw new IOException("Development client classes directory is unavailable");
                     }

                     cached = fingerprintDevelopmentDirectory(codeSource);
                  } else {
                     Path origin = selectProductionOrigin(
                        FabricLoader.getInstance()
                           .getModContainer("noxisclient")
                           .orElseThrow(() -> new IOException("Fabric did not resolve the Noxis mod container"))
                           .getOrigin()
                           .getPaths()
                     );
                     cached = verifyJar(origin, trust.integrityPublicKey(), trust.integrityKeyId());
                  }

                  var10000 = cached;
               } catch (Exception var5) {
                  throw new SecurityException("Client integrity verification failed", var5);
               }

               return var10000;
            }
         }
      }
   }

   static IntegrityFingerprint.Snapshot current() {
      return verifyBeforeLaunch();
   }

   static IntegrityFingerprint.Snapshot verifyJar(Path jar, PublicKey publicKey, String trustedKeyId) throws Exception {
      if (publicKey != null && trustedKeyId != null && !trustedKeyId.isBlank()) {
         verifyModIdentity(jar);
         Map<String, String> manifest = readManifest(jar);
         require(manifest, "formatVersion", "2");
         require(manifest, "digestAlgorithm", "SHA-256");
         require(manifest, "signatureAlgorithm", "SHA256withRSA");
         require(manifest, "canonicalization", "ZS-JAR-V2");
         require(manifest, "keyId", trustedKeyId);
         if (!trustedKeyId.equals(ClientTrust.keyId(publicKey))) {
            throw new IOException("Trusted integrity key id does not match the public key");
         } else {
            IntegrityFingerprint.DigestResult digest = computeJarDigest(jar);
            int declaredCount = parsePositiveInt(manifest.get("entryCount"), "entryCount");
            if (declaredCount != digest.entryCount()) {
               throw new IOException("Integrity entry count mismatch");
            } else {
               String declaredHash = manifest.get("hash");
               if (declaredHash.matches("[0-9a-f]{64}")
                  && MessageDigest.isEqual(declaredHash.getBytes(StandardCharsets.US_ASCII), digest.hash().getBytes(StandardCharsets.US_ASCII))) {
                  String expectedBuildId = "zs2-" + declaredHash.substring(0, 32);
                  require(manifest, "buildId", expectedBuildId);
                  verifySignature(publicKey, signaturePayload(manifest), manifest.get("signature"));
                  return new IntegrityFingerprint.Snapshot(
                     2,
                     declaredHash,
                     jar.getFileName().toString(),
                     manifest.get("signature"),
                     manifest.get("buildId"),
                     manifest.get("keyId"),
                     "SHA-256",
                     "SHA256withRSA",
                     "ZS-JAR-V2",
                     declaredCount,
                     true
                  );
               } else {
                  throw new IOException("Client content hash mismatch");
               }
            }
         }
      } else {
         throw new IOException("Integrity trust root is missing");
      }
   }

   static Path selectProductionOrigin(List<Path> paths) throws IOException {
      if (paths != null && paths.size() == 1) {
         Path origin = paths.getFirst().toAbsolutePath().normalize();
         if (!Files.isRegularFile(origin)) {
            throw new IOException("Production mod origin is not a regular JAR file");
         } else {
            return origin.toRealPath();
         }
      } else {
         throw new IOException("Production mod origin must contain exactly one path");
      }
   }

   static IntegrityFingerprint.DigestResult computeJarDigest(Path jar) throws Exception {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");

      IntegrityFingerprint.DigestResult var24;
      try (ZipFile zip = new ZipFile(jar.toFile())) {
         List<IntegrityFingerprint.EntryData> entries = new ArrayList<>();
         Set<String> names = new HashSet<>();
         int manifestEntries = 0;

         for (ZipEntry entry : zip.stream().toList()) {
            if (!entry.isDirectory()) {
               if (!names.add(entry.getName())) {
                  throw new IOException("Duplicate JAR entry: " + entry.getName());
               }

               if ("META-INF/noxis-integrity.properties".equals(entry.getName())) {
                  manifestEntries++;
               } else {
                  byte[] name = entry.getName().getBytes(StandardCharsets.UTF_8);
                  entries.add(new IntegrityFingerprint.EntryData(entry, name));
               }
            }
         }

         if (manifestEntries != 1) {
            throw new IOException("JAR must contain exactly one integrity manifest");
         }

         if (entries.isEmpty() || entries.size() > 100000) {
            throw new IOException("JAR entry count is outside the integrity verification limit");
         }

         entries.sort((left, right) -> Arrays.compareUnsigned(left.name(), right.name()));
         digest.update(DIGEST_DOMAIN);
         updateInt(digest, entries.size());
         byte[] buffer = new byte[16384];
         long totalContentLength = 0L;

         for (IntegrityFingerprint.EntryData data : entries) {
            long contentLength = data.entry().getSize();
            if (contentLength < 0L || contentLength > 268435456L) {
               throw new IOException("JAR entry size is outside the integrity verification limit: " + data.entry().getName());
            }

            totalContentLength = Math.addExact(totalContentLength, contentLength);
            if (totalContentLength > 1073741824L) {
               throw new IOException("JAR content exceeds the integrity verification limit");
            }

            updateInt(digest, data.name().length);
            digest.update(data.name());
            updateLong(digest, contentLength);
            long actualLength = 0L;

            int read;
            try (InputStream input = zip.getInputStream(data.entry())) {
               while ((read = input.read(buffer)) != -1) {
                  digest.update(buffer, 0, read);
                  actualLength += (long)read;
               }
            }

            if (actualLength != contentLength) {
               throw new IOException("JAR entry length mismatch: " + data.entry().getName());
            }
         }

         var24 = new IntegrityFingerprint.DigestResult(HexFormat.of().formatHex(digest.digest()), entries.size());
      }

      return var24;
   }

   static byte[] signaturePayload(Map<String, String> manifest) {
      StringBuilder payload = new StringBuilder("SlipperyBanana-Integrity-V2\n");

      for (String key : List.of("formatVersion", "buildId", "keyId", "digestAlgorithm", "signatureAlgorithm", "canonicalization", "entryCount", "hash")) {
         payload.append(key).append('=').append(manifest.get(key)).append('\n');
      }

      return payload.toString().getBytes(StandardCharsets.UTF_8);
   }

   private static Map<String, String> readManifest(Path jar) throws IOException {
      Map var4;
      try (ZipFile zip = new ZipFile(jar.toFile())) {
         List<? extends ZipEntry> matches = zip.stream()
            .filter(entry -> !entry.isDirectory() && "META-INF/noxis-integrity.properties".equals(entry.getName()))
            .toList();
         if (matches.size() != 1) {
            throw new IOException("Missing or duplicate integrity manifest");
         }

         try (InputStream input = zip.getInputStream(matches.getFirst())) {
            var4 = StrictProperties.read(input, MANIFEST_KEYS);
         }
      }

      return var4;
   }

   private static void verifyModIdentity(Path jar) throws IOException {
      try (ZipFile zip = new ZipFile(jar.toFile())) {
         ZipEntry descriptor = zip.getEntry("fabric.mod.json");
         if (descriptor == null || descriptor.isDirectory() || descriptor.getSize() < 1L || descriptor.getSize() > 131072L) {
            throw new IOException("Noxis fabric.mod.json is missing or invalid");
         }

         try (InputStream input = zip.getInputStream(descriptor)) {
            byte[] content = input.readNBytes(131073);
            if (content.length > 131072) {
               throw new IOException("Noxis fabric.mod.json is too large");
            }

            JsonObject object = JsonParser.parseString(new String(content, StandardCharsets.UTF_8)).getAsJsonObject();
            if (!object.has("id") || !"noxisclient".equals(object.get("id").getAsString())) {
               throw new IOException("Unexpected Fabric mod id in production origin");
            }
         } catch (IOException var9) {
            throw var9;
         } catch (Exception var10) {
            throw new IOException("Noxis fabric.mod.json is invalid", var10);
         }
      }
   }

   private static void verifySignature(PublicKey key, byte[] payload, String encodedSignature) throws Exception {
      byte[] signatureBytes;
      try {
         signatureBytes = Base64.getDecoder().decode(encodedSignature);
         if (!Base64.getEncoder().encodeToString(signatureBytes).equals(encodedSignature)) {
            throw new IOException("Non-canonical integrity signature");
         }
      } catch (IllegalArgumentException var5) {
         throw new IOException("Invalid integrity signature encoding", var5);
      }

      Signature verifier = Signature.getInstance("SHA256withRSA");
      verifier.initVerify(key);
      verifier.update(payload);
      if (!verifier.verify(signatureBytes)) {
         throw new IOException("Integrity signature is invalid");
      }
   }

   private static IntegrityFingerprint.Snapshot fingerprintDevelopmentDirectory(Path root) throws Exception {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");

      List<Path> files;
      try (Stream<Path> stream = Files.walk(root)) {
         files = stream.filter(x$0 -> Files.isRegularFile(x$0)).sorted(Comparator.comparing(path -> root.relativize(path).toString())).toList();
      }

      for (Path file : files) {
         String relative = root.relativize(file).toString().replace('\\', '/');
         digest.update(relative.getBytes(StandardCharsets.UTF_8));
         digest.update((byte)0);

         try (InputStream input = Files.newInputStream(file)) {
            input.transferTo(new DigestOutputStream(OutputStream.nullOutputStream(), digest));
         }
      }

      String hash = HexFormat.of().formatHex(digest.digest());
      return new IntegrityFingerprint.Snapshot(
         0,
         hash,
         root.getFileName().toString(),
         "DEVELOPMENT",
         "dev-" + hash.substring(0, 32),
         "DEVELOPMENT",
         "SHA-256",
         "DEVELOPMENT",
         "DEVELOPMENT",
         files.size(),
         false
      );
   }

   private static Path resolveCodeSource() throws Exception {
      CodeSource codeSource = Vitality.class.getProtectionDomain().getCodeSource();
      if (codeSource == null) {
         return null;
      } else {
         URL location = codeSource.getLocation();
         if (location == null) {
            return null;
         } else {
            URI uri = location.toURI();
            return Paths.get(uri).toAbsolutePath().normalize();
         }
      }
   }

   private static void require(Map<String, String> manifest, String key, String expected) throws IOException {
      if (!expected.equals(manifest.get(key))) {
         throw new IOException("Unexpected integrity " + key);
      }
   }

   private static int parsePositiveInt(String value, String name) throws IOException {
      if (value != null && value.matches("[1-9][0-9]{0,8}")) {
         return Integer.parseInt(value);
      } else {
         throw new IOException("Invalid integrity " + name);
      }
   }

   private static void updateInt(MessageDigest digest, int value) {
      digest.update(ByteBuffer.allocate(4).putInt(value).array());
   }

   private static void updateLong(MessageDigest digest, long value) {
      digest.update(ByteBuffer.allocate(8).putLong(value).array());
   }

   static record DigestResult(String hash, int entryCount) {
   }

   private static record EntryData(ZipEntry entry, byte[] name) {
   }

   static record Snapshot(
      int formatVersion,
      String hash,
      String source,
      String signature,
      String buildId,
      String keyId,
      String digestAlgorithm,
      String signatureAlgorithm,
      String canonicalization,
      int entryCount,
      boolean verified
   ) {
   }
}
