package com.swvague.ars_odyssey.network;

import com.swvague.ars_odyssey.ArsOdyssey;
import com.swvague.ars_odyssey.archive.ArchiveItemKey;
import com.swvague.ars_odyssey.archive.ArchiveOperationResult;
import com.swvague.ars_odyssey.archive.ArchiveService;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record ExtractArchiveItemPacket(ResourceLocation itemId, String componentKey, int count) implements CustomPacketPayload {
    public static final Type<ExtractArchiveItemPacket> TYPE =
            new Type<>(ArsOdyssey.prefix("extract_archive_item"));

    public static final StreamCodec<ByteBuf, ExtractArchiveItemPacket> STREAM_CODEC = StreamCodec.composite(
            ResourceLocation.STREAM_CODEC,
            ExtractArchiveItemPacket::itemId,
            ByteBufCodecs.STRING_UTF8,
            ExtractArchiveItemPacket::componentKey,
            ByteBufCodecs.VAR_INT,
            ExtractArchiveItemPacket::count,
            ExtractArchiveItemPacket::new);

    public static void handle(ExtractArchiveItemPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player) || packet.itemId() == null) {
                return;
            }
            ArchiveOperationResult result = ArchiveService.extract(
                    player,
                    new ArchiveItemKey(packet.itemId(), packet.componentKey()),
                    Math.max(1, packet.count()));
            if (result.success()) {
                player.displayClientMessage(Component.translatable("ars_odyssey.archive_terminal.extract.success", result.changedCount()), true);
            } else {
                player.displayClientMessage(Component.translatable("ars_odyssey.archive_terminal.extract.failed", result.reason()), true);
            }
        });
    }

    @Override
    public Type<ExtractArchiveItemPacket> type() {
        return TYPE;
    }
}
