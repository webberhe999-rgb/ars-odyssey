package com.swvague.ars_odyssey.knowledge;

/**
 * Player-specific discovery state for a glyph relation.
 *
 * This is separate from EvidenceConfidence. A RESOLVED relation is something
 * the player has actually learned through play or an explicit unlock source,
 * and should outrank automatic confidence in future UI.
 */
public enum PlayerKnowledgeState {
    UNDISCOVERED,
    HINTED,
    RESOLVED
}
