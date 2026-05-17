package com.swvague.ars_odyssey.command;

import com.swvague.ars_odyssey.index.EvidenceConfidence;
import com.swvague.ars_odyssey.archive.ArchiveAccessMode;
import com.swvague.ars_odyssey.archive.ArchiveAccessPolicy;
import com.swvague.ars_odyssey.archive.ArchiveCoreChunkLoading;
import com.swvague.ars_odyssey.archive.ArchiveCoreBinding;
import com.swvague.ars_odyssey.archive.ArchiveCostPolicy;
import com.swvague.ars_odyssey.archive.ArchiveOperationCost;
import com.swvague.ars_odyssey.archive.ArchiveOperationResult;
import com.swvague.ars_odyssey.archive.ArchiveOperationType;
import com.swvague.ars_odyssey.archive.ArchiveService;
import com.swvague.ars_odyssey.archive.cost.ArchivePaymentResult;
import com.swvague.ars_odyssey.archive.provision.ArchiveProvisionRule;
import com.swvague.ars_odyssey.archive.resonance.ResonanceRiskLevel;
import com.swvague.ars_odyssey.archive.resonance.ResonanceService;
import com.swvague.ars_odyssey.archive.resonance.ResonanceState;
import com.swvague.ars_odyssey.archive.PlayerArchiveAttachments;
import com.swvague.ars_odyssey.archive.PlayerArchiveData;
import com.swvague.ars_odyssey.index.GlyphTargetRule;
import com.swvague.ars_odyssey.knowledge.DiscoverySource;
import com.swvague.ars_odyssey.knowledge.GlyphRelation;
import com.swvague.ars_odyssey.knowledge.MatcherTargetResolver;
import com.swvague.ars_odyssey.knowledge.PlayerKnowledgeAttachments;
import com.swvague.ars_odyssey.knowledge.PlayerKnowledgeData;
import com.swvague.ars_odyssey.knowledge.PlayerKnowledgeDataView;
import com.swvague.ars_odyssey.knowledge.RelationType;
import com.swvague.ars_odyssey.knowledge.TargetDescriptor;
import com.swvague.ars_odyssey.knowledge.TargetKind;
import com.swvague.ars_odyssey.knowledge.TruthDelta;
import com.swvague.ars_odyssey.knowledge.discovery.GlyphDiscoveryResult;
import com.swvague.ars_odyssey.knowledge.discovery.GlyphDiscoveryService;
import com.swvague.ars_odyssey.network.ModNetwork;
import com.swvague.ars_odyssey.registry.ModRegistry;
import com.swvague.ars_odyssey.value.ItemValueResolver;
import com.swvague.ars_odyssey.value.ItemValueResult;
import com.hollingsworth.arsnouveau.api.registry.GlyphRegistry;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

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
                                                .executes(ArsOdysseyCommands::debugForgetMatcher)))))
                .then(Commands.literal("debug_item_value")
                        .executes(ArsOdysseyCommands::debugHeldItemValue))
                .then(Commands.literal("archive")
                        .then(Commands.literal("awaken")
                                .executes(ArsOdysseyCommands::archiveAwaken))
                        .then(Commands.literal("status")
                                .executes(ArsOdysseyCommands::archiveStatus))
                        .then(Commands.literal("estimate")
                                .then(Commands.argument("operation", StringArgumentType.word())
                                        .executes(context -> archiveEstimate(context, 1))
                                        .then(Commands.argument("count", IntegerArgumentType.integer(1))
                                                .executes(context -> archiveEstimate(context, IntegerArgumentType.getInteger(context, "count"))))))
                        .then(Commands.literal("deposit")
                                .executes(context -> archiveDeposit(context, context.getSource().getPlayerOrException().getMainHandItem().getCount()))
                                .then(Commands.argument("count", IntegerArgumentType.integer(1))
                                        .executes(context -> archiveDeposit(context, IntegerArgumentType.getInteger(context, "count")))))
                        .then(Commands.literal("extract")
                                .then(Commands.argument("item_id", ResourceLocationArgument.id())
                                        .then(Commands.argument("count", IntegerArgumentType.integer(1))
                                                .executes(ArsOdysseyCommands::archiveExtract))))
                        .then(Commands.literal("query")
                                .executes(ArsOdysseyCommands::archiveQueryHeld)
                                .then(Commands.argument("item_id", ResourceLocationArgument.id())
                                        .executes(ArsOdysseyCommands::archiveQueryItem)))
                        .then(Commands.literal("provision")
                                .then(Commands.literal("set")
                                        .then(Commands.argument("item_id", ResourceLocationArgument.id())
                                                .then(Commands.argument("target_count", IntegerArgumentType.integer(1, 2304))
                                                        .executes(ArsOdysseyCommands::archiveProvisionSet))))
                                .then(Commands.literal("remove")
                                        .then(Commands.argument("item_id", ResourceLocationArgument.id())
                                                .executes(ArsOdysseyCommands::archiveProvisionRemove)))
                                .then(Commands.literal("list")
                                        .executes(ArsOdysseyCommands::archiveProvisionList))
                                .then(Commands.literal("run")
                                        .executes(ArsOdysseyCommands::archiveProvisionRunAll)
                                        .then(Commands.argument("item_id", ResourceLocationArgument.id())
                                                .executes(ArsOdysseyCommands::archiveProvisionRun))))
                        .then(Commands.literal("set_mode")
                                .then(Commands.argument("mode", StringArgumentType.word())
                                        .executes(ArsOdysseyCommands::archiveSetMode))))
                .then(Commands.literal("entanglement")
                        .then(Commands.literal("add")
                                .then(Commands.argument("amount", DoubleArgumentType.doubleArg())
                                        .executes(ArsOdysseyCommands::entanglementAdd)))
                        .then(Commands.literal("set")
                                .then(Commands.argument("amount", DoubleArgumentType.doubleArg(0))
                                        .executes(ArsOdysseyCommands::entanglementSet)))));
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

    private static int archiveAwaken(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        BlockHitResult hit = pickBlock(player, 8.0D);
        if (hit == null || hit.getType() != HitResult.Type.BLOCK) {
            context.getSource().sendFailure(Component.literal("Look at a block to bind the Resonance Archive Core."));
            return 0;
        }

        BlockPos pos = hit.getBlockPos();
        if (player.level().isEmptyBlock(pos)) {
            context.getSource().sendFailure(Component.literal("The Archive Core cannot bind to air."));
            return 0;
        }
        if (!player.level().getBlockState(pos).is(ModRegistry.RESONANCE_ARCHIVE_CORE.get())) {
            context.getSource().sendFailure(Component.literal("This ritual must bind a Resonance Archive Core."));
            return 0;
        }

        PlayerArchiveData data = player.getData(PlayerArchiveAttachments.PLAYER_ARCHIVE);
        data.awaken(player.level().dimension().location(), pos);
        player.setData(PlayerArchiveAttachments.PLAYER_ARCHIVE, data);
        ModNetwork.syncArchive(player, data);

        ArchiveCoreBinding binding = data.coreBinding().orElseThrow();
        boolean chunkForced = ArchiveCoreChunkLoading.ensureForced(player.server, binding);
        context.getSource().sendSuccess(() -> Component.literal(
                "Resonance Archive awakened at " + binding.display()
                        + ". Access mode: " + data.unlockedAccessMode().name().toLowerCase(Locale.ROOT)
                        + ". Core chunk forced: " + chunkForced), false);
        return 1;
    }

    private static int archiveStatus(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        PlayerArchiveData data = player.getData(PlayerArchiveAttachments.PLAYER_ARCHIVE);
        if (!data.isAwakened()) {
            context.getSource().sendSuccess(() -> Component.literal("Resonance Archive is dormant."), false);
            return 0;
        }

        String binding = data.coreBinding()
                .map(ArchiveCoreBinding::display)
                .orElse("unbound");
        ResonanceRiskLevel riskLevel = ResonanceService.refresh(player, data);
        ResonanceState resonance = data.resonanceState();
        context.getSource().sendSuccess(() -> Component.literal(
                "Resonance Archive: awakened, core " + binding
                        + ", access mode " + data.unlockedAccessMode().name().toLowerCase(Locale.ROOT)
                        + ", resonance " + riskLevel.name().toLowerCase(Locale.ROOT)
                        + " stability " + formatDouble(resonance.stability())
                        + ", load " + formatDouble(resonance.load())
                        + ", pressure " + formatDouble(resonance.dissonancePressure())), false);
        return 1;
    }

    private static int archiveEstimate(CommandContext<CommandSourceStack> context, int count) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        ItemStack stack = player.getMainHandItem();
        if (stack.isEmpty()) {
            context.getSource().sendFailure(Component.literal("Hold an item in your main hand."));
            return 0;
        }

        String operationText = StringArgumentType.getString(context, "operation");
        ArchiveOperationType operationType;
        try {
            operationType = ArchiveOperationType.valueOf(operationText.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            context.getSource().sendFailure(Component.literal("Unknown archive operation: " + operationText));
            return 0;
        }

        PlayerArchiveData data = player.getData(PlayerArchiveAttachments.PLAYER_ARCHIVE);
        ArchiveOperationCost cost = ArchiveCostPolicy.estimate(stack, count, operationType);
        ArchiveAccessPolicy.ArchiveAccessCheck access = ArchiveAccessPolicy.check(data, cost);
        context.getSource().sendSuccess(() -> Component.literal(
                "Archive estimate: " + operationType.name().toLowerCase(Locale.ROOT)
                        + " x" + count
                        + ", tier " + cost.itemTier().name().toLowerCase(Locale.ROOT)
                        + ", source " + formatSourceMultiplier(cost.sourceCost())
                        + ", resonance " + formatDouble(cost.resonanceLoad())
                        + ", dissonance pressure " + formatDouble(cost.dissonancePressure())
                        + ", access " + (access.allowed() ? "allowed" : "blocked:" + access.reason())), false);
        return access.allowed() ? 1 : 0;
    }

    private static int archiveDeposit(CommandContext<CommandSourceStack> context, int count) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        ArchiveOperationResult result = ArchiveService.depositHeld(player, count);
        sendArchiveOperationResult(context, "deposit", result);
        return result.success() ? 1 : 0;
    }

    private static int archiveExtract(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        ResourceLocation itemId = ResourceLocationArgument.getId(context, "item_id");
        int count = IntegerArgumentType.getInteger(context, "count");
        ArchiveOperationResult result = ArchiveService.extract(player, itemId, count);
        sendArchiveOperationResult(context, "extract", result);
        return result.success() ? 1 : 0;
    }

    private static int archiveQueryHeld(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        ItemStack held = player.getMainHandItem();
        if (held.isEmpty()) {
            context.getSource().sendFailure(Component.literal("Hold an item in your main hand."));
            return 0;
        }
        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(held.getItem());
        return archiveQuery(context, itemId);
    }

    private static int archiveQueryItem(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        return archiveQuery(context, ResourceLocationArgument.getId(context, "item_id"));
    }

    private static int archiveQuery(CommandContext<CommandSourceStack> context, ResourceLocation itemId) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        PlayerArchiveData data = player.getData(PlayerArchiveAttachments.PLAYER_ARCHIVE);
        long stored = ArchiveService.query(data, itemId);
        context.getSource().sendSuccess(() -> Component.literal(
                "Archive query: " + itemId + " stored " + stored), false);
        return stored > 0 ? 1 : 0;
    }

    private static int archiveProvisionSet(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        ResourceLocation itemId = ResourceLocationArgument.getId(context, "item_id");
        int targetCount = IntegerArgumentType.getInteger(context, "target_count");
        Item item = BuiltInRegistries.ITEM.get(itemId);
        if (!BuiltInRegistries.ITEM.getKey(item).equals(itemId) || item.getDefaultInstance().isEmpty()) {
            context.getSource().sendFailure(Component.literal("Unknown or unsupported item: " + itemId));
            return 0;
        }

        PlayerArchiveData data = player.getData(PlayerArchiveAttachments.PLAYER_ARCHIVE);
        data.provisionRules().set(itemId, targetCount);
        player.setData(PlayerArchiveAttachments.PLAYER_ARCHIVE, data);
        ModNetwork.syncArchive(player, data);
        context.getSource().sendSuccess(() -> Component.literal(
                "Provision rule set: keep " + targetCount + " x " + itemId), false);
        return 1;
    }

    private static int archiveProvisionRemove(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        ResourceLocation itemId = ResourceLocationArgument.getId(context, "item_id");
        PlayerArchiveData data = player.getData(PlayerArchiveAttachments.PLAYER_ARCHIVE);
        boolean removed = data.provisionRules().remove(itemId);
        player.setData(PlayerArchiveAttachments.PLAYER_ARCHIVE, data);
        ModNetwork.syncArchive(player, data);
        if (!removed) {
            context.getSource().sendFailure(Component.literal("No provision rule for " + itemId));
            return 0;
        }
        context.getSource().sendSuccess(() -> Component.literal(
                "Provision rule removed: " + itemId), false);
        return 1;
    }

    private static int archiveProvisionList(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        PlayerArchiveData data = player.getData(PlayerArchiveAttachments.PLAYER_ARCHIVE);
        if (data.provisionRules().all().isEmpty()) {
            context.getSource().sendSuccess(() -> Component.literal("Provision rules: none"), false);
            return 0;
        }

        StringBuilder builder = new StringBuilder("Provision rules:");
        for (ArchiveProvisionRule rule : data.provisionRules().all()) {
            long stored = ArchiveService.query(data, rule.itemId());
            int carried = ArchiveService.inventoryCount(player, rule.itemId());
            builder.append("\n- ")
                    .append(rule.itemId())
                    .append(" keep ")
                    .append(rule.targetCount())
                    .append(", carried ")
                    .append(carried)
                    .append(", stored ")
                    .append(stored);
        }
        context.getSource().sendSuccess(() -> Component.literal(builder.toString()), false);
        return data.provisionRules().size();
    }

    private static int archiveProvisionRun(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        ResourceLocation itemId = ResourceLocationArgument.getId(context, "item_id");
        ArchiveOperationResult result = ArchiveService.provision(player, itemId);
        sendArchiveOperationResult(context, "provision " + itemId, result);
        return result.success() ? 1 : 0;
    }

    private static int archiveProvisionRunAll(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        PlayerArchiveData data = player.getData(PlayerArchiveAttachments.PLAYER_ARCHIVE);
        int successes = 0;
        long moved = 0L;
        StringBuilder failures = new StringBuilder();
        for (ArchiveProvisionRule rule : data.provisionRules().enabledRules()) {
            ArchiveOperationResult result = ArchiveService.provision(player, rule.itemId());
            if (result.success()) {
                successes++;
                moved += result.changedCount();
            } else {
                if (!failures.isEmpty()) {
                    failures.append(", ");
                }
                failures.append(rule.itemId()).append(":").append(result.reason());
            }
        }

        String message = "Provision run: " + successes + " rule(s), moved " + moved;
        if (!failures.isEmpty()) {
            message += ", failed " + failures;
        }
        String finalMessage = message;
        context.getSource().sendSuccess(() -> Component.literal(finalMessage), false);
        return successes;
    }

    private static void sendArchiveOperationResult(CommandContext<CommandSourceStack> context, String action, ArchiveOperationResult result) {
        if (!result.success()) {
            context.getSource().sendFailure(Component.literal(
                    "Archive " + action + " failed: " + result.reason()
                            + ", stored " + result.storedCount()
                            + formatCostSuffix(result.cost())
                            + formatPaymentSuffix(result.payment())));
            return;
        }

        context.getSource().sendSuccess(() -> Component.literal(
                "Archive " + action + ": moved " + result.changedCount()
                        + ", stored " + result.storedCount()
                        + formatCostSuffix(result.cost())
                        + formatPaymentSuffix(result.payment())
                        + formatRiskSuffix(result.resonanceRiskLevel())), false);
    }

    private static int archiveSetMode(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        String modeText = StringArgumentType.getString(context, "mode");
        ArchiveAccessMode mode;
        try {
            mode = ArchiveAccessMode.valueOf(modeText.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            context.getSource().sendFailure(Component.literal("Unknown archive access mode: " + modeText));
            return 0;
        }

        PlayerArchiveData data = player.getData(PlayerArchiveAttachments.PLAYER_ARCHIVE);
        data.setUnlockedAccessMode(mode);
        player.setData(PlayerArchiveAttachments.PLAYER_ARCHIVE, data);
        ModNetwork.syncArchive(player, data);
        context.getSource().sendSuccess(() -> Component.literal(
                "Archive access mode set to " + mode.name().toLowerCase(Locale.ROOT)), false);
        return 1;
    }

    private static int debugHeldItemValue(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        ItemStack stack = player.getMainHandItem();
        if (stack.isEmpty()) {
            context.getSource().sendFailure(Component.literal("Hold an item in your main hand."));
            return 0;
        }

        ItemValueResult result = ItemValueResolver.resolve(stack);
        context.getSource().sendSuccess(() -> Component.literal(
                "Item value: " + result.itemId()
                        + " -> " + result.tier().name().toLowerCase(Locale.ROOT)
                        + " [" + result.tier().level() + "]"
                        + ", source x" + formatSourceMultiplier(result.tier().sourceCostMultiplier())
                        + ", reason " + result.reason()), false);
        return 1;
    }

    private static BlockHitResult pickBlock(ServerPlayer player, double reach) {
        Vec3 start = player.getEyePosition();
        Vec3 end = start.add(player.getLookAngle().scale(reach));
        HitResult hit = player.level().clip(new ClipContext(
                start,
                end,
                ClipContext.Block.OUTLINE,
                ClipContext.Fluid.NONE,
                player
        ));
        return hit instanceof BlockHitResult blockHit ? blockHit : null;
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

    private static int entanglementAdd(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        double amount = DoubleArgumentType.getDouble(context, "amount");
        PlayerKnowledgeData data = player.getData(PlayerKnowledgeAttachments.PLAYER_KNOWLEDGE);
        data.addTruthEntanglement(amount);
        player.setData(PlayerKnowledgeAttachments.PLAYER_KNOWLEDGE, data);
        ModNetwork.syncKnowledge(player, data);
        context.getSource().sendSuccess(() -> Component.literal(
                "Truth entanglement: " + formatDouble(data.getTotalTruthEntanglement())), false);
        return 1;
    }

    private static int entanglementSet(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        double amount = DoubleArgumentType.getDouble(context, "amount");
        PlayerKnowledgeData data = player.getData(PlayerKnowledgeAttachments.PLAYER_KNOWLEDGE);
        data.setTruthEntanglement(amount);
        player.setData(PlayerKnowledgeAttachments.PLAYER_KNOWLEDGE, data);
        ModNetwork.syncKnowledge(player, data);
        context.getSource().sendSuccess(() -> Component.literal(
                "Truth entanglement: " + formatDouble(data.getTotalTruthEntanglement())), false);
        return 1;
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

    private static String formatDouble(double value) {
        if (Math.abs(value - Math.rint(value)) < 0.005D) {
            return Long.toString(Math.round(value));
        }
        return String.format(Locale.ROOT, "%.2f", value);
    }

    private static String formatSourceMultiplier(double value) {
        if (Double.isInfinite(value)) {
            return "blocked";
        }
        return formatDouble(value);
    }

    private static String formatCostSuffix(ArchiveOperationCost cost) {
        if (cost == null) {
            return "";
        }
        return ", source " + formatSourceMultiplier(cost.sourceCost())
                + ", resonance " + formatDouble(cost.resonanceLoad())
                + ", dissonance pressure " + formatDouble(cost.dissonancePressure());
    }

    private static String formatPaymentSuffix(ArchivePaymentResult payment) {
        if (payment == null) {
            return "";
        }
        return ", paid source " + payment.sourcePaid()
                + ", payment " + payment.reason();
    }

    private static String formatRiskSuffix(ResonanceRiskLevel riskLevel) {
        if (riskLevel == null) {
            return "";
        }
        return ", resonance " + riskLevel.name().toLowerCase(Locale.ROOT);
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
