package com.viscriptquests.quest.data.runtime;

import com.lowdragmc.lowdraglib2.syncdata.IPersistedSerializable;
import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted;

// 任务书同步数据：玩家进度来自 SavedData，分类目录来自全局分类配置文件。
public class QuestBookData implements IPersistedSerializable {
    @Persisted
    public QuestPlayerData playerData = new QuestPlayerData();
    @Persisted
    public QuestCategoryListData categoryData = new QuestCategoryListData();
}
