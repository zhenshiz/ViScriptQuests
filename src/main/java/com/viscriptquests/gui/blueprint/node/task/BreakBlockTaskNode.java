package com.viscriptquests.gui.blueprint.node.task;

import com.lowdragmc.lowdraglib2.nodegraphtookit.api.node.NodeAttribute;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.definition.IOptionDefinitionContext;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.definition.IPortDefinitionContext;
import com.viscriptquests.gui.blueprint.QuestBlueprintGraph;
import com.viscriptquests.gui.blueprint.node.QuestBlueprintNode;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.Blocks;

// 破坏方块目标节点，方块参数使用 LDLib2 的 Block 专用选择器。
@NodeAttribute(name = QuestBlueprintNode.ID + "break_block_task", group = QuestBlueprintNode.TASK_GROUP, graphTypes = QuestBlueprintGraph.class)
public class BreakBlockTaskNode extends QuestBlueprintNode {
    @Override
    public Component getDisplayName() {
        return nodeName("break_block_task");
    }

    @Override
    public void onDefineOptions(IOptionDefinitionContext context) {
        blockOption(context, "block", Blocks.STONE);
        taskCommonOptions(context);
    }

    @Override
    public void onDefinePorts(IPortDefinitionContext context) {
        taskFlowPorts(context);
        intInput(context, "break_count", 1);
    }
}
