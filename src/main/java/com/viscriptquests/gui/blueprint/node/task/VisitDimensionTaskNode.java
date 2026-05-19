package com.viscriptquests.gui.blueprint.node.task;

import com.lowdragmc.lowdraglib2.nodegraphtookit.api.node.NodeAttribute;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.definition.IOptionDefinitionContext;
import com.viscriptquests.gui.blueprint.QuestBlueprintGraph;
import com.viscriptquests.gui.blueprint.node.QuestBlueprintNode;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;

// 访问维度目标节点，维度参数复用项目里的资源 ID 补全输入框。
@NodeAttribute(name = "visit_dimension_task", group = QuestBlueprintNode.TASK_GROUP, graphTypes = QuestBlueprintGraph.class)
public class VisitDimensionTaskNode extends QuestBlueprintNode {
    @Override
    public Component getDisplayName() {
        return nodeName("visit_dimension_task");
    }

    @Override
    public void onDefineOptions(IOptionDefinitionContext context) {
        dimensionOption(context, "dimension", Level.OVERWORLD.location().toString());
        taskHintOption(context);
    }
}
