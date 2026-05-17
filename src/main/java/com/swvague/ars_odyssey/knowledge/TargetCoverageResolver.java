package com.swvague.ars_odyssey.knowledge;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

import java.util.Locale;
import java.util.Objects;

public final class TargetCoverageResolver {
    private TargetCoverageResolver() {
    }

    /**
     * Covers check with no level context — class-hierarchy checks (e.g. LivingEntity) will
     * conservatively return {@code false} when a level is required to instantiate the entity.
     * Prefer {@link #covers(TargetDescriptor, TargetDescriptor, Level)} when a level is available.
     */
    public static boolean covers(TargetDescriptor resolved, TargetDescriptor query) {
        return covers(resolved, query, TargetCoverageContext.empty());
    }

    public static boolean covers(TargetDescriptor resolved, TargetDescriptor query, Level level) {
        return covers(resolved, query, TargetCoverageContext.of(level));
    }

    public static boolean covers(TargetDescriptor resolved, TargetDescriptor query, TargetCoverageContext context) {
        if (resolved == null || query == null) {
            return false;
        }
        if (Objects.equals(resolved, query)) {
            return true;
        }

        if (resolved.kind() == TargetKind.ENTITY_TYPE_TAG && query.kind() == TargetKind.ENTITY_TYPE) {
            return entityTypeHasTag(query.id(), resolved.id());
        }
        if (resolved.kind() == TargetKind.BLOCK_TAG && query.kind() == TargetKind.BLOCK) {
            return blockHasTag(query.id(), resolved.id());
        }
        if (resolved.kind() == TargetKind.ITEM_TAG && query.kind() == TargetKind.ITEM) {
            return itemHasTag(query.id(), resolved.id());
        }

        if (resolved.kind() == TargetKind.BEHAVIOR && query.kind() == TargetKind.ENTITY_TYPE) {
            return entityClassCovers(resolved.detail(), query.id(), context);
        }

        return false;
    }

    private static boolean entityTypeHasTag(ResourceLocation entityTypeId, ResourceLocation tagId) {
        if (entityTypeId == null || tagId == null) {
            return false;
        }
        EntityType<?> entityType = BuiltInRegistries.ENTITY_TYPE.get(entityTypeId);
        return BuiltInRegistries.ENTITY_TYPE.getKey(entityType).equals(entityTypeId)
                && entityType.builtInRegistryHolder().tags().map(TagKey::location).anyMatch(tagId::equals);
    }

    private static boolean blockHasTag(ResourceLocation blockId, ResourceLocation tagId) {
        if (blockId == null || tagId == null) {
            return false;
        }
        Block block = BuiltInRegistries.BLOCK.get(blockId);
        return BuiltInRegistries.BLOCK.getKey(block).equals(blockId)
                && block.builtInRegistryHolder().tags().map(TagKey::location).anyMatch(tagId::equals);
    }

    private static boolean itemHasTag(ResourceLocation itemId, ResourceLocation tagId) {
        if (itemId == null || tagId == null) {
            return false;
        }
        Item item = BuiltInRegistries.ITEM.get(itemId);
        return BuiltInRegistries.ITEM.getKey(item).equals(itemId)
                && item.builtInRegistryHolder().tags().map(TagKey::location).anyMatch(tagId::equals);
    }

    private static boolean entityClassCovers(String resolvedDetail, ResourceLocation entityTypeId, TargetCoverageContext context) {
        if (entityTypeId == null) {
            return false;
        }
        String normalizedDetail = resolvedDetail == null ? "" : resolvedDetail.toLowerCase(Locale.ROOT);
        if (normalizedDetail.equals("entity_class:entity") || normalizedDetail.equals("general_entity")) {
            return BuiltInRegistries.ENTITY_TYPE.getKey(BuiltInRegistries.ENTITY_TYPE.get(entityTypeId)).equals(entityTypeId);
        }
        if (!normalizedDetail.equals("entity_class:livingentity")) {
            return false;
        }

        EntityType<?> entityType = BuiltInRegistries.ENTITY_TYPE.get(entityTypeId);
        Level level = context == null ? null : context.level();
        if (!BuiltInRegistries.ENTITY_TYPE.getKey(entityType).equals(entityTypeId) || level == null) {
            return false;
        }

        try {
            Entity entity = entityType.create(level);
            if (entity == null) {
                return false;
            }
            boolean matches = entity instanceof LivingEntity;
            entity.discard();
            return matches;
        } catch (Exception e) {
            return false;
        }
    }
}
