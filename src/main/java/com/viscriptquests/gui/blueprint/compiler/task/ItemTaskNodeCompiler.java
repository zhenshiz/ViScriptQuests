package com.viscriptquests.gui.blueprint.compiler.task;

import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.CustomNodeModelImpl;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegister;
import com.viscriptquests.gui.blueprint.compiler.IQuestTaskNodeCompiler;
import com.viscriptquests.gui.blueprint.compiler.QuestCompileContext;
import com.viscriptquests.gui.blueprint.node.task.ItemTaskNode;
import com.viscriptquests.quest.data.task.ITask;
import com.viscriptquests.quest.data.task.ItemTask;
import net.minecraft.world.item.ItemStack;

@LDLRegister(name = "item_task", registry = IQuestTaskNodeCompiler.ID)
public class ItemTaskNodeCompiler implements IQuestTaskNodeCompiler {
    @Override
    public boolean supports(CustomNodeModelImpl node) {
        return node.getNode() instanceof ItemTaskNode;
    }

    @Override
    public ITask compileTask(QuestCompileContext context, CustomNodeModelImpl node, String stepId) {
        ItemTask task = new ItemTask();
        task.stepId = stepId;
        ItemStack stack = context.getItemStack(node, "item_stack");
        task.itemStack = stack.isEmpty() ? ItemStack.EMPTY : stack.copyWithCount(1);
        task.itemCount = context.tracePortIntValue(node, "item_count", 1, 1);
        task.itemCountExpression.addAll(context.compileRuntimeIntExpression(node, "item_count", 1));
        task.strictComponents = context.getBool(node, "strict_components");
        task.consumeItem = context.getBool(node, "consume_item");
        task.submitMode = context.getSubmitMode(node, "submit_mode");
        return task;
    }
}
