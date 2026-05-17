package com.swvague.ars_odyssey.client.gui.spellbook;

import com.swvague.ars_odyssey.config.OdysseyConfig;
import com.swvague.ars_odyssey.glyph.AugmentOrbitSelf;
import com.swvague.ars_odyssey.item.OdysseySpellBook;
import com.swvague.ars_odyssey.knowledge.PlayerKnowledgeAttachments;
import com.swvague.ars_odyssey.knowledge.PlayerKnowledgeData;
import com.swvague.ars_odyssey.knowledge.complexity.GlyphComplexityCalculator;
import com.swvague.ars_odyssey.knowledge.complexity.TruthEntanglementCalculator;
import com.swvague.ars_odyssey.knowledge.truth.AugmentTruthificationResolver;
import com.swvague.ars_odyssey.knowledge.truth.SpellTruthificationKey;
import com.swvague.ars_odyssey.network.ModNetwork;
import com.hollingsworth.arsnouveau.api.spell.AbstractEffect;
import com.hollingsworth.arsnouveau.api.spell.AbstractSpellPart;
import com.hollingsworth.arsnouveau.client.gui.buttons.CraftingButton;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@OnlyIn(Dist.CLIENT)
public final class SpellBookTruthificationUi {
    private static final int CHECKBOX_SIZE = 8;
    private static final int GOLD = 0xFFFFD34D;
    private static final int GOLD_DARK = 0xFF9A6E16;
    private static final int TEXT = 0xFF6A5630;
    private static final int DISABLED_FILL = 0xFFE1D9BE;
    private static final int DISABLED_BORDER = 0xFF9B9277;
    private static final int DISABLED_MARK = 0xFFB8AF91;

    public boolean handleClick(
            double mouseX,
            double mouseY,
            int button,
            boolean enabled,
            List<CraftingButton> craftingCells,
            List<AbstractSpellPart> spell
    ) {
        if (button != 0
                || !enabled
                || craftingCells == null
                || spell == null
                || !OdysseyConfig.ENABLE_TRUTHIFICATION.get()) {
            return false;
        }

        for (CraftingButton buttonWidget : craftingCells) {
            ResourceLocation glyphId = truthifiableGlyphAt(buttonWidget, spell);
            if (glyphId == null) {
                continue;
            }
            int boxX = checkboxX(buttonWidget);
            int boxY = checkboxY(buttonWidget);
            if (isInside(mouseX, mouseY, boxX, boxY, CHECKBOX_SIZE, CHECKBOX_SIZE)) {
                if (!isTruthificationUnlocked(glyphId, buttonWidget.slotNum, spell)) {
                    return true;
                }
                String key = truthificationKey(buttonWidget.slotNum, glyphId, spell);
                boolean next = !isTruthifiedSlot(key);
                setLocalTruthifiedSlot(key, next);
                ModNetwork.setTruthifiedSpellSlot(glyphId, key, next);
                return true;
            }
        }

        List<ResourceLocation> truthifiableGlyphs = truthifiableGlyphs(spell);
        if (!truthifiableGlyphs.isEmpty()
                && !hasVisibleTruthifiableCell(craftingCells, spell)
                && isInside(mouseX, mouseY, fallbackCheckboxX(craftingCells, Minecraft.getInstance().font.width(Component.translatable("ars_odyssey.truthification.title"))), fallbackCheckboxY(craftingCells), CHECKBOX_SIZE, CHECKBOX_SIZE)) {
            ResourceLocation glyphId = truthifiableGlyphs.getFirst();
            int slot = firstTruthifiableSlot(spell);
            if (!isTruthificationUnlocked(glyphId, slot, spell)) {
                return true;
            }
            String key = truthificationKey(slot, glyphId, spell);
            boolean next = !isTruthifiedSlot(key);
            setLocalTruthifiedSlot(key, next);
            ModNetwork.setTruthifiedSpellSlot(glyphId, key, next);
            return true;
        }

        return false;
    }

    public void render(
            GuiGraphics graphics,
            Font font,
            boolean enabled,
            int x,
            int y
    ) {
        if (!enabled) {
            return;
        }

        Component total = Component.translatable(
                "ars_odyssey.spellbook.stats.total_entanglement",
                formatEntanglement(getTotalEntanglement()));
        graphics.drawString(font, total, x, y, TEXT, false);
    }

    public static Component totalEntanglementText() {
        return Component.translatable(
                "ars_odyssey.spellbook.stats.total_entanglement",
                formatEntanglement(getTotalEntanglement()));
    }

