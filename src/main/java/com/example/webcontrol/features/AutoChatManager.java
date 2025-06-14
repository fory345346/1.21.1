package com.example.webcontrol.features;

import com.example.webcontrol.WebControlConfig;
import com.example.webcontrol.notifications.NotificationManager;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

/**
 * Система автоматических ответов в чат
 */
public class AutoChatManager {
    private static final Logger LOGGER = LoggerFactory.getLogger("webcontrol-autochat");
    private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    
    private static boolean enabled = false;
    private static final Map<String, String> autoResponses = new ConcurrentHashMap<>();
    private static final Map<String, Long> lastResponseTime = new ConcurrentHashMap<>();
    private static final long RESPONSE_COOLDOWN = 30000; // 30 секунд между одинаковыми ответами
    
    // Предустановленные ответы
    static {
        autoResponses.put("(?i).*привет.*", "Привет! Как дела?");
        autoResponses.put("(?i).*как дела.*", "Все отлично, спасибо!");
        autoResponses.put("(?i).*что делаешь.*", "Играю в Minecraft");
        autoResponses.put("(?i).*где ты.*", "Я здесь");
        autoResponses.put("(?i).*помощь.*", "Чем могу помочь?");
        autoResponses.put("(?i).*спасибо.*", "Пожалуйста!");
        autoResponses.put("(?i).*пока.*", "До свидания!");
    }
    
    public static void initialize() {
        LOGGER.info("AutoChat Manager initialized");
        
        // Слушаем входящие сообщения
        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            if (!enabled || overlay) return;
            
            String messageText = message.getString();
            processIncomingMessage(messageText);
        });
    }
    
    private static void processIncomingMessage(String message) {
        try {
            // Проверяем, является ли это личным сообщением или упоминанием
            if (!isPersonalMessage(message)) {
                return;
            }
            
            // Ищем подходящий автоответ
            for (Map.Entry<String, String> entry : autoResponses.entrySet()) {
                String pattern = entry.getKey();
                String response = entry.getValue();
                
                if (Pattern.matches(pattern, message)) {
                    // Проверяем кулдаун
                    String cooldownKey = pattern + ":" + response;
                    Long lastTime = lastResponseTime.get(cooldownKey);
                    long currentTime = System.currentTimeMillis();
                    
                    if (lastTime == null || currentTime - lastTime > RESPONSE_COOLDOWN) {
                        sendAutoResponse(response);
                        lastResponseTime.put(cooldownKey, currentTime);
                        LOGGER.info("Auto response sent: {} -> {}", pattern, response);
                        break; // Отправляем только первый подходящий ответ
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.error("Error processing incoming message", e);
        }
    }
    
    private static boolean isPersonalMessage(String message) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return false;
        
        String playerName = client.player.getName().getString();
        
        // Проверяем различные форматы личных сообщений
        return message.contains(playerName) || 
               message.startsWith("[") || // Возможно личное сообщение в скобках
               message.contains("whispers") || 
               message.contains("tells you") ||
               message.contains("→") || // Стрелка в некоторых чат-плагинах
               message.contains(">>"); // Другой формат личных сообщений
    }
    
    private static void sendAutoResponse(String response) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;
        
        // Добавляем небольшую задержку для естественности
        int delay = 1000 + (int)(Math.random() * 3000); // 1-4 секунды
        
        scheduler.schedule(() -> {
            client.execute(() -> {
                if (client.player != null) {
                    client.player.networkHandler.sendChatMessage(response);
                }
            });
        }, delay, TimeUnit.MILLISECONDS);
    }
    
    // API методы
    public static boolean isEnabled() {
        return enabled;
    }
    
    public static void setEnabled(boolean enabled) {
        AutoChatManager.enabled = enabled;
        WebControlConfig.getInstance().updateAutoChat(enabled, new HashMap<>(autoResponses));
        
        if (enabled) {
            NotificationManager.success("Auto Chat", "Автоответы включены (" + autoResponses.size() + " шаблонов)");
            LOGGER.info("Auto chat enabled with {} response patterns", autoResponses.size());
        } else {
            NotificationManager.info("Auto Chat", "Автоответы отключены");
            LOGGER.info("Auto chat disabled");
        }
    }
    
    public static void addResponse(String pattern, String response) {
        autoResponses.put(pattern, response);
        WebControlConfig.getInstance().updateAutoChat(enabled, new HashMap<>(autoResponses));
        NotificationManager.info("Auto Chat", "Добавлен новый автоответ");
        LOGGER.info("Added auto response: {} -> {}", pattern, response);
    }
    
    public static void removeResponse(String pattern) {
        String removed = autoResponses.remove(pattern);
        if (removed != null) {
            WebControlConfig.getInstance().updateAutoChat(enabled, new HashMap<>(autoResponses));
            NotificationManager.info("Auto Chat", "Автоответ удален");
            LOGGER.info("Removed auto response: {}", pattern);
        }
    }
    
    public static Map<String, String> getAllResponses() {
        return new HashMap<>(autoResponses);
    }
    
    public static void clearAllResponses() {
        autoResponses.clear();
        WebControlConfig.getInstance().updateAutoChat(enabled, new HashMap<>(autoResponses));
        NotificationManager.info("Auto Chat", "Все автоответы очищены");
        LOGGER.info("All auto responses cleared");
    }
    
    public static void resetToDefaults() {
        autoResponses.clear();
        autoResponses.put("(?i).*привет.*", "Привет! Как дела?");
        autoResponses.put("(?i).*как дела.*", "Все отлично, спасибо!");
        autoResponses.put("(?i).*что делаешь.*", "Играю в Minecraft");
        autoResponses.put("(?i).*где ты.*", "Я здесь");
        autoResponses.put("(?i).*помощь.*", "Чем могу помочь?");
        autoResponses.put("(?i).*спасибо.*", "Пожалуйста!");
        autoResponses.put("(?i).*пока.*", "До свидания!");
        
        WebControlConfig.getInstance().updateAutoChat(enabled, new HashMap<>(autoResponses));
        NotificationManager.info("Auto Chat", "Восстановлены стандартные автоответы");
        LOGGER.info("Reset to default auto responses");
    }
    
    public static int getResponseCount() {
        return autoResponses.size();
    }
    
    public static void toggle() {
        setEnabled(!enabled);
    }
    
    public static void loadFromConfig(WebControlConfig config) {
        enabled = config.isAutoChatEnabled();
        Map<String, String> configResponses = config.getAutoChatResponses();
        if (configResponses != null && !configResponses.isEmpty()) {
            autoResponses.clear();
            autoResponses.putAll(configResponses);
        }
        LOGGER.info("Loaded AutoChat settings: enabled={}, responses={}", enabled, autoResponses.size());
    }
    
    public static void shutdown() {
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
        }
    }
}
