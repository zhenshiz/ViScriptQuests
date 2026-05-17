package com.viscriptquests.quest.runtime;

import com.viscriptquests.quest.data.QuestFile;
import com.viscriptquests.quest.data.QuestSavedData;
import com.viscriptquests.quest.data.runtime.PlayerQuestState;
import com.viscriptquests.quest.data.runtime.QuestPlayerData;
import com.viscriptquests.quest.data.runtime.QuestStatus;
import com.viscriptquests.quest.data.runtime.TaskObjectiveProgress;
import com.viscriptquests.quest.data.runtime.TaskProgress;
import com.viscriptquests.quest.data.runtime.TaskStatus;
import com.viscriptquests.quest.data.task.ITask;
import com.viscriptquests.util.QuestFileHelper;
import net.minecraft.server.level.ServerPlayer;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 处理玩家提交目标的运行时服务。
 *
 * <p>一个小任务可以包含多个目标。该服务先提交或自动完成单个目标，
 * 只有同一个小任务下的所有目标完成后，才结算奖励并推进蓝图流程。
 */
public class QuestSubmissionService {
    /**
     * 尝试提交玩家指定小任务中当前可提交的目标。
     *
     * <p>自动 tick 只处理允许自动提交的目标；手动命令会额外尝试提交手动目标。
     * 小任务只有在所有目标都完成后才会真正完成。
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
        if (!syncObjectiveShape(tasks, progress.get(), player)) {
            return false;
        }
        boolean changed = false;
        for (int i = 0; i < tasks.size(); i++) {
            TaskObjectiveProgress objective = progress.get().objectives.get(i);
            if (objective.completed) {
                continue;
            }
            ITask task = tasks.get(i);
            boolean submitted = task.allowsAutoSubmit()
                    ? task.autoCompleteObjective(player, objective)
                    : !automatic && task.submitObjective(player, objective);
            if (submitted) {
                changed = true;
            }
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
        if (!syncObjectiveShape(tasks, progress.get(), player)
                || objectiveIndex < 0
                || objectiveIndex >= tasks.size()
                || objectiveIndex >= progress.get().objectives.size()) {
            return false;
        }
        TaskObjectiveProgress objective = progress.get().objectives.get(objectiveIndex);
        if (!tasks.get(objectiveIndex).submitObjective(player, objective)) {
            return false;
        }
        return completeStepIfReady(player, savedData, playerData, questState, questFile, progress.get(), stepId);
    }

    private static boolean completeStepIfReady(ServerPlayer player, QuestSavedData savedData, QuestPlayerData playerData,
                                               PlayerQuestState questState, QuestFile questFile,
                                               TaskProgress progress, String stepId) {
        if (!progress.areAllObjectivesCompleted()) {
            savedData.setDirty();
            QuestTrackingService.refresh(player);
            return true;
        }
        // 记录提交前已经激活的小任务，提交后追踪服务会用它优先选出刚解锁的新小任务。
        Set<String> activeStepIdsBeforeSubmit = activeStepIds(questState);
        // 到这里才真正提交成功：写进度、发奖励、推进蓝图流程，并刷新任务追踪。
        progress.status = TaskStatus.COMPLETED;
        QuestRewardService.grantStepRewards(player, questFile, questState, stepId);
        QuestFlowExecutor.completeStepNode(player, questState, questFile, stepId);
        savedData.setDirty();
        QuestTrackingService.refreshAfterStepSubmit(player, playerData, questState, stepId, activeStepIdsBeforeSubmit);
        return true;
    }

    private static boolean syncObjectiveShape(List<ITask> tasks, TaskProgress progress, ServerPlayer player) {
        if (tasks.isEmpty()) {
            return false;
        }
        TaskProgress refreshed = TaskProgress.fromTasks(progress.stepId, tasks, null, player);
        for (int i = 0; i < refreshed.objectives.size(); i++) {
            if (i >= progress.objectives.size()) {
                progress.objectives.add(refreshed.objectives.get(i));
                continue;
            }
            TaskObjectiveProgress current = progress.objectives.get(i);
            TaskObjectiveProgress fresh = refreshed.objectives.get(i);
            current.hint = fresh.hint;
            current.displayIcon = fresh.displayIcon;
            current.requiredAmount = fresh.requiredAmount;
            current.manualSubmitRequired = fresh.manualSubmitRequired;
            current.guideMarker = fresh.guideMarker;
            if (!current.completed && !current.manualSubmitRequired) {
                current.currentAmount = fresh.currentAmount;
                current.completed = fresh.completed;
            }
        }
        while (progress.objectives.size() > refreshed.objectives.size()) {
            progress.objectives.removeLast();
        }
        progress.manualSubmitRequired = progress.objectives.stream().anyMatch(objective -> objective.manualSubmitRequired);
        return true;
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
