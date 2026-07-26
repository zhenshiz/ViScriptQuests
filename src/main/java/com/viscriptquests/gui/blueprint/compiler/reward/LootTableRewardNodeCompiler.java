package com.viscriptquests.gui.blueprint.compiler.reward;

import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.CustomNodeModelImpl;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegister;
import com.viscriptquests.gui.blueprint.compiler.IQuestRewardNodeCompiler;
import com.viscriptquests.gui.blueprint.compiler.QuestCompileContext;
import com.viscriptquests.gui.blueprint.node.reward.LootTableRewardNode;
import com.viscriptquests.quest.data.reward.IReward;
import com.viscriptquests.quest.data.reward.LootTableReward;

@LDLRegister(name = "loot_table_reward", registry = IQuestRewardNodeCompiler.ID)
public class LootTableRewardNodeCompiler implements IQuestRewardNodeCompiler {
    @Override
    public boolean supports(CustomNodeModelImpl node) {
        return node.getNode() instanceof LootTableRewardNode;
    }

    @Override
    public IReward compileReward(QuestCompileContext context, CustomNodeModelImpl node, String stepId) {
        LootTableReward reward = context.getLootTableReward(node, "loot_table_config");
        if (reward == null) {
            reward = new LootTableReward();
        }
        reward.stepId = stepId;
        reward.normalizeForGrant();
        IQuestRewardNodeCompiler.applyCommonOptions(context, node, reward);
        return reward;
    }
}
