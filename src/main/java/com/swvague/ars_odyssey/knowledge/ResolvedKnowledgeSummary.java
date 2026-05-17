package com.swvague.ars_odyssey.knowledge;

import net.minecraft.resources.ResourceLocation;

public record ResolvedKnowledgeSummary(
        ResourceLocation glyphId,
        PlayerKnowledgeState state,
        int resolvedCount,
        String targetDisplayName,
        boolean appliesToCurrentTarget
) {
    public ResolvedKnowledgeSummary {
        state = state == null ? PlayerKnowledgeState.UNDISCOVERED : state;
        resolvedCount = Math.max(0, resolvedCount);
        targetDisplayName = targetDisplayName == null ? "" : targetDisplayName;
    }

    public static ResolvedKnowledgeSummary unresolved(ResourceLocation glyphId) {
        return new ResolvedKnowledgeSummary(glyphId, PlayerKnowledgeState.UNDISCOVERED, 0, "", false);
    }

    public static ResolvedKnowledgeSummary resolvedForTarget(ResourceLocation glyphId, String targetDisplayName) {
        return new ResolvedKnowledgeSummary(glyphId, PlayerKnowledgeState.RESOLVED, 1, targetDisplayName, true);
    }

    public static ResolvedKnowledgeSummary resolvedCount(ResourceLocation glyphId, int count) {
        return new ResolvedKnowledgeSummary(glyphId, PlayerKnowledgeState.RESOLVED, count, "", false);
    }

    public static ResolvedKnowledgeSummary resolvedScope(ResourceLocation glyphId, String targetDisplayName) {
        return new ResolvedKnowledgeSummary(glyphId, PlayerKnowledgeState.RESOLVED, 0, targetDisplayName, false);
    }
}
