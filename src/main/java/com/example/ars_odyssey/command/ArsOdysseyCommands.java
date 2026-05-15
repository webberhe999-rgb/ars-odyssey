package com.example.ars_odyssey.command;

import com.example.ars_odyssey.index.EvidenceConfidence;
import com.example.ars_odyssey.index.GlyphTargetRule;
import com.example.ars_odyssey.knowledge.DiscoverySource;
import com.example.ars_odyssey.knowledge.GlyphRelation;
import com.example.ars_odyssey.knowledge.MatcherTargetResolver;
import com.example.ars_odyssey.knowledge.PlayerKnowledgeAttachments;
import com.example.ars_odyssey.knowledge.PlayerKnowledgeData;
import com.example.ars_odyssey.knowledge.PlayerKnowledgeDataView;
import com.example.ars_odyssey.knowledge.RelationType;
import com.example.ars_odyssey.knowledge.TargetDescriptor;
import com.example.ars_odyssey.knowledge.TargetKind;
import com.example.ars_odyssey.knowledge.TruthDelta;
import com.example.ars_odyssey.knowledge.discovery.GlyphDiscoveryResult;
import com.example.ars_odyssey.knowledge.discovery.GlyphDiscoveryService;
import com.example.ars_odyssey.network.ModNetwork;
import com.hollingsworth.arsnouveau.api.registry.GlyphRegistry;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

import java.util.Locale;
import java.util.Optional;

