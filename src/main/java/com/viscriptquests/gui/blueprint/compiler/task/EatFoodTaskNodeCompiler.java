package com.viscriptquests.gui.blueprint.compiler.task;

import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.CustomNodeModelImpl;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegister;
import com.viscriptquests.gui.blueprint.compiler.IQuestTaskNodeCompiler;
import com.viscriptquests.gui.blueprint.compiler.QuestCompileContext;
import com.viscriptquests.gui.blueprint.node.task.EatFoodTaskNode;
import com.viscriptquests.quest.data.ItemMatchRule;
import com.viscriptquests.quest.data.task.EatFoodTask;
import com.viscriptquests.quest.data.task.ITask;

/**
 * 将吃完食物节点编译为支持食用和饮用的事件累计目标。
 */
@LDLRegister(name = "eat_food_task", registry = IQuestTaskNodeCompiler.ID)
public class EatFoodTaskNodeCompiler implements IQuestTaskNodeCompiler {
    @Override
    public boolean supports(CustomNodeModelImpl node) {
        return node.getNode() instanceof EatFoodTaskNode;
    }

    @Override
    public ITask compileTask(QuestCompileContext context, CustomNodeModelImpl node, String stepId) {
        EatFoodTask task = new EatFoodTask();
        task.stepId = stepId;
        task.anyFood = context.getBool(node, "any_food", true);
        task.itemStack = context.getItemStack(node, "item_stack");
        if (!task.itemStack.isEmpty()) {
            task.itemStack = task.itemStack.copyWithCount(1);
        }
        ItemMatchRule rule = context.getItemMatchRule(node, "item_match_rule");
        task.itemMatchRule = rule == null ? new ItemMatchRule() : rule;
//        Object hand = context.getOptionValue(node, "use_hand");
//        task.hand = hand instanceof ItemUseHand useHand ? useHand : ItemUseHand.ANY;
        task.eatCount = context.tracePortIntValue(node, "eat_count", 1, 1);
        task.eatCountExpression.addAll(context.compileRuntimeIntExpression(node, "eat_count", 1));
        return task;
    }
}
