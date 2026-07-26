package com.viscriptquests.quest.runtime;

import com.viscriptquests.event.neoforge.QuestEvent;
import com.viscriptquests.quest.data.QuestFile;
import com.viscriptquests.quest.data.QuestSavedData;
import com.viscriptquests.quest.data.ObjectiveAction;
import com.viscriptquests.quest.data.runtime.PlayerQuestState;
import com.viscriptquests.quest.data.runtime.ObjectiveStatus;
import com.viscriptquests.quest.data.runtime.QuestPlayerData;
import com.viscriptquests.quest.data.runtime.QuestStatus;
import com.viscriptquests.quest.data.runtime.TaskObjectiveProgress;
import com.viscriptquests.quest.data.runtime.TaskProgress;
import com.viscriptquests.quest.data.runtime.TaskStatus;
import com.viscriptquests.quest.data.task.AdvancementTask;
import com.viscriptquests.quest.data.task.BreakBlockTask;
import com.viscriptquests.quest.data.task.CountdownTask;
import com.viscriptquests.quest.data.task.CustomTriggerTask;
import com.viscriptquests.quest.data.task.EntityDeathTask;
import com.viscriptquests.quest.data.task.ITask;
import com.viscriptquests.quest.data.task.InteractEntityTask;
import com.viscriptquests.quest.data.task.KillEntityTask;
import com.viscriptquests.util.QuestFileHelper;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.NeoForge;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 处理玩家提交目标的运行时服务。
 *
 * <p>一个小任务可以包含多个目标。该服务先提交或自动完成当前激活的单个目标，
 * 再沿蓝图的“下一步”关系激活后继目标；当前目标路径中的必做目标完成后，
 * 才结算奖励并推进小任务流程。
 */
public class QuestSubmissionService {
    @FunctionalInterface
    public interface TaskProgressRecorder<T extends ITask> {
        boolean record(ServerPlayer player, T task, TaskObjectiveProgress objective, PlayerQuestState questState);
    }

    /**
     * 尝试提交玩家指定小任务中当前可提交的目标。
     *
     * <p>自动 tick 只处理允许自动提交的目标；手动命令会额外尝试提交手动目标。
     * 小任务只有在当前目标路径中的必做目标都完成后才会真正完成。
     *
     * @param player 服务端玩家，提交目标的玩家
     * @param questId 任务标识，必须已经是运行时使用的规范化标识
     * @param stepId 小任务标识，对应任务文件中的小任务节点
     * @param automatic 是否来自自动提交逻辑
     * @return 是否有目标提交成功，或是否因此完成了该小任务
     */
    public static boolean submit(ServerPlayer player, String questId, String stepId, boolean automatic) {
        QuestSavedData savedData = QuestSavedData.get(player.getServer());
        QuestPlayerData playerData = savedData.getPlayer(player.getUUID());
        var state = playerData.findQuest(questId);
        // 玩家没有接取该任务，或任务已经完成/失败时，不允许再提交小任务。
        if (state.isEmpty() || state.get().status != QuestStatus.ACTIVE) {
            QuestTeamProgressService.findCompletedQuestInScope(player, savedData, questId).ifPresent(ref -> {
                playerData.putQuest(QuestTeamProgressService.copyState(ref.state(), player.registryAccess()));
                savedData.setDirty();
                QuestTrackingService.refresh(player);
            });
            return false;
        }

        QuestFile questFile = QuestFileHelper.getQuest(questId, player.registryAccess()).orElse(null);
        // 运行时任务文件不存在时，无法读取目标定义、奖励和流程边。
        if (questFile == null) {
            return false;
        }

        var questState = state.get();
        var progress = questState.findStepProgress(stepId);
        // 玩家任务状态里没有这个小任务，说明 stepId 不属于当前任务或存档状态异常。
        if (progress.isEmpty()) {
            return false;
        }
        // 已完成的小任务不能重复提交，避免重复发奖励和重复推进流程。
        if (progress.get().status == TaskStatus.COMPLETED) {
            return false;
        }
        // 只有当前被流程解锁的小任务可以提交；锁定、跳过等状态都不应该响应提交。
        if (progress.get().status != TaskStatus.ACTIVE) {
            return false;
        }

        List<ITask> tasks = questFile.findTasksForStep(stepId);
        List<ObjectiveSnapshot> objectiveSnapshots = snapshotObjectives(progress.get());
        if (!QuestObjectiveProgressService.syncObjectiveShape(tasks, progress.get(), player, questState.questVariables)) {
            return false;
        }
        boolean changed = false;
        for (int i = 0; i < tasks.size(); i++) {
            TaskObjectiveProgress objective = progress.get().objectives.get(i);
            ObjectiveSnapshot snapshot = objectiveSnapshot(objectiveSnapshots, i, objective);
            if (objective.isCompleted()) {
                if (snapshot.differsFrom(objective)) {
                    changed = true;
                    postObjectiveEvents(player, questState, progress.get(), objective, i, snapshot, automatic);
                    if (!snapshot.completed) {
                        triggerObjectiveActions(player, questState, questFile, progress.get(), stepId, objective);
                    }
                    if (objective.isFailureCondition()) {
                        return failQuestFromObjective(player, savedData, playerData, questState, questFile,
                                progress.get(), stepId);
                    }
                }
                continue;
            }
            if (!objective.isActive() || snapshot.status != ObjectiveStatus.ACTIVE) {
                continue;
            }
            ITask task = tasks.get(i);
            boolean submitted = objective.isFailureCondition()
                    ? task.autoCompleteObjective(player, objective, questState.questVariables)
                    : (task.allowsAutoSubmit()
                    ? task.autoCompleteObjective(player, objective, questState.questVariables)
                    : !automatic && task.submitObjective(player, objective, questState.questVariables));
            boolean objectiveChanged = snapshot.differsFrom(objective);
            if (submitted || objectiveChanged) {
                changed = true;
                postObjectiveEvents(player, questState, progress.get(), objective, i, snapshot, automatic);
            }
            if (submitted) {
                triggerObjectiveActions(player, questState, questFile, progress.get(), stepId, objective);
                if (objective.isFailureCondition() && objective.isCompleted()) {
                    return failQuestFromObjective(player, savedData, playerData, questState, questFile,
                            progress.get(), stepId);
                }
            }
        }
        if (progress.get().hasTriggeredFailureObjective()) {
            return failQuestFromObjective(player, savedData, playerData, questState, questFile, progress.get(), stepId);
        }
        if (!changed && !progress.get().areAllObjectivesCompleted()) {
            return false;
        }
        return completeStepIfReady(player, savedData, playerData, questState, questFile, progress.get(), stepId);
    }

