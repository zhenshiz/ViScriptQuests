package com.viscriptquests.event.neoforge;

import com.viscriptquests.compat.team.QuestTeamService;
import com.viscriptquests.quest.data.reward.IReward;
import com.viscriptquests.quest.data.runtime.PlayerQuestState;
import com.viscriptquests.quest.data.runtime.TaskObjectiveProgress;
import com.viscriptquests.quest.data.runtime.TaskProgress;
import com.viscriptquests.util.QuestFileHelper;
import lombok.Getter;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.Event;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;

/**
 * 任务运行时发布的服务端 NeoForge 事件。
 *
 * <p>事件目标采用稳定的层级标识：大任务为 {@code questId}，小任务为
 * {@code questId/stepId}，目标为 {@code questId/stepId/objectiveId}。
 */
@Getter
public abstract class QuestEvent extends Event {
    private final ServerPlayer player;
    @Nullable
    private final PlayerQuestState quest;

    protected QuestEvent(ServerPlayer player, @Nullable PlayerQuestState quest) {
        this.player = player;
        this.quest = quest;
    }

    public String getQuestId() {
        return quest == null || quest.questId == null ? "" : quest.questId;
    }

    public String getTarget() {
        return getQuestId();
    }

    public boolean matchesQuest(String questId) {
        return questId != null && !getQuestId().isBlank()
                && getQuestId().equals(QuestFileHelper.normalizeQuestId(questId));
    }

    public List<ServerPlayer> getOnlineMembers() {
        return List.copyOf(QuestTeamService.onlineMembers(player.getServer(), QuestTeamService.scopeOf(player)));
    }

    public static final class QuestStarted extends QuestEvent {
        public QuestStarted(ServerPlayer player, PlayerQuestState quest) {
            super(player, quest);
        }
    }

    @Getter
    public static final class QuestCompleted extends QuestEvent {
        private final boolean forced;

        public QuestCompleted(ServerPlayer player, PlayerQuestState quest, boolean forced) {
            super(player, quest);
            this.forced = forced;
        }
    }

    @Getter
    public static final class QuestFailed extends QuestEvent {
        private final String failedStepId;

        public QuestFailed(ServerPlayer player, PlayerQuestState quest, String failedStepId) {
            super(player, quest);
            this.failedStepId = failedStepId == null ? "" : failedStepId;
        }
    }

    public static final class QuestRevoked extends QuestEvent {
        public QuestRevoked(ServerPlayer player, PlayerQuestState quest) {
            super(player, quest);
        }
    }

    @Getter
    public abstract static class TaskEvent extends QuestEvent {
        private final TaskProgress task;

        protected TaskEvent(ServerPlayer player, PlayerQuestState quest, TaskProgress task) {
            super(player, quest);
            this.task = task;
        }

        public String getStepId() {
            return task.stepId == null ? "" : task.stepId;
        }

        @Override
        public String getTarget() {
            return taskTarget(getQuestId(), getStepId());
        }

        public boolean matchesTask(String questId, String stepId) {
            return matchesQuest(questId) && Objects.equals(getStepId(), stepId);
        }
    }

    @Getter
    public static final class TaskStarted extends TaskEvent {
        private final boolean reentered;

        public TaskStarted(ServerPlayer player, PlayerQuestState quest, TaskProgress task, boolean reentered) {
            super(player, quest, task);
            this.reentered = reentered;
        }
    }

    @Getter
    public static final class TaskCompleted extends TaskEvent {
        private final boolean forced;

        public TaskCompleted(ServerPlayer player, PlayerQuestState quest, TaskProgress task, boolean forced) {
            super(player, quest, task);
            this.forced = forced;
        }
    }

    @Getter
    public static final class TaskFailed extends TaskEvent {
        @Nullable
        private final TaskObjectiveProgress failedObjective;

        public TaskFailed(ServerPlayer player, PlayerQuestState quest, TaskProgress task,
                          @Nullable TaskObjectiveProgress failedObjective) {
            super(player, quest, task);
            this.failedObjective = failedObjective;
        }
    }

    @Getter
    public static final class TaskSkipped extends TaskEvent {
        private final String reason;

        public TaskSkipped(ServerPlayer player, PlayerQuestState quest, TaskProgress task, String reason) {
            super(player, quest, task);
            this.reason = reason == null ? "" : reason;
        }
    }

    @Getter
    public abstract static class ObjectiveEvent extends TaskEvent {
        private final TaskObjectiveProgress objective;
        private final int objectiveIndex;
        private final int previousAmount;
        private final int previousRequiredAmount;
        private final boolean previouslyCompleted;
        private final boolean automatic;

