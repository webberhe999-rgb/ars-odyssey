package com.swvague.ars_odyssey.network;

import com.swvague.ars_odyssey.ArsOdyssey;
import com.swvague.ars_odyssey.glyph.TruthifiedOrbitProjectile;
import com.swvague.ars_odyssey.glyph.TruthifiedOrbitProjectileRegistry;
import com.hollingsworth.arsnouveau.common.entity.EntityOrbitProjectile;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public record ClearTruthifiedProjectilesPacket() implements CustomPacketPayload {

    private static final ClearTruthifiedProjectilesPacket INSTANCE = new ClearTruthifiedProjectilesPacket();

    public static final Type<ClearTruthifiedProjectilesPacket> TYPE =
            new Type<>(ArsOdyssey.prefix("clear_truthified_projectiles"));

    public static final StreamCodec<ByteBuf, ClearTruthifiedProjectilesPacket> STREAM_CODEC =
            StreamCodec.unit(INSTANCE);

    @Override
    public Type<ClearTruthifiedProjectilesPacket> type() {
        return TYPE;
    }

    public static void handle(ClearTruthifiedProjectilesPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer serverPlayer) {
                clearTruthifiedProjectilesFor(serverPlayer);
            }
        });
    }

    /**
     * Discards all truthified Orbit Self projectiles owned by the given player.
     * Safe to call from the server thread.
     */
    public static void clearTruthifiedProjectilesFor(ServerPlayer player) {
        clearTruthifiedProjectilesFor(player, List.of(), true);
        TruthifiedOrbitProjectileRegistry.clear(player.getUUID());
    }

    public static void clearTruthifiedProjectilesFor(ServerPlayer player, List<UUID> spellGroupIds) {
        clearTruthifiedProjectilesFor(player, spellGroupIds, false);
    }

    private static void clearTruthifiedProjectilesFor(ServerPlayer player, List<UUID> spellGroupIds, boolean clearAll) {
        if (player == null) {
            return;
        }
        Set<UUID> groups = spellGroupIds == null ? Set.of() : Set.copyOf(spellGroupIds);
        if (!clearAll && groups.isEmpty()) {
            return;
        }
        ServerLevel level = player.serverLevel();
        AABB searchBox = player.getBoundingBox().inflate(256.0);
        List<EntityOrbitProjectile> projectiles = level.getEntitiesOfClass(
                EntityOrbitProjectile.class,
                searchBox,
                p -> isOwnedBy(p, player)
                        && p instanceof TruthifiedOrbitProjectile t
                        && t.ars_odyssey$isTruthifiedOrbit()
                        && (clearAll || groups.contains(t.ars_odyssey$getSpellGroupId()))
        );
        for (EntityOrbitProjectile p : projectiles) {
            p.discard();
        }
        for (UUID group : groups) {
            TruthifiedOrbitProjectileRegistry.remove(player.getUUID(), group);
        }
    }

    private static boolean isOwnedBy(EntityOrbitProjectile projectile, ServerPlayer player) {
        Entity owner = projectile.getOwner();
        return owner != null && owner.getUUID().equals(player.getUUID());
    }
}
