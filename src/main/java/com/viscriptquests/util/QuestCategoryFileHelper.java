package com.viscriptquests.util;

import com.lowdragmc.lowdraglib2.LDLib2;
import com.lowdragmc.lowdraglib2.Platform;
import com.viscriptquests.ViScriptQuests;
import com.viscriptquests.quest.data.runtime.QuestCategoryData;
import com.viscriptquests.quest.data.runtime.QuestCategoryListData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;

public final class QuestCategoryFileHelper {
    public static final String CATEGORY_FILE = "categories.nbt";

    private static QuestCategoryListData cache;

    private QuestCategoryFileHelper() {
    }

    public static Path categoryDirectory() {
        return LDLib2.getAssetsDir().toPath()
                .resolve(ViScriptQuests.MOD_ID)
                .resolve("category");
    }

    public static Path categoryFile() {
        return categoryDirectory().resolve(CATEGORY_FILE);
    }

    public static QuestCategoryListData getCategories() {
        if (cache != null) {
            return copy(cache);
        }
        cache = readOrEmpty();
        return copy(cache);
    }

    public static List<QuestCategoryData> copyCategories() {
        return getCategories().copyCategories();
    }

    public static void saveCategories(Collection<QuestCategoryData> categories) throws IOException {
        QuestCategoryListData data = QuestCategoryListData.of(categories);
        Files.createDirectories(categoryDirectory());
        NbtIo.writeCompressed(data.serializeNBT(Platform.getFrozenRegistry()), categoryFile());
        cache = copy(data);
    }

    public static void clearCache() {
        cache = null;
    }

    public static Optional<QuestCategoryData> findCategory(String categoryId) {
        String normalizedId = QuestCategoryData.normalizeId(categoryId);
        return copyCategories().stream()
                .filter(category -> category.id.equals(normalizedId))
                .findFirst();
    }

    public static Optional<String> findCategoryIdForQuest(String questId) {
        String normalizedQuestId = QuestFileHelper.normalizeQuestId(questId);
        if (normalizedQuestId.isBlank()) {
            return Optional.empty();
        }
        for (QuestCategoryData category : copyCategories()) {
            if (category.containsQuest(normalizedQuestId)) {
                return Optional.of(category.id);
            }
        }
        return Optional.empty();
    }

    public static LinkedHashMap<String, String> buildQuestCategoryIndex() {
        LinkedHashMap<String, String> index = new LinkedHashMap<>();
        for (QuestCategoryData category : copyCategories()) {
            for (String questId : category.questIds) {
                index.putIfAbsent(QuestFileHelper.normalizeQuestId(questId), category.id);
            }
        }
        return index;
    }

    private static QuestCategoryListData readOrEmpty() {
        Path path = categoryFile();
        if (!Files.exists(path)) {
            return new QuestCategoryListData();
        }
        try {
            CompoundTag tag = NbtIo.readCompressed(path, NbtAccounter.unlimitedHeap());
            QuestCategoryListData data = new QuestCategoryListData();
            data.deserializeNBT(Platform.getFrozenRegistry(), tag);
            return QuestCategoryListData.of(data.copyCategories());
        } catch (IOException e) {
            ViScriptQuests.LOGGER.error("Failed to read quest category config: {}", path, e);
            return new QuestCategoryListData();
        }
    }

    private static QuestCategoryListData copy(QuestCategoryListData source) {
        return QuestCategoryListData.of(source == null ? List.of() : source.copyCategories());
    }
}
