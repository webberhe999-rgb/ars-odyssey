package com.swvague.ars_odyssey.archive.runtime;

import java.util.Set;

public final class ArchiveRuntimeScheduler {
    private ArchiveRuntimeScheduler() {
    }

    public static ArchiveRuntimeStep planStep(ArchiveRuntimeState state, long gameTime, ArchiveRuntimeBudget budget) {
        if (!state.canRebuild(gameTime, budget)) {
            return ArchiveRuntimeStep.idle(state.revision());
        }
        return ArchiveRuntimeStep.rebuild(state.revision(), state.beginRebuild(gameTime), budget);
    }

    public record ArchiveRuntimeStep(
            boolean rebuild,
            long revision,
            Set<ArchiveDirtyReason> reasons,
            int nodeBudget,
            int ledgerBudget
    ) {
        private static ArchiveRuntimeStep idle(long revision) {
            return new ArchiveRuntimeStep(false, revision, Set.of(), 0, 0);
        }

        private static ArchiveRuntimeStep rebuild(long revision, Set<ArchiveDirtyReason> reasons, ArchiveRuntimeBudget budget) {
            return new ArchiveRuntimeStep(
                    true,
                    revision,
                    reasons,
                    budget.maxNodeChecksPerTick(),
                    budget.maxLedgerEntriesPerTick()
            );
        }
    }
}