    public static boolean submitObjective(ServerPlayer player, String questId, String stepId, int objectiveIndex) {
        QuestSavedData savedData = QuestSavedData.get(player.getServer());
        QuestPlayerData playerData = savedData.getPlayer(player.getUUID());
        var state = playerData.findQuest(questId);
        if (state.isEmpty() || state.get().status != QuestStatus.ACTIVE) {
            QuestTeamProgressService.findCompletedQuestInScope(player, savedData, questId).ifPresent(ref -> {
                playerData.putQuest(QuestTeamProgressService.copyState(ref.state(), player.registryAccess()));
                savedData.setDirty();
                QuestTrackingService.refresh(player);
            });
            return false;
        }
        QuestFile questFile = QuestFileHelper.getQuest(questId, player.registryAccess()).orElse(null);
        if (questFile == null) {
            return false;
        }
        PlayerQuestState questState = state.get();
        var progress = questState.findStepProgress(stepId);
        if (progress.isEmpty() || progress.get().status != TaskStatus.ACTIVE) {
            return false;
        }
        List<ITask> tasks = questFile.findTasksForStep(stepId);
        List<ObjectiveSnapshot> objectiveSnapshots = snapshotObjectives(progress.get());
        if (!QuestObjectiveProgressService.syncObjectiveShape(tasks, progress.get(), player, questState.questVariables)
                || objectiveIndex < 0
                || objectiveIndex >= tasks.size()
                || objectiveIndex >= progress.get().objectives.size()) {
            return false;
        }
        TaskObjectiveProgress objective = progress.get().objectives.get(objectiveIndex);
        if (!objective.isActive() || objective.isFailureCondition()) {
            return false;
        }
        ObjectiveSnapshot snapshot = objectiveSnapshot(objectiveSnapshots, objectiveIndex, objective);
        if (!tasks.get(objectiveIndex).submitObjective(player, objective, questState.questVariables)) {
            if (snapshot.differsFrom(objective)) {
                postObjectiveEvents(player, questState, progress.get(), objective, objectiveIndex, snapshot, false);
                savedData.setDirty();
                QuestTrackingService.refresh(player);
                QuestTeamProgressService.syncQuestState(player, questState);
            }
            return false;
        }
        postObjectiveEvents(player, questState, progress.get(), objective, objectiveIndex, snapshot, false);
        triggerObjectiveActions(player, questState, questFile, progress.get(), stepId, objective);
        return completeStepIfReady(player, savedData, playerData, questState, questFile, progress.get(), stepId);
    }

