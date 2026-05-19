package com.viscriptquests.gui.blueprint.compiler.reward;

import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.CustomNodeModelImpl;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegister;
import com.viscriptquests.gui.blueprint.compiler.IQuestRewardNodeCompiler;
import com.viscriptquests.gui.blueprint.compiler.QuestCompileContext;
import com.viscriptquests.gui.blueprint.node.reward.ItemRewardNode;
import com.viscriptquests.quest.data.reward.IReward;
import com.viscriptquests.quest.data.reward.ItemReward;

@LDLRegister(name = "item_reward", registry = IQuestRewardNodeCompiler.ID)
public class ItemRewardNodeCompiler implements IQuestRewardNodeCompiler {
    @Override
    public boolean supports(CustomNodeModelImpl node) {
        return node.getNode() instanceof ItemRewardNode;
    }

    @Override
    public IReward compileReward(QuestCompileContext context, CustomNodeModelImpl node, String stepId) {
        ItemReward reward = new ItemReward();
        reward.stepId = stepId;
        reward.itemStack = context.getItemStack(node, "item_stack");
        reward.teamLeaderOnly = context.getBool(node, "team_leader_only");
        return reward;
    }
}
