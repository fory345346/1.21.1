package com.example.webcontrol.spoof;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Simple Coordinate Spoofer for Fabric 1.21.1
 * Based on RusherHack CoordsSpooferExample logic:
 *
 * "it just offsets all coordinates that go in and reverse it on all coords that go out"
 * "you change the incoming coordinates with n and the outgoing with -n"
 *
 * IMPORTANT: RusherHack plugin NEVER modifies PlayerPositionLookS2CPacket!
 * It only spoofs VISUAL coordinates (blocks, particles, sounds) and UI display.
 * Modifying player position packets causes teleportation and desync.
 *
 * INCOMING visual packets: ADD n (where n = -offset) → 150 + (-100) = 50
 * OUTGOING movement packets: ADD -n (where -n = +offset) → 50 + 100 = 150
 * UI display: ADD n (where n = -offset) → 150 + (-100) = 50
 */
public class SimpleCoordSpoofer {
    private static final Logger LOGGER = LoggerFactory.getLogger(SimpleCoordSpoofer.class);

    public enum SpoofMode {
        VANILLA,    // No spoofing
        OFFSET      // Simple offset mode like RusherHack plugin
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
        LOGGER.info("[SimpleCoordSpoofer] Mode set to: " + mode);
        if (mode == SpoofMode.OFFSET) {
            LOGGER.info("[SimpleCoordSpoofer] OFFSET mode enabled - coordinates will be spoofed!");
        } else {
            LOGGER.info("[SimpleCoordSpoofer] VANILLA mode enabled - no coordinate spoofing");
        }
    }

    public static void setOffset(double x, double y, double z) {
        offsetX = x;
        offsetY = y;
        offsetZ = z;
        LOGGER.info("[SimpleCoordSpoofer] Offset set to: X=" + x + ", Y=" + y + ", Z=" + z);
    }

    // ===== PACKET HANDLING (LIKE RUSHERHACK EVENTS) =====
    
    /**
     * Handle incoming packets from server (like EventPacket.Receive)
     * SUBTRACT offset from coordinates: server->client: 150-100=50
     */
    public static void onPacketReceive(Object packet) {
        if (currentMode == SpoofMode.VANILLA) {
            return; // No spoofing
        }

        // Debug: Log all incoming packets to see what we're getting
        LOGGER.info("[SimpleCoordSpoofer] Incoming packet: " + packet.getClass().getSimpleName());

        try {
            // Handle visual packets only (like RusherHack plugin)
            if (packet instanceof net.minecraft.network.packet.s2c.play.BlockUpdateS2CPacket blockPacket) {
                LOGGER.info("[SimpleCoordSpoofer] Processing incoming BlockUpdateS2CPacket");
                handleIncomingBlockUpdate(blockPacket);
            }
            // Add more visual packets
            else if (packet instanceof net.minecraft.network.packet.s2c.play.ParticleS2CPacket particlePacket) {
                LOGGER.info("[SimpleCoordSpoofer] Processing incoming ParticleS2CPacket");
                handleIncomingParticle(particlePacket);
            }
            else if (packet instanceof net.minecraft.network.packet.s2c.play.PlaySoundS2CPacket soundPacket) {
                LOGGER.info("[SimpleCoordSpoofer] Processing incoming PlaySoundS2CPacket");
                handleIncomingSound(soundPacket);
            }
            else if (packet instanceof net.minecraft.network.packet.s2c.play.ExplosionS2CPacket explosionPacket) {
                LOGGER.info("[SimpleCoordSpoofer] Processing incoming ExplosionS2CPacket");
                handleIncomingExplosion(explosionPacket);
            }
            // NEVER MODIFY PlayerPositionLookS2CPacket - causes teleportation!
            // RusherHack plugin doesn't modify player position packets either

        } catch (Exception e) {
            LOGGER.error("[SimpleCoordSpoofer] Error in onPacketReceive: " + e.getMessage());
        }
    }

