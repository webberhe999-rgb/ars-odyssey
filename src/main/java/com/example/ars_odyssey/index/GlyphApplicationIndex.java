package com.example.ars_odyssey.index;

import com.hollingsworth.arsnouveau.api.spell.AbstractSpellPart;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CocoaBlock;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.NetherWartBlock;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static com.example.ars_odyssey.index.GlyphTargetRule.MatcherType.BEHAVIOR;
import static com.example.ars_odyssey.index.GlyphTargetRule.MatcherType.BLACKLIST;
import static com.example.ars_odyssey.index.GlyphTargetRule.MatcherType.BLOCK_CLASS;
import static com.example.ars_odyssey.index.GlyphTargetRule.MatcherType.BLOCK_TAG;
import static com.example.ars_odyssey.index.GlyphTargetRule.MatcherType.DENY_ENTITY_TYPE_TAG;
import static com.example.ars_odyssey.index.GlyphTargetRule.MatcherType.ENTITY_CLASS;
import static com.example.ars_odyssey.index.GlyphTargetRule.MatcherType.ENTITY_TYPE_TAG;
import static com.example.ars_odyssey.index.GlyphTargetRule.MatcherType.GENERAL_BLOCK;
import static com.example.ars_odyssey.index.GlyphTargetRule.MatcherType.GENERAL_ENTITY;
import static com.example.ars_odyssey.index.GlyphTargetRule.MatcherType.ITEM_TAG;
import static com.example.ars_odyssey.index.GlyphTargetRule.MatcherType.POSITION_PREDICATE;
import static com.example.ars_odyssey.index.GlyphTargetRule.EvidenceLevel.INFERRED;

public final class GlyphApplicationIndex {
    private static final Logger LOGGER = LogManager.getLogger("Ars Odyssey Glyph Index");

    private static final List<GlyphTargetRule> RULES = List.of(
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
            // fangs: 召唤 EvokerFangs 造成延迟伤害，目标为实体
            rule("ars_nouveau:glyph_fangs",
                    List.of(
                            matcher(GENERAL_ENTITY, "true", INFERRED),
                            matcher(ENTITY_CLASS, "LivingEntity", INFERRED)
                    ),
                    List.of("summon", "damage"),
                    "Summons EvokerFangs that deal delayed damage to nearby living entities."),
            // launch: 将实体向上弹射
            rule("ars_nouveau:glyph_launch",
                    List.of(
                            matcher(GENERAL_ENTITY, "true"),
                            matcher(ENTITY_CLASS, "LivingEntity")
                    ),
                    List.of("launch", "motion"),
                    "Launches LivingEntity targets upward."),
            // pull: 将实体向目标位置拉扯
            rule("ars_nouveau:glyph_pull",
                    List.of(
                            matcher(GENERAL_ENTITY, "true"),
                            matcher(ENTITY_CLASS, "LivingEntity")
                    ),
                    List.of("pull", "motion"),
                    "Pulls nearby entities toward the target position.")
    );

    private GlyphApplicationIndex() {
    }

    public static List<GlyphTargetRule> getRules() {
        return RULES;
    }

    public static Optional<GlyphTargetRule> getRule(ResourceLocation glyphId) {
        return RULES.stream()
                .filter(rule -> rule.glyphId().equals(glyphId))
                .findFirst();
    }

    public static List<AbstractSpellPart> search(String query, List<AbstractSpellPart> unlockedSpells) {
        return searchWithReasons(query, unlockedSpells).glyphs();
    }