    public static Component spellComplexityText(List<AbstractSpellPart> spell, ItemStack bookStack) {
        return Component.translatable(
                "ars_odyssey.spellbook.stats.spell_complexity",
                formatOneDecimal(spellComplexity(spell, OdysseySpellBook.complexityTierBonus(bookStack))));
    }

    public static Component orbitManaDrainText(List<AbstractSpellPart> spell) {
        if (spell == null || !OdysseyConfig.ENABLE_TRUTHIFICATION.get()) {
            return Component.empty();
        }
        int enabledOrbitCount = 0;
        for (int slot = 0; slot < spell.size(); slot++) {
            AbstractSpellPart part = spell.get(slot);
            if (!isOrbitSelf(part)) {
                continue;
            }
            String key = truthificationKey(slot, part.getRegistryName(), spell);
            if (isTruthifiedSlot(key) && isTruthificationUnlocked(part.getRegistryName(), slot, spell)) {
                enabledOrbitCount++;
            }
        }
        if (enabledOrbitCount <= 0) {
            return Component.empty();
        }
        if (com.swvague.ars_odyssey.knowledge.truth.TruthificationInsightRules.hasOrbitSelfCompleteInsight(getTotalEntanglement())) {
            return Component.translatable(
                    "ars_odyssey.spellbook.stats.truthified_mana_percent_suffix",
                    formatPercent(stackedPercent(OdysseyConfig.ORBIT_MANA_DRAIN_PERCENT.get(), enabledOrbitCount)));
        }
        return Component.translatable(
                "ars_odyssey.spellbook.stats.truthified_mana_flat_suffix",
                formatEntanglement(OdysseyConfig.ORBIT_MIN_MANA_DRAIN.get()));
    }

    public static Boolean truthificationEnabledForSlot(int slot, ResourceLocation glyphId, List<AbstractSpellPart> spell) {
        if (glyphId == null
                || spell == null
                || slot < 0
                || slot >= spell.size()
                || !isOrbitSelf(spell.get(slot))
                || !glyphId.equals(spell.get(slot).getRegistryName())) {
            return null;
        }
        String key = truthificationKey(slot, glyphId, spell);
        return isTruthifiedSlot(key) && isTruthificationUnlocked(glyphId, slot, spell);
    }

    public void renderCheckboxes(
            GuiGraphics graphics,
            Font font,
            boolean enabled,
            List<CraftingButton> craftingCells,
            List<AbstractSpellPart> spell
    ) {
        if (!enabled || craftingCells == null || spell == null || !OdysseyConfig.ENABLE_TRUTHIFICATION.get()) {
            return;
        }
        List<ResourceLocation> truthifiableGlyphs = truthifiableGlyphs(spell);
        if (truthifiableGlyphs.isEmpty()) {
            return;
        }

        // The "Truthify" label text is intentionally not drawn; only the
        // checkbox boxes are shown. labelWidth is still needed to position the
        // fallback checkbox in the same spot the label used to occupy.
        int labelWidth = font.width(Component.translatable("ars_odyssey.truthification.title"));

        boolean drewSlotCheckbox = false;
        for (CraftingButton buttonWidget : craftingCells) {
            ResourceLocation glyphId = truthifiableGlyphAt(buttonWidget, spell);
            if (glyphId != null) {
                String key = truthificationKey(buttonWidget.slotNum, glyphId, spell);
                drawCheckbox(graphics, checkboxX(buttonWidget), checkboxY(buttonWidget),
                        isTruthifiedSlot(key),
                        isTruthificationUnlocked(glyphId, buttonWidget.slotNum, spell));
                drewSlotCheckbox = true;
            }
        }
        if (!drewSlotCheckbox) {
            ResourceLocation glyphId = truthifiableGlyphs.getFirst();
            int slot = firstTruthifiableSlot(spell);
            String key = truthificationKey(slot, glyphId, spell);
            drawCheckbox(graphics, fallbackCheckboxX(craftingCells, labelWidth), fallbackCheckboxY(craftingCells),
                    isTruthifiedSlot(key), isTruthificationUnlocked(glyphId, slot, spell));
        }
    }

    public static List<ResourceLocation> truthifiableGlyphs(List<AbstractSpellPart> spell) {
        Set<AbstractSpellPart> parts = new LinkedHashSet<>();
        for (AbstractSpellPart part : spell) {
            if (part != null && isOrbitSelf(part)) {
                parts.add(part);
            }
        }
        return parts.stream().map(AbstractSpellPart::getRegistryName).toList();
    }

