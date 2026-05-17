package com.swvague.ars_odyssey.archive;

import java.util.Optional;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;

public record ArchiveCoreBinding(ResourceLocation dimension, BlockPos pos) {
    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putString("dimension", dimension.toString());
        tag.putLong("pos", pos.asLong());
        return tag;
    }

    public static Optional<ArchiveCoreBinding> load(CompoundTag tag) {
        ResourceLocation dimension = ResourceLocation.tryParse(tag.getString("dimension"));
        if (dimension == null || !tag.contains("pos", Tag.TAG_LONG)) {
            return Optional.empty();
        }
        return Optional.of(new ArchiveCoreBinding(dimension, BlockPos.of(tag.getLong("pos"))));
    }

    public String display() {
        return dimension + " " + pos.getX() + " " + pos.getY() + " " + pos.getZ();
    }
}
