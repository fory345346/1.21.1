package com.example.webcontrol.mixin.accessor;

import net.minecraft.network.packet.s2c.play.BlockUpdateS2CPacket;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Accessor mixin for BlockUpdateS2CPacket to modify block position
 */
@Mixin(BlockUpdateS2CPacket.class)
public interface BlockUpdateS2CPacketAccessor {
    
    @Accessor("pos")
    BlockPos getPos();
    
    @Accessor("pos")
    @Mutable
    void setPos(BlockPos pos);
}
