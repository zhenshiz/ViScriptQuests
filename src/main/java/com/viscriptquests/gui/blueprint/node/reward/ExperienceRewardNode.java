package com.viscriptquests.gui.blueprint.node.reward;

import com.lowdragmc.lowdraglib2.nodegraphtookit.api.node.NodeAttribute;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.definition.IOptionDefinitionContext;
import com.viscriptquests.gui.blueprint.QuestBlueprintGraph;
import com.viscriptquests.gui.blueprint.node.QuestBlueprintNode;
import net.minecraft.network.chat.Component;

// 经验奖励节点，用于配置完成任务后给予的经验点数。
@NodeAttribute(name = "experience_reward", group = QuestBlueprintNode.REWARD_GROUP, graphTypes = QuestBlueprintGraph.class)
public class ExperienceRewardNode extends QuestBlueprintNode {
    @Override
    public Component getDisplayName() {
        return nodeName("experience_reward");
    }

    @Override
    public void onDefineOptions(IOptionDefinitionContext context) {
        intOption(context, "experience", 1);
        rewardCommonOptions(context);
    }
}