    public static SearchResult searchWithReasons(String query, List<AbstractSpellPart> unlockedSpells) {
        LOGGER.info("[Ars Odyssey] Search rawQuery=\"{}\" unlockedSpells={}", query == null ? "" : query, unlockedSpells.size());
        TargetQuery targetQuery = parseTargetQuery(query);
        SearchIntent searchIntent = SearchIntentResolver.resolve(query);
        if (targetQuery.normalizedQuery().isEmpty()) {
            LOGGER.info("[Ars Odyssey] Search matched glyph ids={}", unlockedSpells.stream()
                    .map(AbstractSpellPart::getRegistryName)
                    .toList());
            return new SearchResult(new ArrayList<>(unlockedSpells), Map.of());
        }

        Set<ResourceLocation> matchingRuleGlyphIds = new HashSet<>();
        Set<ResourceLocation> blacklistMatchedRuleGlyphIds = new HashSet<>();
        Map<ResourceLocation, List<MatchReason>> reasonsByRuleGlyphId = new LinkedHashMap<>();
        AutoGlyphRuleIndex.AutoRuleSet autoRuleSet = AutoGlyphRuleIndex.getRuleSet(RULES);
        for (GlyphTargetRule rule : RULES) {
            RuleMatch match = matchesRule(rule, targetQuery, MatchReason.RuleSource.MANUAL);
            if (match.positiveMatched()) {
                matchingRuleGlyphIds.add(rule.glyphId());
                reasonsByRuleGlyphId.computeIfAbsent(rule.glyphId(), ignored -> new ArrayList<>()).addAll(match.reasons());
            }
            if (match.blacklistMatched()) {
                blacklistMatchedRuleGlyphIds.add(rule.glyphId());
                if (match.positiveMatched()) {
                    reasonsByRuleGlyphId.computeIfAbsent(rule.glyphId(), ignored -> new ArrayList<>()).addAll(match.blacklistReasons());
                }
            }
        }
        for (GlyphTargetRule rule : autoRuleSet.rules()) {
            RuleMatch match = matchesRule(rule, targetQuery, MatchReason.RuleSource.AUTO);
            if (match.positiveMatched()) {
                matchingRuleGlyphIds.add(rule.glyphId());
                reasonsByRuleGlyphId.computeIfAbsent(rule.glyphId(), ignored -> new ArrayList<>()).addAll(match.reasons());
                List<MatchReason> matchingLimitations = matchingLimitations(rule.glyphId(), autoRuleSet.limitationsByGlyph().getOrDefault(rule.glyphId(), List.of()), targetQuery);
                if (!matchingLimitations.isEmpty()) {
                    blacklistMatchedRuleGlyphIds.add(rule.glyphId());
                    reasonsByRuleGlyphId.computeIfAbsent(rule.glyphId(), ignored -> new ArrayList<>()).addAll(matchingLimitations);
                }
            }
            if (match.blacklistMatched()) {
                blacklistMatchedRuleGlyphIds.add(rule.glyphId());
                if (match.positiveMatched()) {
                    reasonsByRuleGlyphId.computeIfAbsent(rule.glyphId(), ignored -> new ArrayList<>()).addAll(match.blacklistReasons());
                }
            }
        }
        LOGGER.info("[Ars Odyssey] Search positive rule glyph ids={}", matchingRuleGlyphIds);
        LOGGER.info("[Ars Odyssey] Search blacklist matched rule glyph ids={}", blacklistMatchedRuleGlyphIds);

        List<AbstractSpellPart> results = new ArrayList<>();
        Map<ResourceLocation, List<MatchReason>> reasonsByGlyph = new LinkedHashMap<>();
        for (AbstractSpellPart spellPart : unlockedSpells) {
            ResourceLocation glyphId = spellPart.getRegistryName();
            if (searchIntent.type() == SearchIntentType.GLYPH && searchIntent.resolvedId() != null) {
                if (searchIntent.resolvedId().equals(glyphId)) {
                    results.add(spellPart);
                    List<MatchReason> reasons = reasonsByRuleGlyphId.get(glyphId);
                    if (reasons != null && !reasons.isEmpty()) {
                        reasonsByGlyph.put(glyphId, List.copyOf(reasons));
                    } else {
                        bestReasonForGlyph(glyphId).ifPresent(reason -> reasonsByGlyph.put(glyphId, List.of(reason)));
                    }
                }
                continue;
            }
            if (matchesGlyphNameOrId(spellPart, glyphId, targetQuery.normalizedQuery()) || matchingRuleGlyphIds.contains(glyphId)) {
                results.add(spellPart);
                if (reasonsByRuleGlyphId.containsKey(glyphId)) {
                    reasonsByGlyph.put(glyphId, List.copyOf(reasonsByRuleGlyphId.get(glyphId)));
                }
            }
        }
        LOGGER.info("[Ars Odyssey] Search matched glyph ids={}", results.stream()
                .map(AbstractSpellPart::getRegistryName)
                .toList());
        return new SearchResult(results, reasonsByGlyph);
    }

