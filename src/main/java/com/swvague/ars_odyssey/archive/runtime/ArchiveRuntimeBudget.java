package com.swvague.ars_odyssey.archive.runtime;

public record ArchiveRuntimeBudget(
        int maxNodeChecksPerTick,
        int maxLedgerEntriesPerTick,
        int minRebuildIntervalTicks
) {
    public static final ArchiveRuntimeBudget DEFAULT = new ArchiveRuntimeBudget(16, 64, 10);
}
