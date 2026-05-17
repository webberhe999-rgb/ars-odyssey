package com.swvague.ars_odyssey.index;

import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.Optional;

import static com.swvague.ars_odyssey.index.GlyphTargetRule.EvidenceLevel.INFERRED;
import static com.swvague.ars_odyssey.index.GlyphTargetRule.MatcherType.BEHAVIOR;
import static com.swvague.ars_odyssey.index.GlyphTargetRule.MatcherType.BLACKLIST;
import static com.swvague.ars_odyssey.index.GlyphTargetRule.MatcherType.BLOCK_CLASS;
import static com.swvague.ars_odyssey.index.GlyphTargetRule.MatcherType.BLOCK_TAG;
import static com.swvague.ars_odyssey.index.GlyphTargetRule.MatcherType.DENY_ENTITY_TYPE_TAG;
import static com.swvague.ars_odyssey.index.GlyphTargetRule.MatcherType.ENTITY_CLASS;
import static com.swvague.ars_odyssey.index.GlyphTargetRule.MatcherType.GENERAL_BLOCK;
import static com.swvague.ars_odyssey.index.GlyphTargetRule.MatcherType.GENERAL_ENTITY;
import static com.swvague.ars_odyssey.index.GlyphTargetRule.MatcherType.POSITION_PREDICATE;

