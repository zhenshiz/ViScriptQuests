package com.viscriptquests.gui.blueprint;

import com.google.common.base.Predicates;
import com.lowdragmc.lowdraglib2.LDLib2;
import com.lowdragmc.lowdraglib2.configurator.IConfigurable;
import com.lowdragmc.lowdraglib2.configurator.accessors.EnumAccessor;
import com.lowdragmc.lowdraglib2.configurator.accessors.ItemStackAccessor;
import com.lowdragmc.lowdraglib2.configurator.ui.ArrayConfiguratorGroup;
import com.lowdragmc.lowdraglib2.configurator.ui.Configurator;
import com.lowdragmc.lowdraglib2.configurator.ui.ConfiguratorGroup;
import com.lowdragmc.lowdraglib2.configurator.ui.ConfiguratorSelectorConfigurator;
import com.lowdragmc.lowdraglib2.configurator.ui.DataComponentConfigurator;
import com.lowdragmc.lowdraglib2.configurator.ui.RegistrySearchComponent;
import com.lowdragmc.lowdraglib2.configurator.ui.StringConfigurator;
import com.lowdragmc.lowdraglib2.gui.ColorPattern;
import com.lowdragmc.lowdraglib2.gui.sync.bindings.impl.SupplierDataSource;
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.texture.Icons;
import com.lowdragmc.lowdraglib2.gui.texture.ItemStackTexture;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Dialog;
import com.lowdragmc.lowdraglib2.gui.ui.elements.ItemSlot;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.IFieldValueConfigurable;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.type.TypeHandle;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.type.TypeHandleHelpers;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.type.TypeHandles;
import com.viscript_lib.util.item.ItemStackCompareMode;
import com.viscriptquests.ViScriptQuests;
import com.viscriptquests.gui.blueprint.data.QuestRegistryId;
import com.viscriptquests.gui.blueprint.data.MathOperation;
import com.viscriptquests.quest.data.CompareOp;
import com.viscriptquests.quest.data.DisplayIcon;
import com.viscriptquests.quest.data.DisplayIcon.IconType;
import com.viscriptquests.quest.data.ItemMatchRule;
import com.viscriptquests.quest.data.LocationGuideMarkerProvider;
import com.viscriptquests.quest.data.LocationTargetType;
import com.viscriptquests.quest.data.LocationWaypointColor;
import com.viscriptquests.quest.data.QuestJoinMode;
import com.viscriptquests.quest.data.QuestSubmitMode;
import com.viscriptquests.quest.data.TaskObjectiveType;
import com.viscriptquests.quest.data.LootTableConfig;
import com.viscriptquests.quest.data.VariableMutationOp;
import com.viscriptquests.quest.data.reward.LootTableReward;
import dev.vfyjxf.taffy.style.AlignItems;
import dev.vfyjxf.taffy.style.FlexDirection;
import dev.vfyjxf.taffy.style.TaffyDimension;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

public final class QuestBlueprintTypes {
    private static final int ITEM_MATCH_RULE_WIDTH = 180;
    private static final int ITEM_MATCH_SELECTOR_WIDTH = 150;

