package com.viscriptquests.quest.runtime;

import com.viscriptquests.quest.data.QuestFile;
import com.viscriptquests.quest.data.reward.IReward;
import com.viscriptquests.quest.data.runtime.PlayerQuestState;
import lombok.experimental.UtilityClass;
import net.minecraft.server.level.ServerPlayer;

public class QuestRewardService {
    static void grantStepRewards(ServerPlayer player, QuestFile questFile, PlayerQuestState state, String stepId) {
        if (!state.rewardedSteps.add(stepId)) {
            return;
        }
        for (IReward reward : questFile.findRewardsForStep(stepId)) {
            reward.grant(player);
        }
    }

    static void grantQuestCompletionRewards(ServerPlayer player, QuestFile questFile) {
        for (IReward reward : questFile.findQuestCompletionRewards()) {
            reward.grant(player);
        }
    }
}
