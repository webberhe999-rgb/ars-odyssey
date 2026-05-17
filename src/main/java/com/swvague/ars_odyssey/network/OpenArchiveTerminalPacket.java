package com.swvague.ars_odyssey.network;

import com.swvague.ars_odyssey.ArsOdyssey;
import com.swvague.ars_odyssey.archive.menu.ArchiveTerminalMenu;
import com.swvague.ars_odyssey.registry.ModRegistry;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.SimpleMenuProvider;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record OpenArchiveTerminalPacket(int handOrdinal) implements CustomPacketPayload {
    public static final Type<OpenArchiveTerminalPacket> TYPE =
            new Type<>(ArsOdyssey.prefix("open_archive_terminal"));

    public static final StreamCodec<ByteBuf, OpenArchiveTerminalPacket> STREAM_CODEC =
            ByteBufCodecs.VAR_INT.map(OpenArchiveTerminalPacket::new, OpenArchiveTerminalPacket::handOrdinal);

    public static OpenArchiveTerminalPacket forHand(InteractionHand hand) {
        return new OpenArchiveTerminalPacket(hand == null ? InteractionHand.MAIN_HAND.ordinal() : hand.ordinal());
    }

    public static void handle(OpenArchiveTerminalPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }
            InteractionHand hand = packet.resolveHand();
            if (!ModRegistry.isOdysseySpellBook(player.getItemInHand(hand))) {
                hand = ModRegistry.isOdysseySpellBook(player.getOffhandItem()) ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
            }
            if (!ModRegistry.isOdysseySpellBook(player.getItemInHand(hand))) {
                return;
            }

            InteractionHand finalHand = hand;
            player.openMenu(
                    new SimpleMenuProvider(
                            (containerId, inventory, ignored) -> new ArchiveTerminalMenu(containerId, inventory, finalHand),
                            Component.translatable("ars_odyssey.archive_terminal.title")),
                    buffer -> buffer.writeEnum(finalHand));
        });
    }

    @Override
    public Type<OpenArchiveTerminalPacket> type() {
        return TYPE;
    }

    private InteractionHand resolveHand() {
        InteractionHand[] values = InteractionHand.values();
        if (handOrdinal < 0 || handOrdinal >= values.length) {
            return InteractionHand.MAIN_HAND;
        }
        return values[handOrdinal];
    }
}
