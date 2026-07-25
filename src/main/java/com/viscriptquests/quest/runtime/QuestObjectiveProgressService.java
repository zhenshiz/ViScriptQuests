package com.viscriptquests.quest.runtime;

import com.viscriptquests.quest.data.QuestFile;
import com.viscriptquests.quest.data.QuestVariableValue;
import com.viscriptquests.quest.data.runtime.TaskObjectiveProgress;
import com.viscriptquests.quest.data.runtime.TaskProgress;
import com.viscriptquests.quest.data.task.ITask;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;
import java.util.Map;
import java.util.Objects;

// 维护运行时目标进度的形状和展示数据，不负责小任务结算或流程推进。
final class QuestObjectiveProgressService {
    private QuestObjectiveProgressService() {
    }

    static boolean stepContainsTaskType(QuestFile questFile, String stepId, Class<? extends ITask> taskType) {
        if (taskType == null) {
            return true;
        }
        return questFile.findTasksForStep(stepId).stream().anyMatch(taskType::isInstance);
    }

    static boolean syncObjectiveShape(List<ITask> tasks, TaskProgress progress, ServerPlayer player) {
        return syncObjectiveShape(tasks, progress, player, null);
    }

    static boolean syncObjectiveShape(List<ITask> tasks, TaskProgress progress, ServerPlayer player,
                                      Map<String, QuestVariableValue> questVariables) {
        if (tasks.isEmpty()) {
            return false;
        }
        TaskProgress refreshed = TaskProgress.fromTasks(progress.stepId, tasks, null, player, questVariables);
        for (int i = 0; i < refreshed.objectives.size(); i++) {
            if (i >= progress.objectives.size()) {
                progress.objectives.add(refreshed.objectives.get(i));
                continue;
            }
            TaskObjectiveProgress current = progress.objectives.get(i);
            TaskObjectiveProgress fresh = refreshed.objectives.get(i);
            boolean sameObjective = Objects.equals(current.objectiveId, fresh.objectiveId);
            if (!sameObjective) {
                current.currentAmount = 0;
                current.completed = false;
                current.startedGameTime = -1L;
            }
            current.objectiveId = fresh.objectiveId;
            current.hint = fresh.displayHint();
            current.displayIcon = fresh.displayIcon;
            current.objectiveType = fresh.objectiveType;
            current.requiredAmount = fresh.requiredAmount;
            current.manualSubmitRequired = fresh.manualSubmitRequired;
            current.guideMarker = fresh.guideMarker;
            current.progressTextOverride = fresh.progressTextOverride == null ? "" : fresh.progressTextOverride;
            boolean refreshFromPlayerState = i < tasks.size() && tasks.get(i).refreshesProgressFromPlayerState();
            if (!current.completed && !current.manualSubmitRequired && refreshFromPlayerState) {
                current.progressTextOverride = "";
                tasks.get(i).refreshObjectiveProgress(player, current, questVariables);
            } else if (!refreshFromPlayerState) {
                current.currentAmount = Math.min(current.currentAmount, current.requiredAmount);
                current.completed = current.completed || current.currentAmount >= current.requiredAmount;
            }
        }
        while (progress.objectives.size() > refreshed.objectives.size()) {
            progress.objectives.removeLast();
        }
        progress.taskHint = refreshed.taskHint;
        progress.displayIcon = refreshed.displayIcon;
        progress.guideMarker = refreshed.guideMarker;
        progress.manualSubmitRequired = progress.objectives.stream().anyMatch(objective -> objective.manualSubmitRequired);
        return true;
    }
}
