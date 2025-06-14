package com.example.webcontrol.features;

import com.example.webcontrol.WebControlConfig;
import com.example.webcontrol.notifications.NotificationManager;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.sound.SoundEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/**
 * Система уведомлений о важных игровых событиях
 */
public class EventNotificationManager {
    private static final Logger LOGGER = LoggerFactory.getLogger("webcontrol-events");
    
    private static boolean enabled = false;
    private static boolean notifyOnPlayerJoin = true;
    private static boolean notifyOnPlayerLeave = true;
    private static boolean notifyOnDeath = true;
    private static boolean notifyOnMention = true;
    private static boolean notifyOnPrivateMessage = true;
    private static boolean notifyOnServerRestart = true;
    private static boolean notifyOnLowHealth = true;
    private static boolean notifyOnLowHunger = true;
    
    private static final Set<String> knownPlayers = ConcurrentHashMap.newKeySet();
    private static final Set<String> importantKeywords = new HashSet<>();
    private static long lastHealthCheck = 0;
    private static long lastHungerCheck = 0;
    private static final long HEALTH_CHECK_INTERVAL = 5000; // 5 секунд
    
    // Паттерны для распознавания событий в чате
    private static final Pattern DEATH_PATTERN = Pattern.compile("(?i).*(died|killed|death|умер|убит|погиб).*");
    private static final Pattern JOIN_PATTERN = Pattern.compile("(?i).*(joined|вошел|зашел|подключился).*");
    private static final Pattern LEAVE_PATTERN = Pattern.compile("(?i).*(left|quit|вышел|отключился|покинул).*");
    private static final Pattern RESTART_PATTERN = Pattern.compile("(?i).*(restart|reboot|перезагрузка|рестарт).*");
    
    static {
        // Важные ключевые слова для уведомлений
        importantKeywords.add("admin");
        importantKeywords.add("модератор");
        importantKeywords.add("важно");
        importantKeywords.add("внимание");
        importantKeywords.add("срочно");
        importantKeywords.add("ban");
        importantKeywords.add("kick");
        importantKeywords.add("mute");
    }
    
