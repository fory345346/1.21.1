package com.example.webcontrol.keybinds;

import com.example.webcontrol.PanicQuitManager;
import com.example.webcontrol.VisualRangeManager;
import com.example.webcontrol.spoof.CoordSpoofManager;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Advanced keybind management system for WebControl mod
 * Provides customizable hotkeys for all major features
 */
public class KeybindManager {
    private static final Logger LOGGER = LoggerFactory.getLogger("webcontrol-keybinds");
    private static final Map<String, KeyBinding> keybinds = new HashMap<>();
    private static final Map<String, Long> lastKeyPress = new HashMap<>();
    private static final long KEY_COOLDOWN = 250; // 250ms cooldown between key presses
    
    public static void initialize() {
        LOGGER.info("Initializing KeybindManager");
        
        // Register all keybinds
        registerKeybinds();
        
        // Setup tick event for key processing
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null) return;
            processKeybinds();
        });
        
        LOGGER.info("KeybindManager initialized with {} keybinds", keybinds.size());
    }
    
    private static void registerKeybinds() {
        // Only essential keybinds
        registerKeybind("open_web_interface", "Open Web Interface", GLFW.GLFW_KEY_F9);
        // Note: Panic quit hotkey (Shift+Ctrl+P) is handled in PanicQuitManager
    }
    
    private static void registerKeybind(String id, String description, int key) {
        registerKeybind(id, description, key, 0);
    }
    
    private static void registerKeybind(String id, String description, int key, int modifiers) {
        KeyBinding keyBinding = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.webcontrol." + id,
            InputUtil.Type.KEYSYM,
            key,
            "category.webcontrol"
        ));
        
        keybinds.put(id, keyBinding);
        LOGGER.debug("Registered keybind: {} -> {}", id, description);
    }
    
    private static void processKeybinds() {
        long currentTime = System.currentTimeMillis();
        
        keybinds.forEach((id, keyBinding) -> {
            if (keyBinding.wasPressed()) {
                // Check cooldown
                Long lastPress = lastKeyPress.get(id);
                if (lastPress != null && currentTime - lastPress < KEY_COOLDOWN) {
                    return;
                }
                
                lastKeyPress.put(id, currentTime);
                handleKeybind(id);
            }
        });
    }
    
    private static void handleKeybind(String id) {
        try {
            switch (id) {
                case "open_web_interface":
                    openWebInterface();
                    break;

                default:
                    LOGGER.warn("Unknown keybind: {}", id);
                    break;
            }
        } catch (Exception e) {
            LOGGER.error("Error handling keybind '{}': {}", id, e.getMessage());
        }
    }
    
    private static void toggleSpoofing() {
        CoordSpoofManager.SpoofMode currentMode = CoordSpoofManager.getCurrentMode();
        if (currentMode == CoordSpoofManager.SpoofMode.VANILLA) {
            CoordSpoofManager.setMode(CoordSpoofManager.SpoofMode.OFFSET);
            CoordSpoofManager.setOffset(100, 0, 100); // Default offset
            LOGGER.info("Coordinate spoofing enabled (OFFSET mode)");
        } else {
            CoordSpoofManager.setMode(CoordSpoofManager.SpoofMode.VANILLA);
            LOGGER.info("Coordinate spoofing disabled");
        }
    }
    
    private static void cycleSpoofMode() {
        CoordSpoofManager.SpoofMode currentMode = CoordSpoofManager.getCurrentMode();
        CoordSpoofManager.SpoofMode nextMode;

        // Only cycle between VANILLA and OFFSET modes
        switch (currentMode) {
            case VANILLA:
                nextMode = CoordSpoofManager.SpoofMode.OFFSET;
                break;
            case OFFSET:
                nextMode = CoordSpoofManager.SpoofMode.VANILLA;
                break;
            default:
                nextMode = CoordSpoofManager.SpoofMode.VANILLA;
                break;
        }

        CoordSpoofManager.setMode(nextMode);
        LOGGER.info("Mode: " + nextMode.name());
    }
    
    private static void resetCoordinates() {
        CoordSpoofManager.setOffset(0, 0, 0);
        LOGGER.info("Coordinates reset to 0,0,0");
    }
    
    private static void adjustVisualRange(double delta) {
        double currentRange = VisualRangeManager.getRange();
        double newRange = Math.max(10, Math.min(500, currentRange + delta));
        VisualRangeManager.setRange(newRange);
    }
    
    private static void openWebInterface() {
        try {
            String url = "http://localhost:8080";
            ProcessBuilder pb = new ProcessBuilder();
            
            String os = System.getProperty("os.name").toLowerCase();
            if (os.contains("win")) {
                pb.command("cmd", "/c", "start", url);
            } else if (os.contains("mac")) {
                pb.command("open", url);
            } else {
                pb.command("xdg-open", url);
            }
            
            pb.start();
            LOGGER.info("Opening web interface...");
        } catch (Exception e) {
            LOGGER.error("Failed to open web interface", e);
        }
    }
    
    private static void reloadConfiguration() {
        // Reload configuration logic would go here
        LOGGER.info("Configuration reloaded");
    }
    
    public static KeyBinding getKeybind(String id) {
        return keybinds.get(id);
    }
    
    public static Map<String, KeyBinding> getAllKeybinds() {
        return new HashMap<>(keybinds);
    }
}
