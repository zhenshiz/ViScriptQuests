package com.viscriptquests.gui.blueprint.node.math;

import com.lowdragmc.lowdraglib2.nodegraphtookit.api.node.NodeAttribute;
import com.viscriptquests.gui.blueprint.QuestBlueprintGraph;
import com.viscriptquests.gui.blueprint.node.QuestBlueprintNode;
import net.minecraft.network.chat.Component;

// 减法节点：A - B
@NodeAttribute(name = "math_subtract", group = QuestBlueprintNode.MATH_GROUP, graphTypes = QuestBlueprintGraph.class)
public class MathSubtractNode extends MathBinaryNode {
    @Override
    public Component getDisplayName() {
        return nodeName("math_subtract");
    }
}
