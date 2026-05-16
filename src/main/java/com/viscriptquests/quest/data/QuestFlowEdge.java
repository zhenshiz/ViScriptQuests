package com.viscriptquests.quest.data;

import com.lowdragmc.lowdraglib2.syncdata.IPersistedSerializable;
import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted;
import com.viscriptquests.gui.blueprint.QuestBlueprintCompiler;
import net.minecraft.core.HolderLookup;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

// 蓝图编译后的运行时流程边。条件、变量修改和调试输出都挂在边上。
public class QuestFlowEdge implements IPersistedSerializable {
    @Persisted
    public String fromNodeId = "";
    @Persisted
    public String toNodeId = "";
    @Persisted
    public String conditionVariable = "";
    @Persisted
    public QuestBlueprintCompiler.CompareOp compareOp = QuestBlueprintCompiler.CompareOp.EQ;
    @Persisted
    public float compareValue = 0f;
    @Persisted
    public final List<VariableMutation> variableMutations = new ArrayList<>();
    @Persisted
    public final List<QuestDebugPrint> debugPrints = new ArrayList<>();

    public boolean evaluate(Map<String, QuestVariableValue> questVariables, HolderLookup.Provider provider) {
        if (conditionVariable == null || conditionVariable.isEmpty()) {
            return true;
        }
        QuestVariableValue variableValue = questVariables.get(conditionVariable);
        float actual = variableValue == null ? 0f : variableValue.asFloat();
        return compareOp.test(actual, compareValue);
    }

    public void applyMutations(Map<String, QuestVariableValue> questVariables, HolderLookup.Provider provider) {
        for (VariableMutation mutation : variableMutations) {
            mutation.applyTo(questVariables, provider);
        }
    }
}