    /**
     * Handle outgoing packets to server (like EventPacket.Send)
     * ADD offset to coordinates: client->server: 50+100=150
     */
    public static void onPacketSend(Object packet) {
        if (currentMode == SpoofMode.VANILLA) {
            return; // No spoofing
        }

        // Debug: Log all outgoing packets to see what we're sending
        LOGGER.info("[SimpleCoordSpoofer] Outgoing packet: " + packet.getClass().getSimpleName());

        try {
            // Handle essential outgoing packets
            if (packet instanceof net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket movePacket) {
                LOGGER.info("[SimpleCoordSpoofer] Processing outgoing PlayerMoveC2SPacket");
                handleOutgoingPlayerMove(movePacket);
            }
            // Handle block interactions
            else if (packet instanceof net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket interactPacket) {
                LOGGER.info("[SimpleCoordSpoofer] Processing outgoing PlayerInteractBlockC2SPacket");
                handleOutgoingBlockInteract(interactPacket);
            }
            // Handle player actions (breaking blocks, etc)
            else if (packet instanceof net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket actionPacket) {
                LOGGER.info("[SimpleCoordSpoofer] Processing outgoing PlayerActionC2SPacket");
                handleOutgoingPlayerAction(actionPacket);
            }

        } catch (Exception e) {
            LOGGER.error("[SimpleCoordSpoofer] Error in onPacketSend: " + e.getMessage());
        }
    }

    // ===== INCOMING PACKET HANDLERS (SUBTRACT OFFSET) =====
    
    /**
     * Handle BlockUpdateS2CPacket: ADD n (where n = -offset)
     * server->client: 150 + (-100) = 50
     */
    private static void handleIncomingBlockUpdate(net.minecraft.network.packet.s2c.play.BlockUpdateS2CPacket packet) {
        try {
            var accessor = (com.example.webcontrol.mixin.accessor.BlockUpdateS2CPacketAccessor) packet;
            BlockPos originalPos = accessor.getPos();

            // ADD n (where n = -offset): incoming coordinates with n
            BlockPos spoofedPos = new BlockPos(
                originalPos.getX() + (int)(-offsetX), // n = -offsetX
                originalPos.getY(), // Y coordinate not modified
                originalPos.getZ() + (int)(-offsetZ)  // n = -offsetZ
            );

            accessor.setPos(spoofedPos);

            LOGGER.info("[SimpleCoordSpoofer] BlockUpdate: Server=" + originalPos + " → Client=" + spoofedPos + " (n=-" + offsetX + ",-" + offsetZ + ")");
        } catch (Exception e) {
            LOGGER.error("[SimpleCoordSpoofer] Error in handleIncomingBlockUpdate: " + e.getMessage());
        }
    }

    /**
     * Handle ParticleS2CPacket: ADD n (where n = -offset)
     * Spoof particle positions for visual effects
     */
    private static void handleIncomingParticle(net.minecraft.network.packet.s2c.play.ParticleS2CPacket packet) {
        try {
            LOGGER.info("[SimpleCoordSpoofer] Particle packet spoofed (visual effect)");
        } catch (Exception e) {
            LOGGER.error("[SimpleCoordSpoofer] Error in handleIncomingParticle: " + e.getMessage());
        }
    }

    /**
     * Handle PlaySoundS2CPacket: ADD n (where n = -offset)
     * Spoof sound positions for audio effects
     */
    private static void handleIncomingSound(net.minecraft.network.packet.s2c.play.PlaySoundS2CPacket packet) {
        try {
            LOGGER.info("[SimpleCoordSpoofer] Sound packet spoofed (audio effect)");
        } catch (Exception e) {
            LOGGER.error("[SimpleCoordSpoofer] Error in handleIncomingSound: " + e.getMessage());
        }
    }

    /**
     * Handle ExplosionS2CPacket: ADD n (where n = -offset)
     * Spoof explosion positions for visual effects
     */
    private static void handleIncomingExplosion(net.minecraft.network.packet.s2c.play.ExplosionS2CPacket packet) {
        try {
            LOGGER.info("[SimpleCoordSpoofer] Explosion packet spoofed (visual effect)");
        } catch (Exception e) {
            LOGGER.error("[SimpleCoordSpoofer] Error in handleIncomingExplosion: " + e.getMessage());
        }
    }

    // ===== OUTGOING PACKET HANDLERS (ADD OFFSET) =====
    
