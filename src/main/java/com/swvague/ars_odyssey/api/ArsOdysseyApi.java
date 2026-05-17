package com.swvague.ars_odyssey.api;

import com.swvague.ars_odyssey.index.EvidenceConfidence;
import com.swvague.ars_odyssey.knowledge.DiscoverySource;
import com.swvague.ars_odyssey.knowledge.EmptyPlayerKnowledgeView;
import com.swvague.ars_odyssey.knowledge.GlyphRelation;
import com.swvague.ars_odyssey.knowledge.PlayerKnowledgeAttachments;
import com.swvague.ars_odyssey.knowledge.PlayerKnowledgeDataView;
import com.swvague.ars_odyssey.knowledge.PlayerKnowledgeView;
import com.swvague.ars_odyssey.knowledge.RelationType;
import com.swvague.ars_odyssey.knowledge.TargetDescriptor;
import com.swvague.ars_odyssey.knowledge.TruthDelta;
import com.swvague.ars_odyssey.knowledge.discovery.DiscoveryFeedbackService;
import com.swvague.ars_odyssey.knowledge.discovery.GlyphDiscoveryResult;
import com.swvague.ars_odyssey.knowledge.discovery.GlyphDiscoveryService;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import java.util.List;
import java.util.Optional;

/**
 * Public API for Ars Odyssey — the primary integration point for other mods.
 *
 * <h3>Reading knowledge (client &amp; server)</h3>
 * <ul>
 *   <li>{@link #getView(Player)} — read-only view of the player's glyph knowledge</li>
 *   <li>{@link #hasDiscovered(Player, ResourceLocation, TargetDescriptor)}</li>
 * </ul>
 *
 * <h3>Writing knowledge (server-side only)</h3>
 * <ul>
 *   <li>{@link #grantRelation(ServerPlayer, ResourceLocation, RelationType, TargetDescriptor, String)}</li>
 * </ul>
 *
 * <h3>Cross-mod hooks (NeoForge events)</h3>
 * <ul>
 *   <li>{@link com.swvague.ars_odyssey.api.event.OdysseyGlyphRuleEvent} — register static rules during setup</li>
 *   <li>{@link com.swvague.ars_odyssey.api.event.OdysseyRelationDiscoveredEvent} — listen to new discoveries</li>
 * </ul>
 *
 * <p>Thread safety: read methods are safe from any thread. Write methods must be
 * called from the server game thread.
 */
public final class ArsOdysseyApi {
    private ArsOdysseyApi() {
    }

    // ── Read ─────────────────────────────────────────────────────────────────

    /**
     * Returns a read-only view of the player's glyph knowledge.
     *
     * <p>On the client, this reflects the last synced server state.
     * Returns {@link EmptyPlayerKnowledgeView} if the player is null or
     * the attachment is unavailable.
     */
    public static PlayerKnowledgeView getView(Player player) {
        if (player == null) {
            return EmptyPlayerKnowledgeView.INSTANCE;
        }
        try {
            return new PlayerKnowledgeDataView(
                    player.getData(PlayerKnowledgeAttachments.PLAYER_KNOWLEDGE),
                    player.level());
        } catch (Exception e) {
            return EmptyPlayerKnowledgeView.INSTANCE;
        }
    }

    /**
     * Returns true if the player has discovered the given glyph-target relation.
     * Works on both client and server.
     */
    public static boolean hasDiscovered(Player player, ResourceLocation glyphId, TargetDescriptor target) {
        return getView(player).isResolved(glyphId, target);
    }

    /**
     * Returns the number of distinct resolved targets for this glyph.
     * Equivalent to {@code getView(player).countResolvedRelations(glyphId)}.
     */
    public static int countResolvedRelations(Player player, ResourceLocation glyphId) {
        return getView(player).countResolvedRelations(glyphId);
    }

    // ── Write (server-side only) ──────────────────────────────────────────────

    /**
     * Grants the player a discovered glyph relation. On success this method:
     * <ol>
     *   <li>Persists the relation to the player's knowledge attachment.</li>
     *   <li>Updates the glyph's complexity record (if spell parts are present — none
     *       are supplied through this API path, so complexity does not change here).</li>
     *   <li>Syncs the full knowledge state to the client via network packet.</li>
     *   <li>Sends a discovery chat message to the player.</li>
     *   <li>Fires {@link com.swvague.ars_odyssey.api.event.OdysseyRelationDiscoveredEvent}
     *       on {@code NeoForge.EVENT_BUS}.</li>
     * </ol>
     *
     * <p>If the player already has this relation (same glyphId + relationType + target),
     * <strong>none</strong> of the above side effects occur and {@code false} is returned.
     *
     * <p>Must be called on the server game thread.
     *
     * @param player      the player who discovers the relation (server-side)
     * @param glyphId     Ars Nouveau glyph registry name, e.g. {@code ars_nouveau:glyph_harm}
     * @param type        relation type — usually {@link RelationType#APPLIES_TO} for effects,
     *                    {@link RelationType#PRODUCES} for summons
     * @param target      the resolved target ({@link TargetDescriptor#entityType},
     *                    {@link TargetDescriptor#block}, {@link TargetDescriptor#item}, …)
     * @param evidenceKey unique string identifying this discovery source, used for deduplication;
     *                    use a stable constant per glyph+target combo in your mod
     * @return {@code true} if a new relation was added; {@code false} if already known
     */
    public static boolean grantRelation(
            ServerPlayer player,
            ResourceLocation glyphId,
            RelationType type,
            TargetDescriptor target,
            String evidenceKey
    ) {
        if (player == null || glyphId == null || type == null || target == null) {
            return false;
        }
        String key = evidenceKey != null ? evidenceKey : "api_grant";
        GlyphRelation relation = new GlyphRelation(glyphId, type, target, Optional.empty(), key);
        GlyphDiscoveryResult result = GlyphDiscoveryService.resolveRelation(
                relation,
                DiscoverySource.PLAYER_CAST,
                EvidenceConfidence.HIGH,
                new TruthDelta(1, key));
        DiscoveryFeedbackService.DiscoveryApplyResult applied =
                DiscoveryFeedbackService.applyAndNotify(player, result, List.of());
        return applied.added();
    }
}
