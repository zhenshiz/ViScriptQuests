package com.viscriptquests.quest;

import com.lowdragmc.lowdraglib2.Platform;
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class QuestSavedData extends SavedData {
    Map<UUID, List<CategoryQuest>> categoryQuestsMap = new HashMap<>();
    private final ServerLevel world;

    public static SavedData.Factory<QuestSavedData> factory(ServerLevel world) {
        return new SavedData.Factory<>(() -> new QuestSavedData(world), (nbt, r) -> fromNbt(world, nbt), null);
    }

    public QuestSavedData(ServerLevel world) {
        this.world = world;
    }

    public List<CategoryQuest> getCategoryQuests(ServerPlayer player) {
        setDirty();
        return categoryQuestsMap.getOrDefault(player.getUUID(), new ArrayList<>());
    }

    @Nullable
    public CategoryQuest getCategoryQuest(ServerPlayer player, String id) {
        setDirty();
        return getCategoryQuests(player).stream().filter(categoryQuest -> categoryQuest.getId().equals(id)).findFirst().orElse(null);
    }

    public void addCategory(ServerPlayer player, String id, IGuiTexture icon, String name) {
        List<CategoryQuest> quests = categoryQuestsMap.getOrDefault(player.getUUID(), new ArrayList<>());
        quests.add(CategoryQuest.of(id, icon, name));
        categoryQuestsMap.put(player.getUUID(), quests);
        setDirty();
    }

    public void addQuest(ServerPlayer player, String categoryId, QuestInfo questInfo) {
        List<CategoryQuest> categoryQuests = categoryQuestsMap.getOrDefault(player.getUUID(), new ArrayList<>());
        for (CategoryQuest categoryQuest : categoryQuests) {
            if (categoryQuest.getId().equals(categoryId)) {
                categoryQuest.getQuests().add(questInfo);
                break;
            }
        }
        categoryQuestsMap.put(player.getUUID(), categoryQuests);
        setDirty();
    }

    public static QuestSavedData fromNbt(ServerLevel world, CompoundTag nbt) {
        QuestSavedData questSavedData = new QuestSavedData(world);
        for (String player : nbt.getAllKeys()) {
            UUID uuid = UUID.fromString(player);
            List<CategoryQuest> quests = new ArrayList<>();
            ListTag listTag = nbt.getList(player, 10);
            for (int i = 0; i < listTag.size(); i++) {
                CompoundTag compound = listTag.getCompound(i);
                CategoryQuest categoryQuest = new CategoryQuest();
                categoryQuest.deserializeNBT(Platform.getFrozenRegistry(), compound);
                quests.add(categoryQuest);
            }
            questSavedData.categoryQuestsMap.put(uuid, quests);
        }
        return questSavedData;
    }

    @Override
    public @NotNull CompoundTag save(@NotNull CompoundTag compoundTag, HolderLookup.@NotNull Provider provider) {
        for (var entry : categoryQuestsMap.entrySet()) {
            ListTag listTag = new ListTag();
            UUID player = entry.getKey();
            List<CategoryQuest> categoryQuests = entry.getValue();
            for (CategoryQuest categoryQuest : categoryQuests) {
                CompoundTag compound = categoryQuest.serializeNBT(Platform.getFrozenRegistry());
                listTag.add(compound);
            }
            compoundTag.put(player.toString(), listTag);
        }
        return compoundTag;
    }
}
