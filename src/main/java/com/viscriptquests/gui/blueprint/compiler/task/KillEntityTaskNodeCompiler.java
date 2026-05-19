package com.viscriptquests.gui.blueprint.compiler.task;

import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.CustomNodeModelImpl;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegister;
import com.viscriptquests.gui.blueprint.compiler.IQuestTaskNodeCompiler;
import com.viscriptquests.gui.blueprint.compiler.QuestCompileContext;
import com.viscriptquests.gui.blueprint.node.task.KillEntityTaskNode;
import com.viscriptquests.quest.data.task.ITask;
import com.viscriptquests.quest.data.task.KillEntityTask;

@LDLRegister(name = "kill_entity_task", registry = IQuestTaskNodeCompiler.ID)
public class KillEntityTaskNodeCompiler implements IQuestTaskNodeCompiler {
    @Override
    public boolean supports(CustomNodeModelImpl node) {
        return node.getNode() instanceof KillEntityTaskNode;
    }

    @Override
    public ITask compileTask(QuestCompileContext context, CustomNodeModelImpl node, String stepId) {
        KillEntityTask task = new KillEntityTask();
        task.stepId = stepId;
        task.entityType = context.getString(node, "entity_type");
        task.killCount = Math.max(1, context.getInt(node, "kill_count"));
        task.tag = context.getString(node, "tag");
        return task;
    }
}