    public static Optional<MatchReason> bestReasonForGlyph(ResourceLocation glyphId) {
        if (glyphId == null) {
            return Optional.empty();
        }

        List<MatchReason> reasons = new ArrayList<>();
        for (GlyphTargetRule rule : RULES) {
            if (rule.glyphId().equals(glyphId)) {
                addPositiveSummaryReasons(reasons, rule, MatchReason.RuleSource.MANUAL);
            }
        }

        AutoGlyphRuleIndex.AutoRuleSet autoRuleSet = AutoGlyphRuleIndex.getRuleSet(RULES);
        for (GlyphTargetRule rule : autoRuleSet.rules()) {
            if (rule.glyphId().equals(glyphId)) {
                addPositiveSummaryReasons(reasons, rule, MatchReason.RuleSource.AUTO);
            }
        }

        return MatchReasonDisplayReducer.reduce(reasons).stream()
                .filter(reason -> !reason.blacklist())
                .filter(reason -> MatcherPresentation.roleOf(reason.matcher()) != GlyphTargetRule.TooltipRole.LIMIT)
                .findFirst();
    }

    public record SearchResult(List<AbstractSpellPart> glyphs, Map<ResourceLocation, List<MatchReason>> reasonsByGlyph) {
        public SearchResult {
            glyphs = List.copyOf(glyphs);
            reasonsByGlyph = Map.copyOf(reasonsByGlyph);
        }
    }

    public static TargetQuery parseTargetQuery(String query) {
        String rawQuery = query == null ? "" : query;
        String normalizedQuery = rawQuery.toLowerCase(Locale.ROOT).trim();
        boolean explicitTagQuery = normalizedQuery.startsWith("#");
        String idText = explicitTagQuery ? normalizedQuery.substring(1) : normalizedQuery;

        Optional<ResourceLocation> explicitId = parseResourceLocation(idText);
        Optional<ResourceLocation> defaultedId = idText.isBlank()
                ? Optional.empty()
                : explicitId.isPresent()
                ? explicitId
                : parseResourceLocation("minecraft:" + idText);

        Optional<Item> item = Optional.empty();
        Optional<Block> block = Optional.empty();
        Optional<EntityType<?>> entityType = Optional.empty();

        if (!explicitTagQuery && defaultedId.isPresent()) {
            ResourceLocation id = defaultedId.get();
            item = getItem(id);
            block = getBlock(id);
            entityType = getEntityType(id);
        }
        if (!explicitTagQuery) {
            item = item.or(() -> findItemByDisplayName(normalizedQuery));
            block = block.or(() -> findBlockByDisplayName(normalizedQuery));
            entityType = entityType.or(() -> findEntityTypeByDisplayName(normalizedQuery));
        }

        Set<ResourceLocation> queriedTags = new LinkedHashSet<>();
        if (explicitTagQuery && defaultedId.isPresent()) {
            queriedTags.add(defaultedId.get());
        } else if (defaultedId.isPresent() && item.isEmpty() && block.isEmpty() && entityType.isEmpty()) {
            queriedTags.add(defaultedId.get());
        }

        Set<ResourceLocation> itemTags = item.map(GlyphApplicationIndex::getItemTags).orElseGet(Collections::emptySet);
        Set<ResourceLocation> blockTags = new LinkedHashSet<>(block.map(GlyphApplicationIndex::getBlockTags).orElseGet(Collections::emptySet));
        if (item.orElse(null) instanceof BlockItem blockItem) {
            blockTags.addAll(getBlockTags(blockItem.getBlock()));
        }
        Set<ResourceLocation> entityTypeTags = entityType.map(GlyphApplicationIndex::getEntityTypeTags).orElseGet(Collections::emptySet);
        Set<String> derivedMatchers = new LinkedHashSet<>();
        block.ifPresent(resolvedBlock -> addBlockDerivedMatchers(resolvedBlock, derivedMatchers));
        if (item.orElse(null) instanceof BlockItem blockItem) {
            derivedMatchers.add("item_is_block_item");
            addBlockDerivedMatchers(blockItem.getBlock(), derivedMatchers);
        }
        entityType.ifPresent(resolvedEntityType -> addEntityTypeDerivedMatchers(resolvedEntityType, derivedMatchers));

        TargetQuery targetQuery = new TargetQuery(
                rawQuery,
                normalizedQuery,
                explicitTagQuery,
                queriedTags,
                defaultedId,
                item,
                block,
                entityType,
                itemTags,
                blockTags,
                entityTypeTags,
                derivedMatchers
        );
        logTargetQuery(targetQuery);
        return targetQuery;
    }

