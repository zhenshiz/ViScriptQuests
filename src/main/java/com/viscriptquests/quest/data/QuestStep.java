package com.viscriptquests.quest.data;

import com.lowdragmc.lowdraglib2.syncdata.IPersistedSerializable;
import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted;

// 小任务的信息，不包括目标和奖励
public class QuestStep implements IPersistedSerializable {
    @Persisted
    public String stepId = "";
    @Persisted
    public String title = "";
    @Persisted
    public String subtitle = "";
    @Persisted
    public String[] description = new String[0];
}