    private static ResourceLocation truthifiableGlyphAt(CraftingButton craftingButton, List<AbstractSpellPart> spell) {
        if (craftingButton == null
                || !craftingButton.visible
                || spell == null
                || craftingButton.slotNum < 0
                || craftingButton.slotNum >= spell.size()) {
            return null;
        }
        AbstractSpellPart part = spell.get(craftingButton.slotNum);
        return isOrbitSelf(part) ? part.getRegistryName() : null;
    }

    private static boolean isOrbitSelf(AbstractSpellPart part) {
        return part != null && AugmentOrbitSelf.INSTANCE.getRegistryName().equals(part.getRegistryName());
    }

    /**
     * Returns the highest complexity among all AbstractEffect parts in the given spell.
     * Public so that other UI classes (e.g., GuiSpellBookMixin) can reuse this calculation.
     */
    public static double spellComplexity(List<AbstractSpellPart> spell) {
        return spellComplexity(spell, 0);
    }

    public static double spellComplexity(List<AbstractSpellPart> spell, int tierBonus) {
        if (spell == null) return 1.0;
        double complexity = 1.0D;
        for (AbstractSpellPart part : spell) {
            if (part instanceof AbstractEffect && part.getRegistryName() != null) {
                complexity = Math.max(complexity,
                        GlyphComplexityCalculator.calculate(spell, part.getRegistryName(), tierBonus).highestComplexity());
            }
        }
        return complexity;
    }

    private static boolean isTruthified(ResourceLocation glyphId) {
        if (Minecraft.getInstance().player == null || glyphId == null) {
            return false;
        }
        PlayerKnowledgeData data = Minecraft.getInstance().player.getData(PlayerKnowledgeAttachments.PLAYER_KNOWLEDGE);
        return data != null && data.isTruthified(glyphId);
    }

    private static boolean isTruthifiedSlot(String key) {
        if (Minecraft.getInstance().player == null || key == null || key.isBlank()) {
            return false;
        }
        PlayerKnowledgeData data = Minecraft.getInstance().player.getData(PlayerKnowledgeAttachments.PLAYER_KNOWLEDGE);
        return data != null && data.isTruthifiedSpellSlot(key);
    }

    private static void setLocalTruthified(ResourceLocation glyphId, boolean enabled) {
        if (Minecraft.getInstance().player == null || glyphId == null) {
            return;
        }
        PlayerKnowledgeData data = Minecraft.getInstance().player.getData(PlayerKnowledgeAttachments.PLAYER_KNOWLEDGE);
        if (data != null) {
            data.setTruthified(glyphId, enabled);
            Minecraft.getInstance().player.setData(PlayerKnowledgeAttachments.PLAYER_KNOWLEDGE, data);
        }
    }

    private static void setLocalTruthifiedSlot(String key, boolean enabled) {
        if (Minecraft.getInstance().player == null || key == null || key.isBlank()) {
            return;
        }
        PlayerKnowledgeData data = Minecraft.getInstance().player.getData(PlayerKnowledgeAttachments.PLAYER_KNOWLEDGE);
        if (data != null) {
            data.setTruthifiedSpellSlot(key, enabled);
            Minecraft.getInstance().player.setData(PlayerKnowledgeAttachments.PLAYER_KNOWLEDGE, data);
        }
    }

    private static String truthificationKey(int slot, ResourceLocation glyphId, List<AbstractSpellPart> spell) {
        return SpellTruthificationKey.forSlot(spell, slot, glyphId);
    }

    private static int firstTruthifiableSlot(List<AbstractSpellPart> spell) {
        if (spell == null) {
            return -1;
        }
        for (int i = 0; i < spell.size(); i++) {
            if (isOrbitSelf(spell.get(i))) {
                return i;
            }
        }
        return -1;
    }

    private static double getTotalEntanglement() {
        if (Minecraft.getInstance().player == null) return 0.0D;
        PlayerKnowledgeData data = Minecraft.getInstance().player.getData(PlayerKnowledgeAttachments.PLAYER_KNOWLEDGE);
        return TruthEntanglementCalculator.total(data, Minecraft.getInstance().level);
    }

    private static String formatEntanglement(double value) {
        if (Math.abs(value - Math.rint(value)) < 0.005D) {
            return Long.toString(Math.round(value));
        }
        return String.format(java.util.Locale.ROOT, "%.2f", value);
    }

