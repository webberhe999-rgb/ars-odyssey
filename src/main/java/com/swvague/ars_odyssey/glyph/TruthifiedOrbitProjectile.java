package com.swvague.ars_odyssey.glyph;

import java.util.UUID;

public interface TruthifiedOrbitProjectile {
    void ars_odyssey$setTruthifiedOrbit(double totalTruthEntanglement, double consumeChance, UUID spellGroupId);

    boolean ars_odyssey$isTruthifiedOrbit();

    double ars_odyssey$getConsumeChance();

    UUID ars_odyssey$getSpellGroupId();

    /**
     * @return true if vanilla removal should continue; false if truthification preserved this projectile.
     */
    boolean ars_odyssey$shouldConsumeOnHit();
}
