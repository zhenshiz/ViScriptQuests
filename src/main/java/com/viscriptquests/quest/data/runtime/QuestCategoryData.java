package com.viscriptquests.quest.data.runtime;

import com.lowdragmc.lowdraglib2.Platform;
import com.lowdragmc.lowdraglib2.configurator.IConfigurable;
import com.lowdragmc.lowdraglib2.configurator.annotation.Configurable;
import com.lowdragmc.lowdraglib2.syncdata.IPersistedSerializable;
import com.lowdragmc.lowdraglib2.utils.PersistedParser;
import com.mojang.serialization.Codec;
import com.viscriptquests.quest.data.DisplayIcon;
import com.viscriptquests.util.CodecUtil;
import io.netty.buffer.ByteBuf;
import net.minecraft.nbt.Tag;
import net.minecraft.network.codec.StreamCodec;

// 玩家任务分类数据。默认分类和玩家自己的分类都使用同一份结构。
public class QuestCategoryData implements IPersistedSerializable, IConfigurable {
    public static final Codec<QuestCategoryData> CODEC = PersistedParser.createCodec(QuestCategoryData::new);
    public static final StreamCodec<ByteBuf, QuestCategoryData> STREAM_CODEC = PersistedParser.createStreamCodec(QuestCategoryData::new);

    @Configurable(name = "viscript_quests.questCategory.id")
    public String id = "";
    @Configurable(name = "viscript_quests.questCategory.title")
    public String title = "";
    @Configurable(name = "viscript_quests.questCategory.displayIcon", subConfigurable = true, collapse = false)
    public DisplayIcon displayIcon = new DisplayIcon();
    @Configurable(name = "viscript_quests.questCategory.tooltip")
    public String tooltip = "";

    public static QuestCategoryData of(String id, String title, String iconItemId, String tooltip) {
        return of(id, title, DisplayIcon.item(iconItemId), tooltip);
    }

    public static QuestCategoryData of(String id, String title, DisplayIcon displayIcon, String tooltip) {
        QuestCategoryData category = new QuestCategoryData();
        category.id = normalizeId(id);
        category.title = title == null ? "" : title.trim();
        category.displayIcon = displayIcon == null ? new DisplayIcon() : displayIcon.copy();
        category.tooltip = tooltip == null ? "" : tooltip.trim();
        return category;
    }

    public QuestCategoryData copy() {
        Tag tag = CodecUtil.serializeNBT(CODEC, this, Platform.getFrozenRegistry());
        return CodecUtil.deserializeNBT(CODEC, tag, Platform.getFrozenRegistry());
    }

    public boolean hasId(String otherId) {
        return id.equals(normalizeId(otherId));
    }

    public static String normalizeId(String id) {
        return id == null ? "" : id.trim();
    }
}