    public static void printRules() {
        LOGGER.info("[Ars Odyssey] Glyph target rules: {}", RULES.size());
        for (GlyphTargetRule rule : RULES) {
            LOGGER.info("[Ars Odyssey] TargetRule id={} categories={} note={}",
                    rule.glyphId(),
                    rule.categories(),
                    rule.note());
            for (GlyphTargetRule.TargetMatcher matcher : rule.matchers()) {
                LOGGER.info("[Ars Odyssey]   matcher type={} value={} role={} evidence={} override={}",
                        matcher.type(),
                        matcher.value(),
                        MatcherPresentation.roleOf(matcher),
                        MatcherPresentation.evidenceOf(matcher),
                        matcher.evidenceOverride());
            }
        }
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

    private static boolean matchesGlyphNameOrId(AbstractSpellPart spellPart, ResourceLocation glyphId, String query) {
        String localeName = spellPart.getLocaleName();
        if (localeName != null && containsNormalized(localeName, query)) {
            return true;
        }

        if (glyphId == null) {
            return false;
        }

        String fullId = glyphId.toString().toLowerCase(Locale.ROOT);
        String path = glyphId.getPath().toLowerCase(Locale.ROOT);
        return fullId.contains(query) || path.contains(query);
    }

    private static RuleMatch matchesRule(GlyphTargetRule rule, TargetQuery targetQuery, MatchReason.RuleSource source) {
        String query = targetQuery.normalizedQuery();
        boolean positiveMatched = false;
        boolean blacklistMatched = false;
        List<MatchReason> reasons = new ArrayList<>();
        List<MatchReason> blacklistReasons = new ArrayList<>();
        if (containsNormalized(rule.glyphId().toString(), query) || containsNormalized(rule.note(), query)) {
            positiveMatched = true;
        }

        for (String category : rule.categories()) {
            if (containsNormalized(category, query)) {
                positiveMatched = true;
            }
        }

        for (GlyphTargetRule.TargetMatcher matcher : rule.matchers()) {
            ResourceLocation matcherId = ResourceLocation.tryParse(matcher.value().toLowerCase(Locale.ROOT));
            if (matcher.type() == GENERAL_ENTITY && isEntityQuery(targetQuery)) {
                positiveMatched = true;
                addReason(reasons, rule, matcher, false, source);
            }
            if (matcher.type() == GENERAL_BLOCK && isBlockQuery(targetQuery)) {
                positiveMatched = true;
                addReason(reasons, rule, matcher, false, source);
            }
            if (matcher.type() == ITEM_TAG && tagMatches(matcherId, targetQuery.itemTags(), targetQuery.queriedTags())) {
                positiveMatched = true;
                addReason(reasons, rule, matcher, false, source);
            }
            if (matcher.type() == BLOCK_TAG && tagMatches(matcherId, targetQuery.blockTags(), targetQuery.queriedTags())) {
                positiveMatched = true;
                addReason(reasons, rule, matcher, false, source);
            }
            if (matcher.type() == ENTITY_TYPE_TAG && tagMatches(matcherId, targetQuery.entityTypeTags(), targetQuery.queriedTags())) {
                positiveMatched = true;
                addReason(reasons, rule, matcher, false, source);
            }
            if (matcher.type() == GlyphTargetRule.MatcherType.INDEXED_TAG && indexedTagMatches(matcherId, targetQuery)) {
                positiveMatched = true;
                addReason(reasons, rule, matcher, false, source);
            }
            if (matcher.type() == DENY_ENTITY_TYPE_TAG && tagMatches(matcherId, targetQuery.entityTypeTags(), targetQuery.queriedTags())) {
                blacklistMatched = true;
                addReason(blacklistReasons, rule, matcher, true, source);
            }
            if (matcher.type() == ENTITY_CLASS && derivedMatcherMatches(targetQuery, "entity_class", matcher.value())) {
                positiveMatched = true;
                addReason(reasons, rule, matcher, false, source);
            }
            if (matcher.type() == BLOCK_CLASS && derivedMatcherMatches(targetQuery, "block_class", matcher.value())) {
                positiveMatched = true;
                addReason(reasons, rule, matcher, false, source);
            }
            if (matcher.type() == BLACKLIST && blacklistMatches(matcher.value(), targetQuery)) {
                blacklistMatched = true;
                addReason(blacklistReasons, rule, matcher, true, source);
            }

            if (matchesStringFallback(matcher.type()) && containsNormalized(matcher.value(), query)) {
                positiveMatched = true;
                addReason(reasons, rule, matcher, false, source);
            }
            if (matchesBlacklistString(matcher.type()) && containsNormalized(matcher.value(), query)) {
                blacklistMatched = true;
                addReason(blacklistReasons, rule, matcher, true, source);
            }
        }

        if (blacklistMatched && reasons.isEmpty()) {
            positiveMatched = false;
        }

        if (positiveMatched || blacklistMatched) {
            LOGGER.info("[Ars Odyssey] {} rule matched glyphId={} positiveMatched={} blacklistMatched={}",
                    source == MatchReason.RuleSource.AUTO ? "auto" : "manual",
                    rule.glyphId(),
                    positiveMatched,
                    blacklistMatched);
        }
        return new RuleMatch(positiveMatched, blacklistMatched, reasons, blacklistReasons);
    }

    private record RuleMatch(
            boolean positiveMatched,
            boolean blacklistMatched,
            List<MatchReason> reasons,
            List<MatchReason> blacklistReasons
    ) {
        private RuleMatch {
            reasons = List.copyOf(reasons);
            blacklistReasons = List.copyOf(blacklistReasons);
        }
    }

    private static List<MatchReason> matchingLimitations(
            ResourceLocation glyphId,
            List<GlyphTargetRule.TargetMatcher> limitations,
            TargetQuery targetQuery
    ) {
        List<MatchReason> reasons = new ArrayList<>();
        for (GlyphTargetRule.TargetMatcher matcher : limitations) {
            ResourceLocation matcherId = ResourceLocation.tryParse(matcher.value().toLowerCase(Locale.ROOT));
            if (matcher.type() == DENY_ENTITY_TYPE_TAG && tagMatches(matcherId, targetQuery.entityTypeTags(), targetQuery.queriedTags())) {
                addReason(reasons, glyphId, matcher, true, MatchReason.RuleSource.AUTO);
            } else if (matcher.type() == BLACKLIST && blacklistMatches(matcher.value(), targetQuery)) {
                addReason(reasons, glyphId, matcher, true, MatchReason.RuleSource.AUTO);
            }
        }
        return reasons;
    }

    private static void addReason(
            List<MatchReason> reasons,
            GlyphTargetRule rule,
            GlyphTargetRule.TargetMatcher matcher,
            boolean blacklist,
            MatchReason.RuleSource source
    ) {
        addReason(reasons, rule.glyphId(), matcher, blacklist, source);
    }

    private static void addReason(
            List<MatchReason> reasons,
            ResourceLocation glyphId,
            GlyphTargetRule.TargetMatcher matcher,
            boolean blacklist,
            MatchReason.RuleSource source
    ) {
        MatchReason reason = new MatchReason(glyphId, matcher, blacklist, MatcherPresentation.translationKeyOf(matcher), source);
        if (!reasons.contains(reason)) {
            reasons.add(reason);
            LOGGER.info("[Ars Odyssey] matched {} by {} rule {}={} blacklist={}",
                    glyphId,
                    source == MatchReason.RuleSource.AUTO ? "auto" : "manual",
                    matcher.type(),
                    matcher.value(),
                    blacklist);
        }
    }

    private static void addPositiveSummaryReasons(
            List<MatchReason> reasons,
            GlyphTargetRule rule,
            MatchReason.RuleSource source
    ) {
        for (GlyphTargetRule.TargetMatcher matcher : rule.matchers()) {
            if (MatcherPresentation.roleOf(matcher) != GlyphTargetRule.TooltipRole.LIMIT) {
                reasons.add(new MatchReason(rule.glyphId(), matcher, false, MatcherPresentation.translationKeyOf(matcher), source));
            }
        }
    }

    private static boolean isEntityQuery(TargetQuery targetQuery) {
        return targetQuery.entityType().isPresent() || targetQuery.derivedMatchers().contains("entity_class:Entity");
    }

    private static boolean isBlockQuery(TargetQuery targetQuery) {
        return targetQuery.block().isPresent() || targetQuery.item().orElse(null) instanceof BlockItem;
    }

    private static boolean blacklistMatches(String value, TargetQuery targetQuery) {
        String normalizedValue = value.toLowerCase(Locale.ROOT);
        if (normalizedValue.startsWith("entity_class:")) {
            return derivedMatcherMatches(targetQuery, "entity_class", value.substring("entity_class:".length()));
        }
        if (normalizedValue.startsWith("block_class:")) {
            return derivedMatcherMatches(targetQuery, "block_class", value.substring("block_class:".length()));
        }
        if (normalizedValue.startsWith("block_tag:")) {
            ResourceLocation id = ResourceLocation.tryParse(value.substring("block_tag:".length()).toLowerCase(Locale.ROOT));
            return tagMatches(id, targetQuery.blockTags(), targetQuery.queriedTags());
        }
        if (normalizedValue.startsWith("entity_type_tag:")) {
            ResourceLocation id = ResourceLocation.tryParse(value.substring("entity_type_tag:".length()).toLowerCase(Locale.ROOT));
            return tagMatches(id, targetQuery.entityTypeTags(), targetQuery.queriedTags());
        }
        return containsNormalized(value, targetQuery.normalizedQuery());
    }

    private static boolean containsNormalized(String value, String query) {
        if (value == null) {
            return false;
        }
        String normalizedValue = normalizeLoose(value);
        String normalizedQuery = normalizeLoose(query);
        return normalizedValue.contains(normalizedQuery);
    }

    private static String normalizeLoose(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT).replace("_", "");
    }

