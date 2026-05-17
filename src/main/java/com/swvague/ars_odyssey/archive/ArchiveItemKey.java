package com.swvague.ars_odyssey.archive;

import java.util.Optional;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

public record ArchiveItemKey(ResourceLocation itemId, String componentKey) {
    public ArchiveItemKey(ResourceLocation itemId) {
        this(itemId, "");
    }

    public ArchiveItemKey {
        componentKey = componentKey == null ? "" : componentKey;
    }

    public static Optional<ArchiveItemKey> from(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return Optional.empty();
        }
        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
        return Optional.of(new ArchiveItemKey(itemId, componentSignature(stack)));
    }

    public String asStorageKey() {
        return componentKey.isBlank() ? itemId.toString() : itemId + "|" + componentKey;
    }

    public boolean hasComponents() {
        return !componentKey.isBlank();
    }

    private static String componentSignature(ItemStack stack) {
        String signature = stack.getComponentsPatch().toString();
        return "{}".equals(signature) ? "" : signature;
    }
}
