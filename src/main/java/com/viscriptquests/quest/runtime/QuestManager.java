package com.viscriptquests.quest.runtime;

import com.lowdragmc.lowdraglib2.Platform;
import com.lowdragmc.lowdraglib2.networking.rpc.RPCPacketDistributor;
import com.viscriptquests.event.neoforge.QuestEvent;
import com.viscriptquests.network.s2c.S2CPayload;
import com.viscriptquests.quest.data.QuestFile;
import com.viscriptquests.quest.data.QuestSavedData;
import com.viscriptquests.quest.data.runtime.PlayerQuestState;
import com.viscriptquests.quest.data.runtime.QuestBookData;
import com.viscriptquests.quest.data.runtime.QuestCategoryListData;
import com.viscriptquests.quest.data.runtime.QuestPlayerData;
import com.viscriptquests.quest.data.runtime.QuestStatus;
import com.viscriptquests.quest.data.runtime.TaskProgress;
import com.viscriptquests.quest.data.runtime.TaskStatus;
import com.viscriptquests.util.QuestCategoryFileHelper;
import com.viscriptquests.util.QuestFileHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.common.NeoForge;

import java.util.List;
import java.util.Optional;

/**
 * 提供任务运行时操作的统一入口。
 *
 * <p>该类封装任务发放、撤销、追踪、提交、调试查询和变量修改等服务端逻辑。
 * 运行时只返回操作是否成功；指令、脚本或其它调用方自行决定如何反馈或记录日志。
 */
public class QuestManager {
    /**
     * 向客户端发送任务书数据并打开任务书界面。
     *
     * <p>指令、客户端按键和未来脚本 API 都应复用这个入口，避免各处重复拼装任务书同步数据。
     *
     * @param player 服务端玩家，要为该玩家打开任务书
     */
    public static void openQuestBook(ServerPlayer player) {
        refreshQuestBookDisplayData(player);
        QuestTrackingService.refresh(player);
        QuestPlayerData playerData = QuestSavedData.get(player.getServer()).getPlayer(player.getUUID());
        QuestBookData bookData = createBookData(playerData);
        RPCPacketDistributor.rpcToPlayer(player, S2CPayload.OPEN_QUEST_BOOK,
                bookData.serializeNBT(Platform.getFrozenRegistry()));
    }

    /**
     * 向指定玩家发放任务，并根据任务文件里的分类信息归类。
     *
     * <p>方法会读取运行时任务文件，创建玩家独立的任务状态，推进初始流程节点，
     * 自动追踪第一个可执行的小任务，并将玩家任务数据标记为已修改。若任务文件不存在、
     * 分类不存在或同一任务已经处于激活状态，则返回 {@code false}。
     *
     * @param player 服务端玩家，接收该任务的玩家
     * @param questId 任务标识，允许传入未规范化的任务文件标识
     * @return 是否成功发放任务
     */
    public static boolean grant(ServerPlayer player, String questId) {
        String normalizedQuestId = QuestFileHelper.normalizeQuestId(questId);
        Optional<QuestFile> questFile = QuestFileHelper.getQuest(normalizedQuestId, player.registryAccess());
        if (questFile.isEmpty()) {
            return false;
        }

        QuestSavedData savedData = QuestSavedData.get(player.getServer());
        var playerData = savedData.getPlayer(player.getUUID());
        String normalizedCategoryId = QuestCategoryFileHelper.findCategoryIdForQuest(normalizedQuestId).orElse("");
        if (normalizedCategoryId.isBlank()) {
            return false;
        }
        var completedInScope = QuestTeamProgressService.findCompletedQuestInScope(player, savedData, normalizedQuestId);
        if (completedInScope.isPresent()) {
            PlayerQuestState copiedState = QuestTeamProgressService.copyState(completedInScope.get().state(), player.registryAccess());
            playerData.putQuest(copiedState);
            savedData.setDirty();
            QuestTrackingService.refresh(player);
            return false;
        }
        Optional<PlayerQuestState> existing = playerData.findQuest(normalizedQuestId);
        if (existing.isPresent() && existing.get().status == QuestStatus.ACTIVE) {
            return false;
        }
        var activeInScope = QuestTeamProgressService.findActiveQuestInScope(player, savedData, normalizedQuestId);
        if (activeInScope.isPresent()) {
            PlayerQuestState copiedState = QuestTeamProgressService.copyState(activeInScope.get().state(), player.registryAccess());
            playerData.putQuest(copiedState);
            QuestTrackingService.trackFirstActiveStep(player, playerData, copiedState);
            savedData.setDirty();
            QuestTeamProgressService.syncQuestState(player, copiedState);
            return true;
        }

        PlayerQuestState state = PlayerQuestState.fromQuestFile(questFile.get(), player.level().getGameTime(), player);
        state.categoryId = normalizedCategoryId;
        playerData.putQuest(state);
        NeoForge.EVENT_BUS.post(new QuestEvent.QuestStarted(player, state));
        QuestFlowExecutor.advance(player, state, questFile.get());
        QuestTrackingService.trackFirstActiveStep(player, playerData, state);
        savedData.setDirty();
        QuestTeamProgressService.syncQuestState(player, state);
        return true;
    }

