package com.swvague.ars_odyssey.archive.provision;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import com.swvague.ars_odyssey.archive.runtime.ArchiveDirtyReason;
import com.swvague.ars_odyssey.archive.runtime.ArchiveRuntimeState;

import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceLocation;

public class ArchiveProvisionRules {
    private final Map<ResourceLocation, ArchiveProvisionRule> rules = new LinkedHashMap<>();
    private final ArchiveRuntimeState runtimeState;

    public ArchiveProvisionRules(ArchiveRuntimeState runtimeState) {
        this.runtimeState = runtimeState;
    }

    public Optional<ArchiveProvisionRule> get(ResourceLocation itemId) {
        return Optional.ofNullable(rules.get(itemId));
    }

    public Collection<ArchiveProvisionRule> all() {
        return Collections.unmodifiableCollection(rules.values());
    }

    public Collection<ArchiveProvisionRule> enabledRules() {
        return rules.values().stream()
                .filter(ArchiveProvisionRule::enabled)
                .toList();
    }

    public void set(ResourceLocation itemId, int targetCount) {
        rules.put(itemId, new ArchiveProvisionRule(itemId, targetCount, true));
        markDirty();
    }

    public boolean remove(ResourceLocation itemId) {
        boolean removed = rules.remove(itemId) != null;
        if (removed) {
            markDirty();
        }
        return removed;
    }

    public int size() {
        return rules.size();
    }

    public ListTag save() {
        ListTag list = new ListTag();
        for (ArchiveProvisionRule rule : rules.values()) {
            list.add(rule.save());
        }
        return list;
    }

    public void load(ListTag list) {
        rules.clear();
        for (int i = 0; i < list.size(); i++) {
            ArchiveProvisionRule.load(list.getCompound(i))
                    .ifPresent(rule -> rules.put(rule.itemId(), rule));
        }
    }

    private void markDirty() {
        runtimeState.markDirty(ArchiveDirtyReason.PROVISION_RULE_CHANGED);
    }
}
