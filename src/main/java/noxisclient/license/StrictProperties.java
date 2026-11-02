package noxisclient.license;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

final class StrictProperties {
   private static final int MAX_BYTES = 65536;

   private StrictProperties() {
   }

   static Map<String, String> read(InputStream input, Set<String> expectedKeys) throws IOException {
      byte[] bytes = input.readNBytes(65537);
      if (bytes.length > 65536) {
         throw new IOException("Properties resource is too large");
      } else {
         for (byte value : bytes) {
            int unsigned = value & 255;
            if (unsigned > 127 || unsigned < 32 && unsigned != 10 && unsigned != 13 && unsigned != 9) {
               throw new IOException("Properties resource must be plain ASCII text");
            }
         }

         Map<String, String> values = new LinkedHashMap<>();
         String text = new String(bytes, StandardCharsets.US_ASCII);

         for (String rawLine : text.split("\\r?\\n", -1)) {
            if (!rawLine.isBlank()) {
               if (!rawLine.equals(rawLine.trim()) || rawLine.startsWith("#") || rawLine.startsWith("!")) {
                  throw new IOException("Unsupported properties syntax");
               }

               int separator = rawLine.indexOf(61);
               if (separator <= 0) {
                  throw new IOException("Malformed properties entry");
               }

               String key = rawLine.substring(0, separator);
               String valuex = rawLine.substring(separator + 1);
               if (!expectedKeys.contains(key)) {
                  throw new IOException("Unknown properties key: " + key);
               }

               if (valuex.isEmpty()) {
                  throw new IOException("Empty properties value: " + key);
               }

               if (values.putIfAbsent(key, valuex) != null) {
                  throw new IOException("Duplicate properties key: " + key);
               }
            }
         }

         if (!values.keySet().equals(expectedKeys)) {
            Set<String> missing = new LinkedHashSet<>(expectedKeys);
            missing.removeAll(values.keySet());
            throw new IOException("Missing properties keys: " + missing);
         } else {
            return Map.copyOf(values);
         }
      }
   }
}
