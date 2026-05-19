package com.viscriptquests.gui.blueprint.node.task;

import com.lowdragmc.lowdraglib2.nodegraphtookit.api.node.NodeAttribute;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.definition.IOptionDefinitionContext;
import com.viscriptquests.gui.blueprint.QuestBlueprintGraph;
import com.viscriptquests.gui.blueprint.node.QuestBlueprintNode;
import net.minecraft.network.chat.Component;

// 进度目标节点，用于选择玩家需要完成的 Minecraft Advancement。
@NodeAttribute(name = "advancement_task", group = QuestBlueprintNode.TASK_GROUP, graphTypes = QuestBlueprintGraph.class)
public class AdvancementTaskNode extends QuestBlueprintNode {
    @Override
    public Component getDisplayName() {
        return nodeName("advancement_task");
    }

    @Override
    public void onDefineOptions(IOptionDefinitionContext context) {
        advancementOption(context, "advancement_id", "minecraft:story/root");
        taskHintOption(context);
    }
}
