package com.example.ars_odyssey.knowledge;

import com.example.ars_odyssey.knowledge.complexity.GlyphComplexityRecord;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.common.util.INBTSerializable;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class PlayerKnowledgeData implements INBTSerializable<CompoundTag> {
    private int truthValue;
    private final Set<GlyphRelation> discoveredRelations = new LinkedHashSet<>();
    private final Map<ResourceLocation, GlyphComplexityRecord> complexityRecords = new LinkedHashMap<>();

    public int getTruthValue() {
        return truthValue;
    }

    public int truthValue() {
        return truthValue;
    }

    public void addTruth(int amount) {
        truthValue += amount;
    }

    public boolean hasDiscovered(GlyphRelation relation) {
        return discoveredRelations.contains(relation);
    }

    public boolean discover(GlyphRelation relation, TruthDelta delta) {
        if (relation == null) {
            return false;
        }
        // 按语义键去重：(glyphId, relationType, target)
        // evidenceKey 是证据来源，不影响知识唯一性
        boolean alreadyKnown = discoveredRelations.stream().anyMatch(r ->
                java.util.Objects.equals(r.glyphId(), relation.glyphId())
                        && r.relationType() == relation.relationType()
                        && java.util.Objects.equals(r.target(), relation.target()));
        if (alreadyKnown) {
            return false;
        }
        discoveredRelations.add(relation);
        if (delta != null) {
            addTruth(delta.amount());
        }
        return true;
    }

    public boolean forget(ResourceLocation glyphId, TargetDescriptor target) {
        return discoveredRelations.removeIf(relation -> glyphId != null
                && target != null
                && glyphId.equals(relation.glyphId())
                && target.equals(relation.target()));
    }

    public Set<GlyphRelation> getDiscoveredRelations() {
        return Collections.unmodifiableSet(discoveredRelations);
    }

    public Set<GlyphRelation> discoveredRelations() {
        return getDiscoveredRelations();
    }

    public GlyphComplexityRecord getComplexityRecord(ResourceLocation glyphId) {
        if (glyphId == null) {
            return GlyphComplexityRecord.initial(null);
        }
        return complexityRecords.getOrDefault(glyphId, GlyphComplexityRecord.initial(glyphId));
    }

    public Map<ResourceLocation, GlyphComplexityRecord> complexityRecords() {
        return Collections.unmodifiableMap(complexityRecords);
    }

    public boolean updateComplexity(GlyphComplexityRecord candidate) {
        if (candidate == null || candidate.glyphId() == null) {
            return false;
        }
        GlyphComplexityRecord existing = getComplexityRecord(candidate.glyphId());
        if (candidate.isBetterThan(existing)) {
            complexityRecords.put(candidate.glyphId(), candidate);
            return true;
        }
        return false;
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("truthValue", truthValue);

        ListTag relations = new ListTag();
        for (GlyphRelation relation : discoveredRelations) {
            if (relation.glyphId() == null) {
                continue;
            }
            relations.add(saveRelation(relation));
        }
        tag.put("discoveredRelations", relations);
        tag.put("complexityRecords", saveComplexityRecords());
        return tag;
    }

    public static PlayerKnowledgeData load(CompoundTag tag) {
        PlayerKnowledgeData data = new PlayerKnowledgeData();
        data.deserialize(tag);
        return data;
    }

    @Override
    public CompoundTag serializeNBT(HolderLookup.Provider provider) {
        return save();
    }

    @Override
    public void deserializeNBT(HolderLookup.Provider provider, CompoundTag nbt) {
        deserialize(nbt);
    }

    private void deserialize(CompoundTag tag) {
        truthValue = tag.getInt("truthValue");
        discoveredRelations.clear();
        complexityRecords.clear();

        ListTag relations = tag.getList("discoveredRelations", Tag.TAG_COMPOUND);
        for (int i = 0; i < relations.size(); i++) {
            loadRelation(relations.getCompound(i)).ifPresent(discoveredRelations::add);
        }

        ListTag complexity = tag.getList("complexityRecords", Tag.TAG_COMPOUND);
        for (int i = 0; i < complexity.size(); i++) {
            loadComplexityRecord(complexity.getCompound(i)).ifPresent(record ->
                    complexityRecords.put(record.glyphId(), record));
        }
    }

    private static CompoundTag saveRelation(GlyphRelation relation) {
        CompoundTag tag = new CompoundTag();
        tag.putString("glyphId", relation.glyphId().toString());
        tag.putString("relationType", relation.relationType().name());
        tag.put("target", saveTarget(relation.target()));
        relation.result().ifPresent(result -> tag.put("result", saveTarget(result)));
        tag.putString("evidenceKey", relation.evidenceKey());
        return tag;
    }

    private static Optional<GlyphRelation> loadRelation(CompoundTag tag) {
        ResourceLocation glyphId = ResourceLocation.tryParse(tag.getString("glyphId"));
        if (glyphId == null) {
            return Optional.empty();
        }

        RelationType relationType = parseEnum(RelationType.class, tag.getString("relationType"), RelationType.RELATED);
        TargetDescriptor target = loadTarget(tag.getCompound("target"));
        Optional<TargetDescriptor> result = tag.contains("result", Tag.TAG_COMPOUND)
                ? Optional.of(loadTarget(tag.getCompound("result")))
                : Optional.empty();
        return Optional.of(new GlyphRelation(glyphId, relationType, target, result, tag.getString("evidenceKey")));
    }

    private static CompoundTag saveTarget(TargetDescriptor target) {
        CompoundTag tag = new CompoundTag();
        tag.putString("kind", target.kind().name());
        if (target.id() != null) {
            tag.putString("id", target.id().toString());
        }
        tag.putString("detail", target.detail());
        return tag;
    }

    private static TargetDescriptor loadTarget(CompoundTag tag) {
        TargetKind kind = parseEnum(TargetKind.class, tag.getString("kind"), TargetKind.UNKNOWN);
        ResourceLocation id = tag.contains("id", Tag.TAG_STRING)
                ? ResourceLocation.tryParse(tag.getString("id"))
                : null;
        return new TargetDescriptor(kind, id, tag.getString("detail"));
    }

    private ListTag saveComplexityRecords() {
        ListTag records = new ListTag();
        for (GlyphComplexityRecord record : complexityRecords.values()) {
            if (record.glyphId() != null) {
                records.add(saveComplexityRecord(record));
            }
        }
        return records;
    }

    private static CompoundTag saveComplexityRecord(GlyphComplexityRecord record) {
        CompoundTag tag = new CompoundTag();
        tag.putString("glyphId", record.glyphId().toString());
        tag.putDouble("highestComplexity", record.highestComplexity());
        tag.putInt("modifierCount", record.modifierCount());

        ListTag achievedBy = new ListTag();
        for (ResourceLocation glyphId : record.achievedByGlyphs()) {
            if (glyphId != null) {
                achievedBy.add(StringTag.valueOf(glyphId.toString()));
            }
        }
        tag.put("achievedByGlyphs", achievedBy);
        return tag;
    }

    private static Optional<GlyphComplexityRecord> loadComplexityRecord(CompoundTag tag) {
        ResourceLocation glyphId = ResourceLocation.tryParse(tag.getString("glyphId"));
        if (glyphId == null) {
            return Optional.empty();
        }

        ListTag achievedTag = tag.getList("achievedByGlyphs", Tag.TAG_STRING);
        java.util.ArrayList<ResourceLocation> achievedBy = new java.util.ArrayList<>();
        for (int i = 0; i < achievedTag.size(); i++) {
            ResourceLocation achievedId = ResourceLocation.tryParse(achievedTag.getString(i));
            if (achievedId != null) {
                achievedBy.add(achievedId);
            }
        }

        double highestComplexity = tag.contains("highestComplexity", Tag.TAG_DOUBLE)
                ? tag.getDouble("highestComplexity")
                : 1.0D;
        int modifierCount = tag.getInt("modifierCount");
        return Optional.of(new GlyphComplexityRecord(glyphId, highestComplexity, List.copyOf(achievedBy), modifierCount));
    }

    private static <T extends Enum<T>> T parseEnum(Class<T> enumClass, String value, T fallback) {
        try {
            return Enum.valueOf(enumClass, value);
        } catch (IllegalArgumentException e) {
            return fallback;
        }
    }
}
