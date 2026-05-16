package com.viscriptquests.quest.data;

import com.lowdragmc.lowdraglib2.syncdata.IPersistedSerializable;
import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted;
import com.viscriptquests.ViScriptQuests;
import com.viscriptquests.quest.data.runtime.QuestCategoryData;
import com.viscriptquests.quest.data.runtime.QuestCategoryListData;
import com.viscriptquests.quest.data.runtime.QuestPlayerData;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class QuestSavedData extends SavedData implements IPersistedSerializable {
    private static final String DATA_NAME = ViScriptQuests.MOD_ID + "_quests";
    private static final Factory<QuestSavedData> FACTORY = new Factory<>(QuestSavedData::new, QuestSavedData::load);

    @Persisted
    private final Map<UUID, QuestPlayerData> players = new LinkedHashMap<>();
    // 世界默认分类模板。空列表是合法配置，表示新玩家首次打开任务书时没有任何自定义分类。
    @Persisted
    private final List<QuestCategoryData> defaultCategories = new ArrayList<>();

    public static QuestSavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(FACTORY, DATA_NAME);
    }

    public QuestPlayerData getPlayer(UUID playerId) {
        QuestPlayerData playerData = players.computeIfAbsent(playerId, id -> {
            QuestPlayerData data = new QuestPlayerData();
            data.ownerId = id;
            setDirty();
            return data;
        });
        if (playerData.ensureInitialCategories(getDefaultCategories())) {
            setDirty();
        }
        return playerData;
    }

    public List<QuestCategoryData> getDefaultCategories() {
        return defaultCategories;
    }

    public List<QuestCategoryData> copyDefaultCategories() {
        return defaultCategories.stream()
                .map(QuestCategoryData::copy)
                .toList();
    }

    public void putDefaultCategory(QuestCategoryData category) {
        if (category == null || category.id.isBlank()) {
            return;
        }
        defaultCategories.removeIf(existing -> existing.id.equals(category.id));
        defaultCategories.add(category.copy());
        setDirty();
    }

    public void replaceDefaultCategories(List<QuestCategoryData> categories) {
        defaultCategories.clear();
        defaultCategories.addAll(QuestCategoryListData.sanitize(categories));
        setDirty();
    }

    public boolean removeDefaultCategory(String categoryId) {
        boolean removed = defaultCategories.removeIf(category -> category.hasId(categoryId));
        if (removed) {
            setDirty();
        }
        return removed;
    }

    public int resetPlayerCategories(UUID playerId) {
        QuestPlayerData playerData = getPlayer(playerId);
        playerData.resetCategories(copyDefaultCategories());
        setDirty();
        return playerData.categories.size();
    }

    public boolean resetPlayerData(UUID playerId) {
        boolean removed = players.remove(playerId) != null;
        if (removed) {
            setDirty();
        }
        return removed;
    }

    public static QuestSavedData load(CompoundTag tag, HolderLookup.Provider provider) {
        QuestSavedData data = new QuestSavedData();
        data.deserializeNBT(provider, tag);
        return data;
    }

    @Override
    public @NotNull CompoundTag save(CompoundTag tag, HolderLookup.@NotNull Provider provider) {
        tag.merge(serializeNBT(provider));
        return tag;
    }
}
