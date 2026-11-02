package noxisclient.mod.modules.impl.misc;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse.BodyHandlers;
import java.time.Duration;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.network.packet.s2c.play.ChatMessageS2CPacket;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import noxisclient.Alien;
import noxisclient.api.events.impl.PacketEvent;
import noxisclient.mod.modules.Module;
import noxisclient.mod.modules.settings.impl.BooleanSetting;
import noxisclient.mod.modules.settings.impl.SliderSetting;
import noxisclient.mod.modules.settings.impl.StringSetting;

public abstract class AiChatFeature extends Module {
   private final BooleanSetting aiChat = this.add(new BooleanSetting("AiChat", false).setParent().injectTask(this::onAiChatSettingChanged));
   private final StringSetting apiKey = this.add(new StringSetting("ApiKey", "", this.aiChat::isOpen));
   private final StringSetting apiUrl = this.add(new StringSetting("ApiUrl", "https://api.deepseek.com/chat/completions", this.aiChat::isOpen));
   private final StringSetting model = this.add(new StringSetting("Model", "deepseek-chat", this.aiChat::isOpen));
   private final SliderSetting temperature = this.add(new SliderSetting("Temperature", 0.7, 0.0, 2.0, 0.1, this.aiChat::isOpen));
   private final SliderSetting maxTokens = this.add(new SliderSetting("MaxTokens", 100.0, 10.0, 500.0, 10.0, this.aiChat::isOpen));
   private final SliderSetting connectionTimeout = this.add(new SliderSetting("ConnectionTimeout", 30.0, 5.0, 120.0, 5.0, this.aiChat::isOpen));
   private final SliderSetting maxRetries = this.add(new SliderSetting("MaxRetries", 3.0, 0.0, 5.0, 1.0, this.aiChat::isOpen));
   private final BooleanSetting useTargetList = this.add(new BooleanSetting("TargetList", false, this.aiChat::isOpen));
   private final StringSetting targetPlayerIds = this.add(new StringSetting("TargetPlayers", "", () -> this.aiChat.isOpen() && this.useTargetList.getValue()));
   private final BooleanSetting enableAutoReply = this.add(new BooleanSetting("AutoReply", true, this.aiChat::isOpen));
   private final BooleanSetting onlyReplyFriends = this.add(new BooleanSetting("OnlyFriends", true, this.aiChat::isOpen));
   private final BooleanSetting debugMode = this.add(new BooleanSetting("Debug", false, this.aiChat::isOpen));
   private final StringSetting systemPrompt = this.add(new StringSetting("SystemPrompt", "你是一个Minecraft玩家，请用简短中文回复聊天内容。只输出回复内容，不要动作描写。", this.aiChat::isOpen));
   private final Map<String, String> conversationHistory = new HashMap<>();
   private final HttpClient httpClient = HttpClient.newHttpClient();

   protected AiChatFeature(String name) {
      super(name, Module.Category.Misc);
   }

   private void onAiChatSettingChanged() {
      this.conversationHistory.clear();
      if (this.isOn() && this.aiChat.getValue()) {
         this.testAPIConnection();
      }
   }

   @Override
   public void onEnable() {
      this.conversationHistory.clear();
      if (this.aiChat.getValue()) {
         this.sendDebugMessage("AIChat已启用");
         this.testAPIConnection();
      }
   }

   @Override
   public void onDisable() {
      this.conversationHistory.clear();
      this.sendDebugMessage("AIChat已禁");
   }

