package com.example.ars_odyssey.index;

import net.minecraft.network.chat.Component;

/**
 * Confidence assigned by Ars Odyssey's automatic/static inference.
 *
 * This is not the player's knowledge state. Player-facing discovery progress
 * should use the knowledge package state enums, where RESOLVED can outrank
 * HIGH/MEDIUM/LOW/UNKNOWN in future UI.
 */
public enum EvidenceConfidence {
    HIGH,
    MEDIUM,
    LOW,
    UNKNOWN;

    public String translationKey() {
        return switch (this) {
            case HIGH -> "ars_odyssey.tooltip.confidence.high";
            case MEDIUM -> "ars_odyssey.tooltip.confidence.medium";
            case LOW -> "ars_odyssey.tooltip.confidence.low";
            case UNKNOWN -> "ars_odyssey.tooltip.confidence.unknown";
        };
    }

    public Component displayComponent() {
        return Component.translatable(translationKey());
    }

    public static EvidenceConfidence fromCandidateConfidence(GlyphRuleCandidate.Confidence confidence) {
        if (confidence == null) {
            return EvidenceConfidence.UNKNOWN;
        }

        return switch (confidence) {
            case HIGH -> EvidenceConfidence.HIGH;
            case MEDIUM -> EvidenceConfidence.MEDIUM;
            case LOW -> EvidenceConfidence.LOW;
            default -> EvidenceConfidence.UNKNOWN;
        };
    }
}
