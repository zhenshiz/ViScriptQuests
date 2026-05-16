package com.viscriptquests.gui.blueprint.node.math;

import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.definition.IPortDefinitionContext;
import com.viscriptquests.gui.blueprint.node.QuestBlueprintNode;

// 二元数学运算节点基类：A op B → Result，所有端口使用 FLOAT 类型
public abstract class MathBinaryNode extends QuestBlueprintNode {
    @Override
    public void onDefinePorts(IPortDefinitionContext context) {
        floatInput(context, "value_a", 0f);
        floatInput(context, "value_b", 0f);
        floatOutput(context, "result");
    }
}
