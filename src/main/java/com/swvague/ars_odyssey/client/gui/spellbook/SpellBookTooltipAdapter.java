package com.swvague.ars_odyssey.client.gui.spellbook;

import com.swvague.ars_odyssey.client.tooltip.GlyphTooltipHelper;
import com.swvague.ars_odyssey.index.GlyphApplicationIndex;
import com.swvague.ars_odyssey.index.MatchDisplayLabel;
import com.swvague.ars_odyssey.index.MatchReason;
import com.swvague.ars_odyssey.index.MatchReasonDisplayReducer;
import com.swvague.ars_odyssey.index.MatcherPresentation;
import com.swvague.ars_odyssey.index.SearchIntent;
import com.swvague.ars_odyssey.index.SearchIntentType;
import com.swvague.ars_odyssey.knowledge.EmptyPlayerKnowledgeView;
import com.swvague.ars_odyssey.knowledge.MatcherTargetResolver;
import com.swvague.ars_odyssey.knowledge.PlayerKnowledgeAttachments;
import com.swvague.ars_odyssey.knowledge.PlayerKnowledgeDataView;
import com.swvague.ars_odyssey.knowledge.PlayerKnowledgeView;
import com.swvague.ars_odyssey.knowledge.TargetDescriptor;
import com.swvague.ars_odyssey.knowledge.TargetKind;
import com.hollingsworth.arsnouveau.api.spell.AbstractEffect;
import com.hollingsworth.arsnouveau.api.spell.AbstractSpellPart;
import com.hollingsworth.arsnouveau.client.gui.buttons.CraftingButton;
import com.hollingsworth.arsnouveau.client.gui.buttons.GlyphButton;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@OnlyIn(Dist.CLIENT)
public final class SpellBookTooltipAdapter {
    private SpellBookTooltipAdapter() {
    }

    public static void appendTooltip(
            List<Component> tooltip,
            AbstractSpellPart hoveredPart,
            boolean hoveredPartIsGlyphButton,
            Map<ResourceLocation, List<MatchReason>> reasonsByGlyph,
            SearchIntent searchIntent
    ) {
        appendTooltip(tooltip, hoveredPart, hoveredPartIsGlyphButton, reasonsByGlyph, searchIntent, null);
    }

    public static void appendTooltip(
            List<Component> tooltip,
            AbstractSpellPart hoveredPart,
            boolean hoveredPartIsGlyphButton,
            Map<ResourceLocation, List<MatchReason>> reasonsByGlyph,
            SearchIntent searchIntent,
            Boolean truthificationEnabledForSlot
    ) {
        if (hoveredPart == null || hoveredPart.getRegistryName() == null) {
            return;
        }
        if (!(hoveredPart instanceof AbstractEffect)) {
            GlyphTooltipHelper.replaceTruthifiedDescriptionIfNeeded(
                    hoveredPart.getRegistryName(),
                    tooltip,
                    truthificationEnabledForSlot);
            return;
        }

        if (!hoveredPartIsGlyphButton) {
            appendGlyphSummaryTooltip(tooltip, hoveredPart, truthificationEnabledForSlot);
            return;
        }

        ResourceLocation glyphId = hoveredPart.getRegistryName();
        Map<ResourceLocation, List<MatchReason>> safeReasons =
                reasonsByGlyph == null ? Collections.emptyMap() : reasonsByGlyph;
        List<MatchReason> reasons = MatchReasonDisplayReducer.reduce(
                safeReasons.getOrDefault(glyphId, List.of()));

        boolean isTargetSearch = !reasons.isEmpty() && !isCurrentGlyphIntent(searchIntent, glyphId);

        if (isTargetSearch) {
            appendTargetSearchTooltip(tooltip, glyphId, reasons, searchIntent);
        } else {
            appendGlyphSummaryTooltip(tooltip, hoveredPart, truthificationEnabledForSlot);
        }
    }

    public static MatchReason computeHeaderReason(
            String searchText,
            List<AbstractSpellPart> displayedGlyphs,
            Map<ResourceLocation, List<MatchReason>> reasonsByGlyph
    ) {
        if (searchText == null || searchText.trim().isEmpty()) {
            return null;
        }

        Map<ResourceLocation, List<MatchReason>> safeReasons =
                reasonsByGlyph == null ? Collections.emptyMap() : reasonsByGlyph;

        MatchReason best = null;

        for (AbstractSpellPart part : displayedGlyphs) {
            if (part == null || part.getRegistryName() == null) {
                continue;
            }

            List<MatchReason> reasons = safeReasons.getOrDefault(part.getRegistryName(), List.of());
            for (MatchReason reason : reasons) {
                if (best == null || confidenceRank(reason) > confidenceRank(best)) {
                    best = reason;
                }
            }
        }

        return best;
    }

