package com.example.webcontrol.mixin;

import com.example.webcontrol.spoof.CoordSpoofManager;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.PacketCallbacks;
import net.minecraft.network.listener.PacketListener;
import net.minecraft.network.packet.Packet;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Packet interception for coordinate spoofing - EXACTLY like CoordsSpooferExample
 */
@Mixin(ClientConnection.class)
public class ClientConnectionMixin {

    @Inject(method = "handlePacket", at = @At("HEAD"), require = 0)
    private static void genericsFtw(Packet<?> packet, PacketListener listener, CallbackInfo ci) {
        try {
            if (CoordSpoofManager.getCurrentMode() != CoordSpoofManager.SpoofMode.VANILLA) {
                // Handle incoming packets from server
                CoordSpoofManager.packetReceived(packet);
            }
        } catch (Exception e) {
            // Ignore errors to prevent crashes
            CoordSpoofManager.logDebug("Error in packetReceived: " + e.getMessage());
        }
    }

    @Inject(method = "send(Lnet/minecraft/network/packet/Packet;Lnet/minecraft/network/PacketCallbacks;)V", at = @At("HEAD"), require = 0)
    public void sendPacket(Packet<?> packet, @Nullable PacketCallbacks callbacks, CallbackInfo ci) {
        try {
            if (CoordSpoofManager.getCurrentMode() != CoordSpoofManager.SpoofMode.VANILLA) {
                // Handle outgoing packets to server
                CoordSpoofManager.packetSend(packet);
            }
        } catch (Exception e) {
            // Ignore errors to prevent crashes
            CoordSpoofManager.logDebug("Error in packetSend: " + e.getMessage());
        }
    }
}