    private static String formatOneDecimal(double value) {
        return String.format(java.util.Locale.ROOT, "%.1f", value);
    }

    private static String formatPercent(double value) {
        return String.format(java.util.Locale.ROOT, "%.0f%%", value * 100.0D);
    }

    private static double stackedPercent(double basePercent, int stacks) {
        double safeBase = Math.max(0.0D, Math.min(1.0D, basePercent));
        int safeStacks = Math.max(1, stacks);
        return 1.0D - Math.pow(1.0D - safeBase, safeStacks);
    }

    private static boolean isTruthificationUnlocked(ResourceLocation glyphId, int slot, List<AbstractSpellPart> spell) {
        if (Minecraft.getInstance().player == null || glyphId == null) {
            return false;
        }
        PlayerKnowledgeData data = Minecraft.getInstance().player.getData(PlayerKnowledgeAttachments.PLAYER_KNOWLEDGE);
        if (AugmentOrbitSelf.INSTANCE.getRegistryName().equals(glyphId)) {
            return AugmentTruthificationResolver.orbitSelfUnlocked(data, spell, slot, Minecraft.getInstance().level);
        }
        return getTotalEntanglement() >= requiredEntanglementFor(glyphId);
    }

    private static double requiredEntanglementFor(ResourceLocation glyphId) {
        if (AugmentOrbitSelf.INSTANCE.getRegistryName().equals(glyphId)) {
            return OdysseyConfig.ORBIT_SELF_TRUTHIFICATION_REQUIRED_ENTANGLEMENT.get();
        }
        return OdysseyConfig.TRUTHIFICATION_REQUIRED_ENTANGLEMENT.get();
    }

    private static void drawCheckbox(GuiGraphics graphics, int x, int y, boolean checked, boolean enabled) {
        graphics.fill(x, y, x + CHECKBOX_SIZE, y + CHECKBOX_SIZE, enabled ? 0xFFE8D9A5 : DISABLED_FILL);
        if (enabled) {
            drawGoldBorder(graphics, x, y, CHECKBOX_SIZE, CHECKBOX_SIZE);
        } else {
            drawDisabledBorder(graphics, x, y, CHECKBOX_SIZE, CHECKBOX_SIZE);
        }
        if (checked) {
            graphics.fill(x + 2, y + 2, x + CHECKBOX_SIZE - 2, y + CHECKBOX_SIZE - 2, enabled ? GOLD : DISABLED_MARK);
        }
    }

    private static void drawGoldBorder(GuiGraphics graphics, int x, int y, int width, int height) {
        graphics.fill(x, y, x + width, y + 1, GOLD);
        graphics.fill(x, y + height - 1, x + width, y + height, GOLD_DARK);
        graphics.fill(x, y, x + 1, y + height, GOLD);
        graphics.fill(x + width - 1, y, x + width, y + height, GOLD_DARK);
    }

    private static void drawDisabledBorder(GuiGraphics graphics, int x, int y, int width, int height) {
        graphics.fill(x, y, x + width, y + 1, DISABLED_BORDER);
        graphics.fill(x, y + height - 1, x + width, y + height, DISABLED_BORDER);
        graphics.fill(x, y, x + 1, y + height, DISABLED_BORDER);
        graphics.fill(x + width - 1, y, x + width, y + height, DISABLED_BORDER);
    }

    private static int checkboxX(CraftingButton craftingButton) {
        return craftingButton.getX() + (craftingButton.getWidth() - CHECKBOX_SIZE) / 2;
    }

    private static int checkboxY(CraftingButton craftingButton) {
        return craftingButton.getY() + craftingButton.getHeight() + 2;
    }

    private static boolean hasVisibleTruthifiableCell(List<CraftingButton> craftingCells, List<AbstractSpellPart> spell) {
        for (CraftingButton buttonWidget : craftingCells) {
            if (truthifiableGlyphAt(buttonWidget, spell) != null) {
                return true;
            }
        }
        return false;
    }

    private static int fallbackCheckboxX(List<CraftingButton> craftingCells, int labelWidth) {
        int labelX = craftingCells.stream().mapToInt(CraftingButton::getX).min().orElse(0);
        return labelX + labelWidth + 6;
    }

    private static int fallbackCheckboxY(List<CraftingButton> craftingCells) {
        return craftingCells.stream()
                .mapToInt(button -> button.getY() + button.getHeight() + 2)
                .min()
                .orElse(0);
    }

    private static boolean isInside(double mouseX, double mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }
}