    /**
     * 自动提交玩家当前所有激活小任务中指定类型的目标。
     *
     * <p>物品数量、当前位置等可以从玩家当前状态直接重算的目标，联动方不需要手动修改
     * {@link TaskObjectiveProgress}，只需要在合适的服务端事件里调用这个入口。
     * 该方法会复用标准提交流程，自动处理目标进度刷新、小任务完成、奖励、流程推进和 HUD 刷新。
     */
    public static boolean submitActiveTasks(ServerPlayer player, Class<? extends ITask> taskType) {
        if (player == null || player.level().isClientSide()) {
            return false;
        }
        QuestSavedData savedData = QuestSavedData.get(player.getServer());
        QuestPlayerData playerData = savedData.getPlayer(player.getUUID());
        boolean changed = false;
        for (PlayerQuestState questState : new ArrayList<>(playerData.quests)) {
            if (questState.status != QuestStatus.ACTIVE) {
                continue;
            }
            QuestFile questFile = QuestFileHelper.getQuest(questState.questId, player.registryAccess()).orElse(null);
            if (questFile == null) {
                continue;
            }
            for (TaskProgress progress : new ArrayList<>(questState.taskProgresses)) {
                if (progress.status != TaskStatus.ACTIVE
                        || !QuestObjectiveProgressService.stepContainsTaskType(questFile, progress.stepId, taskType)) {
                    continue;
                }
                if (submit(player, questState.questId, progress.stepId, true)) {
                    changed = true;
                }
            }
        }
        return changed;
    }

    /**
     * 记录事件累计型目标的进度，并把后续结算交回任务系统。
     *
     * <p>击杀、对话、打开方块等事件驱动目标通常不能从玩家当前状态反推进度。
     * 联动方在 recorder 中只修改当前目标的 {@code currentAmount / requiredAmount}，
     * 并在满足完成条件时调用 {@link TaskObjectiveProgress#complete()}；
     * 该方法会负责保存数据、完成小任务、发放奖励、推进流程、刷新追踪 HUD 和同步队伍状态。
     * 如果被修改的目标是失败条件并被标记完成，则会结束当前任务为失败。
     */
    public static <T extends ITask> boolean recordTaskProgress(ServerPlayer player, Class<T> taskType,
                                                               TaskProgressRecorder<T> recorder) {
        if (player == null || taskType == null || recorder == null || player.level().isClientSide()) {
            return false;
        }
        QuestSavedData savedData = QuestSavedData.get(player.getServer());
        QuestPlayerData playerData = savedData.getPlayer(player.getUUID());
        boolean changed = false;
        for (PlayerQuestState questState : new ArrayList<>(playerData.quests)) {
            if (questState.status != QuestStatus.ACTIVE) {
                continue;
            }
            QuestFile questFile = QuestFileHelper.getQuest(questState.questId, player.registryAccess()).orElse(null);
            if (questFile == null) {
                continue;
            }
            for (TaskProgress progress : new ArrayList<>(questState.taskProgresses)) {
                if (progress.status != TaskStatus.ACTIVE) {
                    continue;
                }
                if (recordTaskProgressForStep(player, savedData, playerData, questState, questFile,
                        progress, taskType, recorder)) {
                    changed = true;
                }
            }
        }
        return changed;
    }

    public static boolean recordEntityKill(ServerPlayer player, LivingEntity killedEntity) {
        if (player == null || killedEntity == null || player.level().isClientSide()) {
            return false;
        }
        return recordTaskProgress(player, KillEntityTask.class, (recordPlayer, killTask, objective, questState) -> {
            if (!killTask.matches(killedEntity)) {
                return false;
            }
            if (!objective.isActive()) {
                return false;
            }
            int required = Math.max(1, killTask.getRequiredAmount(questState.questVariables, recordPlayer));
            objective.requiredAmount = required;
            objective.currentAmount = Math.min(required, Math.max(0, objective.currentAmount) + 1);
            if (objective.currentAmount >= required) {
                objective.complete();
            }
            return true;
        });
    }

