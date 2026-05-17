package com.swvague.ars_odyssey.value;

import java.util.List;
import java.util.Optional;

import com.swvague.ars_odyssey.config.OdysseyConfig;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;

public final class ItemValueResolver {
    private ItemValueResolver() {
    }

    public static ItemValueTier tierOf(ItemStack stack) {
        return resolve(stack).tier();
    }

    public static ItemValueTier tierOf(Item item) {
        return resolve(item).tier();
    }

    public static ItemValueResult resolve(Item item) {
        return resolve(item.getDefaultInstance());
    }

    public static ItemValueResult resolve(ItemStack stack) {
        if (stack.isEmpty()) {
            return ItemValueResult.of(ItemValueTier.COMMON, ResourceLocation.parse("minecraft:air"), "empty_stack");
        }

        Item item = stack.getItem();
        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(item);

        Optional<ItemValueTier> exactOverride = exactItemOverride(itemId);
        if (exactOverride.isPresent()) {
            return ItemValueResult.of(exactOverride.get(), itemId, "config:item_overrides");
        }

        TagMatch tagOverride = highestConfiguredTagMatch(item);
        ItemValueTier tier = tagOverride.tier;
        String reason = tagOverride.reason;

        Optional<ItemValueTier> modDefault = namespaceDefault(itemId.getNamespace());
        if (modDefault.isPresent() && modDefault.get().level() > tier.level()) {
            tier = modDefault.get();
            reason = "config:mod_defaults";
        }

        Rarity rarity = stack.getRarity();
        ItemValueTier rarityTier = tierFromRarity(rarity);
        if (rarityTier.level() > tier.level()) {
            tier = rarityTier;
            reason = "item_rarity:" + rarity.name().toLowerCase();
        }

        return ItemValueResult.of(tier, itemId, reason);
    }

    private static Optional<ItemValueTier> exactItemOverride(ResourceLocation itemId) {
        return parseMappingList(OdysseyConfig.ITEM_VALUE_OVERRIDES.get(), itemId.toString());
    }

    private static Optional<ItemValueTier> namespaceDefault(String namespace) {
        return parseMappingList(OdysseyConfig.MOD_VALUE_DEFAULTS.get(), namespace);
    }

    private static TagMatch highestConfiguredTagMatch(Item item) {
        TagMatch match = new TagMatch(ItemValueTier.COMMON, "default");

        for (String mapping : OdysseyConfig.ITEM_TAG_VALUE_OVERRIDES.get()) {
            ParsedMapping parsed = parseMapping(mapping);
            if (parsed == null) {
                continue;
            }

            ResourceLocation tagId = parseResourceLocation(parsed.key.startsWith("#") ? parsed.key.substring(1) : parsed.key);
            if (tagId == null || !hasTag(item, tagId)) {
                continue;
            }

            if (parsed.tier.level() > match.tier.level()) {
                match = new TagMatch(parsed.tier, "config:tag_overrides:" + tagId);
            }
        }

        return match;
    }

    private static Optional<ItemValueTier> parseMappingList(List<? extends String> mappings, String key) {
        for (String mapping : mappings) {
            ParsedMapping parsed = parseMapping(mapping);
            if (parsed != null && parsed.key.equals(key)) {
                return Optional.of(parsed.tier);
            }
        }

        return Optional.empty();
    }

    private static ParsedMapping parseMapping(String mapping) {
        int separator = mapping.indexOf('=');
        if (separator <= 0 || separator == mapping.length() - 1) {
            return null;
        }

        String key = mapping.substring(0, separator).trim();
        String tierText = mapping.substring(separator + 1).trim();
        try {
            return new ParsedMapping(key, ItemValueTier.fromLevel(Integer.parseInt(tierText)));
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private static boolean hasTag(Item item, ResourceLocation tagId) {
        return item.builtInRegistryHolder().tags().map(TagKey::location).anyMatch(tagId::equals);
    }

    private static ResourceLocation parseResourceLocation(String text) {
        try {
            return ResourceLocation.parse(text);
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private static ItemValueTier tierFromRarity(Rarity rarity) {
        return switch (rarity) {
            case COMMON -> ItemValueTier.COMMON;
            case UNCOMMON -> ItemValueTier.MATERIAL;
            case RARE -> ItemValueTier.RARE;
            case EPIC -> ItemValueTier.LEGENDARY;
        };
    }

    private record ParsedMapping(String key, ItemValueTier tier) {
    }

    private record TagMatch(ItemValueTier tier, String reason) {
    }
}
