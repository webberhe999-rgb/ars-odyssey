package com.swvague.ars_odyssey.network;

import com.swvague.ars_odyssey.ArsOdyssey;
import com.swvague.ars_odyssey.knowledge.PlayerKnowledgeAttachments;
import com.swvague.ars_odyssey.knowledge.PlayerKnowledgeData;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record SetTruthifiedGlyphPacket(ResourceLocation glyphId, String spellSlotKey, boolean enabled) implements CustomPacketPayload {
    public static final Type<SetTruthifiedGlyphPacket> TYPE =
            new Type<>(ArsOdyssey.prefix("set_truthified_glyph"));

    public static final StreamCodec<ByteBuf, SetTruthifiedGlyphPacket> STREAM_CODEC = StreamCodec.composite(
            ResourceLocation.STREAM_CODEC,
            SetTruthifiedGlyphPacket::glyphId,
            ByteBufCodecs.STRING_UTF8,
            SetTruthifiedGlyphPacket::spellSlotKey,
            ByteBufCodecs.BOOL,
            SetTruthifiedGlyphPacket::enabled,
            SetTruthifiedGlyphPacket::new);

    public static void handle(SetTruthifiedGlyphPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player) || packet.glyphId() == null) {
                return;
            }
            PlayerKnowledgeData data = player.getData(PlayerKnowledgeAttachments.PLAYER_KNOWLEDGE);
            if (packet.spellSlotKey() != null && !packet.spellSlotKey().isBlank()) {
                data.setTruthifiedSpellSlot(packet.spellSlotKey(), packet.enabled());
            } else {
                data.setTruthified(packet.glyphId(), packet.enabled());
            }
            player.setData(PlayerKnowledgeAttachments.PLAYER_KNOWLEDGE, data);
            ModNetwork.syncKnowledge(player, data);
        });
    }

    @Override
    public Type<SetTruthifiedGlyphPacket> type() {
        return TYPE;
    }
}