final class ManualGlyphRules {
    // Built-in rules are immutable and defined at class-load time.
    // registerExternal() appends addon rules once during FMLCommonSetupEvent.
    private static final List<GlyphTargetRule> BUILT_IN = List.of(
            rule("ars_nouveau:glyph_harm",
                    List.of(
                            matcher(GENERAL_ENTITY, "true"),
                            matcher(BLACKLIST, "entity_class:ItemEntity"),
                            matcher(ENTITY_CLASS, "LivingEntity"),
                            matcher(BEHAVIOR, "damage_entity"),
                            matcher(BEHAVIOR, "poison_living_entity_with_duration")
                    ),
                    List.of("damage", "entity", "potion"),
                    "Damages non-item entities; LivingEntity targets can also enter the poison duration branch."),
            rule("ars_nouveau:glyph_heal",
                    List.of(
                            matcher(ENTITY_CLASS, "LivingEntity"),
                            matcher(BEHAVIOR, "heal_living_entity"),
                            matcher(BEHAVIOR, "inverted_heal_and_harm")
                    ),
                    List.of("heal", "entity"),
                    "Heals LivingEntity targets; inverted heal/harm entities are damaged instead."),
            rule("ars_nouveau:glyph_ignite",
                    List.of(
                            matcher(GENERAL_ENTITY, "true"),
                            matcher(BLOCK_CLASS, "CandleBlock"),
                            matcher(BLOCK_CLASS, "CampfireBlock"),
                            matcher(POSITION_PREDICATE, "fire_can_be_placed"),
                            matcher(BEHAVIOR, "set_entity_on_fire"),
                            matcher(BEHAVIOR, "lightable_block", INFERRED)
                    ),
                    List.of("fire", "entity", "block"),
                    "Sets entities on fire; lights candles/campfires or places fire when the target position allows it."),
            rule("ars_nouveau:glyph_freeze",
                    List.of(
                            matcher(ENTITY_CLASS, "LivingEntity"),
                            matcher(BLOCK_TAG, "minecraft:fire"),
                            matcher(BLOCK_CLASS, "LiquidBlock"),
                            matcher(BEHAVIOR, "water_to_ice"),
                            matcher(BEHAVIOR, "lava_to_obsidian_or_cobblestone"),
                            matcher(BEHAVIOR, "ice_upgrade")
                    ),
                    List.of("water", "ice", "entity"),
                    "Slows LivingEntity targets and transforms water, lava, fire, and ice-like blocks."),
            rule("ars_nouveau:glyph_break",
                    List.of(
                            matcher(GENERAL_BLOCK, "true"),
                            matcher(BLOCK_TAG, "ars_nouveau:break_with_pickaxe"),
                            matcher(BLACKLIST, "block_tag:ars_nouveau:break_blacklist"),
                            matcher(BEHAVIOR, "can_block_be_harvested"),
                            matcher(BEHAVIOR, "destroy_respects_claim")
                    ),
                    List.of("block", "mining"),
                    "Breaks harvestable blocks subject to harvest level, claim checks, and break blacklist."),
            rule("ars_nouveau:glyph_crush",
                    List.of(
                            matcher(GENERAL_ENTITY, "true"),
                            matcher(GENERAL_BLOCK, "true"),
                            matcher(ENTITY_CLASS, "ItemEntity"),
                            matcher(BEHAVIOR, "crush_recipe_matches"),
                            matcher(BEHAVIOR, "damage_entity")
                    ),
                    List.of("block", "item", "damage"),
                    "Crushes blocks/items through Ars crush recipes and damages entities."),
            rule("ars_nouveau:glyph_smelt",
                    List.of(
                            matcher(GENERAL_BLOCK, "true"),
                            matcher(ENTITY_CLASS, "ItemEntity"),
                            matcher(BEHAVIOR, "smelting_recipe_matches"),
                            matcher(BEHAVIOR, "smoking_recipe_matches"),
                            matcher(BEHAVIOR, "blasting_recipe_matches"),
                            matcher(BEHAVIOR, "can_block_be_harvested")
                    ),
                    List.of("block", "item", "recipe"),
                    "Smelts blocks or nearby item entities when a furnace/smoking/blasting recipe exists."),
            rule("ars_nouveau:glyph_exchange",
                    List.of(
                            matcher(GENERAL_ENTITY, "true"),
                            matcher(GENERAL_BLOCK, "true"),
                            matcher(BEHAVIOR, "requires_block_item"),
                            matcher(BEHAVIOR, "can_block_be_harvested"),
                            matcher(BEHAVIOR, "teleport_entity_swap")
                    ),
                    List.of("block", "entity", "swap"),
                    "Swaps entity positions or exchanges blocks with BlockItem stacks from the caster inventory."),
            rule("ars_nouveau:glyph_place_block",
                    List.of(
                            matcher(POSITION_PREDICATE, "replaceable_block_position"),
                            matcher(BEHAVIOR, "requires_block_item"),
                            matcher(GENERAL_BLOCK, "true", INFERRED)
                    ),
                    List.of("block", "place"),
                    "Places a BlockItem into a replaceable target position."),
            rule("ars_nouveau:glyph_pickup",
                    List.of(
                            matcher(ENTITY_CLASS, "ItemEntity"),
                            matcher(ENTITY_CLASS, "ExperienceOrb"),
                            matcher(BEHAVIOR, "pickup_into_inventory")
                    ),
                    List.of("item", "pickup"),
                    "Picks up nearby item entities and experience orbs into the caster inventory/player."),
            rule("ars_nouveau:glyph_grow",
                    List.of(
                            matcher(BEHAVIOR, "bonemealable"),
                            matcher(BEHAVIOR, "water_plant_bonemealable")
                    ),
                    List.of("growth", "ripen"),
                    "Uses BoneMealItem.applyBonemeal and growWaterPlant."),
            rule("ars_nouveau:glyph_harvest",
                    List.of(
                            matcher(BLOCK_CLASS, "CropBlock"),
                            matcher(BLOCK_CLASS, "NetherWartBlock"),
                            matcher(BLOCK_CLASS, "CocoaBlock"),
                            matcher(BLOCK_TAG, "ars_nouveau:harvest/stems"),
                            matcher(BEHAVIOR, "mature_crop_harvestable")
                    ),
                    List.of("harvest", "crops"),
                    "Harvests mature crops and replants/reset age where applicable."),
            rule("ars_nouveau:glyph_blink",
                    List.of(
                            matcher(GENERAL_ENTITY, "true"),
                            matcher(DENY_ENTITY_TYPE_TAG, "neoforge:teleporting_not_supported"),
                            matcher(POSITION_PREDICATE, "valid_teleport_position"),
                            matcher(BEHAVIOR, "teleport_entity_or_caster")
                    ),
                    List.of("teleport"),
                    "Teleports caster/entity, excluding entity types that cannot teleport."),
            rule("ars_nouveau:glyph_bounce",
                    List.of(
                            matcher(ENTITY_CLASS, "LivingEntity"),
                            matcher(BEHAVIOR, "apply_bounce_effect")
                    ),
                    List.of("status_effect", "mobility"),
                    "Applies Bounce potion effect to LivingEntity."),
            rule("ars_nouveau:glyph_animate_block",
                    List.of(
                            matcher(BEHAVIOR, "enchanted_falling_block_can_fall"),
                            matcher(BLOCK_CLASS, "AbstractSkullBlock"),
                            matcher(ENTITY_CLASS, "EnchantedFallingBlock")
                    ),
                    List.of("summon", "animated_block"),
                    "Animates blocks that pass EnchantedFallingBlock.canFall; skull blocks become animated heads."),
            rule("ars_nouveau:glyph_fangs",
                    List.of(
                            matcher(GENERAL_ENTITY, "true", INFERRED),
                            matcher(ENTITY_CLASS, "LivingEntity", INFERRED)
                    ),
                    List.of("summon", "damage"),
                    "Summons EvokerFangs that deal delayed damage to nearby living entities."),
            rule("ars_nouveau:glyph_lightning",
                    List.of(
                            matcher(GENERAL_ENTITY, "true", INFERRED),
                            matcher(ENTITY_CLASS, "LivingEntity", INFERRED),
                            matcher(BEHAVIOR, "lightning_carrier_damage"),
                            matcher(BEHAVIOR, "apply_shocked_effect")
                    ),
                    List.of("lightning", "damage", "shocked"),
                    "Summons a lightning carrier that strikes nearby entities and applies Shocked."),
            rule("ars_nouveau:glyph_launch",
                    List.of(
                            matcher(GENERAL_ENTITY, "true"),
                            matcher(ENTITY_CLASS, "LivingEntity")
                    ),
                    List.of("launch", "motion"),
                    "Launches LivingEntity targets upward."),
            rule("ars_nouveau:glyph_pull",
                    List.of(
                            matcher(GENERAL_ENTITY, "true"),
                            matcher(ENTITY_CLASS, "LivingEntity")
                    ),
                    List.of("pull", "motion"),
                    "Pulls nearby entities toward the target position.")
    );