    public static MatchDisplayLabel displayLabel(MatchReason reason, SearchIntent searchIntent) {
        PlayerKnowledgeView knowledgeView = getKnowledgeView();
        SearchIntent safeIntent = safeIntent(searchIntent);

        if (safeIntent.type() == SearchIntentType.GLYPH
                && reason.glyphId().equals(safeIntent.resolvedId())
                && (knowledgeView.countResolvedRelations(reason.glyphId()) > 0
                || knowledgeView.firstAbstractResolvedTarget(reason.glyphId()).isPresent())) {
            return MatchDisplayLabel.resolved();
        }

        Optional<TargetDescriptor> searchTarget = currentSearchTarget(safeIntent);
        if (searchTarget.isPresent() && knowledgeView.isResolved(reason.glyphId(), searchTarget.get())) {
            return MatchDisplayLabel.resolved();
        }

        Optional<TargetDescriptor> target = MatcherTargetResolver.fromMatcher(reason.matcher());
        if (target.isPresent() && knowledgeView.isResolved(reason.glyphId(), target.get())) {
            return MatchDisplayLabel.resolved();
        }
        return MatchDisplayLabel.confidence(reason.confidence());
    }

    private static void appendTargetSearchTooltip(
            List<Component> tooltip,
            ResourceLocation glyphId,
            List<MatchReason> reasons,
            SearchIntent searchIntent
    ) {
        tooltip.add(Component.empty());
        tooltip.add(Component.translatable("ars_odyssey.tooltip.header").withStyle(ChatFormatting.DARK_AQUA));

        int shown = 0;
        for (MatchReason reason : reasons) {
            if (shown >= 3) {
                break;
            }
            tooltip.add(reasonLine(reason, searchIntent));
            tooltip.add(Component.translatable("ars_odyssey.tooltip.evidence", sourceText(reason))
                    .withStyle(ChatFormatting.GRAY));
            shown++;
        }

        SearchIntent safeIntent = safeIntent(searchIntent);
        if (safeIntent.type() == SearchIntentType.ENTITY && safeIntent.resolvedId() != null) {
            GlyphTooltipHelper.appendEntityTargetCategoryLine(
                    tooltip, glyphId, safeIntent.resolvedId());
        }
    }

    private static void appendGlyphSummaryTooltip(List<Component> tooltip, AbstractSpellPart part) {
        appendGlyphSummaryTooltip(tooltip, part, null);
    }

    private static void appendGlyphSummaryTooltip(List<Component> tooltip, AbstractSpellPart part, Boolean truthificationEnabledForSlot) {
        if (part == null || part.getRegistryName() == null) {
            return;
        }
        GlyphTooltipHelper.appendOdysseyTooltip(part.getRegistryName(), tooltip, truthificationEnabledForSlot);
    }

    private static Component reasonLine(MatchReason reason, SearchIntent searchIntent) {
        Component target = targetText(reason);
        Component body;

        if (reason.blacklist()) {
            body = Component.translatable("ars_odyssey.tooltip.limit",
                    Component.translatable("ars_odyssey.tooltip.not_applicable_to", target));
        } else {
            body = switch (MatcherPresentation.roleOf(reason.matcher())) {
                case CONDITION -> Component.translatable("ars_odyssey.tooltip.condition", target);
                case LIMIT -> Component.translatable("ars_odyssey.tooltip.limit",
                        Component.translatable("ars_odyssey.tooltip.not_applicable_to", target));
                default -> Component.translatable("ars_odyssey.tooltip.reason",
                        Component.translatable("ars_odyssey.tooltip.target", target));
            };
        }

        return Component.empty()
                .append(Component.literal("["))
                .append(confidenceText(reason, searchIntent))
                .append(Component.literal("] "))
                .append(body);
    }

    private static Component confidenceText(MatchReason reason, SearchIntent searchIntent) {
        MatchDisplayLabel label = displayLabel(reason, searchIntent);
        return label.text().copy().withColor(label.color());
    }

    private static boolean isCurrentGlyphIntent(SearchIntent searchIntent, ResourceLocation glyphId) {
        SearchIntent safeIntent = safeIntent(searchIntent);
        return safeIntent.type() == SearchIntentType.GLYPH
                && glyphId != null
                && glyphId.equals(safeIntent.resolvedId());
    }

    private static PlayerKnowledgeView getKnowledgeView() {
        if (Minecraft.getInstance().player == null) {
            return EmptyPlayerKnowledgeView.INSTANCE;
        }

        try {
            return new PlayerKnowledgeDataView(
                    Minecraft.getInstance().player.getData(PlayerKnowledgeAttachments.PLAYER_KNOWLEDGE),
                    Minecraft.getInstance().level);
        } catch (Exception e) {
            return EmptyPlayerKnowledgeView.INSTANCE;
        }
    }