    public static QuestBookData createBookData(QuestPlayerData playerData) {
        QuestBookData data = new QuestBookData();
        data.playerData = playerData;
        data.categoryData = QuestCategoryListData.of(QuestCategoryFileHelper.copyCategories());
        return data;
    }

    /**
     * 从指定玩家身上移除任务。
     *
     * <p>方法会移除玩家保存数据中的任务状态。如果被移除的任务正被追踪，
     * 同时清空追踪信息，并将玩家任务数据标记为已修改。
     *
     * @param player 服务端玩家，要从该玩家身上撤销任务
     * @param questId 任务标识，允许传入未规范化的任务文件标识
     * @return 是否成功撤销任务
     */
    public static boolean revoke(ServerPlayer player, String questId) {
        String normalizedQuestId = QuestFileHelper.normalizeQuestId(questId);
        return QuestTeamProgressService.revokeQuestInScope(player, normalizedQuestId);
    }

    /**
     * 将指定任务设为玩家当前追踪任务。
     *
     * <p>方法会委托追踪服务选择该任务中的第一个激活小任务。若任务不存在或未处于激活状态，
     * 返回 {@code false}。
     *
     * @param player 服务端玩家，要修改该玩家的追踪状态
     * @param questId 任务标识，允许传入未规范化的任务文件标识
     * @return 是否成功设置追踪任务
     */
    public static boolean track(ServerPlayer player, String questId) {
        return QuestTrackingService.track(player, questId);
    }

    /**
     * 尝试提交指定小任务下所有当前可提交的目标。
     *
     * <p>该入口主要给指令和调试使用。目标可以先分批提交；只有小任务下所有目标完成后，
     * 才会发放奖励、推进流程节点，并刷新玩家追踪状态。
     *
     * @param player 服务端玩家，提交目标的玩家
     * @param questId 任务标识，允许传入未规范化的任务文件标识
     * @param stepId 小任务标识，对应任务文件中的子任务节点标识
     * @return 是否成功提交了至少一个目标，或是否因此完成该小任务
     */
    public static boolean submit(ServerPlayer player, String questId, String stepId) {
        return QuestSubmissionService.submit(player, QuestFileHelper.normalizeQuestId(questId), stepId, false);
    }

    /**
     * 触发玩家当前激活任务中的自定义目标。
     *
     * <p>该入口供指令、KubeJS 或其它模组联动调用。它不会检查物品、进度等通用条件，
     * 只会完成标识相同且当前已经解锁的小任务目标，然后复用标准流程结算奖励和后续节点。
     *
     * @param player 服务端玩家，要触发该玩家身上的自定义目标
     * @param triggerId 自定义触发标识，必须与蓝图目标里的标识一致
     * @return 是否有匹配目标被触发
     */
    public static boolean triggerCustom(ServerPlayer player, String triggerId) {
        return QuestSubmissionService.triggerCustom(player, triggerId);
    }

    public static void syncQuestBook(ServerPlayer player) {
        QuestPlayerData playerData = QuestSavedData.get(player.getServer()).getPlayer(player.getUUID());
        RPCPacketDistributor.rpcToPlayer(player, S2CPayload.SYNC_QUEST_BOOK,
                createBookData(playerData).serializeNBT(Platform.getFrozenRegistry()));
    }

    /**
     * 手动提交指定小任务下的单个目标。
     *
     * @param player 服务端玩家，提交目标的玩家
     * @param questId 任务标识，允许传入未规范化的任务文件标识
     * @param stepId 小任务标识，对应任务文件中的子任务节点标识
     * @param objectiveIndex 目标在该小任务目标列表中的索引
     * @return 是否成功提交该目标
     */
    public static boolean submitObjective(ServerPlayer player, String questId, String stepId, int objectiveIndex) {
        return QuestSubmissionService.submitObjective(player,
                QuestFileHelper.normalizeQuestId(questId),
                stepId,
                objectiveIndex);
    }

    /**
     * 自动提交玩家当前追踪的小任务。
     *
     * <p>该方法主要供服务端 tick 检查调用。只有玩家当前存在追踪任务和追踪小任务时才会继续提交；
     * 提交流程会要求对应目标中至少存在自动提交目标。无可提交目标时返回 {@code false}。
     *
     * @param player 服务端玩家，要检查该玩家当前追踪的小任务
     * @return 是否成功自动提交当前追踪的小任务
     */
    public static boolean submitTracked(ServerPlayer player) {
        QuestSavedData savedData = QuestSavedData.get(player.getServer());
        var playerData = savedData.getPlayer(player.getUUID());
        if (playerData.trackedQuestId.isBlank() || playerData.trackedStepId.isBlank()) {
            return false;
        }
        return QuestSubmissionService.submit(player,
                QuestFileHelper.normalizeQuestId(playerData.trackedQuestId),
                playerData.trackedStepId,
                true);
    }

