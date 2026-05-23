package com.viscriptquests.quest.data.runtime;

import com.lowdragmc.lowdraglib2.syncdata.IPersistedSerializable;
import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted;
import com.viscriptquests.quest.data.DisplayIcon;
import com.viscriptquests.quest.data.QuestFile;
import com.viscriptquests.quest.data.QuestStep;
import com.viscriptquests.quest.data.task.ITask;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.ArrayList;
import java.util.Objects;

// 任务目标的运行时进度追踪
public class TaskProgress implements IPersistedSerializable {
    @Persisted
    public String stepId = "";
    @Persisted
    public String title = "";
    @Persisted
    public String subtitle = "";
    @Persisted
    public String[] description = new String[0];
    @Persisted
    public Component taskHint = Component.empty();
    @Persisted
    public boolean manualSubmitRequired = false;
    @Persisted
    public DisplayIcon displayIcon = new DisplayIcon();
    @Persisted
    public QuestGuideMarker guideMarker = new QuestGuideMarker();
    @Persisted
    public final List<TaskObjectiveProgress> objectives = new ArrayList<>();
    @Persisted
    public TaskStatus status = TaskStatus.ACTIVE;

    public static TaskProgress fromTask(ITask task, QuestStep step) {
        return fromTask(task, step, null);
    }

    public static TaskProgress fromTask(ITask task, QuestStep step, net.minecraft.server.level.ServerPlayer player) {
        TaskProgress progress = new TaskProgress();
        progress.stepId = task.stepId;
        if (step != null) {
            progress.title = step.title;
            progress.subtitle = step.subtitle;
            progress.description = step.description.clone();
        }
        TaskObjectiveProgress objective = TaskObjectiveProgress.fromTask(task, player);
        progress.objectives.add(objective);
        progress.manualSubmitRequired = objective.manualSubmitRequired;
        progress.taskHint = objective.displayHint();
        progress.displayIcon = objective.displayIcon;
        progress.guideMarker = objective.guideMarker.copy();
        progress.status = TaskStatus.ACTIVE;
        return progress;
    }

    public static TaskProgress fromTasks(String stepId, List<ITask> tasks, QuestStep step,
                                         net.minecraft.server.level.ServerPlayer player) {
        TaskProgress progress = new TaskProgress();
        progress.stepId = stepId == null ? "" : stepId;
        if (step != null) {
            progress.title = step.title;
            progress.subtitle = step.subtitle;
            progress.description = step.description.clone();
        }
        if (tasks == null || tasks.isEmpty()) {
            progress.status = TaskStatus.ACTIVE;
            return progress;
        }
        progress.objectives.clear();
        for (ITask task : tasks) {
            progress.objectives.add(TaskObjectiveProgress.fromTask(task, player));
        }
        progress.manualSubmitRequired = progress.objectives.stream().anyMatch(objective -> objective.manualSubmitRequired);
        progress.taskHint = joinObjectiveHints(progress.objectives);
        TaskObjectiveProgress displayObjective = selectDisplayObjective(progress.objectives);
        progress.displayIcon = displayObjective.displayIcon;
        progress.guideMarker = displayObjective.guideMarker == null
                ? QuestGuideMarker.disabled()
                : displayObjective.guideMarker.copy();
        progress.status = TaskStatus.ACTIVE;
        return progress;
    }