   public void onPacketReceive(PacketEvent.Receive event) {
      if (this.aiChat.getValue() && this.enableAutoReply.getValue() && !event.isCancelled()) {
         String playerName = null;
         String playerMessage = null;
         if (event.getPacket() instanceof ChatMessageS2CPacket) {
            ChatMessageS2CPacket packet = (ChatMessageS2CPacket)event.getPacket();
            UUID senderUuid = packet.sender();
            if (senderUuid.equals(mc.player.getUuid())) {
               if (this.debugMode.getValue()) {
                  this.sendDebugMessage("跳过自己的消息(UUID匹配)");
               }

               return;
            }

            playerName = mc.getNetworkHandler() != null && mc.getNetworkHandler().getPlayerListEntry(senderUuid) != null
               ? mc.getNetworkHandler().getPlayerListEntry(senderUuid).getProfile().getName()
               : "Unknown";
            playerMessage = packet.body().content();
            if (this.debugMode.getValue()) {
               this.sendDebugMessage("收到聊天消息 (UUID: " + senderUuid + "): " + playerName + ": " + playerMessage);
            }
         } else {
            if (!(event.getPacket() instanceof GameMessageS2CPacket)) {
               return;
            }

            GameMessageS2CPacket packetx = (GameMessageS2CPacket)event.getPacket();
            String fullMessage = packetx.content().getString();
            String cleanMessage = Formatting.strip(fullMessage);
            if (this.debugMode.getValue()) {
               this.sendDebugMessage("收到游戏消息: " + cleanMessage);
            }

            if (!this.isOtherPlayerMessage(fullMessage)) {
               if (this.debugMode.getValue()) {
                  this.sendDebugMessage("不是其他玩家消息，跳");
               }

               return;
            }

            playerName = this.extractPlayerName(cleanMessage);
            playerMessage = this.extractMessageContent(cleanMessage);
         }

         if (playerName != null && playerMessage != null) {
            if (playerMessage.startsWith("/") || playerMessage.startsWith("!") || playerMessage.startsWith("$") || playerMessage.startsWith("#")) {
               if (this.debugMode.getValue()) {
                  this.sendDebugMessage("跳过命令消息");
               }

               return;
            }

            if (this.useTargetList.getValue() && !this.isTargetPlayer(playerName)) {
               if (this.debugMode.getValue()) {
                  this.sendDebugMessage("不在目标列表中，跳过: " + playerName);
               }

               return;
            }

            if (this.onlyReplyFriends.getValue() && !this.isFriend(playerName)) {
               if (this.debugMode.getValue()) {
                  this.sendDebugMessage("不是好友，跳过: " + playerName);
               }

               return;
            }

            if (this.debugMode.getValue()) {
               this.sendDebugMessage("开始为 " + playerName + " 调用AI API...");
            }

            this.callAIAsync(playerName, playerMessage);
         }
      }
   }

   private boolean isOtherPlayerMessage(String message) {
      if (message != null && !message.isEmpty() && mc.player != null) {
         String cleanMessage = Formatting.strip(message);
         if (cleanMessage != null && !cleanMessage.isEmpty()) {
            String myName = mc.player.getName().getString();
            String[] systemKeywords = new String[]{
               "[Server]",
               "SYSTEM",
               "公告",
               "提示",
               "TIP",
               "Welcome",
               "欢迎",
               "加入了游",
               "离开了游",
               "joined the game",
               "left the game",
               "[ViaVersion]",
               "There is a newer plugin version available"
            };

            for (String keyword : systemKeywords) {
               if (cleanMessage.contains(keyword)) {
                  return false;
               }
            }

            String potentialName = this.extractPlayerName(cleanMessage);
            return potentialName != null && !potentialName.equals("Unknown") && !potentialName.equalsIgnoreCase(myName)
               ? this.isRealPlayer(potentialName)
               : false;
         } else {
            return false;
         }
      } else {
         return false;
      }
   }

   private boolean isRealPlayer(String name) {
      if (mc.getNetworkHandler() == null) {
         return true;
      } else {
         for (PlayerListEntry entry : mc.getNetworkHandler().getPlayerList()) {
            if (entry.getProfile().getName().equalsIgnoreCase(name)) {
               return true;
            }
         }

         return false;
      }
   }

   private String extractPlayerName(String message) {
      if (message.length() > 3) {
         char first = message.charAt(0);
         char[] openingBrackets = new char[]{'<', '[', '('};
         char[] closingBrackets = new char[]{'>', ']', ')'};

         for (int i = 0; i < openingBrackets.length; i++) {
            if (first == openingBrackets[i]) {
               int closingIdx = message.indexOf(closingBrackets[i] + " ", 1);
               if (closingIdx != -1) {
                  String senderPart = message.substring(1, closingIdx).trim();
                  return this.cleanSenderName(senderPart);
               }
            }
         }
      }

      String senderPart = "";
      if (message.contains(": ")) {
         senderPart = message.substring(0, message.indexOf(": ")).trim();
      } else if (message.contains(" > ")) {
         senderPart = message.substring(0, message.indexOf(" > ")).trim();
      }

      if (!senderPart.isEmpty()) {
         return this.cleanSenderName(senderPart);
      } else {
         String[] words = message.split(" ");
         return words.length > 1 ? this.cleanSenderName(words[0]) : "Unknown";
      }
   }

