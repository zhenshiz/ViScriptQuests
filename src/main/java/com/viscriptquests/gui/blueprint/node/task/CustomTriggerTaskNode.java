package com.viscriptquests.gui.blueprint.node.task;

import com.lowdragmc.lowdraglib2.nodegraphtookit.api.node.NodeAttribute;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.definition.IOptionDefinitionContext;
import com.viscriptquests.gui.blueprint.QuestBlueprintGraph;
import com.viscriptquests.gui.blueprint.node.QuestBlueprintNode;
import net.minecraft.network.chat.Component;

// 自定义触发目标节点，给开发者预留一个可由指令/API 完成的业务标识。
@NodeAttribute(name = "custom_trigger_task", group = QuestBlueprintNode.TASK_GROUP, graphTypes = QuestBlueprintGraph.class)
public class CustomTriggerTaskNode extends QuestBlueprintNode {
    @Override
    public Component getDisplayName() {
        return nodeName("custom_trigger_task");
    }

    @Override
    public void onDefineOptions(IOptionDefinitionContext context) {
        stringOption(context, "trigger_id", "viscript_quests:custom_trigger");
        taskHintOption(context);
    }
}
