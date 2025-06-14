package com.example.webcontrol.mixin.accessor;

import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Accessor mixin for PlayerPositionLookS2CPacket - EXACTLY like CoordsSpooferExample
 * Only X and Z coordinates, NO Y coordinate to prevent teleportation
 * This is the Fabric equivalent of ClientboundPlayerPositionPacket
 */
@Mixin(PlayerPositionLookS2CPacket.class)
public interface PlayerPositionLookS2CPacketAccessor {

    // EXACTLY like IMixinClientboundPlayerPositionPacket in CoordsSpooferExample
    @Accessor("x")
    @Mutable
    void setX(double x);

    @Accessor("z")
    @Mutable
    void setZ(double z);

    @Accessor("x")
    double getX();

    @Accessor("z")
    double getZ();

    // NO Y ACCESSOR - this prevents teleportation like in CoordsSpooferExample
}
