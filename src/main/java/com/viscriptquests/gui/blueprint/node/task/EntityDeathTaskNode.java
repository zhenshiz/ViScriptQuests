package com.viscriptquests.gui.blueprint.node.task;

import com.lowdragmc.lowdraglib2.nodegraphtookit.api.node.NodeAttribute;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.definition.IOptionDefinitionContext;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.definition.IPortDefinitionContext;
import com.viscriptquests.gui.blueprint.QuestBlueprintGraph;
import com.viscriptquests.gui.blueprint.QuestBlueprintTypes;
import com.viscriptquests.gui.blueprint.node.QuestBlueprintNode;
import com.viscriptquests.quest.data.TaskObjectiveType;
import net.minecraft.network.chat.Component;

// 实体死亡目标节点，不要求死亡来源是玩家，默认作为失败条件使用。
@NodeAttribute(name = QuestBlueprintNode.ID + "entity_death_task", group = QuestBlueprintNode.TASK_GROUP, graphTypes = QuestBlueprintGraph.class)
public class EntityDeathTaskNode extends QuestBlueprintNode {
    @Override
    public Component getDisplayName() {
        return nodeName("entity_death_task");
    }

    @Override
    public void onDefineOptions(IOptionDefinitionContext context) {
        entityTypeOption(context, "entity_type", "minecraft:villager");
        stringOption(context, "tag", "");
        enumOption(context, "objective_type", QuestBlueprintTypes.OBJECTIVE_TYPE, TaskObjectiveType.FAILURE);
        taskHintOption(context);
    }

    @Override
    public void onDefinePorts(IPortDefinitionContext context) {
        taskFlowPorts(context);
        intInput(context, "death_count", 1);
    }
}
