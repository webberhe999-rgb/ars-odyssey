package com.swvague.ars_odyssey.block.menu;

import com.swvague.ars_odyssey.block.ResonanceNexusCoreBlock;
import com.swvague.ars_odyssey.registry.ModRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

public class NexusConfigMenu extends AbstractContainerMenu {
    private final BlockPos centerPos;

    public NexusConfigMenu(int containerId, Inventory inventory, RegistryFriendlyByteBuf data) {
        this(containerId, inventory, data == null ? BlockPos.ZERO : data.readBlockPos());
    }

    public NexusConfigMenu(int containerId, Inventory inventory, BlockPos centerPos) {
        super(ModRegistry.NEXUS_CONFIG_MENU.get(), containerId);
        this.centerPos = centerPos;
    }

    public BlockPos centerPos() {
        return centerPos;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        if (player.distanceToSqr(centerPos.getX() + 0.5D, centerPos.getY() + 0.5D, centerPos.getZ() + 0.5D) > 64.0D) {
            return false;
        }
        BlockState state = player.level().getBlockState(centerPos);
        return state.is(ModRegistry.RESONANCE_ARCHIVE_CORE.get())
                && state.getValue(ResonanceNexusCoreBlock.PART) == ResonanceNexusCoreBlock.Part.CENTER;
    }
}
