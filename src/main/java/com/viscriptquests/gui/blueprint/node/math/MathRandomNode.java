package com.viscriptquests.gui.blueprint.node.math;

import com.lowdragmc.lowdraglib2.nodegraphtookit.api.node.NodeAttribute;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.definition.IPortDefinitionContext;
import com.viscriptquests.gui.blueprint.QuestBlueprintGraph;
import com.viscriptquests.gui.blueprint.node.QuestBlueprintNode;
import net.minecraft.network.chat.Component;

// 随机数节点：在 [min, max] 范围内生成随机浮点数
@NodeAttribute(name = "math_random", group = QuestBlueprintNode.MATH_GROUP, graphTypes = QuestBlueprintGraph.class)
public class MathRandomNode extends QuestBlueprintNode {
    @Override
    public Component getDisplayName() {
        return nodeName("math_random");
    }

    @Override
    public void onDefinePorts(IPortDefinitionContext context) {
        floatInput(context, "min", 0f);
        floatInput(context, "max", 1f);
        floatOutput(context, "result");
    }
}
