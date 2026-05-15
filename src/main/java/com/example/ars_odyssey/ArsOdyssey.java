package com.example.ars_odyssey;

import com.example.ars_odyssey.client.tooltip.GlyphItemTooltipHandler;
import com.example.ars_odyssey.command.ArsOdysseyCommands;
import com.example.ars_odyssey.network.ModNetwork;
import com.example.ars_odyssey.registry.ModRegistry;
import com.example.ars_odyssey.index.GlyphDebugIndex;
import com.example.ars_odyssey.knowledge.PlayerKnowledgeAttachments;
import com.example.ars_odyssey.knowledge.PlayerKnowledgeData;
import com.example.ars_odyssey.knowledge.discovery.entity.EntityEffectObservationHandler;
import com.example.ars_odyssey.knowledge.discovery.entity.EntityProductionObservationHandler;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(ArsOdyssey.MODID)
public class ArsOdyssey {
    public static final String MODID = "ars_odyssey";

    private static final Logger LOGGER = LogManager.getLogger();

    public ArsOdyssey(IEventBus modEventBus, ModContainer modContainer) {
        ModRegistry.registerRegistries(modEventBus);
        PlayerKnowledgeAttachments.ATTACHMENTS.register(modEventBus);

        modEventBus.addListener(this::setup);
        modEventBus.addListener(this::doClientStuff);
        modEventBus.addListener(ModNetwork::register);
        NeoForge.EVENT_BUS.register(this);
        NeoForge.EVENT_BUS.register(EntityEffectObservationHandler.INSTANCE);
        NeoForge.EVENT_BUS.register(EntityProductionObservationHandler.INSTANCE);
    }

    public static ResourceLocation prefix(String path) {
        return ResourceLocation.fromNamespaceAndPath(MODID, path);
    }

    private void setup(final FMLCommonSetupEvent event) {
    }

    private void doClientStuff(final FMLClientSetupEvent event) {
        event.enqueueWork(GlyphDebugIndex::printAllGlyphs);
        NeoForge.EVENT_BUS.addListener(GlyphItemTooltipHandler::onItemTooltip);
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        LOGGER.info("HELLO from server starting");
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        ArsOdysseyCommands.register(event.getDispatcher());
    }

    @SubscribeEvent
    public void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            PlayerKnowledgeData data = player.getData(PlayerKnowledgeAttachments.PLAYER_KNOWLEDGE);
            ModNetwork.syncKnowledge(player, data);
        }
    }
}