    /**
     * 刷新任务书需要展示的目标文本、图标和提交按钮状态。
     *
     * <p>该方法只同步运行时任务文件派生出的展示数据，不改变任务流程状态。
     * 打开任务书或服务端提交后调用它，可以让展示字段跟上当前任务定义。
     *
     * @param player 服务端玩家，要刷新该玩家的任务书数据
     */
    public static void refreshQuestBookDisplayData(ServerPlayer player) {
        QuestSavedData savedData = QuestSavedData.get(player.getServer());
        QuestPlayerData playerData = savedData.getPlayer(player.getUUID());
        QuestFileHelper.clearCache();
        for (PlayerQuestState state : playerData.quests) {
            QuestFile questFile = QuestFileHelper.getQuest(state.questId, player.registryAccess()).orElse(null);
            if (questFile == null) {
                continue;
            }
            state.refreshRewardDisplays(questFile, player);
            for (TaskProgress progress : state.taskProgresses) {
                progress.refreshObjectives(questFile, player, state.questVariables);
            }
        }
        savedData.setDirty();
    }

    /**
     * 强制完成指定玩家的任务。
     *
     * <p>方法会将任务内所有小任务标记为完成，发放未发放的小任务奖励和任务完成奖励，
     * 并刷新玩家追踪状态。该方法用于指令、调试或脚本控制，不会检查普通目标条件。
     *
     * @param player 服务端玩家，要修改该玩家的任务状态
     * @param questId 任务标识，允许传入未规范化的任务文件标识
     * @return 是否成功强制完成任务
     */
    public static boolean complete(ServerPlayer player, String questId) {
        String normalizedQuestId = QuestFileHelper.normalizeQuestId(questId);
        QuestSavedData savedData = QuestSavedData.get(player.getServer());
        var playerData = savedData.getPlayer(player.getUUID());
        var state = playerData.findQuest(normalizedQuestId);
        if (state.isEmpty()) {
            return false;
        }
        QuestFlowExecutor.completeQuest(player, state.get(),
                QuestFileHelper.getQuest(normalizedQuestId, player.registryAccess()).orElse(null));
        savedData.setDirty();
        QuestTrackingService.refresh(player);
        QuestTeamProgressService.syncQuestState(player, state.get());
        return true;
    }

    /**
     * 获取玩家在指定任务中当前追踪且可提交的小任务标识。
     *
     * <p>该查询用于 {@code submit} 指令补全，只返回玩家当前明确追踪、任务和小任务都处于
     * 激活状态的步骤 UUID。尚未解锁、未被追踪或已经结束的小任务不会出现在补全列表中。
     *
     * @param player 服务端玩家，要读取该玩家的追踪状态
     * @param questId 任务标识，允许传入未规范化的任务文件标识
     * @return 当前可提交的追踪小任务标识列表；没有匹配项时返回空列表
     */
    public static List<String> getTrackedStepIds(ServerPlayer player, String questId) {
        String normalizedQuestId = QuestFileHelper.normalizeQuestId(questId);
        QuestPlayerData playerData = QuestSavedData.get(player.getServer()).getPlayer(player.getUUID());
        if (!normalizedQuestId.equals(playerData.trackedQuestId)
                || playerData.trackedStepId == null
                || playerData.trackedStepId.isBlank()) {
            return List.of();
        }
        return playerData.findQuest(normalizedQuestId)
                .filter(state -> state.status == QuestStatus.ACTIVE)
                .flatMap(state -> state.findStepProgress(playerData.trackedStepId))
                .filter(progress -> progress.status == TaskStatus.ACTIVE)
                .map(progress -> List.of(progress.stepId))
                .orElseGet(List::of);
    }

    /**
     * 设置玩家指定任务中的运行时变量值。
     *
     * <p>该方法用于调试或脚本控制任务流程变量。设置成功后，后续流程条件和变量展示会读取新值。
     *
     * @param player 服务端玩家，要修改该玩家的任务变量
     * @param questId 任务标识，允许传入未规范化的任务文件标识
     * @param varName 变量名，对应任务文件导出的变量标识
     * @param value 变量的新浮点值
     * @return 是否成功设置变量
     */
    public static boolean setVariable(ServerPlayer player, String questId, String varName, float value) {
        return QuestRuntimeDebug.setVariable(player, questId, varName, value);
    }

    /**
     * 列出玩家当前保存的任务状态。
     *
     * <p>返回内容用于命令或调试界面展示该玩家已经接取的任务、分类和状态摘要。
     *
     * @param player 服务端玩家，要读取该玩家的任务列表
     * @return 任务状态文本列表，列表元素可直接作为反馈消息发送给玩家或命令源
     */
    public static List<Component> list(ServerPlayer player) {
        return QuestRuntimeDebug.list(player);
    }
}
