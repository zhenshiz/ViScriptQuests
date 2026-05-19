package com.viscriptquests.gui.blueprint.node.reward;

import com.lowdragmc.lowdraglib2.nodegraphtookit.api.node.NodeAttribute;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.definition.IOptionDefinitionContext;
import com.viscriptquests.gui.blueprint.QuestBlueprintGraph;
import com.viscriptquests.gui.blueprint.node.QuestBlueprintNode;
import com.viscriptquests.compat.team.QuestTeamService;
import net.minecraft.network.chat.Component;

// 物品奖励配置节点，纯数据节点，参数与 ItemReward 数据类一一对应
@NodeAttribute(name = "item_reward", group = QuestBlueprintNode.REWARD_GROUP, graphTypes = QuestBlueprintGraph.class)
public class ItemRewardNode extends QuestBlueprintNode {
    @Override
    public Component getDisplayName() {
        return nodeName("item_reward");
    }

    @Override
    public void onDefineOptions(IOptionDefinitionContext context) {
        itemStackOption(context, "item_stack");
        if (QuestTeamService.isLoaded()) {
            boolOption(context, "team_leader_only", false);
        }
    }
}
