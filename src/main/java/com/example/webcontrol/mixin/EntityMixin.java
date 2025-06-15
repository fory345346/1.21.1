package com.example.webcontrol.mixin;

import com.example.webcontrol.spoof.TrueRusherHackSpoofer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Mixin to intercept Entity position methods
 * This ensures that when other mods call entity.getX/Y/Z() on the player entity,
 * they get spoofed coordinates
 */
@Mixin(Entity.class)
public class EntityMixin {

    /**
     * Intercept getX() calls for the player entity only
     */
    @Inject(method = "getX", at = @At("RETURN"), cancellable = true)
    private void spoofEntityGetX(CallbackInfoReturnable<Double> cir) {
        // Only spoof coordinates for the client player
        if (isClientPlayer() && TrueRusherHackSpoofer.getCurrentMode() == TrueRusherHackSpoofer.SpoofMode.OFFSET) {
            double realX = cir.getReturnValue();
            double spoofedX = TrueRusherHackSpoofer.getSpoofedX(realX);
            cir.setReturnValue(spoofedX);
        }
    }

    /**
     * Intercept getY() calls for the player entity only
     */
    @Inject(method = "getY", at = @At("RETURN"), cancellable = true)
    private void spoofEntityGetY(CallbackInfoReturnable<Double> cir) {
        // Only spoof coordinates for the client player
        if (isClientPlayer() && TrueRusherHackSpoofer.getCurrentMode() == TrueRusherHackSpoofer.SpoofMode.OFFSET) {
            double realY = cir.getReturnValue();
            double spoofedY = TrueRusherHackSpoofer.getSpoofedY(realY);
            cir.setReturnValue(spoofedY);
        }
    }

    /**
     * Intercept getZ() calls for the player entity only
     */
    @Inject(method = "getZ", at = @At("RETURN"), cancellable = true)
    private void spoofEntityGetZ(CallbackInfoReturnable<Double> cir) {
        // Only spoof coordinates for the client player
        if (isClientPlayer() && TrueRusherHackSpoofer.getCurrentMode() == TrueRusherHackSpoofer.SpoofMode.OFFSET) {
            double realZ = cir.getReturnValue();
            double spoofedZ = TrueRusherHackSpoofer.getSpoofedZ(realZ);
            cir.setReturnValue(spoofedZ);
        }
    }

    /**
     * Intercept getPos() calls for the player entity only
     */
    @Inject(method = "getPos", at = @At("RETURN"), cancellable = true)
    private void spoofEntityGetPos(CallbackInfoReturnable<net.minecraft.util.math.Vec3d> cir) {
        // Only spoof coordinates for the client player
        if (isClientPlayer() && TrueRusherHackSpoofer.getCurrentMode() == TrueRusherHackSpoofer.SpoofMode.OFFSET) {
            net.minecraft.util.math.Vec3d realPos = cir.getReturnValue();
            net.minecraft.util.math.Vec3d spoofedPos = TrueRusherHackSpoofer.getSpoofedPosition(realPos);
            cir.setReturnValue(spoofedPos);
        }
    }

    /**
     * Intercept getBlockPos() calls for the player entity only
     */
    @Inject(method = "getBlockPos", at = @At("RETURN"), cancellable = true)
    private void spoofEntityGetBlockPos(CallbackInfoReturnable<net.minecraft.util.math.BlockPos> cir) {
        // Only spoof coordinates for the client player
        if (isClientPlayer() && TrueRusherHackSpoofer.getCurrentMode() == TrueRusherHackSpoofer.SpoofMode.OFFSET) {
            net.minecraft.util.math.BlockPos realPos = cir.getReturnValue();
            net.minecraft.util.math.BlockPos spoofedPos = new net.minecraft.util.math.BlockPos(
                (int) TrueRusherHackSpoofer.getSpoofedX(realPos.getX()),
                (int) TrueRusherHackSpoofer.getSpoofedY(realPos.getY()),
                (int) TrueRusherHackSpoofer.getSpoofedZ(realPos.getZ())
            );
            cir.setReturnValue(spoofedPos);
        }
    }

    /**
     * Check if this entity is the client player
     */
    private boolean isClientPlayer() {
        try {
            MinecraftClient client = MinecraftClient.getInstance();
            return client != null && client.player != null && (Object) this == client.player;
        } catch (Exception e) {
            return false;
        }
    }
}
