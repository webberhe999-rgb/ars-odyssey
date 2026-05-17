package com.swvague.ars_odyssey.mixin;
import com.swvague.ars_odyssey.client.gui.spellbook.SpellBookDragController;
import com.swvague.ars_odyssey.client.gui.spellbook.SpellBookArchiveUi;
import com.swvague.ars_odyssey.client.gui.spellbook.SpellBookSearchUi;
import com.swvague.ars_odyssey.client.gui.spellbook.SpellBookTooltipAdapter;
import com.swvague.ars_odyssey.client.gui.spellbook.SpellBookTruthificationUi;
import net.minecraft.client.gui.Font;
import org.spongepowered.asm.mixin.injection.Redirect;
import com.swvague.ars_odyssey.index.GlyphApplicationIndex;
import com.swvague.ars_odyssey.index.MatchDisplayLabel;
import com.swvague.ars_odyssey.index.MatchReason;
import com.swvague.ars_odyssey.index.SearchIntent;
import com.swvague.ars_odyssey.index.SearchIntentResolver;
import com.swvague.ars_odyssey.index.SearchIntentType;
import com.swvague.ars_odyssey.registry.ModRegistry;
import com.hollingsworth.arsnouveau.api.spell.AbstractAugment;
import com.hollingsworth.arsnouveau.api.spell.AbstractCastMethod;
import com.hollingsworth.arsnouveau.api.spell.AbstractEffect;
import com.hollingsworth.arsnouveau.api.spell.AbstractSpellPart;
import com.hollingsworth.arsnouveau.client.gui.buttons.CraftingButton;
import com.hollingsworth.arsnouveau.client.gui.buttons.CreateSpellButton;
import com.hollingsworth.arsnouveau.client.gui.buttons.GlyphButton;
import com.hollingsworth.arsnouveau.client.gui.NoShadowTextField;
import com.hollingsworth.arsnouveau.client.gui.book.BaseBook;
import com.hollingsworth.arsnouveau.client.gui.book.GuiSpellBook;
import com.hollingsworth.arsnouveau.client.gui.buttons.GuiImageButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.screens.inventory.PageButton;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Mixin(GuiSpellBook.class)
public abstract class GuiSpellBookMixin extends BaseBook {
    // ---- Odyssey spell-book layout (fully owned, deterministic) ----
    // Section headers + the entanglement/complexity lines are drawn in
    // drawBackgroundElements while a translate(bookLeft, bookTop) is active,
    // so their coordinates are RELATIVE to the book's top-left corner.
    // Glyph buttons are widgets rendered without that translate, so their
    // coordinates are ABSOLUTE and must include bookLeft/bookTop.
    @Unique private static final int ARS_ODYSSEY$LEFT_PAGE_HEADER_MAX_WIDTH = 126;
    @Unique private static final int ARS_ODYSSEY$LEFT_PAGE_TEXT_X = 20;
    @Unique private static final int ARS_ODYSSEY$RIGHT_PAGE_TEXT_X = 154;
    // Entanglement's first line is kept level with the right page's first row
    // (the Effect header), both nudged a little below the book's top border.
    @Unique private static final int ARS_ODYSSEY$ENTANGLEMENT_Y = 20;
    // Form header sits below the two wrapped entanglement lines (20 and ~30).
    @Unique private static final int ARS_ODYSSEY$LEFT_FIRST_Y = 40;
    // Right page first row, aligned with ENTANGLEMENT_Y.
    @Unique private static final int ARS_ODYSSEY$RIGHT_FIRST_Y = 20;
    @Unique private static final int ARS_ODYSSEY$HEADER_H = 9;
    @Unique private static final int ARS_ODYSSEY$GLYPH_ROW_H = 17;
    @Unique private static final int ARS_ODYSSEY$GLYPH_SIZE = 16;
    @Unique private static final int ARS_ODYSSEY$PER_ROW = 6;
    @Unique private static final int ARS_ODYSSEY$COL_W = 20;
    // Left-page glyph rows must stay above the complexity line.
    @Unique private static final int ARS_ODYSSEY$LEFT_LIMIT_Y = 126;
    @Unique private static final int ARS_ODYSSEY$COMPLEXITY_LEFT_PAGE_Y = 130;

