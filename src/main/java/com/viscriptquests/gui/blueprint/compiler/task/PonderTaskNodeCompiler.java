package com.viscriptquests.gui.blueprint.compiler.task;

import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.CustomNodeModelImpl;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegister;
import com.viscriptquests.gui.blueprint.compiler.IQuestTaskNodeCompiler;
import com.viscriptquests.gui.blueprint.compiler.QuestCompileContext;
import com.viscriptquests.gui.blueprint.node.task.PonderTaskNode;
import com.viscriptquests.quest.data.task.ITask;
import com.viscriptquests.quest.data.task.PonderTask;

@LDLRegister(name = "ponder_task", registry = IQuestTaskNodeCompiler.ID)
public class PonderTaskNodeCompiler implements IQuestTaskNodeCompiler {
    @Override
    public boolean supports(CustomNodeModelImpl node) {
        return node.getNode() instanceof PonderTaskNode;
    }

    @Override
    public ITask compileTask(QuestCompileContext context, CustomNodeModelImpl node, String stepId) {
        PonderTask task = new PonderTask();
        task.stepId = stepId;
        task.ponderComponentId = context.getString(node, "ponder_component_id");
        return task;
    }
}
