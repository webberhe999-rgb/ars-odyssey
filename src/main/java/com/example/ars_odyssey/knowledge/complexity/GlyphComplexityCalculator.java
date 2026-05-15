package com.example.ars_odyssey.knowledge.complexity;

import com.hollingsworth.arsnouveau.api.spell.AbstractAugment;
import com.hollingsworth.arsnouveau.api.spell.AbstractSpellPart;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 计算某个 effect 魔符在当前法术中的复杂度。
 *
 * ── 核心规则 ──────────────────────────────────────────────────────────────────
 * 1. 只统计该 effect 后面紧接着的连续 AbstractAugment 段。
 *    遇到下一个非 Augment（Effect / Form）就停止。
 * 2. 如果同一 effect 在法术中出现多次，对每次出现分别计算，取最高复杂度。
 *    不把多个同名 effect 的复杂度相加，也不让一个 effect 吃另一个 effect 后面的 Augment。
 * 3. 当前 effect 前面的 Augment 不算。
 *
 * ── 公式 ──────────────────────────────────────────────────────────────────────
 * 对 effect 后面连续 Augment 段：
 *
 *   complexity = baseTierProduct × duplicateProduct × diversityBonus
 *
 * 其中：
 *
 *   baseTierProduct
 *     = 每种不同 augment 只按自身 tier 计算一次基础系数。
 *
 *   duplicateProduct
 *     = 每种 augment 按出现次数逐项累乘重复奖励。
 *       重复奖励的每一项都会根据该 augment 的 tier 修正。
 *
 *   diversityBonus
 *     = 只根据不同 augment 的种类数计算，不受 tier 影响。
 *
 * 当前没有复杂度上限。
 *
 * ── tier 读取规则 ─────────────────────────────────────────────────────────────
 * augment tier 来自 AugmentTierIndex.tierOf(id)。
 * tier 最小为 1，最大为 99。
 * CREATIVE tier 通常为 99，不再截断为 4。
 *
 * ── base tier factor ─────────────────────────────────────────────────────────
 * 每种不同 augment 只计一次：
 *
 *   factorForTier(tier) = 1.05 + (tier - 1) × 0.20
 *
 * 例：
 *   tier 1  → 1.05
 *   tier 2  → 1.25
 *   tier 3  → 1.45
 *   tier 4  → 1.65
 *   tier 99 → 20.65
 *
 * ── duplicate bonus（同一 augment 重复出现）──────────────────────────────────
 * duplicateBonus 不再是“按 count 查一个总值”，而是按每次出现逐项累乘。
 *
 * 原始边际乘数：
 *   第 1 次 → 1.00
 *   第 2 次 → 1.06
 *   第 3 次 → 1.10
 *   第 4 次 → 1.13
 *   第 5 次及以后 → 1.15
 *
 * 每一个边际乘数都会根据 augment tier 修正：
 *
 *   adjusted = marginal + (tier - 2) × 0.15
 *
 * 例：
 *   tier 2, count 3
 *     = 1.00 × 1.06 × 1.10
 *
 *   tier 3, count 3
 *     = 1.15 × 1.21 × 1.25
 *
 *   tier 1, count 3
 *     = 0.85 × 0.91 × 0.95
 *
 * ── diversity bonus（不同 augment 种类数）────────────────────────────────────
 * diversityBonus 只奖励“不同 augment 种类数”，不考虑 tier：
 *
 *   distinctCount 0/1 → 1.00
 *   distinctCount 2   → 1.20
 *   distinctCount 3   → 1.60
 *   distinctCount 4   → 2.00
 *   distinctCount 5+  → 2.20
 *
 * ── achievedByGlyphs ─────────────────────────────────────────────────────────
 * achievedByGlyphs 保存真实达成结构：
 *
 *   [effectGlyph, aug1, aug2, aug2, ...]
 *
 * 保留顺序，也保留重复 augment，用于 tooltip 展示玩家实际构筑出的组合。
 */
public final class GlyphComplexityCalculator {



    private GlyphComplexityCalculator() {
    }

    // ── 主入口 ──────────────────────────────────────────────────────────────────

