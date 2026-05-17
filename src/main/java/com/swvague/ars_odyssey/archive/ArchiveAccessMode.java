package com.swvague.ars_odyssey.archive;

public enum ArchiveAccessMode {
    SEALED(0),
    INDEXED(1),
    CALLABLE(2),
    PROGRAMMABLE(3),
    CATALYTIC(4);

    private final int rank;

    ArchiveAccessMode(int rank) {
        this.rank = rank;
    }

    public int rank() {
        return rank;
    }

    public boolean allows(ArchiveAccessMode required) {
        return rank >= required.rank;
    }
}
