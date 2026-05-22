package com.viscriptquests.gui.blueprint;

import com.lowdragmc.lowdraglib2.configurator.IConfigurable;
import com.lowdragmc.lowdraglib2.configurator.accessors.EnumAccessor;
import com.lowdragmc.lowdraglib2.configurator.accessors.ItemStackAccessor;
import com.lowdragmc.lowdraglib2.configurator.ui.ArrayConfiguratorGroup;
import com.lowdragmc.lowdraglib2.configurator.ui.Configurator;
import com.lowdragmc.lowdraglib2.configurator.ui.ConfiguratorGroup;
import com.lowdragmc.lowdraglib2.configurator.ui.ConfiguratorSelectorConfigurator;
import com.lowdragmc.lowdraglib2.configurator.ui.StringConfigurator;
import com.lowdragmc.lowdraglib2.gui.texture.Icons;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.type.TypeHandle;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.type.TypeHandleHelpers;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.type.TypeHandles;
import com.viscriptquests.gui.blueprint.data.QuestRegistryId;
import com.viscriptquests.gui.blueprint.data.MathOperation;
import com.viscriptquests.gui.blueprint.QuestBlueprintCompiler.CompareOp;
import com.viscriptquests.quest.data.DisplayIcon;
import com.viscriptquests.quest.data.DisplayIcon.IconType;
import com.viscriptquests.quest.data.QuestJoinMode;
import com.viscriptquests.quest.data.QuestSubmitMode;
import com.viscriptquests.quest.data.TaskObjectiveType;
import com.viscriptquests.quest.data.LootTableConfig;
import com.viscriptquests.quest.data.VariableMutationOp;
import com.viscriptquests.quest.data.reward.LootTableReward;
import dev.vfyjxf.taffy.style.FlexDirection;
import net.minecraft.world.item.ItemStack;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public final class QuestBlueprintTypes {
    // 多行文本
    public static final TypeHandle STRING_ARRAY = TypeHandleHelpers.fromType(String[].class, "String[]");
    // 任务提交模式枚举
    public static final TypeHandle SUBMIT_MODE = TypeHandleHelpers.fromType(QuestSubmitMode.class);
    // 小任务目标语义：必做、可选、失败条件
    public static final TypeHandle OBJECTIVE_TYPE = TypeHandleHelpers.fromType(TaskObjectiveType.class);
    // 汇合模式枚举，表达任选、全做、至少完成 N 个
    public static final TypeHandle JOIN_MODE = TypeHandleHelpers.fromType(QuestJoinMode.class);
    // 战利品表来源类型枚举
    public static final TypeHandle LOOT_TABLE_TYPE = TypeHandleHelpers.fromType(LootTableReward.LootTableType.class);
    // 数学节点的运算模式枚举
    public static final TypeHandle MATH_OPERATION = TypeHandleHelpers.fromType(MathOperation.class);
    // 比较节点的比较模式枚举
    public static final TypeHandle COMPARE_OP = TypeHandleHelpers.fromType(CompareOp.class);
    // 计分板/变量修改节点复用的数值写入运算模式
    public static final TypeHandle VARIABLE_MUTATION_OP = TypeHandleHelpers.fromType(VariableMutationOp.class);
    // 任意类型，用于通用比较节点的输入端口，可接受所有类型的连线
    public static final TypeHandle OBJECT = TypeHandleHelpers.fromType(Object.class, "Object");
    // 显示图标，支持物品图标和资源包图片两种模式
    public static final TypeHandle DISPLAY_ICON = TypeHandleHelpers.fromType(DisplayIcon.class, "DisplayIcon");
    // 战利品奖励本体，蓝图里只把战利品相关字段当作一个复合配置来编辑。
    public static final TypeHandle LOOT_TABLE_REWARD = TypeHandleHelpers.fromType(LootTableReward.class, "LootTableReward");
    // 维度 ID，底层保存为字符串包装，编辑器展示搜索补全框
    public static final TypeHandle DIMENSION_ID = TypeHandleHelpers.customType(QuestRegistryId.class, "viscript_quests:dimension_id", "DimensionId");
    // 实体类型 ID，底层保存为字符串包装，编辑器展示搜索补全框
    public static final TypeHandle ENTITY_TYPE_ID = TypeHandleHelpers.customType(QuestRegistryId.class, "viscript_quests:entity_type_id", "EntityTypeId");
    // 任意实体类型 ID，用于实体交互等目标，不限制为 LivingEntity
    public static final TypeHandle ANY_ENTITY_TYPE_ID = TypeHandleHelpers.customType(QuestRegistryId.class, "viscript_quests:any_entity_type_id", "AnyEntityTypeId");
    // 进度 ID，来自服务端同步的 Advancement 列表
    public static final TypeHandle ADVANCEMENT_ID = TypeHandleHelpers.customType(QuestRegistryId.class, "viscript_quests:advancement_id", "AdvancementId");
    // 自定义战利品表条目列表，必须保留泛型信息，避免 LDLib2 把它当成裸 List。
    public static final TypeHandle LOOT_TABLE_CONFIG_LIST = TypeHandleHelpers.customType(lootTableConfigListType(),
            "viscript_quests:loot_table_config_list", "LootTableConfigList");

    static {
        TypeHandleHelpers.setCustomIcon(TypeHandles.ITEM_STACK, Icons.RESOURCE);
        TypeHandleHelpers.setCustomColorAndIcon(STRING_ARRAY, 0xFFE3890B, Icons.STRING.copy().setColor(0xFFE3890B));
        TypeHandleHelpers.setCustomDefaultValue(STRING_ARRAY, () -> new String[0]);
        TypeHandleHelpers.setCustomColorAndIcon(DISPLAY_ICON, 0xFF9D6BFF, Icons.RESOURCE.copy().setColor(0xFF9D6BFF));
        TypeHandleHelpers.setCustomDefaultValue(DISPLAY_ICON, DisplayIcon::new);
        TypeHandleHelpers.setCustomConfigurable(DISPLAY_ICON, (valueConfigurable, typeHandle) ->
                IConfigurable.create(father -> {
                    DisplayIcon icon = valueConfigurable.getValue();
                    if (icon == null) {
                        icon = createDefaultDisplayIcon(valueConfigurable.getDefaultValue());
                        valueConfigurable.setValue(icon);
                    }
                    father.addConfigurator(createDisplayIconConfigurator(icon, valueConfigurable::notifyValueChanged));
                }));
        TypeHandleHelpers.setCustomColorAndIcon(LOOT_TABLE_REWARD, 0xFFFFD166, Icons.RESOURCE.copy().setColor(0xFFFFD166));
        TypeHandleHelpers.setCustomDefaultValue(LOOT_TABLE_REWARD, QuestBlueprintTypes::defaultLootTableReward);
        TypeHandleHelpers.setCustomConfigurable(LOOT_TABLE_REWARD, (valueConfigurable, typeHandle) ->
                IConfigurable.create(father -> {
                    LootTableReward reward = valueConfigurable.getValue();
                    if (reward == null) {
                        reward = copyOrDefaultLootTableReward(valueConfigurable.getDefaultValue());
                        valueConfigurable.setValue(reward);
                    }
                    father.addConfigurator(createLootTableRewardConfigurator(reward, valueConfigurable::notifyValueChanged));
                }));
        registerRegistryIdType(DIMENSION_ID, "minecraft:overworld");
        registerRegistryIdType(ENTITY_TYPE_ID, "minecraft:pig");
        registerRegistryIdType(ANY_ENTITY_TYPE_ID, "minecraft:pig");
        registerRegistryIdType(ADVANCEMENT_ID, "minecraft:story/root");
        TypeHandleHelpers.setCustomColorAndIcon(LOOT_TABLE_CONFIG_LIST, 0xFFFFD166, Icons.RESOURCE.copy().setColor(0xFFFFD166));
        TypeHandleHelpers.setCustomDefaultValue(LOOT_TABLE_CONFIG_LIST, ArrayList::new);
        TypeHandleHelpers.setCustomConfigurable(LOOT_TABLE_CONFIG_LIST, (valueConfigurable, typeHandle) ->
                IConfigurable.create(father -> father.addConfigurator(createLootTableConfigListConfigurator(valueConfigurable))));
    }

    private static Type lootTableConfigListType() {
        return new ParameterizedType() {
            @Override
            public Type[] getActualTypeArguments() {
                return new Type[]{LootTableConfig.class};
            }

            @Override
            public Type getRawType() {
                return List.class;
            }

            @Override
            public Type getOwnerType() {
                return null;
            }

            @Override
            public String getTypeName() {
                return "java.util.List<com.viscriptquests.quest.data.LootTableConfig>";
            }
        };
    }

    private static DisplayIcon createDefaultDisplayIcon(Object defaultValue) {
        DisplayIcon icon = new DisplayIcon();
        if (defaultValue instanceof DisplayIcon defaultIcon) {
            icon.setType(defaultIcon.getType());
            icon.setItemStack(defaultIcon.getItemStack().copy());
            icon.setTexture(defaultIcon.getTexture());
        }
        return icon;
    }

    private static Configurator createDisplayIconConfigurator(DisplayIcon icon, Runnable onChanged) {
        if (icon.getType() == null) {
            icon.setType(IconType.ITEM);
        }
        var selector = new ConfiguratorSelectorConfigurator<>(
                "",
                icon::getType,
                type -> {
                    icon.setType(type == null ? IconType.ITEM : type);
                    onChanged.run();
                },
                IconType.ITEM,
                true,
                List.of(IconType.values()),
                EnumAccessor::getEnumName,
                (type, group) -> group.addConfigurator(createDisplayIconValueConfigurator(icon, onChanged))
        );
        selector.layout(layout -> layout.widthPercent(100));
        selector.lineContainer.layout(layout -> layout.widthPercent(100));
        selector.inlineContainer.layout(layout -> layout.widthPercent(100));
        selector.selector.layout(layout -> layout.widthPercent(100));
        selector.container.layout(layout -> layout.widthPercent(100));
        selector.container.configuratorContainer.layout(layout -> {
            layout.widthPercent(100);
            layout.paddingAll(0);
            layout.marginLeft(0);
        });

        var container = new Configurator("");
        container.layout(layout -> {
            layout.widthPercent(100);
            layout.flexDirection(FlexDirection.COLUMN);
            layout.gapAll(2);
        });
        container.lineContainer.setDisplay(false);
        container.addChild(selector);
        return container;
    }

    private static Configurator createLootTableConfigListConfigurator(com.lowdragmc.lowdraglib2.nodegraphtookit.api.IFieldValueConfigurable valueConfigurable) {
        List<LootTableConfig> value = valueConfigurable.getValue();
        if (value == null) {
            value = new ArrayList<>();
            valueConfigurable.setValue(value);
        }
        List<LootTableConfig> lootConfigs = value;
        ArrayConfiguratorGroup<LootTableConfig> group = new ArrayConfiguratorGroup<>(
                "",
                false,
                () -> lootConfigs,
                (getter, setter) -> {
                    ConfiguratorGroup itemGroup = new ConfiguratorGroup("", false).hideTitle();
                    itemGroup.setCanCollapse(false);
                    LootTableConfig config = getter.get();
                    if (config == null) {
                        config = new LootTableConfig();
                        setter.accept(config);
                    }
                    LootTableConfig finalConfig = config;
                    finalConfig.buildConfigurator(itemGroup);
                    itemGroup.addEventListener(Configurator.CHANGE_EVENT, event -> {
                        setter.accept(finalConfig);
                        valueConfigurable.notifyValueChanged();
                    });
                    return itemGroup;
                },
                true
        );
        group.setAddDefault(LootTableConfig::new);
        group.setOnUpdate(list -> {
            lootConfigs.clear();
            lootConfigs.addAll(list);
            valueConfigurable.setValue(lootConfigs);
            valueConfigurable.notifyValueChanged();
        });
        group.layout(layout -> layout.widthPercent(100));
        group.configuratorContainer.layout(layout -> {
            layout.widthPercent(100);
            layout.paddingAll(2);
            layout.marginLeft(0);
        });
        return group;
    }

    public static LootTableReward defaultLootTableReward() {
        LootTableReward reward = new LootTableReward();
        reward.dataPackPath = "minecraft:chests/simple_dungeon";
        return reward;
    }

    private static LootTableReward copyOrDefaultLootTableReward(Object defaultValue) {
        if (defaultValue instanceof LootTableReward reward) {
            return reward.copyLootOptions();
        }
        return defaultLootTableReward();
    }

    private static Configurator createLootTableRewardConfigurator(LootTableReward reward, Runnable onChanged) {
        if (reward.lootTableType == null) {
            reward.lootTableType = LootTableReward.LootTableType.DATA_PACK;
        }
        var selector = new ConfiguratorSelectorConfigurator<>(
                "",
                () -> reward.lootTableType,
                type -> {
                    reward.lootTableType = type == null ? LootTableReward.LootTableType.DATA_PACK : type;
                    onChanged.run();
                },
                LootTableReward.LootTableType.DATA_PACK,
                true,
                List.of(LootTableReward.LootTableType.values()),
                EnumAccessor::getEnumName,
                (type, group) -> group.addConfigurator(createLootTableRewardValueConfigurator(reward, onChanged))
        );
        selector.layout(layout -> layout.widthPercent(100));
        selector.lineContainer.layout(layout -> layout.widthPercent(100));
        selector.inlineContainer.layout(layout -> layout.widthPercent(100));
        selector.selector.layout(layout -> {
            layout.width(120);
            layout.minWidth(120);
        });
        selector.container.layout(layout -> layout.widthPercent(100));
        selector.container.configuratorContainer.layout(layout -> {
            layout.widthPercent(100);
            layout.paddingAll(0);
            layout.marginLeft(0);
        });
        return selector;
    }

    private static Configurator createLootTableRewardValueConfigurator(LootTableReward reward, Runnable onChanged) {
        return switch (reward.lootTableType == null ? LootTableReward.LootTableType.DATA_PACK : reward.lootTableType) {
            case DATA_PACK -> {
                var pathConfigurator = new StringConfigurator(
                        "",
                        () -> reward.dataPackPath,
                        value -> {
                            reward.dataPackPath = value == null ? "" : value;
                            onChanged.run();
                        },
                        "minecraft:chests/simple_dungeon",
                        true
                ).setResourceLocation(true);
                pathConfigurator.layout(layout -> layout.widthPercent(100));
                pathConfigurator.lineContainer.layout(layout -> layout.widthPercent(100));
                pathConfigurator.inlineContainer.layout(layout -> layout.widthPercent(100));
                pathConfigurator.textField.layout(layout -> {
                    layout.width(150);
                    layout.minWidth(150);
                });
                yield pathConfigurator;
            }
            case CUSTOM -> {
                var listConfigurator = createLootTableConfigListConfigurator(reward.customLootTable, onChanged);
                yield listConfigurator;
            }
        };
    }

    private static Configurator createLootTableConfigListConfigurator(List<LootTableConfig> lootConfigs, Runnable onChanged) {
        ArrayConfiguratorGroup<LootTableConfig> group = new ArrayConfiguratorGroup<>(
                "",
                false,
                () -> lootConfigs,
                (getter, setter) -> {
                    ConfiguratorGroup itemGroup = new ConfiguratorGroup("", false).hideTitle();
                    itemGroup.setCanCollapse(false);
                    LootTableConfig config = getter.get();
                    if (config == null) {
                        config = new LootTableConfig();
                        setter.accept(config);
                    }
                    LootTableConfig finalConfig = config;
                    finalConfig.buildConfigurator(itemGroup);
                    itemGroup.addEventListener(Configurator.CHANGE_EVENT, event -> {
                        setter.accept(finalConfig);
                        onChanged.run();
                    });
                    return itemGroup;
                },
                true
        );
        group.setAddDefault(LootTableConfig::new);
        group.setOnUpdate(list -> {
            lootConfigs.clear();
            lootConfigs.addAll(list);
            onChanged.run();
        });
        group.layout(layout -> layout.widthPercent(100));
        group.configuratorContainer.layout(layout -> {
            layout.widthPercent(100);
            layout.paddingAll(2);
            layout.marginLeft(0);
        });
        return group;
    }

    private static Configurator createDisplayIconValueConfigurator(DisplayIcon icon, Runnable onChanged) {
        return switch (icon.getType() == null ? IconType.ITEM : icon.getType()) {
            case ITEM -> {
                if (icon.getItemStack() == null) {
                    icon.setItemStack(ItemStack.EMPTY);
                }
                var itemConfigurator = new ItemStackAccessor().create(
                        "",
                        icon::getItemStack,
                        stack -> {
                            icon.setItemStack(stack == null ? ItemStack.EMPTY : stack.copy());
                            onChanged.run();
                        },
                        true,
                        null,
                        null
                );
                itemConfigurator.layout(layout -> layout.widthPercent(100));
                itemConfigurator.lineContainer.layout(layout -> layout.widthPercent(100));
                itemConfigurator.inlineContainer.layout(layout -> layout.widthPercent(100));
                if (itemConfigurator instanceof ConfiguratorGroup itemGroup) {
                    itemGroup.configuratorContainer.layout(layout -> {
                        layout.widthPercent(100);
                        layout.paddingAll(2);
                        layout.marginLeft(0);
                    });
                }
                yield itemConfigurator;
            }
            case TEXTURE -> {
                var textureConfigurator = new StringConfigurator(
                        "",
                        icon::getTexture,
                        value -> {
                            icon.setTexture(value == null ? "" : value);
                            onChanged.run();
                        },
                        "",
                        true
                ).setResourceLocation(true);
                textureConfigurator.layout(layout -> layout.widthPercent(100));
                textureConfigurator.lineContainer.layout(layout -> layout.widthPercent(100));
                textureConfigurator.inlineContainer.layout(layout -> layout.widthPercent(100));
                textureConfigurator.textField.layout(layout -> layout.widthPercent(100));
                yield textureConfigurator;
            }
        };
    }

    private static void registerRegistryIdType(TypeHandle typeHandle, String defaultValue) {
        TypeHandleHelpers.setCustomColorAndIcon(typeHandle, 0xFF8FD8FF, Icons.RESOURCE.copy().setColor(0xFF8FD8FF));
        TypeHandleHelpers.setCustomDefaultValue(typeHandle, () -> new QuestRegistryId(defaultValue));
        TypeHandleHelpers.setCustomConfigurable(typeHandle, (valueConfigurable, type) ->
                IConfigurable.create(father -> father.addConfigurator(QuestRegistryIdConfigurator.create(valueConfigurable, type))));
    }
}
