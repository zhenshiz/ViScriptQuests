package com.viscriptquests.gui.blueprint.node.flow;

import com.lowdragmc.lowdraglib2.nodegraphtookit.api.node.NodeAttribute;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.definition.IPortDefinitionContext;
import com.viscriptquests.gui.blueprint.QuestBlueprintGraph;
import com.viscriptquests.gui.blueprint.node.QuestBlueprintNode;
import net.minecraft.network.chat.Component;

// 小任务子图的入口：目标出口定义小任务目标，奖励出口定义小任务完成后展示和发放的固定奖励。
@NodeAttribute(name = QuestBlueprintNode.ID + "sub_quest_start", group = QuestBlueprintNode.FLOW_GROUP, graphTypes = QuestBlueprintGraph.class)
public class SubQuestStartNode extends QuestBlueprintNode {
    @Override
    public Component getDisplayName() {
        return nodeName("sub_quest_start");
    }

    @Override
    public void onDefinePorts(IPortDefinitionContext context) {
        outputFlow(context, "objectives");
        outputFlow(context, "rewards");
    }
}
