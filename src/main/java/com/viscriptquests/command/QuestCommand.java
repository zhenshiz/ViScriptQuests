package com.viscriptquests.command;

import com.lowdragmc.lowdraglib2.Platform;
import com.lowdragmc.lowdraglib2.gui.factory.PlayerUIMenuType;
import com.lowdragmc.lowdraglib2.networking.rpc.RPCPacketDistributor;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegister;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.viscriptquests.ViScriptQuests;
import com.viscriptquests.gui.editor.QuestEditor;
import com.viscriptquests.network.s2c.S2CPayload;
import com.viscriptquests.quest.data.runtime.QuestCategoryListData;
import com.viscriptquests.quest.data.runtime.QuestCategoryData;
import com.viscriptquests.quest.data.runtime.QuestPlayerData;
import com.viscriptquests.quest.data.runtime.PlayerQuestState;
import com.viscriptquests.quest.data.runtime.QuestStatus;
import com.viscriptquests.quest.data.runtime.TaskStatus;
import com.viscriptquests.util.QuestFileHelper;
import com.viscriptquests.quest.runtime.QuestManager;
import com.viscriptquests.quest.data.QuestSavedData;
import com.viscriptquests.quest.runtime.QuestTrackingService;
import lombok.SneakyThrows;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

@LDLRegister(name = "quest", registry = ICommand.COMMAND_ID)
public class QuestCommand implements ICommand {
    private static final SuggestionProvider<CommandSourceStack> QUEST_SUGGESTIONS = (context, builder) -> {
        getServerQuestFiles().forEach(builder::suggest);
        return builder.buildFuture();
    };

    private static final SuggestionProvider<CommandSourceStack> PROJECT_SUGGESTIONS = (context, builder) -> {
        QuestFileHelper.getServerProjectFiles().forEach(builder::suggest);
        return builder.buildFuture();
    };

    private static final SuggestionProvider<CommandSourceStack> STEP_SUGGESTIONS = (context, builder) -> {
        Collection<ServerPlayer> players = EntityArgument.getPlayers(context, "target");
        String questId = StringArgumentType.getString(context, "quest");
        return SharedSuggestionProvider.suggest(commonValues(players, player -> QuestManager.getStepIds(player, questId)), builder);
    };

    private static final SuggestionProvider<CommandSourceStack> PLAYER_CATEGORY_SUGGESTIONS = (context, builder) -> {
        Collection<ServerPlayer> players = EntityArgument.getPlayers(context, "target");
        var ids = commonValues(players, player -> QuestSavedData.get(player.getServer()).getPlayer(player.getUUID())
                .copyCategories()
                .stream()
                .map(category -> category.id)
                .toList());
        return SharedSuggestionProvider.suggest(ids, builder);
    };