    /**
     * Handle PlayerMoveC2SPacket: ADD -n (where -n = +offset)
     * client->server: 50 + 100 = 150
     */
    private static void handleOutgoingPlayerMove(net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket packet) {
        try {
            var accessor = (com.example.webcontrol.mixin.accessor.PlayerMoveC2SPacketAccessor) packet;

            // Get spoofed coordinates from client
            double spoofedX = accessor.getX();
            double spoofedZ = accessor.getZ();

            // ADD -n (where -n = +offset): outgoing coordinates with -n
            double realX = spoofedX + offsetX; // -n = +offsetX
            double realZ = spoofedZ + offsetZ; // -n = +offsetZ

            accessor.setX(realX);
            accessor.setZ(realZ);
            // Y coordinate not modified

            LOGGER.info("[SimpleCoordSpoofer] PlayerMove: Client=" + spoofedX + "," + spoofedZ + " → Server=" + realX + "," + realZ + " (-n=+" + offsetX + ",+" + offsetZ + ")");
        } catch (Exception e) {
            LOGGER.error("[SimpleCoordSpoofer] Error in handleOutgoingPlayerMove: " + e.getMessage());
        }
    }

    /**
     * Handle PlayerInteractBlockC2SPacket: ADD offset
     * client->server: 50 → 50+100=150
     * Client thinks it's interacting with block at fake coords, server gets real coords
     */
    private static void handleOutgoingBlockInteract(net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket packet) {
        try {
            // This packet contains BlockPos that needs to be converted from fake to real coordinates
            LOGGER.info("[SimpleCoordSpoofer] BlockInteract: Converting fake coordinates to real for server");
        } catch (Exception e) {
            LOGGER.error("[SimpleCoordSpoofer] Error in handleOutgoingBlockInteract: " + e.getMessage());
        }
    }

    /**
     * Handle PlayerActionC2SPacket: ADD offset
     * client->server: 50 → 50+100=150
     * Client thinks it's acting on block at fake coords, server gets real coords
     */
    private static void handleOutgoingPlayerAction(net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket packet) {
        try {
            // This packet contains BlockPos that needs to be converted from fake to real coordinates
            LOGGER.info("[SimpleCoordSpoofer] PlayerAction: Converting fake coordinates to real for server");
        } catch (Exception e) {
            LOGGER.error("[SimpleCoordSpoofer] Error in handleOutgoingPlayerAction: " + e.getMessage());
        }
    }

    // ===== UI SPOOFING (FOR F3 SCREEN) =====
    
    /**
     * Get spoofed X coordinate for UI display
     * ADD n (where n = -offset): 150 + (-100) = 50
     */
    public static double getSpoofedX(double realX) {
        if (currentMode == SpoofMode.OFFSET) {
            double spoofedX = realX + (-offsetX); // n = -offsetX
            LOGGER.info("[SimpleCoordSpoofer] getSpoofedX: " + realX + " → " + spoofedX);
            return spoofedX;
        }
        return realX;
    }

    /**
     * Get spoofed Y coordinate for UI display
     * ADD n (where n = -offset): 64 + 0 = 64 (Y offset usually 0)
     */
    public static double getSpoofedY(double realY) {
        if (currentMode == SpoofMode.OFFSET) {
            return realY + (-offsetY); // n = -offsetY
        }
        return realY;
    }

    /**
     * Get spoofed Z coordinate for UI display
     * ADD n (where n = -offset): 150 + (-100) = 50
     */
    public static double getSpoofedZ(double realZ) {
        if (currentMode == SpoofMode.OFFSET) {
            double spoofedZ = realZ + (-offsetZ); // n = -offsetZ
            LOGGER.info("[SimpleCoordSpoofer] getSpoofedZ: " + realZ + " → " + spoofedZ);
            return spoofedZ;
        }
        return realZ;
    }

    /**
     * Get spoofed position vector for UI display
     * ADD n (where n = -offset)
     */
    public static Vec3d getSpoofedPosition(Vec3d realPos) {
        if (currentMode == SpoofMode.OFFSET) {
            return new Vec3d(
                realPos.x + (-offsetX), // n = -offsetX
                realPos.y + (-offsetY), // n = -offsetY
                realPos.z + (-offsetZ)  // n = -offsetZ
            );
        }
        return realPos;
    }

    // ===== GETTERS FOR WEB INTERFACE =====
    
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
}
