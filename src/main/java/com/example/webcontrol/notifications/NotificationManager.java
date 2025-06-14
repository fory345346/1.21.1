package com.example.webcontrol.notifications;

import com.example.webcontrol.WebControlConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.toast.SystemToast;
import net.minecraft.client.toast.ToastManager;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Advanced notification system for WebControl mod
 * Supports in-game toasts, chat messages, sounds, and web notifications
 */
public class NotificationManager {
    private static final Logger LOGGER = LoggerFactory.getLogger("webcontrol-notifications");
    private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private static final ConcurrentLinkedQueue<Notification> notificationQueue = new ConcurrentLinkedQueue<>();
    
    public enum NotificationType {
        INFO,
        SUCCESS,
        WARNING,
        ERROR,
        SECURITY
    }
    
    public enum NotificationMethod {
        TOAST,
        CHAT,
        SOUND,
        ALL
    }
    
    public static class Notification {
        public final String title;
        public final String message;
        public final NotificationType type;
        public final NotificationMethod method;
        public final long timestamp;
        
        public Notification(String title, String message, NotificationType type, NotificationMethod method) {
            this.title = title;
            this.message = message;
            this.type = type;
            this.method = method;
            this.timestamp = System.currentTimeMillis();
        }
    }
    
    public static void initialize() {
        LOGGER.info("Notification Manager initialized");
        
        // Start notification processor
        scheduler.scheduleAtFixedRate(NotificationManager::processNotifications, 0, 100, TimeUnit.MILLISECONDS);
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
    
    /**
     * Send a notification with specified type and method
     */
    public static void notify(String title, String message, NotificationType type, NotificationMethod method) {
        if (!WebControlConfig.getInstance().showNotifications) {
            return;
        }
        
        Notification notification = new Notification(title, message, type, method);
        notificationQueue.offer(notification);
        
        LOGGER.info("Notification queued: {} - {}", title, message);
    }
    
    /**
     * Convenience methods for different notification types
     */
    public static void info(String title, String message) {
        notify(title, message, NotificationType.INFO, NotificationMethod.ALL);
    }
    
    public static void success(String title, String message) {
        notify(title, message, NotificationType.SUCCESS, NotificationMethod.ALL);
    }
    
    public static void warning(String title, String message) {
        notify(title, message, NotificationType.WARNING, NotificationMethod.ALL);
    }
    
    public static void error(String title, String message) {
        notify(title, message, NotificationType.ERROR, NotificationMethod.ALL);
    }
    
    public static void security(String title, String message) {
        notify(title, message, NotificationType.SECURITY, NotificationMethod.ALL);
    }
    
    /**
     * Quick notification methods
     */
    public static void quickInfo(String message) {
        notify("WebControl", message, NotificationType.INFO, NotificationMethod.CHAT);
    }
    
    public static void quickSuccess(String message) {
        notify("WebControl", message, NotificationType.SUCCESS, NotificationMethod.CHAT);
    }
    
    public static void quickWarning(String message) {
        notify("WebControl", message, NotificationType.WARNING, NotificationMethod.ALL);
    }
    
    public static void quickError(String message) {
        notify("WebControl", message, NotificationType.ERROR, NotificationMethod.ALL);
    }
    
    private static void processNotifications() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) {
            return;
        }
        
        Notification notification = notificationQueue.poll();
        if (notification == null) {
            return;
        }
        
        try {
            switch (notification.method) {
                case TOAST:
                    showToast(notification);
                    break;
                case CHAT:
                    showChatMessage(notification);
                    break;
                case SOUND:
                    playSound(notification);
                    break;
                case ALL:
                    showToast(notification);
                    showChatMessage(notification);
                    playSound(notification);
                    break;
            }
        } catch (Exception e) {
            LOGGER.error("Failed to process notification: {}", e.getMessage());
        }
    }
    
    private static void showToast(Notification notification) {
        MinecraftClient client = MinecraftClient.getInstance();
        ToastManager toastManager = client.getToastManager();
        
        Text title = Text.literal(getColorCode(notification.type) + notification.title);
        Text description = Text.literal("§7" + notification.message);
        
        SystemToast toast = SystemToast.create(client, SystemToast.Type.NARRATOR_TOGGLE, title, description);
        toastManager.add(toast);
    }
    
    private static void showChatMessage(Notification notification) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;
        
        String colorCode = getColorCode(notification.type);
        String prefix = getPrefix(notification.type);
        String message = String.format("%s[%s] §7%s", colorCode, prefix, notification.message);
        
        client.player.sendMessage(Text.literal(message), false);
    }
    
    private static void playSound(Notification notification) {
        if (!WebControlConfig.getInstance().soundEffectsEnabled) {
            return;
        }
        
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;
        
        switch (notification.type) {
            case INFO:
                client.player.playSound(SoundEvents.BLOCK_NOTE_BLOCK_CHIME.value(), 0.5f, 1.0f);
                break;
            case SUCCESS:
                client.player.playSound(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, 0.7f, 1.2f);
                break;
            case WARNING:
                client.player.playSound(SoundEvents.BLOCK_NOTE_BLOCK_PLING.value(), 0.8f, 0.8f);
                break;
            case ERROR:
                client.player.playSound(SoundEvents.BLOCK_ANVIL_LAND, 0.6f, 1.0f);
                break;
            case SECURITY:
                client.player.playSound(SoundEvents.ENTITY_ENDERMAN_SCREAM, 0.5f, 0.8f);
                break;
        }
    }
    
    private static String getColorCode(NotificationType type) {
        switch (type) {
            case INFO: return "§b";
            case SUCCESS: return "§a";
            case WARNING: return "§e";
            case ERROR: return "§c";
            case SECURITY: return "§4";
            default: return "§f";
        }
    }
    
    private static String getPrefix(NotificationType type) {
        switch (type) {
            case INFO: return "INFO";
            case SUCCESS: return "SUCCESS";
            case WARNING: return "WARNING";
            case ERROR: return "ERROR";
            case SECURITY: return "SECURITY";
            default: return "WEBCONTROL";
        }
    }
}
