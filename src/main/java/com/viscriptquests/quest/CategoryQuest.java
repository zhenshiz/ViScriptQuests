package com.viscriptquests.quest;

import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.syncdata.IPersistedSerializable;
import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib2.syncdata.annotation.ReadOnlyManaged;
import lombok.Data;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.Tag;

import java.util.ArrayList;
import java.util.List;

@Data
public class CategoryQuest implements IPersistedSerializable {
    @Persisted
    private String id;
    @Persisted
    private IGuiTexture icon = IGuiTexture.EMPTY;
    @Persisted
    private String name;
    @Persisted
    @ReadOnlyManaged(serializeMethod = "writeQuestInfo", deserializeMethod = "readQuestInfo")
    private List<QuestInfo> quests = new ArrayList<>();

    public static CategoryQuest of(String id, IGuiTexture icon, String name) {
        CategoryQuest categoryQuest = new CategoryQuest();
        categoryQuest.id = id;
        categoryQuest.icon = icon;
        categoryQuest.name = name;
        return categoryQuest;
    }

    private Tag writeQuestInfo(List<QuestInfo> value) {
        return IntTag.valueOf(value.size());
    }

    private List<QuestInfo> readQuestInfo(IntTag tag) {
        List<QuestInfo> list = new ArrayList<>();
        for (int i = 0; i < tag.getAsInt(); i++) {
            list.add(new QuestInfo());
        }
        return list;
    }
}