    private static boolean matchesStringFallback(GlyphTargetRule.MatcherType matcherType) {
        return matcherType == BEHAVIOR
                || matcherType == BLOCK_CLASS
                || matcherType == ENTITY_CLASS
                || matcherType == POSITION_PREDICATE;
    }

    private static boolean matchesBlacklistString(GlyphTargetRule.MatcherType matcherType) {
        return matcherType == BLACKLIST || matcherType == DENY_ENTITY_TYPE_TAG;
    }

    private static boolean tagMatches(ResourceLocation matcherId, Set<ResourceLocation> resolvedTags, Set<ResourceLocation> queriedTags) {
        return matcherId != null && (resolvedTags.contains(matcherId) || queriedTags.contains(matcherId));
    }

    private static boolean indexedTagMatches(ResourceLocation matcherId, TargetQuery targetQuery) {
        return matcherId != null && (targetQuery.queriedTags().contains(matcherId)
                || targetQuery.itemTags().contains(matcherId)
                || targetQuery.blockTags().contains(matcherId)
                || targetQuery.entityTypeTags().contains(matcherId));
    }

    private static boolean derivedMatcherMatches(TargetQuery targetQuery, String prefix, String matcherValue) {
        String simpleMatcherValue = simpleClassName(matcherValue);
        return targetQuery.derivedMatchers().contains(prefix + ":" + simpleMatcherValue);
    }

