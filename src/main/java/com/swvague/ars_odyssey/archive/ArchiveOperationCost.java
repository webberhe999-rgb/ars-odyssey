package com.swvague.ars_odyssey.archive;

import com.swvague.ars_odyssey.value.ItemValueTier;

public record ArchiveOperationCost(
        ArchiveOperationType operationType,
        ArchiveAccessMode requiredAccessMode,
        ItemValueTier itemTier,
        long itemCount,
        double sourceCost,
        double resonanceLoad,
        double dissonancePressure
) {
    public boolean isBlockedByTier() {
        return Double.isInfinite(sourceCost);
    }
}
