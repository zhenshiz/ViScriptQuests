package com.viscriptquests.gui.blueprint.compiler.task;

import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.CustomNodeModelImpl;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegister;
import com.viscriptquests.gui.blueprint.compiler.IQuestTaskNodeCompiler;
import com.viscriptquests.gui.blueprint.compiler.QuestCompileContext;
import com.viscriptquests.gui.blueprint.node.task.InteractEntityTaskNode;
import com.viscriptquests.quest.data.task.ITask;
import com.viscriptquests.quest.data.task.InteractEntityTask;

@LDLRegister(name = "interact_entity_task", registry = IQuestTaskNodeCompiler.ID)
public class InteractEntityTaskNodeCompiler implements IQuestTaskNodeCompiler {
    @Override
    public boolean supports(CustomNodeModelImpl node) {
        return node.getNode() instanceof InteractEntityTaskNode;
    }

    @Override
    public ITask compileTask(QuestCompileContext context, CustomNodeModelImpl node, String stepId) {
        InteractEntityTask task = new InteractEntityTask();
        task.stepId = stepId;
        task.entityType = context.getString(node, "entity_type");
        task.tag = context.getString(node, "tag");
        return task;
    }
}
