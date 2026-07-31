package com.viscriptquests.gui.blueprint.node.player;

import com.lowdragmc.lowdraglib2.nodegraphtookit.api.node.NodeAttribute;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.definition.IOptionDefinitionContext;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.definition.IPortDefinitionContext;
import com.viscriptquests.gui.blueprint.QuestBlueprintGraph;
import com.viscriptquests.gui.blueprint.node.QuestBlueprintNode;
import net.minecraft.network.chat.Component;

/**
 * 读取当前任务玩家背包中符合物品组件匹配规则的物品数量。
 */
@NodeAttribute(name = QuestBlueprintNode.ID + "get_player_item_count", group = QuestBlueprintNode.PLAYER_GROUP,
        graphTypes = QuestBlueprintGraph.class)
public class GetPlayerItemCountNode extends QuestBlueprintNode {
    @Override
    public Component getDisplayName() {
        return nodeName("get_player_item_count");
    }

    @Override
    public void onDefineOptions(IOptionDefinitionContext context) {
        itemStackOption(context, "item_stack");
        itemMatchRuleOption(context, "item_match_rule");
    }

    @Override
    public void onDefinePorts(IPortDefinitionContext context) {
        intOutput(context, "item_count");
    }
}
