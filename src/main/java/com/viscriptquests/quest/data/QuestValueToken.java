package com.viscriptquests.quest.data;

import com.lowdragmc.lowdraglib2.syncdata.IPersistedSerializable;
import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted;

import java.util.ArrayDeque;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;

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

    // 按运行时变量表求值蓝图导出的逆波兰数值表达式。
    public static float evaluate(List<QuestValueToken> expression, Map<String, QuestVariableValue> questVariables) {
        if (expression == null || expression.isEmpty()) {
            return 0f;
        }
        ArrayDeque<Float> stack = new ArrayDeque<>();
        for (QuestValueToken token : expression) {
            switch (token.kind) {
                case CONSTANT -> stack.push(token.value);
                case VARIABLE -> {
                    QuestVariableValue variableValue = questVariables.get(token.variableName);
                    stack.push(variableValue == null ? 0f : variableValue.asFloat());
                }
                case ADD -> stack.push(pop(stack) + pop(stack));
                case SUBTRACT -> {
                    float b = pop(stack);
                    float a = pop(stack);
                    stack.push(a - b);
                }
                case MULTIPLY -> stack.push(pop(stack) * pop(stack));
                case DIVIDE -> {
                    float b = pop(stack);
                    float a = pop(stack);
                    stack.push(b != 0f ? a / b : 0f);
                }
                case CLAMP -> {
                    float max = pop(stack);
                    float min = pop(stack);
                    float value = pop(stack);
                    stack.push(Math.max(min, Math.min(max, value)));
                }
                case RANDOM -> {
                    float max = pop(stack);
                    float min = pop(stack);
                    if (max == min) {
                        stack.push(min);
                    } else {
                        float lower = Math.min(min, max);
                        float upper = Math.max(min, max);
                        stack.push((float) ThreadLocalRandom.current().nextDouble(lower, upper));
                    }
                }
            }
        }
        return stack.isEmpty() ? 0f : stack.pop();
    }

    private static float pop(ArrayDeque<Float> stack) {
        return stack.isEmpty() ? 0f : stack.pop();
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
