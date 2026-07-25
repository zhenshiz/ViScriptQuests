package com.viscriptquests.gui.blueprint.compiler.task;

import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.CustomNodeModelImpl;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegister;
import com.viscriptquests.gui.blueprint.compiler.IQuestTaskNodeCompiler;
import com.viscriptquests.gui.blueprint.compiler.QuestCompileContext;
import com.viscriptquests.gui.blueprint.node.task.EntityDeathTaskNode;
import com.viscriptquests.quest.data.task.EntityDeathTask;
import com.viscriptquests.quest.data.task.ITask;

@LDLRegister(name = "entity_death_task", registry = IQuestTaskNodeCompiler.ID)
public class EntityDeathTaskNodeCompiler implements IQuestTaskNodeCompiler {
    @Override
    public boolean supports(CustomNodeModelImpl node) {
        return node.getNode() instanceof EntityDeathTaskNode;
    }

    @Override
    public ITask compileTask(QuestCompileContext context, CustomNodeModelImpl node, String stepId) {
        EntityDeathTask task = new EntityDeathTask();
        task.stepId = stepId;
        task.entityType = context.getString(node, "entity_type");
        task.deathCount = context.tracePortIntValue(node, "death_count", 1, 1);
        task.deathCountExpression.addAll(context.compileRuntimeIntExpression(node, "death_count", 1));
        task.tag = context.getString(node, "tag");
        return task;
    }
}
