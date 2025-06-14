package com.example.webcontrol.features;

import com.example.webcontrol.WebControlConfig;
import com.example.webcontrol.notifications.NotificationManager;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.DisconnectedScreen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.screen.multiplayer.ConnectScreen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.network.ServerAddress;
import net.minecraft.client.network.ServerInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Автоматическое переподключение к серверу при разрыве соединения
 */
public class AutoReconnectManager {
    private static final Logger LOGGER = LoggerFactory.getLogger("webcontrol-autoreconnect");
    private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    
    private static boolean enabled = false;
    private static int reconnectDelay = 5; // секунды
    private static int maxAttempts = 10;
    private static int currentAttempts = 0;
    private static String lastServerAddress = null;
    private static boolean isReconnecting = false;
    
    public static void initialize() {
        LOGGER.info("AutoReconnect Manager initialized");
        
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (!enabled) return;
            
            // Проверяем, находимся ли мы на экране отключения
            if (client.currentScreen instanceof DisconnectedScreen && !isReconnecting) {
                handleDisconnection(client);
            }
            
            // Сохраняем адрес сервера когда подключены
            if (client.getCurrentServerEntry() != null && client.player != null) {
                lastServerAddress = client.getCurrentServerEntry().address;
                currentAttempts = 0; // Сбрасываем счетчик при успешном подключении
            }
        });
    }
    
    private static void handleDisconnection(MinecraftClient client) {
        if (lastServerAddress == null || currentAttempts >= maxAttempts) {
            if (currentAttempts >= maxAttempts) {
                NotificationManager.error("Auto Reconnect", 
                    "Максимальное количество попыток переподключения достигнуто (" + maxAttempts + ")");
                enabled = false; // Отключаем автопереподключение
            }
            return;
        }
        
        isReconnecting = true;
        currentAttempts++;
        
        NotificationManager.warning("Auto Reconnect", 
            "Попытка переподключения #" + currentAttempts + " через " + reconnectDelay + " секунд...");
        
        scheduler.schedule(() -> {
            try {
                reconnectToServer(client);
            } catch (Exception e) {
                LOGGER.error("Ошибка при переподключении", e);
                isReconnecting = false;
            }
        }, reconnectDelay, TimeUnit.SECONDS);
    }
    
    private static void reconnectToServer(MinecraftClient client) {
        client.execute(() -> {
            try {
                ServerInfo serverInfo = new ServerInfo("Auto Reconnect", lastServerAddress, ServerInfo.ServerType.OTHER);
                ConnectScreen.connect(new MultiplayerScreen(new TitleScreen()), client, 
                                    ServerAddress.parse(lastServerAddress), serverInfo, false, null);
                
                LOGGER.info("Попытка переподключения к серверу: {}", lastServerAddress);
                
                // Планируем проверку успешности подключения
                scheduler.schedule(() -> {
                    if (client.player != null) {
                        NotificationManager.success("Auto Reconnect", "Успешно переподключились к серверу!");
                        currentAttempts = 0;
                    }
                    isReconnecting = false;
                }, 10, TimeUnit.SECONDS);
                
            } catch (Exception e) {
                LOGGER.error("Ошибка при попытке переподключения", e);
                isReconnecting = false;
            }
        });
    }
    
    // API методы
    public static boolean isEnabled() {
        return enabled;
    }
    
    public static void setEnabled(boolean enabled) {
        AutoReconnectManager.enabled = enabled;
        WebControlConfig.getInstance().updateAutoReconnect(enabled, reconnectDelay, maxAttempts);
        
        if (enabled) {
            NotificationManager.success("Auto Reconnect", "Автопереподключение включено");
            LOGGER.info("Auto reconnect enabled: delay={}s, maxAttempts={}", reconnectDelay, maxAttempts);
        } else {
            NotificationManager.info("Auto Reconnect", "Автопереподключение отключено");
            LOGGER.info("Auto reconnect disabled");
            currentAttempts = 0;
            isReconnecting = false;
        }
    }
    
    public static int getReconnectDelay() {
        return reconnectDelay;
    }
    
    public static void setReconnectDelay(int delay) {
        AutoReconnectManager.reconnectDelay = Math.max(1, Math.min(60, delay)); // 1-60 секунд
        WebControlConfig.getInstance().updateAutoReconnect(enabled, reconnectDelay, maxAttempts);
        NotificationManager.info("Auto Reconnect", "Задержка переподключения: " + reconnectDelay + " секунд");
    }
    
    public static int getMaxAttempts() {
        return maxAttempts;
    }
    
    public static void setMaxAttempts(int attempts) {
        AutoReconnectManager.maxAttempts = Math.max(1, Math.min(50, attempts)); // 1-50 попыток
        WebControlConfig.getInstance().updateAutoReconnect(enabled, reconnectDelay, maxAttempts);
        NotificationManager.info("Auto Reconnect", "Максимум попыток: " + maxAttempts);
    }
    
    public static int getCurrentAttempts() {
        return currentAttempts;
    }
    
    public static boolean isReconnecting() {
        return isReconnecting;
    }
    
    public static String getLastServerAddress() {
        return lastServerAddress;
    }
    
    public static void toggle() {
        setEnabled(!enabled);
    }
    
    public static void forceReconnect() {
        if (lastServerAddress != null) {
            MinecraftClient client = MinecraftClient.getInstance();
            currentAttempts = 0;
            isReconnecting = true;
            NotificationManager.info("Auto Reconnect", "Принудительное переподключение...");
            reconnectToServer(client);
        } else {
            NotificationManager.error("Auto Reconnect", "Нет сохраненного адреса сервера");
        }
    }
    
    public static void loadFromConfig(WebControlConfig config) {
        enabled = config.isAutoReconnectEnabled();
        reconnectDelay = config.getAutoReconnectDelay();
        maxAttempts = config.getAutoReconnectMaxAttempts();
        LOGGER.info("Loaded AutoReconnect settings: enabled={}, delay={}s, maxAttempts={}", 
                   enabled, reconnectDelay, maxAttempts);
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
