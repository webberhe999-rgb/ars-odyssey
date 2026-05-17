package com.swvague.ars_odyssey.knowledge;

import net.minecraft.core.RegistryAccess;
import net.minecraft.world.level.Level;

/**
 * Context passed to {@link TargetCoverageResolver#covers(TargetDescriptor, TargetDescriptor, TargetCoverageContext)}
 * so coverage checks can spawn temporary entities for class-hierarchy queries without a
 * hard dependency on {@code Minecraft.getInstance()}.
 *
 * <p>Factory methods:
 * <ul>
 *   <li>{@link #empty()} / {@link #none()} — no level; class-hierarchy checks return false conservatively</li>
 *   <li>{@link #of(Level)} — infer client/server from the level instance</li>
 *   <li>{@link #client(Level)} — explicit client-side context</li>
 *   <li>{@link #server(Level)} — explicit server-side context</li>
 * </ul>
 */
public record TargetCoverageContext(
        Level level,
        RegistryAccess registryAccess,
        boolean clientSide
) {
    /** No level available; conservative fallback — class-hierarchy checks will return {@code false}. */
    public static TargetCoverageContext empty() {
        return new TargetCoverageContext(null, null, false);
    }

    /**
     * Alias for {@link #empty()}.
     * @deprecated prefer {@link #empty()} for clarity
     */
    @Deprecated
    public static TargetCoverageContext none() {
        return empty();
    }

    /** Infer client/server from {@code level.isClientSide()}. */
    public static TargetCoverageContext of(Level level) {
        if (level == null) {
            return empty();
        }
        return new TargetCoverageContext(
                level,
                level.registryAccess(),
                level.isClientSide());
    }

    /** Explicit client-side context. */
    public static TargetCoverageContext client(Level level) {
        if (level == null) {
            return empty();
        }
        return new TargetCoverageContext(level, level.registryAccess(), true);
    }

    /** Explicit server-side context. */
    public static TargetCoverageContext server(Level level) {
        if (level == null) {
            return empty();
        }
        return new TargetCoverageContext(level, level.registryAccess(), false);
    }
}
