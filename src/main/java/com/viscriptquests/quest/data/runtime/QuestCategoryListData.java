package com.viscriptquests.quest.data.runtime;

import com.lowdragmc.lowdraglib2.syncdata.IPersistedSerializable;
import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted;
import com.viscriptquests.util.QuestFileHelper;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

// 分类列表的 RPC/界面数据包装，避免在包里手写列表 NBT。
public class QuestCategoryListData implements IPersistedSerializable {
    @Persisted
    public final List<QuestCategoryData> categories = new ArrayList<>();

    public static QuestCategoryListData of(Collection<QuestCategoryData> categories) {
        QuestCategoryListData data = new QuestCategoryListData();
        data.categories.addAll(sanitize(categories));
        return data;
    }

    public List<QuestCategoryData> copyCategories() {
        return categories.stream()
                .map(QuestCategoryData::copy)
                .toList();
    }

    // 清理无效或重复分类。空列表会原样保留，让服务端默认分类可以被配置为空。
    public static List<QuestCategoryData> sanitize(Collection<QuestCategoryData> categories) {
        List<QuestCategoryData> result = new ArrayList<>();
        Set<String> seenIds = new LinkedHashSet<>();
        for (QuestCategoryData category : categories) {
            if (category == null) {
                continue;
            }
            QuestCategoryData copy = category.copy();
            copy.id = QuestCategoryData.normalizeId(copy.id);
            copy.title = copy.title == null ? "" : copy.title.trim();
            if (copy.id.isBlank() || !seenIds.add(copy.id)) {
                continue;
            }
            copy.normalizeTabBackgrounds();
            sanitizeQuestIds(copy.questIds);
            result.add(copy);
        }
        return result;
    }

    public static void sanitizeQuestIds(List<String> questIds) {
        Set<String> seenIds = new LinkedHashSet<>();
        List<String> normalized = new ArrayList<>();
        for (String questId : questIds) {
            if (questId == null) {
                continue;
            }
            String normalizedQuestId = QuestFileHelper.normalizeQuestId(questId);
            if (normalizedQuestId.isBlank() || !seenIds.add(normalizedQuestId)) {
                continue;
            }
            normalized.add(normalizedQuestId);
        }
        questIds.clear();
        questIds.addAll(normalized);
    }
}
