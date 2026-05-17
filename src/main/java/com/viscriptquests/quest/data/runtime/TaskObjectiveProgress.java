package com.viscriptquests.quest.data.runtime;

import com.lowdragmc.lowdraglib2.syncdata.IPersistedSerializable;
import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted;
import com.viscriptquests.quest.data.DisplayIcon;
import com.viscriptquests.quest.data.task.ITask;
import net.minecraft.server.level.ServerPlayer;

// HUD 展示单个目标用的数据。一个小任务可以包含多个目标，所以这里不要再聚合成一条文本。
public class TaskObjectiveProgress implements IPersistedSerializable {
    @Persisted
    public String hint = "";
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

    public static TaskObjectiveProgress fromTask(ITask task, ServerPlayer player) {
        TaskObjectiveProgress progress = new TaskObjectiveProgress();
        if (task == null) {
            return progress;
        }
        progress.hint = task.getTaskHint().getString();
        DisplayIcon icon = task.getDisplayIcon();
        progress.displayIcon = icon == null ? new DisplayIcon() : icon.copy();
        progress.manualSubmitRequired = !task.allowsAutoSubmit();
        progress.requiredAmount = Math.max(1, task.getRequiredAmount());
        task.refreshObjectiveProgress(player, progress);
        QuestGuideMarker marker = task.getGuideMarker(player);
        progress.guideMarker = marker == null ? QuestGuideMarker.disabled() : marker.copy();
        return progress;
    }

    public String progressText() {
        return "[" + Math.max(0, currentAmount) + "/" + Math.max(1, requiredAmount) + "] ";
    }
}