    private static String simpleClassName(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        int lastDot = value.lastIndexOf('.');
        return lastDot >= 0 ? value.substring(lastDot + 1) : value;
    }

    private static Optional<ResourceLocation> parseResourceLocation(String value) {
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(ResourceLocation.tryParse(value));
    }

    private static Optional<Item> getItem(ResourceLocation id) {
        Item item = BuiltInRegistries.ITEM.get(id);
        return BuiltInRegistries.ITEM.getKey(item).equals(id) ? Optional.of(item) : Optional.empty();
    }

    private static Optional<Block> getBlock(ResourceLocation id) {
        Block block = BuiltInRegistries.BLOCK.get(id);
        return BuiltInRegistries.BLOCK.getKey(block).equals(id) ? Optional.of(block) : Optional.empty();
    }

    private static Optional<EntityType<?>> getEntityType(ResourceLocation id) {
        EntityType<?> entityType = BuiltInRegistries.ENTITY_TYPE.get(id);
        return BuiltInRegistries.ENTITY_TYPE.getKey(entityType).equals(id) ? Optional.of(entityType) : Optional.empty();
    }

    private static Optional<Item> findItemByDisplayName(String query) {
        return BuiltInRegistries.ITEM.stream()
                .filter(item -> containsNormalized(item.getDescription().getString(), query))
                .findFirst();
    }

