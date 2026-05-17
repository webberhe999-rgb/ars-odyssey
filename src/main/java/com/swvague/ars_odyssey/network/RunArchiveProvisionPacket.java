package com.swvague.ars_odyssey.network;

import com.swvague.ars_odyssey.ArsOdyssey;
import com.swvague.ars_odyssey.archive.ArchiveOperationResult;
import com.swvague.ars_odyssey.archive.ArchiveService;
import com.swvague.ars_odyssey.archive.PlayerArchiveAttachments;
import com.swvague.ars_odyssey.archive.PlayerArchiveData;
import com.swvague.ars_odyssey.archive.provision.ArchiveProvisionRule;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record RunArchiveProvisionPacket(ResourceLocation itemId, boolean all) implements CustomPacketPayload {
    public static final Type<RunArchiveProvisionPacket> TYPE =
            new Type<>(ArsOdyssey.prefix("run_archive_provision"));

    public static final StreamCodec<ByteBuf, RunArchiveProvisionPacket> STREAM_CODEC = StreamCodec.composite(
            ResourceLocation.STREAM_CODEC,
            RunArchiveProvisionPacket::itemId,
            ByteBufCodecs.BOOL,
            RunArchiveProvisionPacket::all,
            RunArchiveProvisionPacket::new);

    public static RunArchiveProvisionPacket allRules() {
        return new RunArchiveProvisionPacket(ArsOdyssey.prefix("all"), true);
    }

    public static void handle(RunArchiveProvisionPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }
            if (packet.all()) {
                runAll(player);
                return;
            }
            if (packet.itemId() != null) {
                ArchiveService.provision(player, packet.itemId());
            }
        });
    }

    private static void runAll(ServerPlayer player) {
        PlayerArchiveData data = player.getData(PlayerArchiveAttachments.PLAYER_ARCHIVE);
        for (ArchiveProvisionRule rule : data.provisionRules().enabledRules()) {
            ArchiveOperationResult ignored = ArchiveService.provision(player, rule.itemId());
        }
    }

    @Override
    public Type<RunArchiveProvisionPacket> type() {
        return TYPE;
    }
}
