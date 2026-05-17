package com.swvague.ars_odyssey.knowledge;

import net.minecraft.resources.ResourceLocation;

public record TargetDescriptor(
        TargetKind kind,
        ResourceLocation id,
        String detail
) {
    public TargetDescriptor {
        kind = kind == null ? TargetKind.UNKNOWN : kind;
        detail = detail == null ? "" : detail;
    }

    public static TargetDescriptor block(ResourceLocation id) {
        return new TargetDescriptor(TargetKind.BLOCK, id, "");
    }

    public static TargetDescriptor item(ResourceLocation id) {
        return new TargetDescriptor(TargetKind.ITEM, id, "");
    }

    public static TargetDescriptor entityType(ResourceLocation id) {
        return new TargetDescriptor(TargetKind.ENTITY_TYPE, id, "");
    }

    public static TargetDescriptor blockTag(ResourceLocation id) {
        return new TargetDescriptor(TargetKind.BLOCK_TAG, id, "");
    }

    public static TargetDescriptor itemTag(ResourceLocation id) {
        return new TargetDescriptor(TargetKind.ITEM_TAG, id, "");
    }

    public static TargetDescriptor entityTypeTag(ResourceLocation id) {
        return new TargetDescriptor(TargetKind.ENTITY_TYPE_TAG, id, "");
    }

    public static TargetDescriptor behavior(String detail) {
        return new TargetDescriptor(TargetKind.BEHAVIOR, null, detail);
    }

    public static TargetDescriptor position(String detail) {
        return new TargetDescriptor(TargetKind.POSITION, null, detail);
    }

    public static TargetDescriptor unknown(String detail) {
        return new TargetDescriptor(TargetKind.UNKNOWN, null, detail);
    }
}
