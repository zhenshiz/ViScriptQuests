package com.viscriptquests.quest.runtime;

import com.lowdragmc.lowdraglib2.Platform;
import com.lowdragmc.lowdraglib2.networking.rpc.RPCPacketDistributor;
import com.viscriptquests.network.s2c.S2CPayload;
import com.viscriptquests.quest.data.QuestFile;
import com.viscriptquests.quest.data.QuestSavedData;
import com.viscriptquests.quest.data.runtime.PlayerQuestState;
import com.viscriptquests.quest.data.runtime.QuestPlayerData;
import com.viscriptquests.quest.data.runtime.QuestStatus;
import com.viscriptquests.quest.data.runtime.TaskProgress;
import com.viscriptquests.quest.data.runtime.TaskStatus;
import com.viscriptquests.util.QuestFileHelper;
import lombok.experimental.UtilityClass;
import net.minecraft.server.level.ServerPlayer;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * 管理玩家当前追踪的任务和小任务目标。
 *
 * <p>此服务只维护 <code>QuestPlayerData.trackedQuestId</code> 和
 * <code>QuestPlayerData.trackedStepId</code> 两个运行时字段，不负责判断任务完成条件、
 * 推进流程节点或发放奖励。任务流程服务在发放、提交、撤销或完成任务后调用此服务，
 * 让玩家的追踪目标保持在仍然可执行的激活小任务上。
 * <p>设计上追踪状态是玩家存档数据的一部分，而不是从任务流程每次临时推导。这样任务书、
 * 命令和未来的 HUD 同步逻辑都可以读取同一份状态。除 <code>track</code> 方法外，
 * 其余方法通常由已经持有 <code>QuestSavedData</code> 的流程调用方负责标记存档为已修改。
 */
public class QuestTrackingService {
    /**
     * 将指定任务设为玩家当前追踪任务。
     *
     * <p>任务标识会先规范化。只有玩家已经拥有该任务，并且该任务仍处于激活状态时，
     * 才会把追踪目标切换到该任务的第一个激活小任务。设置成功后会标记玩家任务存档为已修改。
     *
     * @param player  服务端玩家，要修改该玩家的追踪状态
     * @param questId 任务标识，允许传入未规范化的任务文件标识
     * @return 成功设置追踪任务时返回 <code>true</code>；任务不存在或未激活时返回 <code>false</code>
     */
    public static boolean track(ServerPlayer player, String questId) {
        String normalizedQuestId = QuestFileHelper.normalizeQuestId(questId);
        QuestSavedData savedData = QuestSavedData.get(player.getServer());
        QuestPlayerData playerData = savedData.getPlayer(player.getUUID());
        var state = playerData.findQuest(normalizedQuestId);
        if (state.isEmpty() || state.get().status != QuestStatus.ACTIVE) {
            return false;
        }
        trackFirstActiveStep(player, playerData, state.get());
        savedData.setDirty();
        return true;
    }

    /**
     * 将追踪目标设置为任务中的第一个激活小任务。
     *
     * <p>此方法用于任务刚发放、手动选择追踪任务或需要重新定位追踪目标的场景。
     * 如果任务内没有激活小任务，<code>trackedStepId</code> 会被设置为空字符串，
     * 但 <code>trackedQuestId</code> 仍会记录该任务。调用方负责在需要时标记存档为已修改。
     *
     * @param player     服务端玩家，用于触发追踪状态刷新
     * @param playerData 玩家任务数据，保存追踪任务和追踪小任务字段
     * @param state      玩家任务状态，提供任务标识和小任务进度
     */
    public static void trackFirstActiveStep(ServerPlayer player, QuestPlayerData playerData, PlayerQuestState state) {
        playerData.trackedQuestId = state.questId;
        playerData.trackedStepId = firstActiveStepId(state);
        refresh(player);
    }

    /**
     * 在指定任务正被追踪时清空玩家追踪状态。
     *
     * <p>此方法用于撤销任务、任务结束或任务状态失效后的清理流程。只有当前追踪任务与
     * 参数任务标识一致时才会清空字段，避免误清除玩家正在追踪的其他任务。调用方负责标记存档为已修改。
     *
     * @param player  服务端玩家，要清理该玩家的追踪状态
     * @param questId 任务标识，必须与玩家数据中保存的追踪任务标识一致才会生效
     */
    public static void clearTrackedQuest(ServerPlayer player, String questId) {
        QuestPlayerData playerData = QuestSavedData.get(player.getServer()).getPlayer(player.getUUID());
        if (playerData.trackedQuestId.equals(questId)) {
            playerData.trackedQuestId = "";
            playerData.trackedStepId = "";
        }
        refresh(player);
    }

