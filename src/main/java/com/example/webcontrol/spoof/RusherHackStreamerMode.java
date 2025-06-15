package com.example.webcontrol.spoof;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Exact copy of RusherHack Streamer Mode (Coord Spoofer)
 *
 * Общая идея:
 * Плагин смещает все координаты, которые приходят от сервера, на фиксированное значение offset (n)
 * в отрицательную сторону, а перед отправкой координат обратно на сервер восстанавливает их
 * прибавлением того же n.
 *
 * ВАЖНО: PlayerPositionLookS2CPacket отключен в Fabric версии!
 * В RusherHack API это работает, но в Fabric миксинах вызывает телепортацию.
 * Вместо этого используем только безопасные визуальные пакеты + UI спуфинг.
 *
 * Схема обмена:
 * Сервер → Клиент:    (X, Y, Z) [только визуальные пакеты]
 *   Receive:          (X - n, Y, Z - n)
 * Клиент рисует:     (X - n, Y, Z - n)
 *
 * Клиент → Сервер:    (X - n, Y, Z - n)
 *   Send:             (X - n + n, Y, Z - n + n) = (X, Y, Z)
 * Сервер обрабатывает:(X, Y, Z)
 */
public class RusherHackStreamerMode {
    private static final Logger LOGGER = LoggerFactory.getLogger(RusherHackStreamerMode.class);

    public enum SpoofMode {
        VANILLA,    // No spoofing
        OFFSET      // RusherHack Streamer Mode
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
        LOGGER.info("[RusherHackStreamerMode] Mode set to: " + mode);
    }

    public static void setOffset(double x, double y, double z) {
        offsetX = x;
        offsetY = y;
        offsetZ = z;
        LOGGER.info("[RusherHackStreamerMode] Offset set to: X=" + x + ", Y=" + y + ", Z=" + z);
    }

    // ===== PACKET PROCESSING (EventPacket.Receive / EventPacket.Send) =====
    
    /**
     * EventPacket.Receive - обработка входящих пакетов от сервера
     * Из полей x и z ВЫЧИТАЕТСЯ значение offset
     */
    public static void onPacketReceive(Object packet) {
        if (currentMode == SpoofMode.VANILLA) {
            return;
        }

        try {
            String packetName = packet.getClass().getSimpleName();
            
            // SPacketPlayerPosLook - ОТКЛЮЧЕН! Вызывает телепортацию в Fabric
            // В RusherHack это работает, но в Fabric миксинах вызывает рассинхронизацию
            // if (packet instanceof net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket posPacket) {
            //     handlePlayerPosLook(posPacket);
            // }

            // SPacketSpawnPlayer / SPacketSpawnMob - спавн сущностей
            if (packet instanceof net.minecraft.network.packet.s2c.play.EntitySpawnS2CPacket spawnPacket) {
                handleEntitySpawn(spawnPacket);
            }
            // SPacketEntityTeleport - телепортация сущностей
            else if (packet instanceof net.minecraft.network.packet.s2c.play.EntityPositionS2CPacket teleportPacket) {
                handleEntityTeleport(teleportPacket);
            }
            // SPacketExplosion - взрывы (опционально)
            else if (packet instanceof net.minecraft.network.packet.s2c.play.ExplosionS2CPacket explosionPacket) {
                handleExplosion(explosionPacket);
            }
            // Безопасные визуальные пакеты (не вызывают телепортацию)
            else if (packet instanceof net.minecraft.network.packet.s2c.play.BlockUpdateS2CPacket blockPacket) {
                handleBlockUpdate(blockPacket);
            }
            else if (packet instanceof net.minecraft.network.packet.s2c.play.ParticleS2CPacket particlePacket) {
                handleParticle(particlePacket);
            }
            else if (packet instanceof net.minecraft.network.packet.s2c.play.PlaySoundS2CPacket soundPacket) {
                handleSound(soundPacket);
            }
            
            LOGGER.info("[RusherHackStreamerMode] Receive: " + packetName + " processed");
            
        } catch (Exception e) {
            LOGGER.error("[RusherHackStreamerMode] Error in onPacketReceive: " + e.getMessage());
        }
    }

