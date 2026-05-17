package com.swvague.ars_odyssey.registry;

import com.swvague.ars_odyssey.glyph.AugmentOrbitSelf;
import com.hollingsworth.arsnouveau.api.registry.GlyphRegistry;
import com.hollingsworth.arsnouveau.common.spell.method.MethodProjectile;

public final class OdysseyGlyphRegistry {
    private static boolean registered;

    private OdysseyGlyphRegistry() {
    }

    public static void registerGlyphs() {
        if (registered) {
            return;
        }
        GlyphRegistry.registerSpell(AugmentOrbitSelf.INSTANCE);
        MethodProjectile.INSTANCE.compatibleAugments.add(AugmentOrbitSelf.INSTANCE);
        registered = true;
    }
}
