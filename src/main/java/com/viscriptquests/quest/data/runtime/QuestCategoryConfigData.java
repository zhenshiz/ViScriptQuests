package com.viscriptquests.quest.data.runtime;

import com.lowdragmc.lowdraglib2.syncdata.IPersistedSerializable;
import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted;
import com.viscriptquests.util.QuestFileHelper;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

// 分类配置界面的 RPC 数据：分类内容本身，加上服务端扫描到的任务文件候选项。
public class QuestCategoryConfigData implements IPersistedSerializable {
    @Persisted
    public QuestCategoryListData categoryData = new QuestCategoryListData();
    @Persisted
    public final List<String> questIds = new ArrayList<>();

    public static QuestCategoryConfigData of(QuestCategoryListData categoryData, Collection<String> questIds) {
        QuestCategoryConfigData data = new QuestCategoryConfigData();
        data.categoryData = categoryData == null ? new QuestCategoryListData() : categoryData;
        data.questIds.addAll(sanitizeQuestIds(questIds));
        return data;
    }

    public List<String> copyQuestIds() {
        return sanitizeQuestIds(questIds);
    }

    private static List<String> sanitizeQuestIds(Collection<String> questIds) {
        List<String> result = new ArrayList<>();
        Set<String> seenIds = new LinkedHashSet<>();
        if (questIds == null) {
            return result;
        }
        for (String questId : questIds) {
            if (questId == null) {
                continue;
            }
            String normalizedQuestId = QuestFileHelper.normalizeQuestId(questId);
            if (normalizedQuestId.isBlank() || !seenIds.add(normalizedQuestId)) {
                continue;
            }
            result.add(normalizedQuestId);
        }
        return result;
    }
}
