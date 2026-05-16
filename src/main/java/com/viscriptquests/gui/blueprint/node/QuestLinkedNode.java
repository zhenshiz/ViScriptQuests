package com.viscriptquests.gui.blueprint.node;

import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.definition.IOptionDefinitionContext;

// 绑定小任务 ID 的公共节点基类，后面如果改成子图自动注入，只需要改这里。
public abstract class QuestLinkedNode extends QuestBlueprintNode {
    public static final String STEP_ID_OPTION = "step_id";

    protected void stepIdOption(IOptionDefinitionContext context) {
        stepIdOption(context, stepIdDefaultValue());
    }

    protected void stepIdOption(IOptionDefinitionContext context, String defaultValue) {
        stringOption(context, STEP_ID_OPTION, "step_id", defaultValue);
    }

    protected String stepIdDefaultValue() {
        return "";
    }
}
