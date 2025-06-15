package com.example.webcontrol.mixin;

import com.example.webcontrol.spoof.SimpleCoordSpoofer;
import net.minecraft.client.network.ClientPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin to intercept player-specific methods so other mods get spoofed coordinates
 * Focus on ClientPlayerEntity-specific methods that other mods might use
 */
@Mixin(ClientPlayerEntity.class)
public class ClientPlayerEntityMixin {

    /**
     * Intercept sendMovementPackets() to log when movement packets are sent
     * This helps us understand when the player position is being used
     */
    @Inject(method = "sendMovementPackets", at = @At("HEAD"))
    private void onSendMovementPackets(CallbackInfo ci) {
        if (SimpleCoordSpoofer.getCurrentMode() == SimpleCoordSpoofer.SpoofMode.OFFSET) {
            // Log that movement packets are being sent
            // This will help us see when the coordinate spoofing is active
        }
    }
}
