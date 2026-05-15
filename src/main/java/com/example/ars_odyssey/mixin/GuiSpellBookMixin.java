package com.example.ars_odyssey.mixin;
import net.minecraft.client.gui.Font;
import org.spongepowered.asm.mixin.injection.Redirect;
import com.example.ars_odyssey.client.tooltip.GlyphTooltipHelper;
import com.example.ars_odyssey.index.GlyphApplicationIndex;
import com.example.ars_odyssey.index.MatcherPresentation;
import com.example.ars_odyssey.index.MatchDisplayLabel;
import com.example.ars_odyssey.index.MatchReason;
import com.example.ars_odyssey.index.MatchReasonDisplayReducer;
import com.example.ars_odyssey.index.SearchIntent;
import com.example.ars_odyssey.index.SearchIntentResolver;
import com.example.ars_odyssey.index.SearchIntentType;
import com.example.ars_odyssey.index.SearchSuggestion;
import com.example.ars_odyssey.index.SearchSuggestionFilter;
import com.example.ars_odyssey.index.SearchSuggestionProvider;
import com.example.ars_odyssey.knowledge.EmptyPlayerKnowledgeView;
import com.example.ars_odyssey.knowledge.MatcherTargetResolver;
import com.example.ars_odyssey.knowledge.PlayerKnowledgeAttachments;
import com.example.ars_odyssey.knowledge.PlayerKnowledgeDataView;
import com.example.ars_odyssey.knowledge.PlayerKnowledgeView;
import com.example.ars_odyssey.knowledge.TargetDescriptor;
import com.example.ars_odyssey.knowledge.TargetKind;
import com.example.ars_odyssey.registry.ModRegistry;
import com.hollingsworth.arsnouveau.api.spell.AbstractSpellPart;
import com.hollingsworth.arsnouveau.client.gui.buttons.CraftingButton;
import com.hollingsworth.arsnouveau.client.gui.buttons.GlyphButton;
import com.hollingsworth.arsnouveau.client.gui.NoShadowTextField;
import com.hollingsworth.arsnouveau.client.gui.book.BaseBook;
import com.hollingsworth.arsnouveau.client.gui.book.GuiSpellBook;
import com.hollingsworth.arsnouveau.client.gui.utils.RenderUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.screens.inventory.PageButton;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

@Mixin(GuiSpellBook.class)
public abstract class GuiSpellBookMixin extends BaseBook {

    @Shadow public String previousString;
    @Shadow public NoShadowTextField searchBar;
    @Shadow public List<AbstractSpellPart> unlockedSpells;
    @Shadow public List<AbstractSpellPart> displayedGlyphs;
    @Shadow public int page;
    @Shadow public PageButton previousButton;
    @Shadow public ItemStack bookStack;
    @Shadow public Renderable hoveredWidget;
    @Shadow public List<CraftingButton> craftingCells;
    @Shadow public List<AbstractSpellPart> spell;
    @Shadow public PageButton nextGlyphButton;

    @Shadow public abstract void updateNextPageButtons();

    @Unique
    private Map<ResourceLocation, List<MatchReason>> ars_odyssey$reasonsByGlyph = Collections.emptyMap();
    @Unique
    private MatchReason ars_odyssey$headerReason = null;
    @Unique
    private Component ars_odyssey$currentConfidenceHeader = Component.empty();
    @Unique
    private SearchIntent ars_odyssey$searchIntent = new SearchIntent("", SearchIntentType.EMPTY, null, "");
    @Unique
    private SearchSuggestionFilter ars_odyssey$suggestionFilter = SearchSuggestionFilter.ALL;
    @Unique
    private List<SearchSuggestion> ars_odyssey$searchSuggestions = List.of();
    @Unique
    private boolean ars_odyssey$suggestionsVisible = false;
    @Unique
    private boolean ars_odyssey$filterDropdownVisible = false;
    @Unique
    private int ars_odyssey$draggedSpellIndex = -1;
    @Unique
    private int ars_odyssey$hoveredSpellIndex = -1;
    @Unique
    private boolean ars_odyssey$isDraggingSpellPart = false;
    @Unique
    private double ars_odyssey$dragStartX;
    @Unique
    private double ars_odyssey$dragStartY;
    @Unique
    private double ars_odyssey$dragCurrentX;
    @Unique
    private double ars_odyssey$dragCurrentY;
    @Unique
    private static final double ars_odyssey$DRAG_THRESHOLD_SQ = 9.0D;
    @Unique
    private static final double ars_odyssey$SNAP_CUTOFF_SQ = 900.0D;
    @Unique
    private static final int ars_odyssey$SUGGESTION_LIMIT = 10;
    @Unique
    private static final int ars_odyssey$FILTER_WIDTH = 38;
    @Unique
    private static final int ars_odyssey$FILTER_HEIGHT = 12;
    @Unique
    private static final int ars_odyssey$SUGGESTION_ROW_HEIGHT = 12;
    @Unique
    private static final int ars_odyssey$FILTER_OPTION_HEIGHT = 12;

