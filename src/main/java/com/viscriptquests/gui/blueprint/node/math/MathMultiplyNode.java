package com.viscriptquests.gui.blueprint.node.math;

import com.lowdragmc.lowdraglib2.nodegraphtookit.api.node.NodeAttribute;
import com.viscriptquests.gui.blueprint.QuestBlueprintGraph;
import com.viscriptquests.gui.blueprint.node.QuestBlueprintNode;
import net.minecraft.network.chat.Component;

// 乘法节点：A × B
@NodeAttribute(name = "math_multiply", group = QuestBlueprintNode.MATH_GROUP, graphTypes = QuestBlueprintGraph.class)
public class MathMultiplyNode extends MathBinaryNode {
    @Override
    public Component getDisplayName() {
        return nodeName("math_multiply");
    }
}
