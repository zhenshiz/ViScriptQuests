package com.viscriptquests.quest.data;

import com.lowdragmc.lowdraglib2.syncdata.IPersistedSerializable;
import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.ReadOnlyScoreInfo;
import net.minecraft.world.scores.ScoreHolder;
import net.minecraft.world.scores.Scoreboard;

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
    @Persisted
    public String objectiveName = "";
    @Persisted
    public String scoreHolder = "";

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

    public static QuestValueToken scoreboard(String objectiveName, String scoreHolder) {
        QuestValueToken token = new QuestValueToken();
        token.kind = Kind.SCOREBOARD;
        token.objectiveName = objectiveName == null ? "" : objectiveName.trim();
        token.scoreHolder = scoreHolder == null ? "" : scoreHolder.trim();
        return token;
    }

    public static QuestValueToken operator(Kind kind) {
        QuestValueToken token = new QuestValueToken();
        token.kind = kind;
        return token;
    }

    // 按运行时变量表求值蓝图导出的逆波兰数值表达式。
    public static float evaluate(List<QuestValueToken> expression, Map<String, QuestVariableValue> questVariables) {
        return evaluate(expression, questVariables, null);
    }

    // 按运行时变量表和当前玩家求值蓝图导出的逆波兰数值表达式。
    public static float evaluate(List<QuestValueToken> expression, Map<String, QuestVariableValue> questVariables, ServerPlayer player) {
        if (expression == null || expression.isEmpty()) {
            return 0f;
        }
        ArrayDeque<Float> stack = new ArrayDeque<>();
        for (QuestValueToken token : expression) {
            switch (token.kind) {
                case CONSTANT -> stack.push(token.value);
                case VARIABLE -> {
                    QuestVariableValue variableValue = questVariables == null ? null : questVariables.get(token.variableName);
                    stack.push(variableValue == null ? 0f : variableValue.asFloat());
                }
                case SCOREBOARD -> stack.push(readScoreboardValue(player, token.objectiveName, token.scoreHolder));
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

    public static int evaluateInt(List<QuestValueToken> expression, Map<String, QuestVariableValue> questVariables,
                                  ServerPlayer player, int fallback, int minValue) {
        int resolved = expression == null || expression.isEmpty() || hasUnavailableRuntimeInput(expression, questVariables, player)
                ? fallback
                : Math.round(evaluate(expression, questVariables, player));
        return Math.max(minValue, resolved);
    }

    private static boolean hasUnavailableRuntimeInput(List<QuestValueToken> expression,
                                                      Map<String, QuestVariableValue> questVariables,
                                                      ServerPlayer player) {
        for (QuestValueToken token : expression) {
            if (token.kind == Kind.SCOREBOARD && player == null) {
                return true;
            }
            if (token.kind == Kind.VARIABLE
                    && (questVariables == null || !questVariables.containsKey(token.variableName))) {
                return true;
            }
        }
        return false;
    }

    public static float readScoreboardValue(ServerPlayer player, String objectiveName, String scoreHolderName) {
        if (player == null || objectiveName == null || objectiveName.isBlank()) {
            return 0f;
        }
        Scoreboard scoreboard = player.getScoreboard();
        Objective objective = scoreboard.getObjective(objectiveName.trim());
        if (objective == null) {
            return 0f;
        }
        String holderName = scoreHolderName == null || scoreHolderName.isBlank()
                ? player.getScoreboardName()
                : scoreHolderName.trim();
        ReadOnlyScoreInfo scoreInfo = scoreboard.getPlayerScoreInfo(ScoreHolder.forNameOnly(holderName), objective);
        return scoreInfo == null ? 0f : scoreInfo.value();
    }

    private static float pop(ArrayDeque<Float> stack) {
        return stack.isEmpty() ? 0f : stack.pop();
    }

    public enum Kind {
        CONSTANT,
        VARIABLE,
        SCOREBOARD,
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
                && Objects.equals(variableName, that.variableName)
                && Objects.equals(objectiveName, that.objectiveName)
                && Objects.equals(scoreHolder, that.scoreHolder);
    }

    @Override
    public int hashCode() {
        return Objects.hash(kind, variableName, value, objectiveName, scoreHolder);
    }
}