    public static void initialize() {
        LOGGER.info("Event Notification Manager initialized");
        
        // Слушаем сообщения в чате
        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            if (!enabled || overlay) return;
            
            String messageText = message.getString();
            processGameMessage(messageText);
        });
        
        // Проверяем состояние игрока
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (!enabled || client.player == null) return;
            
            long currentTime = System.currentTimeMillis();
            
            // Проверяем здоровье
            if (notifyOnLowHealth && currentTime - lastHealthCheck > HEALTH_CHECK_INTERVAL) {
                checkPlayerHealth(client);
                lastHealthCheck = currentTime;
            }
            
            // Проверяем голод
            if (notifyOnLowHunger && currentTime - lastHungerCheck > HEALTH_CHECK_INTERVAL) {
                checkPlayerHunger(client);
                lastHungerCheck = currentTime;
            }
            
            // Отслеживаем игроков поблизости
            trackNearbyPlayers(client);
        });
    }
    
    private static void processGameMessage(String message) {
        try {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.player == null) return;
            
            String playerName = client.player.getName().getString();
            
            // Проверяем упоминание имени игрока
            if (notifyOnMention && message.contains(playerName)) {
                NotificationManager.warning("Упоминание", "Вас упомянули в чате!");
                playAlertSound();
            }
            
            // Проверяем личные сообщения
            if (notifyOnPrivateMessage && isPrivateMessage(message)) {
                NotificationManager.info("Личное сообщение", "Получено личное сообщение");
                playAlertSound();
            }
            
            // Проверяем смерти
            if (notifyOnDeath && DEATH_PATTERN.matcher(message).matches()) {
                NotificationManager.error("Смерть", "Обнаружена смерть игрока");
            }
            
            // Проверяем подключения/отключения
            if (notifyOnPlayerJoin && JOIN_PATTERN.matcher(message).matches()) {
                NotificationManager.info("Игрок", "Игрок присоединился к серверу");
            }
            
            if (notifyOnPlayerLeave && LEAVE_PATTERN.matcher(message).matches()) {
                NotificationManager.info("Игрок", "Игрок покинул сервер");
            }
            
            // Проверяем перезагрузку сервера
            if (notifyOnServerRestart && RESTART_PATTERN.matcher(message).matches()) {
                NotificationManager.warning("Сервер", "Сервер перезагружается!");
                playAlertSound();
            }
            
            // Проверяем важные ключевые слова
            for (String keyword : importantKeywords) {
                if (message.toLowerCase().contains(keyword.toLowerCase())) {
                    NotificationManager.warning("Важное сообщение", "Обнаружено важное ключевое слово: " + keyword);
                    playAlertSound();
                    break;
                }
            }
            
        } catch (Exception e) {
            LOGGER.error("Error processing game message", e);
        }
    }
    
    private static boolean isPrivateMessage(String message) {
        return message.startsWith("[") || 
               message.contains("whispers") || 
               message.contains("tells you") ||
               message.contains("→") || 
               message.contains(">>");
    }
    
    private static void checkPlayerHealth(MinecraftClient client) {
        if (client.player == null) return;
        
        float health = client.player.getHealth();
        float maxHealth = client.player.getMaxHealth();
        float healthPercent = (health / maxHealth) * 100;
        
        if (healthPercent <= 20) { // Менее 20% здоровья
            NotificationManager.error("Низкое здоровье", 
                String.format("Здоровье критически низкое: %.1f%%", healthPercent));
            playAlertSound();
        } else if (healthPercent <= 50) { // Менее 50% здоровья
            NotificationManager.warning("Здоровье", 
                String.format("Здоровье низкое: %.1f%%", healthPercent));
        }
    }
    
    private static void checkPlayerHunger(MinecraftClient client) {
        if (client.player == null) return;
        
        int hunger = client.player.getHungerManager().getFoodLevel();
        
        if (hunger <= 4) { // Менее 2 единиц еды
            NotificationManager.error("Голод", "Уровень голода критически низкий: " + hunger);
            playAlertSound();
        } else if (hunger <= 10) { // Менее 5 единиц еды
            NotificationManager.warning("Голод", "Уровень голода низкий: " + hunger);
        }
    }
    
    private static void trackNearbyPlayers(MinecraftClient client) {
        if (client.world == null || client.player == null) return;
        
        Set<String> currentPlayers = new HashSet<>();
        
        // Собираем всех игроков в мире
        for (PlayerEntity player : client.world.getPlayers()) {
            if (player != client.player) {
                currentPlayers.add(player.getName().getString());
            }
        }
        
        // Проверяем новых игроков
        for (String playerName : currentPlayers) {
            if (!knownPlayers.contains(playerName)) {
                if (notifyOnPlayerJoin) {
                    NotificationManager.info("Новый игрок", "Игрок " + playerName + " появился в области видимости");
                }
                knownPlayers.add(playerName);
            }
        }
        
        // Проверяем ушедших игроков
        Set<String> leftPlayers = new HashSet<>(knownPlayers);
        leftPlayers.removeAll(currentPlayers);
        
        for (String playerName : leftPlayers) {
            if (notifyOnPlayerLeave) {
                NotificationManager.info("Игрок ушел", "Игрок " + playerName + " исчез из области видимости");
            }
            knownPlayers.remove(playerName);
        }
    }
    
    private static void playAlertSound() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null && WebControlConfig.getInstance().soundEffectsEnabled) {
            client.player.playSound(SoundEvents.BLOCK_NOTE_BLOCK_PLING.value(), 1.0f, 1.5f);
        }
    }
    
    // API методы
    public static boolean isEnabled() {
        return enabled;
    }
    
    public static void setEnabled(boolean enabled) {
        EventNotificationManager.enabled = enabled;
        WebControlConfig.getInstance().updateEventNotifications(enabled, 
            notifyOnPlayerJoin, notifyOnPlayerLeave, notifyOnDeath, notifyOnMention, 
            notifyOnPrivateMessage, notifyOnServerRestart, notifyOnLowHealth, notifyOnLowHunger);
        
        if (enabled) {
            NotificationManager.success("Event Notifications", "Уведомления о событиях включены");
            LOGGER.info("Event notifications enabled");
        } else {
            NotificationManager.info("Event Notifications", "Уведомления о событиях отключены");
            LOGGER.info("Event notifications disabled");
        }
    }
    
    public static void setNotifyOnPlayerJoin(boolean notify) {
        notifyOnPlayerJoin = notify;
        updateConfig();
    }
    
    public static void setNotifyOnPlayerLeave(boolean notify) {
        notifyOnPlayerLeave = notify;
        updateConfig();
    }
    
    public static void setNotifyOnDeath(boolean notify) {
        notifyOnDeath = notify;
        updateConfig();
    }
    
    public static void setNotifyOnMention(boolean notify) {
        notifyOnMention = notify;
        updateConfig();
    }
    
    public static void setNotifyOnPrivateMessage(boolean notify) {
        notifyOnPrivateMessage = notify;
        updateConfig();
    }
    
    public static void setNotifyOnServerRestart(boolean notify) {
        notifyOnServerRestart = notify;
        updateConfig();
    }
    
    public static void setNotifyOnLowHealth(boolean notify) {
        notifyOnLowHealth = notify;
        updateConfig();
    }
    
    public static void setNotifyOnLowHunger(boolean notify) {
        notifyOnLowHunger = notify;
        updateConfig();
    }
    
    private static void updateConfig() {
        WebControlConfig.getInstance().updateEventNotifications(enabled, 
            notifyOnPlayerJoin, notifyOnPlayerLeave, notifyOnDeath, notifyOnMention, 
            notifyOnPrivateMessage, notifyOnServerRestart, notifyOnLowHealth, notifyOnLowHunger);
    }
    
    public static void addImportantKeyword(String keyword) {
        importantKeywords.add(keyword.toLowerCase());
        NotificationManager.info("Event Notifications", "Добавлено ключевое слово: " + keyword);
    }
    
    public static void removeImportantKeyword(String keyword) {
        if (importantKeywords.remove(keyword.toLowerCase())) {
            NotificationManager.info("Event Notifications", "Удалено ключевое слово: " + keyword);
        }
    }
    
    public static Set<String> getImportantKeywords() {
        return new HashSet<>(importantKeywords);
    }
    
    public static void toggle() {
        setEnabled(!enabled);
    }
    
    // Геттеры для всех настроек
    public static boolean isNotifyOnPlayerJoin() { return notifyOnPlayerJoin; }
    public static boolean isNotifyOnPlayerLeave() { return notifyOnPlayerLeave; }
    public static boolean isNotifyOnDeath() { return notifyOnDeath; }
    public static boolean isNotifyOnMention() { return notifyOnMention; }
    public static boolean isNotifyOnPrivateMessage() { return notifyOnPrivateMessage; }
    public static boolean isNotifyOnServerRestart() { return notifyOnServerRestart; }
    public static boolean isNotifyOnLowHealth() { return notifyOnLowHealth; }
    public static boolean isNotifyOnLowHunger() { return notifyOnLowHunger; }
    
    public static void loadFromConfig(WebControlConfig config) {
        enabled = config.isEventNotificationsEnabled();
        notifyOnPlayerJoin = config.isNotifyOnPlayerJoin();
        notifyOnPlayerLeave = config.isNotifyOnPlayerLeave();
        notifyOnDeath = config.isNotifyOnDeath();
        notifyOnMention = config.isNotifyOnMention();
        notifyOnPrivateMessage = config.isNotifyOnPrivateMessage();
        notifyOnServerRestart = config.isNotifyOnServerRestart();
        notifyOnLowHealth = config.isNotifyOnLowHealth();
        notifyOnLowHunger = config.isNotifyOnLowHunger();
        
        LOGGER.info("Loaded Event Notification settings: enabled={}", enabled);
    }
}
