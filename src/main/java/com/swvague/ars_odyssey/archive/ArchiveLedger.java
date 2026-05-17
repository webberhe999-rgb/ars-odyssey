package com.swvague.ars_odyssey.archive;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class ArchiveLedger {
    private final Map<ArchiveItemKey, Long> counts = new LinkedHashMap<>();
    private final Map<ArchiveItemKey, ItemStack> prototypes = new LinkedHashMap<>();

    public Map<ArchiveItemKey, Long> counts() {
        return Collections.unmodifiableMap(counts);
    }

    public long count(ArchiveItemKey key) {
        return counts.getOrDefault(key, 0L);
    }

    public ItemStack prototype(ArchiveItemKey key) {
        if (key == null) {
            return ItemStack.EMPTY;
        }
        ItemStack prototype = prototypes.get(key);
        if (prototype != null && !prototype.isEmpty()) {
            return prototype.copy();
        }
        Item item = BuiltInRegistries.ITEM.get(key.itemId());
        if (item == Items.AIR || !BuiltInRegistries.ITEM.getKey(item).equals(key.itemId())) {
            return ItemStack.EMPTY;
        }
        return item.getDefaultInstance();
    }

    public Optional<ArchiveItemKey> findMatching(net.minecraft.world.item.crafting.Ingredient ingredient) {
        return counts.keySet().stream()
                .filter(key -> count(key) > 0)
                .filter(key -> ingredient.test(prototype(key)))
                .findFirst();
    }

    public boolean add(ItemStack stack, long count) {
        Optional<ArchiveItemKey> key = ArchiveItemKey.from(stack);
        if (key.isEmpty() || count <= 0) {
            return false;
        }
        counts.merge(key.get(), count, Math::addExact);
        prototypes.putIfAbsent(key.get(), stack.copyWithCount(1));
        return true;
    }

    public void addPrototype(ArchiveItemKey key, ItemStack stack, long count) {
        if (key == null || stack == null || stack.isEmpty() || count <= 0) {
            return;
        }
        counts.merge(key, count, Math::addExact);
        prototypes.putIfAbsent(key, stack.copyWithCount(1));
    }

    public boolean remove(ArchiveItemKey key, long count) {
        if (key == null || count <= 0) {
            return false;
        }
        long current = count(key);
        if (current < count) {
            return false;
        }
        if (current == count) {
            counts.remove(key);
            prototypes.remove(key);
        } else {
            counts.put(key, current - count);
        }
        return true;
    }

    public ListTag save() {
        return save(null);
    }

    public ListTag save(HolderLookup.Provider provider) {
        ListTag list = new ListTag();
        for (Map.Entry<ArchiveItemKey, Long> entry : counts.entrySet()) {
            CompoundTag tag = new CompoundTag();
            ArchiveItemKey key = entry.getKey();
            tag.putString("item", key.itemId().toString());
            tag.putString("components", key.componentKey());
            tag.putLong("count", entry.getValue());
            ItemStack prototype = prototypes.get(key);
            if (provider != null && prototype != null && !prototype.isEmpty()) {
                tag.put("stack", prototype.copyWithCount(1).save(provider));
            }
            list.add(tag);
        }
        return list;
    }

    public void load(ListTag list) {
        load(list, null);
    }

    public void load(ListTag list, HolderLookup.Provider provider) {
        counts.clear();
        prototypes.clear();
        for (int i = 0; i < list.size(); i++) {
            CompoundTag tag = list.getCompound(i);
            ResourceLocation itemId = ResourceLocation.tryParse(tag.getString("item"));
            long count = tag.contains("count", Tag.TAG_LONG) ? tag.getLong("count") : 0L;
            if (itemId == null || count <= 0) {
                continue;
            }

            ItemStack prototype = loadPrototype(tag, provider);
            if (!prototype.isEmpty()) {
                ArchiveItemKey.from(prototype).ifPresent(key -> addPrototype(key, prototype, count));
            } else {
                ArchiveItemKey key = new ArchiveItemKey(itemId, tag.getString("components"));
                counts.put(key, count);
            }
        }
    }

    private ItemStack loadPrototype(CompoundTag tag, HolderLookup.Provider provider) {
        if (provider == null || !tag.contains("stack", Tag.TAG_COMPOUND)) {
            return ItemStack.EMPTY;
        }
        return ItemStack.parse(provider, tag.getCompound("stack")).orElse(ItemStack.EMPTY);
    }
}