    /**
     * 计算指定 effect glyph 在法术中的最高复杂度记录。
     * 同一 effect 多次出现时，取各出现位置的 max。
     */
    public static GlyphComplexityRecord calculate(List<AbstractSpellPart> spellParts, ResourceLocation effectGlyphId) {
        if (spellParts == null || spellParts.isEmpty() || effectGlyphId == null) {
            return GlyphComplexityRecord.initial(effectGlyphId);
        }

        GlyphComplexityRecord best = GlyphComplexityRecord.initial(effectGlyphId);

        for (int i = 0; i < spellParts.size(); i++) {
            AbstractSpellPart part = spellParts.get(i);
            if (part == null || !effectGlyphId.equals(part.getRegistryName())) {
                continue;
            }

            // 收集此出现位置后面连续的 Augment（保留顺序，含重复）
            List<ResourceLocation> augmentSeq = collectAugmentSequence(spellParts, i);

            double complexity = computeComplexity(augmentSeq);
            int distinctCount = (int) augmentSeq.stream().distinct().count();

            // achievedByGlyphs 展示真实法术结构：[effectGlyph, aug1, aug2, aug2, ...]
            List<ResourceLocation> achievedBy = new ArrayList<>(augmentSeq.size() + 1);
            achievedBy.add(effectGlyphId);
            achievedBy.addAll(augmentSeq);

            GlyphComplexityRecord candidate = new GlyphComplexityRecord(
                    effectGlyphId, complexity, achievedBy, distinctCount);

            best = keepHigher(best, candidate);
        }
        return best;
    }

    // ── 子序列采集 ──────────────────────────────────────────────────────────────

    /**
     * 从 effectIndex+1 开始，收集连续 AbstractAugment 的 id 列表（含重复，保留顺序）。
     * 遇到非 Augment 立刻停止。
     */
    private static List<ResourceLocation> collectAugmentSequence(List<AbstractSpellPart> parts, int effectIndex) {
        List<ResourceLocation> result = new ArrayList<>();
        for (int i = effectIndex + 1; i < parts.size(); i++) {
            AbstractSpellPart p = parts.get(i);
            if (!(p instanceof AbstractAugment)) break;
            ResourceLocation id = p.getRegistryName();
            if (id != null) result.add(id);
        }
        return result;
    }

    // ── 复杂度计算 ──────────────────────────────────────────────────────────────

    /**
     * 给定 augment 序列（含重复、保留顺序），计算该 effect 位置的复杂度。
     * 供单元测试或外部校验使用（package-private visibility intentional）。
     */
    static double computeComplexity(List<ResourceLocation> augmentSeq) {
        if (augmentSeq.isEmpty()) return 1.0;

        // 分组计数，保留 insertion order 以便调试
        Map<ResourceLocation, Integer> counts = new LinkedHashMap<>();
        for (ResourceLocation id : augmentSeq) {
            counts.merge(id, 1, Integer::sum);
        }

        int distinctCount = counts.size();

        // baseTierProduct：每种 augment 只取一次 tier factor
        double baseTierProduct = 1.0;
        for (ResourceLocation id : counts.keySet()) {
            baseTierProduct *= factorForTier(AugmentTierIndex.tierOf(id));
        }

        // duplicateProduct：每种 augment 按出现次数逐项累乘，并且每一项受该 augment 的 tier 修正
        double duplicateProduct = 1.0;
        for (Map.Entry<ResourceLocation, Integer> entry : counts.entrySet()) {
            ResourceLocation id = entry.getKey();
            int count = entry.getValue();
            int tier = AugmentTierIndex.tierOf(id);

            duplicateProduct *= duplicateBonus(count, tier);
        }

        // 不再设置复杂度上限
        return baseTierProduct * duplicateProduct * diversityBonus(distinctCount);

    }

    // ── 奖励函数（公开，便于测试）──────────────────────────────────────────────

