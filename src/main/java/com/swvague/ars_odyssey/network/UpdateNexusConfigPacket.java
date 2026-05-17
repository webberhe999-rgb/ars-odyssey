package com.swvague.ars_odyssey.network;

import com.swvague.ars_odyssey.ArsOdyssey;
import com.swvague.ars_odyssey.block.ResonanceNexusCoreBlock;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record UpdateNexusConfigPacket(int x, int y, int z, int tier, boolean particles) implements CustomPacketPayload {
    public static final Type<UpdateNexusConfigPacket> TYPE =
            new Type<>(ArsOdyssey.prefix("update_nexus_config"));

    public static final StreamCodec<ByteBuf, UpdateNexusConfigPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT,
            UpdateNexusConfigPacket::x,
            ByteBufCodecs.VAR_INT,
            UpdateNexusConfigPacket::y,
            ByteBufCodecs.VAR_INT,
            UpdateNexusConfigPacket::z,
            ByteBufCodecs.VAR_INT,
            UpdateNexusConfigPacket::tier,
            ByteBufCodecs.BOOL,
            UpdateNexusConfigPacket::particles,
            UpdateNexusConfigPacket::new);

    public static UpdateNexusConfigPacket of(BlockPos center, int tier, boolean particles) {
        return new UpdateNexusConfigPacket(center.getX(), center.getY(), center.getZ(), tier, particles);
    }

    public static void handle(UpdateNexusConfigPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }

            BlockPos center = new BlockPos(packet.x(), packet.y(), packet.z());
            if (player.distanceToSqr(center.getX() + 0.5D, center.getY() + 0.5D, center.getZ() + 0.5D) > 64.0D) {
                return;
            }

            BlockState state = player.level().getBlockState(center);
            if (!(state.getBlock() instanceof ResonanceNexusCoreBlock block)
                    || state.getValue(ResonanceNexusCoreBlock.PART) != ResonanceNexusCoreBlock.Part.CENTER) {
                return;
            }

            int tier = Math.max(0, Math.min(ResonanceNexusCoreBlock.FINAL_TIER, packet.tier()));
            block.applySettings(player.level(), center, tier, packet.particles());
        });
    }

    @Override
    public Type<UpdateNexusConfigPacket> type() {
        return TYPE;
    }
}
