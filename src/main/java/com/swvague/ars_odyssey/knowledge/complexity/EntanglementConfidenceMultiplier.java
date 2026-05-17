package com.swvague.ars_odyssey.knowledge.complexity;

import com.swvague.ars_odyssey.index.EvidenceConfidence;
import com.swvague.ars_odyssey.index.GlyphTargetRule;
import com.swvague.ars_odyssey.index.RuntimeGlyphClassifier;
import net.minecraft.resources.ResourceLocation;

/**
 * 基于 {@link RuntimeGlyphClassifier} 对魔符的推断置信度，返回真理纠缠度奖励倍率。
 *
 * <h3>设计理念</h3>
 * 分类器置信度越低，说明该魔符的行为对系统而言越难预测。
 * 玩家能在这种"更陌生"的魔符上成功完成发现，理应获得更高的真理纠缠度奖励。
 *
 * <table>
 *   <tr><th>推断置信</th><th>倍率</th></tr>
 *   <tr><td>LOW / UNKNOWN</td><td>×{@value #MULTIPLIER_LOW}</td></tr>
 *   <tr><td>MEDIUM</td><td>×{@value #MULTIPLIER_MEDIUM}</td></tr>
 *   <tr><td>HIGH 或手动规则</td><td>×{@value #MULTIPLIER_HIGH}（无加成）</td></tr>
 * </table>
 *
 * <p>手动规则（{@link ManualGlyphRules}）覆盖的魔符不会出现在推断规则集里，
 * 因此始终返回 {@link #MULTIPLIER_HIGH}（1.0）——已知魔符不额外奖励。
 */
public final class EntanglementConfidenceMultiplier {

    /** 低置信推断魔符的纠缠奖励倍率（系统几乎是猜测）。 */
    public static final double MULTIPLIER_LOW = 2.00;

    /** 中置信推断魔符的纠缠奖励倍率（系统有部分信号）。 */
    public static final double MULTIPLIER_MEDIUM = 1.35;

    /** 高置信或手动规则魔符的纠缠奖励倍率（无额外加成）。 */
    public static final double MULTIPLIER_HIGH = 1.00;

    private EntanglementConfidenceMultiplier() {
    }

    /**
     * 返回给定魔符的纠缠倍率。
     *
     * <p>若该魔符存在手动规则（被分类器跳过），则视为 HIGH，倍率 = {@link #MULTIPLIER_HIGH}。
     *
     * @param glyphId 魔符的注册 ID；为 {@code null} 时返回 {@link #MULTIPLIER_HIGH}
     * @return 应乘以当前复杂度以得到纠缠 delta 的倍率
     */
    public static double forGlyph(ResourceLocation glyphId) {
        if (glyphId == null) {
            return MULTIPLIER_HIGH;
        }
        EvidenceConfidence best = bestInferredConfidence(glyphId);
        return switch (best) {
            case LOW, UNKNOWN -> MULTIPLIER_LOW;
            case MEDIUM -> MULTIPLIER_MEDIUM;
            case HIGH -> MULTIPLIER_HIGH;
        };
    }

    /**
     * 查询分类器推断规则集中该魔符的最佳置信度。
     * 若无推断规则（有手动规则或从未被分类），返回 {@link EvidenceConfidence#HIGH}。
     */
    private static EvidenceConfidence bestInferredConfidence(ResourceLocation glyphId) {
        // getRuleSet() fetches ManualGlyphRules internally — no package-private access needed.
        RuntimeGlyphClassifier.InferredRuleSet ruleSet = RuntimeGlyphClassifier.getRuleSet();

        for (GlyphTargetRule rule : ruleSet.rules()) {
            if (!glyphId.equals(rule.glyphId())) {
                continue;
            }
            EvidenceConfidence best = EvidenceConfidence.UNKNOWN;
            for (GlyphTargetRule.TargetMatcher matcher : rule.matchers()) {
                if (ord(matcher.confidence()) > ord(best)) {
                    best = matcher.confidence();
                }
            }
            return best;
        }

        // 无推断规则 → 手动/auto 规则覆盖 → 已知魔符，不额外奖励
        return EvidenceConfidence.HIGH;
    }

    private static int ord(EvidenceConfidence c) {
        return switch (c) {
            case HIGH -> 3;
            case MEDIUM -> 2;
            case LOW -> 1;
            case UNKNOWN -> 0;
        };
    }
}