    // Combined list: BUILT_IN + any external rules registered via OdysseyGlyphRuleEvent.
    // Written once during FMLCommonSetupEvent; read-only afterwards.
    private static volatile List<GlyphTargetRule> ALL = BUILT_IN;

    private ManualGlyphRules() {
    }

    /**
     * Appends addon glyph rules from {@link com.swvague.ars_odyssey.api.event.OdysseyGlyphRuleEvent}.
     *
     * <p><strong>Single-invocation contract:</strong> FML calls this exactly once during
     * {@code FMLCommonSetupEvent.enqueueWork}. Calling it a second time replaces {@code ALL}
     * again (always rebuilding from {@code BUILT_IN + external}), but that would bypass any
     * rules registered in the first call. Do not call this more than once.
     *
     * <p>Must be called on the server game thread (inside {@code enqueueWork}).
     */
    static void registerExternal(List<GlyphTargetRule> external) {
        if (external == null || external.isEmpty()) {
            return;
        }
        List<GlyphTargetRule> combined = new java.util.ArrayList<>(BUILT_IN);
        combined.addAll(external);
        ALL = List.copyOf(combined);
    }

    static List<GlyphTargetRule> rules() {
        return ALL;
    }

    static Optional<GlyphTargetRule> find(ResourceLocation glyphId) {
        return ALL.stream()
                .filter(rule -> rule.glyphId().equals(glyphId))
                .findFirst();
    }

    private static GlyphTargetRule rule(String glyphId, List<GlyphTargetRule.TargetMatcher> matchers, List<String> categories, String note) {
        return new GlyphTargetRule(ResourceLocation.parse(glyphId), matchers, categories, note);
    }

    private static GlyphTargetRule.TargetMatcher matcher(GlyphTargetRule.MatcherType type, String value) {
        return matcher(type, value, null);
    }

    private static GlyphTargetRule.TargetMatcher matcher(
            GlyphTargetRule.MatcherType type,
            String value,
            GlyphTargetRule.EvidenceLevel evidenceOverride
    ) {
        return new GlyphTargetRule.TargetMatcher(type, value, evidenceOverride);
    }
}