    public static boolean recordEntityDeath(LivingEntity deadEntity) {
        if (deadEntity == null || deadEntity.level().isClientSide() || deadEntity.getServer() == null) {
            return false;
        }
        PlayerList players = deadEntity.getServer().getPlayerList();
        boolean changed = false;
        for (ServerPlayer player : players.getPlayers()) {
            if (recordTaskProgress(player, EntityDeathTask.class, (recordPlayer, deathTask, objective, questState) -> {
                if (!deathTask.matches(deadEntity)) {
                    return false;
                }
                if (!objective.isActive()) {
                    return false;
                }
                int required = Math.max(1, deathTask.getRequiredAmount(questState.questVariables, recordPlayer));
                objective.requiredAmount = required;
                objective.currentAmount = Math.min(required, Math.max(0, objective.currentAmount) + 1);
                if (objective.currentAmount >= required) {
                    objective.complete();
                }
                return true;
            })) {
                changed = true;
            }
        }
        return changed;
    }

    public static boolean recordBlockBreak(ServerPlayer player, BlockState blockState) {
        if (player == null || blockState == null || player.level().isClientSide()) {
            return false;
        }
        return recordTaskProgress(player, BreakBlockTask.class, (recordPlayer, breakTask, objective, questState) -> {
            if (!breakTask.matches(blockState)) {
                return false;
            }
            if (!objective.isActive()) {
                return false;
            }
            int required = Math.max(1, breakTask.getRequiredAmount(questState.questVariables, recordPlayer));
            objective.requiredAmount = required;
            objective.currentAmount = Math.min(required, Math.max(0, objective.currentAmount) + 1);
            if (objective.currentAmount >= required) {
                objective.complete();
            }
            return true;
        });
    }

    public static boolean recordEntityInteraction(ServerPlayer player, Entity target) {
        if (player == null || target == null || player.level().isClientSide()) {
            return false;
        }
        return recordTaskProgress(player, InteractEntityTask.class, (recordPlayer, interactTask, objective, questState) -> {
            if (!interactTask.matches(target)) {
                return false;
            }
            if (!objective.isActive()) {
                return false;
            }
            int required = Math.max(1, interactTask.getRequiredAmount(questState.questVariables, recordPlayer));
            objective.requiredAmount = required;
            objective.currentAmount = required;
            objective.complete();
            return true;
        });
    }

    public static boolean recordAdvancementEarn(ServerPlayer player, AdvancementHolder advancement) {
        if (player == null || advancement == null || player.level().isClientSide()) {
            return false;
        }
        return recordTaskProgress(player, AdvancementTask.class, (recordPlayer, advancementTask, objective, questState) -> {
            if (!advancementTask.matches(advancement) || !objective.isActive()) {
                return false;
            }
            int required = Math.max(1, advancementTask.getRequiredAmount(questState.questVariables, recordPlayer));
            objective.requiredAmount = required;
            objective.currentAmount = required;
            objective.complete();
            return true;
        });
    }

    public static boolean triggerCustom(ServerPlayer player, String triggerId) {
        if (player == null || triggerId == null || triggerId.isBlank() || player.level().isClientSide()) {
            return false;
        }
        return recordTaskProgress(player, CustomTriggerTask.class, (recordPlayer, triggerTask, objective, questState) -> {
            if (!triggerTask.matches(triggerId) || !objective.isActive()) {
                return false;
            }
            int required = Math.max(1, triggerTask.getRequiredAmount(questState.questVariables, recordPlayer));
            objective.requiredAmount = required;
            objective.currentAmount = required;
            objective.complete();
            return true;
        });
    }

    public static boolean tickCountdownTasks(ServerPlayer player) {
        return recordTaskProgress(player, CountdownTask.class,
                (recordPlayer, countdownTask, objective, questState) ->
                        countdownTask.autoCompleteObjective(recordPlayer, objective, questState.questVariables));
    }

