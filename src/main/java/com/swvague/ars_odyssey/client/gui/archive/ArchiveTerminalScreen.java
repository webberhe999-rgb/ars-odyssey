package com.swvague.ars_odyssey.client.gui.archive;

import java.util.AbstractMap;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.OptionalInt;

import com.hollingsworth.arsnouveau.client.gui.book.GuiSpellBook;
import com.swvague.ars_odyssey.archive.menu.ArchiveTerminalMenu;
import com.swvague.ars_odyssey.archive.ArchiveCoreBinding;
import com.swvague.ars_odyssey.archive.ArchiveItemKey;
import com.swvague.ars_odyssey.archive.PlayerArchiveAttachments;
import com.swvague.ars_odyssey.archive.PlayerArchiveData;
import com.swvague.ars_odyssey.archive.resonance.ResonanceRiskLevel;
import com.swvague.ars_odyssey.archive.resonance.ResonanceState;
import com.swvague.ars_odyssey.network.ModNetwork;
import com.swvague.ars_odyssey.registry.ModRegistry;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

public class ArchiveTerminalScreen extends AbstractContainerScreen<ArchiveTerminalMenu> {
    private static final int PANEL_W = 214;
    private static final int PANEL_H = 268;
    private static final int TEXT = 0xFF4F4026;
    private static final int MUTED = 0xFF806D48;
    private static final int LINE = 0xFFB89342;
    private static final int TEAL = 0xFF2FAEAA;
    private static final int DEEP = 0xFF21182B;
    private static final int STORED_GRID_X = 14;
    private static final int STORED_LABEL_Y = 40;
    private static final int STORED_GRID_Y = 56;
    private static final int STORED_COLS = 9;
    private static final int STORED_ROWS = 2;
    private static final int CELL_STEP_X = 19;
    private static final int CELL_STEP_Y = 19;
    private static final int CELL_W = 18;
    private static final int CELL_H = 18;
    private static final int EQUIPMENT_X = 14;
    private static final int EQUIPMENT_Y = 112;
    private static final int EQUIPMENT_STEP = 20;
    private static final int PAGE_TAB_X = -24;
    private static final int PAGE_TAB_Y = 50;
    private static final int PAGE_TAB_W = 24;
    private static final int PAGE_TAB_H = 46;
    private static final int PAGE_TAB_GAP = 8;
    private ArchiveItemKey pendingZeroKey;
    private ItemStack pendingZeroStack = ItemStack.EMPTY;
    private final Inventory playerInventory;
    private int storedScrollOffset;
    private TerminalPage page = TerminalPage.MAIN;

    private enum TerminalPage {
        MAIN,
        CRAFTING
    }

    public ArchiveTerminalScreen(ArchiveTerminalMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.playerInventory = inventory;
        imageWidth = PANEL_W;
        imageHeight = PANEL_H;
        inventoryLabelX = ArchiveTerminalMenu.INVENTORY_X;
        inventoryLabelY = ArchiveTerminalMenu.INVENTORY_LABEL_Y;
        titleLabelX = 14;
        titleLabelY = 12;
    }

