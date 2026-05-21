package com.viscriptquests.quest.data;

import com.lowdragmc.lowdraglib2.syncdata.IPersistedSerializable;
import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted;
import net.minecraft.core.HolderLookup;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

// 步骤转移时执行的变量修改指令，由 SetVariableNode 编译生成
// 运行时在转移条件满足后、激活下一步骤前执行
public class VariableMutation implements IPersistedSerializable {
    @Persisted
    public String variableName = "";
    @Persisted
    public VariableMutationOp operation = VariableMutationOp.SET;
    @Persisted
    public float value = 0f;
    @Persisted
    public final List<QuestValueToken> expression = new ArrayList<>();

    // 将此修改应用到变量表
    public void applyTo(Map<String, QuestVariableValue> questVariables, HolderLookup.Provider provider) {
        applyTo(questVariables, provider, null);
    }

    // 将此修改应用到变量表，带玩家上下文时表达式可以读取计分板值。
    public void applyTo(Map<String, QuestVariableValue> questVariables, HolderLookup.Provider provider, ServerPlayer player) {
        if (variableName == null || variableName.isEmpty()) return;
        QuestVariableValue variableValue = questVariables.get(variableName);
        if (variableValue != null && !variableValue.supportsNumericMutation()) {
            return;
        }
        float mutationValue = expression.isEmpty() ? value : QuestValueToken.evaluate(expression, questVariables, player);
        float current = variableValue == null ? 0f : variableValue.asFloat();
        float next = operation.apply(current, mutationValue);
        questVariables.put(variableName, variableValue == null
                ? QuestVariableValue.ofFloat(next)
                : variableValue.withNumericValue(next));
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) return true;
        if (!(object instanceof VariableMutation that)) return false;
        return Float.compare(value, that.value) == 0
                && Objects.equals(variableName, that.variableName)
                && operation == that.operation
                && Objects.equals(expression, that.expression);
    }

    @Override
    public int hashCode() {
        return Objects.hash(variableName, operation, value, expression);
    }
}
