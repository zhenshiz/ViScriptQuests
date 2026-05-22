package com.viscriptquests.gui.blueprint.node.reward;

import com.lowdragmc.lowdraglib2.nodegraphtookit.api.node.NodeAttribute;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.definition.IOptionDefinitionContext;
import com.viscriptquests.gui.blueprint.QuestBlueprintGraph;
import com.viscriptquests.gui.blueprint.QuestBlueprintTypes;
import com.viscriptquests.gui.blueprint.node.QuestBlueprintNode;
import net.minecraft.network.chat.Component;

// 战利品表奖励节点。数据包模式读取 loot table id，自定义模式读取简单概率掉落列表。
@NodeAttribute(name = "loot_table_reward", group = QuestBlueprintNode.REWARD_GROUP, graphTypes = QuestBlueprintGraph.class)
public class LootTableRewardNode extends QuestBlueprintNode {
    @Override
    public Component getDisplayName() {
        return nodeName("loot_table_reward");
    }

    @Override
    public void onDefineOptions(IOptionDefinitionContext context) {
        option(context, "loot_table_config", "loot_table_type", QuestBlueprintTypes.LOOT_TABLE_REWARD, QuestBlueprintTypes.defaultLootTableReward());
        rewardCommonOptions(context);
    }
}
