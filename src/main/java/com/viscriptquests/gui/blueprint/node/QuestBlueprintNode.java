package com.viscriptquests.gui.blueprint.node;

import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.data.TextWrap;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.gui.ui.utils.UIElementProvider;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.node.IOptionBuilder;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.node.Node;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.node.NodeAttribute;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.port.IPortBuilder;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.type.TypeHandle;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.type.TypeHandles;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.NodeModel;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.NodeOption;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.definition.IOptionDefinitionContext;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.definition.IPortDefinitionContext;
import com.viscriptquests.ViScriptQuests;
import com.viscriptquests.compat.team.QuestTeamService;
import com.viscriptquests.gui.blueprint.QuestBlueprintTypes;
import com.viscriptquests.gui.blueprint.data.QuestRegistryId;
import com.viscriptquests.quest.data.DisplayIcon;
import com.viscriptquests.quest.data.ItemMatchRule;
import com.viscriptquests.quest.data.TaskObjectiveType;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

// 任务蓝图节点的公共基类，提供端口/选项的便捷方法和翻译键支持
public abstract class QuestBlueprintNode extends Node {
    public static final String ID = ViScriptQuests.MOD_ID + ":";
    public static final String SHOW_IN_OBJECTIVE_LIST_OPTION = "show_in_objective_list";
    public static final String OBJECTIVE_ICON_OPTION = "objective_icon";
    public static final String TASK_HINT_OPTION = "task_hint";
    public static final String SHOW_IN_REWARD_LIST_OPTION = "show_in_reward_list";
    public static final String REWARD_ICON_OPTION = "reward_icon";
    public static final String REWARD_TOOLTIP_OPTION = "reward_tooltip";

    public static final String DEBUG_GROUP = "debug";
    public static final String FLOW_GROUP = "flow";
    public static final String LOGIC_GROUP = "logic";
    public static final String MATH_GROUP = "math";
    public static final String PLAYER_GROUP = "player";
    public static final String REWARD_GROUP = "reward";
    public static final String SCOREBOARD_GROUP = "scoreboard";
    public static final String TASK_GROUP = "task";
    public static final String VARIABLE_GROUP = "variable";

    @Override
    public UIElement createDescriptionUI() {
        NodeAttribute attribute = getClass().getAnnotation(NodeAttribute.class);
        if (attribute == null) {
            return null;
        }

        String nodePath = pathOf(attribute.name());
        String nodeKey = ViScriptQuests.MOD_ID + ".blueprint.node." + nodePath;
        var container = new UIElement();
        container.layout(layout -> layout.widthPercent(100).gapAll(3));

        UIElement title = UIElementProvider.iconText(Node::getNodeIcon, Node::getDisplayName).apply(this);
        title.layout(layout -> layout.widthPercent(100));
        container.addChild(title);

        if (!attribute.group().isBlank()) {
            container.addChild(descriptionLabel(Component.translatable(
                    ViScriptQuests.MOD_ID + ".blueprint.description.category",
                    Component.translatable(ViScriptQuests.MOD_ID + ".blueprint.category." + attribute.group())
            ), 5));
        }

        container.addChildren(
                descriptionLabel(Component.translatable(
                        ViScriptQuests.MOD_ID + ".blueprint.description.section.description"
                ).withStyle(ChatFormatting.BOLD), 6),
                descriptionLabel(Component.translatable(nodeKey + ".description"), 6),
                descriptionLabel(Component.translatable(
                        ViScriptQuests.MOD_ID + ".blueprint.description.section.usage"
                ).withStyle(ChatFormatting.BOLD), 6),
                descriptionLabel(Component.translatable(nodeKey + ".usage"), 6)
        );
        return container;
    }

    private static Label descriptionLabel(Component text, float fontSize) {
        Label label = new Label();
        label.setText(text);
        label.layout(layout -> layout.widthPercent(100));
        label.textStyle(style -> style
                .fontSize(fontSize)
                .textWrap(TextWrap.WRAP)
                .adaptiveHeight(true));
        return label;
    }

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

    protected void taskHintOption(IOptionDefinitionContext context) {
        boolOption(context, SHOW_IN_OBJECTIVE_LIST_OPTION, true);
        if (getBoolOptionValue(SHOW_IN_OBJECTIVE_LIST_OPTION, true)) {
            displayIconOption(context, OBJECTIVE_ICON_OPTION);
            stringOption(context, TASK_HINT_OPTION, "");
        }
    }

    protected void taskCommonOptions(IOptionDefinitionContext context) {
        enumOption(context, "objective_type", QuestBlueprintTypes.OBJECTIVE_TYPE, TaskObjectiveType.REQUIRED);
        taskHintOption(context);
    }

    protected void taskFlowPorts(IPortDefinitionContext context) {
        inputFlow(context);
        outputFlow(context, "next");
    }

    protected void rewardCommonOptions(IOptionDefinitionContext context) {
        boolOption(context, SHOW_IN_REWARD_LIST_OPTION, true);
        if (getBoolOptionValue(SHOW_IN_REWARD_LIST_OPTION, true)) {
            displayIconOption(context, REWARD_ICON_OPTION);
            stringOption(context, REWARD_TOOLTIP_OPTION, "");
        }
        if (QuestTeamService.isLoaded()) {
            boolOption(context, "team_leader_only", false);
        }
    }

