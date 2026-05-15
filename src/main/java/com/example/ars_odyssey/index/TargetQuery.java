package com.example.ars_odyssey.index;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

import java.util.Optional;
import java.util.Set;

public record TargetQuery(
        String rawQuery,
        String normalizedQuery,
        boolean explicitTagQuery,
        Set<ResourceLocation> queriedTags,
        Optional<ResourceLocation> explicitId,
        Optional<Item> item,
        Optional<Block> block,
        Optional<EntityType<?>> entityType,
        Set<ResourceLocation> itemTags,
        Set<ResourceLocation> blockTags,
        Set<ResourceLocation> entityTypeTags,
        Set<String> derivedMatchers
) {
    public TargetQuery {
        rawQuery = rawQuery == null ? "" : rawQuery;
        normalizedQuery = normalizedQuery == null ? "" : normalizedQuery;
        queriedTags = Set.copyOf(queriedTags);
        itemTags = Set.copyOf(itemTags);
        blockTags = Set.copyOf(blockTags);
        entityTypeTags = Set.copyOf(entityTypeTags);
        derivedMatchers = Set.copyOf(derivedMatchers);
    }
}
