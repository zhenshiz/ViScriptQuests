package com.viscriptquests.quest.data;

import com.lowdragmc.lowdraglib2.syncdata.IPersistedSerializable;
import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted;

import java.util.Objects;

// 运行时数值表达式的逆波兰 token，用于保存蓝图中的简单数学连线
public class QuestValueToken implements IPersistedSerializable {
    @Persisted
    public Kind kind = Kind.CONSTANT;
    @Persisted
    public String variableName = "";
    @Persisted
    public float value = 0f;

    public static QuestValueToken constant(float value) {
        QuestValueToken token = new QuestValueToken();
        token.kind = Kind.CONSTANT;
        token.value = value;
        return token;
    }

    public static QuestValueToken variable(String variableName) {
        QuestValueToken token = new QuestValueToken();
        token.kind = Kind.VARIABLE;
        token.variableName = variableName;
        return token;
    }

    public static QuestValueToken operator(Kind kind) {
        QuestValueToken token = new QuestValueToken();
        token.kind = kind;
        return token;
    }

    public enum Kind {
        CONSTANT,
        VARIABLE,
        ADD,
        SUBTRACT,
        MULTIPLY,
        DIVIDE,
        CLAMP,
        RANDOM
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) return true;
        if (!(object instanceof QuestValueToken that)) return false;
        return Float.compare(value, that.value) == 0
                && kind == that.kind
                && Objects.equals(variableName, that.variableName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(kind, variableName, value);
    }
}
