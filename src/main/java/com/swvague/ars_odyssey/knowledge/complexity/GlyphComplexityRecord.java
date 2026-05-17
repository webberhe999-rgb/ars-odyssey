package com.swvague.ars_odyssey.knowledge.complexity;

import net.minecraft.resources.ResourceLocation;

import java.util.List;

/**
 * 记录某个 effect 魔符历史上达到的最高复杂度快照。
 *
 * 复杂度公式（见 GlyphComplexityCalculator）：
 *   baseTierProduct × duplicateProduct × diversityBonus，上限 6.0
 *
 * achievedByGlyphs：达成最高复杂度时的实际法术结构，格式为
 *   [effectGlyphId, aug1, aug2, aug2, ...]（保留重复，展示真实组合）
 *
 * modifierCount：该次达成中不同增强魔符种类数（distinct augment count）
 *
 * truthEntanglement = highestComplexity × resolvedCount（展示层计算，不在此类存储）
 */
public record GlyphComplexityRecord(
        ResourceLocation glyphId,
        double highestComplexity,
        List<ResourceLocation> achievedByGlyphs,
        int modifierCount
) {
    public GlyphComplexityRecord {
        achievedByGlyphs = achievedByGlyphs == null ? List.of() : List.copyOf(achievedByGlyphs);
        highestComplexity = Math.max(1.0, highestComplexity);
        modifierCount = Math.max(0, modifierCount);
    }

    public static GlyphComplexityRecord initial(ResourceLocation glyphId) {
        return new GlyphComplexityRecord(glyphId, 1.0, List.of(), 0);
    }

    public boolean isBetterThan(GlyphComplexityRecord other) {
        return this.highestComplexity > other.highestComplexity;
    }

    /**
     * 真理纠缠度 = 最高复杂度 × 已解析个数。
     * 在展示层调用，不存储。
     */
    public double truthEntanglement(int resolvedCount) {
        return highestComplexity * resolvedCount;
    }
}
