package com.example.ars_odyssey.network;

import com.example.ars_odyssey.ArsOdyssey;
import com.example.ars_odyssey.knowledge.PlayerKnowledgeAttachments;
import com.example.ars_odyssey.knowledge.PlayerKnowledgeData;
import io.netty.buffer.ByteBuf;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public record SyncPlayerKnowledgePacket(CompoundTag tag) implements CustomPacketPayload {
    private static final Logger LOGGER = LogManager.getLogger("Ars Odyssey Network");

    public static final Type<SyncPlayerKnowledgePacket> TYPE =
            new Type<>(ArsOdyssey.prefix("sync_player_knowledge"));

    public static final StreamCodec<ByteBuf, SyncPlayerKnowledgePacket> STREAM_CODEC =
            ByteBufCodecs.COMPOUND_TAG.map(SyncPlayerKnowledgePacket::new, SyncPlayerKnowledgePacket::tag);

    public static SyncPlayerKnowledgePacket from(PlayerKnowledgeData data) {
        return new SyncPlayerKnowledgePacket(data == null ? new CompoundTag() : data.save());
    }

    public static void handle(SyncPlayerKnowledgePacket packet, IPayloadContext context) {
        // Deserialize on the network thread (no game-state access needed).
        PlayerKnowledgeData data = PlayerKnowledgeData.load(packet.tag());
        int relationCount = data.discoveredRelations().size();
        LOGGER.info("[Ars Odyssey] [CLIENT SYNC] received SyncPlayerKnowledgePacket: relations={} complexityRecords={}",
                relationCount, data.complexityRecords().size());

        // Apply on the main game thread — required by NeoForge; setData is not thread-safe.
        context.enqueueWork(() -> {
            if (context.player() == null) {
                LOGGER.warn("[Ars Odyssey] [CLIENT SYNC] player is null on main thread, skipping setData");
                return;
            }
            context.player().setData(PlayerKnowledgeAttachments.PLAYER_KNOWLEDGE, data);
            LOGGER.info("[Ars Odyssey] [CLIENT SYNC] setData applied on main thread: relations={}", relationCount);
        });
    }

    @Override
    public Type<SyncPlayerKnowledgePacket> type() {
        return TYPE;
    }
}
