package noxisclient.license;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpClient.Redirect;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.spec.MGF1ParameterSpec;
import java.security.spec.X509EncodedKeySpec;
import java.time.Duration;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.OAEPParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.crypto.spec.PSource.PSpecified;

final class GateChannel {
   private static final Gson JSON = new Gson();
   private static final SecureRandom RANDOM = new SecureRandom();
   private static final int MAX_RESPONSE_BYTES = 1048576;
   private static final int AES_KEY_BYTES = 32;
   private static final int GCM_IV_BYTES = 12;
   private final URI origin;
   private final HttpClient http;
   private final GateTokenVerifier tokens;
   private volatile GateChannel.CryptoSession cryptoSession;

   GateChannel() {
      ClientTrust.Config trust = ClientTrust.current();
      this.origin = ClientTrust.apiOrigin();
      this.tokens = new GateTokenVerifier(trust.licensePublicKey(), trust.licenseKeyId());
      this.http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(8L)).followRedirects(Redirect.NEVER).build();
   }

   GateChannel.Lease authorize(String key, String hwid, GateChannel.BuildBinding build) throws IOException, InterruptedException {
      try {
         return this.authorizeOnce(key, hwid, build);
      } catch (GateChannel.CryptoSessionExpired var5) {
         this.invalidateCryptoSession();
         return this.authorizeOnce(key, hwid, build);
      }
   }

   GateChannel.Lease pulse(GateChannel.Lease lease, GateChannel.BuildBinding build) throws IOException, InterruptedException {
      try {
         return this.pulseOnce(lease, build);
      } catch (GateChannel.CryptoSessionExpired var4) {
         this.invalidateCryptoSession();
         return this.pulseOnce(lease, build);
      }
   }

   private GateChannel.Lease authorizeOnce(String key, String hwid, GateChannel.BuildBinding build) throws IOException, InterruptedException {
      String clientNonce = randomNonce();
      JsonObject challengeRequest = new JsonObject();
      challengeRequest.addProperty("clientNonce", clientNonce);
      JsonObject challenge = this.postGate("/api/gate/challenge", challengeRequest);
      String challengeToken = required(challenge, "challengeToken");
      GateTokenVerifier.Claims challengeClaims = this.tokens.verify(challengeToken, "noxis-challenge", 300L);
      requireEqual(GateTokenVerifier.text(challengeClaims.body(), "clientNonce"), clientNonce, "challenge nonce");
      String requestNonce = randomNonce();
      JsonObject request = this.payload(hwid, build);
      request.addProperty("key", key);
      request.addProperty("clientNonce", clientNonce);
      request.addProperty("challengeToken", challengeToken);
      request.addProperty("requestNonce", requestNonce);
      JsonObject response = this.tokens.signedResponse(this.postGate("/api/gate/authorize", request), "/api/gate/authorize", requestNonce);
      if (!"OK".equals(optional(response, "status"))) {
         throw new GateChannel.GateRejected(optional(response, "message", "Authorization rejected"));
      } else {
         String session = required(response, "session");
         GateTokenVerifier.Lease capability = this.tokens
            .verifyCapability(required(response, "capabilityToken"), key, hwid, build.buildId(), build.integrityHash(), clientNonce, session);
         return new GateChannel.Lease(key, hwid, session, clientNonce, capability.token(), capability.expiresAt());
      }
   }

   private GateChannel.Lease pulseOnce(GateChannel.Lease lease, GateChannel.BuildBinding build) throws IOException, InterruptedException {
      String requestNonce = randomNonce();
      JsonObject request = this.payload(lease.hwid(), build);
      request.addProperty("key", lease.key());
      request.addProperty("session", lease.session());
      request.addProperty("clientNonce", lease.clientNonce());
      request.addProperty("capabilityToken", lease.capabilityToken());
      request.addProperty("requestNonce", requestNonce);
      JsonObject response = this.tokens.signedResponse(this.postGate("/api/gate/pulse", request), "/api/gate/pulse", requestNonce);
      if (!"OK".equals(optional(response, "status"))) {
         throw new GateChannel.GateRejected(optional(response, "message", "Authorization rejected"));
      } else {
         GateTokenVerifier.Lease capability = this.tokens
            .verifyCapability(
               required(response, "capabilityToken"), lease.key(), lease.hwid(), build.buildId(), build.integrityHash(), lease.clientNonce(), lease.session()
            );
         return new GateChannel.Lease(lease.key(), lease.hwid(), lease.session(), lease.clientNonce(), capability.token(), capability.expiresAt());
      }
   }

   private JsonObject payload(String hwid, GateChannel.BuildBinding build) {
      JsonObject request = new JsonObject();
      request.addProperty("hwid", hwid);
      request.addProperty("version", "2.0");
      request.addProperty("minecraftVersion", "1.21.1");
      request.addProperty("timestamp", System.currentTimeMillis());
      if (build.integrity() != null) {
         IntegrityFingerprint.Snapshot integrity = build.integrity();
         request.addProperty("formatVersion", integrity.formatVersion());
         request.addProperty("buildId", integrity.buildId());
         request.addProperty("keyId", integrity.keyId());
         request.addProperty("digestAlgorithm", integrity.digestAlgorithm());
         request.addProperty("signatureAlgorithm", integrity.signatureAlgorithm());
         request.addProperty("canonicalization", integrity.canonicalization());
         request.addProperty("entryCount", integrity.entryCount());
         request.addProperty("hash", integrity.hash());
         request.addProperty("signature", integrity.signature());
      } else {
         request.addProperty("integrityMode", "disabled");
      }

      return request;
   }

   private JsonObject postGate(String path, JsonObject body) throws IOException, InterruptedException {
      GateChannel.CryptoSession session = this.cryptoSession();
      GateChannel.CryptoSession.Request encrypted = session.encrypt("POST", path, JSON.toJson((JsonElement)body));
      JsonObject response = this.send(
         HttpRequest.newBuilder(this.origin.resolve(path))
            .timeout(Duration.ofSeconds(15L))
            .header("Accept", "application/json")
            .header("Content-Type", "application/json")
            .header("X-Crypto-Client", session.clientId())
            .header("X-Timestamp", encrypted.timestamp())
            .header("X-Nonce", encrypted.nonce())
            .header("X-Signature", encrypted.signature())
            .POST(BodyPublishers.ofString(JSON.toJson((JsonElement)encrypted.envelope()), StandardCharsets.UTF_8))
            .build()
      );
      if (!isEnvelope(response)) {
         throw new GateChannel.CryptoSessionExpired();
      } else {
         return session.decrypt(response);
      }
   }

   private GateChannel.CryptoSession cryptoSession() throws IOException, InterruptedException {
      GateChannel.CryptoSession current = this.cryptoSession;
      if (current != null) {
         return current;
      } else {
         synchronized (this) {
            if (this.cryptoSession == null) {
               this.cryptoSession = this.exchangeCryptoSession();
            }

            return this.cryptoSession;
         }
      }
   }

   private GateChannel.CryptoSession exchangeCryptoSession() throws IOException, InterruptedException {
      JsonObject descriptor = this.get("/api/crypto/public-key");
      GateTokenVerifier.Claims claims = this.tokens.verify(required(descriptor, "keyToken"), "noxis-crypto-key", 300L);
      JsonObject keyClaims = claims.body();
      requireEqual(GateTokenVerifier.text(keyClaims, "keyAlgorithm"), "RSA-3072", "crypto key algorithm");
      requireEqual(GateTokenVerifier.text(keyClaims, "encryptionAlgorithm"), "RSA-OAEP-256", "crypto encryption algorithm");
      byte[] aesKey = new byte[32];
      RANDOM.nextBytes(aesKey);
      String exchangeNonce = randomNonce();
      JsonObject exchange = new JsonObject();
      exchange.addProperty("encryptedKey", this.encryptAesKey(GateTokenVerifier.text(keyClaims, "publicKey"), aesKey));
      exchange.addProperty("clientNonce", exchangeNonce);
      JsonObject response = this.tokens.signedResponse(this.postPlain("/api/crypto/exchange", exchange), "/api/crypto/exchange", exchangeNonce);
      requireEqual(required(response, "clientNonce"), exchangeNonce, "crypto exchange nonce");
      return new GateChannel.CryptoSession(required(response, "clientId"), aesKey);
   }

   private String encryptAesKey(String encodedKey, byte[] aesKey) throws IOException {
      try {
         PublicKey key = KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(Base64.getDecoder().decode(encodedKey)));
         Cipher cipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding");
         cipher.init(1, key, new OAEPParameterSpec("SHA-256", "MGF1", MGF1ParameterSpec.SHA256, PSpecified.DEFAULT));
         return Base64.getEncoder().encodeToString(cipher.doFinal(aesKey));
      } catch (Exception var5) {
         throw new IOException("Authorization server crypto key is invalid", var5);
      }
   }

   private JsonObject get(String path) throws IOException, InterruptedException {
      return this.send(HttpRequest.newBuilder(this.origin.resolve(path)).timeout(Duration.ofSeconds(15L)).header("Accept", "application/json").GET().build());
   }

   private JsonObject postPlain(String path, JsonObject body) throws IOException, InterruptedException {
      return this.send(
         HttpRequest.newBuilder(this.origin.resolve(path))
            .timeout(Duration.ofSeconds(15L))
            .header("Accept", "application/json")
            .header("Content-Type", "application/json")
            .POST(BodyPublishers.ofString(JSON.toJson((JsonElement)body), StandardCharsets.UTF_8))
            .build()
      );
   }

   private JsonObject send(HttpRequest request) throws IOException, InterruptedException {
      HttpResponse<String> response = this.http.send(request, BodyHandlers.ofString(StandardCharsets.UTF_8));
      String contentType = response.headers().firstValue("Content-Type").orElse("").toLowerCase();
      if (contentType.startsWith("application/json") && response.body().getBytes(StandardCharsets.UTF_8).length <= 1048576) {
         try {
            JsonElement parsed = JsonParser.parseString(response.body());
            if (!parsed.isJsonObject()) {
               throw new IOException("Authorization server response is invalid");
            } else {
               return parsed.getAsJsonObject();
            }
         } catch (IOException var5) {
            throw var5;
         } catch (Exception var6) {
            throw new IOException("Authorization server response is invalid", var6);
         }
      } else {
         throw new IOException("Authorization server response is invalid");
      }
   }

   private void invalidateCryptoSession() {
      synchronized (this) {
         this.cryptoSession = null;
      }
   }

   private static boolean isEnvelope(JsonObject object) {
      return !optional(object, "payload").isBlank() && !optional(object, "iv").isBlank();
   }

   private static String required(JsonObject object, String field) throws IOException {
      String value = optional(object, field);
      if (value.isBlank()) {
         throw new IOException("Authorization response is incomplete");
      } else {
         return value;
      }
   }

   private static String optional(JsonObject object, String field) {
      try {
         return object != null && object.has(field) && object.get(field).isJsonPrimitive() ? object.get(field).getAsString().trim() : "";
      } catch (Exception var3) {
         return "";
      }
   }

   private static String optional(JsonObject object, String field, String fallback) {
      String value = optional(object, field);
      return value.isBlank() ? fallback : value;
   }

   private static void requireEqual(String actual, String expected, String field) throws IOException {
      if (!MessageDigest.isEqual(actual.getBytes(StandardCharsets.UTF_8), expected.getBytes(StandardCharsets.UTF_8))) {
         throw new IOException("Authorization " + field + " mismatch");
      }
   }

   private static String randomNonce() {
      byte[] bytes = new byte[32];
      RANDOM.nextBytes(bytes);
      return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
   }

   static record BuildBinding(String buildId, String integrityHash, IntegrityFingerprint.Snapshot integrity) {
      BuildBinding(String buildId, String integrityHash, IntegrityFingerprint.Snapshot integrity) {
         if (buildId != null && !buildId.isBlank() && integrityHash != null && !integrityHash.isBlank()) {
            this.buildId = buildId;
            this.integrityHash = integrityHash;
            this.integrity = integrity;
         } else {
            throw new IllegalArgumentException("Build binding is incomplete");
         }
      }
   }

   private static record CryptoSession(String clientId, byte[] key) {
      private CryptoSession(String clientId, byte[] key) {
         if (clientId != null && !clientId.isBlank() && key != null && key.length == 32) {
            key = (byte[])key.clone();
            this.clientId = clientId;
            this.key = key;
         } else {
            throw new IllegalArgumentException("Invalid crypto session");
         }
      }

      GateChannel.CryptoSession.Request encrypt(String method, String path, String plainJson) throws IOException {
         try {
            byte[] iv = new byte[12];
            GateChannel.RANDOM.nextBytes(iv);
            Cipher aes = Cipher.getInstance("AES/GCM/NoPadding");
            aes.init(1, new SecretKeySpec(this.key, "AES"), new GCMParameterSpec(128, iv));
            String payload = Base64.getEncoder().encodeToString(aes.doFinal(plainJson.getBytes(StandardCharsets.UTF_8)));
            String encodedIv = Base64.getEncoder().encodeToString(iv);
            String timestamp = Long.toString(System.currentTimeMillis());
            String nonce = GateChannel.randomNonce();
            String signature = hmac(this.key, method + "\n" + path + "\n" + timestamp + "\n" + nonce + "\n" + payload + "\n" + encodedIv);
            JsonObject envelope = new JsonObject();
            envelope.addProperty("payload", payload);
            envelope.addProperty("iv", encodedIv);
            return new GateChannel.CryptoSession.Request(envelope, timestamp, nonce, signature);
         } catch (Exception var12) {
            throw new IOException("Authorization request encryption failed", var12);
         }
      }

      JsonObject decrypt(JsonObject envelope) throws IOException {
         try {
            byte[] iv = Base64.getDecoder().decode(GateChannel.required(envelope, "iv"));
            if (iv.length != 12) {
               throw new IOException("Authorization response is invalid");
            } else {
               Cipher aes = Cipher.getInstance("AES/GCM/NoPadding");
               aes.init(2, new SecretKeySpec(this.key, "AES"), new GCMParameterSpec(128, iv));
               byte[] plain = aes.doFinal(Base64.getDecoder().decode(GateChannel.required(envelope, "payload")));
               JsonElement parsed = JsonParser.parseString(new String(plain, StandardCharsets.UTF_8));
               if (!parsed.isJsonObject()) {
                  throw new IOException("Authorization response is invalid");
               } else {
                  return parsed.getAsJsonObject();
               }
            }
         } catch (IOException var6) {
            throw var6;
         } catch (Exception var7) {
            throw new GateChannel.CryptoSessionExpired();
         }
      }

      private static String hmac(byte[] key, String value) throws Exception {
         Mac mac = Mac.getInstance("HmacSHA256");
         mac.init(new SecretKeySpec(key, "HmacSHA256"));
         return Base64.getEncoder().encodeToString(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));
      }

      private static record Request(JsonObject envelope, String timestamp, String nonce, String signature) {
      }
   }

   private static final class CryptoSessionExpired extends IOException {
      private CryptoSessionExpired() {
         super("Authorization secure channel needs renewal");
      }
   }

   static final class GateRejected extends IOException {
      GateRejected(String message) {
         super(message != null && !message.isBlank() ? message : "Authorization rejected");
      }
   }

   static record Lease(String key, String hwid, String session, String clientNonce, String capabilityToken, long expiresAt) {
   }
}
