package com.viscriptquests.gui.blueprint.node.task;

import com.lowdragmc.lowdraglib2.nodegraphtookit.api.node.NodeAttribute;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.definition.IOptionDefinitionContext;
import com.viscriptquests.gui.blueprint.QuestBlueprintGraph;
import com.viscriptquests.gui.blueprint.node.QuestBlueprintNode;
import net.minecraft.network.chat.Component;

// 位置目标节点，用于生成“到达指定位置”的任务和 HUD 导航标记。
@NodeAttribute(name = "location_task", group = QuestBlueprintNode.TASK_GROUP, graphTypes = QuestBlueprintGraph.class)
public class LocationTaskNode extends QuestBlueprintNode {
    @Override
    public Component getDisplayName() {
        return nodeName("location_task");
    }

    @Override
    public void onDefineOptions(IOptionDefinitionContext context) {
        dimensionOption(context, "dimension", "minecraft:overworld");
        floatOption(context, "x", 0.0f);
        floatOption(context, "y", 64.0f);
        floatOption(context, "z", 0.0f);
        floatOption(context, "arrival_radius", 3.0f);
        taskHintOption(context);
        stringOption(context, "marker_label", "");
        displayIconOption(context, "marker_icon");
        colorOption(context, "marker_color", 0xFFD8C7FF);
    }
}
