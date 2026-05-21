package com.viscriptquests.gui.blueprint.node.variable;

import com.lowdragmc.lowdraglib2.nodegraphtookit.api.node.NodeAttribute;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.definition.IOptionDefinitionContext;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.definition.IPortDefinitionContext;
import com.viscriptquests.gui.blueprint.QuestBlueprintGraph;
import com.viscriptquests.gui.blueprint.node.QuestBlueprintNode;
import net.minecraft.network.chat.Component;

// 将数值表达式写回任务变量的流程节点，语义为“变量名 = 值”。
@NodeAttribute(name = "set_variable", group = QuestBlueprintNode.VARIABLE_GROUP, graphTypes = QuestBlueprintGraph.class)
public class SetVariableNode extends QuestBlueprintNode {
    @Override
    public Component getDisplayName() {
        return nodeName("set_variable");
    }

    @Override
    public void onDefineOptions(IOptionDefinitionContext context) {
        stringOption(context, "variable_name", "");
    }

    @Override
    public void onDefinePorts(IPortDefinitionContext context) {
        inputFlow(context);
        floatInput(context, "value", 0f);
        floatOutput(context, "result");
        outputFlow(context, "next");
    }
}
