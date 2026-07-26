package com.viscriptquests.gui.blueprint.node.logic;

import com.lowdragmc.lowdraglib2.nodegraphtookit.api.node.NodeAttribute;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.constant.Constant;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.NodeModel;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.NodeOption;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.definition.IOptionDefinitionContext;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.definition.IPortDefinitionContext;
import com.viscriptquests.gui.blueprint.QuestBlueprintGraph;
import com.viscriptquests.gui.blueprint.QuestBlueprintTypes;
import com.viscriptquests.gui.blueprint.node.QuestBlueprintNode;
import com.viscriptquests.quest.data.CompareOp;
import net.minecraft.network.chat.Component;

@NodeAttribute(name = QuestBlueprintNode.ID + "compare", group = QuestBlueprintNode.LOGIC_GROUP, graphTypes = QuestBlueprintGraph.class)
public class CompareOperationNode extends QuestBlueprintNode {
    public static final String OPERATOR_OPTION = "operator";

    @Override
    public Component getDisplayName() {
        return nodeName("compare");
    }

    @Override
    public void onDefineOptions(IOptionDefinitionContext context) {
        enumOption(context, OPERATOR_OPTION, QuestBlueprintTypes.COMPARE_OP, CompareOp.EQ);
    }

    @Override
    public void onDefinePorts(IPortDefinitionContext context) {
        floatInput(context, "value_a", 0f);
        floatInput(context, "value_b", 0f);
        boolOutput(context, "result");
    }

    public static CompareOp operationOf(NodeModel nodeModel) {
        Constant constant = nodeModel.getInputConstantsById().get(NodeOption.PORT_ID_PREFIX + OPERATOR_OPTION);
        return fromValue(constant == null ? null : constant.getValue());
    }

    public static CompareOp fromValue(Object value) {
        return value instanceof CompareOp op ? op : CompareOp.EQ;
    }
}
