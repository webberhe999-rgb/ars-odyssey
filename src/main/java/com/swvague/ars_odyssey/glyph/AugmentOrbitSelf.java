package com.swvague.ars_odyssey.glyph;

import com.swvague.ars_odyssey.ArsOdyssey;
import com.hollingsworth.arsnouveau.api.spell.AbstractAugment;
import com.hollingsworth.arsnouveau.api.spell.SpellTier;

public class AugmentOrbitSelf extends AbstractAugment {
    public static final AugmentOrbitSelf INSTANCE = new AugmentOrbitSelf();

    private AugmentOrbitSelf() {
        super(ArsOdyssey.prefix("glyph_orbit_self"), "Orbit");
    }

    @Override
    public int getDefaultManaCost() {
        return 10;
    }

    @Override
    public SpellTier defaultTier() {
        return SpellTier.TWO;
    }

    @Override
    public String getBookDescription() {
        return "Augments Projectile spells, turning their projectiles into orbiting turrets around the caster until they hit. Stack for more rings.";
    }
}
