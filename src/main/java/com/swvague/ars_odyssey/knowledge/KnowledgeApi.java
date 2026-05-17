package com.swvague.ars_odyssey.knowledge;

import com.swvague.ars_odyssey.api.ArsOdysseyApi;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

/**
 * Legacy façade — kept for backward compatibility.
 * New code should use {@link ArsOdysseyApi} directly.
 *
 * @deprecated Migrate callers to {@link ArsOdysseyApi}.
 */
@Deprecated
public final class KnowledgeApi {
    private KnowledgeApi() {
    }

    /**
     * @deprecated Use {@link ArsOdysseyApi#grantRelation} instead.
     */
    @Deprecated
    public static boolean onGlyphRelationDiscovered(
            PlayerKnowledgeData knowledgeData,
            ResourceLocation glyphId,
            TargetDescriptor target,
            GlyphRelation relation
    ) {
        if (knowledgeData == null) {
            return false;
        }
        GlyphRelation discoveredRelation = relation != null
                ? relation
                : new GlyphRelation(glyphId, RelationType.RELATED, target,
                java.util.Optional.empty(), "player_discovery");
        return knowledgeData.discover(discoveredRelation, new TruthDelta(1, "player_discovery"));
    }

    /**
     * @deprecated Use {@link ArsOdysseyApi#getView(Player)} and {@link PlayerKnowledgeView} instead.
     */
    @Deprecated
    public static int getTruthValue(PlayerKnowledgeData knowledgeData) {
        return knowledgeData == null ? 0 : knowledgeData.getTruthValue();
    }

    /**
     * @deprecated Use {@link ArsOdysseyApi#hasDiscovered(Player, ResourceLocation, TargetDescriptor)} instead.
     */
    @Deprecated
    public static boolean hasDiscovered(PlayerKnowledgeData knowledgeData, GlyphRelation relation) {
        return knowledgeData != null && knowledgeData.hasDiscovered(relation);
    }

    /**
     * @deprecated Mutate via {@link ArsOdysseyApi#grantRelation} which handles sync automatically.
     */
    @Deprecated
    public static void addTruth(PlayerKnowledgeData knowledgeData, int amount) {
        if (knowledgeData != null) {
            knowledgeData.addTruth(amount);
        }
    }
}
