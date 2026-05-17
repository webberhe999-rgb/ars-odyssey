package com.swvague.ars_odyssey.client.gui.spellbook;

import com.swvague.ars_odyssey.index.SearchSuggestion;
import com.swvague.ars_odyssey.index.SearchSuggestionFilter;
import com.swvague.ars_odyssey.index.SearchSuggestionProvider;
import com.hollingsworth.arsnouveau.client.gui.NoShadowTextField;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.List;

@OnlyIn(Dist.CLIENT)
public final class SpellBookSearchUi {
    private static final int SUGGESTION_LIMIT = 10;
    private static final int FILTER_WIDTH = 38;
    private static final int FILTER_HEIGHT = 12;
    private static final int SUGGESTION_ROW_HEIGHT = 12;
    private static final int FILTER_OPTION_HEIGHT = 12;

    private SearchSuggestionFilter suggestionFilter = SearchSuggestionFilter.ALL;
    private List<SearchSuggestion> searchSuggestions = List.of();
    private boolean suggestionsVisible = false;
    private boolean filterDropdownVisible = false;

    public void reset() {
        suggestionFilter = SearchSuggestionFilter.ALL;
        searchSuggestions = List.of();
        suggestionsVisible = false;
        filterDropdownVisible = false;
    }

    public boolean handleClick(double mouseX, double mouseY, int button, NoShadowTextField searchBar, int bookLeft) {
        if (button != 0 || searchBar == null) {
            return false;
        }

        if (isInside(mouseX, mouseY, filterX(searchBar), filterY(searchBar), FILTER_WIDTH, FILTER_HEIGHT)) {
            filterDropdownVisible = !filterDropdownVisible;
            return true;
        }

        int filterOptionIndex = filterOptionIndexAt(mouseX, mouseY, searchBar);
        if (filterOptionIndex >= 0) {
            suggestionFilter = SearchSuggestionFilter.values()[filterOptionIndex];
            filterDropdownVisible = false;
            refresh(searchBar);
            return true;
        }

        int suggestionIndex = suggestionIndexAt(mouseX, mouseY, searchBar, bookLeft);
        if (suggestionIndex >= 0 && suggestionIndex < searchSuggestions.size()) {
            SearchSuggestion suggestion = searchSuggestions.get(suggestionIndex);
            searchBar.setValue(suggestion.queryText());
            hide();
            filterDropdownVisible = false;
            return true;
        }

        filterDropdownVisible = false;
        return false;
    }

    public void refresh(NoShadowTextField searchBar) {
        if (searchBar == null) {
            searchSuggestions = List.of();
            suggestionsVisible = false;
            return;
        }
        searchSuggestions = SearchSuggestionProvider.suggest(
                searchBar.getValue(),
                suggestionFilter,
                SUGGESTION_LIMIT);
        suggestionsVisible = !searchBar.getValue().trim().isEmpty()
                && !searchSuggestions.isEmpty();
    }

    public void hide() {
        searchSuggestions = List.of();
        suggestionsVisible = false;
        filterDropdownVisible = false;
    }

    public void render(GuiGraphics graphics, Font font, NoShadowTextField searchBar, int bookLeft, int mouseX, int mouseY) {
        if (searchBar == null) {
            return;
        }

        renderFilter(graphics, font, searchBar, mouseX, mouseY);
        renderSuggestions(graphics, font, searchBar, bookLeft, mouseX, mouseY);
        renderFilterDropdown(graphics, font, searchBar, mouseX, mouseY);
    }

    private void renderFilter(GuiGraphics graphics, Font font, NoShadowTextField searchBar, int mouseX, int mouseY) {
        int x = filterX(searchBar);
        int y = filterY(searchBar);
        boolean hovered = isInside(mouseX, mouseY, x, y, FILTER_WIDTH, FILTER_HEIGHT);
        drawPanel(graphics, x, y, FILTER_WIDTH, FILTER_HEIGHT,
                hovered || filterDropdownVisible ? 0xF0F3E2BC : 0xE8EBD8AD);
        Component text = Component.translatable(suggestionFilter.translationKey());
        graphics.drawString(font, text, x + 3, y + 2, 0xFF6A6255, false);
        graphics.drawString(font, Component.literal("v"), x + FILTER_WIDTH - 8, y + 2, 0xFF6A6255, false);
    }