   private String cleanSenderName(String senderPart) {
      if (senderPart.contains(" ")) {
         String[] parts = senderPart.split(" ");
         return parts[parts.length - 1].replaceAll("[^a-zA-Z0-9_]", "");
      } else {
         return senderPart.replaceAll("[^a-zA-Z0-9_]", "");
      }
   }

   private String extractMessageContent(String message) {
      if (message.length() > 3) {
         char first = message.charAt(0);
         char[] openingBrackets = new char[]{'<', '[', '('};
         char[] closingBrackets = new char[]{'>', ']', ')'};

         for (int i = 0; i < openingBrackets.length; i++) {
            if (first == openingBrackets[i]) {
               int closingIdx = message.indexOf(closingBrackets[i] + " ", 1);
               if (closingIdx != -1) {
                  return message.substring(closingIdx + 2).trim();
               }
            }
         }
      }

      if (message.contains(": ")) {
         return message.substring(message.indexOf(": ") + 2).trim();
      } else if (message.contains(" > ")) {
         return message.substring(message.indexOf(" > ") + 3).trim();
      } else if (message.contains(">")) {
         int lastEnd = message.lastIndexOf(">");
         return message.substring(lastEnd + 1).trim();
      } else {
         String[] words = message.split(" ", 2);
         return words.length > 1 ? words[1].trim() : message;
      }
   }

   private boolean isFriend(String playerName) {
      return Alien.FRIEND.isFriend(playerName);
   }

   private boolean isTargetPlayer(String playerName) {
      if (!this.useTargetList.getValue()) {
         return true;
      } else {
         String targetIds = this.targetPlayerIds.getValue();
         if (targetIds != null && !targetIds.trim().isEmpty()) {
            String[] targetIdArray = targetIds.split(",");

            for (String targetId : targetIdArray) {
               if (targetId.trim().equalsIgnoreCase(playerName)) {
                  return true;
               }
            }

            return false;
         } else {
            return true;
         }
      }
   }

   private void callAIAsync(String playerName, String message) {
      CompletableFuture.runAsync(() -> {
         try {
            if (this.debugMode.getValue()) {
               this.sendDebugMessage("AI请求...");
            }

            String response = this.callAIWithRetry(playerName, message, 0);
            if (this.debugMode.getValue()) {
               this.sendDebugMessage("AI响应: " + response);
            }

            if (response != null && !response.isEmpty()) {
               this.sendAIResponse(response);
            }
         } catch (Exception var4) {
            if (this.debugMode.getValue()) {
               this.sendDebugMessage("AI调用错误: " + var4.getMessage());
            }

            var4.printStackTrace();
         }
      });
   }

   private String callAIWithRetry(String playerName, String message, int retryCount) {
      try {
         return this.callAI(playerName, message);
      } catch (Exception var7) {
         if ((double)retryCount < this.maxRetries.getValue()) {
            this.sendDebugMessage("API调用失败，重试中... (" + (retryCount + 1) + "/" + this.maxRetries.getValue() + ")");

            try {
               Thread.sleep(1000L);
            } catch (InterruptedException var6) {
               Thread.currentThread().interrupt();
            }

            return this.callAIWithRetry(playerName, message, retryCount + 1);
         } else {
            this.sendDebugMessage("API调用失败，已达到最大重试次");
            return null;
         }
      }
   }