    private static <T extends ITask> boolean recordTaskProgressForStep(ServerPlayer player, QuestSavedData savedData,
                                                                       QuestPlayerData playerData,
                                                                       PlayerQuestState questState,
                                                                       QuestFile questFile,
                                                                       TaskProgress progress,
                                                                       Class<T> taskType,
                                                                       TaskProgressRecorder<T> recorder) {
        List<ITask> tasks = questFile.findTasksForStep(progress.stepId);
        List<ObjectiveSnapshot> objectiveSnapshots = snapshotObjectives(progress);
        if (!QuestObjectiveProgressService.syncObjectiveShape(tasks, progress, player, questState.questVariables)) {
            return false;
        }
        boolean changed = false;
        for (int i = 0; i < tasks.size() && i < progress.objectives.size(); i++) {
            ITask task = tasks.get(i);
            if (!taskType.isInstance(task)) {
                continue;
            }
            TaskObjectiveProgress objective = progress.objectives.get(i);
            ObjectiveSnapshot snapshot = objectiveSnapshot(objectiveSnapshots, i, objective);
            if (!objective.isActive() || snapshot.status != ObjectiveStatus.ACTIVE) {
                continue;
            }
            boolean recorded = recorder.record(player, taskType.cast(task), objective, questState);
            if (recorded || snapshot.differsFrom(objective)) {
                changed = true;
                postObjectiveEvents(player, questState, progress, objective, i, snapshot, true);
                if (!snapshot.completed && objective.isCompleted()) {
                    triggerObjectiveActions(player, questState, questFile, progress, progress.stepId, objective);
                }
                if (objective.isFailureCondition() && objective.isCompleted()) {
                    return failQuestFromObjective(player, savedData, playerData, questState, questFile,
                            progress, progress.stepId);
                }
            }
        }
        if (!changed) {
            return false;
        }
        completeStepIfReady(player, savedData, playerData, questState, questFile, progress, progress.stepId);
        return true;
    }

    private static boolean completeStepIfReady(ServerPlayer player, QuestSavedData savedData, QuestPlayerData playerData,
                                               PlayerQuestState questState, QuestFile questFile,
                                               TaskProgress progress, String stepId) {
        if (progress.hasTriggeredFailureObjective()) {
            return failQuestFromObjective(player, savedData, playerData, questState, questFile, progress, stepId);
        }
        progress.refreshSummary();
        if (!progress.areAllObjectivesCompleted()) {
            savedData.setDirty();
            QuestTrackingService.refresh(player);
            QuestTeamProgressService.syncQuestState(player, questState);
            return true;
        }
        // 记录提交前已经激活的小任务，提交后追踪服务会用它优先选出刚解锁的新小任务。
        Set<String> activeStepIdsBeforeSubmit = activeStepIds(questState);
        // 到这里才真正提交成功：写进度、发奖励、推进蓝图流程，并刷新任务追踪。
        progress.skipUnfinishedObjectives();
        progress.status = TaskStatus.COMPLETED;
        NeoForge.EVENT_BUS.post(new QuestEvent.TaskCompleted(player, questState, progress, false));
        QuestCompletionNotificationService.notifyTaskCompleted(player, progress);
        QuestRewardService.grantStepRewards(player, questFile, questState, stepId);
        QuestFlowExecutor.completeStepNode(player, questState, questFile, stepId);
        savedData.setDirty();
        QuestTrackingService.refreshAfterStepSubmit(player, playerData, questState, stepId, activeStepIdsBeforeSubmit);
        QuestTeamProgressService.syncQuestState(player, questState);
        return true;
    }

    private static void triggerObjectiveActions(ServerPlayer player, PlayerQuestState questState, QuestFile questFile,
                                                TaskProgress progress, String stepId,
                                                TaskObjectiveProgress objective) {
        if (objective == null || !objective.isCompleted()
                || objective.objectiveId == null || objective.objectiveId.isBlank()) {
            return;
        }
        boolean activatedObjective = false;
        for (ObjectiveAction action : questFile.findObjectiveActions(stepId, objective.objectiveId)) {
            if (action == null || action.actionId == null || action.actionId.isBlank()) {
                continue;
            }
            String triggerKey = stepId + ":" + objective.objectiveId + ":" + action.actionId;
            if (!questState.triggeredObjectiveActions.add(triggerKey)) {
                continue;
            }
            if (!canRunObjectiveAction(player, questState, action)) {
                continue;
            }
            if (action.edge != null) {
                action.edge.applyMutations(questState.questVariables, player.registryAccess(), player);
                QuestFlowExecutor.printDebugMessages(player, questState, action.edge);
            }
            for (var reward : action.rewards) {
                QuestRewardService.grantDynamicReward(player, reward, questState, stepId);
            }
            for (String targetObjectiveId : action.activateObjectiveIds) {
                activatedObjective |= activateObjective(player, questState, questFile, progress,
                        stepId, targetObjectiveId);
            }
        }
        if (activatedObjective) {
            progress.refreshSummary();
        }
    }