    /**
     * 在小任务提交后刷新玩家的追踪目标。
     *
     * <p>只有玩家当前追踪的正是刚完成的小任务时，才会移动到同一任务中的下一个激活小任务。
     * 如果没有新的激活小任务，并且任务已经不再处于激活状态，则清空追踪任务。调用方负责在
     * 完成任务状态修改后标记存档为已修改。
     *
     * @param player          服务端玩家，用于触发追踪状态刷新
     * @param playerData      玩家任务数据，保存当前追踪任务和追踪小任务字段
     * @param questState      玩家任务状态，提供新的小任务进度和任务状态
     * @param completedStepId 字符串标识，表示刚完成的小任务
     */
    public static void refreshAfterStepSubmit(ServerPlayer player, QuestPlayerData playerData,
                                       PlayerQuestState questState, String completedStepId,
                                       Set<String> activeStepIdsBeforeSubmit) {
        if (playerData.trackedQuestId.equals(questState.questId) && playerData.trackedStepId.equals(completedStepId)) {
            if (questState.status != QuestStatus.ACTIVE) {
                playerData.trackedQuestId = "";
                playerData.trackedStepId = "";
            } else {
                playerData.trackedQuestId = questState.questId;
                playerData.trackedStepId = nextStepAfterSubmit(questState, completedStepId, activeStepIdsBeforeSubmit);
            }
        }
        refresh(player);
    }

    /**
     * 刷新玩家客户端或界面侧的追踪状态展示。
     *
     * @param player 服务端玩家，要刷新该玩家的追踪状态展示
     */
    public static void refresh(ServerPlayer player) {
        QuestPlayerData playerData = QuestSavedData.get(player.getServer()).getPlayer(player.getUUID());
        refreshTrackedTaskObjectives(player, playerData);
        RPCPacketDistributor.rpcToPlayer(player, S2CPayload.SYNC_QUEST_HUD,
                playerData.serializeNBT(Platform.getFrozenRegistry()));
    }

    private static void refreshTrackedTaskObjectives(ServerPlayer player, QuestPlayerData playerData) {
        if (playerData.trackedQuestId == null || playerData.trackedQuestId.isBlank()
                || playerData.trackedStepId == null || playerData.trackedStepId.isBlank()) {
            return;
        }
        PlayerQuestState state = playerData.findQuest(playerData.trackedQuestId).orElse(null);
        if (state == null) {
            return;
        }
        TaskProgress progress = state.findStepProgress(playerData.trackedStepId).orElse(null);
        if (progress == null) {
            return;
        }
        QuestFile questFile = QuestFileHelper.getQuest(playerData.trackedQuestId, player.registryAccess()).orElse(null);
        if (questFile != null) {
            progress.refreshObjectives(questFile, player);
        }
    }

    /**
     * 返回任务状态中的第一个激活小任务标识。
     *
     * <p>选择第一个激活小任务作为自动追踪目标，可以让线性任务和分支任务都使用同一套规则。
     * 对于同时存在多个激活小任务的并行流程，返回值取决于 <code>taskProgresses</code> 的保存顺序。
     *
     * @param state 玩家任务状态，提供小任务进度列表
     * @return 第一个激活小任务的标识；没有激活小任务时返回空字符串
     */
    private static String firstActiveStepId(PlayerQuestState state) {
        return state.taskProgresses.stream()
                .filter(progress -> progress.status == TaskStatus.ACTIVE)
                .map(progress -> progress.stepId)
                .findFirst()
                .orElse("");
    }

    /**
     * 按提交后的任务流选择新的追踪小任务。
     *
     * <p>优先选择这次提交刚解锁的小任务；如果没有新解锁的小任务，再回到同一个大任务里
     * 已经激活但未完成的小任务。这样并行分支不会被新分支抢乱，同时任务完成时也不会自动跳到别的大任务。
     *
     * @param state 玩家任务状态，提供提交后的最新小任务进度
     * @param completedStepId 刚刚完成的小任务标识
     * @param activeStepIdsBeforeSubmit 提交前已经处于激活状态的小任务集合
     * @return 应该继续追踪的小任务标识；没有可追踪目标时返回空字符串
     */
    private static String nextStepAfterSubmit(PlayerQuestState state, String completedStepId,
                                              Set<String> activeStepIdsBeforeSubmit) {
        Set<String> activeStepIds = activeStepIds(state);
        return activeStepIds.stream()
                .filter(stepId -> !stepId.equals(completedStepId))
                .filter(stepId -> activeStepIdsBeforeSubmit == null || !activeStepIdsBeforeSubmit.contains(stepId))
                .findFirst()
                .orElseGet(() -> activeStepIds.stream()
                        .filter(stepId -> !stepId.equals(completedStepId))
                        .findFirst()
                        .orElse(""));
    }

    private static Set<String> activeStepIds(PlayerQuestState state) {
        Set<String> stepIds = new LinkedHashSet<>();
        for (TaskProgress progress : state.taskProgresses) {
            if (progress.status == TaskStatus.ACTIVE) {
                stepIds.add(progress.stepId);
            }
        }
        return stepIds;
    }
}
