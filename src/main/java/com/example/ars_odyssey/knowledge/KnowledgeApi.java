package com.example.ars_odyssey.knowledge;

import net.minecraft.resources.ResourceLocation;

public final class KnowledgeApi {
    private KnowledgeApi() {
    }

    public static boolean onGlyphRelationDiscovered(
            PlayerKnowledgeData knowledgeData,
            ResourceLocation glyphId,
            TargetDescriptor target,
            GlyphRelation relation
    ) {
        if (knowledgeData == null) {
            throw new UnsupportedOperationException("Player knowledge data is not connected yet.");
        }
        GlyphRelation discoveredRelation = relation == null
                ? new GlyphRelation(glyphId, RelationType.RELATED, target, java.util.Optional.empty(), "player_discovery")
                : relation;
        return knowledgeData.discover(discoveredRelation, new TruthDelta(1, "player_discovery"));
    }

    public static int getTruthValue(PlayerKnowledgeData knowledgeData) {
        if (knowledgeData == null) {
            throw new UnsupportedOperationException("Player knowledge data is not connected yet.");
        }
        return knowledgeData.getTruthValue();
    }

    public static boolean hasDiscovered(PlayerKnowledgeData knowledgeData, GlyphRelation relation) {
        if (knowledgeData == null) {
            throw new UnsupportedOperationException("Player knowledge data is not connected yet.");
        }
        return knowledgeData.hasDiscovered(relation);
    }

    public static void addTruth(PlayerKnowledgeData knowledgeData, int amount) {
        if (knowledgeData == null) {
            throw new UnsupportedOperationException("Player knowledge data is not connected yet.");
        }
        knowledgeData.addTruth(amount);
    }
}
