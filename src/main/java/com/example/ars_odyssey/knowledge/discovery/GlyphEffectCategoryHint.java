package com.example.ars_odyssey.knowledge.discovery;

import com.example.ars_odyssey.index.EvidenceConfidence;
import com.example.ars_odyssey.knowledge.DiscoverySource;
import net.minecraft.resources.ResourceLocation;

import java.util.Set;

public record GlyphEffectCategoryHint(
        ResourceLocation glyphId,
        Set<GlyphEffectCategory> categories,
        EvidenceConfidence confidence,
        DiscoverySource source,
        String evidenceKey
) {
    public GlyphEffectCategoryHint {
        categories = categories == null || categories.isEmpty()
                ? Set.of(GlyphEffectCategory.UNKNOWN)
                : Set.copyOf(categories);
        confidence = confidence == null ? EvidenceConfidence.UNKNOWN : confidence;
        source = source == null ? DiscoverySource.STATIC_SOURCE_SCAN : source;
        evidenceKey = evidenceKey == null ? "" : evidenceKey;
    }
}
