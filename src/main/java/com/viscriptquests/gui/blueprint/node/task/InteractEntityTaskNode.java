package com.viscriptquests.gui.blueprint.node.task;

import com.lowdragmc.lowdraglib2.nodegraphtookit.api.node.NodeAttribute;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.definition.IOptionDefinitionContext;
import com.viscriptquests.gui.blueprint.QuestBlueprintGraph;
import com.viscriptquests.gui.blueprint.node.QuestBlueprintNode;
import net.minecraft.network.chat.Component;

// 实体交互目标节点，用于配置右键交互的实体类型和可选命令标签。
@NodeAttribute(name = "interact_entity_task", group = QuestBlueprintNode.TASK_GROUP, graphTypes = QuestBlueprintGraph.class)
public class InteractEntityTaskNode extends QuestBlueprintNode {
    @Override
    public Component getDisplayName() {
        return nodeName("interact_entity_task");
    }

    @Override
    public void onDefineOptions(IOptionDefinitionContext context) {
        anyEntityTypeOption(context, "entity_type", "minecraft:pig");
        stringOption(context, "tag", "");
        taskHintOption(context);
    }
}
