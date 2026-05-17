package com.swvague.ars_odyssey.client.gui.nexus;

import com.swvague.ars_odyssey.block.ResonanceNexusCoreBlock;
import com.swvague.ars_odyssey.block.menu.NexusConfigMenu;
import com.swvague.ars_odyssey.network.ModNetwork;
import com.swvague.ars_odyssey.registry.ModRegistry;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

public class NexusConfigScreen extends AbstractContainerScreen<NexusConfigMenu> {
    private static final int PANEL_W = 214;
    private static final int PANEL_H = 154;
    private static final int TEXT = 0xFF4F4026;
    private static final int MUTED = 0xFF806D48;
    private static final int LINE = 0xFFB89342;
    private static final int TEAL = 0xFF2FAEAA;
    private static final int DEEP = 0xFF21182B;

    private Button particleButton;
    private Button previousTierButton;
    private Button nextTierButton;

    public NexusConfigScreen(NexusConfigMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = PANEL_W;
        imageHeight = PANEL_H;
        titleLabelX = 14;
        titleLabelY = 12;
        inventoryLabelY = 10000;
    }

    public static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(ModRegistry.NEXUS_CONFIG_MENU.get(), NexusConfigScreen::new);
    }

    @Override
    protected void init() {
        super.init();
        int x = leftPos + 18;
        int y = topPos + 48;
        particleButton = addRenderableWidget(Button.builder(Component.empty(), button -> {
            NexusState state = nexusState();
            sendConfig(state.tier(), !state.particles());
        }).bounds(x, y, 178, 18).build());

        previousTierButton = addRenderableWidget(Button.builder(Component.literal("-"), button -> {
            NexusState state = nexusState();
            sendConfig(Math.max(0, state.tier() - 1), state.particles());
        }).bounds(x, y + 44, 28, 18).build());

        nextTierButton = addRenderableWidget(Button.builder(Component.literal("+"), button -> {
            NexusState state = nexusState();
            sendConfig(Math.min(ResonanceNexusCoreBlock.FINAL_TIER, state.tier() + 1), state.particles());
        }).bounds(x + 150, y + 44, 28, 18).build());

        for (int i = 0; i <= ResonanceNexusCoreBlock.FINAL_TIER; i++) {
            final int tier = i;
            addRenderableWidget(Button.builder(Component.literal(Integer.toString(i)), button -> {
                NexusState state = nexusState();
                sendConfig(tier, state.particles());
            }).bounds(x + i * 28, y + 70, 24, 18).build());
        }
        refreshButtons();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        refreshButtons();
        renderBackground(graphics, mouseX, mouseY, partialTicks);
        super.render(graphics, mouseX, mouseY, partialTicks);
        renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTicks, int mouseX, int mouseY) {
        drawPanel(graphics, leftPos, topPos, imageWidth, imageHeight);
        NexusState state = nexusState();
        graphics.drawString(font, Component.translatable("screen.ars_odyssey.nexus_config.position",
                menu.centerPos().getX(), menu.centerPos().getY(), menu.centerPos().getZ()),
                leftPos + 14, topPos + 28, MUTED, false);

        graphics.drawString(font, Component.translatable("screen.ars_odyssey.nexus_config.particles"),
                leftPos + 18, topPos + 39, TEXT, false);
        graphics.drawString(font, Component.translatable("screen.ars_odyssey.nexus_config.tier"),
                leftPos + 18, topPos + 81, TEXT, false);
        graphics.drawString(font, tierName(state.tier()),
                leftPos + 54, topPos + 103, tierColor(state.tier()), false);
        drawMeter(graphics, leftPos + 18, topPos + 124, 178, 8,
                state.tier() / (double) ResonanceNexusCoreBlock.FINAL_TIER, tierColor(state.tier()));
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, titleLabelX, titleLabelY, DEEP, false);
    }

    private void refreshButtons() {
        if (particleButton == null || previousTierButton == null || nextTierButton == null) {
            return;
        }
        NexusState state = nexusState();
        particleButton.setMessage(Component.translatable(state.particles()
                ? "screen.ars_odyssey.nexus_config.particles.on"
                : "screen.ars_odyssey.nexus_config.particles.off"));
        previousTierButton.active = state.tier() > 0;
        nextTierButton.active = state.tier() < ResonanceNexusCoreBlock.FINAL_TIER;
    }

    private NexusState nexusState() {
        if (minecraft == null || minecraft.level == null) {
            return new NexusState(0, true);
        }
        BlockState state = minecraft.level.getBlockState(menu.centerPos());
        if (!state.is(ModRegistry.RESONANCE_ARCHIVE_CORE.get())) {
            return new NexusState(0, false);
        }
        return new NexusState(
                state.getValue(ResonanceNexusCoreBlock.TIER),
                state.getValue(ResonanceNexusCoreBlock.PARTICLES));
    }

    private void sendConfig(int tier, boolean particles) {
        ModNetwork.updateNexusConfig(menu.centerPos(), tier, particles);
    }

    private Component tierName(int tier) {
        String suffix = switch (tier) {
            case 4 -> "law";
            case 5 -> "final";
            default -> Integer.toString(tier);
        };
        return Component.translatable("screen.ars_odyssey.nexus_config.tier." + suffix);
    }

    private int tierColor(int tier) {
        return switch (tier) {
            case 4 -> 0xFF8E46D6;
            case 5 -> 0xFF18CDE0;
            default -> TEAL;
        };
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

    private void drawMeter(GuiGraphics graphics, int x, int y, int width, int height, double fill, int color) {
        graphics.fill(x, y, x + width, y + height, 0xFF6C5430);
        graphics.fill(x + 1, y + 1, x + width - 1, y + height - 1, 0xFFE6D49C);
        int filled = (int) Math.round((width - 2) * Math.max(0.0D, Math.min(1.0D, fill)));
        graphics.fill(x + 1, y + 1, x + 1 + filled, y + height - 1, color);
    }

    private record NexusState(int tier, boolean particles) {
    }
}
