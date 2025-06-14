package com.example.webcontrol.mixin;

import com.example.webcontrol.spoof.CoordSpoofManager;
import com.example.webcontrol.PanicQuitManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InGameHud.class)
public class InGameHudMixin {
    
    @Inject(method = "render", at = @At("TAIL"))
    private void renderCustomCoordinates(DrawContext context, net.minecraft.client.render.RenderTickCounter tickCounter, CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        
        if (client.player == null) {
            return;
        }
        
        // Don't render if F3 debug screen is open (to avoid conflicts)
        if (client.getDebugHud().shouldShowDebugHud()) {
            return;
        }
        
        // Position coordinates in RusherHack style - bottom left with padding
        int x = 6;
        int y = context.getScaledWindowHeight() - 12;
        
        // Check if text replacement mode is enabled
        if (CoordSpoofManager.shouldUseTextReplacement()) {
            String replacementText = CoordSpoofManager.getTextCoordinates();
            String displayText = replacementText != null && !replacementText.isEmpty() ? replacementText : "HIDDEN";
            
            // Render replacement text in red color for protection mode
            context.drawTextWithShadow(client.textRenderer, 
                Text.literal(displayText).styled(style -> style.withColor(0xFF5555)), 
                x, y, 0xFFFFFF);
        } else {
            // SECURITY FIX: Get player position safely, use fallback if player is null (e.g., in queue)
            Vec3d realPos = client.player != null ? client.player.getPos() : new Vec3d(0, 0, 0);
            Vec3d displayPos;

            // Apply spoofing if enabled (including rapid changes for HUD)
            if (CoordSpoofManager.getCurrentMode() != CoordSpoofManager.SpoofMode.VANILLA) {
                if (CoordSpoofManager.isRapidHudMode()) {
                    // Use rapidly changing coordinates for HUD
                    Vec3d rapidCoords = CoordSpoofManager.getRapidlyChangingCoords();
                    displayPos = rapidCoords != null ? rapidCoords : CoordSpoofManager.getSpoofedPosition(realPos);
                } else {
                    displayPos = CoordSpoofManager.getSpoofedPosition(realPos);
                }
            } else {
                displayPos = realPos;
            }
            
            // Format coordinates in RusherHack style: X: 1,347.47, Y: 79.00, Z: 454.87, [X: 168, Z: 56.86]
            String xCoord = String.format("%.2f", displayPos.x);
            String yCoord = String.format("%.2f", displayPos.y);
            String zCoord = String.format("%.2f", displayPos.z);
            
            // Calculate chunk coordinates
            int chunkX = (int) Math.floor(displayPos.x) >> 4;
            int chunkZ = (int) Math.floor(displayPos.z) >> 4;
            double chunkLocalZ = displayPos.z - (chunkZ * 16);
            
            // Create styled text with RusherHack colors (light blue for labels, white for values)
            Text styledText = Text.literal("X: ").styled(style -> style.withColor(0x55FFFF)) // Light blue
                .append(Text.literal(xCoord).styled(style -> style.withColor(0xFFFFFF))) // White
                .append(Text.literal(", Y: ").styled(style -> style.withColor(0x55FFFF))) // Light blue
                .append(Text.literal(yCoord).styled(style -> style.withColor(0xFFFFFF))) // White
                .append(Text.literal(", Z: ").styled(style -> style.withColor(0x55FFFF))) // Light blue
                .append(Text.literal(zCoord).styled(style -> style.withColor(0xFFFFFF))) // White
                .append(Text.literal(", [X: ").styled(style -> style.withColor(0x55FFFF))) // Light blue
                .append(Text.literal(String.valueOf(chunkX)).styled(style -> style.withColor(0xFFFFFF))) // White
                .append(Text.literal(", Z: ").styled(style -> style.withColor(0x55FFFF))) // Light blue
                .append(Text.literal(String.format("%.2f", chunkLocalZ)).styled(style -> style.withColor(0xFFFFFF))) // White
                .append(Text.literal("]").styled(style -> style.withColor(0x55FFFF))); // Light blue
            
            // Render with shadow for better readability
            context.drawTextWithShadow(client.textRenderer, styledText, x, y, 0xFFFFFF);
        }
        
        // Add a subtle background for better readability when spoofing is active
        if (CoordSpoofManager.getCurrentMode() != CoordSpoofManager.SpoofMode.VANILLA || 
            CoordSpoofManager.shouldUseTextReplacement()) {
            
            // Calculate text width for background
            String fullText;
            if (CoordSpoofManager.shouldUseTextReplacement()) {
                String replacementText = CoordSpoofManager.getTextCoordinates();
                fullText = replacementText != null ? replacementText : "HIDDEN";
            } else {
                // SECURITY FIX: Use safe position access for background calculation
                Vec3d safePlayerPos = client.player != null ? client.player.getPos() : new Vec3d(0, 0, 0);
                Vec3d displayPos = CoordSpoofManager.getCurrentMode() != CoordSpoofManager.SpoofMode.VANILLA ?
                    (CoordSpoofManager.isRapidChangeMode() ?
                        (CoordSpoofManager.getRapidlyChangingCoords() != null ? CoordSpoofManager.getRapidlyChangingCoords() : CoordSpoofManager.getSpoofedPosition(safePlayerPos)) :
                        CoordSpoofManager.getSpoofedPosition(safePlayerPos)) :
                    safePlayerPos;
                
                int chunkX = (int) Math.floor(displayPos.x) >> 4;
                double chunkLocalZ = displayPos.z - ((int) Math.floor(displayPos.z) >> 4) * 16;
                fullText = String.format("X: %.2f, Y: %.2f, Z: %.2f, [X: %d, Z: %.2f]", 
                    displayPos.x, displayPos.y, displayPos.z, chunkX, chunkLocalZ);
            }
            
            int textWidth = client.textRenderer.getWidth(fullText);
            int textHeight = client.textRenderer.fontHeight;
            
            // Draw background rectangle with transparency
            context.fill(x - 2, y - 2, x + textWidth + 2, y + textHeight + 2, 0x80000000);
        }
        
        // Update panic quit status
        PanicQuitManager.updateRealCoordsStatus();
        
        // Render panic quit indicator if enabled
        if (PanicQuitManager.isPanicQuitEnabled()) {
            renderPanicQuitIndicator(context, client);
        }
    }
    
