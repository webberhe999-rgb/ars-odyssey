package com.swvague.ars_odyssey.value;

import net.minecraft.resources.ResourceLocation;

public record ItemValueResult(ItemValueTier tier, ResourceLocation itemId, String reason) {
    public static ItemValueResult of(ItemValueTier tier, ResourceLocation itemId, String reason) {
        return new ItemValueResult(tier, itemId, reason);
    }
}
