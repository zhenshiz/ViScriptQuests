package com.viscriptquests.quest.data.runtime;

import com.lowdragmc.lowdraglib2.syncdata.IPersistedSerializable;
import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted;
import com.viscriptquests.quest.data.DisplayIcon;

/** 携带客户端任务完成提示所需的标题、图标和任务层级。 */
public class QuestCompletionToastData implements IPersistedSerializable {
    @Persisted
    public boolean questCompletion;
    @Persisted
    public String title = "";
    @Persisted
    public DisplayIcon icon = new DisplayIcon();

    /**
     * 创建已完成小任务的提示数据。
     *
     * @param progress 已完成小任务的运行时进度，提供标题和图标
     * @return 用于客户端小任务完成提示的数据
     */
    public static QuestCompletionToastData task(TaskProgress progress) {
        QuestCompletionToastData data = new QuestCompletionToastData();
        data.questCompletion = false;
        data.title = progress.title == null || progress.title.isBlank() ? progress.stepId : progress.title;
        data.icon = progress.displayIcon == null ? new DisplayIcon() : progress.displayIcon.copy();
        return data;
    }

    /**
     * 创建已完成大任务的提示数据。
     *
     * @param state 已完成大任务的运行时状态，提供标题和图标
     * @return 用于客户端大任务完成提示的数据
     */
    public static QuestCompletionToastData quest(PlayerQuestState state) {
        QuestCompletionToastData data = new QuestCompletionToastData();
        data.questCompletion = true;
        data.title = state.title == null || state.title.isBlank() ? state.questId : state.title;
        data.icon = state.icon == null ? new DisplayIcon() : state.icon.copy();
        return data;
    }
}