    // Mutable cursor + recorded header positions, rebuilt every layoutAllGlyphs.
    @Unique private boolean ars_odyssey$onRightPage;
    @Unique private int ars_odyssey$rowTopY;
    @Unique private int ars_odyssey$col;
    @Unique private int ars_odyssey$section; // 0=none 1=form 2=augment 3=effect
    @Unique private boolean ars_odyssey$sectionHasGlyph;
    @Unique private int ars_odyssey$formHeaderX = -1;
    @Unique private int ars_odyssey$formHeaderY = -1;
    @Unique private int ars_odyssey$augmentHeaderX = -1;
    @Unique private int ars_odyssey$augmentHeaderY = -1;
    @Unique private int ars_odyssey$effectHeaderX = -1;
    @Unique private int ars_odyssey$effectHeaderY = -1;

    @Shadow public String previousString;
    @Shadow public NoShadowTextField searchBar;
    @Shadow public EditBox spell_name;
    @Shadow public List<AbstractSpellPart> unlockedSpells;
    @Shadow public List<AbstractSpellPart> displayedGlyphs;
    @Shadow public int page;
    @Shadow public PageButton nextButton;
    @Shadow public PageButton previousButton;
    @Shadow public ItemStack bookStack;
    @Shadow public Renderable hoveredWidget;
    @Shadow public List<CraftingButton> craftingCells;
    @Shadow public List<AbstractSpellPart> spell;
    @Shadow public PageButton nextGlyphButton;
    @Shadow public PageButton prevGlyphButton;
    @Shadow public int maxManaCache;
    @Shadow public List<GlyphButton> glyphButtons;
    @Shadow public CreateSpellButton createSpellButton;
    @Shadow public InteractionHand hand;
    @Shadow int currentCostCache;

    @Shadow public abstract void updateNextPageButtons();

    @Unique
    private Map<ResourceLocation, List<MatchReason>> ars_odyssey$reasonsByGlyph = Collections.emptyMap();
    @Unique
    private MatchReason ars_odyssey$headerReason = null;
    @Unique
    private SearchIntent ars_odyssey$searchIntent = new SearchIntent("", SearchIntentType.EMPTY, null, "");
    @Unique
    private final SpellBookSearchUi ars_odyssey$searchUi = new SpellBookSearchUi();
    @Unique
    private final SpellBookArchiveUi ars_odyssey$archiveUi = new SpellBookArchiveUi();
    @Unique
    private final SpellBookDragController ars_odyssey$dragController = new SpellBookDragController();
    @Unique
    private final SpellBookTruthificationUi ars_odyssey$truthificationUi = new SpellBookTruthificationUi();
    @Unique
    private final Map<AbstractWidget, Boolean> ars_odyssey$hiddenArchiveWidgets = new IdentityHashMap<>();
    @Unique
    private boolean ars_odyssey$archiveWasOpen;

    @Invoker("layoutAllGlyphs")
    protected abstract void ars_odyssey$layoutAllGlyphs(int page);

    @Invoker("validate")
    protected abstract void ars_odyssey$validate();

    @Invoker("updateNextGlyphArrow")
    protected abstract void ars_odyssey$updateNextGlyphArrow();

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (ars_odyssey$dragController.mouseClicked(
                mouseX,
                mouseY,
                button,
                ars_odyssey$isOdysseyBook(),
                craftingCells,
                spell)) {
            return true;
        }

        if (ars_odyssey$isOdysseyBook()
                && ars_odyssey$archiveUi.handleClick(mouseX, mouseY, button, bookLeft, bookTop, hand)) {
            return true;
        }

        if (ars_odyssey$isOdysseyBook()
                && ars_odyssey$truthificationUi.handleClick(
                mouseX,
                mouseY,
                button,
                true,
                craftingCells,
                spell)) {
            return true;
        }

