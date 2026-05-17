package com.swvague.ars_odyssey.archive.runtime;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

public class ArchiveRuntimeState {
    private final EnumSet<ArchiveDirtyReason> dirtyReasons = EnumSet.noneOf(ArchiveDirtyReason.class);
    private long revision;
    private long lastRebuildGameTime = Long.MIN_VALUE;

    public long revision() {
        return revision;
    }

    public long lastRebuildGameTime() {
        return lastRebuildGameTime;
    }

    public boolean isDirty() {
        return !dirtyReasons.isEmpty();
    }

    public Set<ArchiveDirtyReason> dirtyReasons() {
        return Collections.unmodifiableSet(dirtyReasons);
    }

    public void markDirty(ArchiveDirtyReason reason) {
        if (reason != null && dirtyReasons.add(reason)) {
            revision++;
        }
    }

    public boolean canRebuild(long gameTime, ArchiveRuntimeBudget budget) {
        return isDirty() && gameTime - lastRebuildGameTime >= budget.minRebuildIntervalTicks();
    }

    public Set<ArchiveDirtyReason> beginRebuild(long gameTime) {
        EnumSet<ArchiveDirtyReason> reasons = dirtyReasons.isEmpty()
                ? EnumSet.noneOf(ArchiveDirtyReason.class)
                : EnumSet.copyOf(dirtyReasons);
        dirtyReasons.clear();
        lastRebuildGameTime = gameTime;
        return Collections.unmodifiableSet(reasons);
    }
}
