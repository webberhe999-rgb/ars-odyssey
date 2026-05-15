package com.example.ars_odyssey.network;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import com.example.ars_odyssey.knowledge.PlayerKnowledgeData;

public final class ModNetwork {
    private ModNetwork() {
    }

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        registrar.playToClient(
                SyncPlayerKnowledgePacket.TYPE,
                SyncPlayerKnowledgePacket.STREAM_CODEC,
                SyncPlayerKnowledgePacket::handle);
    }

    public static void syncKnowledge(ServerPlayer player, PlayerKnowledgeData data) {
        PacketDistributor.sendToPlayer(player, SyncPlayerKnowledgePacket.from(data));
    }
}
