package com.swvague.ars_odyssey.knowledge.complexity;

import com.swvague.ars_odyssey.knowledge.GlyphRelation;
import com.swvague.ars_odyssey.knowledge.PlayerKnowledgeData;
import com.swvague.ars_odyssey.knowledge.ResolvedTargetCounter;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Calculates the current truth entanglement ledger from discovered knowledge.
 *
 * <p>The stored {@code totalTruthEntanglement} field is a historical counter kept
 * for save compatibility and command/debug bookkeeping. Player-facing totals
 * should use this calculator so the total equals the sum of each glyph's current
 * contribution.</p>
 */
public final class TruthEntanglementCalculator {
    private TruthEntanglementCalculator() {
    }

    public static double total(PlayerKnowledgeData data, Level level) {
        if (data == null) {
            return 0.0D;
        }

        double total = 0.0D;
        for (ResourceLocation glyphId : discoveredGlyphIds(data)) {
            total += glyph(data, glyphId, level);
        }
        return total;
    }

    public static double glyph(PlayerKnowledgeData data, ResourceLocation glyphId, Level level) {
        if (data == null || glyphId == null) {
            return 0.0D;
        }

        int resolvedCount = ResolvedTargetCounter.countResolvedTargets(
                data.discoveredRelations(),
                glyphId,
                level);
        if (resolvedCount <= 0) {
            return 0.0D;
        }

        double complexity = data.getComplexityRecord(glyphId).highestComplexity();
        double confidenceMultiplier = EntanglementConfidenceMultiplier.forGlyph(glyphId);
        return complexity * resolvedCount * confidenceMultiplier;
    }

    private static Set<ResourceLocation> discoveredGlyphIds(PlayerKnowledgeData data) {
        Set<ResourceLocation> glyphIds = new LinkedHashSet<>();
        for (GlyphRelation relation : data.discoveredRelations()) {
            if (relation != null && relation.glyphId() != null) {
                glyphIds.add(relation.glyphId());
            }
        }
        return glyphIds;
    }
}
