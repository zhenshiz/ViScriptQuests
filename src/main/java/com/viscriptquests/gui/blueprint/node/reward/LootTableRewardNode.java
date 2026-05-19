package com.viscriptquests.gui.blueprint.node.reward;

import com.lowdragmc.lowdraglib2.nodegraphtookit.api.node.NodeAttribute;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.definition.IOptionDefinitionContext;
import com.viscriptquests.compat.team.QuestTeamService;
import com.viscriptquests.gui.blueprint.QuestBlueprintGraph;
import com.viscriptquests.gui.blueprint.QuestBlueprintTypes;
import com.viscriptquests.gui.blueprint.node.QuestBlueprintNode;
import com.viscriptquests.quest.data.LootTableType;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;

// 战利品表奖励节点。数据包模式读取 loot table id，自定义模式读取简单概率掉落列表。
@NodeAttribute(name = "loot_table_reward", group = QuestBlueprintNode.REWARD_GROUP, graphTypes = QuestBlueprintGraph.class)
public class LootTableRewardNode extends QuestBlueprintNode {
    @Override
    public Component getDisplayName() {
        return nodeName("loot_table_reward");
    }

    @Override
    public void onDefineOptions(IOptionDefinitionContext context) {
        enumOption(context, "loot_table_type", QuestBlueprintTypes.LOOT_TABLE_TYPE, LootTableType.DATA_PACK);
        if (selectedLootTableType() == LootTableType.CUSTOM) {
            option(context, "custom_loot_table", QuestBlueprintTypes.LOOT_TABLE_CONFIG_LIST, new ArrayList<>());
        } else {
            stringOption(context, "data_pack_path", "minecraft:chests/simple_dungeon");
        }
        if (QuestTeamService.isLoaded()) {
            boolOption(context, "team_leader_only", false);
        }
    }

    private LootTableType selectedLootTableType() {
        LootTableType[] selected = {LootTableType.DATA_PACK};
        var option = getNodeOptionById("loot_table_type");
        if (option != null) {
            option.tryGetValue(LootTableType.class).ifSuccess(type -> {
                if (type instanceof LootTableType lootTableType) {
                    selected[0] = lootTableType;
                }
            });
        }
        return selected[0];
    }
}
