package com.example.webcontrol.features;

import com.example.webcontrol.WebControlConfig;
import com.example.webcontrol.notifications.NotificationManager;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.math.MathHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Random;

/**
 * Система анти-AFK для предотвращения автоматического кика за неактивность
 */
public class AntiAFKManager {
    private static final Logger LOGGER = LoggerFactory.getLogger("webcontrol-antiafk");
    private static final Random random = new Random();
    
    private static boolean enabled = false;
    private static int actionInterval = 30; // секунды между действиями
    private static AFKAction currentAction = AFKAction.MOUSE_MOVEMENT;
    
    private static long lastActionTime = 0;
    private static long lastPlayerMovement = 0;
    private static double lastPlayerX = 0;
    private static double lastPlayerY = 0;
    private static double lastPlayerZ = 0;
    
    public enum AFKAction {
        MOUSE_MOVEMENT("Движение мыши"),
        SMALL_MOVEMENT("Небольшие движения"),
        JUMP("Прыжки"),
        ROTATION("Поворот головы"),
        SNEAK("Приседание"),
        MIXED("Смешанные действия");
        
        private final String description;
        
        AFKAction(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
    }
    
    public static void initialize() {
        LOGGER.info("AntiAFK Manager initialized");
        
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (!enabled || client.player == null || client.world == null) {
                return;
            }
            
            updatePlayerPosition(client.player);
            
            long currentTime = System.currentTimeMillis();
            
            // Проверяем, двигался ли игрок недавно
            if (currentTime - lastPlayerMovement < 5000) { // 5 секунд
                lastActionTime = currentTime; // Сбрасываем таймер если игрок активен
                return;
            }
            
            // Выполняем анти-AFK действие если прошло достаточно времени
            if (currentTime - lastActionTime > actionInterval * 1000L) {
                performAntiAFKAction(client);
                lastActionTime = currentTime;
            }
        });
    }
    
    private static void updatePlayerPosition(ClientPlayerEntity player) {
        double currentX = player.getX();
        double currentY = player.getY();
        double currentZ = player.getZ();
        
        // Проверяем, изменилась ли позиция игрока
        if (Math.abs(currentX - lastPlayerX) > 0.1 || 
            Math.abs(currentY - lastPlayerY) > 0.1 || 
            Math.abs(currentZ - lastPlayerZ) > 0.1) {
            
            lastPlayerMovement = System.currentTimeMillis();
            lastPlayerX = currentX;
            lastPlayerY = currentY;
            lastPlayerZ = currentZ;
        }
    }
    
    private static void performAntiAFKAction(MinecraftClient client) {
        try {
            switch (currentAction) {
                case MOUSE_MOVEMENT:
                    performMouseMovement(client);
                    break;
                case SMALL_MOVEMENT:
                    performSmallMovement(client);
                    break;
                case JUMP:
                    performJump(client);
                    break;
                case ROTATION:
                    performRotation(client);
                    break;
                case SNEAK:
                    performSneak(client);
                    break;
                case MIXED:
                    performMixedAction(client);
                    break;
            }
            
            LOGGER.debug("Performed anti-AFK action: {}", currentAction.getDescription());
        } catch (Exception e) {
            LOGGER.error("Error performing anti-AFK action", e);
        }
    }
    
    private static void performMouseMovement(MinecraftClient client) {
        if (client.player == null) return;
        
        // Небольшое движение мыши
        float yawChange = (random.nextFloat() - 0.5f) * 2.0f; // -1 до 1 градус
        float pitchChange = (random.nextFloat() - 0.5f) * 1.0f; // -0.5 до 0.5 градус
        
        client.player.setYaw(client.player.getYaw() + yawChange);
        client.player.setPitch(MathHelper.clamp(client.player.getPitch() + pitchChange, -90.0f, 90.0f));
    }
    
    private static void performSmallMovement(MinecraftClient client) {
        if (client.player == null) return;
        
        // Очень небольшое движение вперед-назад
        double direction = random.nextBoolean() ? 0.01 : -0.01;
        double yaw = Math.toRadians(client.player.getYaw());
        
        double deltaX = -Math.sin(yaw) * direction;
        double deltaZ = Math.cos(yaw) * direction;
        
        client.player.setPosition(
            client.player.getX() + deltaX,
            client.player.getY(),
            client.player.getZ() + deltaZ
        );
    }
    
    private static void performJump(MinecraftClient client) {
        if (client.player == null || !client.player.isOnGround()) return;
        
        client.player.jump();
    }
    
    private static void performRotation(MinecraftClient client) {
        if (client.player == null) return;
        
        // Поворот на случайный угол
        float newYaw = client.player.getYaw() + (random.nextFloat() * 20 - 10); // -10 до 10 градусов
        client.player.setYaw(newYaw);
    }
    
    private static void performSneak(MinecraftClient client) {
        if (client.player == null) return;
        
        // Кратковременное приседание
        client.player.setSneaking(true);
        
        // Планируем отключение приседания через короткое время
        new Thread(() -> {
            try {
                Thread.sleep(200); // 200ms
                if (client.player != null) {
                    client.player.setSneaking(false);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();
    }
    
    private static void performMixedAction(MinecraftClient client) {
        // Случайно выбираем одно из действий
        AFKAction[] actions = {AFKAction.MOUSE_MOVEMENT, AFKAction.ROTATION, AFKAction.JUMP, AFKAction.SNEAK};
        AFKAction randomAction = actions[random.nextInt(actions.length)];
        
        switch (randomAction) {
            case MOUSE_MOVEMENT:
                performMouseMovement(client);
                break;
            case ROTATION:
                performRotation(client);
                break;
            case JUMP:
                performJump(client);
                break;
            case SNEAK:
                performSneak(client);
                break;
        }
    }
    
    // API методы
    public static boolean isEnabled() {
        return enabled;
    }
    
    public static void setEnabled(boolean enabled) {
        AntiAFKManager.enabled = enabled;
        WebControlConfig.getInstance().updateAntiAFK(enabled, actionInterval, currentAction);
        
        if (enabled) {
            NotificationManager.success("Anti-AFK", "Анти-AFK включен (" + currentAction.getDescription() + ")");
            LOGGER.info("Anti-AFK enabled: action={}, interval={}s", currentAction, actionInterval);
        } else {
            NotificationManager.info("Anti-AFK", "Анти-AFK отключен");
            LOGGER.info("Anti-AFK disabled");
        }
    }
    
    public static int getActionInterval() {
        return actionInterval;
    }
    
    public static void setActionInterval(int interval) {
        AntiAFKManager.actionInterval = Math.max(5, Math.min(300, interval)); // 5-300 секунд
        WebControlConfig.getInstance().updateAntiAFK(enabled, actionInterval, currentAction);
        NotificationManager.info("Anti-AFK", "Интервал действий: " + actionInterval + " секунд");
    }
    
    public static AFKAction getCurrentAction() {
        return currentAction;
    }
    
    public static void setCurrentAction(AFKAction action) {
        AntiAFKManager.currentAction = action;
        WebControlConfig.getInstance().updateAntiAFK(enabled, actionInterval, currentAction);
        NotificationManager.info("Anti-AFK", "Тип действия: " + action.getDescription());
    }
    
    public static long getTimeSinceLastAction() {
        return System.currentTimeMillis() - lastActionTime;
    }
    
    public static long getTimeSinceLastMovement() {
        return System.currentTimeMillis() - lastPlayerMovement;
    }
    
    public static void toggle() {
        setEnabled(!enabled);
    }
    
    public static void loadFromConfig(WebControlConfig config) {
        enabled = config.isAntiAFKEnabled();
        actionInterval = config.getAntiAFKInterval();

        // Безопасная загрузка действия
        try {
            Object actionObj = config.getAntiAFKAction();
            if (actionObj instanceof AFKAction) {
                currentAction = (AFKAction) actionObj;
            } else if (actionObj instanceof String) {
                currentAction = AFKAction.valueOf((String) actionObj);
            } else {
                currentAction = AFKAction.MOUSE_MOVEMENT; // По умолчанию
            }
        } catch (Exception e) {
            LOGGER.warn("Failed to load AFK action from config, using default: {}", e.getMessage());
            currentAction = AFKAction.MOUSE_MOVEMENT;
        }

        LOGGER.info("Loaded AntiAFK settings: enabled={}, interval={}s, action={}",
                   enabled, actionInterval, currentAction);
    }
}
