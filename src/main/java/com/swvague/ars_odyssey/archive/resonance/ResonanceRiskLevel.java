package com.swvague.ars_odyssey.archive.resonance;

public enum ResonanceRiskLevel {
    STABLE,
    STRAINED,
    UNSTABLE,
    OVERLOADED,
    DISSONANT;

    public static ResonanceRiskLevel from(ResonanceState state) {
        if (state.dissonancePressure() >= 100.0D) {
            return DISSONANT;
        }
        if (state.isOverloaded()) {
            return OVERLOADED;
        }
        if (state.stability() < 35.0D || state.dissonancePressure() >= 50.0D) {
            return UNSTABLE;
        }
        if (state.stability() < 70.0D || state.load() >= state.stability() * 0.6D) {
            return STRAINED;
        }
        return STABLE;
    }
}
