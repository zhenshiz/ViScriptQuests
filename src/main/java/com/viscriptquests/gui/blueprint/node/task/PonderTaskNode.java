package com.viscriptquests.gui.blueprint.node.task;

import com.lowdragmc.lowdraglib2.nodegraphtookit.api.node.NodeAttribute;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.definition.IOptionDefinitionContext;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.definition.IPortDefinitionContext;
import com.viscriptquests.gui.blueprint.QuestBlueprintGraph;
import com.viscriptquests.gui.blueprint.node.QuestBlueprintNode;
import net.minecraft.network.chat.Component;

// 思索教程目标，玩家在任务书中点击“查看”后打开 Ponder 并完成该目标。
@NodeAttribute(name = QuestBlueprintNode.ID + "ponder_task", group = QuestBlueprintNode.TASK_GROUP, graphTypes = QuestBlueprintGraph.class)
public class PonderTaskNode extends QuestBlueprintNode {
    @Override
    public Component getDisplayName() {
        return nodeName("ponder_task");
    }

    @Override
    public void onDefineOptions(IOptionDefinitionContext context) {
        ponderComponentOption(context, "ponder_component_id", "minecraft:crafting_table");
        taskCommonOptions(context);
    }

    @Override
    public void onDefinePorts(IPortDefinitionContext context) {
        taskFlowPorts(context);
    }
}
