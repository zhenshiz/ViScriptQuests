package com.viscriptquests.quest.data;

import com.lowdragmc.lowdraglib2.configurator.ConfiguratorAccessors;
import com.lowdragmc.lowdraglib2.configurator.IConfigurable;
import com.lowdragmc.lowdraglib2.configurator.accessors.IConfiguratorAccessor;
import com.lowdragmc.lowdraglib2.configurator.annotation.ConfigSelector;
import com.lowdragmc.lowdraglib2.configurator.annotation.Configurable;
import com.lowdragmc.lowdraglib2.configurator.ui.Configurator;
import com.lowdragmc.lowdraglib2.configurator.ui.ConfiguratorGroup;
import com.lowdragmc.lowdraglib2.syncdata.IPersistedSerializable;
import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted;
import com.viscript_lib.util.item.ItemStackCompareMode;
import com.viscript_lib.util.item.ItemUtil;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

/**
 * 物品组件匹配规则。模式含义与 VSL 的 {@link ItemStackCompareMode} 保持一致：
 * 比较所有组件、只比较指定组件、或比较时排除指定组件。
 */
public class ItemMatchRule implements IConfigurable, IPersistedSerializable {
    @Configurable(name = "viscript_quests.item_match_rule.compare_mode")
    @ConfigSelector(subConfiguratorBuilder = "compareModeSubConfiguratorBuilder")
    @Persisted
    private ItemStackCompareMode compareMode = ItemStackCompareMode.ALL_COMPONENTS;
    @Persisted
    private List<DataComponentType<?>> components = new ArrayList<>();

    public ItemMatchRule() {
    }

    public ItemMatchRule(ItemStackCompareMode compareMode, List<DataComponentType<?>> components) {
        this.compareMode = compareMode == null ? ItemStackCompareMode.ALL_COMPONENTS : compareMode;
        this.components = copyComponents(components);
    }

    public boolean matches(ItemStack candidate, ItemStack target) {
        return ItemUtil.isSameItem(candidate, target, resolvedCompareMode(), resolvedComponents());
    }

    public int getItemForPlayerCount(ServerPlayer player, ItemStack itemStack) {
        return ItemUtil.getItemForPlayerCount(player, itemStack, resolvedCompareMode(), resolvedComponents());
    }

    public int removeItemForPlayer(ServerPlayer player, ItemStack itemStack, int count) {
        return ItemUtil.removeItemForPlayer(player, itemStack, count, resolvedCompareMode(), resolvedComponents());
    }

    public ItemMatchRule copy() {
        return new ItemMatchRule(resolvedCompareMode(), resolvedComponents());
    }

    public ItemStackCompareMode resolvedCompareMode() {
        return compareMode == null ? ItemStackCompareMode.ALL_COMPONENTS : compareMode;
    }

    public List<DataComponentType<?>> resolvedComponents() {
        return components == null ? List.of() : components;
    }

    public void ensureDefaults() {
        if (compareMode == null) {
            compareMode = ItemStackCompareMode.ALL_COMPONENTS;
        }
        if (components == null) {
            components = new ArrayList<>();
        }
    }

    public ItemStackCompareMode getCompareMode() {
        return compareMode;
    }

    public void setCompareMode(ItemStackCompareMode compareMode) {
        this.compareMode = compareMode == null ? ItemStackCompareMode.ALL_COMPONENTS : compareMode;
    }

    public List<DataComponentType<?>> getComponents() {
        ensureDefaults();
        return components;
    }

    public void setComponents(List<DataComponentType<?>> components) {
        this.components = copyComponents(components);
    }

    private void compareModeSubConfiguratorBuilder(ItemStackCompareMode value, ConfiguratorGroup group) {
        if (value == ItemStackCompareMode.ALL_COMPONENTS) {
            return;
        }
        group.addConfigurator(createComponentsConfigurator(this, () -> {
        }));
    }

    public static Configurator createComponentsConfigurator(ItemMatchRule rule, Runnable onChanged) {
        rule.ensureDefaults();
        try {
            Field field = ItemMatchRule.class.getDeclaredField("components");
            @SuppressWarnings({"rawtypes", "unchecked"})
            Configurator configurator = ((IConfiguratorAccessor) ConfiguratorAccessors.findByType(field.getGenericType())).create(
                    "viscript_quests.item_match_rule.components",
                    rule::getComponents,
                    value -> {
                        if (value instanceof List<?> list) {
                            rule.setComponents(copyComponents(list));
                        } else {
                            rule.setComponents(List.of());
                        }
                        onChanged.run();
                    },
                    true,
                    field,
                    rule
            ).setTips("viscript_quests.item_match_rule.components.tips");
            if (configurator instanceof ConfiguratorGroup componentGroup) {
                componentGroup.setCollapse(false);
            }
            return configurator;
        } catch (NoSuchFieldException e) {
            throw new IllegalStateException(e);
        }
    }

    private static List<DataComponentType<?>> copyComponents(List<?> source) {
        List<DataComponentType<?>> copy = new ArrayList<>();
        if (source == null) {
            return copy;
        }
        for (Object value : source) {
            if (value instanceof DataComponentType<?> componentType) {
                copy.add(componentType);
            }
        }
        return copy;
    }
}
