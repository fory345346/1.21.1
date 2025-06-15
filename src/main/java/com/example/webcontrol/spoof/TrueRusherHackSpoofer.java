package com.example.webcontrol.spoof;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;

/**
 * TRUE RusherHack Streamer Mode Implementation
 * 
 * Ключевое отличие: мы БУКВАЛЬНО МОДИФИЦИРУЕМ ПОЛЯ В ПАКЕТАХ
 * как это делает оригинальный RusherHack плагин
 * 
 * Автор объяснил:
 * "it just offsets all coordinates that go in and reverse it on all coords that go out"
 * "you change the incoming coordinates with n and the outgoing with -n"
 * 
 * server->client: 150 50 150 → my plugin: 150-100 = 50, 50, 150-100, 50
 * client->server: 50+100, 50, 50+100
 */
public class TrueRusherHackSpoofer {
    private static final Logger LOGGER = LoggerFactory.getLogger(TrueRusherHackSpoofer.class);

    public enum SpoofMode {
        VANILLA,    // No spoofing
        OFFSET      // True RusherHack mode
    }

    private static SpoofMode currentMode = SpoofMode.VANILLA;
    private static double offsetX = 0; // n для X
    private static double offsetY = 0; // n для Y (обычно 0)
    private static double offsetZ = 0; // n для Z

    // ===== CONFIGURATION =====
    
    public static SpoofMode getCurrentMode() {
        return currentMode;
    }

    public static void setMode(SpoofMode mode) {
        currentMode = mode;
        LOGGER.info("[TrueRusherHackSpoofer] Mode set to: " + mode);
    }

    public static void setOffset(double x, double y, double z) {
        offsetX = x;
        offsetY = y;
        offsetZ = z;
        LOGGER.info("[TrueRusherHackSpoofer] Offset set to: X=" + x + ", Y=" + y + ", Z=" + z);
    }

    // ===== TRUE RUSHERHACK LOGIC =====
    
    /**
     * EventPacket.Receive - БУКВАЛЬНО МОДИФИЦИРУЕМ ПОЛЯ В ПАКЕТАХ
     * "you change the incoming coordinates with n" где n = -offset
     */
    public static void onPacketReceive(Object packet) {
        if (currentMode == SpoofMode.VANILLA) {
            return;
        }

        try {
            String packetName = packet.getClass().getSimpleName();
            LOGGER.info("[TrueRusherHackSpoofer] Receive: " + packetName);
            
            // БУКВАЛЬНО МОДИФИЦИРУЕМ ВСЕ ПОЛЯ КООРДИНАТ В ПАКЕТЕ
            modifyPacketCoordinates(packet, true); // true = subtract offset
            
        } catch (Exception e) {
            LOGGER.error("[TrueRusherHackSpoofer] Error in onPacketReceive: " + e.getMessage());
        }
    }

    /**
     * EventPacket.Send - БУКВАЛЬНО МОДИФИЦИРУЕМ ПОЛЯ В ПАКЕТАХ
     * "you change the outgoing coordinates with -n" где -n = +offset
     */
    public static void onPacketSend(Object packet) {
        if (currentMode == SpoofMode.VANILLA) {
            return;
        }

        try {
            String packetName = packet.getClass().getSimpleName();
            LOGGER.info("[TrueRusherHackSpoofer] Send: " + packetName);
            
            // БУКВАЛЬНО МОДИФИЦИРУЕМ ВСЕ ПОЛЯ КООРДИНАТ В ПАКЕТЕ
            modifyPacketCoordinates(packet, false); // false = add offset
            
        } catch (Exception e) {
            LOGGER.error("[TrueRusherHackSpoofer] Error in onPacketSend: " + e.getMessage());
        }
    }

    /**
     * КЛЮЧЕВОЙ МЕТОД: Буквально модифицируем все поля координат в пакете
     * Это то что делает оригинальный RusherHack плагин
     */
    private static void modifyPacketCoordinates(Object packet, boolean subtractOffset) {
        Class<?> clazz = packet.getClass();
        
        // Получаем ВСЕ поля пакета
        Field[] fields = getAllFields(clazz);
        
        for (Field field : fields) {
            try {
                field.setAccessible(true);
                
                String fieldName = field.getName();
                Class<?> fieldType = field.getType();
                
                // Модифицируем поля координат
                if (isCoordinateField(fieldName, fieldType)) {
                    modifyCoordinateField(packet, field, subtractOffset);
                }
                
            } catch (Exception e) {
                // Игнорируем ошибки отдельных полей
            }
        }
    }

    /**
     * Получить все поля класса включая наследованные
     */
    private static Field[] getAllFields(Class<?> clazz) {
        java.util.List<Field> fields = new java.util.ArrayList<>();
        
        while (clazz != null) {
            fields.addAll(java.util.Arrays.asList(clazz.getDeclaredFields()));
            clazz = clazz.getSuperclass();
        }
        
        return fields.toArray(new Field[0]);
    }

