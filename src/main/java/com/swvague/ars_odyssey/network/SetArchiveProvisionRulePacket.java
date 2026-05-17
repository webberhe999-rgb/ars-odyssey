package com.swvague.ars_odyssey.network;

import com.swvague.ars_odyssey.ArsOdyssey;
import com.swvague.ars_odyssey.archive.PlayerArchiveAttachments;
import com.swvague.ars_odyssey.archive.PlayerArchiveData;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record SetArchiveProvisionRulePacket(ResourceLocation itemId, int targetCount) implements CustomPacketPayload {
    public static final Type<SetArchiveProvisionRulePacket> TYPE =
            new Type<>(ArsOdyssey.prefix("set_archive_provision_rule"));

    public static final StreamCodec<ByteBuf, SetArchiveProvisionRulePacket> STREAM_CODEC = StreamCodec.composite(
            ResourceLocation.STREAM_CODEC,
            SetArchiveProvisionRulePacket::itemId,
            ByteBufCodecs.VAR_INT,
            SetArchiveProvisionRulePacket::targetCount,
            SetArchiveProvisionRulePacket::new);

    public static void handle(SetArchiveProvisionRulePacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player) || packet.itemId() == null) {
                return;
            }
            Item item = BuiltInRegistries.ITEM.get(packet.itemId());
            if (!BuiltInRegistries.ITEM.getKey(item).equals(packet.itemId()) || item.getDefaultInstance().isEmpty()) {
                return;
            }

            int targetCount = Math.max(1, Math.min(2304, packet.targetCount()));
            PlayerArchiveData data = player.getData(PlayerArchiveAttachments.PLAYER_ARCHIVE);
            data.provisionRules().set(packet.itemId(), targetCount);
            player.setData(PlayerArchiveAttachments.PLAYER_ARCHIVE, data);
            ModNetwork.syncArchive(player, data);
        });
    }

    @Override
    public Type<SetArchiveProvisionRulePacket> type() {
        return TYPE;
    }
}
