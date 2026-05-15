package com.example.ars_odyssey.knowledge;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CropBlock;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

public final class ResolvedTargetCounter {
    private ResolvedTargetCounter() {
    }

    public static int countResolvedTargets(Collection<GlyphRelation> relations, ResourceLocation glyphId) {
        return collectCoveredTargetKeys(relations, glyphId, null).size();
    }

    public static int countResolvedTargets(Collection<GlyphRelation> relations, ResourceLocation glyphId, Level level) {
        return collectCoveredTargetKeys(relations, glyphId, level).size();
    }

    public static Set<String> collectCoveredTargetKeys(Collection<GlyphRelation> relations, ResourceLocation glyphId) {
        return collectCoveredTargetKeys(relations, glyphId, null);
    }

    public static Set<String> collectCoveredTargetKeys(Collection<GlyphRelation> relations, ResourceLocation glyphId, Level level) {
        Set<String> keys = new LinkedHashSet<>();
        if (relations == null || glyphId == null) {
            return keys;
        }

        for (GlyphRelation relation : relations) {
            if (relation == null || !Objects.equals(glyphId, relation.glyphId())) {
                continue;
            }
            addCoveredTargetKeys(keys, relation.target(), level);
        }
        return keys;
    }

    private static void addCoveredTargetKeys(Set<String> keys, TargetDescriptor target, Level level) {
        if (target == null) {
            return;
        }

        switch (target.kind()) {
            case ENTITY_TYPE -> addEntityKey(keys, target.id());
            case BLOCK -> addBlockKey(keys, target.id());
            case ITEM -> addItemKey(keys, target.id());
            case ENTITY_TYPE_TAG -> addEntityTypeTagKeys(keys, target.id());
            case BLOCK_TAG -> addBlockTagKeys(keys, target.id());
            case ITEM_TAG -> addItemTagKeys(keys, target.id());
            case BEHAVIOR -> addBehaviorKeys(keys, target.detail(), level);
            default -> {
            }
        }
    }

    private static void addEntityTypeTagKeys(Set<String> keys, ResourceLocation tagId) {
        if (tagId == null) {
            return;
        }
        BuiltInRegistries.ENTITY_TYPE.stream()
                .filter(entityType -> entityType.builtInRegistryHolder().tags().map(TagKey::location).anyMatch(tagId::equals))
                .map(BuiltInRegistries.ENTITY_TYPE::getKey)
                .forEach(id -> addEntityKey(keys, id));
    }

    private static void addBlockTagKeys(Set<String> keys, ResourceLocation tagId) {
        if (tagId == null) {
            return;
        }
        BuiltInRegistries.BLOCK.stream()
                .filter(block -> block.builtInRegistryHolder().tags().map(TagKey::location).anyMatch(tagId::equals))
                .map(BuiltInRegistries.BLOCK::getKey)
                .forEach(id -> addBlockKey(keys, id));
    }

    private static void addItemTagKeys(Set<String> keys, ResourceLocation tagId) {
        if (tagId == null) {
            return;
        }
        BuiltInRegistries.ITEM.stream()
                .filter(item -> item.builtInRegistryHolder().tags().map(TagKey::location).anyMatch(tagId::equals))
                .map(BuiltInRegistries.ITEM::getKey)
                .forEach(id -> addItemKey(keys, id));
    }

    private static void addBehaviorKeys(Set<String> keys, String detail, Level level) {
        String normalized = detail == null ? "" : detail.toLowerCase(Locale.ROOT);
        if (normalized.startsWith("entity_class:")) {
            addEntityClassKeys(keys, detail.substring("entity_class:".length()), level);
        } else if (normalized.startsWith("block_class:")) {
            addBlockClassKeys(keys, detail.substring("block_class:".length()));
        }
    }

    private static void addEntityClassKeys(Set<String> keys, String className, Level level) {
        String simpleName = simpleName(className);
        for (EntityType<?> entityType : BuiltInRegistries.ENTITY_TYPE) {
            ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(entityType);
            if (entityTypeMatchesClass(entityType, simpleName, level)) {
                addEntityKey(keys, id);
            }
        }
    }

    private static boolean entityTypeMatchesClass(EntityType<?> entityType, String simpleName, Level level) {
        if (simpleName == null || simpleName.isBlank()) {
            return false;
        }
        if (level == null) {
            return false;
        }

        try {
            Entity entity = entityType.create(level);
            if (entity == null) {
                return false;
            }
            boolean matches = switch (simpleName) {
                case "Entity" -> true;
                case "LivingEntity" -> entity instanceof LivingEntity;
                case "Mob" -> entity instanceof Mob;
                case "Animal" -> entity instanceof Animal;
                default -> entity.getClass().getSimpleName().equals(simpleName);
            };
            entity.discard();
            return matches;
        } catch (Exception e) {
            return false;
        }
    }

    private static void addBlockClassKeys(Set<String> keys, String className) {
        String simpleName = simpleName(className);
        for (Block block : BuiltInRegistries.BLOCK) {
            boolean matches = switch (simpleName) {
                case "CropBlock" -> block instanceof CropBlock;
                default -> block.getClass().getSimpleName().equals(simpleName);
            };
            if (matches) {
                addBlockKey(keys, BuiltInRegistries.BLOCK.getKey(block));
            }
        }
    }

    private static void addEntityKey(Set<String> keys, ResourceLocation id) {
        addKey(keys, "entity", id);
    }

    private static void addBlockKey(Set<String> keys, ResourceLocation id) {
        addKey(keys, "block", id);
    }

    private static void addItemKey(Set<String> keys, ResourceLocation id) {
        addKey(keys, "item", id);
    }

    private static void addKey(Set<String> keys, String prefix, ResourceLocation id) {
        if (id != null) {
            keys.add(prefix + ":" + id);
        }
    }

    private static String simpleName(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        int lastDot = value.lastIndexOf('.');
        return lastDot >= 0 ? value.substring(lastDot + 1) : value;
    }
}