    private void renderFilterDropdown(GuiGraphics graphics, Font font, NoShadowTextField searchBar, int mouseX, int mouseY) {
        if (!filterDropdownVisible) {
            return;
        }

        int x = filterX(searchBar);
        int y = filterY(searchBar) + FILTER_HEIGHT - 1;
        int width = FILTER_WIDTH;
        SearchSuggestionFilter[] filters = SearchSuggestionFilter.values();
        int height = filters.length * FILTER_OPTION_HEIGHT;
        drawPanel(graphics, x, y, width, height, 0xF4EBD8AD);

        for (int i = 0; i < filters.length; i++) {
            int rowY = y + i * FILTER_OPTION_HEIGHT;
            boolean hovered = isInside(mouseX, mouseY, x, rowY, width, FILTER_OPTION_HEIGHT);
            if (hovered || filters[i] == suggestionFilter) {
                graphics.fill(x + 1, rowY + 1, x + width - 1, rowY + FILTER_OPTION_HEIGHT - 1,
                        hovered ? 0xFFF6E8C1 : 0xFFE6D4A7);
            }

            Component text = Component.translatable(filters[i].translationKey());
            graphics.drawString(font, text, x + 3, rowY + 2, 0xFF6A6255, false);
        }
    }

    private void renderSuggestions(GuiGraphics graphics, Font font, NoShadowTextField searchBar, int bookLeft, int mouseX, int mouseY) {
        if (!suggestionsVisible || searchSuggestions.isEmpty() || searchBar.getValue().trim().isEmpty()) {
            return;
        }

        int x = suggestionX(searchBar, bookLeft);
        int y = suggestionY(searchBar);
        int width = suggestionWidth();
        int height = searchSuggestions.size() * SUGGESTION_ROW_HEIGHT;
        drawPanel(graphics, x, y, width, height, 0xF0E4D3AA);

        for (int i = 0; i < searchSuggestions.size(); i++) {
            SearchSuggestion suggestion = searchSuggestions.get(i);
            int rowY = y + i * SUGGESTION_ROW_HEIGHT;
            if (isInside(mouseX, mouseY, x, rowY, width, SUGGESTION_ROW_HEIGHT)) {
                graphics.fill(x + 1, rowY + 1, x + width - 1, rowY + SUGGESTION_ROW_HEIGHT - 1, 0xFFEADCB7);
            }

            String text = suggestion.displayName().isBlank()
                    ? suggestion.queryText()
                    : suggestion.displayName() + " - " + suggestion.queryText();
            graphics.drawString(font, font.plainSubstrByWidth(text, width - 6), x + 3, rowY + 2, -8355712, false);
        }
    }

    private static void drawPanel(GuiGraphics graphics, int x, int y, int width, int height, int fillColor) {
        graphics.fill(x, y, x + width, y + height, fillColor);
        graphics.fill(x, y, x + width, y + 1, 0xFFB79E72);
        graphics.fill(x, y + height - 1, x + width, y + height, 0xFF8D744E);
        graphics.fill(x, y, x + 1, y + height, 0xFFB79E72);
        graphics.fill(x + width - 1, y, x + width, y + height, 0xFF8D744E);
    }

    private int suggestionIndexAt(double mouseX, double mouseY, NoShadowTextField searchBar, int bookLeft) {
        if (!suggestionsVisible || searchSuggestions.isEmpty() || searchBar.getValue().trim().isEmpty()) {
            return -1;
        }

        int x = suggestionX(searchBar, bookLeft);
        int y = suggestionY(searchBar);
        int width = suggestionWidth();
        int height = searchSuggestions.size() * SUGGESTION_ROW_HEIGHT;
        if (!isInside(mouseX, mouseY, x, y, width, height)) {
            return -1;
        }
        return (int) ((mouseY - y) / SUGGESTION_ROW_HEIGHT);
    }

    private int filterOptionIndexAt(double mouseX, double mouseY, NoShadowTextField searchBar) {
        if (!filterDropdownVisible) {
            return -1;
        }

        int x = filterX(searchBar);
        int y = filterY(searchBar) + FILTER_HEIGHT - 1;
        int height = SearchSuggestionFilter.values().length * FILTER_OPTION_HEIGHT;
        if (!isInside(mouseX, mouseY, x, y, FILTER_WIDTH, height)) {
            return -1;
        }
        return (int) ((mouseY - y) / FILTER_OPTION_HEIGHT);
    }

    private static boolean isInside(double mouseX, double mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    private static int filterX(NoShadowTextField searchBar) {
        return searchBar.getX() - FILTER_WIDTH;
    }

    private static int filterY(NoShadowTextField searchBar) {
        return searchBar.getY();
    }

    private static int suggestionX(NoShadowTextField searchBar, int bookLeft) {
        int right = searchBar.getX() + searchBar.getWidth();
        return Math.max(bookLeft + 148, right - suggestionWidth());
    }

    private static int suggestionY(NoShadowTextField searchBar) {
        return searchBar.getY() + searchBar.getHeight() + 2;
    }

    private static int suggestionWidth() {
        return 132;
    }
}