    // 多行文本
    public static final TypeHandle STRING_ARRAY = TypeHandleHelpers.customType(String[].class, namespacedType("string_array"), "String[]");
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
    // 位置目标的寻路点来源：坐标、生物群系或结构。
    public static final TypeHandle LOCATION_TARGET_TYPE = TypeHandleHelpers.fromType(LocationTargetType.class);
    // 位置目标的导航标记提供方：本模组 HUD 或外部小地图。
    public static final TypeHandle LOCATION_MARKER_PROVIDER = TypeHandleHelpers.fromType(LocationGuideMarkerProvider.class);
    // 外部小地图导航点使用的固定色板。
    public static final TypeHandle LOCATION_WAYPOINT_COLOR = TypeHandleHelpers.fromType(LocationWaypointColor.class);
    // 比较节点的比较模式枚举
    public static final TypeHandle COMPARE_OP = TypeHandleHelpers.fromType(CompareOp.class);
    // 计分板/变量修改节点复用的数值写入运算模式
    public static final TypeHandle VARIABLE_MUTATION_OP = TypeHandleHelpers.fromType(VariableMutationOp.class);
    // 任意类型，用于通用比较节点的输入端口，可接受所有类型的连线
    public static final TypeHandle OBJECT = TypeHandleHelpers.customType(Object.class, namespacedType("object"), "Object");
    // 显示图标，支持物品图标和资源包图片两种模式
    public static final TypeHandle DISPLAY_ICON = TypeHandleHelpers.customType(DisplayIcon.class, namespacedType("display_icon"), "DisplayIcon");
    // 只配置物品身份和组件，不配置数量；任务/奖励数量由单独的动态输入端口决定。
    public static final TypeHandle ITEM_IDENTITY_STACK = TypeHandleHelpers.customType(ItemStack.class,
            namespacedType("item_identity_stack"), "ItemStack");
    // 物品目标的组件匹配规则，支持全量比较、指定组件比较和排除组件比较。
    public static final TypeHandle ITEM_MATCH_RULE = TypeHandleHelpers.customType(ItemMatchRule.class,
            namespacedType("item_match_rule"), "ItemMatchRule");
    // 战利品奖励本体，蓝图里只把战利品相关字段当作一个复合配置来编辑。
    public static final TypeHandle LOOT_TABLE_REWARD = TypeHandleHelpers.customType(LootTableReward.class, namespacedType("loot_table_reward"), "LootTableReward");
    // 维度 ID，底层保存为字符串包装，编辑器展示搜索补全框
    public static final TypeHandle DIMENSION_ID = TypeHandleHelpers.customType(QuestRegistryId.class, "viscript_quests:dimension_id", "DimensionId");
    // 实体类型 ID，底层保存为字符串包装，编辑器展示搜索补全框
    public static final TypeHandle ENTITY_TYPE_ID = TypeHandleHelpers.customType(QuestRegistryId.class, "viscript_quests:entity_type_id", "EntityTypeId");
    // 任意实体类型 ID，用于实体交互等目标，不限制为 LivingEntity
    public static final TypeHandle ANY_ENTITY_TYPE_ID = TypeHandleHelpers.customType(QuestRegistryId.class, "viscript_quests:any_entity_type_id", "AnyEntityTypeId");
    // 生物群系 ID，用于位置目标动态定位。
    public static final TypeHandle BIOME_ID = TypeHandleHelpers.customType(QuestRegistryId.class, "viscript_quests:biome_id", "BiomeId");
    // 结构 ID，用于位置目标动态定位。
    public static final TypeHandle STRUCTURE_ID = TypeHandleHelpers.customType(QuestRegistryId.class, "viscript_quests:structure_id", "StructureId");
    // 进度 ID，使用 VSL 的数据包 JSON 文件补全框读取 advancement 资源
    public static final TypeHandle ADVANCEMENT_ID = TypeHandleHelpers.customType(QuestRegistryId.class, "viscript_quests:advancement_id", "AdvancementId");
    // 思索组件 ID，来自 Ponder Index 中实际注册了教程的物品/方块。
    public static final TypeHandle PONDER_COMPONENT_ID = TypeHandleHelpers.customType(QuestRegistryId.class,
            "viscript_quests:ponder_component_id", "PonderComponentId");
    static {
        TypeHandleHelpers.setCustomIcon(TypeHandles.ITEM_STACK, Icons.RESOURCE);
        registerStringArrayType(STRING_ARRAY);
        registerObjectType(OBJECT);
        registerDisplayIconType(DISPLAY_ICON);
        registerItemIdentityStackType(ITEM_IDENTITY_STACK);
        registerItemMatchRuleType(ITEM_MATCH_RULE);
        registerLootTableRewardType(LOOT_TABLE_REWARD);
        registerRegistryIdType(DIMENSION_ID, "minecraft:overworld");
        registerRegistryIdType(ENTITY_TYPE_ID, "minecraft:pig");
        registerRegistryIdType(ANY_ENTITY_TYPE_ID, "minecraft:pig");
        registerRegistryIdType(BIOME_ID, "minecraft:plains");
        registerRegistryIdType(STRUCTURE_ID, "minecraft:village_plains");
        registerRegistryIdType(ADVANCEMENT_ID, "minecraft:story/root");
        registerRegistryIdType(PONDER_COMPONENT_ID, "minecraft:crafting_table");
    }

