package com.example.webcontrol.spoof;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

/**
 * Universal Coordinate Spoofer that processes ALL coordinate fields in packets
 * Based on exact RusherHack plugin logic:
 * 
 * 1. EventPacket.Receive: subtract offset from ALL coordinate fields
 * 2. EventPacket.Send: add offset to ALL coordinate fields
 * 
 * This processes x, y, z fields in entities, players, blocks, etc.
 */
public class UniversalCoordSpoofer {
    private static final Logger LOGGER = LoggerFactory.getLogger(UniversalCoordSpoofer.class);

    public enum SpoofMode {
        VANILLA,    // No spoofing
        OFFSET      // Universal offset mode like RusherHack plugin
    }

    private static SpoofMode currentMode = SpoofMode.VANILLA;
    private static double offsetX = 0;
    private static double offsetY = 0;
    private static double offsetZ = 0;

    // ===== BASIC CONFIGURATION =====
    
    public static SpoofMode getCurrentMode() {
        return currentMode;
    }

    public static void setMode(SpoofMode mode) {
        currentMode = mode;
        LOGGER.info("[UniversalCoordSpoofer] Mode set to: " + mode);
    }

    public static void setOffset(double x, double y, double z) {
        offsetX = x;
        offsetY = y;
        offsetZ = z;
        LOGGER.info("[UniversalCoordSpoofer] Offset set to: X=" + x + ", Y=" + y + ", Z=" + z);
    }

    // ===== UNIVERSAL PACKET PROCESSING =====
    
    /**
     * Process incoming packet: SUBTRACT offset from ALL coordinate fields
     * Like RusherHack EventPacket.Receive
     */
    public static void processIncomingPacket(Object packet) {
        if (currentMode == SpoofMode.VANILLA) {
            return;
        }

        try {
            LOGGER.info("[UniversalCoordSpoofer] Processing incoming: " + packet.getClass().getSimpleName());
            
            // Process all coordinate fields in the packet
            processCoordinateFields(packet, true); // true = subtract offset
            
        } catch (Exception e) {
            LOGGER.error("[UniversalCoordSpoofer] Error processing incoming packet: " + e.getMessage());
        }
    }

    /**
     * Process outgoing packet: ADD offset to ALL coordinate fields
     * Like RusherHack EventPacket.Send
     */
    public static void processOutgoingPacket(Object packet) {
        if (currentMode == SpoofMode.VANILLA) {
            return;
        }

        try {
            LOGGER.info("[UniversalCoordSpoofer] Processing outgoing: " + packet.getClass().getSimpleName());
            
            // Process all coordinate fields in the packet
            processCoordinateFields(packet, false); // false = add offset
            
        } catch (Exception e) {
            LOGGER.error("[UniversalCoordSpoofer] Error processing outgoing packet: " + e.getMessage());
        }
    }

    /**
     * Process all coordinate fields in a packet using reflection
     * This finds and modifies x, y, z fields automatically
     */
    private static void processCoordinateFields(Object packet, boolean subtractOffset) {
        Class<?> clazz = packet.getClass();
        
        // Process all fields in the packet
        Field[] fields = clazz.getDeclaredFields();
        for (Field field : fields) {
            try {
                field.setAccessible(true);
                
                // Skip static and final fields
                if (Modifier.isStatic(field.getModifiers()) || Modifier.isFinal(field.getModifiers())) {
                    continue;
                }
                
                String fieldName = field.getName().toLowerCase();
                Class<?> fieldType = field.getType();
                
                // Process coordinate fields
                if (isCoordinateField(fieldName, fieldType)) {
                    processCoordinateField(packet, field, subtractOffset);
                }
                
            } catch (Exception e) {
                // Ignore field processing errors
            }
        }
    }

