package noxisclient.license;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.PublicKey;
import java.security.Signature;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;

final class GateTokenVerifier {
   private static final long CLOCK_SKEW_SECONDS = 30L;
   private static final int MAX_TOKEN_LENGTH = 49152;
   private final PublicKey publicKey;
   private final String keyId;

   GateTokenVerifier(PublicKey publicKey, String keyId) {
      if (publicKey != null && keyId != null && !keyId.isBlank()) {
         this.publicKey = publicKey;
         this.keyId = keyId;
      } else {
         throw new IllegalArgumentException("Missing gate trust root");
      }
   }

   JsonObject signedResponse(JsonObject envelope, String path, String requestNonce) throws IOException {
      GateTokenVerifier.Claims claims = this.verify(tokenField(envelope, "responseToken"), "noxis-response", 120L);
      requireEqual(text(claims.body(), "path"), path, "response path");
      requireEqual(text(claims.body(), "requestNonce"), requestNonce, "response nonce");
      JsonElement body = claims.body().get("body");
      if (body != null && body.isJsonObject()) {
         return body.getAsJsonObject().deepCopy();
      } else {
         throw new IOException("Signed response body is missing");
      }
   }

   GateTokenVerifier.Lease verifyCapability(String token, String key, String hwid, String buildId, String integrityHash, String clientNonce, String session) throws IOException {
      GateTokenVerifier.Claims claims = this.verify(token, "noxis-capability", 900L);
      JsonObject body = claims.body();
      requireEqual(text(body, "licenseHash"), sha256(key), "license binding");
      requireEqual(text(body, "hwidHash"), sha256(hwid), "device binding");
      requireEqual(text(body, "buildId"), buildId, "build binding");
      requireEqual(text(body, "integrityHash"), integrityHash, "integrity binding");
      requireEqual(text(body, "clientNonce"), clientNonce, "client binding");
      requireEqual(text(body, "session"), session, "session binding");
      return new GateTokenVerifier.Lease(token, claims.expiresAt());
   }

   GateTokenVerifier.Claims verify(String token, String expectedType, long maximumLifetimeSeconds) throws IOException {
      if (token != null && !token.isBlank() && token.length() <= 49152 && token.indexOf(61) < 0) {
         int separator = token.indexOf(46);
         if (separator > 0 && separator == token.lastIndexOf(46) && separator != token.length() - 1) {
            String payloadPart = token.substring(0, separator);
            byte[] payloadBytes = canonicalDecode(payloadPart, 24576);
            byte[] signature = canonicalDecode(token.substring(separator + 1), 256);

            try {
               Signature verifier = Signature.getInstance("Ed25519");
               verifier.initVerify(this.publicKey);
               verifier.update(payloadPart.getBytes(StandardCharsets.US_ASCII));
               if (!verifier.verify(signature)) {
                  throw new IOException("Authorization token signature is invalid");
               }
            } catch (IOException var17) {
               throw var17;
            } catch (Exception var18) {
               throw new IOException("Authorization token verification failed", var18);
            }

            JsonObject body;
            try {
               JsonElement parsed = JsonParser.parseString(new String(payloadBytes, StandardCharsets.UTF_8));
               if (!parsed.isJsonObject()) {
                  throw new IOException("Authorization token payload is invalid");
               }

               body = parsed.getAsJsonObject();
            } catch (IOException var19) {
               throw var19;
            } catch (Exception var20) {
               throw new IOException("Authorization token payload is invalid", var20);
            }

            if (integer(body, "v") != 1L) {
               throw new IOException("Unsupported authorization token version");
            } else {
               requireEqual(text(body, "typ"), expectedType, "token type");
               requireEqual(text(body, "kid"), this.keyId, "token key");
               String tokenId = text(body, "jti");
               if (!tokenId.matches("[A-Za-z0-9_-]{16,256}")) {
                  throw new IOException("Authorization token id is invalid");
               } else {
                  long issuedAt = integer(body, "iat");
                  long expiresAt = integer(body, "exp");
                  long now = Instant.now().getEpochSecond();
                  if (issuedAt <= now + 30L && expiresAt > now - 30L && expiresAt > issuedAt && expiresAt - issuedAt <= maximumLifetimeSeconds) {
                     return new GateTokenVerifier.Claims(body, expiresAt);
                  } else {
                     throw new IOException("Authorization token is expired");
                  }
               }
            }
         } else {
            throw new IOException("Malformed authorization token");
         }
      } else {
         throw new IOException("Malformed authorization token");
      }
   }

   static String text(JsonObject object, String field) throws IOException {
      if ((object == null ? null : object.get(field)) instanceof JsonPrimitive primitive && primitive.isString()) {
         String text = primitive.getAsString();
         if (!text.isBlank() && text.length() <= 16384) {
            return text;
         }

         throw new IOException("Invalid signed field: " + field);
      }

      throw new IOException("Missing signed field: " + field);
   }

   private static String tokenField(JsonObject object, String field) throws IOException {
      return text(object, field);
   }

   private static long integer(JsonObject object, String field) throws IOException {
      if ((object == null ? null : object.get(field)) instanceof JsonPrimitive primitive && primitive.isNumber()) {
         String raw = primitive.getAsString();
         if (!raw.matches("-?(0|[1-9][0-9]*)")) {
            throw new IOException("Invalid signed number: " + field);
         }

         try {
            return Long.parseLong(raw);
         } catch (NumberFormatException var6) {
            throw new IOException("Signed number is out of range: " + field, var6);
         }
      }

      throw new IOException("Missing signed number: " + field);
   }

   private static byte[] canonicalDecode(String value, int maximumLength) throws IOException {
      try {
         byte[] decoded = Base64.getUrlDecoder().decode(value);
         if (decoded.length <= maximumLength && Base64.getUrlEncoder().withoutPadding().encodeToString(decoded).equals(value)) {
            return decoded;
         } else {
            throw new IOException("Invalid token encoding");
         }
      } catch (IllegalArgumentException var3) {
         throw new IOException("Invalid token encoding", var3);
      }
   }

   private static void requireEqual(String actual, String expected, String field) throws IOException {
      if (expected == null || !MessageDigest.isEqual(actual.getBytes(StandardCharsets.UTF_8), expected.getBytes(StandardCharsets.UTF_8))) {
         throw new IOException("Authorization " + field + " mismatch");
      }
   }

   private static String sha256(String value) throws IOException {
      try {
         return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.trim().getBytes(StandardCharsets.UTF_8)));
      } catch (Exception var2) {
         throw new IOException("SHA-256 is unavailable", var2);
      }
   }

   static record Claims(JsonObject body, long expiresAt) {
   }

   static record Lease(String token, long expiresAt) {
   }
}