    private static Optional<Block> findBlockByDisplayName(String query) {
        return BuiltInRegistries.BLOCK.stream()
                .filter(block -> containsNormalized(block.getName().getString(), query))
                .findFirst();
    }

    private static Optional<EntityType<?>> findEntityTypeByDisplayName(String query) {
        return BuiltInRegistries.ENTITY_TYPE.stream()
                .filter(entityType -> containsNormalized(entityType.getDescription().getString(), query))
                .findFirst();
    }

    private static Set<ResourceLocation> getItemTags(Item item) {
        return item.builtInRegistryHolder().tags()
                .map(TagKey::location)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
    }

    private static Set<ResourceLocation> getBlockTags(Block block) {
        return block.builtInRegistryHolder().tags()
                .map(TagKey::location)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
    }

    private static Set<ResourceLocation> getEntityTypeTags(EntityType<?> entityType) {
        return entityType.builtInRegistryHolder().tags()
                .map(TagKey::location)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
    }

    private static void addBlockDerivedMatchers(Block block, Set<String> derivedMatchers) {
        derivedMatchers.add("block_class:" + block.getClass().getSimpleName());
        if (block instanceof CropBlock) {
            derivedMatchers.add("block_class:CropBlock");
        }
        if (block instanceof NetherWartBlock) {
            derivedMatchers.add("block_class:NetherWartBlock");
        }
        if (block instanceof CocoaBlock) {
            derivedMatchers.add("block_class:CocoaBlock");
        }
    }

    private static void addEntityTypeDerivedMatchers(EntityType<?> entityType, Set<String> derivedMatchers) {
        if (Minecraft.getInstance().level == null) {
            LOGGER.info("[Ars Odyssey] Unable to infer entity class because level is null.");
            return;
        }

        try {
            Entity entity = entityType.create(Minecraft.getInstance().level);
            if (entity == null) {
                LOGGER.info("[Ars Odyssey] Unable to infer entity class because entityType.create returned null for {}.",
                        BuiltInRegistries.ENTITY_TYPE.getKey(entityType));
                return;
            }

            derivedMatchers.add("entity_class:" + entity.getClass().getSimpleName());
            if (entity instanceof LivingEntity) {
                derivedMatchers.add("entity_class:LivingEntity");
            }
            derivedMatchers.add("entity_class:Entity");
            entity.discard();
        } catch (Exception e) {
            LOGGER.info("[Ars Odyssey] Unable to infer entity class for {}: {}",
                    BuiltInRegistries.ENTITY_TYPE.getKey(entityType),
                    e.toString());
        }
    }

    private static void logTargetQuery(TargetQuery targetQuery) {
        LOGGER.info("[Ars Odyssey] TargetQuery raw={}", targetQuery.rawQuery());
        LOGGER.info("[Ars Odyssey]   item={}", targetQuery.item()
                .map(BuiltInRegistries.ITEM::getKey)
                .map(ResourceLocation::toString)
                .orElse("<none>"));
        LOGGER.info("[Ars Odyssey]   block={}", targetQuery.block()
                .map(BuiltInRegistries.BLOCK::getKey)
                .map(ResourceLocation::toString)
                .orElse("<none>"));
        LOGGER.info("[Ars Odyssey]   entityType={}", targetQuery.entityType()
                .map(BuiltInRegistries.ENTITY_TYPE::getKey)
                .map(ResourceLocation::toString)
                .orElse("<none>"));
        LOGGER.info("[Ars Odyssey]   queriedTags={}", targetQuery.queriedTags());
        LOGGER.info("[Ars Odyssey]   itemTags={}", targetQuery.itemTags());
        LOGGER.info("[Ars Odyssey]   blockTags={}", targetQuery.blockTags());
        LOGGER.info("[Ars Odyssey]   entityTypeTags={}", targetQuery.entityTypeTags());
        LOGGER.info("[Ars Odyssey]   derivedMatchers={}", targetQuery.derivedMatchers());
    }
}
