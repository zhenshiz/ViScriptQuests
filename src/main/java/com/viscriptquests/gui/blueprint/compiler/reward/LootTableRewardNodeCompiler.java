package com.viscriptquests.gui.blueprint.compiler.reward;

import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.CustomNodeModelImpl;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegister;
import com.viscriptquests.gui.blueprint.compiler.IQuestRewardNodeCompiler;
import com.viscriptquests.gui.blueprint.compiler.QuestCompileContext;
import com.viscriptquests.gui.blueprint.node.reward.LootTableRewardNode;
import com.viscriptquests.quest.data.reward.IReward;
import com.viscriptquests.quest.data.LootTableConfig;
import com.viscriptquests.quest.data.reward.LootTableReward;

@LDLRegister(name = "loot_table_reward", registry = IQuestRewardNodeCompiler.ID)
public class LootTableRewardNodeCompiler implements IQuestRewardNodeCompiler {
    @Override
    public boolean supports(CustomNodeModelImpl node) {
        return node.getNode() instanceof LootTableRewardNode;
    }

    @Override
    public IReward compileReward(QuestCompileContext context, CustomNodeModelImpl node, String stepId) {
        LootTableReward reward = new LootTableReward();
        reward.stepId = stepId;
        LootTableReward configured = context.getLootTableReward(node, "loot_table_config");
        if (configured != null) {
            configured.copyLootOptionsTo(reward);
        } else {
            applyLegacyOptions(context, node, reward);
        }
        reward.normalizeForGrant();
        IQuestRewardNodeCompiler.applyCommonOptions(context, node, reward);
        return reward;
    }

    private static void applyLegacyOptions(QuestCompileContext context, CustomNodeModelImpl node, LootTableReward reward) {
        reward.lootTableType = context.getLootTableType(node, "loot_table_type");
        if (reward.lootTableType == LootTableReward.LootTableType.CUSTOM) {
            reward.customLootTable.clear();
            for (LootTableConfig lootConfig : context.getLootTableConfigs(node, "custom_loot_table")) {
                reward.customLootTable.add(lootConfig.copy());
            }
            return;
        }
        reward.dataPackPath = context.getString(node, "data_pack_path").trim();
    }
}
