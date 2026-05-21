package com.viscriptquests.quest.data;

import com.lowdragmc.lowdraglib2.syncdata.IPersistedSerializable;
import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted;
import net.minecraft.core.HolderLookup;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

// DebugPrintVariableNode 编译出的运行时表达式
public class DebugValuePrint implements IPersistedSerializable {
    @Persisted
    public String placeholder = "";
    @Persisted
    public final List<QuestValueToken> expression = new ArrayList<>();

    public float evaluate(Map<String, QuestVariableValue> questVariables, HolderLookup.Provider provider) {
        return QuestValueToken.evaluate(expression, questVariables);
    }

    public float evaluate(Map<String, QuestVariableValue> questVariables, HolderLookup.Provider provider, ServerPlayer player) {
        return QuestValueToken.evaluate(expression, questVariables, player);
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) return true;
        if (!(object instanceof DebugValuePrint that)) return false;
        return Objects.equals(placeholder, that.placeholder)
                && Objects.equals(expression, that.expression);
    }

    @Override
    public int hashCode() {
        return Objects.hash(placeholder, expression);
    }
}
