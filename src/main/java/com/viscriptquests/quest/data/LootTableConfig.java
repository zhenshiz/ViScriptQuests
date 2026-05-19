package com.viscriptquests.quest.data;

import com.lowdragmc.lowdraglib2.configurator.IConfigurable;
import com.lowdragmc.lowdraglib2.configurator.accessors.ItemStackAccessor;
import com.lowdragmc.lowdraglib2.configurator.annotation.ConfigNumber;
import com.lowdragmc.lowdraglib2.configurator.ui.ConfiguratorGroup;
import com.lowdragmc.lowdraglib2.configurator.ui.NumberConfigurator;
import com.lowdragmc.lowdraglib2.syncdata.IPersistedSerializable;
import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted;
import net.minecraft.world.item.ItemStack;

// 自定义战利品表的一条掉落配置：每个物品按自己的概率独立抽取。
public class LootTableConfig implements IPersistedSerializable, IConfigurable {
    @Persisted
    public ItemStack itemStack = ItemStack.EMPTY;
    @Persisted
    public float probability = 1.0f;

    public LootTableConfig copy() {
        LootTableConfig config = new LootTableConfig();
        config.itemStack = itemStack == null ? ItemStack.EMPTY : itemStack.copy();
        config.probability = probability;
        return config;
    }

    public float clampedProbability() {
        return Math.max(0.0f, Math.min(1.0f, probability));
    }

    @Override
    public void buildConfigurator(ConfiguratorGroup father) {
        var itemConfigurator = new ItemStackAccessor().create(
                "viscript_quests.loot_table_config.item_stack",
                () -> itemStack == null ? ItemStack.EMPTY : itemStack,
                stack -> itemStack = stack == null ? ItemStack.EMPTY : stack.copy(),
                true,
                null,
                null
        );
        var probabilityConfigurator = new NumberConfigurator(
                "viscript_quests.loot_table_config.probability",
                () -> probability,
                value -> probability = Math.max(0.0f, Math.min(1.0f, value.floatValue())),
                1.0f,
                true
        ).setType(ConfigNumber.Type.FLOAT).setRange(0.0f, 1.0f).setWheel(0.05f);
        father.addConfigurators(itemConfigurator, probabilityConfigurator);
    }
}
