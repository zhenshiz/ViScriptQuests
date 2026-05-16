package com.viscriptquests.quest.data;

import com.lowdragmc.lowdraglib2.syncdata.IPersistedSerializable;
import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted;
import net.minecraft.world.item.ItemStack;

//大任务的信息
public class QuestDefinition implements IPersistedSerializable {
    @Persisted
    public String questId = "";
    @Persisted
    public String categoryId = "";
    @Persisted
    public String title = "";
    @Persisted
    public String subtitle = "";
    @Persisted
    public DisplayIcon icon = new DisplayIcon();
}
