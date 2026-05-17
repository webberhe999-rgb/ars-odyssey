package com.swvague.ars_odyssey.archive;

import com.swvague.ars_odyssey.config.OdysseyConfig;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;

public final class ArchiveCoreChunkLoading {
    private ArchiveCoreChunkLoading() {
    }

    public static boolean ensureForced(MinecraftServer server, PlayerArchiveData data) {
        if (server == null || data == null || !data.isAwakened() || !OdysseyConfig.FORCE_LOAD_ARCHIVE_CORE_CHUNK.get()) {
            return false;
        }
        return data.coreBinding()
                .map(binding -> ensureForced(server, binding))
                .orElse(false);
    }

    public static boolean ensureForced(MinecraftServer server, ArchiveCoreBinding binding) {
        if (server == null || binding == null || !OdysseyConfig.FORCE_LOAD_ARCHIVE_CORE_CHUNK.get()) {
            return false;
        }
        ServerLevel level = server.getLevel(ResourceKey.create(Registries.DIMENSION, binding.dimension()));
        if (level == null) {
            return false;
        }
        ChunkPos chunkPos = new ChunkPos(binding.pos());
        level.setChunkForced(chunkPos.x, chunkPos.z, true);
        return true;
    }
}
