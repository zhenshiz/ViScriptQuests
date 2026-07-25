package com.viscriptquests.quest.data.runtime;

import com.lowdragmc.lowdraglib2.Platform;
import com.lowdragmc.lowdraglib2.configurator.IConfigurable;
import com.lowdragmc.lowdraglib2.configurator.annotation.Configurable;
import com.lowdragmc.lowdraglib2.syncdata.IPersistedSerializable;
import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib2.utils.PersistedParser;
import com.mojang.serialization.Codec;
import com.viscript_lib.util.CodecUtil;
import com.viscriptquests.quest.data.DisplayIcon;
import com.viscriptquests.util.QuestFileHelper;
import io.netty.buffer.ByteBuf;
import net.minecraft.nbt.Tag;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

// 玩家任务分类数据。默认分类和玩家自己的分类都使用同一份结构。
public class QuestCategoryData implements IPersistedSerializable, IConfigurable {
    public static final ResourceLocation DEFAULT_TAB_BACKGROUND =
            ResourceLocation.parse("viscript_quests:textures/gui/tag/tag_blue.png");
    public static final ResourceLocation DEFAULT_SELECTED_TAB_BACKGROUND =
            ResourceLocation.parse("viscript_quests:textures/gui/tag/tag_blue_select.png");
    public static final Codec<QuestCategoryData> CODEC = PersistedParser.createCodec(QuestCategoryData::new);
    public static final StreamCodec<ByteBuf, QuestCategoryData> STREAM_CODEC = PersistedParser.createStreamCodec(QuestCategoryData::new);

    @Configurable(name = "viscript_quests.questCategory.id")
    public String id = "";
    @Configurable(name = "viscript_quests.questCategory.title")
    public String title = "";
    @Configurable(name = "viscript_quests.questCategory.displayIcon", subConfigurable = true, collapse = false)
    public DisplayIcon displayIcon = new DisplayIcon();
    @Configurable(name = "viscript_quests.questCategory.tabBackground")
    public ResourceLocation tabBackground = DEFAULT_TAB_BACKGROUND;
    @Configurable(name = "viscript_quests.questCategory.selectedTabBackground")
    public ResourceLocation selectedTabBackground = DEFAULT_SELECTED_TAB_BACKGROUND;
    // 该分类下的大任务文件标识，格式与发放任务命令的 quest 参数一致，但不包含外层引号。
    @Persisted
    public final List<String> questIds = new ArrayList<>();

    public static QuestCategoryData of(String id, String title, String iconItemId) {
        return of(id, title, DisplayIcon.item(iconItemId));
    }

    public static QuestCategoryData of(String id, String title, DisplayIcon displayIcon) {
        QuestCategoryData category = new QuestCategoryData();
        category.id = normalizeId(id);
        category.title = title == null ? "" : title.trim();
        category.displayIcon = displayIcon == null ? new DisplayIcon() : displayIcon.copy();
        return category;
    }

    public QuestCategoryData copy() {
        Tag tag = CodecUtil.serializeNBT(CODEC, this, Platform.getFrozenRegistry());
        return CodecUtil.deserializeNBT(CODEC, tag, Platform.getFrozenRegistry());
    }

    public boolean hasId(String otherId) {
        return id.equals(normalizeId(otherId));
    }

    public boolean containsQuest(String questId) {
        String normalizedQuestId = QuestFileHelper.normalizeQuestId(questId);
        return questIds.stream().anyMatch(existing -> existing.equals(normalizedQuestId));
    }

    public ResourceLocation tabBackgroundLocation(boolean selected) {
        ResourceLocation configured = selected ? selectedTabBackground : tabBackground;
        return configured == null
                ? selected ? DEFAULT_SELECTED_TAB_BACKGROUND : DEFAULT_TAB_BACKGROUND
                : configured;
    }

    public void normalizeTabBackgrounds() {
        if (tabBackground == null) {
            tabBackground = DEFAULT_TAB_BACKGROUND;
        }
        if (selectedTabBackground == null) {
            selectedTabBackground = DEFAULT_SELECTED_TAB_BACKGROUND;
        }
    }

    public static String normalizeId(String id) {
        return id == null ? "" : id.trim();
    }
}
