package com.swvague.ars_odyssey.network;

import com.swvague.ars_odyssey.ArsOdyssey;
import com.swvague.ars_odyssey.archive.ArchiveOperationResult;
import com.swvague.ars_odyssey.archive.ArchiveService;
import com.swvague.ars_odyssey.registry.ModRegistry;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record DepositCarriedArchiveItemPacket(int count) implements CustomPacketPayload {
    public static final Type<DepositCarriedArchiveItemPacket> TYPE =
            new Type<>(ArsOdyssey.prefix("deposit_carried_archive_item"));

    public static final StreamCodec<ByteBuf, DepositCarriedArchiveItemPacket> STREAM_CODEC =
            ByteBufCodecs.VAR_INT.map(DepositCarriedArchiveItemPacket::new, DepositCarriedArchiveItemPacket::count);

    public static void handle(DepositCarriedArchiveItemPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }
            ItemStack carried = player.containerMenu.getCarried();
            if (carried.isEmpty()) {
                return;
            }
            if (ModRegistry.isOdysseySpellBook(carried)) {
                player.displayClientMessage(Component.translatable("ars_odyssey.archive_terminal.deposit.denied_spellbook"), true);
                return;
            }
            ArchiveOperationResult result = ArchiveService.depositCarried(player, Math.max(1, packet.count()));
            if (!result.success()) {
                player.displayClientMessage(Component.translatable("ars_odyssey.archive_terminal.deposit.failed", result.reason()), true);
            }
        });
    }

    @Override
    public Type<DepositCarriedArchiveItemPacket> type() {
        return TYPE;
    }
}
