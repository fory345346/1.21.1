package com.example.webcontrol;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PanicQuitManager {
    private static final Logger LOGGER = LoggerFactory.getLogger("webcontrol-panic");
    private static boolean realCoordsShown = false;
    private static KeyBinding panicQuitKey;
    private static long lastKeyPressTime = 0;
    private static final long KEY_PRESS_COOLDOWN = 1000; // 1 second cooldown
    private static boolean hasCheckedWorldJoin = false;
    
    public static void initialize() {
        // Load saved panic quit state from config
        WebControlConfig config = WebControlConfig.getInstance();
        // Register key binding: Shift + Ctrl + P
        panicQuitKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.webcontrol.panicquit",
            InputUtil.Type.KEYSYM,
            InputUtil.UNKNOWN_KEY.getCode(), // No default key, we'll handle combination manually
            "category.webcontrol"
        ));
        
        // Register client tick event to check for key combination and world join
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            // Check if player just joined a world (world exists but we haven't checked yet)
            if (client.player != null && client.world != null && !hasCheckedWorldJoin) {
                hasCheckedWorldJoin = true;
                onPlayerJoinWorld();
            } else if (client.player == null || client.world == null) {
                hasCheckedWorldJoin = false; // Reset when leaving world
            }

            if (client.player == null) return;

            // Check for Shift + Ctrl + P combination manually
            boolean shiftPressed = InputUtil.isKeyPressed(client.getWindow().getHandle(), GLFW.GLFW_KEY_LEFT_SHIFT) ||
                                 InputUtil.isKeyPressed(client.getWindow().getHandle(), GLFW.GLFW_KEY_RIGHT_SHIFT);
            boolean ctrlPressed = InputUtil.isKeyPressed(client.getWindow().getHandle(), GLFW.GLFW_KEY_LEFT_CONTROL) ||
                                InputUtil.isKeyPressed(client.getWindow().getHandle(), GLFW.GLFW_KEY_RIGHT_CONTROL);
            boolean pPressed = InputUtil.isKeyPressed(client.getWindow().getHandle(), GLFW.GLFW_KEY_P);

            // Check if all three keys are pressed simultaneously
            if (shiftPressed && ctrlPressed && pPressed) {
                long currentTime = System.currentTimeMillis();
                if (currentTime - lastKeyPressTime > KEY_PRESS_COOLDOWN) {
                    togglePanicQuit();
                    lastKeyPressTime = currentTime;
                    LOGGER.info("Panic quit hotkey activated: Shift+Ctrl+P");
                }
            }
        });
    }
    
    private static void togglePanicQuit() {
        WebControlConfig config = WebControlConfig.getInstance();
        boolean newState = !config.isPanicQuitEnabled();
        config.updatePanicQuit(newState);

        MinecraftClient client = MinecraftClient.getInstance();

        if (newState) {
            LOGGER.info("Panic Quit ACTIVATED");

            // Play warning sound
            if (client.player != null) {
                client.player.playSound(SoundEvents.BLOCK_NOTE_BLOCK_PLING.value(), 1.0f, 0.5f);
            }

            // Send message to player
            if (client.player != null) {
                client.player.sendMessage(Text.literal("§c§lPANIC QUIT ACTIVATED"), false);
            }

            // Check if real coordinates are being shown
            checkRealCoordinates();

            // If real coordinates are shown, disconnect immediately
            if (realCoordsShown) {
                LOGGER.warn("Real coordinates detected immediately upon activation - triggering panic quit!");
                performPanicQuit();
            }
        } else {
            LOGGER.info("Panic Quit DEACTIVATED");

            // Play deactivation sound
            if (client.player != null) {
                client.player.playSound(SoundEvents.BLOCK_NOTE_BLOCK_CHIME.value(), 1.0f, 1.0f);
            }

            // Send message to player
            if (client.player != null) {
                client.player.sendMessage(Text.literal("§a§lPanic Quit Deactivated"), false);
            }
        }
    }
    
    private static void checkRealCoordinates() {
        // Check if real coordinates are being shown based on the modern CoordSpoofManager system
        // Real coordinates are shown when:
        // 1. Spoofing mode is set to VANILLA (no spoofing)
        // 2. AND text replacement mode is not active (which would hide coordinates completely)

        com.example.webcontrol.spoof.CoordSpoofManager.SpoofMode currentMode =
            com.example.webcontrol.spoof.CoordSpoofManager.getCurrentMode();

        boolean textReplacementActive = com.example.webcontrol.spoof.CoordSpoofManager.shouldUseTextReplacement();

        // Real coordinates are shown if spoofing is disabled (VANILLA mode)
        // and text replacement is not hiding the coordinates
        boolean previousStatus = realCoordsShown;
        realCoordsShown = (currentMode == com.example.webcontrol.spoof.CoordSpoofManager.SpoofMode.VANILLA)
                         && !textReplacementActive;

        // Log coordinate status changes for debugging
        if (previousStatus != realCoordsShown) {
            LOGGER.info("Coordinate status changed: realCoordsShown={}, spoofMode={}, textReplacement={}",
                       realCoordsShown, currentMode, textReplacementActive);
        }
    }
    
    private static void performPanicQuit() {
        LOGGER.warn("PANIC QUIT TRIGGERED! Real coordinates detected - disconnecting from server!");

        MinecraftClient client = MinecraftClient.getInstance();

        // Play panic sound
        if (client.player != null) {
            client.player.playSound(SoundEvents.ENTITY_ENDERMAN_SCREAM, 1.0f, 0.8f);
        }

        // Send emergency message to player
        if (client.player != null) {
            client.player.sendMessage(Text.literal("§4§l⚠ PANIC QUIT: Real coordinates detected!"), false);
        }

        // NOTE: Panic quit remains enabled after disconnect - this is intentional
        // The user needs to manually disable it to prevent repeated disconnects
        LOGGER.info("Panic quit remains ACTIVE after disconnect - disable manually to rejoin safely");

        // Disconnect from server
        client.execute(() -> {
            if (client.world != null) {
                client.world.disconnect();
            }
            client.setScreen(new net.minecraft.client.gui.screen.TitleScreen());
        });
    }
    
    public static boolean isPanicQuitEnabled() {
        return WebControlConfig.getInstance().isPanicQuitEnabled();
    }
    
    public static boolean areRealCoordsShown() {
        return realCoordsShown;
    }
    
    public static void updateRealCoordsStatus() {
        if (isPanicQuitEnabled()) {
            boolean previousStatus = realCoordsShown;
            checkRealCoordinates();

            // If real coordinates just became visible, trigger panic quit
            if (!previousStatus && realCoordsShown) {
                LOGGER.warn("Real coordinates just became visible - triggering panic quit!");
                performPanicQuit();
            }
        }
    }
    
    public static void setPanicQuitEnabled(boolean enabled) {
        WebControlConfig.getInstance().updatePanicQuit(enabled);
    }

    /**
     * Public method for web API to toggle panic quit with full functionality
     * Includes sounds, messages, and coordinate checking
     */
    public static void performToggle() {
        togglePanicQuit();
    }

    /**
     * Called when player joins a world to check if panic quit should be active
     * This ensures panic quit monitoring continues after reconnecting
     */
    public static void onPlayerJoinWorld() {
        if (isPanicQuitEnabled()) {
            LOGGER.info("Player joined world with panic quit ACTIVE - monitoring for real coordinates");

            // Immediately check coordinates when joining
            checkRealCoordinates();

            // If real coordinates are showing, trigger panic quit immediately
            if (realCoordsShown) {
                LOGGER.warn("Real coordinates detected upon joining world - triggering panic quit!");

                // Add a small delay to ensure the world is fully loaded before disconnecting
                MinecraftClient client = MinecraftClient.getInstance();
                client.execute(() -> {
                    // Double-check after a tick to make sure we're really in the world
                    if (client.world != null && client.player != null) {
                        client.player.sendMessage(Text.literal("§4§l⚠ PANIC QUIT: Real coordinates detected upon joining!"), false);
                        performPanicQuit();
                    }
                });
            }
        }
    }
}