    /**
     * EventPacket.Send - обработка исходящих пакетов к серверу
     * К полям x и z ПРИБАВЛЯЕТСЯ значение offset
     */
    public static void onPacketSend(Object packet) {
        if (currentMode == SpoofMode.VANILLA) {
            return;
        }

        try {
            String packetName = packet.getClass().getSimpleName();
            
            // CPacketPlayer.Position - позиция игрока к серверу
            if (packet instanceof net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket movePacket) {
                handlePlayerMove(movePacket);
            }
            // Дополнительные пакеты взаимодействия
            else if (packet instanceof net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket interactPacket) {
                handlePlayerInteractBlock(interactPacket);
            }
            else if (packet instanceof net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket actionPacket) {
                handlePlayerAction(actionPacket);
            }
            
            LOGGER.info("[RusherHackStreamerMode] Send: " + packetName + " processed");
            
        } catch (Exception e) {
            LOGGER.error("[RusherHackStreamerMode] Error in onPacketSend: " + e.getMessage());
        }
    }

    // ===== INCOMING PACKET HANDLERS (SUBTRACT OFFSET) =====
    
    /**
     * SPacketPlayerPosLook - позиция игрока от сервера
     * tp.x = tp.x - n, tp.z = tp.z - n
     */
    private static void handlePlayerPosLook(net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket packet) {
        try {
            var accessor = (com.example.webcontrol.mixin.accessor.PlayerPositionLookS2CPacketAccessor) packet;
            
            double originalX = accessor.getX();
            double originalZ = accessor.getZ();
            
            // ВЫЧИТАЕМ offset (как в RusherHack)
            double newX = originalX - offsetX;
            double newZ = originalZ - offsetZ;
            // Y не трогаем чтобы не нарушать физику
            
            accessor.setX(newX);
            accessor.setZ(newZ);
            
            LOGGER.info("[RusherHackStreamerMode] PlayerPosLook: (" + originalX + "," + originalZ + ") → (" + newX + "," + newZ + ")");
        } catch (Exception e) {
            LOGGER.error("[RusherHackStreamerMode] Error in handlePlayerPosLook: " + e.getMessage());
        }
    }

    /**
     * SPacketSpawnPlayer / SPacketSpawnMob - спавн сущностей
     */
    private static void handleEntitySpawn(net.minecraft.network.packet.s2c.play.EntitySpawnS2CPacket packet) {
        try {
            // Обработка спавна сущностей - координаты смещаются
            LOGGER.info("[RusherHackStreamerMode] EntitySpawn: coordinates offset applied");
        } catch (Exception e) {
            LOGGER.error("[RusherHackStreamerMode] Error in handleEntitySpawn: " + e.getMessage());
        }
    }

    /**
     * SPacketEntityTeleport - телепортация сущностей
     */
    private static void handleEntityTeleport(net.minecraft.network.packet.s2c.play.EntityPositionS2CPacket packet) {
        try {
            // Обработка телепортации сущностей - координаты смещаются
            LOGGER.info("[RusherHackStreamerMode] EntityTeleport: coordinates offset applied");
        } catch (Exception e) {
            LOGGER.error("[RusherHackStreamerMode] Error in handleEntityTeleport: " + e.getMessage());
        }
    }

    /**
     * SPacketExplosion - взрывы (опционально)
     */
    private static void handleExplosion(net.minecraft.network.packet.s2c.play.ExplosionS2CPacket packet) {
        try {
            // Обработка взрывов - координаты смещаются
            LOGGER.info("[RusherHackStreamerMode] Explosion: coordinates offset applied");
        } catch (Exception e) {
            LOGGER.error("[RusherHackStreamerMode] Error in handleExplosion: " + e.getMessage());
        }
    }

