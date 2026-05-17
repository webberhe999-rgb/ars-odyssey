package com.swvague.ars_odyssey;

import com.swvague.ars_odyssey.api.event.OdysseyGlyphRuleEvent;
import com.swvague.ars_odyssey.archive.ArchiveCoreChunkLoading;
import com.swvague.ars_odyssey.archive.PlayerArchiveAttachments;
import com.swvague.ars_odyssey.archive.PlayerArchiveData;
import com.swvague.ars_odyssey.client.OdysseyKeyBindings;
import com.swvague.ars_odyssey.client.gui.archive.ArchiveTerminalScreen;
import com.swvague.ars_odyssey.client.gui.nexus.NexusConfigScreen;
import com.swvague.ars_odyssey.client.renderer.OdysseyEntityRenderers;
import com.swvague.ars_odyssey.client.tooltip.GlyphItemTooltipHandler;
import com.swvague.ars_odyssey.knowledge.GlyphRelation;
import com.swvague.ars_odyssey.knowledge.complexity.EntanglementConfidenceMultiplier;
import com.swvague.ars_odyssey.command.ArsOdysseyCommands;
import com.swvague.ars_odyssey.config.OdysseyConfig;
import com.swvague.ars_odyssey.index.GlyphApplicationIndex;
import com.swvague.ars_odyssey.index.GlyphDebugIndex;
import com.swvague.ars_odyssey.knowledge.PlayerKnowledgeAttachments;
import com.swvague.ars_odyssey.knowledge.PlayerKnowledgeData;
import com.swvague.ars_odyssey.knowledge.discovery.entity.EntityEffectObservationHandler;
import com.swvague.ars_odyssey.knowledge.discovery.entity.EntityProductionObservationHandler;
import com.swvague.ars_odyssey.network.ModNetwork;
import com.swvague.ars_odyssey.registry.ModRegistry;
import com.swvague.ars_odyssey.registry.OdysseyGlyphRegistry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(ArsOdyssey.MODID)
public class ArsOdyssey {
    public static final String MODID = "ars_odyssey";

    private static final Logger LOGGER = LogManager.getLogger();
    private static final boolean DEBUG_PRINT_GLYPH_INDEX = Boolean.getBoolean("ars_odyssey.debugGlyphIndex");

    public ArsOdyssey(IEventBus modEventBus, ModContainer modContainer) {
        OdysseyGlyphRegistry.registerGlyphs();
        ModRegistry.registerRegistries(modEventBus);
        PlayerKnowledgeAttachments.ATTACHMENTS.register(modEventBus);
        PlayerArchiveAttachments.ATTACHMENTS.register(modEventBus);
        modContainer.registerConfig(ModConfig.Type.COMMON, OdysseyConfig.SPEC);

        modEventBus.addListener(this::setup);
        modEventBus.addListener(this::doClientStuff);
        modEventBus.addListener(ModNetwork::register);
        modEventBus.addListener(ModRegistry::registerEntityAttributes);
        modEventBus.addListener(OdysseyEntityRenderers::registerEntityRenderers);
        modEventBus.addListener(OdysseyKeyBindings::onRegisterKeyMappings);
        modEventBus.addListener(ArchiveTerminalScreen::registerScreens);
        modEventBus.addListener(NexusConfigScreen::registerScreens);
        NeoForge.EVENT_BUS.register(this);
        NeoForge.EVENT_BUS.register(EntityEffectObservationHandler.INSTANCE);
        NeoForge.EVENT_BUS.register(EntityProductionObservationHandler.INSTANCE);
    }

    public static ResourceLocation prefix(String path) {
        return ResourceLocation.fromNamespaceAndPath(MODID, path);
    }

    private void setup(final FMLCommonSetupEvent event) {
        // Let addon mods register their glyph rules during setup.
        // enqueueWork ensures this runs on the main thread after all mods have loaded.
        event.enqueueWork(() -> {
            OdysseyGlyphRuleEvent ruleEvent = new OdysseyGlyphRuleEvent();
            NeoForge.EVENT_BUS.post(ruleEvent);
            GlyphApplicationIndex.registerExternalRules(ruleEvent.getCollectedRules());
            LOGGER.info("[Ars Odyssey] Registered {} external glyph rule(s) from addon mods.",
                    ruleEvent.getCollectedRules().size());
        });
    }

    private void doClientStuff(final FMLClientSetupEvent event) {
        if (DEBUG_PRINT_GLYPH_INDEX) {
            event.enqueueWork(GlyphDebugIndex::printAllGlyphs);
        }
        NeoForge.EVENT_BUS.addListener(GlyphItemTooltipHandler::onItemTooltip);
        NeoForge.EVENT_BUS.addListener(OdysseyKeyBindings::onClientTick);
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        ArsOdysseyCommands.register(event.getDispatcher());
    }

    @SubscribeEvent
    public void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        PlayerKnowledgeData data = player.getData(PlayerKnowledgeAttachments.PLAYER_KNOWLEDGE);

        // Retroactively populate entanglement for players who had discoveries before the
        // entanglement feature was introduced (totalTruthEntanglement defaults to 0 on old saves).
        if (data.getTotalTruthEntanglement() == 0.0 && !data.getDiscoveredRelations().isEmpty()) {
            double retroactive = data.getDiscoveredRelations().stream()
                    .filter(r -> r.glyphId() != null)
                    .mapToDouble(r -> {
                        double complexity = data.getComplexityRecord(r.glyphId()).highestComplexity();
                        double multiplier = EntanglementConfidenceMultiplier.forGlyph(r.glyphId());
                        return complexity * multiplier;
                    })
                    .sum();
            if (retroactive > 0) {
                data.addTruthEntanglement(retroactive);
                player.setData(PlayerKnowledgeAttachments.PLAYER_KNOWLEDGE, data);
                LOGGER.info("[Ars Odyssey] Retroactively added {} truth entanglement for {} (had {} discoveries).",
                        retroactive, player.getName().getString(), data.getDiscoveredRelations().size());
            }
        }

        ModNetwork.syncKnowledge(player, data);
        PlayerArchiveData archiveData = player.getData(PlayerArchiveAttachments.PLAYER_ARCHIVE);
        ArchiveCoreChunkLoading.ensureForced(player.server, archiveData);
        ModNetwork.syncArchive(player, archiveData);
    }
}
