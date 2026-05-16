package com.viscriptquests.quest.data;

import com.lowdragmc.lowdraglib2.syncdata.IPersistedSerializable;
import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted;
import net.minecraft.core.HolderLookup;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;

// DebugPrintVariableNode 编译出的运行时表达式
public class DebugValuePrint implements IPersistedSerializable {
    @Persisted
    public String placeholder = "";
    @Persisted
    public final List<QuestValueToken> expression = new ArrayList<>();

    public float evaluate(Map<String, QuestVariableValue> questVariables, HolderLookup.Provider provider) {
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
