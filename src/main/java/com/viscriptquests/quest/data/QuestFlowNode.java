package com.viscriptquests.quest.data;

import com.lowdragmc.lowdraglib2.syncdata.IPersistedSerializable;
import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted;

// 蓝图编译后的运行时流程节点
public class QuestFlowNode implements IPersistedSerializable {
    @Persisted
    public String nodeId = "";
    @Persisted
    public String type = "";
    @Persisted
    public String stepId = "";
    @Persisted
    public QuestJoinMode joinMode = QuestJoinMode.ANY;
    @Persisted
    public int requiredCount = 1;
    @Persisted
    public boolean success = true;
}
