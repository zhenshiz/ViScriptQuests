package com.viscriptquests.quest.data.runtime;

import com.lowdragmc.lowdraglib2.syncdata.IPersistedSerializable;
import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted;
import com.viscriptquests.quest.data.DisplayIcon;
import com.viscriptquests.quest.data.QuestStep;
import com.viscriptquests.quest.data.task.ITask;

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
    public String taskHint = "";
    @Persisted
    public DisplayIcon hudIcon = new DisplayIcon();
    @Persisted
    public TaskStatus status = TaskStatus.ACTIVE;

    public static TaskProgress fromTask(ITask task, QuestStep step) {
        TaskProgress progress = new TaskProgress();
        progress.stepId = task.stepId;
        if (step != null) {
            progress.title = step.title;
            progress.subtitle = step.subtitle;
            progress.description = step.description.clone();
        }
        progress.taskHint = task.getTaskHint().getString();
        progress.hudIcon = task.getHudIcon();
        progress.status = TaskStatus.ACTIVE;
        return progress;
    }
}