    /** tier 1→1.05  tier 2→1.25  tier 3→1.45  tier 4→1.65 */
    public static double factorForTier(int tier) {
        int clamped = Math.min(Math.max(tier, AugmentTierIndex.MIN_TIER), AugmentTierIndex.MAX_TIER);
        return 1.05 + (clamped - 1) * 0.20;
    }

    /** distinctCount：1→1.00  2→1.20  3→1.60  4→2.00  5+→2.20 */
    public static double diversityBonus(int distinctCount) {
        return switch (distinctCount) {
            case 0, 1 -> 1.00;
            case 2 -> 1.20;
            case 3 -> 1.60;
            case 4 -> 2.00;
            default -> 2.20;
        };
    }

    /**
     * 重复奖励：每次出现逐项累乘，并按 augment tier 修正。
     *
     * 基础边际乘数：
     * 第1次 → 1.00
     * 第2次 → 1.06
     * 第3次 → 1.10
     * 第4次 → 1.13
     * 第5次及以后 → 1.15
     *
     * tier 修正：
     * adjusted = marginal + (tier - 2) * 0.15
     *
     * 例：
     * tier 2, count 3 = 1.00 × 1.06 × 1.10
     * tier 3, count 3 = 1.15 × 1.21 × 1.25
     */
    public static double duplicateBonus(int count, int tier) {
        if (count <= 0) return 1.0;

        int clampedTier = Math.min(Math.max(tier, AugmentTierIndex.MIN_TIER), AugmentTierIndex.MAX_TIER);
        double tierOffset = (clampedTier - 2) * 0.15;

        double product = 1.0;

        for (int occurrence = 1; occurrence <= count; occurrence++) {
            double marginal;

            if (occurrence == 1) {
                marginal = 1.00;
            } else if (occurrence == 2) {
                marginal = 1.06;
            } else if (occurrence == 3) {
                marginal = 1.10;
            } else if (occurrence == 4) {
                marginal = 1.13;
            } else {
                marginal = 1.15;
            }

            product *= marginal + tierOffset;
        }

        return product;
    }

    /**
     * 向后兼容：不指定 tier 时，按 tier 2 计算，也就是不修正。
     */
    public static double duplicateBonus(int count) {
        return duplicateBonus(count, 2);
    }

    /**
     * 第 n 次出现该 augment 时提供的原始边际奖励。
     */
    private static double duplicateMarginalBonus(int occurrence) {
        if (occurrence <= 1) return 1.00;
        if (occurrence == 2) return 1.06;
        if (occurrence == 3) return 1.10;
        if (occurrence == 4) return 1.13;
        return 1.15;
    }

    /**
     * 根据 augment tier 修正每一个重复奖励边际项。
     */
    private static double applyTierToDuplicateMarginal(double marginal, int tier) {
        int clamped = Math.min(Math.max(tier, 1), 4);

        return switch (clamped) {
            case 1 -> marginal * 0.60;
            case 2 -> marginal;
            case 3 -> marginal + 0.15;
            case 4 -> marginal + 0.30;
            default -> marginal;
        };
    }

    // ── 工具 ────────────────────────────────────────────────────────────────────

    /**
     * 比较两条记录，返回复杂度更高的那条。
     */
    public static GlyphComplexityRecord keepHigher(GlyphComplexityRecord existing, GlyphComplexityRecord candidate) {
        return candidate.isBetterThan(existing) ? candidate : existing;
    }

    /**
     * 向后兼容：返回 achievedByGlyphs 中排除 effect 自身后的 modifier 列表。
     * 若某个 effect 出现多次，以复杂度最高的那次的序列为准。
     */
    public static List<ResourceLocation> collectModifierGlyphs(
            List<AbstractSpellPart> spellParts, ResourceLocation effectGlyphId) {
        GlyphComplexityRecord record = calculate(spellParts, effectGlyphId);
        List<ResourceLocation> modifiers = new ArrayList<>(record.achievedByGlyphs());
        // achievedByGlyphs 第一个元素是 effectGlyphId 本身，去掉
        if (!modifiers.isEmpty() && effectGlyphId != null && effectGlyphId.equals(modifiers.get(0))) {
            modifiers.remove(0);
        }
        return modifiers;
    }
}
