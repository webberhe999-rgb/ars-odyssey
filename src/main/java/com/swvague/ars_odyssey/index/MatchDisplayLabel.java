package com.swvague.ars_odyssey.index;

import net.minecraft.network.chat.Component;

public record MatchDisplayLabel(
        Component text,
        int color,
        boolean isResolved
) {
    private static final int RESOLVED_COLOR = 0xFFD700;
    private static final int HIGH_COLOR = 0xAA00AA;
    private static final int MEDIUM_COLOR = 0x5555FF;
    private static final int LOW_COLOR = 0x55FF55;
    private static final int UNKNOWN_COLOR = 0xFFFFFF;

    public static MatchDisplayLabel resolved() {
        return new MatchDisplayLabel(Component.translatable("ars_odyssey.tooltip.resolved"), RESOLVED_COLOR, true);
    }

    public static MatchDisplayLabel confidence(EvidenceConfidence confidence) {
        EvidenceConfidence safeConfidence = confidence == null ? EvidenceConfidence.UNKNOWN : confidence;
        return new MatchDisplayLabel(safeConfidence.displayComponent(), colorFor(safeConfidence), false);
    }

    private static int colorFor(EvidenceConfidence confidence) {
        return switch (confidence) {
            case HIGH -> HIGH_COLOR;
            case MEDIUM -> MEDIUM_COLOR;
            case LOW -> LOW_COLOR;
            case UNKNOWN -> UNKNOWN_COLOR;
        };
    }
}
