# Ars Odyssey Knowledge System Plan

## Core Philosophy

Ars Odyssey is built around one central idea:

**Knowledge is magic.**

The Odyssey Spell Book should not only be a list of glyphs. It should become a record of what the player has learned about the world, about glyph behavior, and about the hidden relationships between spells and targets.

## Glyph Knowledge Index

The Glyph Knowledge Index should describe more than whether a glyph "can apply to" a target.

Each discovered relationship should have a relation type:

- `APPLIES_TO`: the glyph can act on the target.
- `CONDITION`: the glyph requires a condition to be true.
- `PRODUCES`: the glyph can produce a result, output, or transformation.
- `LIMITATION`: the glyph does not apply under this condition or to this target.
- `RELATED`: the glyph has a meaningful relationship to the target, but the relation is not yet more specific.

This lets the system distinguish target knowledge from recipe knowledge, result knowledge, and failure knowledge.

## Truth Value

Future versions should introduce a player-owned **Truth Value**.

Truth Value represents how much verified magical knowledge the player has accumulated. It should be stored in player data, not in static global state.

The long-term loop is:

1. The player experiments with a glyph.
2. The glyph interacts with a block, item, entity, tag, state, or condition.
3. Ars Odyssey records the discovered relation.
4. The player gains Truth.
5. The Odyssey Spell Book becomes more informative as the player's knowledge grows.

## Discovery Logic

### First Version

The first implementation can use a simple rule:

`glyph + target + rule match = discovered relation`

If the player casts a glyph at a target and the Glyph Knowledge Index has a matching rule, the relation can be unlocked immediately.

This is enough to prove the progression loop without needing deep world-state validation.

### Second Version

The second implementation should verify actual effects before unlocking knowledge.

Examples:

- A block changed from water to ice.
- A LivingEntity received a potion effect.
- An item entity was converted by a recipe.
- A block was harvested or broken.
- A teleport succeeded.
- A limitation was observed because the spell failed against a denied target.

This version should detect real world or entity state changes before awarding Truth.

## Future API Surface

Leave room for a small knowledge API:

```java
void onGlyphRelationDiscovered(ServerPlayer player, ResourceLocation glyphId, TargetDescriptor target, GlyphRelation relation);

int getTruthValue(ServerPlayer player);

boolean hasDiscovered(ServerPlayer player, GlyphRelation relation);

void addTruth(ServerPlayer player, int amount);
```

The exact `TargetDescriptor` and `GlyphRelation` types can be designed later. They should be serializable and stable across game sessions.

## Spell Book Integration

Future UI work:

- Show Truth Value in the upper-left corner of the Odyssey Spell Book.
- Reveal relation details only after discovery.
- Use undiscovered relations as hints rather than full answers.
- Keep the original Ars Nouveau spell book behavior unchanged for non-Odyssey books.

## Current Non-Goals

Do not implement these yet:

- No GUI integration.
- No capability/player-data implementation.
- No search logic changes.
- No JEI integration.
- No JSON knowledge database.

This document is only the design anchor for the future knowledge and Truth systems.
