package com.viscriptquests.quest.data;

import com.lowdragmc.lowdraglib2.syncdata.IPersistedSerializable;
import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted;

//大任务的信息
public class QuestDefinition implements IPersistedSerializable {
    @Persisted
    public String questId = "";
    @Persisted
    public String title = "";
    @Persisted
    public String subtitle = "";
    @Persisted
    public DisplayIcon icon = new DisplayIcon();
}
