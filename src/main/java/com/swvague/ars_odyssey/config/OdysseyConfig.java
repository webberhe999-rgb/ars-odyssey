package com.swvague.ars_odyssey.config;

import java.util.List;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class OdysseyConfig {
    public static final ModConfigSpec SPEC;

    public static final ModConfigSpec.BooleanValue ENABLE_TRUTHIFICATION;
    public static final ModConfigSpec.DoubleValue TRUTHIFICATION_REQUIRED_ENTANGLEMENT;
    public static final ModConfigSpec.DoubleValue ORBIT_SELF_TRUTHIFICATION_REQUIRED_ENTANGLEMENT;
    public static final ModConfigSpec.IntValue ORBIT_MAX_DURATION_TICKS;
    public static final ModConfigSpec.DoubleValue ORBIT_RADIUS_BONUS_PER_RING;
    public static final ModConfigSpec.DoubleValue ORBIT_EXTRA_HIT_ENTANGLEMENT_STEP;
    public static final ModConfigSpec.DoubleValue ORBIT_EXTRA_HIT_BASE_ENTANGLEMENT;
    public static final ModConfigSpec.DoubleValue ORBIT_CONSUME_CHANCE_TIER_ONE;
    public static final ModConfigSpec.DoubleValue ORBIT_CONSUME_CHANCE_TIER_TWO;
    public static final ModConfigSpec.DoubleValue ORBIT_CONSUME_CHANCE_TIER_THREE;
    public static final ModConfigSpec.DoubleValue ORBIT_CONSUME_CHANCE_ONE;
    public static final ModConfigSpec.DoubleValue ORBIT_CONSUME_CHANCE_TWO;
    public static final ModConfigSpec.DoubleValue ORBIT_CONSUME_CHANCE_THREE;
    public static final ModConfigSpec.DoubleValue ORBIT_COMPLETE_INSIGHT_CONSUME_CHANCE;
    public static final ModConfigSpec.DoubleValue ORBIT_MANA_DRAIN_THRESHOLD;
    public static final ModConfigSpec.DoubleValue ORBIT_MANA_DRAIN_PERCENT;
    public static final ModConfigSpec.DoubleValue ORBIT_MIN_MANA_DRAIN;
    public static final ModConfigSpec.BooleanValue ENABLE_COMPLEXITY_MAX_CAP;
    public static final ModConfigSpec.DoubleValue COMPLEXITY_MAX_CAP;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> ITEM_VALUE_OVERRIDES;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> ITEM_TAG_VALUE_OVERRIDES;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> MOD_VALUE_DEFAULTS;
    public static final ModConfigSpec.BooleanValue ENABLE_ARCHIVE_SOURCE_PAYMENT;
    public static final ModConfigSpec.IntValue ARCHIVE_SOURCE_RANGE;
    public static final ModConfigSpec.BooleanValue FORCE_LOAD_ARCHIVE_CORE_CHUNK;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();

        builder.push("truthification");
        ENABLE_TRUTHIFICATION = builder
                .comment("Enables player-controlled truthification changes for supported glyphs.")
                .define("enabled", true);
        TRUTHIFICATION_REQUIRED_ENTANGLEMENT = builder
                .comment("Fallback required total truth entanglement for truthifiable glyphs without a dedicated requirement.")
                .defineInRange("default_required_entanglement", 1000.0D, 0.0D, Double.MAX_VALUE);
        ORBIT_SELF_TRUTHIFICATION_REQUIRED_ENTANGLEMENT = builder
                .comment("Required entanglement of the strongest effect glyph modified by Orbit before a player can enable its truthification.")
                .defineInRange("orbit_self_required_entanglement", 1000.0D, 0.0D, Double.MAX_VALUE);
        ORBIT_MAX_DURATION_TICKS = builder
                .comment("Maximum lifetime in ticks for Orbit Self projectiles (6000 = 5 minutes). For truthified projectiles the timer pauses while the owner still has Orbit Self truthified.")
                .defineInRange("orbit_max_duration_ticks", 6000, 1, Integer.MAX_VALUE);
        ORBIT_RADIUS_BONUS_PER_RING = builder
                .comment("Additional actual orbit radius added by each extra Orbit Self ring after the first.")
                .defineInRange("orbit_radius_bonus_per_ring", 1.5D, 0.0D, 16.0D);
        ORBIT_EXTRA_HIT_ENTANGLEMENT_STEP = builder
                .comment("Deprecated legacy linear extra-hit step. Kept for old config compatibility; Orbit Self now uses orbit_extra_hit_base_entanglement.")
                .defineInRange("orbit_extra_hit_entanglement_step", 250.0D, 1.0D, Double.MAX_VALUE);
        ORBIT_EXTRA_HIT_BASE_ENTANGLEMENT = builder
                .comment("Deprecated extra-hit base threshold. Kept for old config compatibility; Orbit Self now uses consume chance tiers.")
                .defineInRange("orbit_extra_hit_base_entanglement", 1000.0D, 1.0D, Double.MAX_VALUE);
        ORBIT_CONSUME_CHANCE_TIER_ONE = builder
                .comment("Modified effect glyph entanglement required for the first Orbit projectile consume-chance tier.")
                .defineInRange("orbit_consume_chance_tier_one", 1000.0D, 0.0D, Double.MAX_VALUE);
        ORBIT_CONSUME_CHANCE_TIER_TWO = builder
                .comment("Modified effect glyph entanglement required for the second Orbit projectile consume-chance tier.")
                .defineInRange("orbit_consume_chance_tier_two", 5000.0D, 0.0D, Double.MAX_VALUE);
        ORBIT_CONSUME_CHANCE_TIER_THREE = builder
                .comment("Modified effect glyph entanglement required for the third Orbit projectile consume-chance tier.")
                .defineInRange("orbit_consume_chance_tier_three", 7000.0D, 0.0D, Double.MAX_VALUE);
        ORBIT_CONSUME_CHANCE_ONE = builder
                .comment("Projectile consume chance at tier one. 0.80 means an 80 percent chance to be consumed on hit.")
                .defineInRange("orbit_consume_chance_one", 0.80D, 0.0D, 1.0D);
        ORBIT_CONSUME_CHANCE_TWO = builder
                .comment("Projectile consume chance at tier two.")
                .defineInRange("orbit_consume_chance_two", 0.60D, 0.0D, 1.0D);
        ORBIT_CONSUME_CHANCE_THREE = builder
                .comment("Projectile consume chance at tier three.")
                .defineInRange("orbit_consume_chance_three", 0.55D, 0.0D, 1.0D);
        ORBIT_COMPLETE_INSIGHT_CONSUME_CHANCE = builder
                .comment("Projectile consume chance when total truth entanglement exceeds 15 times Orbit Self's third truthification condition.")
                .defineInRange("orbit_complete_insight_consume_chance", 0.05D, 0.0D, 1.0D);
        ORBIT_MANA_DRAIN_THRESHOLD = builder
                .comment("Deprecated threshold kept for old config compatibility. Orbit drains fixed mana while truthified; complete insight additionally drains current mana percent.")
                .defineInRange("orbit_mana_drain_threshold", 1000.0D, 0.0D, Double.MAX_VALUE);
        ORBIT_MANA_DRAIN_PERCENT = builder
                .comment("Percent of current mana used by complete-insight truthified Orbit groups. Multiple complete-insight groups stack multiplicatively.")
                .defineInRange("orbit_mana_drain_percent", 0.10D, 0.0D, 1.0D);
        ORBIT_MIN_MANA_DRAIN = builder
                .comment("Flat mana drained every second by truthified Orbit. Complete insight uses at least this much.")
                .defineInRange("orbit_min_mana_drain", 20.0D, 0.0D, Double.MAX_VALUE);
        builder.pop();

        builder.push("complexity");
        ENABLE_COMPLEXITY_MAX_CAP = builder
                .comment("If true, glyph complexity is capped by complexity.max_cap.")
                .define("enable_max_cap", false);
        COMPLEXITY_MAX_CAP = builder
                .comment("Optional maximum complexity cap. Used only when enable_max_cap is true.")
                .defineInRange("max_cap", 6.0D, 1.0D, Double.MAX_VALUE);
        builder.pop();

        builder.push("item_value");
        ITEM_VALUE_OVERRIDES = builder
                .comment(
                        "Exact item value overrides used by Archive, transmutation, storage, and dissonance systems.",
                        "Format: namespace:path=tier.",
                        "Core tiers: 0 common, 1 material, 2 arcane, 3 rare, 4 legendary, 5 forbidden.",
                        "Endgame reserve tiers: 10 mythic, 25 primordial, 77 axiomatic, 99 singularity.",
                        "Higher tier means higher Source/Resonance cost and stricter automation rules.")
                .defineList("item_overrides", List.of(
                        "minecraft:nether_star=4",
                        "minecraft:dragon_egg=5",
                        "minecraft:elytra=4",
                        "minecraft:beacon=4",
                        "minecraft:enchanted_golden_apple=4",
                        "minecraft:command_block=5",
                        "minecraft:chain_command_block=5",
                        "minecraft:repeating_command_block=5",
                        "minecraft:structure_block=5",
                        "minecraft:barrier=5"
                ), OdysseyConfig::isTierMapping);
        ITEM_TAG_VALUE_OVERRIDES = builder
                .comment(
                        "Item tag value overrides. Format: namespace:path=tier or #namespace:path=tier.",
                        "When multiple tags match, the highest tier wins so broad tags cannot cheapen a rare item.")
                .defineList("tag_overrides", List.of(
                        "c:storage_blocks/netherite=5",
                        "c:ingots/netherite=4",
                        "c:gems/diamond=3",
                        "c:gems/emerald=3",
                        "c:gems=2",
                        "c:storage_blocks=2",
                        "c:ingots=1",
                        "c:dusts=1",
                        "ars_nouveau:glyphs=2"
                ), OdysseyConfig::isTierMapping);
        MOD_VALUE_DEFAULTS = builder
                .comment(
                        "Default item value by source mod namespace. Format: namespace=tier.",
                        "Use this for broad compatibility with addon and tech mods before a dedicated compat module exists.")
                .defineList("mod_defaults", List.of(
                        "ars_nouveau=2",
                        "mekanism=2",
                        "ae2=2",
                        "create=1",
                        "draconicevolution=10"
                ), OdysseyConfig::isTierMapping);
        builder.pop();

        builder.push("archive");
        ENABLE_ARCHIVE_SOURCE_PAYMENT = builder
                .comment("If true, Archive operations consume Ars Nouveau Source near the bound Resonance Archive Core.")
                .define("enable_source_payment", true);
        ARCHIVE_SOURCE_RANGE = builder
                .comment("Range around the bound Resonance Archive Core used when searching for Ars Nouveau Source providers.")
                .defineInRange("source_range", 10, 1, 64);
        FORCE_LOAD_ARCHIVE_CORE_CHUNK = builder
                .comment("If true, the bound Resonance Archive Core chunk is force-loaded when the archive awakens or the player logs in.")
                .define("force_load_core_chunk", true);
        builder.pop();

        SPEC = builder.build();
    }

    private static boolean isTierMapping(Object value) {
        if (!(value instanceof String text)) {
            return false;
        }

        int separator = text.indexOf('=');
        if (separator <= 0 || separator == text.length() - 1) {
            return false;
        }

        try {
            int tier = Integer.parseInt(text.substring(separator + 1).trim());
            return tier >= 0 && tier <= 99;
        } catch (NumberFormatException ignored) {
            return false;
        }
    }

    private OdysseyConfig() {
    }
}