    /**
     * Проверить является ли поле координатой
     */
    private static boolean isCoordinateField(String fieldName, Class<?> fieldType) {
        // Проверяем имена полей
        String name = fieldName.toLowerCase();
        boolean isCoordName = name.equals("x") || name.equals("y") || name.equals("z") ||
                             name.contains("pos") || name.contains("coord") ||
                             name.equals("dx") || name.equals("dy") || name.equals("dz");
        
        // Проверяем типы полей
        boolean isCoordType = fieldType == double.class || fieldType == Double.class ||
                             fieldType == float.class || fieldType == Float.class ||
                             fieldType == int.class || fieldType == Integer.class ||
                             fieldType == Vec3d.class || fieldType == BlockPos.class;
        
        return isCoordName && isCoordType;
    }

    /**
     * БУКВАЛЬНО МОДИФИЦИРУЕМ ПОЛЕ КООРДИНАТЫ
     */
    private static void modifyCoordinateField(Object packet, Field field, boolean subtractOffset) {
        try {
            String fieldName = field.getName();
            Class<?> fieldType = field.getType();
            
            // Определяем какой офсет использовать
            double offset = getOffsetForField(fieldName);
            if (offset == 0) return; // Не модифицируем если офсет 0
            
            if (fieldType == double.class || fieldType == Double.class) {
                double value = field.getDouble(packet);
                double newValue = subtractOffset ? value - offset : value + offset;
                field.setDouble(packet, newValue);
                
                LOGGER.info("[TrueRusherHackSpoofer] " + fieldName + ": " + value + " → " + newValue);
                
            } else if (fieldType == float.class || fieldType == Float.class) {
                float value = field.getFloat(packet);
                float newValue = subtractOffset ? value - (float)offset : value + (float)offset;
                field.setFloat(packet, newValue);
                
                LOGGER.info("[TrueRusherHackSpoofer] " + fieldName + ": " + value + " → " + newValue);
                
            } else if (fieldType == int.class || fieldType == Integer.class) {
                int value = field.getInt(packet);
                int newValue = subtractOffset ? value - (int)offset : value + (int)offset;
                field.setInt(packet, newValue);
                
                LOGGER.info("[TrueRusherHackSpoofer] " + fieldName + ": " + value + " → " + newValue);
                
            } else if (fieldType == Vec3d.class) {
                Vec3d value = (Vec3d) field.get(packet);
                if (value != null) {
                    Vec3d newValue = subtractOffset ?
                        new Vec3d(value.x - offsetX, value.y - offsetY, value.z - offsetZ) :
                        new Vec3d(value.x + offsetX, value.y + offsetY, value.z + offsetZ);
                    field.set(packet, newValue);
                    
                    LOGGER.info("[TrueRusherHackSpoofer] " + fieldName + ": " + value + " → " + newValue);
                }
                
            } else if (fieldType == BlockPos.class) {
                BlockPos value = (BlockPos) field.get(packet);
                if (value != null) {
                    BlockPos newValue = subtractOffset ?
                        new BlockPos(value.getX() - (int)offsetX, value.getY() - (int)offsetY, value.getZ() - (int)offsetZ) :
                        new BlockPos(value.getX() + (int)offsetX, value.getY() + (int)offsetY, value.getZ() + (int)offsetZ);
                    field.set(packet, newValue);
                    
                    LOGGER.info("[TrueRusherHackSpoofer] " + fieldName + ": " + value + " → " + newValue);
                }
            }
            
        } catch (Exception e) {
            // Игнорируем ошибки отдельных полей
        }
    }

    /**
     * Получить подходящий офсет для поля
     */
    private static double getOffsetForField(String fieldName) {
        String name = fieldName.toLowerCase();
        if (name.contains("x") || name.equals("dx")) return offsetX;
        if (name.contains("y") || name.equals("dy")) return offsetY;
        if (name.contains("z") || name.equals("dz")) return offsetZ;
        return 0;
    }

    // ===== UI SPOOFING =====
    
    public static double getSpoofedX(double realX) {
        if (currentMode == SpoofMode.OFFSET) {
            return realX - offsetX; // Клиент видит смещённые координаты
        }
        return realX;
    }

    public static double getSpoofedY(double realY) {
        if (currentMode == SpoofMode.OFFSET) {
            return realY - offsetY;
        }
        return realY;
    }

    public static double getSpoofedZ(double realZ) {
        if (currentMode == SpoofMode.OFFSET) {
            return realZ - offsetZ;
        }
        return realZ;
    }

    public static Vec3d getSpoofedPosition(Vec3d realPos) {
        if (currentMode == SpoofMode.OFFSET) {
            return new Vec3d(
                realPos.x - offsetX,
                realPos.y - offsetY,
                realPos.z - offsetZ
            );
        }
        return realPos;
    }

    // ===== GETTERS =====
    
    public static String getModeString() {
        return currentMode.name().toLowerCase();
    }

    public static double getOffsetX() { return offsetX; }
    public static double getOffsetY() { return offsetY; }
    public static double getOffsetZ() { return offsetZ; }
}
