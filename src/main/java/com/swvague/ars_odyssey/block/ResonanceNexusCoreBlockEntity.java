package com.swvague.ars_odyssey.block;

import com.swvague.ars_odyssey.registry.ModRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class ResonanceNexusCoreBlockEntity extends BlockEntity {
    public ResonanceNexusCoreBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModRegistry.RESONANCE_NEXUS_CORE_BLOCK_ENTITY.get(), pos, blockState);
    }
}
