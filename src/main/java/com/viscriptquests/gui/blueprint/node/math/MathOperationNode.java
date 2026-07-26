package com.viscriptquests.gui.blueprint.node.math;

import com.lowdragmc.lowdraglib2.nodegraphtookit.api.node.NodeAttribute;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.type.TypeHandles;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.constant.Constant;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.NodeModel;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.NodeOption;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.definition.IOptionDefinitionContext;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.definition.IPortDefinitionContext;
import com.viscriptquests.gui.blueprint.QuestBlueprintGraph;
import com.viscriptquests.gui.blueprint.QuestBlueprintTypes;
import com.viscriptquests.gui.blueprint.data.MathOperation;
import com.viscriptquests.gui.blueprint.node.QuestBlueprintNode;
import net.minecraft.network.chat.Component;

@NodeAttribute(name = QuestBlueprintNode.ID + "math_operation", group = QuestBlueprintNode.MATH_GROUP, graphTypes = QuestBlueprintGraph.class)
public class MathOperationNode extends QuestBlueprintNode {
    public static final String OPERATION_OPTION = "operation";
    public static final String INPUT_COUNT_OPTION = "input_count";
    public static final int MIN_INPUT_COUNT = 2;
    public static final int MAX_INPUT_COUNT = 64;

    @Override
    public Component getDisplayName() {
        return nodeName("math_operation");
    }

    @Override
    public void onDefineOptions(IOptionDefinitionContext context) {
        enumOption(context, OPERATION_OPTION, QuestBlueprintTypes.MATH_OPERATION, MathOperation.ADD);
        if (selectedOperation().usesVariadicInputs()) {
            intOption(context, INPUT_COUNT_OPTION, MIN_INPUT_COUNT);
        }
    }

    @Override
    public void onDefinePorts(IPortDefinitionContext context) {
        MathOperation operation = selectedOperation();
        if (operation.usesVariadicInputs()) {
            int inputCount = selectedInputCount();
            for (int i = 1; i <= inputCount; i++) {
                addIndexedValueInput(context, i, operation.defaultInputValue(i));
            }
        } else if (operation == MathOperation.CLAMP) {
            floatInput(context, "value", 0f);
            floatInput(context, "min", 0f);
            floatInput(context, "max", 1f);
        } else if (operation == MathOperation.RANDOM) {
            floatInput(context, "min", 0f);
            floatInput(context, "max", 1f);
        }
        floatOutput(context, "result");
    }

    public static String inputId(int index) {
        return "value_" + index;
    }

    public static boolean isIndexedValueInput(String portId) {
        if (portId == null || !portId.startsWith("value_")) {
            return false;
        }
        try {
            return Integer.parseInt(portId.substring("value_".length())) >= 1;
        } catch (NumberFormatException ignored) {
            return false;
        }
    }

    public static MathOperation operationOf(NodeModel nodeModel) {
        Constant constant = nodeModel.getInputConstantsById().get(NodeOption.PORT_ID_PREFIX + OPERATION_OPTION);
        return MathOperation.fromValue(constant == null ? null : constant.getValue());
    }

    public static int inputCountOf(NodeModel nodeModel) {
        Constant constant = nodeModel.getInputConstantsById().get(NodeOption.PORT_ID_PREFIX + INPUT_COUNT_OPTION);
        int inputCount = MIN_INPUT_COUNT;
        if (constant != null && constant.getValue() instanceof Number number) {
            inputCount = number.intValue();
        }
        return Math.max(MIN_INPUT_COUNT, Math.min(MAX_INPUT_COUNT, inputCount));
    }

    private MathOperation selectedOperation() {
        return MathOperation.fromValue(getOptionValue(OPERATION_OPTION));
    }

    private int selectedInputCount() {
        if (getNodeModel() instanceof NodeModel nodeModel) {
            return inputCountOf(nodeModel);
        }
        return MIN_INPUT_COUNT;
    }

    @Override
    public boolean retainsOptionValue(String optionId) {
        return super.retainsOptionValue(optionId) || INPUT_COUNT_OPTION.equals(optionId);
    }

    private void addIndexedValueInput(IPortDefinitionContext context, int index, float defaultValue) {
        var builder = context.addInputPort(inputId(index), TypeHandles.FLOAT)
                .withDisplayName(Component.translatable("viscript_quests.blueprint.port.value_index", index));
        builder.withDefaultValue(defaultValue);
        builder.build();
    }
}
