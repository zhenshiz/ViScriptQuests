package com.viscriptquests.gui.blueprint.compiler.task;

import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.CustomNodeModelImpl;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegister;
import com.viscriptquests.gui.blueprint.compiler.IQuestTaskNodeCompiler;
import com.viscriptquests.gui.blueprint.compiler.QuestCompileContext;
import com.viscriptquests.gui.blueprint.node.task.CurrencyTaskNode;
import com.viscriptquests.quest.data.task.CurrencyTask;
import com.viscriptquests.quest.data.task.ITask;
import com.viscriptshop.ViscriptShop;

@LDLRegister(name = "currency_task", registry = IQuestTaskNodeCompiler.ID, modID = ViscriptShop.MOD_ID)
public class CurrencyTaskNodeCompiler implements IQuestTaskNodeCompiler {
    @Override
    public boolean supports(CustomNodeModelImpl node) { return node.getNode() instanceof CurrencyTaskNode; }

    @Override
    public ITask compileTask(QuestCompileContext context, CustomNodeModelImpl node, String stepId) {
        CurrencyTask task = new CurrencyTask();
        task.stepId = stepId;
        task.currency = context.tracePortIntValue(node, "currency", 1, 1);
        task.currencyExpression.addAll(context.compileRuntimeIntExpression(node, "currency", 1));
        task.consumeCurrency = context.getBool(node, "consume_currency", true);
        task.submitMode = context.getSubmitMode(node, "submit_mode");
        return task;
    }
}
