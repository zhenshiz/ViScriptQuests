package com.viscriptquests.gui.blueprint.compiler.reward;

import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.CustomNodeModelImpl;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegister;
import com.viscriptquests.gui.blueprint.compiler.IQuestRewardNodeCompiler;
import com.viscriptquests.gui.blueprint.compiler.QuestCompileContext;
import com.viscriptquests.gui.blueprint.node.reward.CommandRewardNode;
import com.viscriptquests.quest.data.reward.CommandReward;
import com.viscriptquests.quest.data.reward.IReward;

@LDLRegister(name = "command_reward", registry = IQuestRewardNodeCompiler.ID)
public class CommandRewardNodeCompiler implements IQuestRewardNodeCompiler {
    @Override
    public boolean supports(CustomNodeModelImpl node) {
        return node.getNode() instanceof CommandRewardNode;
    }

    @Override
    public IReward compileReward(QuestCompileContext context, CustomNodeModelImpl node, String stepId) {
        CommandReward reward = new CommandReward();
        reward.stepId = stepId;
        reward.command = context.getString(node, "command").trim();
        IQuestRewardNodeCompiler.applyCommonOptions(context, node, reward);
        return reward;
    }
}
