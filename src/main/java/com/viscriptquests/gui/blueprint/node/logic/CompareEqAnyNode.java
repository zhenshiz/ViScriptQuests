package com.viscriptquests.gui.blueprint.node.logic;

import com.lowdragmc.lowdraglib2.nodegraphtookit.api.node.NodeAttribute;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.definition.IPortDefinitionContext;
import com.viscriptquests.gui.blueprint.QuestBlueprintGraph;
import com.viscriptquests.gui.blueprint.node.QuestBlueprintNode;
import net.minecraft.network.chat.Component;

// 任意类型相等比较节点，使用 Java Object.equals() 进行判断
// 端口类型为 Object，可接受 INT、FLOAT、STRING、ItemStack 等任意类型的连线
@NodeAttribute(name = "compare_eq_any", group = QuestBlueprintNode.LOGIC_GROUP, graphTypes = QuestBlueprintGraph.class)
public class CompareEqAnyNode extends QuestBlueprintNode {
    @Override
    public Component getDisplayName() {
        return nodeName("compare_eq_any");
    }

    @Override
    public void onDefinePorts(IPortDefinitionContext context) {
        objectInput(context, "value_a");
        objectInput(context, "value_b");
        boolOutput(context, "result");
    }
}
