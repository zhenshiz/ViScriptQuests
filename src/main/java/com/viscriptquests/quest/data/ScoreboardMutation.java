package com.viscriptquests.quest.data;

import com.lowdragmc.lowdraglib2.syncdata.IPersistedSerializable;
import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.ScoreAccess;
import net.minecraft.world.scores.ScoreHolder;
import net.minecraft.world.scores.Scoreboard;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

// 步骤转移时执行的计分板修改指令，由 ModifyScoreboardValueNode 编译生成。
public class ScoreboardMutation implements IPersistedSerializable {
    @Persisted
    public String objectiveName = "";
    @Persisted
    public String scoreHolder = "";
    @Persisted
    public VariableMutationOp operation = VariableMutationOp.SET;
    @Persisted
    public float value = 0f;
    @Persisted
    public final List<QuestValueToken> expression = new ArrayList<>();

    public void applyTo(ServerPlayer player, Map<String, QuestVariableValue> questVariables) {
        if (player == null || objectiveName == null || objectiveName.isBlank()) {
            return;
        }
        Scoreboard scoreboard = player.getScoreboard();
        Objective objective = scoreboard.getObjective(objectiveName.trim());
        if (objective == null) {
            return;
        }
        ScoreHolder holder = ScoreHolder.forNameOnly(resolveScoreHolderName(player));
        ScoreAccess scoreAccess = scoreboard.getOrCreatePlayerScore(holder, objective);
        float operand = expression.isEmpty() ? value : QuestValueToken.evaluate(expression, questVariables, player);
        int nextValue = Math.round(operation.apply(scoreAccess.get(), operand));
        try {
            scoreAccess.set(nextValue);
        } catch (IllegalStateException ignored) {
            // 原版只允许修改非只读目标；只读目标保持原值，避免任务流程因此中断。
        }
    }

    private String resolveScoreHolderName(ServerPlayer player) {
        String normalized = scoreHolder == null ? "" : scoreHolder.trim();
        return normalized.isEmpty() ? player.getScoreboardName() : normalized;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) return true;
        if (!(object instanceof ScoreboardMutation that)) return false;
        return Float.compare(value, that.value) == 0
                && Objects.equals(objectiveName, that.objectiveName)
                && Objects.equals(scoreHolder, that.scoreHolder)
                && operation == that.operation
                && Objects.equals(expression, that.expression);
    }

    @Override
    public int hashCode() {
        return Objects.hash(objectiveName, scoreHolder, operation, value, expression);
    }
}
