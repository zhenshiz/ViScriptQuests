package com.viscriptquests.gui.blueprint.compiler.task;

import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.CustomNodeModelImpl;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegister;
import com.viscriptquests.gui.blueprint.compiler.IQuestTaskNodeCompiler;
import com.viscriptquests.gui.blueprint.compiler.QuestCompileContext;
import com.viscriptquests.gui.blueprint.node.task.CustomTriggerTaskNode;
import com.viscriptquests.quest.data.task.CustomTriggerTask;
import com.viscriptquests.quest.data.task.ITask;

@LDLRegister(name = "custom_trigger_task", registry = IQuestTaskNodeCompiler.ID)
public class CustomTriggerTaskNodeCompiler implements IQuestTaskNodeCompiler {
    @Override
    public boolean supports(CustomNodeModelImpl node) {
        return node.getNode() instanceof CustomTriggerTaskNode;
    }

    @Override
    public ITask compileTask(QuestCompileContext context, CustomNodeModelImpl node, String stepId) {
        CustomTriggerTask task = new CustomTriggerTask();
        task.stepId = stepId;
        task.triggerId = context.getString(node, "trigger_id");
        return task;
    }
}
