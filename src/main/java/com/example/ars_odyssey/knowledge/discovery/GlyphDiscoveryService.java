package com.example.ars_odyssey.knowledge.discovery;

import com.example.ars_odyssey.index.EvidenceConfidence;
import com.example.ars_odyssey.knowledge.DiscoverySource;
import com.example.ars_odyssey.knowledge.GlyphRelation;
import com.example.ars_odyssey.knowledge.PlayerKnowledgeData;
import com.example.ars_odyssey.knowledge.TruthDelta;
import com.example.ars_odyssey.knowledge.complexity.GlyphComplexityCalculator;
import com.example.ars_odyssey.knowledge.complexity.GlyphComplexityRecord;
import com.hollingsworth.arsnouveau.api.spell.AbstractSpellPart;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.Optional;

public final class GlyphDiscoveryService {
    private GlyphDiscoveryService() {
    }

    /**
     * Builds the unified discovery result used by future discovery sources.
     *
     * TODO future persistence flow:
     * 1. Receive a GlyphDiscoveryResult from player casting, condition unlocks,
     *    item submissions, boss kills, structure research, automated research,
     *    datapacks, or addon APIs.
     * 2. Read the player's PlayerKnowledgeData from the future attachment/capability.
     * 3. Check whether result.relation() was already discovered.
     * 4. If the relation is new, add it to PlayerKnowledgeData.
     * 5. Apply result.truthDelta() exactly once for that newly discovered relation.
     * 6. Expose the resulting state through PlayerKnowledgeView.
     * 7. The GUI can then prefer MatchDisplayLabel.resolved() over automatic
     *    EvidenceConfidence labels.
     *
     * This method intentionally does not write player data yet.
     */
    public static GlyphDiscoveryResult resolveRelation(
            GlyphRelation relation,
            DiscoverySource source,
            EvidenceConfidence confidence,
            TruthDelta truthDelta
    ) {
        DiscoverySource safeSource = source == null ? DiscoverySource.STATIC_SOURCE_SCAN : source;
        TruthDelta safeTruthDelta = truthDelta == null ? new TruthDelta(0, safeSource.name().toLowerCase(java.util.Locale.ROOT)) : truthDelta;
        return new GlyphDiscoveryResult(relation, safeTruthDelta, safeSource);
    }

    public static GlyphDiscoveryResult debugResolvedRelation(
            GlyphRelation relation,
            DiscoverySource source
    ) {
        return resolveRelation(
                relation,
                source,
                EvidenceConfidence.HIGH,
                new TruthDelta(0, "debug")
        );
    }

    public static boolean applyDiscovery(PlayerKnowledgeData data, GlyphDiscoveryResult result) {
        if (data == null || result == null || result.relation() == null) {
            return false;
        }
        return data.discover(result.relation(), result.truthDelta());
    }

    public static Optional<GlyphComplexityRecord> updateComplexity(
            PlayerKnowledgeData data,
            ResourceLocation glyphId,
            List<AbstractSpellPart> spellParts
    ) {
        if (data == null || glyphId == null || spellParts == null || spellParts.isEmpty()) {
            return Optional.empty();
        }
        GlyphComplexityRecord candidate = GlyphComplexityCalculator.calculate(spellParts, glyphId);
        if (candidate.highestComplexity() <= 1.0D) {
            data.updateComplexity(candidate);
            return Optional.empty();
        }
        return data.updateComplexity(candidate) ? Optional.of(candidate) : Optional.empty();
    }

    public static ResearchResult completeResearch(ResearchRequest request, GlyphDiscoveryResult result) {
        return new ResearchResult(result, result != null, result == null ? "missing_discovery_result" : "");
    }

    public static ResearchResult failResearch(ResearchRequest request, String failureKey) {
        return new ResearchResult(null, false, failureKey);
    }
}