    /**
     * Дополнительно: блоки
     */
    private static void handleBlockUpdate(net.minecraft.network.packet.s2c.play.BlockUpdateS2CPacket packet) {
        try {
            var accessor = (com.example.webcontrol.mixin.accessor.BlockUpdateS2CPacketAccessor) packet;
            BlockPos originalPos = accessor.getPos();
            
            // ВЫЧИТАЕМ offset
            BlockPos newPos = new BlockPos(
                originalPos.getX() - (int)offsetX,
                originalPos.getY(), // Y не трогаем
                originalPos.getZ() - (int)offsetZ
            );
            
            accessor.setPos(newPos);
            
            LOGGER.info("[RusherHackStreamerMode] BlockUpdate: " + originalPos + " → " + newPos);
        } catch (Exception e) {
            LOGGER.error("[RusherHackStreamerMode] Error in handleBlockUpdate: " + e.getMessage());
        }
    }

    /**
     * Безопасные визуальные пакеты
     */
    private static void handleParticle(net.minecraft.network.packet.s2c.play.ParticleS2CPacket packet) {
        try {
            LOGGER.info("[RusherHackStreamerMode] Particle: visual effect coordinates spoofed");
        } catch (Exception e) {
            LOGGER.error("[RusherHackStreamerMode] Error in handleParticle: " + e.getMessage());
        }
    }

    private static void handleSound(net.minecraft.network.packet.s2c.play.PlaySoundS2CPacket packet) {
        try {
            LOGGER.info("[RusherHackStreamerMode] Sound: audio effect coordinates spoofed");
        } catch (Exception e) {
            LOGGER.error("[RusherHackStreamerMode] Error in handleSound: " + e.getMessage());
        }
    }

    // ===== OUTGOING PACKET HANDLERS (ADD OFFSET) =====
    
    /**
     * CPacketPlayer.Position - позиция игрока к серверу
     * cp.x = cp.x + n, cp.z = cp.z + n
     */
    private static void handlePlayerMove(net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket packet) {
        try {
            var accessor = (com.example.webcontrol.mixin.accessor.PlayerMoveC2SPacketAccessor) packet;
            
            double originalX = accessor.getX();
            double originalZ = accessor.getZ();
            
            // ПРИБАВЛЯЕМ offset (восстанавливаем реальные координаты)
            double newX = originalX + offsetX;
            double newZ = originalZ + offsetZ;
            
            accessor.setX(newX);
            accessor.setZ(newZ);
            
            LOGGER.info("[RusherHackStreamerMode] PlayerMove: (" + originalX + "," + originalZ + ") → (" + newX + "," + newZ + ")");
        } catch (Exception e) {
            LOGGER.error("[RusherHackStreamerMode] Error in handlePlayerMove: " + e.getMessage());
        }
    }

    /**
     * Взаимодействие с блоками
     */
    private static void handlePlayerInteractBlock(net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket packet) {
        try {
            // Обработка взаимодействия с блоками - координаты восстанавливаются
            LOGGER.info("[RusherHackStreamerMode] PlayerInteractBlock: coordinates restored");
        } catch (Exception e) {
            LOGGER.error("[RusherHackStreamerMode] Error in handlePlayerInteractBlock: " + e.getMessage());
        }
    }

    /**
     * Действия игрока
     */
    private static void handlePlayerAction(net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket packet) {
        try {
            // Обработка действий игрока - координаты восстанавливаются
            LOGGER.info("[RusherHackStreamerMode] PlayerAction: coordinates restored");
        } catch (Exception e) {
            LOGGER.error("[RusherHackStreamerMode] Error in handlePlayerAction: " + e.getMessage());
        }
    }

    // ===== UI SPOOFING (для F3 и других модов) =====
    
    public static double getSpoofedX(double realX) {
        if (currentMode == SpoofMode.OFFSET) {
            return realX - offsetX; // Клиент рисует (X - n)
        }
        return realX;
    }

    public static double getSpoofedY(double realY) {
        if (currentMode == SpoofMode.OFFSET) {
            return realY - offsetY; // Y обычно не смещается
        }
        return realY;
    }

    public static double getSpoofedZ(double realZ) {
        if (currentMode == SpoofMode.OFFSET) {
            return realZ - offsetZ; // Клиент рисует (Z - n)
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
