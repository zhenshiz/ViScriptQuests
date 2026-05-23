package com.viscriptquests.gui.blueprint.node.flow;

import com.lowdragmc.lowdraglib2.nodegraphtookit.api.node.NodeAttribute;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.definition.IOptionDefinitionContext;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.definition.IPortDefinitionContext;
import com.viscriptquests.gui.blueprint.QuestBlueprintGraph;
import com.viscriptquests.gui.blueprint.node.QuestLinkedNode;
import net.minecraft.network.chat.Component;

// 小任务节点，作为目标和奖励的分组容器
@NodeAttribute(name = "sub_quest", group = QuestLinkedNode.FLOW_GROUP, graphTypes = QuestBlueprintGraph.class)
public class SubQuestNode extends QuestLinkedNode {
    @Override
    public Component getDisplayName() {
        return nodeName("sub_quest");
    }

    @Override
    public void onDefineOptions(IOptionDefinitionContext context) {
        stringOption(context, "title", "");
        stringOption(context, "subtitle", "");
        stringArrayOption(context, "description");
    }

    @Override
    public void onDefinePorts(IPortDefinitionContext context) {
        inputFlow(context);
        outputFlow(context, "success");
        outputFlow(context, "failure");
    }
}