        if (ars_odyssey$isOdysseyBook()
                && ars_odyssey$searchUi.handleClick(mouseX, mouseY, button, searchBar, bookLeft)) {
            return true;
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (ars_odyssey$isOdysseyBook() && ars_odyssey$archiveUi.isOpen()) {
            return true;
        }
        if (ars_odyssey$dragController.mouseDragged(mouseX, mouseY, craftingCells)) {
            return true;
        }

        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (ars_odyssey$dragController.mouseReleased(
                mouseX,
                mouseY,
                button,
                craftingCells,
                spell,
                () -> {
                    if (nextGlyphButton != null) {
                        ars_odyssey$updateNextGlyphArrow();
                    }
                },
                this::ars_odyssey$validate)) {
            return true;
        }

        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (ars_odyssey$isOdysseyBook() && ars_odyssey$archiveUi.isOpen()) {
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 256) {
            if (ars_odyssey$archiveUi.isOpen()) {
                ars_odyssey$archiveUi.close();
                return true;
            }
            ars_odyssey$searchUi.hide();
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void ars_odyssey$renderDraggedSpellPart(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks, CallbackInfo ci) {
        if (ars_odyssey$isOdysseyBook()) {
            if (!ars_odyssey$archiveUi.isOpen()) {
                ars_odyssey$searchUi.render(graphics, font, searchBar, bookLeft, mouseX, mouseY);
                ars_odyssey$truthificationUi.renderCheckboxes(
                        graphics,
                        font,
                        true,
                        craftingCells,
                        spell);
            }
            ars_odyssey$archiveUi.render(graphics, font, bookLeft, bookTop, mouseX, mouseY);
        }

        ars_odyssey$dragController.renderDraggedSpellPart(graphics, spell);
    }

    @Inject(method = "render", at = @At("HEAD"))
    private void ars_odyssey$prepareArchiveRender(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks, CallbackInfo ci) {
        if (!ars_odyssey$isOdysseyBook()) {
            return;
        }
        boolean archiveOpen = ars_odyssey$archiveUi.isOpen();
        if (archiveOpen && !ars_odyssey$archiveWasOpen) {
            ars_odyssey$hideSpellBookWorkAreaForArchive();
        } else if (!archiveOpen && ars_odyssey$archiveWasOpen) {
            ars_odyssey$restoreSpellBookWorkArea();
        }
        ars_odyssey$archiveWasOpen = archiveOpen;
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void ars_odyssey$initOdysseyState(CallbackInfo ci) {
        if (ars_odyssey$reasonsByGlyph == null) {
            ars_odyssey$reasonsByGlyph = Collections.emptyMap();
        }
        ars_odyssey$headerReason = null;
        ars_odyssey$searchIntent = new SearchIntent("", SearchIntentType.EMPTY, null, "");
        ars_odyssey$searchUi.reset();
        ars_odyssey$archiveUi.reset();
        ars_odyssey$archiveWasOpen = false;
        ars_odyssey$hiddenArchiveWidgets.clear();
        ars_odyssey$dragController.clearState();
        if (ars_odyssey$isOdysseyBook()) {
            ars_odyssey$replaceUtilityBookmarks();
        }
    }

    @Unique
    private boolean ars_odyssey$isOdysseyBook() {
        return bookStack != null && ModRegistry.isOdysseySpellBook(bookStack);
    }

    @Unique
    private void ars_odyssey$replaceUtilityBookmarks() {
        List<Renderable> snapshot = List.copyOf(this.renderables);
        for (Renderable renderable : snapshot) {
            if (renderable instanceof GuiImageButton button
                    && button.getX() == bookLeft - 15
                    && ars_odyssey$isReplaceableBookmarkY(button.getY())) {
                this.removeWidget(button);
            }
        }
    }

    @Unique
    private boolean ars_odyssey$isReplaceableBookmarkY(int y) {
        int relativeY = y - bookTop;
        return relativeY == 22
                || relativeY == 70
                || relativeY == 94
                || relativeY == 118
                || relativeY == 142;
    }

    @Unique
    private void ars_odyssey$hideSpellBookWorkAreaForArchive() {
        ars_odyssey$dragController.clearState();
        ars_odyssey$hiddenArchiveWidgets.clear();
        for (Renderable renderable : this.renderables) {
            if (renderable instanceof AbstractWidget widget
                    && widget.visible
                    && widget.getX() >= bookLeft - 5) {
                ars_odyssey$hiddenArchiveWidgets.put(widget, true);
                widget.visible = false;
            }
        }
    }

    @Unique
    private void ars_odyssey$restoreSpellBookWorkArea() {
        for (Map.Entry<AbstractWidget, Boolean> entry : ars_odyssey$hiddenArchiveWidgets.entrySet()) {
            entry.getKey().visible = entry.getValue();
        }
        ars_odyssey$hiddenArchiveWidgets.clear();
        ars_odyssey$layoutAllGlyphs(page);
        ars_odyssey$validate();
    }


    @Inject(method = "onSearchChanged", at = @At("HEAD"), cancellable = true)
    private void ars_odyssey$customSearch(String str, CallbackInfo ci) {
        // Only modify the search behavior for Odyssey Spell Book.
        // Other Ars Nouveau spell books keep their original behavior.
        if (!ModRegistry.isOdysseySpellBook(bookStack)) {
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
        ars_odyssey$searchUi.refresh(searchBar);
        GlyphApplicationIndex.SearchResult result = GlyphApplicationIndex.searchWithReasons(query, unlockedSpells);
        displayedGlyphs = result.glyphs();
        ars_odyssey$searchIntent = SearchIntentResolver.resolve(query);
        ars_odyssey$reasonsByGlyph = SpellBookTooltipAdapter.enrichWithSelfSearchReasons(
                result.reasonsByGlyph() == null ? Collections.emptyMap() : result.reasonsByGlyph(),
                ars_odyssey$searchIntent,
                displayedGlyphs);

        ars_odyssey$headerReason = SpellBookTooltipAdapter.computeHeaderReason(
                searchBar == null ? "" : searchBar.getValue(),
                displayedGlyphs,
                ars_odyssey$reasonsByGlyph);




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
        if (!ModRegistry.isOdysseySpellBook(bookStack)) {
            return;
        }
        AbstractSpellPart hoveredPart = SpellBookTooltipAdapter.hoveredSpellPart(hoveredWidget, mouseX, mouseY);
        if (hoveredPart == null || hoveredPart.getRegistryName() == null) {
            return;
        }
        Boolean truthificationEnabledForSlot = hoveredWidget instanceof CraftingButton craftingButton
                ? SpellBookTruthificationUi.truthificationEnabledForSlot(
                craftingButton.slotNum,
                hoveredPart.getRegistryName(),
                spell)
                : null;
        SpellBookTooltipAdapter.appendTooltip(
                tooltip,
                hoveredPart,
                hoveredWidget instanceof GlyphButton,
                ars_odyssey$reasonsByGlyph,
                ars_odyssey$searchIntent,
                truthificationEnabledForSlot);
    }
    // Entanglement (top) + complexity (left-page last row) are drawn here so
    // they ALWAYS show, on every page (the Form-header redirect only fires on
    // page 0, since AN sets formTextRow=0 on other pages). This TAIL-inject
    // mechanism is proven to render.
    @Inject(method = "drawBackgroundElements", at = @At("TAIL"))
    private void ars_odyssey$drawEntanglementAndComplexity(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks, CallbackInfo ci) {
        if (!ModRegistry.isOdysseySpellBook(bookStack)) {
            return;
        }
        ars_odyssey$drawWrappedLine(
                graphics,
                font,
                SpellBookTruthificationUi.totalEntanglementText().getString(),
                ARS_ODYSSEY$LEFT_PAGE_TEXT_X,
                ARS_ODYSSEY$ENTANGLEMENT_Y,
                ARS_ODYSSEY$LEFT_PAGE_HEADER_MAX_WIDTH,
                0xFF6A5630,
                false);
        ars_odyssey$drawWrappedLine(
                graphics,
                font,
                SpellBookTruthificationUi.spellComplexityText(spell, bookStack).getString(),
                ARS_ODYSSEY$LEFT_PAGE_TEXT_X,
                ARS_ODYSSEY$COMPLEXITY_LEFT_PAGE_Y,
                ARS_ODYSSEY$LEFT_PAGE_HEADER_MAX_WIDTH,
                0xFF6A5630,
                false);
    }

    // Headers are repositioned to the slot recorded by the glyph layout so
    // they always sit directly above their own glyph rows.
    @Redirect(
            method = "drawBackgroundElements",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/GuiGraphics;drawString(Lnet/minecraft/client/gui/Font;Ljava/lang/String;III Z)I",
                    ordinal = 0
            )
    )
    private int ars_odyssey$repositionFormHeader(
            GuiGraphics graphics, Font font, String text, int x, int y, int color, boolean dropShadow) {
        if (!ModRegistry.isOdysseySpellBook(bookStack) || ars_odyssey$formHeaderY < 0) {
            return graphics.drawString(font, text, x, y, color, dropShadow);
        }
        return graphics.drawString(font, text, ars_odyssey$formHeaderX, ars_odyssey$formHeaderY, color, dropShadow);
    }

    @Redirect(
            method = "drawBackgroundElements",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/GuiGraphics;drawString(Lnet/minecraft/client/gui/Font;Ljava/lang/String;III Z)I",
                    ordinal = 1
            )
    )
    private int ars_odyssey$repositionEffectHeader(
            GuiGraphics graphics, Font font, String text, int x, int y, int color, boolean dropShadow) {
        if (!ModRegistry.isOdysseySpellBook(bookStack) || ars_odyssey$effectHeaderY < 0) {
            return graphics.drawString(font, text, x, y, color, dropShadow);
        }
        return graphics.drawString(font, text, ars_odyssey$effectHeaderX, ars_odyssey$effectHeaderY, color, dropShadow);
    }

    @Redirect(
            method = "drawBackgroundElements",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/GuiGraphics;drawString(Lnet/minecraft/client/gui/Font;Ljava/lang/String;III Z)I",
                    ordinal = 2
            )
    )
    private int ars_odyssey$repositionAugmentHeader(
            GuiGraphics graphics, Font font, String text, int x, int y, int color, boolean dropShadow) {
        if (!ModRegistry.isOdysseySpellBook(bookStack) || ars_odyssey$augmentHeaderY < 0) {
            return graphics.drawString(font, text, x, y, color, dropShadow);
        }
        return graphics.drawString(font, text, ars_odyssey$augmentHeaderX, ars_odyssey$augmentHeaderY, color, dropShadow);
    }

    @Inject(method = "layoutAllGlyphs", at = @At("HEAD"))
    private void ars_odyssey$resetGlyphLayoutCursor(int page, CallbackInfo ci) {
        ars_odyssey$onRightPage = false;
        ars_odyssey$rowTopY = ARS_ODYSSEY$LEFT_FIRST_Y;
        ars_odyssey$col = 0;
        ars_odyssey$section = 0;
        ars_odyssey$sectionHasGlyph = false;
        ars_odyssey$formHeaderX = -1;
        ars_odyssey$formHeaderY = -1;
        ars_odyssey$augmentHeaderX = -1;
        ars_odyssey$augmentHeaderY = -1;
        ars_odyssey$effectHeaderX = -1;
        ars_odyssey$effectHeaderY = -1;
    }

    @Unique
    private int ars_odyssey$classifySection(AbstractSpellPart part) {
        if (part instanceof AbstractCastMethod) {
            return 1;
        }
        if (part instanceof AbstractAugment) {
            return 2;
        }
        return 3; // AbstractEffect / anything else
    }

    @Unique
    private void ars_odyssey$recordHeader(int section, int relX, int relY) {
        if (section == 1) {
            ars_odyssey$formHeaderX = relX;
            ars_odyssey$formHeaderY = relY;
        } else if (section == 2) {
            ars_odyssey$augmentHeaderX = relX;
            ars_odyssey$augmentHeaderY = relY;
        } else {
            ars_odyssey$effectHeaderX = relX;
            ars_odyssey$effectHeaderY = relY;
        }
    }

    /**
     * Fully owns the Odyssey glyph grid: forms/augments/effects each get a
     * header row, glyphs flow 6-per-row, and when the left page is full the
     * remaining glyphs (and any not-yet-started section's header) wrap to the
     * right page starting at its top. AN's own x/y are discarded.
     */
    @Redirect(
            method = "layoutAllGlyphs",
            at = @At(value = "NEW", target = "com/hollingsworth/arsnouveau/client/gui/buttons/GlyphButton")
    )
    private GlyphButton ars_odyssey$layoutOdysseyGlyph(int x, int y, AbstractSpellPart part, Button.OnPress onPress) {
        if (!ModRegistry.isOdysseySpellBook(bookStack)) {
            return new GlyphButton(x, y, part, onPress);
        }

        int secType = ars_odyssey$classifySection(part);
        if (secType != ars_odyssey$section) {
            ars_odyssey$section = secType;
            ars_odyssey$sectionHasGlyph = false;
            if (ars_odyssey$col != 0) {
                ars_odyssey$rowTopY += ARS_ODYSSEY$GLYPH_ROW_H;
                ars_odyssey$col = 0;
            }
            // Need room for the header plus at least one glyph row.
            if (!ars_odyssey$onRightPage
                    && ars_odyssey$rowTopY + ARS_ODYSSEY$HEADER_H + ARS_ODYSSEY$GLYPH_SIZE > ARS_ODYSSEY$LEFT_LIMIT_Y) {
                ars_odyssey$onRightPage = true;
                ars_odyssey$rowTopY = ARS_ODYSSEY$RIGHT_FIRST_Y;
                ars_odyssey$col = 0;
            }
            ars_odyssey$recordHeader(
                    secType,
                    ars_odyssey$onRightPage ? ARS_ODYSSEY$RIGHT_PAGE_TEXT_X : ARS_ODYSSEY$LEFT_PAGE_TEXT_X,
                    ars_odyssey$rowTopY);
            ars_odyssey$rowTopY += ARS_ODYSSEY$HEADER_H;
        }

        if (ars_odyssey$col >= ARS_ODYSSEY$PER_ROW) {
            ars_odyssey$col = 0;
            ars_odyssey$rowTopY += ARS_ODYSSEY$GLYPH_ROW_H;
        }

        // Left-page cap: spill the rest to the right page. If the section has
        // no glyph on the left yet, move its header along with it.
        if (!ars_odyssey$onRightPage
                && ars_odyssey$rowTopY + ARS_ODYSSEY$GLYPH_SIZE > ARS_ODYSSEY$LEFT_LIMIT_Y) {
            ars_odyssey$onRightPage = true;
            ars_odyssey$rowTopY = ARS_ODYSSEY$RIGHT_FIRST_Y;
            ars_odyssey$col = 0;
            if (!ars_odyssey$sectionHasGlyph) {
                ars_odyssey$recordHeader(ars_odyssey$section, ARS_ODYSSEY$RIGHT_PAGE_TEXT_X, ars_odyssey$rowTopY);
                ars_odyssey$rowTopY += ARS_ODYSSEY$HEADER_H;
            }
        }

        int baseX = bookLeft + (ars_odyssey$onRightPage ? ARS_ODYSSEY$RIGHT_PAGE_TEXT_X : ARS_ODYSSEY$LEFT_PAGE_TEXT_X);
        int absX = baseX + ars_odyssey$col * ARS_ODYSSEY$COL_W;
        int absY = bookTop + ars_odyssey$rowTopY;
        ars_odyssey$col++;
        ars_odyssey$sectionHasGlyph = true;
        return new GlyphButton(absX, absY, part, onPress);
    }

    @Redirect(
            method = "drawBackgroundElements",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/GuiGraphics;drawString(Lnet/minecraft/client/gui/Font;Ljava/lang/String;III Z)I",
                    ordinal = 4
            )
    )
    private int ars_odyssey$appendTruthifiedManaDrain(
            GuiGraphics graphics,
            Font font,
            String text,
            int x,
            int y,
            int color,
            boolean dropShadow
    ) {
        if (!ModRegistry.isOdysseySpellBook(bookStack)) {
            return graphics.drawString(font, text, x, y, color, dropShadow);
        }
        String drainSuffix = SpellBookTruthificationUi.orbitManaDrainText(spell).getString();
        if (drainSuffix.isBlank()) {
            return graphics.drawString(font, text, x, y, color, dropShadow);
        }
        String expected = currentCostCache + "  /  " + maxManaCache;
        if (!expected.equals(text)) {
            return graphics.drawString(font, text, x, y, color, dropShadow);
        }
        return graphics.drawString(font, currentCostCache + drainSuffix, x, y, color, dropShadow);
    }

    @Unique
    private int ars_odyssey$drawWrappedLine(
            GuiGraphics graphics,
            Font font,
            String text,
            int x,
            int y,
            int maxWidth,
            int color,
            boolean dropShadow
    ) {
        if (font.width(text) <= maxWidth) {
            graphics.drawString(font, text, x, y, color, dropShadow);
            return 1;
        }
        int split = text.lastIndexOf(' ');
        if (split <= 0) {
            split = text.lastIndexOf(':');
        }
        if (split <= 0 || split >= text.length() - 1) {
            graphics.drawString(font, text, x, y, color, dropShadow);
            return 1;
        }
        graphics.drawString(font, text.substring(0, split + 1).trim(), x, y, color, dropShadow);
        graphics.drawString(font, text.substring(split + 1).trim(), x, y + 10, color, dropShadow);
        return 2;
    }
}
