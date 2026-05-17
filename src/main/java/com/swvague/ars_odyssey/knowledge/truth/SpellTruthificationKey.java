package com.swvague.ars_odyssey.knowledge.truth;

import com.hollingsworth.arsnouveau.api.spell.AbstractSpellPart;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.Locale;

/**
 * Stable key for a truthification toggle on one glyph slot in one spell recipe.
 */
public final class SpellTruthificationKey {
    private SpellTruthificationKey() {
    }

    public static String forSlot(List<AbstractSpellPart> spell, int slot, ResourceLocation glyphId) {
        if (spell == null || slot < 0 || glyphId == null) {
            return "";
        }
        return fingerprint(spell) + "|slot=" + slot + "|glyph=" + glyphId;
    }

    public static String fingerprint(List<AbstractSpellPart> spell) {
        if (spell == null || spell.isEmpty()) {
            return "empty";
        }
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < spell.size(); i++) {
            if (i > 0) {
                builder.append('>');
            }
            AbstractSpellPart part = spell.get(i);
            ResourceLocation id = part == null ? null : part.getRegistryName();
            builder.append(id == null ? "null" : id.toString().toLowerCase(Locale.ROOT));
        }
        return builder.toString();
    }
}