    /**
     * Check if a field represents coordinates
     */
    private static boolean isCoordinateField(String fieldName, Class<?> fieldType) {
        // Check for coordinate field names
        boolean isCoordName = fieldName.contains("x") || fieldName.contains("y") || fieldName.contains("z") ||
                             fieldName.contains("pos") || fieldName.contains("position") ||
                             fieldName.contains("coord") || fieldName.contains("location");
        
        // Check for coordinate field types
        boolean isCoordType = fieldType == double.class || fieldType == Double.class ||
                             fieldType == float.class || fieldType == Float.class ||
                             fieldType == int.class || fieldType == Integer.class ||
                             fieldType == Vec3d.class || fieldType == BlockPos.class;
        
        return isCoordName && isCoordType;
    }

    /**
     * Process a single coordinate field
     */
    private static void processCoordinateField(Object packet, Field field, boolean subtractOffset) {
        try {
            Class<?> fieldType = field.getType();
            String fieldName = field.getName();
            
            if (fieldType == double.class || fieldType == Double.class) {
                double value = field.getDouble(packet);
                double newValue = subtractOffset ? 
                    value - getOffsetForField(fieldName) : 
                    value + getOffsetForField(fieldName);
                field.setDouble(packet, newValue);
                
                LOGGER.info("[UniversalCoordSpoofer] " + fieldName + ": " + value + " → " + newValue);
                
            } else if (fieldType == float.class || fieldType == Float.class) {
                float value = field.getFloat(packet);
                float newValue = subtractOffset ? 
                    value - (float)getOffsetForField(fieldName) : 
                    value + (float)getOffsetForField(fieldName);
                field.setFloat(packet, newValue);
                
                LOGGER.info("[UniversalCoordSpoofer] " + fieldName + ": " + value + " → " + newValue);
                
            } else if (fieldType == int.class || fieldType == Integer.class) {
                int value = field.getInt(packet);
                int newValue = subtractOffset ? 
                    value - (int)getOffsetForField(fieldName) : 
                    value + (int)getOffsetForField(fieldName);
                field.setInt(packet, newValue);
                
                LOGGER.info("[UniversalCoordSpoofer] " + fieldName + ": " + value + " → " + newValue);
                
            } else if (fieldType == Vec3d.class) {
                Vec3d value = (Vec3d) field.get(packet);
                if (value != null) {
                    Vec3d newValue = subtractOffset ?
                        new Vec3d(value.x - offsetX, value.y - offsetY, value.z - offsetZ) :
                        new Vec3d(value.x + offsetX, value.y + offsetY, value.z + offsetZ);
                    field.set(packet, newValue);
                    
                    LOGGER.info("[UniversalCoordSpoofer] " + fieldName + ": " + value + " → " + newValue);
                }
                
            } else if (fieldType == BlockPos.class) {
                BlockPos value = (BlockPos) field.get(packet);
                if (value != null) {
                    BlockPos newValue = subtractOffset ?
                        new BlockPos(value.getX() - (int)offsetX, value.getY() - (int)offsetY, value.getZ() - (int)offsetZ) :
                        new BlockPos(value.getX() + (int)offsetX, value.getY() + (int)offsetY, value.getZ() + (int)offsetZ);
                    field.set(packet, newValue);
                    
                    LOGGER.info("[UniversalCoordSpoofer] " + fieldName + ": " + value + " → " + newValue);
                }
            }
            
        } catch (Exception e) {
            // Ignore individual field errors
        }
    }

    /**
     * Get the appropriate offset for a field based on its name
     */
    private static double getOffsetForField(String fieldName) {
        String name = fieldName.toLowerCase();
        if (name.contains("x")) return offsetX;
        if (name.contains("y")) return offsetY;
        if (name.contains("z")) return offsetZ;
        return 0; // Default to no offset
    }

    // ===== UI SPOOFING METHODS =====
    
    public static double getSpoofedX(double realX) {
        if (currentMode == SpoofMode.OFFSET) {
            return realX - offsetX; // Subtract for UI display
        }
        return realX;
    }

    public static double getSpoofedY(double realY) {
        if (currentMode == SpoofMode.OFFSET) {
            return realY - offsetY; // Subtract for UI display
        }
        return realY;
    }

    public static double getSpoofedZ(double realZ) {
        if (currentMode == SpoofMode.OFFSET) {
            return realZ - offsetZ; // Subtract for UI display
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