    protected void rewardFlowPorts(IPortDefinitionContext context) {
        inputFlow(context);
        outputFlow(context, "next");
    }

    protected void dimensionOption(IOptionDefinitionContext context, String id, String defaultValue) {
        registryIdOption(context, id, id, QuestBlueprintTypes.DIMENSION_ID, defaultValue);
    }

    protected void entityTypeOption(IOptionDefinitionContext context, String id, String defaultValue) {
        registryIdOption(context, id, id, QuestBlueprintTypes.ENTITY_TYPE_ID, defaultValue);
    }

    protected void anyEntityTypeOption(IOptionDefinitionContext context, String id, String defaultValue) {
        registryIdOption(context, id, id, QuestBlueprintTypes.ANY_ENTITY_TYPE_ID, defaultValue);
    }

    protected void advancementOption(IOptionDefinitionContext context, String id, String defaultValue) {
        registryIdOption(context, id, id, QuestBlueprintTypes.ADVANCEMENT_ID, defaultValue);
    }

    protected void ponderComponentOption(IOptionDefinitionContext context, String id, String defaultValue) {
        registryIdOption(context, id, id, QuestBlueprintTypes.PONDER_COMPONENT_ID, defaultValue);
    }

    protected void biomeOption(IOptionDefinitionContext context, String id, String defaultValue) {
        registryIdOption(context, id, id, QuestBlueprintTypes.BIOME_ID, defaultValue);
    }

    protected void structureOption(IOptionDefinitionContext context, String id, String defaultValue) {
        registryIdOption(context, id, id, QuestBlueprintTypes.STRUCTURE_ID, defaultValue);
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

    protected void doubleOption(IOptionDefinitionContext context, String id, double defaultValue) {
        option(context, id, TypeHandles.DOUBLE, defaultValue);
    }

    protected void boolOption(IOptionDefinitionContext context, String id, boolean defaultValue) {
        option(context, id, TypeHandles.BOOL, defaultValue);
    }

    private boolean getBoolOptionValue(String id, boolean fallback) {
        Object value = getOptionValue(id);
        return value instanceof Boolean booleanValue ? booleanValue : fallback;
    }

    protected Object getOptionValue(String id) {
        if (!(getNodeModel() instanceof NodeModel model)) {
            return null;
        }
        var constant = model.getInputConstantsById().get(NodeOption.PORT_ID_PREFIX + id);
        return constant == null ? null : constant.getValue();
    }

    // 动态表单暂时省略这些选项时保留其值，重新显示后继续使用作者原先填写的内容。
    public boolean retainsOptionValue(String optionId) {
        return TASK_HINT_OPTION.equals(optionId)
                || OBJECTIVE_ICON_OPTION.equals(optionId)
                || REWARD_ICON_OPTION.equals(optionId)
                || REWARD_TOOLTIP_OPTION.equals(optionId);
    }

    protected void itemStackOption(IOptionDefinitionContext context, String id) {
        option(context, id, QuestBlueprintTypes.ITEM_IDENTITY_STACK, ItemStack.EMPTY);
    }

    protected void itemMatchRuleOption(IOptionDefinitionContext context, String id) {
        option(context, id, QuestBlueprintTypes.ITEM_MATCH_RULE, new ItemMatchRule());
    }

    protected void blockOption(IOptionDefinitionContext context, String id, Block defaultValue) {
        option(context, id, TypeHandles.BLOCK, defaultValue);
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

    protected void intInput(IPortDefinitionContext context, String id, int defaultValue) {
        input(context, id, TypeHandles.INT, defaultValue);
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

    protected void intOutput(IPortDefinitionContext context, String id) {
        context.addOutputPort(id, TypeHandles.INT)
                .withDisplayName(portName(id))
                .build();
    }

    protected void option(IOptionDefinitionContext context, String id, TypeHandle type, Object defaultValue) {
        option(context, id, id, type, defaultValue);
    }

    protected void option(IOptionDefinitionContext context, String id, String displayKey, TypeHandle type, Object defaultValue) {
        IOptionBuilder<?> builder = context.addOption(id, type)
                .withDisplayName(portName(displayKey))
                .withDefaultValue(defaultValue);
        builder.build();
    }

    private void registryIdOption(IOptionDefinitionContext context, String id, String displayKey, TypeHandle type, String defaultValue) {
        option(context, id, displayKey, type, new QuestRegistryId(defaultValue));
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
        return Component.translatable(ViScriptQuests.MOD_ID + ".blueprint.node." + key);
    }

    protected Component portName(String key) {
        return Component.translatable(ViScriptQuests.MOD_ID + ".blueprint.port." + key);
    }

    public static String pathOf(String nodeId) {
        if (nodeId == null) {
            return "";
        }
        int namespaceIndex = nodeId.indexOf(':');
        return namespaceIndex >= 0 ? nodeId.substring(namespaceIndex + 1) : nodeId;
    }

}