   private String callAI(String playerName, String message) {
      try {
         String requestBody = this.buildRequestBody(playerName, message);
         HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(this.apiUrl.getValue()))
            .header("Content-Type", "application/json")
            .header("Authorization", "Bearer " + this.apiKey.getValue())
            .timeout(Duration.ofSeconds((long)this.connectionTimeout.getValue()))
            .POST(BodyPublishers.ofString(requestBody))
            .build();
         HttpResponse<String> response = this.httpClient.send(request, BodyHandlers.ofString());
         if (this.debugMode.getValue()) {
            this.sendDebugMessage("API响应: " + response.statusCode());
            if (response.statusCode() != 200) {
               this.sendDebugMessage("响应内容: " + response.body());
            }
         }

         String result = this.parseAIResponse(response.body());
         if (result == null && this.debugMode.getValue()) {
            this.sendDebugMessage("无法从响应中解析内容: " + response.body());
         }

         return result;
      } catch (Exception var7) {
         this.sendDebugMessage("AI调用错误: " + var7.getMessage());
         var7.printStackTrace();
         return null;
      }
   }

   private String buildRequestBody(String playerName, String message) {
      String escapedSystemPrompt = this.systemPrompt.getValue().replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r");
      String escapedPlayerName = playerName.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r");
      String escapedMessage = message.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r");
      return String.format(
         Locale.US,
         "{\n    \"model\": \"%s\",\n    \"messages\": [\n        {\n            \"role\": \"system\",\n            \"content\": \"%s\"\n        },\n        {\n            \"role\": \"user\",\n            \"content\": \"玩家 %s 说: %s\"\n        }\n    ],\n    \"temperature\": %.1f,\n    \"max_tokens\": %d,\n    \"stream\": false\n}\n",
         this.model.getValue(),
         escapedSystemPrompt,
         escapedPlayerName,
         escapedMessage,
         this.temperature.getValue(),
         (int)this.maxTokens.getValue()
      );
   }

   private String parseAIResponse(String responseBody) {
      if (responseBody != null && !responseBody.isEmpty()) {
         try {
            int contentIdx = responseBody.indexOf("\"content\"");
            if (contentIdx != -1) {
               int startQuote = responseBody.indexOf("\"", contentIdx + 9);
               if (startQuote != -1) {
                  StringBuilder result = new StringBuilder();
                  boolean escaped = false;

                  for (int i = startQuote + 1; i < responseBody.length(); i++) {
                     char c = responseBody.charAt(i);
                     if (escaped) {
                        switch (c) {
                           case '"':
                              result.append('"');
                              break;
                           case '\\':
                              result.append('\\');
                              break;
                           case 'n':
                              result.append('\n');
                              break;
                           case 'r':
                              result.append('\r');
                              break;
                           case 't':
                              result.append('\t');
                              break;
                           default:
                              result.append(c);
                        }

                        escaped = false;
                     } else if (c == '\\') {
                        escaped = true;
                     } else {
                        if (c == '"') {
                           return result.toString();
                        }

                        result.append(c);
                     }
                  }
               }
            }
         } catch (Exception var8) {
            if (this.debugMode.getValue()) {
               this.sendDebugMessage("解析响应失败: " + var8.getMessage());
            }

            var8.printStackTrace();
         }

         return null;
      } else {
         return null;
      }
   }

   private void sendAIResponse(String response) {
      if (mc.player != null && mc.getNetworkHandler() != null && response != null && !response.isEmpty()) {
         String cleanResponse = response.replace("\n", " ").replace("\r", " ").trim();
         if (cleanResponse.length() > 255) {
            cleanResponse = cleanResponse.substring(0, 252) + "...";
         }

         mc.getNetworkHandler().sendChatMessage(cleanResponse);
      }
   }

   private void sendDebugMessage(String message) {
      if (mc.player != null) {
         String debugMessage = "§7[AIChat Debug] §f" + message;
         mc.player.sendMessage(Text.literal(debugMessage));
      }
   }

   private void testAPIConnection() {
      CompletableFuture.runAsync(
         () -> {
            try {
               this.sendDebugMessage("开始测试API连接...");
               HttpRequest testRequest = HttpRequest.newBuilder()
                  .uri(URI.create(this.apiUrl.getValue()))
                  .header("Content-Type", "application/json")
                  .header("Authorization", "Bearer " + this.apiKey.getValue())
                  .timeout(Duration.ofSeconds(10L))
                  .POST(
                     BodyPublishers.ofString(
                        String.format(
                           "{\n    \"model\": \"%s\",\n    \"messages\": [{\"role\": \"user\", \"content\": \"test\"}],\n    \"max_tokens\": 10\n}\n",
                           this.model.getValue()
                        )
                     )
                  )
                  .build();
               HttpResponse<String> response = this.httpClient.send(testRequest, BodyHandlers.ofString());
               this.sendDebugMessage("API连接测试结果: " + response.statusCode());
               if (response.statusCode() == 200) {
                  this.sendDebugMessage("API连接成功");
               } else {
                  this.sendDebugMessage("API连接失败，状态码: " + response.statusCode());
                  this.sendDebugMessage("响应内容: " + response.body().substring(0, Math.min(200, response.body().length())));
               }
            } catch (Exception var3) {
               this.sendDebugMessage("API连接测试失败: " + var3.getMessage());
               var3.printStackTrace();
            }
         }
      );
   }
}
