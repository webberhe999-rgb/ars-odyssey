package com.swvague.ars_odyssey.archive;

import com.swvague.ars_odyssey.archive.cost.ArchivePaymentResult;
import com.swvague.ars_odyssey.archive.resonance.ResonanceRiskLevel;

public record ArchiveOperationResult(
        boolean success,
        String reason,
        long changedCount,
        long storedCount,
        ArchiveOperationCost cost,
        ArchivePaymentResult payment,
        ResonanceRiskLevel resonanceRiskLevel
) {
    public static ArchiveOperationResult success(long changedCount, long storedCount, ArchiveOperationCost cost, ArchivePaymentResult payment, ResonanceRiskLevel riskLevel) {
        return new ArchiveOperationResult(true, "ok", changedCount, storedCount, cost, payment, riskLevel);
    }

    public static ArchiveOperationResult failure(String reason, long storedCount, ArchiveOperationCost cost) {
        return new ArchiveOperationResult(false, reason, 0L, storedCount, cost, null, null);
    }

    public static ArchiveOperationResult failure(String reason, long storedCount, ArchiveOperationCost cost, ArchivePaymentResult payment) {
        return new ArchiveOperationResult(false, reason, 0L, storedCount, cost, payment, null);
    }
}