    private static String namespacedType(String path) {
        return ViScriptQuests.MOD_ID + ":" + path;
    }

    private static void registerStringArrayType(TypeHandle typeHandle) {
        TypeHandleHelpers.setCustomColorAndIcon(typeHandle, 0xFFE3890B, Icons.STRING.copy().setColor(0xFFE3890B));
        TypeHandleHelpers.setCustomDefaultValue(typeHandle, () -> new String[0]);
    }

    private static void registerObjectType(TypeHandle typeHandle) {
        TypeHandleHelpers.setCustomColorAndIcon(typeHandle, 0xFFB8B8B8, Icons.NODE.copy().setColor(0xFFB8B8B8));
    }

    private static void registerDisplayIconType(TypeHandle typeHandle) {
        TypeHandleHelpers.setCustomColorAndIcon(typeHandle, 0xFF9D6BFF, Icons.RESOURCE.copy().setColor(0xFF9D6BFF));
        TypeHandleHelpers.setCustomDefaultValue(typeHandle, DisplayIcon::new);
        TypeHandleHelpers.setCustomConfigurable(typeHandle, (valueConfigurable, ignored) ->
                IConfigurable.create(father -> {
                    DisplayIcon icon = valueConfigurable.getValue();
                    if (icon == null) {
                        icon = createDefaultDisplayIcon(valueConfigurable.getDefaultValue());
                        valueConfigurable.setValue(icon);
                    }
                    father.addConfigurator(createDisplayIconConfigurator(icon, valueConfigurable::notifyValueChanged));
                }));
    }

    private static void registerItemIdentityStackType(TypeHandle typeHandle) {
        TypeHandleHelpers.setCustomColorAndIcon(typeHandle, 0xFFFFD166, Icons.RESOURCE.copy().setColor(0xFFFFD166));
        TypeHandleHelpers.setCustomDefaultValue(typeHandle, () -> ItemStack.EMPTY);
        TypeHandleHelpers.setCustomConfigurable(typeHandle, (valueConfigurable, ignored) ->
                IConfigurable.create(father -> father.addConfigurator(createItemIdentityStackConfigurator(valueConfigurable))));
    }

    private static void registerItemMatchRuleType(TypeHandle typeHandle) {
        TypeHandleHelpers.setCustomColorAndIcon(typeHandle, 0xFFFFD166, Icons.RESOURCE.copy().setColor(0xFFFFD166));
        TypeHandleHelpers.setCustomDefaultValue(typeHandle, ItemMatchRule::new);
        TypeHandleHelpers.setCustomConfigurable(typeHandle, (valueConfigurable, ignored) ->
                IConfigurable.create(father -> {
                    ItemMatchRule rule = valueConfigurable.getValue();
                    if (rule == null) {
                        rule = copyOrDefaultItemMatchRule(valueConfigurable.getDefaultValue());
                        valueConfigurable.setValue(rule);
                    }
                    rule.ensureDefaults();
                    father.addConfigurator(createItemMatchRuleConfigurator(rule, valueConfigurable::notifyValueChanged));
                }));
    }

