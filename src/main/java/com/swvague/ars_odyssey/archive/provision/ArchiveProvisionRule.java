package com.swvague.ars_odyssey.archive.provision;

import java.util.Optional;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;

public record ArchiveProvisionRule(ResourceLocation itemId, int targetCount, boolean enabled) {
    public ArchiveProvisionRule {
        targetCount = Math.max(1, targetCount);
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putString("item", itemId.toString());
        tag.putInt("targetCount", targetCount);
        tag.putBoolean("enabled", enabled);
        return tag;
    }

    public static Optional<ArchiveProvisionRule> load(CompoundTag tag) {
        ResourceLocation itemId = ResourceLocation.tryParse(tag.getString("item"));
        if (itemId == null) {
            return Optional.empty();
        }
        int targetCount = tag.contains("targetCount", Tag.TAG_INT) ? tag.getInt("targetCount") : 1;
        boolean enabled = !tag.contains("enabled", Tag.TAG_BYTE) || tag.getBoolean("enabled");
        return Optional.of(new ArchiveProvisionRule(itemId, targetCount, enabled));
    }
}
