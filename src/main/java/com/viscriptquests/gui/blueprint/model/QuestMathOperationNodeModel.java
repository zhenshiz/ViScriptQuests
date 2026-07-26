package com.viscriptquests.gui.blueprint.model;

import com.lowdragmc.lowdraglib2.nodegraphtookit.model.constant.Constant;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.NodeOption;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.PortModel;
import com.viscriptquests.gui.blueprint.data.MathOperation;
import com.viscriptquests.gui.blueprint.node.math.MathOperationNode;

public class QuestMathOperationNodeModel extends QuestBlueprintNodeModel {
    private boolean growingInputs;

    @Override
    public void onConnection(PortModel selfConnectedPortModel, PortModel otherConnectedPortModel) {
        super.onConnection(selfConnectedPortModel, otherConnectedPortModel);
        tryGrowVariadicInputPorts(selfConnectedPortModel);
    }

    private void tryGrowVariadicInputPorts(PortModel connectedPort) {
        if (growingInputs || !(getNode() instanceof MathOperationNode)) {
            return;
        }
        if (connectedPort == null || !MathOperationNode.isIndexedValueInput(connectedPort.getPortId())) {
            return;
        }

        MathOperation operation = MathOperationNode.operationOf(this);
        if (!operation.usesVariadicInputs()) {
            return;
        }

        int inputCount = MathOperationNode.inputCountOf(this);
        if (inputCount >= MathOperationNode.MAX_INPUT_COUNT || !allCurrentInputsConnected(inputCount, connectedPort)) {
            return;
        }

        Constant constant = getInputConstantsById().get(NodeOption.PORT_ID_PREFIX + MathOperationNode.INPUT_COUNT_OPTION);
        if (constant == null) {
            return;
        }

        // LDLib2 的端口由选项重定义，这里只在输入接满时递增数量，断开线时不自动缩回，避免误删作者已经布好的连线。
        growingInputs = true;
        try {
            constant.setValue(inputCount + 1);
            defineNode();
        } finally {
            growingInputs = false;
        }
    }

    private boolean allCurrentInputsConnected(int inputCount, PortModel justConnectedPort) {
        for (int i = 1; i <= inputCount; i++) {
            PortModel input = getInputsById().get(MathOperationNode.inputId(i));
            if (input == null) {
                return false;
            }
            if (input == justConnectedPort) {
                continue;
            }
            if (input.getConnectedPorts().isEmpty()) {
                return false;
            }
        }
        return true;
    }
}
