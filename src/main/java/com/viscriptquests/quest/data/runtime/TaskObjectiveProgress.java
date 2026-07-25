package com.viscriptquests.quest.data.runtime;

import com.lowdragmc.lowdraglib2.syncdata.IPersistedSerializable;
import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted;
import com.viscriptquests.quest.data.DisplayIcon;
import com.viscriptquests.quest.data.QuestVariableValue;
import com.viscriptquests.quest.data.TaskObjectiveType;
import com.viscriptquests.quest.data.task.ITask;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.Map;

// HUD 展示单个目标用的数据。一个小任务可以包含多个目标，所以这里不要再聚合成一条文本。
public class TaskObjectiveProgress implements IPersistedSerializable {
    @Persisted
    public String objectiveId = "";
    @Persisted
    public Component hint = Component.empty();
    @Persisted
    public DisplayIcon displayIcon = new DisplayIcon();
    @Persisted
    public int currentAmount = 0;
    @Persisted
    public int requiredAmount = 1;
    @Persisted
    public boolean manualSubmitRequired = false;
    @Persisted
    public boolean completed = false;
    @Persisted
    public QuestGuideMarker guideMarker = new QuestGuideMarker();
    @Persisted
    public TaskObjectiveType objectiveType = TaskObjectiveType.REQUIRED;
    // 目标激活后的起始游戏时间。倒计时类目标用它避免刷新 HUD 时重置计时。
    @Persisted
    public long startedGameTime = -1L;
    // 进度文本覆盖值，用于倒计时这类不适合显示“当前/需求”的目标。
    @Persisted
    public String progressTextOverride = "";
    // Ponder 查看按钮数据。ponderViewAction 控制是否显示按钮，组件 ID 由按钮点击时校验。
    @Persisted
    public String ponderComponentId = "";
    @Persisted
    public boolean ponderViewAction = false;

    public static TaskObjectiveProgress fromTask(ITask task, ServerPlayer player) {
        return fromTask(task, player, null);
    }

    public static TaskObjectiveProgress fromTask(ITask task, ServerPlayer player,
                                                 Map<String, QuestVariableValue> questVariables) {
        TaskObjectiveProgress progress = new TaskObjectiveProgress();
        if (task == null) {
            return progress;
        }
        progress.objectiveId = task.objectiveId == null ? "" : task.objectiveId;
        progress.objectiveType = task.objectiveType == null ? TaskObjectiveType.REQUIRED : task.objectiveType;
        Component hint = task.getTaskHint(player, questVariables);
        progress.hint = hint == null ? Component.empty() : hint.copy();
        DisplayIcon icon = task.getDisplayIcon();
        progress.displayIcon = icon == null ? new DisplayIcon() : icon.copy();
        progress.manualSubmitRequired = !progress.isFailureCondition() && !task.allowsAutoSubmit();
        progress.requiredAmount = Math.max(1, task.getRequiredAmount(questVariables, player));
        String ponderComponentId = task.getPonderComponentId();
        progress.ponderComponentId = ponderComponentId == null ? "" : ponderComponentId.trim();
        progress.ponderViewAction = task.hasPonderViewAction();
        task.refreshObjectiveProgress(player, progress, questVariables);
        QuestGuideMarker marker = task.getGuideMarker(player);
        progress.guideMarker = marker == null ? QuestGuideMarker.disabled() : marker.copy();
        return progress;
    }

    public String progressText() {
        if (progressTextOverride != null && !progressTextOverride.isBlank()) {
            return progressTextOverride;
        }
        return "(" + Math.max(0, currentAmount) + "/" + Math.max(1, requiredAmount) + ")";
    }

    public Component objectiveTypeLabel() {
        return Component.translatable((objectiveType == null ? TaskObjectiveType.REQUIRED : objectiveType).getName());
    }

    public Component progressHintWithType() {
        if (isRequired()) {
            return progressHint();
        }
        return Component.literal(progressText())
                .append(Component.literal("["))
                .append(objectiveTypeLabel())
                .append(Component.literal("] "))
                .append(displayHint());
    }

    public boolean isRequired() {
        return objectiveType == null || objectiveType.isRequired();
    }

    public boolean isOptional() {
        return objectiveType != null && objectiveType.isOptional();
    }

    public boolean isFailureCondition() {
        return objectiveType != null && objectiveType.isFailureCondition();
    }

    public Component displayHint() {
        return hint == null ? Component.empty() : hint;
    }

    public Component progressHint() {
        return Component.literal(progressText()).append(displayHint());
    }

    public int displayTextColor() {
        return (objectiveType == null ? TaskObjectiveType.REQUIRED : objectiveType).getDisplayTextColor();
    }

    public boolean hasPonderView() {
        return ponderViewAction;
    }
}
