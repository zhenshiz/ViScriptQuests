package com.viscriptquests.gui.blueprint.compiler.task;

import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.CustomNodeModelImpl;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegister;
import com.viscriptquests.gui.blueprint.compiler.IQuestTaskNodeCompiler;
import com.viscriptquests.gui.blueprint.compiler.QuestCompileContext;
import com.viscriptquests.gui.blueprint.node.task.AdvancementTaskNode;
import com.viscriptquests.quest.data.task.AdvancementTask;
import com.viscriptquests.quest.data.task.ITask;

@LDLRegister(name = "advancement_task", registry = IQuestTaskNodeCompiler.ID)
public class AdvancementTaskNodeCompiler implements IQuestTaskNodeCompiler {
    @Override
    public boolean supports(CustomNodeModelImpl node) {
        return node.getNode() instanceof AdvancementTaskNode;
    }

    @Override
    public ITask compileTask(QuestCompileContext context, CustomNodeModelImpl node, String stepId) {
        AdvancementTask task = new AdvancementTask();
        task.stepId = stepId;
        task.advancementId = context.getString(node, "advancement_id");
        return task;
    }
}
