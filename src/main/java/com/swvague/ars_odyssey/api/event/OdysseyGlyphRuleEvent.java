package com.swvague.ars_odyssey.api.event;

import com.swvague.ars_odyssey.index.GlyphTargetRule;
import net.neoforged.bus.api.Event;

import java.util.ArrayList;
import java.util.List;

/**
 * Fired on {@link net.neoforged.neoforge.common.NeoForge#EVENT_BUS NeoForge.EVENT_BUS}
 * during {@code FMLCommonSetupEvent.enqueueWork}.
 *
 * <p>Other mods listen to this event to register their custom glyph target rules
 * into the Ars Odyssey search and tooltip system. Rules registered here will
 * appear in the Odyssey Spell Book's target search results and confidence tooltip
 * alongside the built-in Ars Nouveau rules.
 *
 * <p><strong>Important:</strong> You must register your listener on
 * {@code NeoForge.EVENT_BUS}, <em>not</em> your mod's {@code modEventBus}.
 * This event is a game event posted to the shared NeoForge bus so that
 * all mods can observe it regardless of load order.
 *
 * <p>Example usage in a NeoForge mod:
 * <pre>{@code
 *   // In your mod constructor:
 *   NeoForge.EVENT_BUS.addListener((OdysseyGlyphRuleEvent event) -> {
 *       event.register(new GlyphTargetRule(
 *           ResourceLocation.parse("mymod:glyph_custom"),
 *           List.of(
 *               new GlyphTargetRule.TargetMatcher(
 *                   GlyphTargetRule.MatcherType.ENTITY_CLASS, "LivingEntity")
 *           ),
 *           List.of("entity", "custom"),
 *           "My custom glyph applies to living entities."
 *       ));
 *   });
 * }</pre>
 */
public class OdysseyGlyphRuleEvent extends Event {

    private final List<GlyphTargetRule> rules = new ArrayList<>();

    /** Register one glyph target rule. Null values are silently ignored. */
    public void register(GlyphTargetRule rule) {
        if (rule != null) {
            rules.add(rule);
        }
    }

    /**
     * Returns all rules collected by listeners.
     * This is an internal method — Ars Odyssey calls it after posting the event.
     */
    public List<GlyphTargetRule> getCollectedRules() {
        return List.copyOf(rules);
    }
}
