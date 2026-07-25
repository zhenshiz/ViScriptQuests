package com.viscriptquests.quest.data.task;

import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegister;
import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted;
import com.viscriptquests.quest.data.DisplayIcon;
import com.viscriptquests.quest.data.QuestValueToken;
import com.viscriptquests.quest.data.QuestVariableValue;
import com.viscriptquests.quest.data.TaskObjectiveType;
import com.viscriptquests.quest.data.runtime.TaskObjectiveProgress;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

// 倒计时目标会在小任务激活后开始计时，默认作为失败条件使用。
@LDLRegister(name = "countdown_task", registry = ITask.ID)
public class CountdownTask extends ITask {
    private static final int TICKS_PER_SECOND = 20;

    @Persisted
    public int durationSeconds = 60;
    @Persisted
    public final List<QuestValueToken> durationExpression = new ArrayList<>();

    public CountdownTask() {
        objectiveType = TaskObjectiveType.FAILURE;
    }

    @Override
    public boolean checkCompletion(ServerPlayer player) {
        return false;
    }

    @Override
    public boolean onComplete(ServerPlayer player) {
        return true;
    }

    @Override
    public int getRequiredAmount() {
        return getRequiredAmount(null, null);
    }

    @Override
    public int getRequiredAmount(Map<String, QuestVariableValue> questVariables, ServerPlayer player) {
        return QuestValueToken.evaluateInt(durationExpression, questVariables, player, durationSeconds, 1);
    }

    @Override
    public void refreshObjectiveProgress(ServerPlayer player, TaskObjectiveProgress progress) {
        refreshObjectiveProgress(player, progress, null);
    }

    @Override
    public void refreshObjectiveProgress(ServerPlayer player, TaskObjectiveProgress progress,
                                         Map<String, QuestVariableValue> questVariables) {
        int duration = getRequiredAmount(questVariables, player);
        progress.requiredAmount = duration;
        if (player == null) {
            progress.currentAmount = progress.completed ? 0 : duration;
            progress.progressTextOverride = "(" + formatTime(progress.currentAmount) + ")";
            return;
        }
        if (progress.completed) {
            progress.currentAmount = 0;
            progress.progressTextOverride = "(" + formatTime(0) + ")";
            return;
        }
        if (progress.startedGameTime < 0L) {
            progress.startedGameTime = player.level().getGameTime();
        }
        long elapsedTicks = Math.max(0L, player.level().getGameTime() - progress.startedGameTime);
        int remainingSeconds = (int) Math.max(0L,
                (duration * (long) TICKS_PER_SECOND - elapsedTicks + TICKS_PER_SECOND - 1L) / TICKS_PER_SECOND);
        progress.currentAmount = remainingSeconds;
        progress.progressTextOverride = "(" + formatTime(remainingSeconds) + ")";
    }

    @Override
    public boolean autoCompleteObjective(ServerPlayer player, TaskObjectiveProgress progress) {
        return autoCompleteObjective(player, progress, null);
    }

    @Override
    public boolean autoCompleteObjective(ServerPlayer player, TaskObjectiveProgress progress,
                                         Map<String, QuestVariableValue> questVariables) {
        if (progress.completed) {
            return false;
        }
        refreshObjectiveProgress(player, progress, questVariables);
        progress.completed = progress.currentAmount <= 0;
        return true;
    }

    @Override
    protected Component getDefaultTaskHint() {
        return getDefaultTaskHint(null, null);
    }

    @Override
    protected Component getDefaultTaskHint(ServerPlayer player, Map<String, QuestVariableValue> questVariables) {
        return Component.translatable("viscript_quests.task_hint.countdown_task",
                getRequiredAmount(questVariables, player));
    }

    @Override
    public DisplayIcon getDisplayIcon() {
        return DisplayIcon.item(Items.CLOCK.getDefaultInstance());
    }

    private static String formatTime(int totalSeconds) {
        int safeSeconds = Math.max(0, totalSeconds);
        int minutes = safeSeconds / 60;
        int seconds = safeSeconds % 60;
        return String.format("%02d:%02d", minutes, seconds);
    }
}
