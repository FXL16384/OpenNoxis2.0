package noxisclient.license;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.PublicKey;
import java.security.interfaces.RSAKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Locale;

final class ClientTrust {
   private static volatile ClientTrust.Config cached;

   private ClientTrust() {
   }

   static ClientTrust.Config current() {
      ClientTrust.Config config = cached;
      if (config != null) {
         return config;
      } else {
         synchronized (ClientTrust.class) {
            if (cached == null) {
               try {
                  cached = loadGeneratedTrust();
               } catch (Exception var4) {
                  throw new SecurityException("Client trust roots are unavailable", var4);
               }
            }

            return cached;
         }
      }
   }

   static URI apiOrigin() {
      ClientTrust.Config config = current();
      boolean requestedDevMode = Boolean.getBoolean("noxis.license.devMode");
      if (!requestedDevMode) {
         return config.productionOrigin();
      } else if (!config.developmentBuild()) {
         throw new SecurityException("Development mode is not available in a production build");
      } else {
         String value = System.getProperty("noxis.license.devServer", "").trim();
         if (value.isEmpty()) {
            throw new SecurityException("Development server was not configured");
         } else {
            URI uri = validateOrigin(value);
            if (!isLoopback(uri.getHost())) {
               throw new SecurityException("Development server must use a loopback address");
            } else {
               return uri;
            }
         }
      }
   }

   static boolean isDevelopmentMode() {
      return current().developmentBuild() && Boolean.getBoolean("noxis.license.devMode");
   }

   static boolean isLocalTestBuild() {
      return false;
   }

   static boolean isLicenseVerificationEnabled() {
      return true;
   }

   static boolean isIntegrityVerificationEnabled() {
      return false;
   }

   private static ClientTrust.Config loadGeneratedTrust() throws Exception {
      boolean developmentBuild = false;
      boolean integrityVerification = isIntegrityVerificationEnabled();
      boolean licenseVerification = isLicenseVerificationEnabled();
      PublicKey integrityKey = integrityVerification ? decodeKey("RSA", "UNAVAILABLE", developmentBuild) : null;
      PublicKey licenseKey = licenseVerification
         ? decodeKey("Ed25519", "MCowBQYDK2VwAyEAR20ViYKzl75Y4WfMQcoliVNeGGjuzfSYUhS+oVa1apY=", developmentBuild)
         : null;
      if (integrityVerification && (!developmentBuild || integrityKey != null)) {
         requireKeyId(integrityKey, "UNAVAILABLE", "integrity");
      }

      if (licenseVerification && (!developmentBuild || licenseKey != null)) {
         requireKeyId(licenseKey, "sha256:cb6f45ff88ddd95b8bdacb57da7f030cc0ab6446b2b5999193d1a15eb441ef90", "license");
      }

      byte[] originKeyMaterial = licenseKey == null ? "Noxis-Development-Origin-Key-v1".getBytes(StandardCharsets.US_ASCII) : licenseKey.getEncoded();
      URI origin = decodeProtectedOrigin(
         "KPNMSWD4s3-izi-D3-IVAT9UJeXpmFvWQ6hZ8kl5", "8d1b0887e7a5ba637a1163a8db14de1f25318f00a74ac78e0bd1a73f3f1f859e", originKeyMaterial
      );
      return new ClientTrust.Config(
         developmentBuild, origin, "UNAVAILABLE", integrityKey, "sha256:cb6f45ff88ddd95b8bdacb57da7f030cc0ab6446b2b5999193d1a15eb441ef90", licenseKey
      );
   }

   private static PublicKey decodeKey(String algorithm, String encoded, boolean allowMissing) throws Exception {
      if ("UNAVAILABLE".equals(encoded)) {
         if (allowMissing) {
            return null;
         } else {
            throw new IllegalStateException("Missing production public key");
         }
      } else {
         byte[] der = Base64.getDecoder().decode(encoded);
         PublicKey key = KeyFactory.getInstance(algorithm).generatePublic(new X509EncodedKeySpec(der));
         if (!"RSA".equals(algorithm) || key instanceof RSAKey rsaKey && rsaKey.getModulus().bitLength() >= 2048) {
            return key;
         } else {
            throw new IllegalStateException("Integrity RSA public key is too small");
         }
      }
   }

