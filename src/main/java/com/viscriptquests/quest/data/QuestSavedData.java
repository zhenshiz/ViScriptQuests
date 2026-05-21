package com.viscriptquests.quest.data;

import com.lowdragmc.lowdraglib2.syncdata.IPersistedSerializable;
import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted;
import com.viscriptquests.ViScriptQuests;
import com.viscriptquests.quest.data.runtime.QuestPlayerData;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public class QuestSavedData extends SavedData implements IPersistedSerializable {
    private static final String DATA_NAME = ViScriptQuests.MOD_ID + "_quests";
    private static final Factory<QuestSavedData> FACTORY = new Factory<>(QuestSavedData::new, QuestSavedData::load);

    @Persisted
    private final Map<UUID, QuestPlayerData> players = new LinkedHashMap<>();

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
        return playerData;
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
