package com.viscriptquests.gui.blueprint.node.task;

import com.lowdragmc.lowdraglib2.nodegraphtookit.api.node.NodeAttribute;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.definition.IOptionDefinitionContext;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.definition.IPortDefinitionContext;
import com.viscriptquests.gui.blueprint.QuestBlueprintGraph;
import com.viscriptquests.gui.blueprint.QuestBlueprintTypes;
import com.viscriptquests.gui.blueprint.node.QuestBlueprintNode;
import com.viscriptquests.quest.data.QuestSubmitMode;
import net.minecraft.network.chat.Component;

// 物品目标配置节点，纯数据节点，参数与 ItemTask 数据类一一对应
@NodeAttribute(name = QuestBlueprintNode.ID + "item_task", group = QuestBlueprintNode.TASK_GROUP, graphTypes = QuestBlueprintGraph.class)
public class ItemTaskNode extends QuestBlueprintNode {
    @Override
    public Component getDisplayName() {
        return nodeName("item_task");
    }

    @Override
    public void onDefineOptions(IOptionDefinitionContext context) {
        itemStackOption(context, "item_stack");
        boolOption(context, "strict_components", false);
        boolOption(context, "consume_item", true);
        enumOption(context, "submit_mode", QuestBlueprintTypes.SUBMIT_MODE, QuestSubmitMode.AUTO);
        taskCommonOptions(context);
    }

    @Override
    public void onDefinePorts(IPortDefinitionContext context) {
        taskFlowPorts(context);
        intInput(context, "item_count", 1);
    }
}
