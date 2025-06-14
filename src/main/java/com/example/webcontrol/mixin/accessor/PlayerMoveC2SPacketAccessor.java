package com.example.webcontrol.mixin.accessor;

import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Accessor mixin for PlayerMoveC2SPacket - Fabric equivalent of ServerboundMovePlayerPacket
 * EXACTLY like IMixinServerboundMovePlayerPacket in CoordsSpooferExample
 */
@Mixin(PlayerMoveC2SPacket.class)
public interface PlayerMoveC2SPacketAccessor {

    // EXACTLY like IMixinServerboundMovePlayerPacket in CoordsSpooferExample
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

    // NO Y ACCESSOR - only X and Z like in example
}