    public void refreshObjectives(QuestFile questFile, net.minecraft.server.level.ServerPlayer player) {
        if (questFile == null || stepId == null || stepId.isBlank()) {
            return;
        }
        List<ITask> tasks = questFile.findTasksForStep(stepId);
        if (tasks.isEmpty()) {
            return;
        }
        net.minecraft.server.level.ServerPlayer refreshPlayer = status == TaskStatus.ACTIVE ? player : null;
        TaskProgress refreshed = fromTasks(stepId, tasks, questFile.findStep(stepId).orElse(null), refreshPlayer);
        taskHint = refreshed.taskHint;
        manualSubmitRequired = refreshed.manualSubmitRequired;
        displayIcon = refreshed.displayIcon;
        guideMarker = refreshed.guideMarker;
        for (int i = 0; i < refreshed.objectives.size(); i++) {
            TaskObjectiveProgress refreshedObjective = refreshed.objectives.get(i);
            if (i >= objectives.size()) {
                objectives.add(refreshedObjective);
                continue;
            }
            TaskObjectiveProgress current = objectives.get(i);
            boolean sameObjective = Objects.equals(current.objectiveId, refreshedObjective.objectiveId);
            if (!sameObjective) {
                current.currentAmount = 0;
                current.completed = false;
                current.startedGameTime = -1L;
            }
            current.objectiveId = refreshedObjective.objectiveId;
            current.hint = refreshedObjective.displayHint();
            current.displayIcon = refreshedObjective.displayIcon;
            current.objectiveType = refreshedObjective.objectiveType;
            current.requiredAmount = refreshedObjective.requiredAmount;
            current.manualSubmitRequired = refreshedObjective.manualSubmitRequired;
            current.guideMarker = refreshedObjective.guideMarker;
            current.progressTextOverride = refreshedObjective.progressTextOverride == null
                    ? ""
                    : refreshedObjective.progressTextOverride;
            boolean refreshFromPlayerState = i < tasks.size() && tasks.get(i).refreshesProgressFromPlayerState();
            if (!current.completed && !current.manualSubmitRequired && refreshFromPlayerState && refreshPlayer != null) {
                current.progressTextOverride = "";
                tasks.get(i).refreshObjectiveProgress(refreshPlayer, current);
            } else if (!refreshFromPlayerState) {
                current.currentAmount = Math.min(current.currentAmount, current.requiredAmount);
                current.completed = current.completed || current.currentAmount >= current.requiredAmount;
            } else if (current.manualSubmitRequired) {
                current.currentAmount = Math.min(current.currentAmount, current.requiredAmount);
                current.completed = current.completed || current.currentAmount >= current.requiredAmount;
            }
        }
        while (objectives.size() > refreshed.objectives.size()) {
            objectives.removeLast();
        }
    }

    public boolean areAllObjectivesCompleted() {
        if (objectives.isEmpty() || hasTriggeredFailureObjective()) {
            return false;
        }
        boolean hasRequired = objectives.stream()
                .anyMatch(objective -> objective != null && objective.isRequired());
        if (hasRequired) {
            return objectives.stream()
                    .filter(objective -> objective != null && objective.isRequired())
                    .allMatch(objective -> objective.completed);
        }
        boolean hasOptional = objectives.stream()
                .anyMatch(objective -> objective != null && objective.isOptional());
        if (!hasOptional) {
            return false;
        }
        return objectives.stream()
                .filter(objective -> objective != null && objective.isOptional())
                .allMatch(objective -> objective.completed);
    }

    public boolean hasTriggeredFailureObjective() {
        return objectives.stream()
                .anyMatch(objective -> objective != null && objective.isFailureCondition() && objective.completed);
    }

    public Component displayTaskHint() {
        return taskHint == null ? Component.empty() : taskHint;
    }

    private static Component joinObjectiveHints(List<TaskObjectiveProgress> objectives) {
        Component result = Component.empty();
        boolean appended = false;
        for (TaskObjectiveProgress objective : objectives) {
            if (objective == null || objective.displayHint().getString().isBlank()) {
                continue;
            }
            if (appended) {
                result = result.copy().append(Component.literal("\n"));
            }
            result = result.copy().append(objective.displayHint());
            appended = true;
        }
        return result;
    }

    private static TaskObjectiveProgress selectDisplayObjective(List<TaskObjectiveProgress> objectives) {
        return objectives.stream()
                .filter(objective -> objective != null && !objective.isFailureCondition())
                .filter(objective -> objective.guideMarker != null && objective.guideMarker.isEnabled())
                .findFirst()
                .orElseGet(() -> objectives.stream()
                        .filter(objective -> objective != null && objective.guideMarker != null && objective.guideMarker.isEnabled())
                        .findFirst()
                        .orElseGet(() -> objectives.stream()
                                .filter(objective -> objective != null && !objective.isFailureCondition())
                                .findFirst()
                                .orElse(objectives.getFirst())));
    }
}
