package com.viscriptquests.util;

import com.lowdragmc.lowdraglib2.LDLib2;
import com.lowdragmc.lowdraglib2.Platform;
import com.viscriptquests.gui.data.Quest;
import com.viscriptquests.quest.QuestDefinition;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

@ParametersAreNonnullByDefault
public class QuestHelper {
    private final static Map<String, QuestDefinition> CACHE = new HashMap<>();
    public static final String QUEST_PATH = "viscript_quests/quest";

    public static int clearCache() {
        var count = CACHE.size();
        CACHE.clear();
        return count;
    }

    @Nullable
    public static QuestDefinition getQuest(String questLocation) {
        return getQuest(questLocation, true);
    }


    public static QuestDefinition getQuest(String questLocation, boolean useCache) {
        return useCache ? CACHE.getOrDefault(questLocation, getQuest(questLocation)) : loadQuest(questLocation);
    }

    private static QuestDefinition loadQuest(String questLocation) {
        if (questLocation.startsWith("\"")) questLocation = questLocation.substring(1);
        if (questLocation.endsWith("\"")) questLocation = questLocation.substring(0, questLocation.length() - 1);
        File file = new File(LDLib2.getAssetsDir(), QUEST_PATH + "/" + questLocation + Quest.SUFFIX);
        CompoundTag compoundTag;
        if (!file.exists()) return null;
        try (var inputStream = Files.newInputStream(file.toPath())) {
            compoundTag = NbtIo.readCompressed(inputStream, NbtAccounter.unlimitedHeap());
        } catch (IOException e) {
            compoundTag = new CompoundTag();
        }

        QuestDefinition questDefinition = new QuestDefinition();
        questDefinition.deserializeNBT(Platform.getFrozenRegistry(), compoundTag);
        CACHE.put(questLocation, questDefinition);
        return questDefinition;
    }
}
