package com.swvague.ars_odyssey.knowledge;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

import java.util.Objects;
import java.util.Optional;

public class PlayerKnowledgeDataView implements PlayerKnowledgeView {
    private final PlayerKnowledgeData data;
    private final Level level;

    public PlayerKnowledgeDataView(PlayerKnowledgeData data) {
        this(data, null);
    }

    public PlayerKnowledgeDataView(PlayerKnowledgeData data, Level level) {
        this.data = data;
        this.level = level;
    }

    @Override
    public boolean isResolved(ResourceLocation glyphId, TargetDescriptor target) {
        if (data == null || glyphId == null || target == null) {
            return false;
        }

        return data.discoveredRelations().stream()
                .anyMatch(relation -> Objects.equals(glyphId, relation.glyphId())
                        && TargetCoverageResolver.covers(relation.target(), target, level));
    }

    @Override
    public int countResolvedRelations(ResourceLocation glyphId) {
        if (data == null || glyphId == null) {
            return 0;
        }

        return ResolvedTargetCounter.countResolvedTargets(data.discoveredRelations(), glyphId, level);
    }

    @Override
    public Optional<TargetDescriptor> firstAbstractResolvedTarget(ResourceLocation glyphId) {
        if (data == null || glyphId == null) {
            return Optional.empty();
        }

        return data.discoveredRelations().stream()
                .filter(relation -> Objects.equals(glyphId, relation.glyphId()))
                .map(GlyphRelation::target)
                .filter(target -> !isConcreteTarget(target))
                .findFirst();
    }

    private static boolean isConcreteTarget(TargetDescriptor target) {
        return target != null && switch (target.kind()) {
            case BLOCK, ITEM, ENTITY_TYPE -> true;
            default -> false;
        };
    }

    public int countAllResolvedRelations(ResourceLocation glyphId) {
        if (data == null || glyphId == null) {
            return 0;
        }

        return (int) data.discoveredRelations().stream()
                .filter(relation -> Objects.equals(glyphId, relation.glyphId()))
                .count();
    }

    @Override
    public ResolvedKnowledgeSummary summarizeForTarget(ResourceLocation glyphId, String targetDisplayName, TargetDescriptor target) {
        if (isResolved(glyphId, target)) {
            return ResolvedKnowledgeSummary.resolvedForTarget(glyphId, targetDisplayName);
        }
        return ResolvedKnowledgeSummary.unresolved(glyphId);
    }

    @Override
    public ResolvedKnowledgeSummary summarizeForGlyph(ResourceLocation glyphId) {
        int count = countResolvedRelations(glyphId);
        if (count > 0) {
            return ResolvedKnowledgeSummary.resolvedCount(glyphId, count);
        }
        Optional<TargetDescriptor> abstractTarget = firstAbstractResolvedTarget(glyphId);
        if (abstractTarget.isPresent()) {
            return ResolvedKnowledgeSummary.resolvedScope(glyphId, abstractTarget.get().detail());
        }
        return ResolvedKnowledgeSummary.unresolved(glyphId);
    }
}
