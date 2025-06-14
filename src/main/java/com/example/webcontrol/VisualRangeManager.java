package com.example.webcontrol;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Set;

public class VisualRangeManager {
    private static final Logger LOGGER = LoggerFactory.getLogger("webcontrol-visualrange");
    private static boolean enabled = false;
    private static double range = 100.0; // Default range in blocks
    private static final Set<String> playersInRange = new HashSet<>();
    private static long lastCheckTime = 0;
    private static final long CHECK_INTERVAL = 1000; // Check every 1 second
    
    public static void initialize() {
        LOGGER.info("Initializing Visual Range Manager");
        
        // Register client tick event to check for players
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (!enabled || client.player == null || client.world == null) {
                return;
            }
            
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastCheckTime < CHECK_INTERVAL) {
                return;
            }
            lastCheckTime = currentTime;
            
            checkPlayersInRange(client);
        });
    }
    
    private static void checkPlayersInRange(MinecraftClient client) {
        if (client.player == null || client.world == null) {
            return;
        }
        
        Vec3d playerPos = client.player.getPos();
        Set<String> currentPlayersInRange = new HashSet<>();
        
        // Check all players in the world
        for (PlayerEntity player : client.world.getPlayers()) {
            // Skip the local player
            if (player == client.player) {
                continue;
            }
            
            String playerName = player.getName().getString();
            Vec3d otherPlayerPos = player.getPos();
            double distance = playerPos.distanceTo(otherPlayerPos);
            
            if (distance <= range) {
                currentPlayersInRange.add(playerName);
                
                // If this player wasn't in range before, send enter message
                if (!playersInRange.contains(playerName)) {
                    sendPlayerEnterMessage(client, playerName, distance);
                }
            }
        }
        
        // Check for players who left the range
        for (String playerName : playersInRange) {
            if (!currentPlayersInRange.contains(playerName)) {
                sendPlayerLeaveMessage(client, playerName);
            }
        }
        
        // Update the tracked players
        playersInRange.clear();
        playersInRange.addAll(currentPlayersInRange);
    }
    
    private static void sendPlayerEnterMessage(MinecraftClient client, String playerName, double distance) {
        String message = String.format("§5Visual Range §7%s entered visual range (%.1f blocks) §a[+]",
                                      playerName, distance);
        client.player.sendMessage(Text.literal(message), false);
        LOGGER.info("Player entered visual range: {} at {:.1f} blocks", playerName, distance);
    }

    private static void sendPlayerLeaveMessage(MinecraftClient client, String playerName) {
        String message = String.format("§5Visual Range §7%s left visual range §c[-]", playerName);
        client.player.sendMessage(Text.literal(message), false);
        LOGGER.info("Player left visual range: {}", playerName);
    }
    
    // Public API methods
    public static boolean isEnabled() {
        return enabled;
    }
    
    public static void setEnabled(boolean enabled) {
        VisualRangeManager.enabled = enabled;
        WebControlConfig.getInstance().updateVisualRange(enabled, range);
        
        if (enabled) {
            LOGGER.info("Visual Range enabled with range: {:.1f} blocks", range);
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.player != null) {
                client.player.sendMessage(Text.literal("§5Visual Range §7enabled (Range: " + range + " blocks) §a[+]"), false);
            }
        } else {
            LOGGER.info("Visual Range disabled");
            playersInRange.clear(); // Clear tracked players when disabled
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.player != null) {
                client.player.sendMessage(Text.literal("§5Visual Range §7disabled §c[-]"), false);
            }
        }
    }
    
    public static double getRange() {
        return range;
    }
    
    public static void setRange(double range) {
        VisualRangeManager.range = Math.max(10.0, Math.min(500.0, range)); // Clamp between 10-500 blocks
        WebControlConfig.getInstance().updateVisualRange(enabled, VisualRangeManager.range);
        LOGGER.info("Visual Range set to: {:.1f} blocks", VisualRangeManager.range);
        
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null && enabled) {
            client.player.sendMessage(Text.literal("§5Visual Range §7range set to " + VisualRangeManager.range + " blocks"), false);
        }
    }
    
    public static Set<String> getPlayersInRange() {
        return new HashSet<>(playersInRange);
    }
    
    public static int getPlayerCount() {
        return playersInRange.size();
    }
    
    public static void toggle() {
        setEnabled(!enabled);
    }
    
    // Load settings from config
    public static void loadFromConfig(WebControlConfig config) {
        enabled = config.isVisualRangeEnabled();
        range = config.getVisualRangeDistance();
        LOGGER.info("Loaded Visual Range settings: enabled={}, range={:.1f}", enabled, range);
    }
}
