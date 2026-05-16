package com.viscriptquests.quest.data;

import com.lowdragmc.lowdraglib2.syncdata.IPersistedSerializable;
import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted;
import net.minecraft.core.HolderLookup;

import java.util.Map;

// 步骤转移时执行的变量修改指令，由 SetVariableNode 编译生成
// 运行时在转移条件满足后、激活下一步骤前执行
public class VariableMutation implements IPersistedSerializable {
    @Persisted
    public String variableName = "";
    @Persisted
    public VariableMutationOp operation = VariableMutationOp.SET;
    @Persisted
    public float value = 0f;

    // 将此修改应用到变量表
    public void applyTo(Map<String, QuestVariableValue> questVariables, HolderLookup.Provider provider) {
        if (variableName == null || variableName.isEmpty()) return;
        QuestVariableValue variableValue = questVariables.get(variableName);
        if (variableValue != null && !variableValue.supportsNumericMutation()) {
            return;
        }
        float current = variableValue == null ? 0f : variableValue.asFloat();
        float next = operation.apply(current, value);
        questVariables.put(variableName, variableValue == null
                ? QuestVariableValue.ofFloat(next)
                : variableValue.withNumericValue(next));
    }
}
