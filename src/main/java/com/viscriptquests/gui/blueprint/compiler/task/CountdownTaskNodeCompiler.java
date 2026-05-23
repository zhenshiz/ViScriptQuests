package com.viscriptquests.gui.blueprint.compiler.task;

import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.CustomNodeModelImpl;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegister;
import com.viscriptquests.gui.blueprint.compiler.IQuestTaskNodeCompiler;
import com.viscriptquests.gui.blueprint.compiler.QuestCompileContext;
import com.viscriptquests.gui.blueprint.node.task.CountdownTaskNode;
import com.viscriptquests.quest.data.task.CountdownTask;
import com.viscriptquests.quest.data.task.ITask;

@LDLRegister(name = "countdown_task", registry = IQuestTaskNodeCompiler.ID)
public class CountdownTaskNodeCompiler implements IQuestTaskNodeCompiler {
    @Override
    public boolean supports(CustomNodeModelImpl node) {
        return node.getNode() instanceof CountdownTaskNode;
    }

    @Override
    public ITask compileTask(QuestCompileContext context, CustomNodeModelImpl node, String stepId) {
        CountdownTask task = new CountdownTask();
        task.stepId = stepId;
        task.durationSeconds = Math.max(1, context.getInt(node, "duration_seconds"));
        return task;
    }
}
