package com.swvague.ars_odyssey.knowledge.truth;

import com.swvague.ars_odyssey.config.OdysseyConfig;
import com.swvague.ars_odyssey.knowledge.PlayerKnowledgeData;
import com.swvague.ars_odyssey.knowledge.complexity.TruthEntanglementCalculator;
import com.hollingsworth.arsnouveau.api.spell.AbstractAugment;
import com.hollingsworth.arsnouveau.api.spell.AbstractCastMethod;
import com.hollingsworth.arsnouveau.api.spell.AbstractEffect;
import com.hollingsworth.arsnouveau.api.spell.AbstractSpellPart;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * Resolves truthification eligibility for augment glyphs from the effect chain
 * they modify. Augments should not use their own glyph entanglement as the main
 * threshold; they inherit truthification strength from the effect glyphs in the
 * current spell segment.
 */
public final class AugmentTruthificationResolver {
    private AugmentTruthificationResolver() {
    }

    public static double highestModifiedEffectEntanglement(
            PlayerKnowledgeData data,
            List<AbstractSpellPart> spellParts,
            int augmentSlot,
            Level level
    ) {
        if (data == null || spellParts == null || augmentSlot < 0 || augmentSlot >= spellParts.size()) {
            return 0.0D;
        }

        AbstractSpellPart previousNonAugment = previousNonAugment(spellParts, augmentSlot);
        if (previousNonAugment instanceof AbstractEffect effect && effect.getRegistryName() != null) {
            return TruthEntanglementCalculator.glyph(data, effect.getRegistryName(), level);
        }

        double highest = 0.0D;
        for (int i = augmentSlot + 1; i < spellParts.size(); i++) {
            AbstractSpellPart part = spellParts.get(i);
            if (part == null) {
                continue;
            }
            if (part instanceof AbstractCastMethod) {
                break;
            }
            if (part instanceof AbstractEffect effect && effect.getRegistryName() != null) {
                highest = Math.max(highest, TruthEntanglementCalculator.glyph(data, effect.getRegistryName(), level));
            }
        }
        return highest;
    }

    public static boolean orbitSelfUnlocked(
            PlayerKnowledgeData data,
            List<AbstractSpellPart> spellParts,
            int augmentSlot,
            Level level
    ) {
        return highestModifiedEffectEntanglement(data, spellParts, augmentSlot, level)
                >= OdysseyConfig.ORBIT_SELF_TRUTHIFICATION_REQUIRED_ENTANGLEMENT.get();
    }

    public static double orbitTierOneThreshold() {
        return Math.min(
                OdysseyConfig.ORBIT_CONSUME_CHANCE_TIER_ONE.get(),
                OdysseyConfig.ORBIT_SELF_TRUTHIFICATION_REQUIRED_ENTANGLEMENT.get());
    }

    private static AbstractSpellPart previousNonAugment(List<AbstractSpellPart> spellParts, int augmentSlot) {
        for (int i = augmentSlot - 1; i >= 0; i--) {
            AbstractSpellPart part = spellParts.get(i);
            if (part != null && !(part instanceof AbstractAugment)) {
                return part;
            }
        }
        return null;
    }
}
