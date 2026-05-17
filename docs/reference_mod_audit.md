# Ars Odyssey Reference Mod Audit

## 2026-05-15 - Ars Elemental

Reference repository:

- https://github.com/Alexthw46/Ars-Elemental
- Branch observed: `1.21`
- Scope of this audit: package structure, registry layout, Ars Nouveau integration style, and what Ars Odyssey can safely learn from it.

License note:

- GitHub metadata shows GPL-3.0, while the README text mentions LGPL V3 usage through Maven.
- Ars Odyssey should treat this as a structure reference only. Do not copy large code blocks or implementation details directly.

## Observed Structure

Ars Elemental uses a broad but clear top-level package split:

- `client`
- `common`
- `datagen`
- `documentation`
- `event`
- `mixin`
- `network`
- `recipe`
- `registry`
- `util`
- `world`

This is useful for Ars Odyssey because our current code is concentrated around a few large classes:

- `GuiSpellBookMixin`
- `GlyphApplicationIndex`
- `EntityEffectObservationHandler`
- `EntityProductionObservationHandler`

Ars Elemental also keeps major DeferredRegister holders under `registry`:

- `ModAdvTriggers`
- `ModEntities`
- `ModItems`
- `ModParticles`
- `ModPotions`
- `ModRegistry`
- `ModTiles`

Ars Odyssey currently has a smaller surface area, but the same direction is healthy:

- keep NeoForge deferred registries in `registry`
- keep Ars Nouveau API registration in an explicit Ars integration class
- avoid package/path mismatches such as `ArsNouveauRegistry.java` living outside `registry`

## Ars Nouveau Integration Pattern

Ars Elemental has a dedicated `ArsNouveauRegistry` with:

- `init()`
- `registerGlyphs()`
- `registerRituals()`
- `registerFamiliars()`
- `registerPerks()`
- `postInit()`

It also keeps a `registeredSpells` list for datagen/documentation style flows.

For Ars Odyssey, this suggests:

- If we keep custom glyph registration, it should live in a clearly named Ars integration class.
- If we are not currently registering custom glyphs, the example `ArsNouveauRegistry` / `TestEffect` path should be marked as example-only or removed.
- Runtime glyph discovery should remain separate from registration. `GlyphRuntimeDiscovery` should not become the place that registers things.

## Registry Pattern

Ars Elemental centralizes bus registration through a `registerRegistries(IEventBus bus)` method in `ModRegistry`, while feature-specific registry holders remain split into dedicated files.

For Ars Odyssey:

- `ModRegistry` is fine for the current tiny item set.
- If the project grows, split by domain:
  - `ModItems`
  - `ModDataAttachments` or `PlayerKnowledgeAttachments`
  - `ModNetwork`
  - future `ModBlocks` / `ModMenus` only when they actually exist
- Do not keep example cosmetic/sound entries in the primary registry once they are no longer used.

## Event Pattern

Ars Elemental separates event handlers into files such as:

- `DamageEvents`
- `Events`
- `GlyphEvents`
- `ShieldEvents`
- `SummonEvents`

For Ars Odyssey, the equivalent split should be by observation concern:

- `EntityEffectObservationHandler` for direct entity before/after snapshots.
- `EntityProductionObservationHandler` for summon/product/carrier observation.
- Future `BlockEffectObservationHandler` for block snapshots.
- Shared discovery write/sync/message logic should move out of handlers into a common service.

Suggested extraction:

- `DiscoveryFeedbackService`
  - applies `GlyphDiscoveryResult`
  - updates complexity
  - syncs `PlayerKnowledgeData`
  - sends translated feedback messages

This would let event handlers stay small and focused on attribution.

## GUI / Client Structure

Ars Elemental has a dedicated `client` package.

For Ars Odyssey:

- Keep `GlyphTooltipHelper` in `client/tooltip`.
- Move Spell Book UI-only helpers out of `GuiSpellBookMixin` into `client/gui/spellbook`.
- Suggested helper names:
  - `SpellBookSearchUi`
  - `SpellBookSuggestionDropdown`
  - `SpellBookDragController`
  - `SpellBookTooltipAdapter`

Mixin classes should stay thin:

- check whether the book is `ars_odyssey:odyssey_boundless_spell_book`
- call helper
- return to vanilla/Ars Nouveau flow

## Search / Index Structure

Ars Elemental is a content addon, while Ars Odyssey is becoming a knowledge/index addon. So we should not mirror its content package one-to-one.

Still, the structure lesson applies:

- one class should not own a whole subsystem
- split data tables from runtime services
- split common model from client presentation

Recommended split for `GlyphApplicationIndex`:

- `ManualGlyphRules`
- `AutoGlyphRuleIndex`
- `TargetQueryParser`
- `GlyphRuleMatcher`
- `GlyphSearchService`

Keep:

- `GlyphTargetRule`
- `MatchReason`
- `MatcherPresentation`

But avoid letting `GlyphApplicationIndex` keep growing.

## Things Not To Copy

- Do not copy Ars Elemental glyph implementations.
- Do not copy large registry methods directly.
- Do not copy license-sensitive source text.
- Do not adopt its content-driven structure blindly; Ars Odyssey is knowledge/search/discovery-driven.
- Do not put all Ars integration logic into one giant class if we can keep small domain services.

## Recommended Refactor Order For Ars Odyssey

1. Move or remove example/template residue:
   - `ExampleConfig`
   - `ExampleCosmetic`
   - `TestEffect`
   - example sound / `star_hat`
   - misplaced `ArsNouveauRegistry`

2. Create `client/gui/spellbook` helpers and shrink `GuiSpellBookMixin`.

3. Split `GlyphApplicationIndex` into parser, rules, matcher, and search service.

4. Extract dynamic discovery feedback into a shared service.

5. Normalize target parsing:
   - player input -> `TargetDescriptor`
   - matcher -> `TargetDescriptor`
   - coverage check with explicit `Level` / lookup context

6. Keep docs updated after each structural change.
