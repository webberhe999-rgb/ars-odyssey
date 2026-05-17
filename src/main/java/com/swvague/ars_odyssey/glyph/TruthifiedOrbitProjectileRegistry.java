package com.swvague.ars_odyssey.glyph;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Tracks active truthified Orbit Self spell groups per owner.
 *
 * <p>This intentionally tracks spell groups, not glyph ids. A future per-spell
 * truthification toggle can reuse the same grouping layer without changing
 * projectile lifetime semantics.</p>
 */
public final class TruthifiedOrbitProjectileRegistry {
    private static final Map<UUID, Deque<UUID>> GROUPS_BY_OWNER = new LinkedHashMap<>();
    private static final Map<UUID, Integer> PERCENT_STACKS_BY_GROUP = new LinkedHashMap<>();
    // Number of truthified projectiles a group spawned with (initial + splits).
    private static final Map<UUID, Integer> INITIAL_COUNT_BY_GROUP = new LinkedHashMap<>();
    private static final Map<UUID, Long> LAST_DRAIN_TICK_BY_OWNER = new LinkedHashMap<>();

    private TruthifiedOrbitProjectileRegistry() {
    }

    public static synchronized List<UUID> register(UUID ownerId, UUID groupId, int maxGroups) {
        return register(ownerId, groupId, maxGroups, 1);
    }

    public static synchronized List<UUID> register(UUID ownerId, UUID groupId, int maxGroups, int percentStacks) {
        if (ownerId == null || groupId == null) {
            return List.of();
        }
        int safeMax = Math.max(1, maxGroups);
        Deque<UUID> groups = GROUPS_BY_OWNER.computeIfAbsent(ownerId, ignored -> new ArrayDeque<>());
        groups.remove(groupId);
        groups.addLast(groupId);
        PERCENT_STACKS_BY_GROUP.put(groupId, Math.max(1, percentStacks));

        List<UUID> removed = new ArrayList<>();
        while (groups.size() > safeMax) {
            UUID oldest = groups.removeFirst();
            if (oldest != null) {
                removed.add(oldest);
                PERCENT_STACKS_BY_GROUP.remove(oldest);
                INITIAL_COUNT_BY_GROUP.remove(oldest);
            }
        }
        return removed;
    }

    /** Records how many truthified projectiles {@code groupId} started with. */
    public static synchronized void setInitialProjectileCount(UUID groupId, int count) {
        if (groupId != null) {
            INITIAL_COUNT_BY_GROUP.put(groupId, Math.max(0, count));
        }
    }

    /** Total initial truthified projectiles across all of the owner's groups. */
    public static synchronized int initialProjectileCount(UUID ownerId) {
        Deque<UUID> groups = GROUPS_BY_OWNER.get(ownerId);
        if (groups == null || groups.isEmpty()) {
            return 0;
        }
        int total = 0;
        for (UUID groupId : groups) {
            total += Math.max(0, INITIAL_COUNT_BY_GROUP.getOrDefault(groupId, 0));
        }
        return total;
    }

    public static synchronized void remove(UUID ownerId, UUID groupId) {
        if (ownerId == null || groupId == null) {
            return;
        }
        Deque<UUID> groups = GROUPS_BY_OWNER.get(ownerId);
        if (groups == null) {
            return;
        }
        groups.remove(groupId);
        PERCENT_STACKS_BY_GROUP.remove(groupId);
        INITIAL_COUNT_BY_GROUP.remove(groupId);
        if (groups.isEmpty()) {
            GROUPS_BY_OWNER.remove(ownerId);
            LAST_DRAIN_TICK_BY_OWNER.remove(ownerId);
        }
    }

    public static synchronized void clear(UUID ownerId) {
        if (ownerId != null) {
            Deque<UUID> groups = GROUPS_BY_OWNER.remove(ownerId);
            if (groups != null) {
                for (UUID groupId : groups) {
                    PERCENT_STACKS_BY_GROUP.remove(groupId);
                    INITIAL_COUNT_BY_GROUP.remove(groupId);
                }
            }
            LAST_DRAIN_TICK_BY_OWNER.remove(ownerId);
        }
    }

    public static synchronized boolean markDrainIfDue(UUID ownerId, long gameTime, long intervalTicks) {
        if (ownerId == null) {
            return false;
        }
        Long previous = LAST_DRAIN_TICK_BY_OWNER.get(ownerId);
        if (previous != null && gameTime - previous < intervalTicks) {
            return false;
        }
        LAST_DRAIN_TICK_BY_OWNER.put(ownerId, gameTime);
        return true;
    }

    public static synchronized int activePercentStacks(UUID ownerId) {
        Deque<UUID> groups = GROUPS_BY_OWNER.get(ownerId);
        if (groups == null || groups.isEmpty()) {
            return 0;
        }
        int stacks = 0;
        for (UUID groupId : groups) {
            stacks += Math.max(1, PERCENT_STACKS_BY_GROUP.getOrDefault(groupId, 1));
        }
        return stacks;
    }
}
