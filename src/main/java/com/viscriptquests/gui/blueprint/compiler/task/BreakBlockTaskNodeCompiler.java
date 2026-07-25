package com.viscriptquests.gui.blueprint.compiler.task;

import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.CustomNodeModelImpl;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegister;
import com.viscriptquests.gui.blueprint.compiler.IQuestTaskNodeCompiler;
import com.viscriptquests.gui.blueprint.compiler.QuestCompileContext;
import com.viscriptquests.gui.blueprint.node.task.BreakBlockTaskNode;
import com.viscriptquests.quest.data.task.BreakBlockTask;
import com.viscriptquests.quest.data.task.ITask;

@LDLRegister(name = "break_block_task", registry = IQuestTaskNodeCompiler.ID)
public class BreakBlockTaskNodeCompiler implements IQuestTaskNodeCompiler {
    @Override
    public boolean supports(CustomNodeModelImpl node) {
        return node.getNode() instanceof BreakBlockTaskNode;
    }

    @Override
    public ITask compileTask(QuestCompileContext context, CustomNodeModelImpl node, String stepId) {
        BreakBlockTask task = new BreakBlockTask();
        task.stepId = stepId;
        task.block = context.getBlock(node, "block");
        task.breakCount = context.tracePortIntValue(node, "break_count", 1, 1);
        task.breakCountExpression.addAll(context.compileRuntimeIntExpression(node, "break_count", 1));
        return task;
    }
}
