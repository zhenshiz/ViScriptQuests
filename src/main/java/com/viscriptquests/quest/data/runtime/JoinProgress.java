package com.viscriptquests.quest.data.runtime;

import com.lowdragmc.lowdraglib2.syncdata.IPersistedSerializable;
import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted;

import java.util.LinkedHashSet;
import java.util.Set;

// Join 节点的运行时到达状态。每条进入 Join 的边都算一个来源分支。
public class JoinProgress implements IPersistedSerializable {
    @Persisted
    public String joinNodeId = "";
    @Persisted
    public final Set<String> arrivedFromNodeIds = new LinkedHashSet<>();
    @Persisted
    public boolean resolved = false;
}
