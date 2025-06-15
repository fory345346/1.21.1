package com.example.webcontrol.mixin;

import com.example.webcontrol.spoof.TrueRusherHackSpoofer;
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
            // TRUE RusherHack implementation - modifies packet fields directly
            TrueRusherHackSpoofer.onPacketReceive(packet);
        } catch (Exception e) {
            // Ignore errors to prevent crashes
        }
    }

    @Inject(method = "send(Lnet/minecraft/network/packet/Packet;Lnet/minecraft/network/PacketCallbacks;)V", at = @At("HEAD"), require = 0)
    public void sendPacket(Packet<?> packet, @Nullable PacketCallbacks callbacks, CallbackInfo ci) {
        try {
            // TRUE RusherHack implementation - modifies packet fields directly
            TrueRusherHackSpoofer.onPacketSend(packet);
        } catch (Exception e) {
            // Ignore errors to prevent crashes
        }
    }
}
