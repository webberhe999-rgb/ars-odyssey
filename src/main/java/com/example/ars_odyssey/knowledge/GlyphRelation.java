package com.example.ars_odyssey.knowledge;

import net.minecraft.resources.ResourceLocation;

import java.util.Optional;

public record GlyphRelation(
        ResourceLocation glyphId,
        RelationType relationType,
        TargetDescriptor target,
        Optional<TargetDescriptor> result,
        String evidenceKey
) {
    public GlyphRelation {
        relationType = relationType == null ? RelationType.RELATED : relationType;
        target = target == null ? TargetDescriptor.unknown("") : target;
        result = result == null ? Optional.empty() : result;
        evidenceKey = evidenceKey == null ? "" : evidenceKey;
    }
}
