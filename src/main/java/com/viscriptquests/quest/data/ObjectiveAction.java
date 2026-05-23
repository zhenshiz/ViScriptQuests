package com.viscriptquests.quest.data;

import com.lowdragmc.lowdraglib2.syncdata.IPersistedSerializable;
import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted;
import com.viscriptquests.quest.data.reward.IReward;

import java.util.ArrayList;
import java.util.List;

// 单个目标完成后触发的动作边，复用主流程边的条件、变量修改、计分板修改和调试输出语义。
public class ObjectiveAction implements IPersistedSerializable {
    @Persisted
    public String actionId = "";
    @Persisted
    public String stepId = "";
    @Persisted
    public String objectiveId = "";
    @Persisted
    public final List<QuestFlowEdge> gates = new ArrayList<>();
    @Persisted
    public QuestFlowEdge edge = new QuestFlowEdge();
    @Persisted
    public final List<IReward> rewards = new ArrayList<>();

    public boolean isFor(String stepId, String objectiveId) {
        return this.stepId.equals(stepId == null ? "" : stepId)
                && this.objectiveId.equals(objectiveId == null ? "" : objectiveId);
    }
}
