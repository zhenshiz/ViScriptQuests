package com.viscriptquests.gui.blueprint.compiler;

import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.CustomNodeModelImpl;
import com.lowdragmc.lowdraglib2.registry.ILDLRegister;
import com.viscriptquests.ViScriptQuests;
import com.viscriptquests.quest.data.reward.IReward;

import java.util.function.Supplier;

// 将奖励节点编译成运行时 IReward 的扩展点。
public interface IQuestRewardNodeCompiler extends ILDLRegister<IQuestRewardNodeCompiler, Supplier<IQuestRewardNodeCompiler>> {
    String ID = ViScriptQuests.MOD_ID + ":blueprint_reward_node_compiler";

    boolean supports(CustomNodeModelImpl node);

    IReward compileReward(QuestCompileContext context, CustomNodeModelImpl node, String stepId);
}
