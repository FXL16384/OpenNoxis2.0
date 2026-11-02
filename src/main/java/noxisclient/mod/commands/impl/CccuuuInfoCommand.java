package noxisclient.mod.commands.impl;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import noxisclient.Alien;
import noxisclient.mod.commands.Command;

public class CccuuuInfoCommand extends Command {
   private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(ZoneOffset.UTC);
   private static final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneOffset.UTC);
   private static final String API_URL = "https://3c3u.cc/api/getPlayer?name=";

   public CccuuuInfoCommand() {
      super("cccuuuinfo", "<玩家ID>");
   }

   @Override
   public void runCommand(String[] parameters) {
      if (parameters.length == 1 && !parameters[0].trim().isEmpty()) {
         String playerName = parameters[0].trim();
         this.sendChatMessage("§e正在搜索中...");
         Alien.THREAD.execute(() -> this.queryPlayer(playerName));
      } else {
         this.sendUsage();
      }
   }

   private void queryPlayer(String playerName) {
      try {
         String encodedName = URLEncoder.encode(playerName, StandardCharsets.UTF_8);
         HttpURLConnection connection = (HttpURLConnection)URI.create("https://3c3u.cc/api/getPlayer?name=" + encodedName).toURL().openConnection();
         connection.setRequestMethod("GET");
         connection.setConnectTimeout(5000);
         connection.setReadTimeout(8000);
         connection.setRequestProperty("Accept", "application/json");
         connection.setRequestProperty("User-Agent", "NoxisClient");
         if (connection.getResponseCode() != 200) {
            this.sendNotFound();
            return;
         }

         StringBuilder response = new StringBuilder();
         BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8));

         String line;
         try {
            while ((line = reader.readLine()) != null) {
               response.append(line);
            }
         } catch (Throwable var9) {
            try {
               reader.close();
            } catch (Throwable var8) {
               var9.addSuppressed(var8);
            }

            throw var9;
         }

         reader.close();
         JsonObject root = JsonParser.parseString(response.toString()).getAsJsonObject();
         if (!root.has("code") || root.get("code").getAsInt() != 200 || !root.has("data") || !root.get("data").isJsonObject()) {
            this.sendNotFound();
            return;
         }

         JsonObject data = root.getAsJsonObject("data");
         this.sendPlayerInfo(data);
      } catch (Exception var10) {
         this.sendNotFound();
      }
   }

   private void sendPlayerInfo(JsonObject data) {
      long lastLoginTime = this.getLong(data, "lastLoginTime");
      long lastLogoutTime = this.getLong(data, "lastLogoutTime");
      boolean offline = lastLogoutTime >= lastLoginTime;
      String logoutStatus = offline ? " §8（已离线" : " §a（在线）";
      this.sendChatMessage("§6玩家§r：§a" + this.getString(data, "name"));
      this.sendChatMessage("§6UUID§r：§f" + this.getString(data, "uuid"));
      this.sendChatMessage("§6首次加入§r：§f" + this.formatDate(this.getLong(data, "firstLoginTime")) + " §7左右");
      this.sendChatMessage("§6最近上线§r：§b" + this.formatDateTime(lastLoginTime));
      this.sendChatMessage("§6最近下线§r：§b" + this.formatDateTime(lastLogoutTime) + logoutStatus);
      this.sendChatMessage("§6总在线时长§r：§e" + this.formatHours(this.getLong(data, "duration")) + " §7小时");
      this.sendChatMessage("§6近一个月在线§r：§e" + this.formatHours(this.getLong(data, "month_duration")) + " §7小时");
      this.sendChatMessage("§6击杀数§r：§c" + this.getInt(data, "c_kill"));
      this.sendChatMessage("§6死亡数§r：§c" + this.getInt(data, "c_death"));
      this.sendChatMessage("§6发言数§r：§d" + this.getInt(data, "c_chat"));
   }

   private void sendNotFound() {
      this.sendChatMessage("§c玩家不存");
   }

   private String formatDate(long epochSeconds) {
      return DATE_FORMAT.format(Instant.ofEpochSecond(epochSeconds));
   }

   private String formatDateTime(long epochSeconds) {
      return DATE_TIME_FORMAT.format(Instant.ofEpochSecond(epochSeconds));
   }

   private String formatHours(long seconds) {
      return String.format(Locale.ROOT, "%.1f", (double)seconds / 3600.0);
   }

   private String getString(JsonObject data, String memberName) {
      return data.has(memberName) && !data.get(memberName).isJsonNull() ? data.get(memberName).getAsString() : "";
   }

   private long getLong(JsonObject data, String memberName) {
      return data.has(memberName) && !data.get(memberName).isJsonNull() ? data.get(memberName).getAsLong() : 0L;
   }

   private int getInt(JsonObject data, String memberName) {
      return data.has(memberName) && !data.get(memberName).isJsonNull() ? data.get(memberName).getAsInt() : 0;
   }

   @Override
   public String[] getAutocorrect(int count, List<String> seperated) {
      return null;
   }
}
