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
    public final List<QuestCategoryData> categories = new ArrayList<>();
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

    public Optional<QuestCategoryData> findCategory(String categoryId) {
        String normalizedCategoryId = QuestCategoryData.normalizeId(categoryId);
        return categories.stream()
                .filter(category -> category.id.equals(normalizedCategoryId))
                .findFirst();
    }

    public void putCategory(QuestCategoryData category) {
        if (category == null || category.id.isBlank()) {
            return;
        }
        categories.removeIf(existing -> existing.id.equals(category.id));
        categories.add(category.copy());
    }

    public boolean removeCategory(String categoryId) {
        String normalizedCategoryId = QuestCategoryData.normalizeId(categoryId);
        return categories.removeIf(category -> category.id.equals(normalizedCategoryId));
    }

    public List<QuestCategoryData> copyCategories() {
        return categories.stream()
                .map(QuestCategoryData::copy)
                .toList();
    }

    public void resetCategories(List<QuestCategoryData> defaultCategories) {
        categories.clear();
        categories.addAll(QuestCategoryListData.sanitize(defaultCategories));
    }

    public boolean ensureInitialCategories(List<QuestCategoryData> defaultCategories) {
        boolean changed = false;
        for (QuestCategoryData category : defaultCategories) {
            if (category == null || category.id.isBlank()) {
                continue;
            }
            if (findCategory(category.id).isPresent()) {
                continue;
            }
            categories.add(category.copy());
            changed = true;
        }
        return changed;
    }
}
