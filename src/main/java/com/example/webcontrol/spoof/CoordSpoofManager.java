package com.example.webcontrol.spoof;

import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class CoordSpoofManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(CoordSpoofManager.class);

    public enum SpoofMode {
        VANILLA,    // No spoofing
        OFFSET      // Simple offset mode: server->client subtract, client->server add
    }

    private static SpoofMode currentMode = SpoofMode.VANILLA;
    private static double offsetX = 0;
    private static double offsetY = 0;
    private static double offsetZ = 0;

    // Removed unused variables for deleted modes
    
    // Enhanced features
    private static boolean animateCoords = false;
    private static boolean obscureRotations = false;
    private static String spoofedBiome = "";
    private static boolean biomeSpoofingEnabled = false;
    private static Random random = new Random();
    private static long animationStartTime = 0;
    
    // Removed unused features for simplification

    public static SpoofMode getCurrentMode() {
        return currentMode;
    }

    public static void setMode(SpoofMode mode) {
        currentMode = mode;
        logInfo("Coordinate spoofing mode set to: " + mode);
    }

    public static void setOffset(double x, double y, double z) {
        offsetX = x;
        offsetY = y;
        offsetZ = z;
        logInfo("Coordinate offset set to: X=" + x + ", Y=" + y + ", Z=" + z);
    }

    // onF3Opened method removed - not needed for VANILLA/OFFSET modes

    public static Vec3d getSpoofedPosition(Vec3d realPos) {
        Vec3d basePos;

        switch (currentMode) {
            case VANILLA:
                return realPos;
            case OFFSET:
                // Simple offset mode: subtract offset from real coordinates
                // server->client: 150 → 150-100=50 (if offset=100)
                basePos = new Vec3d(
                    realPos.x - offsetX,
                    realPos.y - offsetY,
                    realPos.z - offsetZ
                );
                break;
            default:
                return realPos;
        }

        // Apply animation if enabled
        if (animateCoords && currentMode != SpoofMode.VANILLA) {
            if (animationStartTime == 0) {
                animationStartTime = System.currentTimeMillis();
            }

            long elapsed = System.currentTimeMillis() - animationStartTime;
            double animationFactor = Math.sin(elapsed * 0.001) * 0.1; // Subtle animation

            return new Vec3d(
                basePos.x + animationFactor,
                basePos.y,
                basePos.z + animationFactor
            );
        }

        return basePos;
    }

    public static BlockPos getSpoofedBlockPos(BlockPos realPos) {
        Vec3d spoofedPos = getSpoofedPosition(new Vec3d(realPos.getX(), realPos.getY(), realPos.getZ()));
        return new BlockPos((int)Math.floor(spoofedPos.x), (int)Math.floor(spoofedPos.y), (int)Math.floor(spoofedPos.z));
    }

    public static double getSpoofedX(double realX) {
        if (currentMode == SpoofMode.VANILLA) {
            return realX;
        }
        // OFFSET mode: subtract offset
        return realX - offsetX;
    }

    public static double getSpoofedY(double realY) {
        if (currentMode == SpoofMode.VANILLA) {
            return realY;
        }
        // OFFSET mode: subtract offset
        return realY - offsetY;
    }

    public static double getSpoofedZ(double realZ) {
        if (currentMode == SpoofMode.VANILLA) {
            return realZ;
        }
        // OFFSET mode: subtract offset
        return realZ - offsetZ;
    }

    // Enhanced features methods
    public static void setAnimateCoords(boolean animate) {
        animateCoords = animate;
        if (!animate) {
            animationStartTime = 0;
        }
    }
    
    public static boolean isAnimateCoords() {
        return animateCoords;
    }
    
    public static void setObscureRotations(boolean obscure) {
        obscureRotations = obscure;
    }
    
    public static boolean isObscureRotations() {
        return obscureRotations;
    }
    
    public static void setBiomeSpoof(String biome) {
        spoofedBiome = biome;
        biomeSpoofingEnabled = !biome.isEmpty();
    }
    
    public static String getSpoofedBiome() {
        return biomeSpoofingEnabled ? spoofedBiome : "";
    }
    
    public static boolean isBiomeSpoofingEnabled() {
        return biomeSpoofingEnabled;
    }
    
    // Preset methods removed - only VANILLA and OFFSET modes supported
    
    // Simple stubs for removed methods to prevent compilation errors
    public static boolean shouldUseRapidChange() { return false; }
    public static Vec3d getRapidlyChangingCoords() { return null; }
    public static boolean shouldUseTextReplacement() { return false; }
    public static String getTextCoordinates() { return null; }
    public static boolean isRapidHudMode() { return false; }
    public static boolean isRapidChangeMode() { return false; }
    public static boolean isTextReplaceMode() { return false; }
    public static String getReplacementText() { return "HIDDEN"; }
    public static void setRapidChangeMode(boolean enabled) { /* stub */ }
    public static void setRapidHudMode(boolean enabled) { /* stub */ }
    public static void setTextReplaceMode(boolean enabled) { /* stub */ }
    public static void setReplacementText(String text) { /* stub */ }

    // Getters for web interface
    public static String getModeString() {
        return currentMode.name().toLowerCase();
    }

    public static double getOffsetX() {
        return offsetX;
    }

    public static double getOffsetY() {
        return offsetY;
    }

    public static double getOffsetZ() {
        return offsetZ;
    }

    // Streamer mode methods removed - only VANILLA and OFFSET modes supported

    // Coordinate conversion methods simplified for VANILLA/OFFSET modes only

    // Simplified coordinate preparation methods for VANILLA/OFFSET modes only
    public static double prepareReceiveX(double realX) {
        if (currentMode == SpoofMode.OFFSET) {
            return realX - offsetX; // SUBTRACT offset for incoming
        }
        return realX; // VANILLA mode - no changes
    }

    public static double prepareReceiveZ(double realZ) {
        if (currentMode == SpoofMode.OFFSET) {
            return realZ - offsetZ; // SUBTRACT offset for incoming
        }
        return realZ; // VANILLA mode - no changes
    }

    public static double prepareSendX(double spoofedX) {
        if (currentMode == SpoofMode.OFFSET) {
            return spoofedX + offsetX; // ADD offset for outgoing
        }
        return spoofedX; // VANILLA mode - no changes
    }

    public static double prepareSendZ(double spoofedZ) {
        if (currentMode == SpoofMode.OFFSET) {
            return spoofedZ + offsetZ; // ADD offset for outgoing
        }
        return spoofedZ; // VANILLA mode - no changes
    }

    // Debug logging methods
    public static void logDebug(String message) {
        LOGGER.debug("[CoordSpoof] " + message);
    }

    public static void logInfo(String message) {
        LOGGER.info("[CoordSpoof] " + message);
    }

    public static void logError(String message, Throwable throwable) {
        LOGGER.error("[CoordSpoof] " + message, throwable);
    }

    // Handle incoming packets from server: SUBTRACT offset from coordinates
    // Example: server sends X=150, Z=150 → plugin shows X=150-100=50, Z=150-100=50 to client
    public static void packetReceived(Object packet) {
        try {
            if (currentMode == SpoofMode.VANILLA) {
                return; // No spoofing
            }

            // Only apply simple offset logic for OFFSET mode
            if (currentMode == SpoofMode.OFFSET) {
                handleIncomingPacket(packet);
            }

        } catch (Exception e) {
            logDebug("Error in packetReceived: " + e.getMessage());
        }
    }

    // Handle outgoing packets to server: ADD offset to coordinates
    // Example: client sends X=50, Z=50 → plugin sends X=50+100=150, Z=50+100=150 to server
    public static void packetSend(Object packet) {
        try {
            if (currentMode == SpoofMode.VANILLA) {
                return; // No spoofing
            }

            // Only apply simple offset logic for OFFSET mode
            if (currentMode == SpoofMode.OFFSET) {
                handleOutgoingPacket(packet);
            }

        } catch (Exception e) {
            logDebug("Error in packetSend: " + e.getMessage());
        }
    }

    // Old method removed - replaced with simple offset logic

    // Handle incoming packets: SUBTRACT offset (server → client)
    // server->client: 150 50 150 → plugin: 150-100=50, 50, 150-100=50
    public static void handleIncomingPacket(Object packet) {
        try {
            // TEMPORARILY DISABLE PlayerPositionLookS2CPacket - causes teleportation
            /*
            if (packet instanceof net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket posPacket) {
                subtractOffsetFromPlayerPosition(posPacket);
            }
            */

            // Handle BlockUpdateS2CPacket - block positions from server
            if (packet instanceof net.minecraft.network.packet.s2c.play.BlockUpdateS2CPacket blockPacket) {
                subtractOffsetFromBlockPosition(blockPacket);
            }
            // Add more packet types as needed

        } catch (Exception e) {
            logDebug("Error in handleIncomingPacket: " + e.getMessage());
        }
    }

    // Handle outgoing packets: ADD offset (client → server)
    // client->server: 50+100, 50, 50+100
    public static void handleOutgoingPacket(Object packet) {
        try {
            // Handle PlayerMoveC2SPacket - player movement to server
            if (packet instanceof net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket movePacket) {
                addOffsetToPlayerMovement(movePacket);
            }
            // Add more packet types as needed

        } catch (Exception e) {
            logDebug("Error in handleOutgoingPacket: " + e.getMessage());
        }
    }

    // SUBTRACT offset from incoming player position (server → client)
    // Example: server sends X=150, Z=150 → show X=50, Z=50 to client (offset=100)
    public static void subtractOffsetFromPlayerPosition(net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket packet) {
        try {
            // Get original coordinates from server
            double originalX = packet.getX();
            double originalZ = packet.getZ();

            // SUBTRACT offset to show fake coordinates to client
            // server->client: 150 → 150-100=50 (if offset=100)
            double fakeX = originalX - offsetX;
            double fakeZ = originalZ - offsetZ;

            // Use accessor to set fake coordinates
            var accessor = (com.example.webcontrol.mixin.accessor.PlayerPositionLookS2CPacketAccessor) packet;
            accessor.setX(fakeX);
            accessor.setZ(fakeZ);
            // Y coordinate is NOT modified

            logDebug("PlayerPosition: Server=" + originalX + "," + originalZ + " → Client=" + fakeX + "," + fakeZ + " (offset=" + offsetX + "," + offsetZ + ")");
        } catch (Exception e) {
            logDebug("Error in subtractOffsetFromPlayerPosition: " + e.getMessage());
        }
    }

    // ADD offset to outgoing player movement (client → server)
    // Example: client sends X=50, Z=50 → send X=150, Z=150 to server (offset=100)
    public static void addOffsetToPlayerMovement(net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket packet) {
        try {
            var accessor = (com.example.webcontrol.mixin.accessor.PlayerMoveC2SPacketAccessor) packet;

            // Get fake coordinates from client
            double fakeX = accessor.getX();
            double fakeZ = accessor.getZ();

            // ADD offset to convert fake coordinates back to real coordinates
            // client->server: 50+100=150 (if offset=100)
            double realX = fakeX + offsetX;
            double realZ = fakeZ + offsetZ;

            accessor.setX(realX);
            accessor.setZ(realZ);
            // Y coordinate is not modified

            logDebug("PlayerMovement: Client=" + fakeX + "," + fakeZ + " → Server=" + realX + "," + realZ + " (offset=" + offsetX + "," + offsetZ + ")");
        } catch (Exception e) {
            logDebug("Error in addOffsetToPlayerMovement: " + e.getMessage());
        }
    }

    // SUBTRACT offset from incoming block position (server → client)
    // Example: server sends block at X=150, Z=150 → show at X=50, Z=50 to client
    public static void subtractOffsetFromBlockPosition(net.minecraft.network.packet.s2c.play.BlockUpdateS2CPacket packet) {
        try {
            var accessor = (com.example.webcontrol.mixin.accessor.BlockUpdateS2CPacketAccessor) packet;

            BlockPos originalPos = accessor.getPos();

            // SUBTRACT offset to show fake position to client
            BlockPos fakePos = new BlockPos(
                originalPos.getX() - (int)offsetX,
                originalPos.getY(), // Y is not modified
                originalPos.getZ() - (int)offsetZ
            );

            accessor.setPos(fakePos);

            logDebug("BlockUpdate: Server=" + originalPos + " → Client=" + fakePos + " (offset=" + offsetX + "," + offsetZ + ")");
        } catch (Exception e) {
            logDebug("Error in subtractOffsetFromBlockPosition: " + e.getMessage());
        }
    }

    // Old method removed - replaced with simple offset logic

    private static void handleChunkDataPacket(net.minecraft.network.packet.s2c.play.ChunkDataS2CPacket packet) {
        // Chunk data packets are more complex and may require different handling
        // For now, just log
        logDebug("ChunkData packet received - complex handling needed");
    }

    private static void handleParticlePacket(net.minecraft.network.packet.s2c.play.ParticleS2CPacket packet) {
        try {
            // Particles have coordinates that should be spoofed for visual consistency
            logDebug("Particle packet received - visual spoofing");
        } catch (Exception e) {
            logDebug("Error modifying Particle packet: " + e.getMessage());
        }
    }

    private static void handleSoundPacket(net.minecraft.network.packet.s2c.play.PlaySoundS2CPacket packet) {
        try {
            // Sound packets have coordinates that should be spoofed for audio positioning
            logDebug("Sound packet received - audio positioning spoofing");
        } catch (Exception e) {
            logDebug("Error modifying Sound packet: " + e.getMessage());
        }
    }

    private static void handleExplosionPacket(net.minecraft.network.packet.s2c.play.ExplosionS2CPacket packet) {
        try {
            // Explosion packets have coordinates that should be spoofed for visual effects
            logDebug("Explosion packet received - visual effects spoofing");
        } catch (Exception e) {
            logDebug("Error modifying Explosion packet: " + e.getMessage());
        }
    }
}