   private static void requireKeyId(PublicKey key, String configured, String description) throws Exception {
      if (key == null) {
         throw new IllegalStateException("Missing " + description + " public key");
      } else {
         String actual = keyId(key);
         if (!MessageDigest.isEqual(actual.getBytes(StandardCharsets.US_ASCII), configured.getBytes(StandardCharsets.US_ASCII))) {
            throw new IllegalStateException("Incorrect " + description + " key id");
         }
      }
   }

   static String keyId(PublicKey key) throws Exception {
      byte[] digest = MessageDigest.getInstance("SHA-256").digest(key.getEncoded());
      return "sha256:" + HexFormat.of().formatHex(digest);
   }

   static URI decodeProtectedOrigin(String payload, String expectedSha256, byte[] keyMaterial) throws Exception {
      if (payload != null && !payload.isBlank() && expectedSha256 != null && !expectedSha256.isBlank() && keyMaterial != null && keyMaterial.length != 0) {
         byte[] encrypted = Base64.getUrlDecoder().decode(payload);
         if (encrypted.length >= 8 && encrypted.length <= 512) {
            MessageDigest maskDigest = MessageDigest.getInstance("SHA-256");
            maskDigest.update("Noxis-Origin-Envelope-v1".getBytes(StandardCharsets.US_ASCII));
            maskDigest.update(keyMaterial);
            byte[] mask = maskDigest.digest();
            byte[] originBytes = new byte[encrypted.length];

            for (int index = 0; index < encrypted.length; index++) {
               originBytes[index] = (byte)(encrypted[index] ^ mask[index % mask.length]);
            }

            byte[] expected = HexFormat.of().parseHex(expectedSha256);
            byte[] actual = MessageDigest.getInstance("SHA-256").digest(originBytes);
            if (!MessageDigest.isEqual(expected, actual)) {
               throw new SecurityException("License origin integrity check failed");
            } else {
               return validateOrigin(new String(originBytes, StandardCharsets.UTF_8));
            }
         } else {
            throw new SecurityException("License origin protection has an invalid length");
         }
      } else {
         throw new SecurityException("License origin protection is incomplete");
      }
   }

   private static URI validateOrigin(String value) {
      URI uri;
      try {
         uri = URI.create(value).normalize();
      } catch (Exception var4) {
         throw new SecurityException("Invalid license origin", var4);
      }

      String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase(Locale.ROOT);
      if (!"http".equals(scheme) && !"https".equals(scheme)) {
         throw new SecurityException("License origin must use HTTP or HTTPS");
      } else if (uri.getHost() != null
         && !uri.getHost().isBlank()
         && uri.getUserInfo() == null
         && uri.getQuery() == null
         && uri.getFragment() == null
         && (uri.getPath() == null || uri.getPath().isEmpty() || "/".equals(uri.getPath()))) {
         String canonical = scheme + "://" + uri.getHost().toLowerCase(Locale.ROOT) + (uri.getPort() == -1 ? "" : ":" + uri.getPort());
         return URI.create(canonical);
      } else {
         throw new SecurityException("License origin must not contain credentials, path, query, or fragment");
      }
   }

   private static boolean isLoopback(String host) {
      if (host == null) {
         return false;
      } else {
         String normalized = host.toLowerCase(Locale.ROOT);
         return "localhost".equals(normalized) || "127.0.0.1".equals(normalized) || "::1".equals(normalized) || "[::1]".equals(normalized);
      }
   }

   static record Config(
      boolean developmentBuild, URI productionOrigin, String integrityKeyId, PublicKey integrityPublicKey, String licenseKeyId, PublicKey licensePublicKey
   ) {
   }
}
