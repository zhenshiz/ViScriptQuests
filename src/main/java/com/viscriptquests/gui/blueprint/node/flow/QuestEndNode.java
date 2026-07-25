package com.viscriptquests.gui.blueprint.node.flow;

import com.lowdragmc.lowdraglib2.nodegraphtookit.api.node.NodeAttribute;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.definition.IOptionDefinitionContext;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.definition.IPortDefinitionContext;
import com.viscriptquests.gui.blueprint.QuestBlueprintGraph;
import com.viscriptquests.gui.blueprint.node.QuestBlueprintNode;
import net.minecraft.network.chat.Component;

// 任务终点节点，标记任务的结束状态
@NodeAttribute(name = QuestBlueprintNode.ID + "quest_end", group = QuestBlueprintNode.FLOW_GROUP, graphTypes = QuestBlueprintGraph.class)
public class QuestEndNode extends QuestBlueprintNode {
    @Override
    public Component getDisplayName() {
        return nodeName("quest_end");
    }

    @Override
    public void onDefineOptions(IOptionDefinitionContext context) {
        boolOption(context, "success", true);
    }

    @Override
    public void onDefinePorts(IPortDefinitionContext context) {
        inputFlow(context);
    }
}
