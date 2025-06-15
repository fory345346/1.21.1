package com.example.webcontrol.mixin;

import com.example.webcontrol.spoof.TrueRusherHackSpoofer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.DebugHud;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(DebugHud.class)
public class DebugHudMixin {
    
    @Inject(method = "getLeftText", at = @At("RETURN"), cancellable = true)
    private void modifyLeftText(CallbackInfoReturnable<List<String>> cir) {
        if (TrueRusherHackSpoofer.getCurrentMode() == TrueRusherHackSpoofer.SpoofMode.VANILLA) {
            return; // Don't modify in vanilla mode
        }
        
        List<String> lines = cir.getReturnValue();
        MinecraftClient client = MinecraftClient.getInstance();
        
        if (client.player == null || lines.isEmpty()) {
            return;
        }
        
        // Simple coordinate spoofing (like RusherHack plugin) - no complex modes
        else {
            Vec3d playerPos = client.player.getPos();
            Vec3d spoofedPos = TrueRusherHackSpoofer.getSpoofedPosition(playerPos);

            // Replace coordinate lines with spoofed values
            for (int i = 0; i < lines.size(); i++) {
                String line = lines.get(i);
                if (line.startsWith("XYZ:")) {
                    lines.set(i, String.format("XYZ: %.3f / %.3f / %.3f",
                        spoofedPos.x, spoofedPos.y, spoofedPos.z));
                } else if (line.contains("Block:")) {
                    lines.set(i, String.format("Block: %d %d %d",
                        (int)Math.floor(spoofedPos.x), (int)Math.floor(spoofedPos.y), (int)Math.floor(spoofedPos.z)));
                } else if (line.contains("Chunk:")) {
                    int chunkX = (int)Math.floor(spoofedPos.x) >> 4;
                    int chunkZ = (int)Math.floor(spoofedPos.z) >> 4;
                    int localX = (int)Math.floor(spoofedPos.x) & 15;
                    int localZ = (int)Math.floor(spoofedPos.z) & 15;
                    lines.set(i, String.format("Chunk: %d %d %d in %d %d",
                        localX, (int)Math.floor(spoofedPos.y), localZ, chunkX, chunkZ));
                }
            }
        }

        // No biome spoofing in simple mode
        
        cir.setReturnValue(lines);
    }
    
    @Inject(method = "shouldShowDebugHud", at = @At("HEAD"))
    private void onDebugHudToggle(CallbackInfoReturnable<Boolean> cir) {
        // Trigger F3 opened event for custom mode
        // onF3Opened method removed - not needed for VANILLA/OFFSET modes
    }
}