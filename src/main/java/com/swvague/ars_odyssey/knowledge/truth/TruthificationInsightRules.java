package com.swvague.ars_odyssey.knowledge.truth;

import com.swvague.ars_odyssey.config.OdysseyConfig;

/**
 * Centralizes "complete insight" thresholds for truthified spell behavior.
 *
 * <p>For Orbit Self, complete insight is intentionally derived from the third
 * consume-chance tier. That keeps the rule tied to this glyph's own
 * truthification curve instead of another global magic number.</p>
 */
public final class TruthificationInsightRules {
    private TruthificationInsightRules() {
    }

    public static double orbitSelfThirdCondition() {
        return OdysseyConfig.ORBIT_CONSUME_CHANCE_TIER_THREE.get();
    }

    public static double orbitSelfCompleteInsightRequirement() {
        return orbitSelfThirdCondition() * 15.0D;
    }

    public static boolean hasOrbitSelfCompleteInsight(double totalTruthEntanglement) {
        return totalTruthEntanglement > orbitSelfCompleteInsightRequirement();
    }
}