public final class ArsOdysseyCommands {
    private ArsOdysseyCommands() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("arsodyssey")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("research_complete")
                        .then(Commands.argument("glyph_id", ResourceLocationArgument.id())
                                .then(Commands.argument("target", StringArgumentType.greedyString())
                                        .executes(ArsOdysseyCommands::completeResearch))))
                .then(Commands.literal("debug_resolve_matcher")
                        .then(Commands.argument("glyph_id", ResourceLocationArgument.id())
                                .then(Commands.argument("matcher_type", StringArgumentType.word())
                                        .then(Commands.argument("matcher_value", StringArgumentType.greedyString())
                                                .executes(ArsOdysseyCommands::debugResolveMatcher)))))
                .then(Commands.literal("research_forget")
                        .then(Commands.argument("glyph_id", ResourceLocationArgument.id())
                                .then(Commands.argument("target", StringArgumentType.greedyString())
                                        .executes(ArsOdysseyCommands::forgetResearch))))
                .then(Commands.literal("debug_forget_matcher")
                        .then(Commands.argument("glyph_id", ResourceLocationArgument.id())
                                .then(Commands.argument("matcher_type", StringArgumentType.word())
                                        .then(Commands.argument("matcher_value", StringArgumentType.greedyString())
                                                .executes(ArsOdysseyCommands::debugForgetMatcher))))));
    }

    private static int completeResearch(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ResourceLocation glyphId = parseGlyphId(context, ResourceLocationArgument.getId(context, "glyph_id"));
        if (glyphId == null) {
            return 0;
        }

        String targetText = StringArgumentType.getString(context, "target");
        Optional<TargetDescriptor> target = parseTarget(targetText);
        if (target.isEmpty() || target.get().kind() == TargetKind.UNKNOWN) {
            context.getSource().sendFailure(Component.translatable("ars_odyssey.command.unknown_target", targetText));
            return 0;
        }

        return applyResearch(context, glyphId, target.get(), targetText);
    }

    private static int debugResolveMatcher(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ResourceLocation glyphId = parseGlyphId(context, ResourceLocationArgument.getId(context, "glyph_id"));
        if (glyphId == null) {
            return 0;
        }

        String typeText = StringArgumentType.getString(context, "matcher_type");
        String value = StringArgumentType.getString(context, "matcher_value");
        GlyphTargetRule.MatcherType matcherType;
        try {
            matcherType = GlyphTargetRule.MatcherType.valueOf(typeText.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            context.getSource().sendFailure(Component.translatable("ars_odyssey.command.unknown_matcher_type", typeText));
            return 0;
        }

        Optional<TargetDescriptor> target = MatcherTargetResolver.fromMatcher(new GlyphTargetRule.TargetMatcher(matcherType, value));
        if (target.isEmpty()) {
            context.getSource().sendFailure(Component.translatable("ars_odyssey.command.unknown_target", value));
            return 0;
        }

        return applyResearch(context, glyphId, target.get(), matcherType + " " + value);
    }

    private static int forgetResearch(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ResourceLocation glyphId = parseGlyphId(context, ResourceLocationArgument.getId(context, "glyph_id"));
        if (glyphId == null) {
            return 0;
        }

        String targetText = StringArgumentType.getString(context, "target");
        Optional<TargetDescriptor> target = parseTarget(targetText);
        if (target.isEmpty() || target.get().kind() == TargetKind.UNKNOWN) {
            context.getSource().sendFailure(Component.translatable("ars_odyssey.command.unknown_target", targetText));
            return 0;
        }

        return forgetResearch(context, glyphId, target.get());
    }

    private static int debugForgetMatcher(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ResourceLocation glyphId = parseGlyphId(context, ResourceLocationArgument.getId(context, "glyph_id"));
        if (glyphId == null) {
            return 0;
        }

        String typeText = StringArgumentType.getString(context, "matcher_type");
        String value = StringArgumentType.getString(context, "matcher_value");
        GlyphTargetRule.MatcherType matcherType;
        try {
            matcherType = GlyphTargetRule.MatcherType.valueOf(typeText.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            context.getSource().sendFailure(Component.translatable("ars_odyssey.command.unknown_matcher_type", typeText));
            return 0;
        }

        Optional<TargetDescriptor> target = MatcherTargetResolver.fromMatcher(new GlyphTargetRule.TargetMatcher(matcherType, value));
        if (target.isEmpty()) {
            context.getSource().sendFailure(Component.translatable("ars_odyssey.command.unknown_target", value));
            return 0;
        }

        return forgetResearch(context, glyphId, target.get());
    }

    private static int applyResearch(CommandContext<CommandSourceStack> context, ResourceLocation glyphId, TargetDescriptor target, String targetText)
            throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        PlayerKnowledgeData data = player.getData(PlayerKnowledgeAttachments.PLAYER_KNOWLEDGE);
        GlyphRelation relation = new GlyphRelation(
                glyphId,
                RelationType.APPLIES_TO,
                target,
                Optional.empty(),
                "command_research_complete"
        );
        GlyphDiscoveryResult result = GlyphDiscoveryService.resolveRelation(
                relation,
                DiscoverySource.STRUCTURE_RESEARCH,
                EvidenceConfidence.HIGH,
                new TruthDelta(0, "command_research_complete")
        );
        boolean added = GlyphDiscoveryService.applyDiscovery(data, result);
        player.setData(PlayerKnowledgeAttachments.PLAYER_KNOWLEDGE, data);
        ModNetwork.syncKnowledge(player, data);
        Component glyphName = glyphDisplay(glyphId);
        Component targetName = targetDisplay(target);
        Component summary = glyphSummary(data, glyphId, player);
        if (added) {
            context.getSource().sendSuccess(() -> Component.translatable(
                    "ars_odyssey.command.research.completed",
                    glyphName,
                    targetName,
                    summary), false);
            return 1;
        }

        context.getSource().sendSuccess(() -> Component.translatable(
                "ars_odyssey.command.research.already_known",
                glyphName,
                targetName,
                summary), false);
        return 0;
    }

    private static int forgetResearch(CommandContext<CommandSourceStack> context, ResourceLocation glyphId, TargetDescriptor target)
            throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        PlayerKnowledgeData data = player.getData(PlayerKnowledgeAttachments.PLAYER_KNOWLEDGE);
        boolean removed = data.forget(glyphId, target);
        player.setData(PlayerKnowledgeAttachments.PLAYER_KNOWLEDGE, data);
        ModNetwork.syncKnowledge(player, data);

        Component glyphName = glyphDisplay(glyphId);
        Component targetName = targetDisplay(target);
        Component summary = glyphSummary(data, glyphId, player);
        if (removed) {
            context.getSource().sendSuccess(() -> Component.translatable(
                    "ars_odyssey.command.research.forgot",
                    glyphName,
                    targetName,
                    summary), false);
            return 1;
        }

        context.getSource().sendSuccess(() -> Component.translatable(
                "ars_odyssey.command.research.not_known",
                glyphName,
                targetName,
                summary), false);
        return 0;
    }

    private static ResourceLocation parseGlyphId(CommandContext<CommandSourceStack> context, ResourceLocation glyphId) {
        if (glyphId == null || !GlyphRegistry.getSpellpartMap().containsKey(glyphId)) {
            context.getSource().sendFailure(Component.translatable("ars_odyssey.command.unknown_glyph", String.valueOf(glyphId)));
            return null;
        }
        return glyphId;
    }

    private static Component glyphDisplay(ResourceLocation glyphId) {
        return Optional.ofNullable(GlyphRegistry.getSpellpartMap().get(glyphId))
                .map(part -> Component.literal(part.getLocaleName()))
                .orElseGet(() -> Component.literal(glyphId.toString()));
    }

    private static Component glyphSummary(PlayerKnowledgeData data, ResourceLocation glyphId, ServerPlayer player) {
        PlayerKnowledgeDataView view = new PlayerKnowledgeDataView(data, player.level());
        return Component.translatable("ars_odyssey.command.summary.count", view.countResolvedRelations(glyphId));
    }

    private static Component targetDisplay(TargetDescriptor target) {
        if (target == null) {
            return Component.literal("");
        }

        if (target.id() != null) {
            return switch (target.kind()) {
                case ENTITY_TYPE -> Optional.ofNullable(BuiltInRegistries.ENTITY_TYPE.get(target.id()))
                        .filter(entityType -> BuiltInRegistries.ENTITY_TYPE.getKey(entityType).equals(target.id()))
                        .map(EntityType::getDescription)
                        .orElseGet(() -> Component.literal(target.id().toString()));
                case BLOCK -> Optional.ofNullable(BuiltInRegistries.BLOCK.get(target.id()))
                        .filter(block -> BuiltInRegistries.BLOCK.getKey(block).equals(target.id()))
                        .map(Block::getName)
                        .orElseGet(() -> Component.literal(target.id().toString()));
                case ITEM -> Optional.ofNullable(BuiltInRegistries.ITEM.get(target.id()))
                        .filter(item -> BuiltInRegistries.ITEM.getKey(item).equals(target.id()))
                        .map(Item::getDescription)
                        .orElseGet(() -> Component.literal(target.id().toString()));
                case BLOCK_TAG, ITEM_TAG, ENTITY_TYPE_TAG -> Component.literal("#" + target.id());
                default -> Component.literal(target.id().toString());
            };
        }

        if (target.kind() == TargetKind.BEHAVIOR) {
            return behaviorDisplay(target.detail());
        }
        if (target.kind() == TargetKind.POSITION) {
            return Component.translatable("ars_odyssey.target.position_predicate." + target.detail());
        }
        return Component.literal(target.detail());
    }

    private static Component behaviorDisplay(String detail) {
        if (detail == null || detail.isBlank()) {
            return Component.literal("");
        }
        if (detail.startsWith("entity_class:")) {
            return Component.translatable("ars_odyssey.target.entity_class." + simpleName(detail.substring("entity_class:".length())));
        }
        if (detail.startsWith("block_class:")) {
            return Component.translatable("ars_odyssey.target.block_class." + simpleName(detail.substring("block_class:".length())));
        }
        if (detail.equals("general_entity")) {
            return Component.translatable("ars_odyssey.target.entity_class.Entity");
        }
        if (detail.equals("general_block")) {
            return Component.translatable("ars_odyssey.target.general_block");
        }
        return Component.translatable("ars_odyssey.target.behavior." + detail);
    }

    private static String simpleName(String value) {
        int lastDot = value == null ? -1 : value.lastIndexOf('.');
        return lastDot >= 0 ? value.substring(lastDot + 1) : value;
    }

    private static Optional<TargetDescriptor> parseTarget(String text) {
        if (text == null || text.isBlank()) {
            return Optional.empty();
        }

        String normalized = text.trim().toLowerCase(Locale.ROOT);
        if (normalized.startsWith("#")) {
            return parseTagTarget(normalized.substring(1));
        }

        Optional<ResourceLocation> id = parseId(normalized);
        if (id.isEmpty()) {
            return Optional.of(TargetDescriptor.unknown(text));
        }

        ResourceLocation resolvedId = id.get();
        if (isEntityType(resolvedId)) {
            return Optional.of(TargetDescriptor.entityType(resolvedId));
        }
        if (isBlock(resolvedId)) {
            return Optional.of(TargetDescriptor.block(resolvedId));
        }
        if (isItem(resolvedId)) {
            return Optional.of(TargetDescriptor.item(resolvedId));
        }
        return Optional.of(TargetDescriptor.unknown(text));
    }

    private static Optional<TargetDescriptor> parseTagTarget(String tagText) {
        String type = "";
        String idText = tagText;
        int firstColon = tagText.indexOf(':');
        int secondColon = firstColon < 0 ? -1 : tagText.indexOf(':', firstColon + 1);
        if (secondColon > 0) {
            type = tagText.substring(0, firstColon);
            idText = tagText.substring(firstColon + 1);
        }

        Optional<ResourceLocation> id = parseId(idText);
        if (id.isEmpty()) {
            return Optional.of(TargetDescriptor.unknown("#" + tagText));
        }

        return Optional.of(switch (type) {
            case "block" -> TargetDescriptor.blockTag(id.get());
            case "item" -> TargetDescriptor.itemTag(id.get());
            case "entity" -> TargetDescriptor.entityTypeTag(id.get());
            default -> TargetDescriptor.entityTypeTag(id.get());
        });
    }

    private static Optional<ResourceLocation> parseId(String text) {
        if (text == null || text.isBlank()) {
            return Optional.empty();
        }
        if (text.contains(":")) {
            return Optional.ofNullable(ResourceLocation.tryParse(text));
        }
        return Optional.ofNullable(ResourceLocation.tryParse("minecraft:" + text));
    }

    private static boolean isEntityType(ResourceLocation id) {
        EntityType<?> entityType = BuiltInRegistries.ENTITY_TYPE.get(id);
        return BuiltInRegistries.ENTITY_TYPE.getKey(entityType).equals(id);
    }

    private static boolean isBlock(ResourceLocation id) {
        Block block = BuiltInRegistries.BLOCK.get(id);
        return BuiltInRegistries.BLOCK.getKey(block).equals(id);
    }

    private static boolean isItem(ResourceLocation id) {
        Item item = BuiltInRegistries.ITEM.get(id);
        return BuiltInRegistries.ITEM.getKey(item).equals(id);
    }
}
