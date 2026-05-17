package com.swvague.ars_odyssey.index;

import java.util.ArrayList;
import java.util.List;

public final class MatchReasonDisplayReducer {
    private MatchReasonDisplayReducer() {
    }

    public static List<MatchReason> reduce(List<MatchReason> reasons) {
        if (reasons == null || reasons.isEmpty()) {
            return List.of();
        }

        List<MatchReason> limits = new ArrayList<>();
        MatchReason bestPositive = null;

        for (MatchReason reason : reasons) {
            if (reason == null || reason.matcher() == null) {
                continue;
            }

            if (isLimit(reason)) {
                limits.add(reason);
                continue;
            }

            if (bestPositive == null || compare(reason, bestPositive) > 0) {
                bestPositive = reason;
            }
        }

        List<MatchReason> reduced = new ArrayList<>();
        if (bestPositive != null) {
            reduced.add(bestPositive);
        }
        reduced.addAll(limits);
        return List.copyOf(reduced);
    }

    private static boolean isLimit(MatchReason reason) {
        return reason.blacklist() || MatcherPresentation.roleOf(reason.matcher()) == GlyphTargetRule.TooltipRole.LIMIT;
    }

    private static int compare(MatchReason left, MatchReason right) {
        int confidenceCompare = Integer.compare(confidenceRank(left.confidence()), confidenceRank(right.confidence()));
        if (confidenceCompare != 0) {
            return confidenceCompare;
        }

        int specificityCompare = Integer.compare(specificityRank(left.matcher().type()), specificityRank(right.matcher().type()));
        if (specificityCompare != 0) {
            return specificityCompare;
        }

        boolean sameDisplayKey = !left.displayKey().isBlank() && left.displayKey().equals(right.displayKey());
        if (sameDisplayKey) {
            return 0;
        }

        return 0;
    }

    private static int confidenceRank(EvidenceConfidence confidence) {
        return switch (confidence) {
            case HIGH -> 4;
            case MEDIUM -> 3;
            case LOW -> 2;
            case UNKNOWN -> 1;
        };
    }

    private static int specificityRank(GlyphTargetRule.MatcherType type) {
        return switch (type) {
            case ENTITY_TYPE_TAG, BLOCK_TAG, ITEM_TAG -> 5;
            case ENTITY_CLASS, BLOCK_CLASS -> 4;
            case BEHAVIOR, POSITION_PREDICATE -> 3;
            case INDEXED_TAG -> 2;
            case GENERAL_ENTITY, GENERAL_BLOCK -> 1;
            case BLACKLIST, DENY_ENTITY_TYPE_TAG -> 0;
        };
    }
}
