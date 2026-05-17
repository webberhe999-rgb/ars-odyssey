package com.swvague.ars_odyssey.archive;

public enum ArchiveOperationType {
    DEPOSIT(ArchiveAccessMode.SEALED, 0.5D, 0.0D),
    EXTRACT(ArchiveAccessMode.CALLABLE, 1.0D, 0.1D),
    PROVISION(ArchiveAccessMode.CALLABLE, 1.25D, 0.2D),
    CRAFT(ArchiveAccessMode.PROGRAMMABLE, 2.0D, 0.35D),
    REMOTE_CONTROL(ArchiveAccessMode.PROGRAMMABLE, 2.5D, 0.5D),
    RITUAL(ArchiveAccessMode.CATALYTIC, 3.0D, 0.75D),
    TRANSMUTE(ArchiveAccessMode.CATALYTIC, 4.0D, 1.0D);

    private final ArchiveAccessMode requiredAccessMode;
    private final double sourceMultiplier;
    private final double resonanceLoadMultiplier;

    ArchiveOperationType(ArchiveAccessMode requiredAccessMode, double sourceMultiplier, double resonanceLoadMultiplier) {
        this.requiredAccessMode = requiredAccessMode;
        this.sourceMultiplier = sourceMultiplier;
        this.resonanceLoadMultiplier = resonanceLoadMultiplier;
    }

    public ArchiveAccessMode requiredAccessMode() {
        return requiredAccessMode;
    }

    public double sourceMultiplier() {
        return sourceMultiplier;
    }

    public double resonanceLoadMultiplier() {
        return resonanceLoadMultiplier;
    }
}
