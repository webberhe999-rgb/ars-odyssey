package com.swvague.ars_odyssey.knowledge;

import com.swvague.ars_odyssey.index.GlyphTargetRule;
import net.minecraft.resources.ResourceLocation;

import java.util.Optional;

public final class MatcherTargetResolver {
    private MatcherTargetResolver() {
    }

    public static Optional<TargetDescriptor> fromMatcher(GlyphTargetRule.TargetMatcher matcher) {
        if (matcher == null) {
            return Optional.empty();
        }

        String value = matcher.value();
        return Optional.of(switch (matcher.type()) {
            case BLOCK_TAG -> blockTag(value);
            case ITEM_TAG -> itemTag(value);
            case ENTITY_TYPE_TAG, DENY_ENTITY_TYPE_TAG -> entityTypeTag(value);
            case BLOCK_CLASS -> TargetDescriptor.behavior("block_class:" + value);
            case ENTITY_CLASS -> TargetDescriptor.behavior("entity_class:" + value);
            case BEHAVIOR -> TargetDescriptor.behavior(value);
            case POSITION_PREDICATE -> TargetDescriptor.position(value);
            case GENERAL_ENTITY -> TargetDescriptor.behavior("general_entity");
            case GENERAL_BLOCK -> TargetDescriptor.behavior("general_block");
            case INDEXED_TAG -> TargetDescriptor.behavior("indexed_tag:" + value);
            case BLACKLIST -> TargetDescriptor.unknown(value);
        });
    }

    private static TargetDescriptor blockTag(String value) {
        ResourceLocation id = ResourceLocation.tryParse(value);
        return id == null ? TargetDescriptor.unknown(value) : TargetDescriptor.blockTag(id);
    }

    private static TargetDescriptor itemTag(String value) {
        ResourceLocation id = ResourceLocation.tryParse(value);
        return id == null ? TargetDescriptor.unknown(value) : TargetDescriptor.itemTag(id);
    }

    private static TargetDescriptor entityTypeTag(String value) {
        ResourceLocation id = ResourceLocation.tryParse(value);
        return id == null ? TargetDescriptor.unknown(value) : TargetDescriptor.entityTypeTag(id);
    }
}
