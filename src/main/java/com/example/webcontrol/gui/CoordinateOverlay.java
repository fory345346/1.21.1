package com.example.webcontrol.gui;

// import com.example.webcontrol.WebControlClient; // Removed - file deleted
import com.example.webcontrol.spoof.CoordSpoofManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import java.awt.*;
import java.io.InputStream;
import java.text.DecimalFormat;

public class CoordinateOverlay {
    private static final DecimalFormat COORDINATE_FORMAT = new DecimalFormat("#,###.###");
    private static final float NETHER_RATIO = 8.0f;
    private static Font montserratFont;

    static {
        try {
            // Load Montserrat font from resources
            InputStream fontStream = CoordinateOverlay.class.getResourceAsStream("/assets/webcontrol/fonts/Montserrat-Regular.ttf");
            if (fontStream != null) {
                montserratFont = Font.createFont(Font.TRUETYPE_FONT, fontStream).deriveFont(14f);
                GraphicsEnvironment.getLocalGraphicsEnvironment().registerFont(montserratFont);
            }
        } catch (Exception e) {
            e.printStackTrace();
            montserratFont = null;
        }
    }

    public static void render(DrawContext context) {
        MinecraftClient client = MinecraftClient.getInstance();
        // if (!WebControlClient.shouldShowCoordinates()) return; // Removed - file deleted
        // Always show coordinates for now

        // SECURITY FIX: Show spoofed coordinates even when player is null (e.g., in queue)
        Vec3d realPos = new Vec3d(0, 0, 0);
        boolean isNether = false;

        if (client.player != null) {
            PlayerEntity player = client.player;
            isNether = player.getWorld().getRegistryKey() == World.NETHER;
            realPos = player.getPos();
        }

        // Get spoofed coordinates (will use fallback values if player is null)
        Vec3d displayPos = CoordSpoofManager.getSpoofedPosition(realPos);

        String coordText;
        if (isNether) {
            coordText = String.format("X: %s, Y: %s, Z: %s [%s, %s]",
                    COORDINATE_FORMAT.format(displayPos.x),
                    COORDINATE_FORMAT.format(displayPos.y),
                    COORDINATE_FORMAT.format(displayPos.z),
                    COORDINATE_FORMAT.format(displayPos.x * NETHER_RATIO),
                    COORDINATE_FORMAT.format(displayPos.z * NETHER_RATIO));
        } else {
            coordText = String.format("X: %s, Y: %s, Z: %s [%s, %s]",
                    COORDINATE_FORMAT.format(displayPos.x),
                    COORDINATE_FORMAT.format(displayPos.y),
                    COORDINATE_FORMAT.format(displayPos.z),
                    COORDINATE_FORMAT.format(displayPos.x / NETHER_RATIO),
                    COORDINATE_FORMAT.format(displayPos.z / NETHER_RATIO));
        }

        // Draw at bottom left with Montserrat-style rendering
        int screenHeight = client.getWindow().getScaledHeight();
        int padding = 8;
        int yPos = screenHeight - 20;

        // Draw background for better readability
        int textWidth = client.textRenderer.getWidth(coordText);
        context.fill(padding - 3, yPos - 3, padding + textWidth + 3, yPos + 12, 0x80000000);

        // Draw text with custom styling to mimic Montserrat
        context.drawTextWithShadow(client.textRenderer, coordText, padding, yPos, 0xE0E0E0);
    }
}