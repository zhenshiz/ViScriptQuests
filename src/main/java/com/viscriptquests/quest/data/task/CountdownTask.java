package com.viscriptquests.quest.data.task;

import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegister;
import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted;
import com.viscriptquests.quest.data.DisplayIcon;
import com.viscriptquests.quest.data.TaskObjectiveType;
import com.viscriptquests.quest.data.runtime.TaskObjectiveProgress;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Items;

// 倒计时目标会在小任务激活后开始计时，默认作为失败条件使用。
@LDLRegister(name = "countdown_task", registry = ITask.ID)
public class CountdownTask extends ITask {
    private static final int TICKS_PER_SECOND = 20;

    @Persisted
    public int durationSeconds = 60;

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
        return Math.max(1, durationSeconds);
    }

    @Override
    public void refreshObjectiveProgress(ServerPlayer player, TaskObjectiveProgress progress) {
        int duration = getRequiredAmount();
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
        if (progress.completed) {
            return false;
        }
        refreshObjectiveProgress(player, progress);
        progress.completed = progress.currentAmount <= 0;
        return true;
    }

    @Override
    protected Component getDefaultTaskHint() {
        return Component.translatable("viscript_quests.task_hint.countdown_task", getRequiredAmount());
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
