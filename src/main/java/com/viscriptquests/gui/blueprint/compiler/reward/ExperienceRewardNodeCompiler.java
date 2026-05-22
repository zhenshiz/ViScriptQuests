package com.viscriptquests.gui.blueprint.compiler.reward;

import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.CustomNodeModelImpl;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegister;
import com.viscriptquests.gui.blueprint.compiler.IQuestRewardNodeCompiler;
import com.viscriptquests.gui.blueprint.compiler.QuestCompileContext;
import com.viscriptquests.gui.blueprint.node.reward.ExperienceRewardNode;
import com.viscriptquests.quest.data.reward.ExperienceReward;
import com.viscriptquests.quest.data.reward.IReward;

@LDLRegister(name = "experience_reward", registry = IQuestRewardNodeCompiler.ID)
public class ExperienceRewardNodeCompiler implements IQuestRewardNodeCompiler {
    @Override
    public boolean supports(CustomNodeModelImpl node) {
        return node.getNode() instanceof ExperienceRewardNode;
    }

    @Override
    public IReward compileReward(QuestCompileContext context, CustomNodeModelImpl node, String stepId) {
        ExperienceReward reward = new ExperienceReward();
        reward.stepId = stepId;
        reward.experience = Math.max(0, context.getInt(node, "experience"));
        IQuestRewardNodeCompiler.applyCommonOptions(context, node, reward);
        return reward;
    }
}
