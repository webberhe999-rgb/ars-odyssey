package com.swvague.ars_odyssey.network;

import com.swvague.ars_odyssey.ArsOdyssey;
import com.swvague.ars_odyssey.archive.PlayerArchiveAttachments;
import com.swvague.ars_odyssey.archive.PlayerArchiveData;

import io.netty.buffer.ByteBuf;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record SyncPlayerArchivePacket(CompoundTag tag) implements CustomPacketPayload {
    public static final Type<SyncPlayerArchivePacket> TYPE =
            new Type<>(ArsOdyssey.prefix("sync_player_archive"));

    public static final StreamCodec<ByteBuf, SyncPlayerArchivePacket> STREAM_CODEC =
            ByteBufCodecs.COMPOUND_TAG.map(SyncPlayerArchivePacket::new, SyncPlayerArchivePacket::tag);

    public static SyncPlayerArchivePacket from(PlayerArchiveData data) {
        return new SyncPlayerArchivePacket(data == null ? new CompoundTag() : data.save());
    }

    public static SyncPlayerArchivePacket from(ServerPlayer player, PlayerArchiveData data) {
        return new SyncPlayerArchivePacket(data == null ? new CompoundTag() : data.save(player.registryAccess()));
    }

    public static void handle(SyncPlayerArchivePacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() != null) {
                PlayerArchiveData data = PlayerArchiveData.load(packet.tag(), context.player().registryAccess());
                context.player().setData(PlayerArchiveAttachments.PLAYER_ARCHIVE, data);
            }
        });
    }

    @Override
    public Type<SyncPlayerArchivePacket> type() {
        return TYPE;
    }
}
