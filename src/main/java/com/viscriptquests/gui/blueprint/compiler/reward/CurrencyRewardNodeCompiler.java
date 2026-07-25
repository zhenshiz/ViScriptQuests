package com.viscriptquests.gui.blueprint.compiler.reward;

import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.CustomNodeModelImpl;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegister;
import com.viscriptquests.gui.blueprint.compiler.IQuestRewardNodeCompiler;
import com.viscriptquests.gui.blueprint.compiler.QuestCompileContext;
import com.viscriptquests.gui.blueprint.node.reward.CurrencyRewardNode;
import com.viscriptquests.quest.data.reward.CurrencyReward;
import com.viscriptquests.quest.data.reward.IReward;

@LDLRegister(name = "currency_reward", registry = IQuestRewardNodeCompiler.ID)
public class CurrencyRewardNodeCompiler implements IQuestRewardNodeCompiler {
    @Override
    public boolean supports(CustomNodeModelImpl node) {
        return node.getNode() instanceof CurrencyRewardNode;
    }

    @Override
    public IReward compileReward(QuestCompileContext context, CustomNodeModelImpl node, String stepId) {
        CurrencyReward reward = new CurrencyReward();
        reward.stepId = stepId;
        reward.currency = context.tracePortIntValue(node, "currency", 1, 0);
        reward.currencyExpression.addAll(context.compileRuntimeIntExpression(node, "currency", 1));
        IQuestRewardNodeCompiler.applyCommonOptions(context, node, reward);
        return reward;
    }
}
