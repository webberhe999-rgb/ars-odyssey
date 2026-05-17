package com.swvague.ars_odyssey.archive.cost;

public record ArchivePaymentResult(
        boolean success,
        String reason,
        int sourcePaid,
        double resonanceLoadAccepted,
        double dissonancePressureAccepted
) {
    public static ArchivePaymentResult success(int sourcePaid, double resonanceLoadAccepted, double dissonancePressureAccepted) {
        return new ArchivePaymentResult(true, "paid", sourcePaid, resonanceLoadAccepted, dissonancePressureAccepted);
    }

    public static ArchivePaymentResult skipped(String reason, double resonanceLoadAccepted, double dissonancePressureAccepted) {
        return new ArchivePaymentResult(true, reason, 0, resonanceLoadAccepted, dissonancePressureAccepted);
    }

    public static ArchivePaymentResult failure(String reason, int sourcePaid) {
        return new ArchivePaymentResult(false, reason, sourcePaid, 0.0D, 0.0D);
    }
}
