package com.swvague.ars_odyssey.index;

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

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

final class TargetQueryParser {
    private static final Logger LOGGER = LogManager.getLogger("Ars Odyssey Glyph Index");

    private TargetQueryParser() {
    }

    static TargetQuery parse(String query) {
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

        Set<ResourceLocation> itemTags = item.map(TargetQueryParser::getItemTags).orElseGet(Collections::emptySet);
        Set<ResourceLocation> blockTags = new LinkedHashSet<>(block.map(TargetQueryParser::getBlockTags).orElseGet(Collections::emptySet));
        if (item.orElse(null) instanceof BlockItem blockItem) {
            blockTags.addAll(getBlockTags(blockItem.getBlock()));
        }
        Set<ResourceLocation> entityTypeTags = entityType.map(TargetQueryParser::getEntityTypeTags).orElseGet(Collections::emptySet);
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
            LOGGER.debug("[Ars Odyssey] Unable to infer entity class because level is null.");
            return;
        }

        try {
            Entity entity = entityType.create(Minecraft.getInstance().level);
            if (entity == null) {
                LOGGER.debug("[Ars Odyssey] Unable to infer entity class because entityType.create returned null for {}.",
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
            LOGGER.debug("[Ars Odyssey] Unable to infer entity class for {}: {}",
                    BuiltInRegistries.ENTITY_TYPE.getKey(entityType),
                    e.toString());
        }
    }

    private static void logTargetQuery(TargetQuery targetQuery) {
        LOGGER.debug("[Ars Odyssey] TargetQuery raw={}", targetQuery.rawQuery());
        LOGGER.debug("[Ars Odyssey]   item={}", targetQuery.item()
                .map(BuiltInRegistries.ITEM::getKey)
                .map(ResourceLocation::toString)
                .orElse("<none>"));
        LOGGER.debug("[Ars Odyssey]   block={}", targetQuery.block()
                .map(BuiltInRegistries.BLOCK::getKey)
                .map(ResourceLocation::toString)
                .orElse("<none>"));
        LOGGER.debug("[Ars Odyssey]   entityType={}", targetQuery.entityType()
                .map(BuiltInRegistries.ENTITY_TYPE::getKey)
                .map(ResourceLocation::toString)
                .orElse("<none>"));
        LOGGER.debug("[Ars Odyssey]   queriedTags={}", targetQuery.queriedTags());
        LOGGER.debug("[Ars Odyssey]   itemTags={}", targetQuery.itemTags());
        LOGGER.debug("[Ars Odyssey]   blockTags={}", targetQuery.blockTags());
        LOGGER.debug("[Ars Odyssey]   entityTypeTags={}", targetQuery.entityTypeTags());
        LOGGER.debug("[Ars Odyssey]   derivedMatchers={}", targetQuery.derivedMatchers());
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
}