    private static boolean activateObjective(ServerPlayer player, PlayerQuestState questState, QuestFile questFile,
                                             TaskProgress progress, String stepId, String objectiveId) {
        TaskObjectiveProgress objective = progress.findObjectiveProgress(objectiveId).orElse(null);
        if (objective == null || !objective.activate()) {
            return false;
        }
        for (ITask task : questFile.findTasksForStep(stepId)) {
            if (task != null && objectiveId.equals(task.objectiveId)) {
                task.refreshObjectiveProgress(player, objective, questState.questVariables);
                break;
            }
        }
        return true;
    }

    private static boolean canRunObjectiveAction(ServerPlayer player, PlayerQuestState questState, ObjectiveAction action) {
        for (var gate : action.gates) {
            if (gate != null && !gate.evaluate(questState.questVariables, player.registryAccess(), player)) {
                return false;
            }
        }
        return action.edge == null || action.edge.evaluate(questState.questVariables, player.registryAccess(), player);
    }

    private static boolean failQuestFromObjective(ServerPlayer player, QuestSavedData savedData,
                                                  QuestPlayerData playerData, PlayerQuestState questState,
                                                  QuestFile questFile, TaskProgress progress, String stepId) {
        Set<String> activeStepIdsBeforeSubmit = activeStepIds(questState);
        QuestFlowExecutor.failStepNode(player, questState, questFile, stepId);
        savedData.setDirty();
        if (questState.status != QuestStatus.ACTIVE && playerData.trackedQuestId.equals(questState.questId)) {
            playerData.trackedQuestId = "";
            playerData.trackedStepId = "";
            QuestTrackingService.refresh(player);
        } else {
            QuestTrackingService.refreshAfterStepSubmit(player, playerData, questState, stepId, activeStepIdsBeforeSubmit);
        }
        QuestTeamProgressService.syncQuestState(player, questState);
        return true;
    }

    private static void postObjectiveEvents(ServerPlayer player, PlayerQuestState questState,
                                            TaskProgress progress, TaskObjectiveProgress objective,
                                            int objectiveIndex, ObjectiveSnapshot snapshot,
                                            boolean automatic) {
        if (snapshot.differsFrom(objective)) {
            NeoForge.EVENT_BUS.post(new QuestEvent.ObjectiveProgress(player, questState, progress, objective,
                    objectiveIndex, snapshot.amount, snapshot.requiredAmount, snapshot.completed, automatic));
        }
        if (!snapshot.completed && objective.isCompleted()) {
            NeoForge.EVENT_BUS.post(new QuestEvent.ObjectiveCompleted(player, questState, progress, objective,
                    objectiveIndex, snapshot.amount, snapshot.requiredAmount, false, automatic));
        }
    }

    private static List<ObjectiveSnapshot> snapshotObjectives(TaskProgress progress) {
        List<ObjectiveSnapshot> snapshots = new ArrayList<>(progress.objectives.size());
        for (TaskObjectiveProgress objective : progress.objectives) {
            snapshots.add(ObjectiveSnapshot.of(objective));
        }
        return snapshots;
    }

    private static ObjectiveSnapshot objectiveSnapshot(List<ObjectiveSnapshot> snapshots, int index,
                                                       TaskObjectiveProgress objective) {
        return index >= 0 && index < snapshots.size() ? snapshots.get(index) : ObjectiveSnapshot.of(objective);
    }

    private record ObjectiveSnapshot(int amount, int requiredAmount, ObjectiveStatus status, boolean completed) {
        private static ObjectiveSnapshot of(TaskObjectiveProgress objective) {
            return new ObjectiveSnapshot(objective.currentAmount, objective.requiredAmount,
                    objective.status, objective.isCompleted());
        }

        private boolean differsFrom(TaskObjectiveProgress objective) {
            return amount != objective.currentAmount
                    || requiredAmount != objective.requiredAmount
                    || status != objective.status;
        }
    }

    private static Set<String> activeStepIds(PlayerQuestState questState) {
        Set<String> stepIds = new LinkedHashSet<>();
        for (var progress : questState.taskProgresses) {
            if (progress.status == TaskStatus.ACTIVE) {
                stepIds.add(progress.stepId);
            }
        }
        return stepIds;
    }
}
