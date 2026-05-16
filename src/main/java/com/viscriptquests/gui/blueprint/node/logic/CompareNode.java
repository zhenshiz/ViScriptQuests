package com.viscriptquests.gui.blueprint.node.logic;

import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.definition.IPortDefinitionContext;
import com.viscriptquests.gui.blueprint.node.QuestBlueprintNode;

// 数值比较节点基类，端口使用 FLOAT 类型，运行时将输入转为 double 进行比较
// 图模型层支持 INT → FLOAT 隐式转换，所以 INT 变量也能连线
public abstract class CompareNode extends QuestBlueprintNode {

    @Override
    public void onDefinePorts(IPortDefinitionContext context) {
        floatInput(context, "value_a", 0f);
        floatInput(context, "value_b", 0f);
        boolOutput(context, "result");
    }
}