    @Invoker("layoutAllGlyphs")
    protected abstract void ars_odyssey$layoutAllGlyphs(int page);

    @Invoker("validate")
    protected abstract void ars_odyssey$validate();

    @Invoker("updateNextGlyphArrow")
    protected abstract void ars_odyssey$updateNextGlyphArrow();

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (ars_odyssey$shouldHandleSpellDrag(button)) {
            int index = ars_odyssey$getSpellPartIndexAt(mouseX, mouseY);
            if (ars_odyssey$hasSpellPartAt(index)) {
                ars_odyssey$draggedSpellIndex = index;
                ars_odyssey$hoveredSpellIndex = index;
                ars_odyssey$isDraggingSpellPart = true;
                ars_odyssey$dragStartX = mouseX;
                ars_odyssey$dragStartY = mouseY;
                ars_odyssey$dragCurrentX = mouseX;
                ars_odyssey$dragCurrentY = mouseY;
                ars_odyssey$hideDraggedSlot(index);
                return true;
            }
        }

        if (ars_odyssey$handleSearchSuggestionClick(mouseX, mouseY, button)) {
            return true;
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (ars_odyssey$isDraggingSpellPart) {
            ars_odyssey$dragCurrentX = mouseX;
            ars_odyssey$dragCurrentY = mouseY;
            ars_odyssey$hoveredSpellIndex = ars_odyssey$getNearestSpellPartIndex(mouseX, mouseY, ars_odyssey$SNAP_CUTOFF_SQ);
            return true;
        }

        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (ars_odyssey$isDraggingSpellPart) {
            int sourceIndex = ars_odyssey$draggedSpellIndex;
            int clickedIndex = ars_odyssey$getSpellPartIndexAt(mouseX, mouseY);
            int targetIndex = ars_odyssey$getNearestSpellPartIndex(mouseX, mouseY, ars_odyssey$SNAP_CUTOFF_SQ);
            boolean shouldDelete = button == 0
                    && sourceIndex == clickedIndex
                    && !ars_odyssey$hasDraggedFarEnough(mouseX, mouseY);
            boolean recipeChanged = false;

            if (shouldDelete) {
                ars_odyssey$clearSpellPartAt(sourceIndex);
                recipeChanged = true;
            } else if (button == 0 && targetIndex >= 0 && targetIndex != sourceIndex) {
                ars_odyssey$moveSpellPart(sourceIndex, targetIndex);
                recipeChanged = true;
            }

            if (!recipeChanged) {
                ars_odyssey$validate();
            }

            ars_odyssey$clearDragState();
            return true;
        }

        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 256) {
            ars_odyssey$hideSearchSuggestions();
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void ars_odyssey$renderDraggedSpellPart(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks, CallbackInfo ci) {
        if (ars_odyssey$isOdysseyBook()) {
            ars_odyssey$renderSearchFilter(graphics, mouseX, mouseY);
            ars_odyssey$renderSearchSuggestions(graphics, mouseX, mouseY);
            ars_odyssey$renderSearchFilterDropdown(graphics, mouseX, mouseY);
        }

        if (!ars_odyssey$isDraggingSpellPart || !ars_odyssey$hasSpellPartAt(ars_odyssey$draggedSpellIndex)) {
            return;
        }

        AbstractSpellPart draggedPart = spell.get(ars_odyssey$draggedSpellIndex);
        RenderUtils.drawSpellPart(
                draggedPart,
                graphics,
                (int) ars_odyssey$dragCurrentX - 8,
                (int) ars_odyssey$dragCurrentY - 8,
                16,
                false,
                200
        );
    }
    @Inject(method = "init", at = @At("TAIL"))
    private void ars_odyssey$initOdysseyState(CallbackInfo ci) {
        if (ars_odyssey$reasonsByGlyph == null) {
            ars_odyssey$reasonsByGlyph = Collections.emptyMap();
        }
        ars_odyssey$headerReason = null;
        ars_odyssey$searchIntent = new SearchIntent("", SearchIntentType.EMPTY, null, "");
        ars_odyssey$suggestionFilter = SearchSuggestionFilter.ALL;
        ars_odyssey$searchSuggestions = List.of();
        ars_odyssey$suggestionsVisible = false;
        ars_odyssey$filterDropdownVisible = false;
        ars_odyssey$clearDragState();
    }

    @Unique
    private boolean ars_odyssey$isOdysseyBook() {
        return bookStack != null && bookStack.is(ModRegistry.ODYSSEY_SPELL_BOOK.get());
    }

    @Unique
    private boolean ars_odyssey$shouldHandleSpellDrag(int button) {
        return button == 0
                && ars_odyssey$isOdysseyBook()
                && craftingCells != null
                && spell != null;
    }

    @Unique
    private boolean ars_odyssey$handleSearchSuggestionClick(double mouseX, double mouseY, int button) {
        if (button != 0 || !ars_odyssey$isOdysseyBook() || searchBar == null) {
            return false;
        }

        if (ars_odyssey$isInside(mouseX, mouseY,
                ars_odyssey$filterX(), ars_odyssey$filterY(),
                ars_odyssey$FILTER_WIDTH, ars_odyssey$FILTER_HEIGHT)) {
            ars_odyssey$filterDropdownVisible = !ars_odyssey$filterDropdownVisible;
            return true;
        }

        int filterOptionIndex = ars_odyssey$filterOptionIndexAt(mouseX, mouseY);
        if (filterOptionIndex >= 0) {
            ars_odyssey$suggestionFilter = SearchSuggestionFilter.values()[filterOptionIndex];
            ars_odyssey$filterDropdownVisible = false;
            ars_odyssey$refreshSearchSuggestions();
            return true;
        }

        int suggestionIndex = ars_odyssey$suggestionIndexAt(mouseX, mouseY);
        if (suggestionIndex >= 0 && suggestionIndex < ars_odyssey$searchSuggestions.size()) {
            SearchSuggestion suggestion = ars_odyssey$searchSuggestions.get(suggestionIndex);
            searchBar.setValue(suggestion.queryText());
            ars_odyssey$hideSearchSuggestions();
            ars_odyssey$filterDropdownVisible = false;
            return true;
        }

        ars_odyssey$filterDropdownVisible = false;
        return false;
    }

    @Unique
    private void ars_odyssey$refreshSearchSuggestions() {
        if (searchBar == null) {
            ars_odyssey$searchSuggestions = List.of();
            ars_odyssey$suggestionsVisible = false;
            return;
        }
        ars_odyssey$searchSuggestions = SearchSuggestionProvider.suggest(
                searchBar.getValue(),
                ars_odyssey$suggestionFilter,
                ars_odyssey$SUGGESTION_LIMIT);
        ars_odyssey$suggestionsVisible = !searchBar.getValue().trim().isEmpty()
                && !ars_odyssey$searchSuggestions.isEmpty();
    }

    @Unique
    private void ars_odyssey$hideSearchSuggestions() {
        ars_odyssey$searchSuggestions = List.of();
        ars_odyssey$suggestionsVisible = false;
        ars_odyssey$filterDropdownVisible = false;
    }

    @Unique
    private void ars_odyssey$renderSearchFilter(GuiGraphics graphics, int mouseX, int mouseY) {
        if (searchBar == null) {
            return;
        }

        int x = ars_odyssey$filterX();
        int y = ars_odyssey$filterY();
        boolean hovered = ars_odyssey$isInside(mouseX, mouseY, x, y, ars_odyssey$FILTER_WIDTH, ars_odyssey$FILTER_HEIGHT);
        ars_odyssey$drawPanel(graphics, x, y, ars_odyssey$FILTER_WIDTH, ars_odyssey$FILTER_HEIGHT,
                hovered || ars_odyssey$filterDropdownVisible ? 0xF0F3E2BC : 0xE8EBD8AD);
        Component text = Component.translatable(ars_odyssey$suggestionFilter.translationKey());
        graphics.drawString(font, text, x + 3, y + 2, 0xFF6A6255, false);
        graphics.drawString(font, Component.literal("v"), x + ars_odyssey$FILTER_WIDTH - 8, y + 2, 0xFF6A6255, false);
    }

    @Unique
    private void ars_odyssey$renderSearchFilterDropdown(GuiGraphics graphics, int mouseX, int mouseY) {
        if (!ars_odyssey$filterDropdownVisible || searchBar == null) {
            return;
        }

        int x = ars_odyssey$filterX();
        int y = ars_odyssey$filterY() + ars_odyssey$FILTER_HEIGHT - 1;
        int width = ars_odyssey$FILTER_WIDTH;
        SearchSuggestionFilter[] filters = SearchSuggestionFilter.values();
        int height = filters.length * ars_odyssey$FILTER_OPTION_HEIGHT;
        ars_odyssey$drawPanel(graphics, x, y, width, height, 0xF4EBD8AD);

        for (int i = 0; i < filters.length; i++) {
            int rowY = y + i * ars_odyssey$FILTER_OPTION_HEIGHT;
            boolean hovered = ars_odyssey$isInside(mouseX, mouseY, x, rowY, width, ars_odyssey$FILTER_OPTION_HEIGHT);
            if (hovered || filters[i] == ars_odyssey$suggestionFilter) {
                graphics.fill(x + 1, rowY + 1, x + width - 1, rowY + ars_odyssey$FILTER_OPTION_HEIGHT - 1,
                        hovered ? 0xFFF6E8C1 : 0xFFE6D4A7);
            }

            Component text = Component.translatable(filters[i].translationKey());
            graphics.drawString(font, text, x + 3, rowY + 2, 0xFF6A6255, false);
        }
    }

    @Unique
    private void ars_odyssey$renderSearchSuggestions(GuiGraphics graphics, int mouseX, int mouseY) {
        if (!ars_odyssey$suggestionsVisible || searchBar == null || ars_odyssey$searchSuggestions.isEmpty() || searchBar.getValue().trim().isEmpty()) {
            return;
        }

        int x = ars_odyssey$suggestionX();
        int y = ars_odyssey$suggestionY();
        int width = ars_odyssey$suggestionWidth();
        int height = ars_odyssey$searchSuggestions.size() * ars_odyssey$SUGGESTION_ROW_HEIGHT;
        ars_odyssey$drawPanel(graphics, x, y, width, height, 0xF0E4D3AA);

        for (int i = 0; i < ars_odyssey$searchSuggestions.size(); i++) {
            SearchSuggestion suggestion = ars_odyssey$searchSuggestions.get(i);
            int rowY = y + i * ars_odyssey$SUGGESTION_ROW_HEIGHT;
            if (ars_odyssey$isInside(mouseX, mouseY, x, rowY, width, ars_odyssey$SUGGESTION_ROW_HEIGHT)) {
                graphics.fill(x + 1, rowY + 1, x + width - 1, rowY + ars_odyssey$SUGGESTION_ROW_HEIGHT - 1, 0xFFEADCB7);
            }

            String text = suggestion.displayName().isBlank()
                    ? suggestion.queryText()
                    : suggestion.displayName() + " - " + suggestion.queryText();
            graphics.drawString(font, font.plainSubstrByWidth(text, width - 6), x + 3, rowY + 2, -8355712, false);
        }
    }

    @Unique
    private void ars_odyssey$drawPanel(GuiGraphics graphics, int x, int y, int width, int height, int fillColor) {
        graphics.fill(x, y, x + width, y + height, fillColor);
        graphics.fill(x, y, x + width, y + 1, 0xFFB79E72);
        graphics.fill(x, y + height - 1, x + width, y + height, 0xFF8D744E);
        graphics.fill(x, y, x + 1, y + height, 0xFFB79E72);
        graphics.fill(x + width - 1, y, x + width, y + height, 0xFF8D744E);
    }

    @Unique
    private int ars_odyssey$suggestionIndexAt(double mouseX, double mouseY) {
        if (!ars_odyssey$suggestionsVisible || ars_odyssey$searchSuggestions.isEmpty() || searchBar == null || searchBar.getValue().trim().isEmpty()) {
            return -1;
        }

        int x = ars_odyssey$suggestionX();
        int y = ars_odyssey$suggestionY();
        int width = ars_odyssey$suggestionWidth();
        int height = ars_odyssey$searchSuggestions.size() * ars_odyssey$SUGGESTION_ROW_HEIGHT;
        if (!ars_odyssey$isInside(mouseX, mouseY, x, y, width, height)) {
            return -1;
        }
        return (int) ((mouseY - y) / ars_odyssey$SUGGESTION_ROW_HEIGHT);
    }

    @Unique
    private boolean ars_odyssey$isInside(double mouseX, double mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    @Unique
    private int ars_odyssey$filterX() {
        return searchBar.getX() - ars_odyssey$FILTER_WIDTH;
    }

    @Unique
    private int ars_odyssey$filterY() {
        return searchBar.getY();
    }

    @Unique
    private int ars_odyssey$suggestionX() {
        int right = searchBar.getX() + searchBar.getWidth();
        return Math.max(bookLeft + 148, right - ars_odyssey$suggestionWidth());
    }

    @Unique
    private int ars_odyssey$suggestionY() {
        return searchBar.getY() + searchBar.getHeight() + 2;
    }

    @Unique
    private int ars_odyssey$filterOptionIndexAt(double mouseX, double mouseY) {
        if (!ars_odyssey$filterDropdownVisible || searchBar == null) {
            return -1;
        }

        int x = ars_odyssey$filterX();
        int y = ars_odyssey$filterY() + ars_odyssey$FILTER_HEIGHT - 1;
        int height = SearchSuggestionFilter.values().length * ars_odyssey$FILTER_OPTION_HEIGHT;
        if (!ars_odyssey$isInside(mouseX, mouseY, x, y, ars_odyssey$FILTER_WIDTH, height)) {
            return -1;
        }
        return (int) ((mouseY - y) / ars_odyssey$FILTER_OPTION_HEIGHT);
    }

    @Unique
    private int ars_odyssey$suggestionWidth() {
        return 132;
    }

    @Unique
    private int ars_odyssey$getSpellPartIndexAt(double mouseX, double mouseY) {
        if (craftingCells == null) {
            return -1;
        }

        for (CraftingButton craftingButton : craftingCells) {
            if (craftingButton.visible && craftingButton.isMouseOver(mouseX, mouseY)) {
                return craftingButton.slotNum;
            }
        }
        return -1;
    }

    @Unique
    private int ars_odyssey$getNearestSpellPartIndex(double mouseX, double mouseY, double cutoffDistanceSq) {
        if (craftingCells == null) {
            return -1;
        }

        int nearestIndex = -1;
        double nearestDistanceSq = cutoffDistanceSq;

        for (CraftingButton craftingButton : craftingCells) {
            if (!craftingButton.visible) {
                continue;
            }

            double centerX = craftingButton.getX() + craftingButton.getWidth() / 2.0D;
            double centerY = craftingButton.getY() + craftingButton.getHeight() / 2.0D;
            double dx = mouseX - centerX;
            double dy = mouseY - centerY;
            double distanceSq = dx * dx + dy * dy;
            if (distanceSq <= nearestDistanceSq) {
                nearestDistanceSq = distanceSq;
                nearestIndex = craftingButton.slotNum;
            }
        }

        return nearestIndex;
    }

    @Unique
    private boolean ars_odyssey$hasSpellPartAt(int index) {
        return index >= 0
                && spell != null
                && index < spell.size()
                && spell.get(index) != null;
    }

    @Unique
    private void ars_odyssey$hideDraggedSlot(int index) {
        if (craftingCells == null) {
            return;
        }

        for (CraftingButton craftingButton : craftingCells) {
            if (craftingButton.slotNum == index) {
                craftingButton.setAbstractSpellPart(null);
                return;
            }
        }
    }

    @Unique
    private void ars_odyssey$clearSpellPartAt(int index) {
        if (!ars_odyssey$hasSpellPartAt(index)) {
            return;
        }

        spell.set(index, null);
        if (spell.stream().allMatch(part -> part == null)) {
            spell.clear();
        }
        ars_odyssey$refreshSpellRecipe();
    }

    @Unique
    private void ars_odyssey$moveSpellPart(int sourceIndex, int targetIndex) {
        if (!ars_odyssey$hasSpellPartAt(sourceIndex)) {
            return;
        }

        AbstractSpellPart movedPart = spell.remove(sourceIndex);
        int insertionIndex;
        if (targetIndex >= spell.size()) {
            insertionIndex = spell.size();
        } else if (targetIndex > sourceIndex) {
            insertionIndex = targetIndex;
        } else {
            insertionIndex = Math.min(targetIndex + 1, spell.size());
        }

        spell.add(insertionIndex, movedPart);
        ars_odyssey$refreshSpellRecipe();
    }

    @Unique
    private void ars_odyssey$refreshSpellRecipe() {
        if (nextGlyphButton != null) {
            ars_odyssey$updateNextGlyphArrow();
        }
        ars_odyssey$validate();
    }

    @Unique
    private boolean ars_odyssey$hasDraggedFarEnough(double mouseX, double mouseY) {
        double dx = mouseX - ars_odyssey$dragStartX;
        double dy = mouseY - ars_odyssey$dragStartY;
        return dx * dx + dy * dy > ars_odyssey$DRAG_THRESHOLD_SQ;
    }

    @Unique
    private void ars_odyssey$clearDragState() {
        ars_odyssey$draggedSpellIndex = -1;
        ars_odyssey$hoveredSpellIndex = -1;
        ars_odyssey$isDraggingSpellPart = false;
        ars_odyssey$dragStartX = 0.0D;
        ars_odyssey$dragStartY = 0.0D;
        ars_odyssey$dragCurrentX = 0.0D;
        ars_odyssey$dragCurrentY = 0.0D;
    }

    @Inject(method = "onSearchChanged", at = @At("HEAD"), cancellable = true)
    private void ars_odyssey$customSearch(String str, CallbackInfo ci) {
        // Only modify the search behavior for Odyssey Spell Book.
        // Other Ars Nouveau spell books keep their original behavior.
        if (!bookStack.is(ModRegistry.ODYSSEY_SPELL_BOOK.get())) {
            return;
        }

        if (str.equals(previousString)) {
            ci.cancel();
            return;
        }

        previousString = str;
        String query = str.toLowerCase(Locale.ROOT).trim();

        if (!query.isEmpty()) {
            searchBar.setSuggestion("");
        } else {
            searchBar.setSuggestion(Component.translatable("ars_nouveau.spell_book_gui.search").getString());
        }
        ars_odyssey$refreshSearchSuggestions();
        GlyphApplicationIndex.SearchResult result = GlyphApplicationIndex.searchWithReasons(query, unlockedSpells);
        displayedGlyphs = result.glyphs();
        ars_odyssey$searchIntent = SearchIntentResolver.resolve(query);
        ars_odyssey$reasonsByGlyph = result.reasonsByGlyph() == null
                ? Collections.emptyMap()
                : ars_odyssey$withGlyphSearchSummaryReasons(result.reasonsByGlyph());

        ars_odyssey$headerReason = ars_odyssey$computeHeaderReason();
        ars_odyssey$currentConfidenceHeader = ars_odyssey$computeConfidenceHeader();




        updateNextPageButtons();


        page = 0;
        previousButton.active = false;
        previousButton.visible = false;

        ars_odyssey$layoutAllGlyphs(page);
        ars_odyssey$validate();

        ci.cancel();
    }

    @Inject(method = "collectTooltips", at = @At("TAIL"))
    private void ars_odyssey$appendMatchReasons(GuiGraphics stack, int mouseX, int mouseY, List<Component> tooltip, CallbackInfo ci) {
        if (!bookStack.is(ModRegistry.ODYSSEY_SPELL_BOOK.get())) {
            return;
        }
        AbstractSpellPart hoveredPart = ars_odyssey$hoveredSpellPart(mouseX, mouseY);
        if (hoveredPart == null || hoveredPart.getRegistryName() == null) {
            return;
        }
        /*
        // 只对效果类魔符显示，Form / Augment 不显示
        if (!(glyphButton.abstractSpellPart instanceof AbstractEffect)) {
        */
        if (!(hoveredWidget instanceof GlyphButton)) {
            ars_odyssey$appendGlyphSummaryTooltip(tooltip, hoveredPart);
            return;
        }

        ResourceLocation glyphId = hoveredPart.getRegistryName();
        Map<ResourceLocation, List<MatchReason>> reasonsByGlyph =
                ars_odyssey$reasonsByGlyph == null ? Collections.emptyMap() : ars_odyssey$reasonsByGlyph;
        List<MatchReason> reasons = MatchReasonDisplayReducer.reduce(
                reasonsByGlyph.getOrDefault(glyphId, List.of()));

        boolean isTargetSearch = !reasons.isEmpty() && !ars_odyssey$isCurrentGlyphIntent(glyphId);

        if (isTargetSearch) {
            // 按目标搜索（如搜 zombie）：显示该魔符对该目标的匹配原因
            tooltip.add(Component.empty());
            tooltip.add(Component.translatable("ars_odyssey.tooltip.header").withStyle(ChatFormatting.DARK_AQUA));
            int shown = 0;
            for (MatchReason reason : reasons) {
                if (shown >= 3) {
                    break;
                }
                tooltip.add(ars_odyssey$reasonLine(reason));
                tooltip.add(Component.translatable("ars_odyssey.tooltip.evidence", ars_odyssey$sourceText(reason))
                        .withStyle(ChatFormatting.GRAY));
                shown++;
            }
            // 目标特化效果类型行（ENTITY 搜索时追加）
            if (ars_odyssey$searchIntent.type() == SearchIntentType.ENTITY
                    && ars_odyssey$searchIntent.resolvedId() != null) {
                GlyphTooltipHelper.appendEntityTargetCategoryLine(
                        tooltip, glyphId, ars_odyssey$searchIntent.resolvedId());
            }
        } else {
            ars_odyssey$appendGlyphSummaryTooltip(tooltip, hoveredPart);
            // 无搜索或按魔符名搜索：显示该魔符的总摘要（解析个数 or 系统推断）
        }
    }

    @Unique
    private AbstractSpellPart ars_odyssey$hoveredSpellPart(int mouseX, int mouseY) {
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

    @Unique
    private void ars_odyssey$appendGlyphSummaryTooltip(List<Component> tooltip, AbstractSpellPart part) {
        if (part == null || part.getRegistryName() == null) {
            return;
        }
        GlyphTooltipHelper.appendOdysseyTooltip(part.getRegistryName(), tooltip);
    }

    @Unique
    private Component ars_odyssey$reasonLine(MatchReason reason) {
        Component target = ars_odyssey$targetText(reason);
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
                .append(ars_odyssey$confidenceText(reason))
                .append(Component.literal("] "))
                .append(body);
    }
    @Unique
    private Optional<Component> ars_odyssey$glyphSearchSummaryLine(ResourceLocation glyphId, List<MatchReason> reasons) {
        PlayerKnowledgeView knowledgeView = ars_odyssey$getKnowledgeView();
        int resolvedCount = knowledgeView.countResolvedRelations(glyphId);
        if (resolvedCount > 0) {
            MatchDisplayLabel label = MatchDisplayLabel.resolved();
            return Optional.of(Component.empty()
                    .append(Component.literal("["))
                    .append(label.text().copy().withColor(label.color()))
                    .append(Component.literal("] "))
                    .append(Component.translatable("ars_odyssey.tooltip.resolved_count", resolvedCount)));
        }

        if (reasons.isEmpty()) {
            return Optional.empty();
        }

        MatchDisplayLabel label = MatchDisplayLabel.confidence(reasons.getFirst().confidence());
        return Optional.of(Component.empty()
                .append(Component.literal("["))
                .append(label.text().copy().withColor(label.color()))
                .append(Component.literal("] "))
                .append(Component.literal("系统提示")));
    }
    @Unique
    private MatchReason ars_odyssey$computeHeaderReason() {
        if (searchBar == null || searchBar.getValue().trim().isEmpty()) {
            return null;
        }

        Map<ResourceLocation, List<MatchReason>> reasonsByGlyph =
                ars_odyssey$reasonsByGlyph == null ? Collections.emptyMap() : ars_odyssey$reasonsByGlyph;

        MatchReason best = null;

        for (AbstractSpellPart part : displayedGlyphs) {
            if (part == null || part.getRegistryName() == null) {
                continue;
            }

            List<MatchReason> reasons = reasonsByGlyph.getOrDefault(part.getRegistryName(), List.of());
            for (MatchReason reason : reasons) {
                if (best == null || ars_odyssey$confidenceRank(reason) > ars_odyssey$confidenceRank(best)) {
                    best = reason;
                }
            }
        }

        return best;
    }
    @Unique
    private int ars_odyssey$confidenceColor(MatchReason reason) {
        return ars_odyssey$displayLabel(reason).color();
    }
    @Unique
    private int ars_odyssey$confidenceRank(MatchReason reason) {
        return switch (reason.confidence()) {
            case HIGH -> 4;
            case MEDIUM -> 3;
            case LOW -> 2;
            case UNKNOWN -> 1;
        };
    }
    @Unique
    private Component ars_odyssey$computeConfidenceHeader() {
        if (searchBar == null || searchBar.getValue().trim().isEmpty()) {
            return Component.empty();
        }

        boolean hasHigh = false;
        boolean hasMedium = false;
        boolean hasLow = false;
        boolean hasUnknown = false;

        for (List<MatchReason> reasons : ars_odyssey$reasonsByGlyph.values()) {
            for (MatchReason reason : reasons) {
                switch (reason.confidence()) {
                    case HIGH -> hasHigh = true;
                    case MEDIUM -> hasMedium = true;
                    case LOW -> hasLow = true;
                    case UNKNOWN -> hasUnknown = true;
                }
            }
        }

        if (hasHigh) {
            return Component.translatable("ars_odyssey.tooltip.confidence.high")
                    .withStyle(ChatFormatting.DARK_PURPLE);
        }
        if (hasMedium) {
            return Component.translatable("ars_odyssey.tooltip.confidence.medium")
                    .withStyle(ChatFormatting.BLUE);
        }
        if (hasLow) {
            return Component.translatable("ars_odyssey.tooltip.confidence.low")
                    .withStyle(ChatFormatting.GREEN);
        }
        if (hasUnknown) {
            return Component.translatable("ars_odyssey.tooltip.confidence.unknown")
                    .withStyle(ChatFormatting.BLACK);
        }

        return Component.empty();
    }
    @Unique
    private Component ars_odyssey$confidenceText(MatchReason reason) {
        MatchDisplayLabel label = ars_odyssey$displayLabel(reason);
        return label.text().copy().withColor(label.color());
    }
    @Unique
    private MatchDisplayLabel ars_odyssey$displayLabel(MatchReason reason) {
        PlayerKnowledgeView knowledgeView = ars_odyssey$getKnowledgeView();
        if (ars_odyssey$searchIntent.type() == SearchIntentType.GLYPH
                && reason.glyphId().equals(ars_odyssey$searchIntent.resolvedId())
                && (knowledgeView.countResolvedRelations(reason.glyphId()) > 0
                || knowledgeView.firstAbstractResolvedTarget(reason.glyphId()).isPresent())) {
            return MatchDisplayLabel.resolved();
        }

        Optional<TargetDescriptor> searchTarget = ars_odyssey$currentSearchTarget();
        if (searchTarget.isPresent() && knowledgeView.isResolved(reason.glyphId(), searchTarget.get())) {
            return MatchDisplayLabel.resolved();
        }

        Optional<TargetDescriptor> target = MatcherTargetResolver.fromMatcher(reason.matcher());
        if (target.isPresent() && knowledgeView.isResolved(reason.glyphId(), target.get())) {
            return MatchDisplayLabel.resolved();
        }
        return MatchDisplayLabel.confidence(reason.confidence());
    }
    @Unique
    private boolean ars_odyssey$isCurrentGlyphIntent(ResourceLocation glyphId) {
        return ars_odyssey$searchIntent.type() == SearchIntentType.GLYPH
                && glyphId != null
                && glyphId.equals(ars_odyssey$searchIntent.resolvedId());
    }
    @Unique
    private PlayerKnowledgeView ars_odyssey$getKnowledgeView() {
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
    @Unique
    private Map<ResourceLocation, List<MatchReason>> ars_odyssey$withGlyphSearchSummaryReasons(Map<ResourceLocation, List<MatchReason>> reasonsByGlyph) {
        if (ars_odyssey$searchIntent.type() != SearchIntentType.GLYPH || ars_odyssey$searchIntent.resolvedId() == null) {
            return reasonsByGlyph;
        }

        ResourceLocation glyphId = ars_odyssey$searchIntent.resolvedId();
        boolean displayed = displayedGlyphs.stream()
                .anyMatch(part -> part != null && glyphId.equals(part.getRegistryName()));
        if (!displayed) {
            return reasonsByGlyph;
        }

        Map<ResourceLocation, List<MatchReason>> mutable = new LinkedHashMap<>(reasonsByGlyph);
        if (!mutable.containsKey(glyphId) || mutable.get(glyphId).isEmpty()) {
            GlyphApplicationIndex.bestReasonForGlyph(glyphId)
                    .ifPresent(reason -> mutable.put(glyphId, List.of(reason)));
        }
        return Map.copyOf(mutable);
    }
    @Unique
    private Optional<TargetDescriptor> ars_odyssey$currentSearchTarget() {
        if (ars_odyssey$searchIntent.resolvedId() == null) {
            return Optional.empty();
        }

        return switch (ars_odyssey$searchIntent.type()) {
            case ENTITY -> Optional.of(TargetDescriptor.entityType(ars_odyssey$searchIntent.resolvedId()));
            case BLOCK -> Optional.of(TargetDescriptor.block(ars_odyssey$searchIntent.resolvedId()));
            case ITEM -> Optional.of(TargetDescriptor.item(ars_odyssey$searchIntent.resolvedId()));
            case TAG -> Optional.of(new TargetDescriptor(TargetKind.UNKNOWN, ars_odyssey$searchIntent.resolvedId(), "tag"));
            default -> Optional.empty();
        };
    }
    @Unique
    private Component ars_odyssey$targetText(MatchReason reason) {
        String key = reason.displayKey();
        if (!key.isBlank() && Language.getInstance().has(key)) {
            return Component.translatable(key);
        }
        return Component.literal(MatcherPresentation.rawValueForDisplay(reason.matcher().value()));
    }

    @Unique
    private Component ars_odyssey$targetDescriptorText(TargetDescriptor target) {
        if (target.kind() == TargetKind.BEHAVIOR) {
            String detail = target.detail();
            if (detail.startsWith("entity_class:")) {
                return Component.translatable("ars_odyssey.target.entity_class." + ars_odyssey$simpleName(detail.substring("entity_class:".length())));
            }
            if (detail.startsWith("block_class:")) {
                return Component.translatable("ars_odyssey.target.block_class." + ars_odyssey$simpleName(detail.substring("block_class:".length())));
            }
            if (detail.equals("general_entity")) {
                return Component.translatable("ars_odyssey.target.entity_class.Entity");
            }
            if (detail.equals("general_block")) {
                return Component.translatable("ars_odyssey.target.general_block");
            }
            return Component.translatable("ars_odyssey.target.behavior." + detail);
        }
        if (target.id() != null) {
            return Component.literal(target.id().toString());
        }
        return Component.literal(target.detail());
    }

    @Unique
    private String ars_odyssey$simpleName(String value) {
        int lastDot = value == null ? -1 : value.lastIndexOf('.');
        return lastDot >= 0 ? value.substring(lastDot + 1) : value;
    }

    @Unique
    private Component ars_odyssey$sourceText(MatchReason reason) {
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
    @Redirect(
            method = "drawBackgroundElements",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/GuiGraphics;drawString(Lnet/minecraft/client/gui/Font;Ljava/lang/String;III Z)I",
                    ordinal = 1
            )
    )
    private int ars_odyssey$replaceEffectHeader(
            GuiGraphics graphics,
            Font font,
            String text,
            int x,
            int y,
            int color,
            boolean dropShadow
    ) {
        if (!bookStack.is(ModRegistry.ODYSSEY_SPELL_BOOK.get())) {
            return graphics.drawString(font, text, x, y, color, dropShadow);
        }

        if (ars_odyssey$headerReason == null) {
            return graphics.drawString(font, text, x, y, color, dropShadow);
        }

        MatchDisplayLabel label = ars_odyssey$displayLabel(ars_odyssey$headerReason);
        return graphics.drawString(
                font,
                label.text().getString(),
                x,
                y,
                label.color(),
                dropShadow
        );
    }
}
