package com.viscriptquests.quest.data.runtime;

import com.lowdragmc.lowdraglib2.syncdata.IPersistedSerializable;
import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted;
import com.viscriptquests.quest.data.DisplayIcon;
import net.minecraft.network.chat.Component;

// 奖励的轻量显示数据，用于客户端任务书 UI 展示
public class RewardDisplay implements IPersistedSerializable {
    // 关联的小任务 ID，为空表示大任务完成时发放的全局奖励
    @Persisted
    public String stepId = "";
    // UI 显示文本。保留 Component 到客户端再解析，避免服务端语言提前固定物品名。
    @Persisted
    public Component displayText = Component.empty();
    // UI 显示图标
    @Persisted
    public DisplayIcon icon = new DisplayIcon();

    public Component displayText() {
        return displayText == null ? Component.empty() : displayText;
    }
}
