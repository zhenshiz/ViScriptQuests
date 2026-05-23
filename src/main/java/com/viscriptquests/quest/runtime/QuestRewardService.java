package com.viscriptquests.quest.runtime;

import com.viscriptquests.compat.team.QuestTeamScope;
import com.viscriptquests.compat.team.QuestTeamService;
import com.viscriptquests.quest.data.QuestFile;
import com.viscriptquests.quest.data.QuestSavedData;
import com.viscriptquests.quest.data.reward.IReward;
import com.viscriptquests.quest.data.runtime.PlayerQuestState;
import com.viscriptquests.quest.data.runtime.QuestPlayerData;
import net.minecraft.server.level.ServerPlayer;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

public class QuestRewardService {
    static void grantStepRewards(ServerPlayer player, QuestFile questFile, PlayerQuestState state, String stepId) {
        if (!state.rewardedSteps.add(stepId)) {
            return;
        }
        for (IReward reward : questFile.findRewardsForStep(stepId)) {
            grantReward(player, reward);
        }
    }

    static void grantQuestCompletionRewards(ServerPlayer player, QuestFile questFile) {
        for (IReward reward : questFile.findQuestCompletionRewards()) {
            grantReward(player, reward);
        }
    }

    static void grantDynamicReward(ServerPlayer player, IReward reward) {
        grantReward(player, reward);
    }

    public static void grantPendingRewards(ServerPlayer player) {
        QuestPlayerData playerData = QuestSavedData.get(player.getServer()).getPlayer(player.getUUID());
        if (playerData.pendingRewards.isEmpty()) {
            return;
        }
        var rewards = new java.util.ArrayList<>(playerData.pendingRewards);
        playerData.pendingRewards.clear();
        QuestSavedData.get(player.getServer()).setDirty();
        for (IReward reward : rewards) {
            reward.grant(player);
        }
    }

    private static void grantReward(ServerPlayer sourcePlayer, IReward reward) {
        QuestTeamScope scope = QuestTeamService.scopeOf(sourcePlayer);
        if (!scope.isParty()) {
            reward.grant(sourcePlayer);
            return;
        }

        Set<UUID> recipients = new LinkedHashSet<>();
        if (reward.teamLeaderOnly) {
            recipients.add(scope.leaderOr(sourcePlayer.getUUID()));
        } else {
            recipients.addAll(scope.memberIds());
        }
        QuestSavedData savedData = QuestSavedData.get(sourcePlayer.getServer());
        for (UUID recipientId : recipients) {
            ServerPlayer recipient = sourcePlayer.getServer().getPlayerList().getPlayer(recipientId);
            if (recipient != null) {
                reward.grant(recipient);
                continue;
            }
            savedData.getPlayer(recipientId).pendingRewards.add(copyReward(reward, sourcePlayer.registryAccess()));
            savedData.setDirty();
        }
    }

    private static IReward copyReward(IReward reward, net.minecraft.core.HolderLookup.Provider provider) {
        return com.viscriptquests.util.CodecUtil.deserializeNBT(IReward.CODEC,
                com.viscriptquests.util.CodecUtil.serializeNBT(IReward.CODEC, reward, provider),
                provider);
    }
}