    private static Optional<TargetDescriptor> currentSearchTarget(SearchIntent searchIntent) {
        SearchIntent safeIntent = safeIntent(searchIntent);
        if (safeIntent.resolvedId() == null) {
            return Optional.empty();
        }

        return switch (safeIntent.type()) {
            case ENTITY -> Optional.of(TargetDescriptor.entityType(safeIntent.resolvedId()));
            case BLOCK -> Optional.of(TargetDescriptor.block(safeIntent.resolvedId()));
            case ITEM -> Optional.of(TargetDescriptor.item(safeIntent.resolvedId()));
            case TAG -> Optional.of(new TargetDescriptor(TargetKind.UNKNOWN, safeIntent.resolvedId(), "tag"));
            default -> Optional.empty();
        };
    }

    private static Component targetText(MatchReason reason) {
        String key = reason.displayKey();
        if (!key.isBlank() && Language.getInstance().has(key)) {
            return Component.translatable(key);
        }
        return Component.literal(MatcherPresentation.rawValueForDisplay(reason.matcher().value()));
    }

    private static Component sourceText(MatchReason reason) {
        if (reason.source() == MatchReason.RuleSource.AUTO) {
            return Component.translatable("ars_odyssey.source.auto_source_scan");
        }
        String key = switch (MatcherPresentation.evidenceOf(reason.matcher())) {
            case CODE_CONFIRMED -> "ars_odyssey.source.code_confirmed";
            case RUNTIME_CHECK -> "ars_odyssey.source.runtime_condition";
            case INDEX_HELPER -> "ars_odyssey.source.indexed_tag";
            case INFERRED -> "ars_odyssey.source.inferred";
            case LIMITATION -> "ars_odyssey.source.limitation";
            case AUTO_SOURCE_SCAN -> "ars_odyssey.source.auto_source_scan";
        };
        return Component.translatable(key);
    }

    /**
     * Returns the hovered {@link AbstractSpellPart} if {@code hoveredWidget} is a
     * {@link GlyphButton} or {@link CraftingButton} under the mouse, {@code null} otherwise.
     * Extracted here so {@code GuiSpellBookMixin} stays a thin bridge.
     */
    public static AbstractSpellPart hoveredSpellPart(Renderable hoveredWidget, int mouseX, int mouseY) {
        if (hoveredWidget instanceof GlyphButton glyphButton
                && glyphButton.abstractSpellPart != null
                && glyphButton.isMouseOver(mouseX, mouseY)) {
            return glyphButton.abstractSpellPart;
        }
        if (hoveredWidget instanceof CraftingButton craftingButton
                && craftingButton.getAbstractSpellPart() != null
                && craftingButton.isMouseOver(mouseX, mouseY)) {
            return craftingButton.getAbstractSpellPart();
        }
        return null;
    }

    /**
     * When the search intent is "search by glyph name" (e.g. the user typed "harm"),
     * the normal search returns reasons keyed by the matching glyph, but not a
     * reason for the glyph <em>itself</em>. This helper adds a self-search reason
     * so the tooltip shows the glyph's best contextual label.
     *
     * <p>Does nothing when {@code searchIntent} is not {@link SearchIntentType#GLYPH}.
     */
    public static Map<ResourceLocation, List<MatchReason>> enrichWithSelfSearchReasons(
            Map<ResourceLocation, List<MatchReason>> reasonsByGlyph,
            SearchIntent searchIntent,
            List<AbstractSpellPart> displayedGlyphs
    ) {
        if (searchIntent == null
                || searchIntent.type() != SearchIntentType.GLYPH
                || searchIntent.resolvedId() == null) {
            return reasonsByGlyph;
        }

        ResourceLocation glyphId = searchIntent.resolvedId();
        boolean displayed = displayedGlyphs != null && displayedGlyphs.stream()
                .anyMatch(part -> part != null && glyphId.equals(part.getRegistryName()));
        if (!displayed) {
            return reasonsByGlyph;
        }

        Map<ResourceLocation, List<MatchReason>> safe =
                reasonsByGlyph == null ? Collections.emptyMap() : reasonsByGlyph;
        if (!safe.containsKey(glyphId) || safe.get(glyphId).isEmpty()) {
            Map<ResourceLocation, List<MatchReason>> mutable = new LinkedHashMap<>(safe);
            GlyphApplicationIndex.bestReasonForGlyph(glyphId)
                    .ifPresent(reason -> mutable.put(glyphId, List.of(reason)));
            return Map.copyOf(mutable);
        }
        return safe;
    }

    private static int confidenceRank(MatchReason reason) {
        return switch (reason.confidence()) {
            case HIGH -> 4;
            case MEDIUM -> 3;
            case LOW -> 2;
            case UNKNOWN -> 1;
        };
    }

    private static SearchIntent safeIntent(SearchIntent searchIntent) {
        return searchIntent == null
                ? new SearchIntent("", SearchIntentType.EMPTY, null, "")
                : searchIntent;
    }
}