        protected ObjectiveEvent(ServerPlayer player, PlayerQuestState quest, TaskProgress task,
                                 TaskObjectiveProgress objective, int objectiveIndex,
                                 int previousAmount, int previousRequiredAmount,
                                 boolean previouslyCompleted, boolean automatic) {
            super(player, quest, task);
            this.objective = objective;
            this.objectiveIndex = objectiveIndex;
            this.previousAmount = previousAmount;
            this.previousRequiredAmount = previousRequiredAmount;
            this.previouslyCompleted = previouslyCompleted;
            this.automatic = automatic;
        }

        public String getObjectiveId() {
            return objective.objectiveId == null ? "" : objective.objectiveId;
        }

        @Override
        public String getTarget() {
            return objectiveTarget(getQuestId(), getStepId(), getObjectiveId(), objectiveIndex);
        }

        public int getCurrentAmount() {
            return objective.currentAmount;
        }

        public int getAmountDelta() {
            return objective.currentAmount - previousAmount;
        }

        public int getRequiredAmount() {
            return objective.requiredAmount;
        }

        public boolean isCompleted() {
            return objective.completed;
        }

        public boolean matchesObjective(String questId, String stepId, String objectiveId) {
            return matchesTask(questId, stepId) && Objects.equals(getObjectiveId(), objectiveId);
        }

        public boolean matchesObjectiveIndex(String questId, String stepId, int objectiveIndex) {
            return matchesTask(questId, stepId) && this.objectiveIndex == objectiveIndex;
        }
    }

    public static final class ObjectiveProgress extends ObjectiveEvent {
        public ObjectiveProgress(ServerPlayer player, PlayerQuestState quest, TaskProgress task,
                                 TaskObjectiveProgress objective, int objectiveIndex,
                                 int previousAmount, int previousRequiredAmount,
                                 boolean previouslyCompleted, boolean automatic) {
            super(player, quest, task, objective, objectiveIndex, previousAmount,
                    previousRequiredAmount, previouslyCompleted, automatic);
        }
    }

    public static final class ObjectiveCompleted extends ObjectiveEvent {
        public ObjectiveCompleted(ServerPlayer player, PlayerQuestState quest, TaskProgress task,
                                  TaskObjectiveProgress objective, int objectiveIndex,
                                  int previousAmount, int previousRequiredAmount,
                                  boolean previouslyCompleted, boolean automatic) {
            super(player, quest, task, objective, objectiveIndex, previousAmount,
                    previousRequiredAmount, previouslyCompleted, automatic);
        }
    }

    @Getter
    public static final class RewardGranted extends QuestEvent {
        private final ServerPlayer sourcePlayer;
        private final IReward reward;
        private final String stepId;
        private final String rewardSource;

        public RewardGranted(ServerPlayer sourcePlayer, ServerPlayer recipient,
                             @Nullable PlayerQuestState quest, IReward reward,
                             String stepId, String rewardSource) {
            super(recipient, quest);
            this.sourcePlayer = sourcePlayer;
            this.reward = reward;
            this.stepId = stepId == null ? "" : stepId;
            this.rewardSource = rewardSource == null ? "" : rewardSource;
        }

        @Override
        public String getTarget() {
            if (getQuestId().isBlank()) {
                return "";
            }
            return stepId.isBlank() ? getQuestId() : taskTarget(getQuestId(), stepId);
        }

        public boolean matchesTask(String questId, String stepId) {
            return matchesQuest(questId) && Objects.equals(this.stepId, stepId);
        }

        public boolean isTeamDelivery() {
            return !sourcePlayer.getUUID().equals(getPlayer().getUUID());
        }
    }

    public static String taskTarget(String questId, String stepId) {
        return nullToEmpty(questId) + "/" + nullToEmpty(stepId);
    }

    public static String objectiveTarget(String questId, String stepId, String objectiveId, int objectiveIndex) {
        String targetId = objectiveId == null || objectiveId.isBlank() ? "@" + objectiveIndex : objectiveId;
        return taskTarget(questId, stepId) + "/" + targetId;
    }

    public static String objectiveTarget(String questId, String stepId, String objectiveId) {
        return taskTarget(questId, stepId) + "/" + nullToEmpty(objectiveId);
    }

    public static String objectiveIndexTarget(String questId, String stepId, int objectiveIndex) {
        return taskTarget(questId, stepId) + "/@" + objectiveIndex;
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
