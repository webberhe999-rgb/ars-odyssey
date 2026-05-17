package com.swvague.ars_odyssey.archive;

import java.util.Optional;

import com.swvague.ars_odyssey.archive.provision.ArchiveProvisionRules;
import com.swvague.ars_odyssey.archive.resonance.ResonanceState;
import com.swvague.ars_odyssey.archive.runtime.ArchiveDirtyReason;
import com.swvague.ars_odyssey.archive.runtime.ArchiveRuntimeState;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.common.util.INBTSerializable;

public class PlayerArchiveData implements INBTSerializable<CompoundTag> {
    private boolean awakened;
    private ArchiveAccessMode unlockedAccessMode = ArchiveAccessMode.SEALED;
    private ArchiveCoreBinding coreBinding;
    private final ArchiveLedger ledger = new ArchiveLedger();
    private final ArchiveRuntimeState runtimeState = new ArchiveRuntimeState();
    private final ArchiveProvisionRules provisionRules = new ArchiveProvisionRules(runtimeState);
    private final ResonanceState resonanceState = new ResonanceState();

    public boolean isAwakened() {
        return awakened;
    }

    public ArchiveAccessMode unlockedAccessMode() {
        return unlockedAccessMode;
    }

    public Optional<ArchiveCoreBinding> coreBinding() {
        return Optional.ofNullable(coreBinding);
    }

    public ArchiveLedger ledger() {
        return ledger;
    }

    public ArchiveRuntimeState runtimeState() {
        return runtimeState;
    }

    public ArchiveProvisionRules provisionRules() {
        return provisionRules;
    }

    public ResonanceState resonanceState() {
        return resonanceState;
    }

    public void awaken(ResourceLocation dimension, BlockPos pos) {
        awakened = true;
        unlockedAccessMode = ArchiveAccessMode.INDEXED;
        coreBinding = new ArchiveCoreBinding(dimension, pos.immutable());
        runtimeState.markDirty(ArchiveDirtyReason.CORE_BOUND);
    }

    public void setUnlockedAccessMode(ArchiveAccessMode mode) {
        if (mode != null) {
            unlockedAccessMode = mode;
            awakened = awakened || mode != ArchiveAccessMode.SEALED;
            runtimeState.markDirty(ArchiveDirtyReason.ACCESS_MODE_CHANGED);
        }
    }

    public CompoundTag save() {
        return save(null);
    }

    public CompoundTag save(HolderLookup.Provider provider) {
        CompoundTag tag = new CompoundTag();
        tag.putBoolean("awakened", awakened);
        tag.putString("unlockedAccessMode", unlockedAccessMode.name());
        if (coreBinding != null) {
            tag.put("coreBinding", coreBinding.save());
        }
        tag.put("ledger", ledger.save(provider));
        tag.put("provisionRules", provisionRules.save());
        tag.put("resonance", resonanceState.save());
        return tag;
    }

    public static PlayerArchiveData load(CompoundTag tag) {
        return load(tag, null);
    }

    public static PlayerArchiveData load(CompoundTag tag, HolderLookup.Provider provider) {
        PlayerArchiveData data = new PlayerArchiveData();
        data.deserialize(tag, provider);
        return data;
    }

    @Override
    public CompoundTag serializeNBT(HolderLookup.Provider provider) {
        return save(provider);
    }

    @Override
    public void deserializeNBT(HolderLookup.Provider provider, CompoundTag nbt) {
        deserialize(nbt, provider);
    }

    private void deserialize(CompoundTag tag) {
        deserialize(tag, null);
    }

    private void deserialize(CompoundTag tag, HolderLookup.Provider provider) {
        awakened = tag.getBoolean("awakened");
        unlockedAccessMode = parseMode(tag.getString("unlockedAccessMode"));
        coreBinding = null;
        if (tag.contains("coreBinding", Tag.TAG_COMPOUND)) {
            ArchiveCoreBinding.load(tag.getCompound("coreBinding")).ifPresent(binding -> coreBinding = binding);
        }
        if (tag.contains("ledger", Tag.TAG_LIST)) {
            ledger.load(tag.getList("ledger", Tag.TAG_COMPOUND), provider);
        } else {
            ledger.load(new net.minecraft.nbt.ListTag());
        }
        if (tag.contains("provisionRules", Tag.TAG_LIST)) {
            provisionRules.load(tag.getList("provisionRules", Tag.TAG_COMPOUND));
        } else {
            provisionRules.load(new net.minecraft.nbt.ListTag());
        }
        if (tag.contains("resonance", Tag.TAG_COMPOUND)) {
            resonanceState.load(tag.getCompound("resonance"));
        } else {
            resonanceState.load(new CompoundTag());
        }
    }

    private static ArchiveAccessMode parseMode(String value) {
        try {
            return ArchiveAccessMode.valueOf(value);
        } catch (IllegalArgumentException ignored) {
            return ArchiveAccessMode.SEALED;
        }
    }
}
