package com.viscriptquests.gui.blueprint.node.flow;

import com.lowdragmc.lowdraglib2.nodegraphtookit.api.node.NodeAttribute;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.definition.IOptionDefinitionContext;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.definition.IPortDefinitionContext;
import com.viscriptquests.gui.blueprint.QuestBlueprintGraph;
import com.viscriptquests.gui.blueprint.node.QuestBlueprintNode;
import net.minecraft.network.chat.Component;

// 任务起点节点，定义任务的基本信息
@NodeAttribute(name = QuestBlueprintNode.ID + "quest_start", group = QuestBlueprintNode.FLOW_GROUP, graphTypes = QuestBlueprintGraph.class)
public class QuestStartNode extends QuestBlueprintNode {
    @Override
    public Component getDisplayName() {
        return nodeName("quest_start");
    }

    @Override
    public void onDefineOptions(IOptionDefinitionContext context) {
        stringOption(context, "title", "");
        stringOption(context, "subtitle", "");
        displayIconOption(context, "icon");
    }

    @Override
    public void onDefinePorts(IPortDefinitionContext context) {
        outputFlow(context, "next");
    }
}
