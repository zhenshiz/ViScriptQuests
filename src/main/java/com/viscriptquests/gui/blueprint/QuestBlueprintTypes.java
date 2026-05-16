package com.viscriptquests.gui.blueprint;

import com.lowdragmc.lowdraglib2.configurator.IConfigurable;
import com.lowdragmc.lowdraglib2.configurator.accessors.EnumAccessor;
import com.lowdragmc.lowdraglib2.configurator.accessors.ItemStackAccessor;
import com.lowdragmc.lowdraglib2.configurator.ui.Configurator;
import com.lowdragmc.lowdraglib2.configurator.ui.ConfiguratorGroup;
import com.lowdragmc.lowdraglib2.configurator.ui.ConfiguratorSelectorConfigurator;
import com.lowdragmc.lowdraglib2.configurator.ui.StringConfigurator;
import com.lowdragmc.lowdraglib2.gui.texture.Icons;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.type.TypeHandle;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.type.TypeHandleHelpers;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.type.TypeHandles;
import com.viscriptquests.gui.blueprint.QuestBlueprintCompiler.CompareOp;
import com.viscriptquests.quest.data.DisplayIcon;
import com.viscriptquests.quest.data.DisplayIcon.IconType;
import com.viscriptquests.quest.data.QuestJoinMode;
import com.viscriptquests.quest.data.QuestSubmitMode;
import dev.vfyjxf.taffy.style.FlexDirection;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public final class QuestBlueprintTypes {
    // 多行文本
    public static final TypeHandle STRING_ARRAY = TypeHandleHelpers.fromType(String[].class, "String[]");
    // 任务提交模式枚举
    public static final TypeHandle SUBMIT_MODE = TypeHandleHelpers.fromType(QuestSubmitMode.class);
    // 比较运算符枚举
    public static final TypeHandle COMPARE_OP = TypeHandleHelpers.fromType(CompareOp.class);
    // 汇合模式枚举，表达任选、全做、至少完成 N 个
    public static final TypeHandle JOIN_MODE = TypeHandleHelpers.fromType(QuestJoinMode.class);
    // 任意类型，用于通用比较节点的输入端口，可接受所有类型的连线
    public static final TypeHandle OBJECT = TypeHandleHelpers.fromType(Object.class, "Object");
    // 显示图标，支持物品图标和资源包图片两种模式
    public static final TypeHandle DISPLAY_ICON = TypeHandleHelpers.fromType(DisplayIcon.class, "DisplayIcon");

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
}
