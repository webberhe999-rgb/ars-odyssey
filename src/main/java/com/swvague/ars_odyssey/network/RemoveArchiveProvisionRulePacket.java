package com.swvague.ars_odyssey.network;

import com.swvague.ars_odyssey.ArsOdyssey;
import com.swvague.ars_odyssey.archive.PlayerArchiveAttachments;
import com.swvague.ars_odyssey.archive.PlayerArchiveData;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record RemoveArchiveProvisionRulePacket(ResourceLocation itemId) implements CustomPacketPayload {
    public static final Type<RemoveArchiveProvisionRulePacket> TYPE =
            new Type<>(ArsOdyssey.prefix("remove_archive_provision_rule"));

    public static final StreamCodec<ByteBuf, RemoveArchiveProvisionRulePacket> STREAM_CODEC =
            ResourceLocation.STREAM_CODEC.map(RemoveArchiveProvisionRulePacket::new, RemoveArchiveProvisionRulePacket::itemId);

    public static void handle(RemoveArchiveProvisionRulePacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player) || packet.itemId() == null) {
                return;
            }
            PlayerArchiveData data = player.getData(PlayerArchiveAttachments.PLAYER_ARCHIVE);
            if (data.provisionRules().remove(packet.itemId())) {
                player.setData(PlayerArchiveAttachments.PLAYER_ARCHIVE, data);
                ModNetwork.syncArchive(player, data);
            }
        });
    }

    @Override
    public Type<RemoveArchiveProvisionRulePacket> type() {
        return TYPE;
    }
}
