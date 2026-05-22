package com.viscriptquests.gui.blueprint.node.reward;

import com.lowdragmc.lowdraglib2.nodegraphtookit.api.node.NodeAttribute;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.definition.IOptionDefinitionContext;
import com.viscriptquests.gui.blueprint.QuestBlueprintGraph;
import com.viscriptquests.gui.blueprint.node.QuestBlueprintNode;
import net.minecraft.network.chat.Component;

// 指令奖励节点，只保存作者填写的服务端命令，实际执行由运行时奖励负责。
@NodeAttribute(name = "command_reward", group = QuestBlueprintNode.REWARD_GROUP, graphTypes = QuestBlueprintGraph.class)
public class CommandRewardNode extends QuestBlueprintNode {
    @Override
    public Component getDisplayName() {
        return nodeName("command_reward");
    }

    @Override
    public void onDefineOptions(IOptionDefinitionContext context) {
        stringOption(context, "command", "");
        rewardCommonOptions(context);
    }
}
