package com.viscriptquests.gui.blueprint.compiler.task;

import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.CustomNodeModelImpl;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegister;
import com.viscriptquests.gui.blueprint.compiler.IQuestTaskNodeCompiler;
import com.viscriptquests.gui.blueprint.compiler.QuestCompileContext;
import com.viscriptquests.gui.blueprint.node.task.VisitDimensionTaskNode;
import com.viscriptquests.quest.data.task.ITask;
import com.viscriptquests.quest.data.task.VisitDimensionTask;

@LDLRegister(name = "visit_dimension_task", registry = IQuestTaskNodeCompiler.ID)
public class VisitDimensionTaskNodeCompiler implements IQuestTaskNodeCompiler {
    @Override
    public boolean supports(CustomNodeModelImpl node) {
        return node.getNode() instanceof VisitDimensionTaskNode;
    }

    @Override
    public ITask compileTask(QuestCompileContext context, CustomNodeModelImpl node, String stepId) {
        VisitDimensionTask task = new VisitDimensionTask();
        task.stepId = stepId;
        task.dimension = context.getString(node, "dimension");
        return task;
    }
}