    private static void registerLootTableRewardType(TypeHandle typeHandle) {
        TypeHandleHelpers.setCustomColorAndIcon(typeHandle, 0xFFFFD166, Icons.RESOURCE.copy().setColor(0xFFFFD166));
        TypeHandleHelpers.setCustomDefaultValue(typeHandle, QuestBlueprintTypes::defaultLootTableReward);
        TypeHandleHelpers.setCustomConfigurable(typeHandle, (valueConfigurable, ignored) ->
                IConfigurable.create(father -> {
                    LootTableReward reward = valueConfigurable.getValue();
                    if (reward == null) {
                        reward = copyOrDefaultLootTableReward(valueConfigurable.getDefaultValue());
                        valueConfigurable.setValue(reward);
                    }
                    father.addConfigurator(createLootTableRewardConfigurator(reward, valueConfigurable::notifyValueChanged));
                }));
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

    private static ItemMatchRule copyOrDefaultItemMatchRule(Object defaultValue) {
        if (defaultValue instanceof ItemMatchRule rule) {
            return rule.copy();
        }
        return new ItemMatchRule();
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
        LootTableReward.LootTableType type = reward.lootTableType == null
                ? LootTableReward.LootTableType.DATA_PACK
                : reward.lootTableType;
        if (type == LootTableReward.LootTableType.CUSTOM) {
            return createLootTableConfigListConfigurator(reward.customLootTable, onChanged);
        }

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
        return pathConfigurator;
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
        IconType type = icon.getType() == null ? IconType.ITEM : icon.getType();
        if (type == IconType.ITEM) {
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
            return itemConfigurator;
        }

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
        return textureConfigurator;
    }

    private static Configurator createItemMatchRuleConfigurator(ItemMatchRule rule, Runnable onChanged) {
        rule.ensureDefaults();
        var selector = new ConfiguratorSelectorConfigurator<>(
                "",
                rule::resolvedCompareMode,
                mode -> {
                    rule.setCompareMode(mode == null ? ItemStackCompareMode.ALL_COMPONENTS : mode);
                    onChanged.run();
                },
                ItemStackCompareMode.ALL_COMPONENTS,
                true,
                List.of(ItemStackCompareMode.values()),
                EnumAccessor::getEnumName,
                (mode, group) -> {
                    if (mode != ItemStackCompareMode.ALL_COMPONENTS) {
                        Configurator configurator = ItemMatchRule.createComponentsConfigurator(rule, onChanged);
                        configureItemMatchComponentsLayout(configurator);
                        group.addConfigurator(configurator);
                    }
                }
        );
        configureItemMatchRuleLayout(selector);
        return selector;
    }

    private static void configureItemMatchRuleLayout(ConfiguratorSelectorConfigurator<?> selector) {
        selector.layout(layout -> {
            layout.widthPercent(100);
            layout.maxWidth(ITEM_MATCH_RULE_WIDTH);
        });
        selector.lineContainer.layout(layout -> {
            layout.widthPercent(100);
            layout.maxWidth(ITEM_MATCH_RULE_WIDTH);
        });
        selector.inlineContainer.layout(layout -> {
            layout.widthPercent(100);
            layout.maxWidth(ITEM_MATCH_SELECTOR_WIDTH);
        });
        selector.selector.layout(layout -> {
            layout.widthPercent(100);
            layout.maxWidth(ITEM_MATCH_SELECTOR_WIDTH);
        });
        selector.container.layout(layout -> {
            layout.widthPercent(100);
            layout.maxWidth(ITEM_MATCH_RULE_WIDTH);
        });
        selector.container.configuratorContainer.layout(layout -> {
            layout.widthPercent(100);
            layout.maxWidth(ITEM_MATCH_RULE_WIDTH);
            layout.paddingAll(2);
            layout.marginLeft(0);
        });
    }

    private static void configureItemMatchComponentsLayout(Configurator configurator) {
        configurator.layout(layout -> {
            layout.widthPercent(100);
            layout.maxWidth(ITEM_MATCH_RULE_WIDTH);
        });
        configurator.lineContainer.layout(layout -> {
            layout.widthPercent(100);
            layout.maxWidth(ITEM_MATCH_RULE_WIDTH);
        });
        configurator.inlineContainer.layout(layout -> {
            layout.widthPercent(100);
            layout.maxWidth(ITEM_MATCH_RULE_WIDTH);
        });
        if (configurator instanceof ConfiguratorGroup group) {
            group.configuratorContainer.layout(layout -> {
                layout.widthPercent(100);
                layout.maxWidth(ITEM_MATCH_RULE_WIDTH - 8);
                layout.paddingAll(2);
                layout.marginLeft(0);
            });
        }
        if (configurator instanceof ArrayConfiguratorGroup<?> arrayGroup) {
            dockArrayButtonsToHeader(arrayGroup);
        }
    }

    private static void dockArrayButtonsToHeader(ArrayConfiguratorGroup<?> group) {
        group.buttonGroup.removeSelf();
        group.buttonGroup.addEventListener(UIEvents.MOUSE_DOWN, event -> event.stopPropagation());
        group.buttonGroup.layout(layout -> {
            layout.flexDirection(FlexDirection.ROW);
            layout.alignItems(AlignItems.CENTER);
            layout.alignSelf(AlignItems.CENTER);
            layout.paddingAll(0);
            layout.marginLeft(2);
        }).style(style -> style.backgroundTexture(IGuiTexture.EMPTY));
        group.lineContainer.addChildAt(group.buttonGroup, Math.max(0, group.lineContainer.getChildren().size() - 1));
    }

    private static Configurator createItemIdentityStackConfigurator(IFieldValueConfigurable valueConfigurable) {
        Supplier<ItemStack> supplier = () -> readItemIdentityStack(valueConfigurable);
        Consumer<ItemStack> consumer = stack -> {
            valueConfigurable.setValue(normalizeItemIdentityStack(stack));
            valueConfigurable.notifyValueChanged();
        };
        ItemStack initial = supplier.get();
        valueConfigurable.setValue(initial);

        var group = new ConfiguratorGroup("");
        var slot = new ItemSlot();
        slot.layout(layout -> layout.width(14).height(14));
        slot.bindDataSource(SupplierDataSource.of(supplier));
        Consumer<ItemStack> updater = itemStack -> {
            ItemStack normalizedStack = normalizeItemIdentityStack(itemStack);
            slot.setItem(normalizedStack);
            consumer.accept(normalizedStack);
        };
        var inventoryButton = new Button();
        inventoryButton.style(style -> style.tooltips("ldlib.gui.editor.configurator.select_item.tooltip"));
        inventoryButton.noText();
        inventoryButton.addPreIcon(new ItemStackTexture(Items.CHEST));
        group.inlineContainer.getLayout().flexDirection(FlexDirection.ROW);
        group.inlineContainer.addChildren(slot, new UIElement().layout(layout -> layout.flex(1)), inventoryButton);

        var componentsConfigurator = new DataComponentConfigurator(
                supplier.get().getItem().components(),
                () -> supplier.get().getComponentsPatch(),
                patch -> {
                    ItemStack current = supplier.get();
                    updater.accept(new ItemStack(current.getItem().builtInRegistryHolder(), 1, patch));
                },
                valueConfigurable.forceUpdate()
        );
        inventoryButton.setOnClick(event -> {
            if (!LDLib2.isClient()) return;
            var mui = event.currentElement.getModularUI();
            if (mui == null || Minecraft.getInstance().player == null) return;
            var inventory = Minecraft.getInstance().player.getInventory();
            var dialog = new Dialog().setTitle("ldlib.gui.editor.configurator.select_item");
            dialog.width(TaffyDimension.length(180));

            var selected = new ItemSlot[]{null};
            var selectedStack = new ItemStack[]{ItemStack.EMPTY};

            var picker = new UIElement().layout(layout -> layout.alignItems(AlignItems.CENTER));

            var main = new UIElement();
            for (int r = 0; r < 3; r++) {
                var row = new UIElement().layout(layout -> layout.flexDirection(FlexDirection.ROW));
                for (int c = 0; c < 9; c++) {
                    row.addChild(createPickerSlot(inventory.getItem(r * 9 + c + 9).copy(), selected, selectedStack));
                }
                main.addChild(row);
            }
            picker.addChild(main);

            var hotbar = new UIElement().layout(layout -> layout
                    .flexDirection(FlexDirection.ROW)
                    .marginTop(5));
            for (int c = 0; c < 9; c++) {
                hotbar.addChild(createPickerSlot(inventory.getItem(c).copy(), selected, selectedStack));
            }
            picker.addChild(hotbar);

            dialog.addContent(picker);
            dialog.addButton(new Button()
                    .setOnClick(e -> {
                        ItemStack selectedIdentityStack = normalizeItemIdentityStack(selectedStack[0]);
                        updater.accept(selectedIdentityStack);
                        componentsConfigurator.setPrototype(selectedIdentityStack.getItem().components());
                        group.notifyChanges();
                        dialog.close();
                    })
                    .setText("ldlib.gui.tips.confirm")
                    .addClass("__confirm-button__"));
            dialog.addButton(new Button()
                    .setOnClick(e -> dialog.close())
                    .setText("ldlib.gui.tips.cancel")
                    .addClass("__cancel-button__"));
            dialog.show(mui);
            event.stopImmediatePropagation();
        });
        ItemStack defaultValue = normalizeItemIdentityStack(valueConfigurable.getDefaultValue());
        var itemConfigurator = new RegistrySearchComponent.Item(
                "configurator.item",
                () -> supplier.get().getItem(),
                item -> {
                    updater.accept(new ItemStack(item.builtInRegistryHolder(), 1, supplier.get().getComponentsPatch()));
                    componentsConfigurator.setPrototype(item.components());
                },
                defaultValue.getItem(),
                valueConfigurable.forceUpdate()
        );

        group.addConfigurators(itemConfigurator, componentsConfigurator);
        if (LDLib2.isJeiLoaded()) {
            RegistrySearchComponent.JEISupport.ghostItem(group, Predicates.alwaysTrue(), itemStack -> {
                ItemStack selectedIdentityStack = normalizeItemIdentityStack(itemStack);
                updater.accept(selectedIdentityStack);
                componentsConfigurator.setPrototype(selectedIdentityStack.getItem().components());
                group.notifyChanges();
            });
        }
        if (LDLib2.isReiLoaded()) {
            RegistrySearchComponent.REISupport.ghostItem(group, Predicates.alwaysTrue(), itemStack -> {
                ItemStack selectedIdentityStack = normalizeItemIdentityStack(itemStack);
                updater.accept(selectedIdentityStack);
                componentsConfigurator.setPrototype(selectedIdentityStack.getItem().components());
                group.notifyChanges();
            });
        }
        if (LDLib2.isEmiLoaded()) {
            RegistrySearchComponent.EMISupport.ghostItem(group, Predicates.alwaysTrue(), itemStack -> {
                ItemStack selectedIdentityStack = normalizeItemIdentityStack(itemStack);
                updater.accept(selectedIdentityStack);
                componentsConfigurator.setPrototype(selectedIdentityStack.getItem().components());
                group.notifyChanges();
            });
        }
        group.layout(layout -> layout.widthPercent(100));
        group.lineContainer.layout(layout -> layout.widthPercent(100));
        group.configuratorContainer.layout(layout -> {
            layout.widthPercent(100);
            layout.paddingAll(2);
            layout.marginLeft(0);
        });
        return group;
    }

    private static ItemStack readItemIdentityStack(IFieldValueConfigurable valueConfigurable) {
        ItemStack stack = normalizeItemIdentityStack(valueConfigurable.getValue());
        if (stack.isEmpty()) {
            stack = normalizeItemIdentityStack(valueConfigurable.getDefaultValue());
        }
        return stack;
    }

    private static ItemStack normalizeItemIdentityStack(ItemStack stack) {
        return stack == null || stack.isEmpty() ? ItemStack.EMPTY : stack.copyWithCount(1);
    }

    private static ItemSlot createPickerSlot(ItemStack stack, ItemSlot[] selected, ItemStack[] selectedStack) {
        var itemSlot = new ItemSlot();
        itemSlot.setItem(stack, false);
        itemSlot.addEventListener(UIEvents.MOUSE_DOWN, e -> {
            if (selected[0] == itemSlot) return;
            if (selected[0] != null) {
                selected[0].getStyle().overlayTexture(IGuiTexture.EMPTY);
            }
            selected[0] = itemSlot;
            selectedStack[0] = itemSlot.getValue();
            itemSlot.getStyle().overlayTexture(ColorPattern.T_BLUE.rectTexture());
        });
        return itemSlot;
    }

    private static void registerRegistryIdType(TypeHandle typeHandle, String defaultValue) {
        TypeHandleHelpers.setCustomColorAndIcon(typeHandle, 0xFF8FD8FF, Icons.RESOURCE.copy().setColor(0xFF8FD8FF));
        TypeHandleHelpers.setCustomDefaultValue(typeHandle, () -> new QuestRegistryId(defaultValue));
        TypeHandleHelpers.setCustomConfigurable(typeHandle, (valueConfigurable, type) ->
                IConfigurable.create(father -> father.addConfigurator(QuestRegistryIdConfigurator.create(valueConfigurable, type))));
    }
}
