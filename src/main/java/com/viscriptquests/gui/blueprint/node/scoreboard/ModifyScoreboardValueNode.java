package com.viscriptquests.gui.blueprint.node.scoreboard;

import com.lowdragmc.lowdraglib2.nodegraphtookit.api.node.NodeAttribute;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.constant.Constant;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.NodeModel;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.NodeOption;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.definition.IOptionDefinitionContext;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.definition.IPortDefinitionContext;
import com.viscriptquests.gui.blueprint.QuestBlueprintGraph;
import com.viscriptquests.gui.blueprint.QuestBlueprintTypes;
import com.viscriptquests.gui.blueprint.node.QuestBlueprintNode;
import com.viscriptquests.quest.data.VariableMutationOp;
import net.minecraft.network.chat.Component;

@NodeAttribute(name = "modify_scoreboard_value", group = QuestBlueprintNode.SCOREBOARD_GROUP, graphTypes = QuestBlueprintGraph.class)
public class ModifyScoreboardValueNode extends QuestBlueprintNode {
    public static final String OPERATION_OPTION = "operation";

    @Override
    public Component getDisplayName() {
        return nodeName("modify_scoreboard_value");
    }

    @Override
    public void onDefineOptions(IOptionDefinitionContext context) {
        stringOption(context, "objective_name", "");
        stringOption(context, "score_holder", "");
        enumOption(context, OPERATION_OPTION, QuestBlueprintTypes.VARIABLE_MUTATION_OP, VariableMutationOp.SET);
    }

    @Override
    public void onDefinePorts(IPortDefinitionContext context) {
        inputFlow(context);
        floatInput(context, "value", 0f);
        outputFlow(context, "next");
    }

    public static VariableMutationOp operationOf(NodeModel nodeModel) {
        Constant constant = nodeModel.getInputConstantsById().get(NodeOption.PORT_ID_PREFIX + OPERATION_OPTION);
        return VariableMutationOp.fromValue(constant == null ? null : constant.getValue());
    }
}