    public static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(ModRegistry.ARCHIVE_TERMINAL_MENU.get(), ArchiveTerminalScreen::new);
    }

    @Override
    protected void init() {
        super.init();
        addRenderableWidget(Button.builder(Component.translatable("ars_odyssey.archive_terminal.spellbook"), button ->
                minecraft.setScreen(new GuiSpellBook(menu.returnHand()))
        ).bounds(leftPos + imageWidth - 82, topPos + 9, 72, 14).build());
        syncPageSlots();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        renderBackground(graphics, mouseX, mouseY, partialTicks);
        super.render(graphics, mouseX, mouseY, partialTicks);
        renderTooltip(graphics, mouseX, mouseY);
        renderArchiveTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTicks, int mouseX, int mouseY) {
        syncPageSlots();
        PlayerArchiveData data = playerArchive();
        drawPanel(graphics, leftPos, topPos, imageWidth, imageHeight);
        drawHeader(graphics, data);
        drawPageTabs(graphics, leftPos + PAGE_TAB_X, topPos + PAGE_TAB_Y);
        drawStoredGrid(graphics, data, leftPos + STORED_GRID_X, topPos + STORED_LABEL_Y);
        if (page == TerminalPage.CRAFTING) {
            drawCraftingPanel(graphics, leftPos + ArchiveTerminalMenu.CRAFT_GRID_X, topPos + ArchiveTerminalMenu.CRAFT_GRID_Y - 16);
            drawEquipmentPanel(graphics, leftPos + EQUIPMENT_X, topPos + EQUIPMENT_Y - 16);
        } else {
            drawMainPage(graphics, data, leftPos + 14, topPos + 96);
        }
        drawInventorySlots(graphics);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, tr("ars_odyssey.archive_terminal.title"), titleLabelX, titleLabelY, DEEP, false);
        graphics.drawString(font, tr("ars_odyssey.archive_terminal.inventory"), inventoryLabelX, inventoryLabelY, TEXT, false);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && handlePageTabClick(mouseX, mouseY)) {
            return true;
        }
        if (button == 0 && handleStoredScrollbarClick(mouseX, mouseY)) {
            return true;
        }
        PlayerArchiveData data = playerArchive();
        if (data.isAwakened() && handleProvisionClick(mouseX, mouseY, button, data)) {
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (scrollY != 0 && inside(mouseX, mouseY, leftPos, topPos, imageWidth, imageHeight)) {
            return scrollStored(scrollY);
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    private boolean handleProvisionClick(double mouseX, double mouseY, int button, PlayerArchiveData data) {
        if (button == 0 || button == 1) {
            if (handleStoredGridClick(mouseX, mouseY, button, data)) {
                return true;
            }
        }
        return false;
    }

    private boolean handleStoredGridClick(double mouseX, double mouseY, int button, PlayerArchiveData data) {
        OptionalInt maybeIndex = storedCellIndex(mouseX, mouseY);
        if (maybeIndex.isEmpty()) {
            return false;
        }

        ItemStack carried = menu.getCarried();
        if (!carried.isEmpty()) {
            ModNetwork.depositCarriedArchiveItem(button == 1 ? 1 : carried.getCount());
            return true;
        }

        int index = maybeIndex.getAsInt();
        List<Map.Entry<ArchiveItemKey, Long>> entries = storedEntries(data);
        if (index >= entries.size()) {
            return true;
        }

        Map.Entry<ArchiveItemKey, Long> entry = entries.get(index);
        int requested = button == 1
                ? 1
                : data.ledger().prototype(entry.getKey()).getMaxStackSize();
        requested = (int) Math.max(1L, Math.min(entry.getValue(), requested));
        if (hasShiftDown()) {
            ModNetwork.extractArchiveItem(entry.getKey(), requested);
        } else {
            if (requested >= entry.getValue()) {
                pendingZeroKey = entry.getKey();
                pendingZeroStack = data.ledger().prototype(entry.getKey());
            }
            ModNetwork.extractArchiveItemToCursor(entry.getKey(), requested);
        }
        return true;
    }

    private void drawHeader(GuiGraphics graphics, PlayerArchiveData data) {
        String state = data.isAwakened()
                ? tr("ars_odyssey.archive_terminal.access." + data.unlockedAccessMode().name().toLowerCase(Locale.ROOT))
                : tr("ars_odyssey.archive_terminal.state.dormant");
        graphics.drawString(font, state, leftPos + imageWidth - 14 - font.width(state), topPos + 12, data.isAwakened() ? TEAL : MUTED, false);
        String binding = data.coreBinding().map(ArchiveCoreBinding::display)
                .orElse(tr("ars_odyssey.archive_terminal.core.unbound"));
        drawClipped(graphics, tr("ars_odyssey.archive_terminal.core", binding), leftPos + 14, topPos + 26, imageWidth - 28, MUTED);
        graphics.fill(leftPos + 12, topPos + 34, leftPos + imageWidth - 12, topPos + 35, 0x77957634);
    }

    private void drawResonance(GuiGraphics graphics, PlayerArchiveData data, int x, int y, int width) {
        ResonanceState state = data.resonanceState();
        double[] projected = projectedResonance(state);
        double stability = projected[0];
        double load = projected[1];
        double pressure = projected[2];
        ResonanceRiskLevel risk = riskFor(stability, load, pressure);
        graphics.drawString(font, tr("ars_odyssey.archive_terminal.resonance"), x, y, TEXT, false);
        graphics.drawString(font, tr("ars_odyssey.archive_terminal.risk." + risk.name().toLowerCase(Locale.ROOT)), x + 76, y, riskColor(risk), false);
        drawMeter(graphics, x, y + 15, width, 7, stability / 100.0D, 0xFF5EBE6B);
        drawMeter(graphics, x, y + 26, width, 7, load / Math.max(1.0D, stability), 0xFFE2A93B);
        drawMeter(graphics, x, y + 37, width, 7, pressure / 100.0D, 0xFFB0446D);
        graphics.drawString(font, "S " + formatDouble(stability), x + width + 8, y + 13, MUTED, false);
        graphics.drawString(font, "L " + formatDouble(load), x + width + 8, y + 24, MUTED, false);
        graphics.drawString(font, "D " + formatDouble(pressure), x + width + 8, y + 35, MUTED, false);
    }

    private void drawPageTabs(GuiGraphics graphics, int x, int y) {
        drawPageTab(graphics, tr("ars_odyssey.archive_terminal.page.main.short"), x, y, page == TerminalPage.MAIN);
        drawPageTab(graphics, tr("ars_odyssey.archive_terminal.page.crafting.short"), x, y + PAGE_TAB_H + PAGE_TAB_GAP, page == TerminalPage.CRAFTING);
    }

    private void drawPageTab(GuiGraphics graphics, String label, int x, int y, boolean active) {
        graphics.fill(x, y, x + PAGE_TAB_W + 3, y + PAGE_TAB_H, active ? LINE : 0xFF6A4D91);
        graphics.fill(x + 2, y + 2, x + PAGE_TAB_W + 2, y + PAGE_TAB_H - 2, active ? 0xFFFFF2C6 : 0xFF4D2E78);
        graphics.fill(x + PAGE_TAB_W, y + 4, x + PAGE_TAB_W + 4, y + PAGE_TAB_H - 4, active ? 0xFFFFF2C6 : 0xFF4D2E78);
        int textX = x + Math.max(4, (PAGE_TAB_W - font.width(label)) / 2);
        graphics.drawString(font, label, textX, y + (PAGE_TAB_H - 8) / 2, active ? TEXT : 0xFFFFFFFF, false);
    }

    private void drawMainPage(GuiGraphics graphics, PlayerArchiveData data, int x, int y) {
        graphics.drawString(font, tr("ars_odyssey.archive_terminal.network"), x, y, TEXT, false);
        drawResonance(graphics, data, x, y + 14, 126);
    }

    private void drawStoredGrid(GuiGraphics graphics, PlayerArchiveData data, int x, int y) {
        graphics.drawString(font, tr("ars_odyssey.archive_terminal.stored"), x, y, TEXT, false);
        List<Map.Entry<ArchiveItemKey, Long>> entries = storedEntries(data);
        int maxOffset = maxStoredScrollOffset(data);
        if (storedScrollOffset > maxOffset) {
            storedScrollOffset = maxOffset;
        }
        int gridY = y + 16;
        for (int row = 0; row < STORED_ROWS; row++) {
            for (int col = 0; col < STORED_COLS; col++) {
                int cellX = x + col * CELL_STEP_X;
                int cellY = gridY + row * CELL_STEP_Y;
                int index = row * STORED_COLS + col;
                boolean provisioned = index < entries.size()
                        && data.provisionRules().get(entries.get(index).getKey().itemId()).isPresent();
                drawCell(graphics, cellX, cellY, provisioned);
                if (index < entries.size()) {
                    drawStack(graphics, entries.get(index), cellX + 1, cellY + 1);
                }
            }
        }
        if (entries.isEmpty()) {
            drawClipped(graphics,
                    data.isAwakened()
                            ? tr("ars_odyssey.archive_terminal.empty")
                            : tr("ars_odyssey.archive_terminal.awaiting_core"),
                    x + 50,
                    y,
                    132,
                    MUTED);
        }
        drawStoredScrollBar(graphics, data, x + STORED_COLS * CELL_STEP_X + 3, gridY);
    }

    private void drawStoredScrollBar(GuiGraphics graphics, PlayerArchiveData data, int x, int y) {
        int trackHeight = STORED_ROWS * CELL_STEP_Y - 2;
        int maxOffset = maxStoredScrollOffset(data);
        graphics.fill(x, y, x + 4, y + trackHeight, 0xFFC6AF70);
        graphics.fill(x + 1, y + 1, x + 3, y + trackHeight - 1, 0xFFEAD9A4);
        int thumbHeight = maxOffset <= 0 ? trackHeight - 2 : Math.max(8, (trackHeight - 2) * STORED_ROWS * STORED_COLS / Math.max(STORED_ROWS * STORED_COLS, storedEntryCount(data)));
        int thumbTravel = Math.max(0, trackHeight - 2 - thumbHeight);
        int thumbY = y + 1 + (maxOffset <= 0 ? 0 : (int) Math.round(thumbTravel * (storedScrollOffset / (double) maxOffset)));
        graphics.fill(x + 1, thumbY, x + 3, thumbY + thumbHeight, maxOffset <= 0 ? MUTED : TEAL);
    }

    private void drawCraftingPanel(GuiGraphics graphics, int x, int y) {
        graphics.drawString(font, tr("ars_odyssey.archive_terminal.crafting"), x, y, TEXT, false);
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                drawVanillaSlot(graphics,
                        leftPos + ArchiveTerminalMenu.CRAFT_GRID_X + col * 18,
                        topPos + ArchiveTerminalMenu.CRAFT_GRID_Y + row * 18);
            }
        }
        graphics.drawString(font, ">", leftPos + ArchiveTerminalMenu.CRAFT_GRID_X + 62, topPos + ArchiveTerminalMenu.CRAFT_GRID_Y + 20, MUTED, false);
        drawVanillaSlot(graphics, leftPos + ArchiveTerminalMenu.CRAFT_RESULT_X, topPos + ArchiveTerminalMenu.CRAFT_RESULT_Y);
    }

    private void drawEquipmentPanel(GuiGraphics graphics, int x, int y) {
        graphics.drawString(font, tr("ars_odyssey.archive_terminal.equipment"), x, y, TEXT, false);
        for (int i = 0; i < 5; i++) {
            int[] pos = equipmentSlotPosition(x, y + 16, i);
            drawVanillaSlot(graphics, pos[0], pos[1]);
            ItemStack stack = equipmentStack(i);
            if (!stack.isEmpty()) {
                graphics.renderItem(stack, pos[0], pos[1]);
                graphics.renderItemDecorations(font, stack, pos[0], pos[1]);
            }
        }
    }

    private void drawInventorySlots(GuiGraphics graphics) {
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                drawVanillaSlot(graphics,
                        leftPos + ArchiveTerminalMenu.INVENTORY_X + col * 18,
                        topPos + ArchiveTerminalMenu.INVENTORY_GRID_Y + row * 18);
            }
        }
        for (int col = 0; col < 9; col++) {
            drawVanillaSlot(graphics,
                    leftPos + ArchiveTerminalMenu.INVENTORY_X + col * 18,
                    topPos + ArchiveTerminalMenu.HOTBAR_Y);
        }
    }

    private void renderArchiveTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
        if (page == TerminalPage.CRAFTING) {
            OptionalInt equipmentIndex = equipmentCellIndex(mouseX, mouseY);
            if (equipmentIndex.isPresent()) {
                ItemStack stack = equipmentStack(equipmentIndex.getAsInt());
                if (!stack.isEmpty()) {
                    graphics.renderTooltip(font, stack, mouseX, mouseY);
                    return;
                }
            }
        }
        PlayerArchiveData data = playerArchive();
        List<Map.Entry<ArchiveItemKey, Long>> entries = storedEntries(data);
        OptionalInt maybeIndex = storedCellIndex(mouseX, mouseY);
        if (maybeIndex.isPresent() && maybeIndex.getAsInt() < entries.size()) {
            ArchiveItemKey key = entries.get(maybeIndex.getAsInt()).getKey();
            ItemStack stack = zeroEntryStack(data, key);
            if (!stack.isEmpty()) {
                graphics.renderTooltip(font, stack, mouseX, mouseY);
            }
        }
    }

    private void drawPanel(GuiGraphics graphics, int x, int y, int width, int height) {
        graphics.fill(x - 2, y - 2, x + width + 2, y + height + 2, 0xCC21182B);
        graphics.fill(x, y, x + width, y + height, 0xFFF8EDCB);
        graphics.fill(x + 4, y + 4, x + width - 4, y + height - 4, 0xFFF1E2B7);
        graphics.fill(x, y, x + width, y + 2, LINE);
        graphics.fill(x, y + height - 2, x + width, y + height, LINE);
        graphics.fill(x, y, x + 2, y + height, LINE);
        graphics.fill(x + width - 2, y, x + width, y + height, LINE);
    }

    private void drawCell(GuiGraphics graphics, int x, int y, boolean provisioned) {
        graphics.fill(x, y, x + CELL_W, y + CELL_H, provisioned ? TEAL : 0xFFC6AF70);
        graphics.fill(x + 1, y + 1, x + CELL_W - 1, y + CELL_H - 1, 0xFFF7EAC5);
        graphics.fill(x + 2, y + 2, x + CELL_W - 2, y + CELL_H - 2, 0xFFEAD9A4);
    }

    private void drawVanillaSlot(GuiGraphics graphics, int slotX, int slotY) {
        graphics.fill(slotX - 1, slotY - 1, slotX + 17, slotY + 17, 0xFFC6AF70);
        graphics.fill(slotX, slotY, slotX + 16, slotY + 16, 0xFFF7EAC5);
        graphics.fill(slotX + 1, slotY + 1, slotX + 15, slotY + 15, 0xFFEAD9A4);
    }

    private void drawStack(GuiGraphics graphics, Map.Entry<ArchiveItemKey, Long> entry, int x, int y) {
        ItemStack stack = zeroEntryStack(playerArchive(), entry.getKey());
        if (stack.isEmpty()) {
            return;
        }
        graphics.renderItem(stack, x, y);
        String count = formatCount(entry.getValue());
        graphics.drawString(font, count, x + 17 - font.width(count), y - 1, 0xFFFFFFFF, true);
    }

    private ItemStack zeroEntryStack(PlayerArchiveData data, ArchiveItemKey key) {
        if (pendingZeroKey != null && pendingZeroKey.equals(key) && data.ledger().count(key) <= 0) {
            return pendingZeroStack.copy();
        }
        return data.ledger().prototype(key);
    }

    private void drawMeter(GuiGraphics graphics, int x, int y, int width, int height, double fill, int color) {
        graphics.fill(x, y, x + width, y + height, 0xFF6C5430);
        graphics.fill(x + 1, y + 1, x + width - 1, y + height - 1, 0xFFE6D49C);
        int filled = (int) Math.round((width - 2) * Math.max(0.0D, Math.min(1.0D, fill)));
        graphics.fill(x + 1, y + 1, x + 1 + filled, y + height - 1, color);
    }

    private void drawClipped(GuiGraphics graphics, String text, int x, int y, int width, int color) {
        graphics.drawString(font, font.plainSubstrByWidth(text, width), x, y, color, false);
    }

    private List<Map.Entry<ArchiveItemKey, Long>> storedEntries(PlayerArchiveData data) {
        if (menu.getCarried().isEmpty()) {
            pendingZeroKey = null;
            pendingZeroStack = ItemStack.EMPTY;
        }
        List<Map.Entry<ArchiveItemKey, Long>> entries = data.ledger().counts().entrySet().stream()
                .sorted(Comparator.comparing(entry -> entry.getKey().asStorageKey()))
                .skip(storedScrollOffset)
                .limit(STORED_COLS * STORED_ROWS)
                .toList();
        if (pendingZeroKey == null || pendingZeroStack.isEmpty() || data.ledger().count(pendingZeroKey) > 0) {
            return entries;
        }
        if (entries.stream().anyMatch(entry -> entry.getKey().equals(pendingZeroKey)) || entries.size() >= STORED_COLS * STORED_ROWS) {
            return entries;
        }
        java.util.ArrayList<Map.Entry<ArchiveItemKey, Long>> withZero = new java.util.ArrayList<>(entries);
        withZero.add(new AbstractMap.SimpleImmutableEntry<>(pendingZeroKey, 0L));
        return withZero;
    }

    private int maxStoredScrollOffset(PlayerArchiveData data) {
        int total = storedEntryCount(data);
        int visible = STORED_COLS * STORED_ROWS;
        if (total <= visible) {
            return 0;
        }
        int max = total - visible;
        return (int) Math.ceil(max / (double) STORED_COLS) * STORED_COLS;
    }

    private int storedEntryCount(PlayerArchiveData data) {
        int total = data.ledger().counts().size();
        if (pendingZeroKey != null && !pendingZeroStack.isEmpty() && data.ledger().count(pendingZeroKey) <= 0) {
            total++;
        }
        return total;
    }

    private PlayerArchiveData playerArchive() {
        if (minecraft == null || minecraft.player == null) {
            return new PlayerArchiveData();
        }
        return minecraft.player.getData(PlayerArchiveAttachments.PLAYER_ARCHIVE);
    }

    private double[] projectedResonance(ResonanceState state) {
        if (minecraft == null || minecraft.level == null || state.lastUpdateGameTime() == Long.MIN_VALUE) {
            return new double[] {state.stability(), state.load(), state.dissonancePressure()};
        }
        long elapsed = Math.max(0L, minecraft.level.getGameTime() - state.lastUpdateGameTime());
        double seconds = elapsed / 20.0D;
        double load = Math.max(0.0D, state.load() - seconds * 2.0D);
        double pressure = Math.max(0.0D, state.dissonancePressure() - seconds * 0.35D);
        double stability = state.stability();
        if (load < stability) {
            stability = Math.max(0.0D, Math.min(100.0D, stability + seconds * 0.25D));
        }
        return new double[] {stability, load, pressure};
    }

    private ResonanceRiskLevel riskFor(double stability, double load, double pressure) {
        if (pressure >= 100.0D) {
            return ResonanceRiskLevel.DISSONANT;
        }
        if (load > stability) {
            return ResonanceRiskLevel.OVERLOADED;
        }
        if (stability < 35.0D || pressure >= 50.0D) {
            return ResonanceRiskLevel.UNSTABLE;
        }
        if (stability < 70.0D || load >= stability * 0.6D) {
            return ResonanceRiskLevel.STRAINED;
        }
        return ResonanceRiskLevel.STABLE;
    }

    private int riskColor(ResonanceRiskLevel risk) {
        return switch (risk) {
            case STABLE -> 0xFF267F55;
            case STRAINED -> 0xFF9E7A1E;
            case UNSTABLE -> 0xFFB45E27;
            case OVERLOADED -> 0xFFC23838;
            case DISSONANT -> 0xFF8E3DB8;
        };
    }

    private String formatDouble(double value) {
        return String.format(Locale.ROOT, "%.1f", value);
    }

    private String formatCount(long count) {
        if (count >= 1_000_000L) {
            return (count / 1_000_000L) + "m";
        }
        if (count >= 10_000L) {
            return (count / 1_000L) + "k";
        }
        return Long.toString(count);
    }

    private String tr(String key, Object... args) {
        return Component.translatable(key, args).getString();
    }

    private boolean handlePageTabClick(double mouseX, double mouseY) {
        int x = leftPos + PAGE_TAB_X;
        int y = topPos + PAGE_TAB_Y;
        if (inside(mouseX, mouseY, x, y, PAGE_TAB_W + 4, PAGE_TAB_H)) {
            page = TerminalPage.MAIN;
            syncPageSlots();
            return true;
        }
        if (inside(mouseX, mouseY, x, y + PAGE_TAB_H + PAGE_TAB_GAP, PAGE_TAB_W + 4, PAGE_TAB_H)) {
            page = TerminalPage.CRAFTING;
            syncPageSlots();
            return true;
        }
        return false;
    }

    private void syncPageSlots() {
        menu.setCraftingPageVisible(page == TerminalPage.CRAFTING);
    }

    private boolean scrollStored(double scrollY) {
        int maxOffset = maxStoredScrollOffset(playerArchive());
        if (maxOffset <= 0) {
            return false;
        }
        int notches = Math.max(1, (int) Math.ceil(Math.abs(scrollY)));
        int direction = scrollY < 0 ? 1 : -1;
        storedScrollOffset = Math.max(0, Math.min(maxOffset, storedScrollOffset + direction * STORED_COLS * notches));
        return true;
    }

    private boolean handleStoredScrollbarClick(double mouseX, double mouseY) {
        PlayerArchiveData data = playerArchive();
        int maxOffset = maxStoredScrollOffset(data);
        if (maxOffset <= 0) {
            return false;
        }
        int scrollX = leftPos + STORED_GRID_X + STORED_COLS * CELL_STEP_X + 3;
        int scrollY = topPos + STORED_GRID_Y;
        int trackHeight = STORED_ROWS * CELL_STEP_Y - 2;
        if (!inside(mouseX, mouseY, scrollX - 2, scrollY, 8, trackHeight)) {
            return false;
        }
        double progress = Math.max(0.0D, Math.min(1.0D, (mouseY - scrollY) / Math.max(1.0D, trackHeight - 1.0D)));
        int row = (int) Math.round((maxOffset / (double) STORED_COLS) * progress);
        storedScrollOffset = Math.max(0, Math.min(maxOffset, row * STORED_COLS));
        return true;
    }

    private ItemStack equipmentStack(int index) {
        return switch (index) {
            case 0 -> playerInventory.armor.get(3);
            case 1 -> playerInventory.armor.get(2);
            case 2 -> playerInventory.armor.get(1);
            case 3 -> playerInventory.armor.get(0);
            case 4 -> playerInventory.offhand.getFirst();
            default -> ItemStack.EMPTY;
        };
    }

    private int[] equipmentSlotPosition(int x, int y, int index) {
        return switch (index) {
            case 0 -> new int[] {x, y};
            case 1 -> new int[] {x + EQUIPMENT_STEP, y};
            case 2 -> new int[] {x, y + EQUIPMENT_STEP};
            case 3 -> new int[] {x + EQUIPMENT_STEP, y + EQUIPMENT_STEP};
            case 4 -> new int[] {x + EQUIPMENT_STEP / 2, y + EQUIPMENT_STEP * 2};
            default -> new int[] {x, y};
        };
    }

    private OptionalInt equipmentCellIndex(double mouseX, double mouseY) {
        int baseX = leftPos + EQUIPMENT_X;
        int baseY = topPos + EQUIPMENT_Y;
        for (int i = 0; i < 5; i++) {
            int[] pos = equipmentSlotPosition(baseX, baseY, i);
            if (inside(mouseX, mouseY, pos[0], pos[1], 16, 16)) {
                return OptionalInt.of(i);
            }
        }
        return OptionalInt.empty();
    }

    private OptionalInt storedCellIndex(double mouseX, double mouseY) {
        int gridX = leftPos + STORED_GRID_X;
        int gridY = topPos + STORED_GRID_Y;
        int localX = (int) Math.floor(mouseX - gridX);
        int localY = (int) Math.floor(mouseY - gridY);
        if (localX < 0 || localY < 0) {
            return OptionalInt.empty();
        }
        int col = localX / CELL_STEP_X;
        int row = localY / CELL_STEP_Y;
        if (col < 0 || col >= STORED_COLS || row < 0 || row >= STORED_ROWS) {
            return OptionalInt.empty();
        }
        int inCellX = localX - col * CELL_STEP_X;
        int inCellY = localY - row * CELL_STEP_Y;
        if (inCellX >= CELL_W || inCellY >= CELL_H) {
            return OptionalInt.empty();
        }
        return OptionalInt.of(row * STORED_COLS + col);
    }

    private boolean inside(double mouseX, double mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseY >= y && mouseX < x + width && mouseY < y + height;
    }
}
