package com.swvague.ars_odyssey.archive;

public final class ArchiveAccessPolicy {
    private ArchiveAccessPolicy() {
    }

    public static ArchiveAccessCheck check(PlayerArchiveData data, ArchiveOperationCost cost) {
        if (data == null || !data.isAwakened()) {
            return ArchiveAccessCheck.denied("archive_dormant");
        }
        if (data.coreBinding().isEmpty()) {
            return ArchiveAccessCheck.denied("archive_unbound");
        }
        if (cost.isBlockedByTier()) {
            return ArchiveAccessCheck.denied("tier_forbidden");
        }
        if (cost.itemTier().level() >= 10
                && (cost.operationType() == ArchiveOperationType.EXTRACT
                || cost.operationType() == ArchiveOperationType.PROVISION)) {
            return ArchiveAccessCheck.denied("high_tier_requires_ritual");
        }
        if (!data.unlockedAccessMode().allows(cost.requiredAccessMode())) {
            return ArchiveAccessCheck.denied("requires_" + cost.requiredAccessMode().name().toLowerCase(java.util.Locale.ROOT));
        }
        return ArchiveAccessCheck.allow();
    }

    public record ArchiveAccessCheck(boolean allowed, String reason) {
        private static ArchiveAccessCheck allow() {
            return new ArchiveAccessCheck(true, "allowed");
        }

        private static ArchiveAccessCheck denied(String reason) {
            return new ArchiveAccessCheck(false, reason);
        }
    }
}
