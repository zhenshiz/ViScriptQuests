package com.viscriptquests.event.kubejs;

import com.viscriptquests.event.neoforge.QuestEvent;
import com.viscriptquests.quest.data.reward.IReward;
import com.viscriptquests.quest.data.runtime.PlayerQuestState;
import com.viscriptquests.quest.data.runtime.TaskObjectiveProgress;
import com.viscriptquests.quest.data.runtime.TaskProgress;
import dev.latvian.mods.kubejs.player.EntityArrayList;
import dev.latvian.mods.kubejs.player.KubePlayerEvent;
import lombok.Getter;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

@Getter
public abstract class QuestEventJS implements KubePlayerEvent {
    private final QuestEvent event;

    protected QuestEventJS(QuestEvent event) {
        this.event = event;
    }

    @Override
    public ServerPlayer getEntity() {
        return event.getPlayer();
    }

    @Nullable
    public PlayerQuestState getQuest() {
        return event.getQuest();
    }

    public String getQuestId() {
        return event.getQuestId();
    }

    public String getTarget() {
        return event.getTarget();
    }

    public boolean matchesQuest(String questId) {
        return event.matchesQuest(questId);
    }

    @Nullable
    public TaskProgress getTask() {
        return event instanceof QuestEvent.TaskEvent taskEvent ? taskEvent.getTask() : null;
    }

    public String getStepId() {
        return event instanceof QuestEvent.TaskEvent taskEvent ? taskEvent.getStepId() : "";
    }

    public boolean matchesTask(String questId, String stepId) {
        return event instanceof QuestEvent.TaskEvent taskEvent && taskEvent.matchesTask(questId, stepId);
    }

    @Nullable
    public TaskObjectiveProgress getObjective() {
        return event instanceof QuestEvent.ObjectiveEvent objectiveEvent ? objectiveEvent.getObjective() : null;
    }

    public String getObjectiveId() {
        return event instanceof QuestEvent.ObjectiveEvent objectiveEvent ? objectiveEvent.getObjectiveId() : "";
    }

    public boolean matchesObjective(String questId, String stepId, String objectiveId) {
        return event instanceof QuestEvent.ObjectiveEvent objectiveEvent
                && objectiveEvent.matchesObjective(questId, stepId, objectiveId);
    }

    public boolean matchesObjectiveIndex(String questId, String stepId, int objectiveIndex) {
        return event instanceof QuestEvent.ObjectiveEvent objectiveEvent
                && objectiveEvent.matchesObjectiveIndex(questId, stepId, objectiveIndex);
    }

    public EntityArrayList getOnlineMembers() {
        return new EntityArrayList(event.getOnlineMembers());
    }

    public static final class QuestStarted extends QuestEventJS {
        public QuestStarted(QuestEvent.QuestStarted event) {
            super(event);
        }
    }

    public static final class QuestCompleted extends QuestEventJS {
        private final QuestEvent.QuestCompleted event;

        public QuestCompleted(QuestEvent.QuestCompleted event) {
            super(event);
            this.event = event;
        }

        public boolean isForced() {
            return event.isForced();
        }
    }

    public static final class QuestFailed extends QuestEventJS {
        private final QuestEvent.QuestFailed event;

        public QuestFailed(QuestEvent.QuestFailed event) {
            super(event);
            this.event = event;
        }

        public String getFailedStepId() {
            return event.getFailedStepId();
        }
    }

    public static final class QuestRevoked extends QuestEventJS {
        public QuestRevoked(QuestEvent.QuestRevoked event) {
            super(event);
        }
    }

    public static final class TaskStarted extends QuestEventJS {
        private final QuestEvent.TaskStarted event;

        public TaskStarted(QuestEvent.TaskStarted event) {
            super(event);
            this.event = event;
        }

        public boolean isReentered() {
            return event.isReentered();
        }
    }

    public static final class TaskCompleted extends QuestEventJS {
        private final QuestEvent.TaskCompleted event;

        public TaskCompleted(QuestEvent.TaskCompleted event) {
            super(event);
            this.event = event;
        }

        public boolean isForced() {
            return event.isForced();
        }
    }

    public static final class TaskFailed extends QuestEventJS {
        private final QuestEvent.TaskFailed event;

        public TaskFailed(QuestEvent.TaskFailed event) {
            super(event);
            this.event = event;
        }

        @Nullable
        public TaskObjectiveProgress getFailedObjective() {
            return event.getFailedObjective();
        }
    }

    public static final class TaskSkipped extends QuestEventJS {
        private final QuestEvent.TaskSkipped event;

        public TaskSkipped(QuestEvent.TaskSkipped event) {
            super(event);
            this.event = event;
        }

        public String getReason() {
            return event.getReason();
        }
    }

    public abstract static class Objective extends QuestEventJS {
        private final QuestEvent.ObjectiveEvent event;

        protected Objective(QuestEvent.ObjectiveEvent event) {
            super(event);
            this.event = event;
        }

        public int getObjectiveIndex() {
            return event.getObjectiveIndex();
        }

        public int getPreviousAmount() {
            return event.getPreviousAmount();
        }

        public int getCurrentAmount() {
            return event.getCurrentAmount();
        }

        public int getAmountDelta() {
            return event.getAmountDelta();
        }

        public int getPreviousRequiredAmount() {
            return event.getPreviousRequiredAmount();
        }

        public int getRequiredAmount() {
            return event.getRequiredAmount();
        }

        public boolean wasCompleted() {
            return event.isPreviouslyCompleted();
        }

        public boolean isCompleted() {
            return event.isCompleted();
        }

        public boolean isAutomatic() {
            return event.isAutomatic();
        }
    }

    public static final class ObjectiveProgress extends Objective {
        public ObjectiveProgress(QuestEvent.ObjectiveProgress event) {
            super(event);
        }
    }

    public static final class ObjectiveCompleted extends Objective {
        public ObjectiveCompleted(QuestEvent.ObjectiveCompleted event) {
            super(event);
        }
    }

    public static final class RewardGranted extends QuestEventJS {
        private final QuestEvent.RewardGranted event;

        public RewardGranted(QuestEvent.RewardGranted event) {
            super(event);
            this.event = event;
        }

        public ServerPlayer getSourcePlayer() {
            return event.getSourcePlayer();
        }

        public IReward getReward() {
            return event.getReward();
        }

        public String getStepId() {
            return event.getStepId();
        }

        public String getRewardSource() {
            return event.getRewardSource();
        }

        public boolean isTeamDelivery() {
            return event.isTeamDelivery();
        }

        @Override
        public boolean matchesTask(String questId, String stepId) {
            return event.matchesTask(questId, stepId);
        }
    }
}
