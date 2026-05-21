package com.viscriptquests.quest.data.runtime;

import com.lowdragmc.lowdraglib2.syncdata.IPersistedSerializable;
import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted;
import com.viscriptquests.quest.data.reward.IReward;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class QuestPlayerData implements IPersistedSerializable {
    @Persisted
    public UUID ownerId = new UUID(0, 0);
    @Persisted
    public String trackedQuestId = "";
    @Persisted
    public String trackedStepId = "";
    @Persisted
    public final List<PlayerQuestState> quests = new ArrayList<>();
    @Persisted
    public final List<IReward> pendingRewards = new ArrayList<>();

    public Optional<PlayerQuestState> findQuest(String questId) {
        return quests.stream()
                .filter(quest -> quest.questId.equals(questId))
                .findFirst();
    }

    public void putQuest(PlayerQuestState state) {
        quests.removeIf(quest -> quest.questId.equals(state.questId));
        quests.add(state);
    }

    public boolean removeQuest(String questId) {
        return quests.removeIf(quest -> quest.questId.equals(questId));
    }
}