    private void renderPanicQuitIndicator(DrawContext context, MinecraftClient client) {
        // Position in bottom left corner for better visibility
        int screenWidth = context.getScaledWindowWidth();
        int screenHeight = context.getScaledWindowHeight();

        String panicText = "⚠ PANIC QUIT ACTIVE";
        int textWidth = client.textRenderer.getWidth(panicText);
        int textHeight = client.textRenderer.fontHeight;

        // Calculate hint text dimensions first
        String keyHint = "Press Shift+Ctrl+P to disable";
        int hintWidth = client.textRenderer.getWidth(keyHint);
        int hintHeight = client.textRenderer.fontHeight;

        // Calculate total dimensions including hint text
        int totalWidth = Math.max(textWidth, hintWidth);
        int totalHeight = textHeight + 4 + hintHeight;

        // Position in right bottom corner, higher up so it fits on screen
        int padding = 8;
        int x = screenWidth - totalWidth - padding; // Right side
        int y = screenHeight - totalHeight - padding - 10; // Higher up to fit on screen



        // Create subtle pulsing effect for the background only
        long time = System.currentTimeMillis();
        float pulse = (float) (0.8 + 0.2 * Math.sin(time * 0.005)); // Slower, more subtle pulse
        int bgAlpha = (int) (200 * pulse);
        int backgroundColor = (bgAlpha << 24) | 0x000000; // Black background with pulsing alpha

        // Draw larger background with proper sizing
        int bgPadding = 6;
        context.fill(x - bgPadding, y - bgPadding, x + totalWidth + bgPadding, y + totalHeight + bgPadding, backgroundColor);

        // Draw bright border for high visibility
        int borderColor = 0xFFFF4444; // Bright red border, always visible
        context.fill(x - bgPadding - 1, y - bgPadding - 1, x + totalWidth + bgPadding + 1, y - bgPadding, borderColor); // Top
        context.fill(x - bgPadding - 1, y + totalHeight + bgPadding, x + totalWidth + bgPadding + 1, y + totalHeight + bgPadding + 1, borderColor); // Bottom
        context.fill(x - bgPadding - 1, y - bgPadding, x - bgPadding, y + totalHeight + bgPadding, borderColor); // Left
        context.fill(x + totalWidth + bgPadding, y - bgPadding, x + totalWidth + bgPadding + 1, y + totalHeight + bgPadding, borderColor); // Right

        // Draw text with high contrast - bright white with dark shadow
        Text panicTextComponent = Text.literal(panicText).styled(style -> style.withBold(true));

        // Draw dark shadow for contrast
        context.drawText(client.textRenderer, panicTextComponent, x + 1, y + 1, 0xFF000000, false);

        // Main text in bright white for maximum readability
        context.drawText(client.textRenderer, panicTextComponent, x, y, 0xFFFFFFFF, false);

        // Add key combination hint below with better contrast
        int hintX = x;
        int hintY = y + textHeight + 4;

        // Draw hint with shadow for readability
        context.drawText(client.textRenderer, Text.literal(keyHint), hintX + 1, hintY + 1, 0xFF000000, false);
        context.drawText(client.textRenderer,
            Text.literal(keyHint).styled(style -> style.withColor(0xFFCCCCCC)),
            hintX, hintY, 0xFFFFFFFF, false);
    }
}