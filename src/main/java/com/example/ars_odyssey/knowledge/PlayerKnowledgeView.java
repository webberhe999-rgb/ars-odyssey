package com.example.ars_odyssey.knowledge;

import net.minecraft.resources.ResourceLocation;

import java.util.Optional;

public interface PlayerKnowledgeView {
    boolean isResolved(ResourceLocation glyphId, TargetDescriptor target);

    int countResolvedRelations(ResourceLocation glyphId);

    Optional<TargetDescriptor> firstAbstractResolvedTarget(ResourceLocation glyphId);

    ResolvedKnowledgeSummary summarizeForTarget(ResourceLocation glyphId, String targetDisplayName, TargetDescriptor target);

    ResolvedKnowledgeSummary summarizeForGlyph(ResourceLocation glyphId);
}
