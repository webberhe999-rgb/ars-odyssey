package com.swvague.ars_odyssey.api.event;

import com.swvague.ars_odyssey.knowledge.GlyphRelation;
import com.swvague.ars_odyssey.knowledge.RelationType;
import com.swvague.ars_odyssey.knowledge.TargetDescriptor;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/**
 * Fired on {@code NeoForge.EVENT_BUS} (server-side) whenever a player discovers a new
 * glyph relation through gameplay (casting spells, summons, fangs damage, etc.) or via
 * {@link com.swvague.ars_odyssey.api.ArsOdysseyApi#grantRelation}.
 *
 * <p>This event fires <em>after</em> the relation has been persisted and synced to the
 * client. It is not cancellable — use it for side-effects only (rewards, advancements,
 * inter-mod callbacks, etc.)
 *
 * <p>Example:
 * <pre>{@code
 *   NeoForge.EVENT_BUS.addListener((OdysseyRelationDiscoveredEvent event) -> {
 *       if (event.getRelation().glyphId().equals(MY_GLYPH)) {
 *           event.getServerPlayer().addItem(new ItemStack(Items.DIAMOND));
 *       }
 *   });
 * }</pre>
 */
public class OdysseyRelationDiscoveredEvent extends PlayerEvent {

    private final GlyphRelation relation;
    private final int resolvedCount;

    public OdysseyRelationDiscoveredEvent(ServerPlayer player, GlyphRelation relation, int resolvedCount) {
        super(player);
        this.relation = relation;
        this.resolvedCount = resolvedCount;
    }

    /** The player as a {@link ServerPlayer} (always valid — this event is server-only). */
    public ServerPlayer getServerPlayer() {
        return (ServerPlayer) getEntity();
    }

    /** The newly discovered glyph relation (glyphId, relationType, target). */
    public GlyphRelation getRelation() {
        return relation;
    }

    /** Convenience — the glyph that was discovered. */
    public ResourceLocation getGlyphId() {
        return relation.glyphId();
    }

    /** Convenience — what kind of relation was resolved (APPLIES_TO, PRODUCES, …). */
    public RelationType getRelationType() {
        return relation.relationType();
    }

    /** Convenience — the target descriptor (entity type, block, item, etc.). */
    public TargetDescriptor getTarget() {
        return relation.target();
    }

    /**
     * The total number of distinct resolved targets for this glyph,
     * counted after this discovery was added.
     */
    public int getResolvedCount() {
        return resolvedCount;
    }
}
