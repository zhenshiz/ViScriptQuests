package com.viscriptquests.gui.blueprint.node.math;

import com.lowdragmc.lowdraglib2.nodegraphtookit.api.node.NodeAttribute;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.definition.IPortDefinitionContext;
import com.viscriptquests.gui.blueprint.QuestBlueprintGraph;
import com.viscriptquests.gui.blueprint.node.QuestBlueprintNode;
import net.minecraft.network.chat.Component;

// 限制范围节点：将值限制在 [min, max] 之间
@NodeAttribute(name = "math_clamp", group = QuestBlueprintNode.MATH_GROUP, graphTypes = QuestBlueprintGraph.class)
public class MathClampNode extends QuestBlueprintNode {
    @Override
    public Component getDisplayName() {
        return nodeName("math_clamp");
    }

    @Override
    public void onDefinePorts(IPortDefinitionContext context) {
        floatInput(context, "value", 0f);
        floatInput(context, "min", 0f);
        floatInput(context, "max", 1f);
        floatOutput(context, "result");
    }
}
