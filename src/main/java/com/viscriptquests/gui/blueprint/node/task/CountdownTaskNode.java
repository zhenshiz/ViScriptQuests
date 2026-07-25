package com.viscriptquests.gui.blueprint.node.task;

import com.lowdragmc.lowdraglib2.nodegraphtookit.api.node.NodeAttribute;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.definition.IOptionDefinitionContext;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.definition.IPortDefinitionContext;
import com.viscriptquests.gui.blueprint.QuestBlueprintGraph;
import com.viscriptquests.gui.blueprint.QuestBlueprintTypes;
import com.viscriptquests.gui.blueprint.node.QuestBlueprintNode;
import com.viscriptquests.quest.data.TaskObjectiveType;
import net.minecraft.network.chat.Component;

// 倒计时目标节点，默认用于“超时失败”这类小任务限制。
@NodeAttribute(name = QuestBlueprintNode.ID + "countdown_task", group = QuestBlueprintNode.TASK_GROUP, graphTypes = QuestBlueprintGraph.class)
public class CountdownTaskNode extends QuestBlueprintNode {
    @Override
    public Component getDisplayName() {
        return nodeName("countdown_task");
    }

    @Override
    public void onDefineOptions(IOptionDefinitionContext context) {
        enumOption(context, "objective_type", QuestBlueprintTypes.OBJECTIVE_TYPE, TaskObjectiveType.FAILURE);
        taskHintOption(context);
    }

    @Override
    public void onDefinePorts(IPortDefinitionContext context) {
        taskFlowPorts(context);
        intInput(context, "duration_seconds", 60);
    }
}
