package com.swvague.ars_odyssey.knowledge;

import net.minecraft.resources.ResourceLocation;

import java.util.Optional;

public final class EmptyPlayerKnowledgeView implements PlayerKnowledgeView {
    public static final EmptyPlayerKnowledgeView INSTANCE = new EmptyPlayerKnowledgeView();

    private EmptyPlayerKnowledgeView() {
    }

    @Override
    public boolean isResolved(ResourceLocation glyphId, TargetDescriptor target) {
        return false;
    }

    @Override
    public int countResolvedRelations(ResourceLocation glyphId) {
        return 0;
    }

    @Override
    public Optional<TargetDescriptor> firstAbstractResolvedTarget(ResourceLocation glyphId) {
        return Optional.empty();
    }

    @Override
    public ResolvedKnowledgeSummary summarizeForTarget(ResourceLocation glyphId, String targetDisplayName, TargetDescriptor target) {
        return ResolvedKnowledgeSummary.unresolved(glyphId);
    }

    @Override
    public ResolvedKnowledgeSummary summarizeForGlyph(ResourceLocation glyphId) {
        return ResolvedKnowledgeSummary.unresolved(glyphId);
    }
}
