package com.viscriptquests.gui.blueprint.node;

import com.lowdragmc.lowdraglib2.nodegraphtookit.api.node.IOptionBuilder;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.node.Node;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.port.IPortBuilder;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.type.TypeHandle;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.type.TypeHandles;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.definition.IOptionDefinitionContext;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.definition.IPortDefinitionContext;
import com.viscriptquests.gui.blueprint.QuestBlueprintTypes;
import com.viscriptquests.quest.data.DisplayIcon;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

// 任务蓝图节点的公共基类，提供端口/选项的便捷方法和翻译键支持
public abstract class QuestBlueprintNode extends Node {
    public static final String DEBUG_GROUP = "debug";
    public static final String FLOW_GROUP = "flow";
    public static final String LOGIC_GROUP = "logic";
    public static final String MATH_GROUP = "math";
    public static final String REWARD_GROUP = "reward";
    public static final String TASK_GROUP = "task";

    protected void inputFlow(IPortDefinitionContext context) {
        input(context, "in", TypeHandles.EXECUTION_FLOW, null);
    }

    protected void outputFlow(IPortDefinitionContext context, String id) {
        context.addOutputPort(id, TypeHandles.EXECUTION_FLOW)
                .withDisplayName(portName(id))
                .build();
    }

    protected void stringOption(IOptionDefinitionContext context, String id, String defaultValue) {
        option(context, id, TypeHandles.STRING, defaultValue);
    }

    protected void stringOption(IOptionDefinitionContext context, String id, String displayKey, String defaultValue) {
        option(context, id, displayKey, TypeHandles.STRING, defaultValue);
    }

    protected void stringArrayOption(IOptionDefinitionContext context, String id) {
        option(context, id, QuestBlueprintTypes.STRING_ARRAY, new String[0]);
    }

    protected void intOption(IOptionDefinitionContext context, String id, int defaultValue) {
        option(context, id, TypeHandles.INT, defaultValue);
    }

    protected void colorOption(IOptionDefinitionContext context, String id, int defaultValue) {
        option(context, id, TypeHandles.COLOR, defaultValue);
    }

    protected void floatOption(IOptionDefinitionContext context, String id, float defaultValue) {
        option(context, id, TypeHandles.FLOAT, defaultValue);
    }

    protected void boolOption(IOptionDefinitionContext context, String id, boolean defaultValue) {
        option(context, id, TypeHandles.BOOL, defaultValue);
    }

    protected void itemStackOption(IOptionDefinitionContext context, String id) {
        option(context, id, TypeHandles.ITEM_STACK, ItemStack.EMPTY);
    }

    protected void displayIconOption(IOptionDefinitionContext context, String id) {
        displayIconOption(context, id, new DisplayIcon());
    }

    protected void displayIconOption(IOptionDefinitionContext context, String id, DisplayIcon defaultValue) {
        option(context, id, QuestBlueprintTypes.DISPLAY_ICON, defaultValue == null ? new DisplayIcon() : defaultValue);
    }

    // 枚举类型选项，蓝图编辑器中自动生成下拉选择器
    protected <T extends Enum<T>> void enumOption(IOptionDefinitionContext context, String id, TypeHandle type, T defaultValue) {
        option(context, id, type, defaultValue);
    }

    protected void boolInput(IPortDefinitionContext context, String id, boolean defaultValue) {
        input(context, id, TypeHandles.BOOL, defaultValue);
    }

    protected void stringInput(IPortDefinitionContext context, String id, String defaultValue) {
        input(context, id, TypeHandles.STRING, defaultValue);
    }

    protected void floatInput(IPortDefinitionContext context, String id, float defaultValue) {
        input(context, id, TypeHandles.FLOAT, defaultValue);
    }

    // 任意类型输入端口，接受所有连线（用于通用比较节点）
    protected void objectInput(IPortDefinitionContext context, String id) {
        input(context, id, QuestBlueprintTypes.OBJECT, null);
    }

    protected void boolOutput(IPortDefinitionContext context, String id) {
        context.addOutputPort(id, TypeHandles.BOOL)
                .withDisplayName(portName(id))
                .build();
    }

    protected void floatOutput(IPortDefinitionContext context, String id) {
        context.addOutputPort(id, TypeHandles.FLOAT)
                .withDisplayName(portName(id))
                .build();
    }

    private void option(IOptionDefinitionContext context, String id, TypeHandle type, Object defaultValue) {
        option(context, id, id, type, defaultValue);
    }

    private void option(IOptionDefinitionContext context, String id, String displayKey, TypeHandle type, Object defaultValue) {
        IOptionBuilder<?> builder = context.addOption(id, type)
                .withDisplayName(portName(displayKey))
                .withDefaultValue(defaultValue);
        builder.build();
    }

    private void input(IPortDefinitionContext context, String id, TypeHandle type, Object defaultValue) {
        IPortBuilder<?> builder = context.addInputPort(id, type)
                .withDisplayName(portName(id));
        if (defaultValue != null) {
            builder.withDefaultValue(defaultValue);
        }
        builder.build();
    }

    protected Component nodeName(String key) {
        return Component.translatable("viscript_quests.blueprint.node." + key);
    }

    protected Component portName(String key) {
        return Component.translatable("viscript_quests.blueprint.port." + key);
    }
}
