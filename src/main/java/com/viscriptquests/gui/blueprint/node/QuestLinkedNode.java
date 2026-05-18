package com.viscriptquests.gui.blueprint.node;

import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.definition.IOptionDefinitionContext;

// 带小任务 ID 的流程节点基类；目标和奖励通过小任务子图自动归属，不再继承这里。
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
