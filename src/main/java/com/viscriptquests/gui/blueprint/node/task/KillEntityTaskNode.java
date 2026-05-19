package com.viscriptquests.gui.blueprint.node.task;

import com.lowdragmc.lowdraglib2.nodegraphtookit.api.node.NodeAttribute;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.definition.IOptionDefinitionContext;
import com.viscriptquests.gui.blueprint.QuestBlueprintGraph;
import com.viscriptquests.gui.blueprint.node.QuestBlueprintNode;
import net.minecraft.network.chat.Component;

// 击杀实体目标节点，用于配置目标实体、累计击杀数量和可选的实体命令标签。
@NodeAttribute(name = "kill_entity_task", group = QuestBlueprintNode.TASK_GROUP, graphTypes = QuestBlueprintGraph.class)
public class KillEntityTaskNode extends QuestBlueprintNode {
    @Override
    public Component getDisplayName() {
        return nodeName("kill_entity_task");
    }

    @Override
    public void onDefineOptions(IOptionDefinitionContext context) {
        entityTypeOption(context, "entity_type", "minecraft:zombie");
        intOption(context, "kill_count", 1);
        stringOption(context, "tag", "");
    }
}