    @Override
    public void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext buildContext, Commands.CommandSelection commandSelection) {
        dispatcher.register(Commands.literal(ViScriptQuests.MOD_ID)
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("editor")
                        .requires(source -> source.hasPermission(4))
                        .executes(this::openEditor)
                        .then(Commands.argument("project", StringArgumentType.string())
                                .suggests(PROJECT_SUGGESTIONS)
                                .executes(this::openEditorWithProject)))
                .then(Commands.literal("book")
                        .executes(this::openBook))
                .then(Commands.literal("category")
                        .then(Commands.literal("config")
                                .executes(this::openCategoryConfig)))
                .then(Commands.literal("reload")
                        .then(Commands.argument("target", EntityArgument.players())
                                .executes(this::reloadPlayers)))
                .then(Commands.literal("grant")
                        .then(Commands.argument("target", EntityArgument.players())
                                .then(Commands.argument("quest", StringArgumentType.string())
                                        .suggests(QUEST_SUGGESTIONS)
                                        .then(Commands.argument("category", StringArgumentType.string())
                                                .suggests(PLAYER_CATEGORY_SUGGESTIONS)
                                                .executes(this::grant)))))
                .then(Commands.literal("revoke")
                        .then(Commands.argument("target", EntityArgument.players())
                                .then(Commands.argument("quest", StringArgumentType.string())
                                        .suggests(QUEST_SUGGESTIONS)
                                        .executes(this::revoke))))
                .then(Commands.literal("complete")
                        .then(Commands.argument("target", EntityArgument.players())
                                .then(Commands.argument("quest", StringArgumentType.string())
                                        .suggests(QUEST_SUGGESTIONS)
                                        .executes(this::complete))))
                .then(Commands.literal("submit")
                        .then(Commands.argument("target", EntityArgument.players())
                                .then(Commands.argument("quest", StringArgumentType.string())
                                        .suggests(QUEST_SUGGESTIONS)
                                        .then(Commands.argument("step", StringArgumentType.string())
                                                .suggests(STEP_SUGGESTIONS)
                                                .executes(this::submit))))));
    }

    @SneakyThrows
    private int openEditor(CommandContext<CommandSourceStack> context) {
        ServerPlayer player = context.getSource().getPlayer();
        if (player == null) {
            throw playerOnlyException();
        }
        PlayerUIMenuType.openUI(player, QuestEditor.EDITOR_ID);
        return 1;
    }

    // 从服务端加载项目文件并打开编辑器
    @SneakyThrows
    private int openEditorWithProject(CommandContext<CommandSourceStack> context) {
        ServerPlayer player = context.getSource().getPlayer();
        if (player == null) {
            throw playerOnlyException();
        }
        String projectId = QuestFileHelper.normalizeProjectId(StringArgumentType.getString(context, "project"));
        var graphTag = QuestFileHelper.readProject(projectId);
        if (graphTag.isEmpty()) {
            context.getSource().sendFailure(
                    Component.translatable("commands.viscript_quests.quest.editor.not_found", projectId));
            return 0;
        }
        PlayerUIMenuType.openUI(player, QuestEditor.EDITOR_ID);
        RPCPacketDistributor.rpcToPlayer(player, S2CPayload.OPEN_EDITOR_WITH_PROJECT, graphTag.get());
        return 1;
    }

    @SneakyThrows
    private int openBook(CommandContext<CommandSourceStack> context) {
        ServerPlayer player = context.getSource().getPlayer();
        if (player == null) {
            throw playerOnlyException();
        }
        QuestManager.refreshQuestBookDisplayData(player);
        QuestTrackingService.refresh(player);
        QuestPlayerData playerData = QuestSavedData.get(player.getServer()).getPlayer(player.getUUID());
        CompoundTag data = playerData.serializeNBT(Platform.getFrozenRegistry());
        RPCPacketDistributor.rpcToPlayer(player, S2CPayload.OPEN_QUEST_BOOK, data);
        return 1;
    }

    @SneakyThrows
    private int openCategoryConfig(CommandContext<CommandSourceStack> context) {
        ServerPlayer player = context.getSource().getPlayer();
        if (player == null) {
            throw playerOnlyException();
        }
        QuestCategoryListData data = QuestCategoryListData.of(
                QuestSavedData.get(player.getServer()).copyDefaultCategories());
        RPCPacketDistributor.rpcToPlayer(player, S2CPayload.OPEN_CATEGORY_CONFIG,
                data.serializeNBT(Platform.getFrozenRegistry()));
        return 1;
    }

    @SneakyThrows
    private int reloadPlayers(CommandContext<CommandSourceStack> context) {
        Collection<ServerPlayer> players = EntityArgument.getPlayers(context, "target");
        int successCount = 0;
        for (ServerPlayer player : players) {
            QuestSavedData.get(player.getServer()).resetPlayerData(player.getUUID());
            QuestTrackingService.refresh(player);
            successCount++;
            if (players.size() == 1) {
                context.getSource().sendSuccess(() -> Component.translatable(
                        "commands.viscript_quests.quest.reload.player", player.getDisplayName()), true);
            }
        }
        sendMultiTargetSummary(context, successCount, players.size());
        return successCount;
    }

    @SneakyThrows
    private int grant(CommandContext<CommandSourceStack> context) {
        Collection<ServerPlayer> players = EntityArgument.getPlayers(context, "target");
        String questId = StringArgumentType.getString(context, "quest");
        String categoryId = StringArgumentType.getString(context, "category");
        return applyToPlayers(context, players,
                player -> QuestManager.grant(player, questId, categoryId),
                player -> player.createCommandSourceStack().sendSuccess(
                        () -> Component.translatable("viscript_quests.quest.granted", questId), false),
                player -> grantPrecheckFailure(player, questId, categoryId),
                player -> grantPrecheckFailure(player, questId, categoryId),
                player -> Component.translatable(
                        "commands.viscript_quests.quest.grant.success",
                        player.getDisplayName(), QuestFileHelper.normalizeQuestId(questId), categoryId),
                false);
    }

    @SneakyThrows
    private int revoke(CommandContext<CommandSourceStack> context) {
        Collection<ServerPlayer> players = EntityArgument.getPlayers(context, "target");
        String questId = StringArgumentType.getString(context, "quest");
        return applyToPlayers(context, players,
                player -> QuestManager.revoke(player, questId),
                player -> player.createCommandSourceStack().sendSuccess(
                        () -> Component.translatable("viscript_quests.quest.revoked", questId), false),
                player -> statePrecheckFailure(player, questId),
                player -> stateMissingFailure(player, questId),
                player -> Component.translatable(
                        "commands.viscript_quests.quest.revoke.success",
                        player.getDisplayName(), QuestFileHelper.normalizeQuestId(questId)),
                false);
    }

    @SneakyThrows
    private int complete(CommandContext<CommandSourceStack> context) {
        Collection<ServerPlayer> players = EntityArgument.getPlayers(context, "target");
        String questId = StringArgumentType.getString(context, "quest");
        return applyToPlayers(context, players,
                player -> QuestManager.complete(player, questId),
                player -> player.createCommandSourceStack().sendSuccess(
                        () -> Component.translatable("viscript_quests.quest.completed", questId), true),
                player -> statePrecheckFailure(player, questId),
                player -> stateMissingFailure(player, questId),
                player -> Component.translatable(
                        "commands.viscript_quests.quest.complete.success",
                        player.getDisplayName(), QuestFileHelper.normalizeQuestId(questId)),
                true);
    }

    @SneakyThrows
    private int submit(CommandContext<CommandSourceStack> context) {
        Collection<ServerPlayer> players = EntityArgument.getPlayers(context, "target");
        String questId = StringArgumentType.getString(context, "quest");
        String stepId = StringArgumentType.getString(context, "step");
        return applyToPlayers(context, players,
                player -> QuestManager.submit(player, questId, stepId),
                player -> player.createCommandSourceStack().sendSuccess(
                        () -> Component.translatable("viscript_quests.quest.objective_completed", stepId, questId), true),
                player -> submitPrecheckFailure(player, questId, stepId),
                player -> Component.translatable("commands.viscript_quests.quest.submit.consume_failed", stepId),
                player -> Component.translatable(
                        "commands.viscript_quests.quest.submit.success",
                        player.getDisplayName(), QuestFileHelper.normalizeQuestId(questId), stepId),
                true);
    }

    private int applyToPlayers(CommandContext<CommandSourceStack> context,
                               Collection<ServerPlayer> players,
                               Function<ServerPlayer, Boolean> action,
                               Consumer<ServerPlayer> successNotification,
                               Function<ServerPlayer, Component> precheckFailure,
                               Function<ServerPlayer, Component> fallbackFailure,
                               Function<ServerPlayer, Component> successMessage,
                               boolean broadcastSingleSuccess) {
        int successCount = 0;
        for (ServerPlayer player : players) {
            Component failure = precheckFailure.apply(player);
            if (failure != null) {
                context.getSource().sendFailure(failure);
                continue;
            }
            if (action.apply(player)) {
                successCount++;
                successNotification.accept(player);
                if (players.size() == 1) {
                    context.getSource().sendSuccess(() -> successMessage.apply(player), broadcastSingleSuccess);
                }
                continue;
            }
            context.getSource().sendFailure(fallbackFailure.apply(player));
        }
        if (players.size() > 1) {
            int finalSuccessCount = successCount;
            Component summary = Component.translatable(
                    "commands.viscript_quests.quest.targets.result", finalSuccessCount, players.size());
            if (successCount > 0) {
                context.getSource().sendSuccess(() -> summary, true);
            } else {
                context.getSource().sendFailure(summary);
            }
        }
        return successCount;
    }

    private void sendMultiTargetSummary(CommandContext<CommandSourceStack> context, int successCount, int targetCount) {
        if (targetCount <= 1) {
            return;
        }
        Component summary = Component.translatable(
                "commands.viscript_quests.quest.targets.result", successCount, targetCount);
        if (successCount > 0) {
            context.getSource().sendSuccess(() -> summary, true);
        } else {
            context.getSource().sendFailure(summary);
        }
    }

    private static List<String> commonValues(Collection<ServerPlayer> players, Function<ServerPlayer, List<String>> valueProvider) {
        Set<String> values = new LinkedHashSet<>();
        boolean first = true;
        for (ServerPlayer player : players) {
            if (first) {
                values.addAll(valueProvider.apply(player));
                first = false;
            } else {
                values.retainAll(new LinkedHashSet<>(valueProvider.apply(player)));
            }
        }
        return new ArrayList<>(values);
    }

    private Component grantPrecheckFailure(ServerPlayer player, String questId, String categoryId) {
        String normalizedQuestId = QuestFileHelper.normalizeQuestId(questId);
        if (QuestFileHelper.getQuest(normalizedQuestId, player.registryAccess()).isEmpty()) {
            return Component.translatable("commands.viscript_quests.quest.missing", normalizedQuestId);
        }
        String normalizedCategoryId = QuestCategoryData.normalizeId(categoryId);
        var playerData = QuestSavedData.get(player.getServer()).getPlayer(player.getUUID());
        if (playerData.findCategory(normalizedCategoryId).isEmpty()) {
            return Component.translatable("commands.viscript_quests.quest.category.missing", normalizedCategoryId);
        }
        var state = playerData.findQuest(normalizedQuestId);
        if (state.isEmpty() || state.get().status != QuestStatus.ACTIVE) {
            return null;
        }
        return Component.translatable(
                "commands.viscript_quests.quest.grant.already_active",
                normalizedQuestId, player.getDisplayName());
    }

    private Component stateMissingFailure(ServerPlayer player, String questId) {
        return Component.translatable(
                "commands.viscript_quests.quest.state.missing",
                QuestFileHelper.normalizeQuestId(questId), player.getDisplayName());
    }

    private Component statePrecheckFailure(ServerPlayer player, String questId) {
        String normalizedQuestId = QuestFileHelper.normalizeQuestId(questId);
        var playerData = QuestSavedData.get(player.getServer()).getPlayer(player.getUUID());
        var state = playerData.findQuest(normalizedQuestId);
        if (state.isPresent()) {
            return null;
        }
        return stateMissingFailure(player, questId);
    }

    private Component submitPrecheckFailure(ServerPlayer player, String questId, String stepId) {
        String normalizedQuestId = QuestFileHelper.normalizeQuestId(questId);
        var playerData = QuestSavedData.get(player.getServer()).getPlayer(player.getUUID());
        Optional<PlayerQuestState> state = playerData.findQuest(normalizedQuestId);
        if (state.isEmpty() || state.get().status != QuestStatus.ACTIVE) {
            return stateMissingFailure(player, questId);
        }
        var questFile = QuestFileHelper.getQuest(normalizedQuestId, player.registryAccess());
        if (questFile.isEmpty()) {
            return Component.translatable("commands.viscript_quests.quest.missing", normalizedQuestId);
        }
        var progress = state.get().findStepProgress(stepId);
        if (progress.isEmpty()) {
            return Component.translatable("commands.viscript_quests.quest.task.missing", stepId, normalizedQuestId);
        }
        if (progress.get().status == TaskStatus.COMPLETED) {
            return Component.translatable("commands.viscript_quests.quest.task.already_completed", stepId);
        }
        if (progress.get().status != TaskStatus.ACTIVE) {
            return Component.translatable("commands.viscript_quests.quest.task.not_active", stepId);
        }
        var tasks = questFile.get().findTasksForStep(stepId);
        if (tasks.isEmpty()) {
            return Component.translatable("commands.viscript_quests.quest.task.definition_missing", stepId, normalizedQuestId);
        }
        boolean completed = tasks.stream().allMatch(task -> task.checkCompletion(player));
        if (!completed) {
            return Component.translatable("commands.viscript_quests.quest.submit.missing_items", stepId);
        }
        return null;
    }

    // 扫描 quest 目录下的所有 .quest 文件，返回带引号的相对路径用于命令建议
    public static List<String> getServerQuestFiles() {
        List<String> questFiles = new ArrayList<>();
        Path directory = QuestFileHelper.questDirectory();
        if (Files.exists(directory) && Files.isDirectory(directory)) {
            try (var stream = Files.walk(directory)) {
                stream.filter(Files::isRegularFile).forEach(file -> {
                    String path = file.toString();
                    if (path.endsWith(QuestFileHelper.QUEST_SUFFIX)) {
                        String relative = directory.relativize(file).toString()
                                .replace('\\', '/')
                                .replace(QuestFileHelper.QUEST_SUFFIX, "");
                        questFiles.add("\"" + relative + "\"");
                    }
                });
            } catch (IOException ignored) {
            }
        }
        return questFiles;
    }
}
