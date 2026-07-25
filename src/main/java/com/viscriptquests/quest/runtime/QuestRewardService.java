package com.viscriptquests.quest.runtime;

import com.viscriptquests.compat.team.QuestTeamScope;
import com.viscriptquests.compat.team.QuestTeamService;
import com.viscriptquests.event.neoforge.QuestEvent;
import com.viscript_lib.util.CodecUtil;
import com.viscriptquests.quest.data.QuestFile;
import com.viscriptquests.quest.data.QuestSavedData;
import com.viscriptquests.quest.data.QuestVariableValue;
import com.viscriptquests.quest.data.reward.IReward;
import com.viscriptquests.quest.data.runtime.PlayerQuestState;
import com.viscriptquests.quest.data.runtime.QuestPlayerData;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.common.NeoForge;

import java.util.Map;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

public class QuestRewardService {
    static void grantStepRewards(ServerPlayer player, QuestFile questFile, PlayerQuestState state, String stepId) {
        if (!state.rewardedSteps.add(stepId)) {
            return;
        }
        for (IReward reward : questFile.findRewardsForStep(stepId)) {
            grantReward(player, reward, state, stepId, "task");
        }
    }

    static void grantQuestCompletionRewards(ServerPlayer player, QuestFile questFile, PlayerQuestState state) {
        for (IReward reward : questFile.findQuestCompletionRewards()) {
            grantReward(player, reward, state, "", "quest");
        }
    }

    static void grantDynamicReward(ServerPlayer player, IReward reward, PlayerQuestState state, String stepId) {
        grantReward(player, reward, state, stepId, "objective");
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
            NeoForge.EVENT_BUS.post(new QuestEvent.RewardGranted(player, player, null, reward,
                    reward.stepId, "pending"));
        }
    }

    private static void grantReward(ServerPlayer sourcePlayer, IReward reward, PlayerQuestState state,
                                    String stepId, String rewardSource) {
        Map<String, QuestVariableValue> questVariables = state == null ? Map.of() : state.questVariables;
        QuestTeamScope scope = QuestTeamService.scopeOf(sourcePlayer);
        if (!scope.isParty()) {
            IReward resolved = resolvedReward(reward, questVariables, sourcePlayer, sourcePlayer.registryAccess());
            resolved.grant(sourcePlayer);
            NeoForge.EVENT_BUS.post(new QuestEvent.RewardGranted(sourcePlayer, sourcePlayer, state, resolved,
                    stepId, rewardSource));
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
            IReward resolved = resolvedReward(reward, questVariables, sourcePlayer, sourcePlayer.registryAccess());
            if (recipient != null) {
                resolved.grant(recipient);
                NeoForge.EVENT_BUS.post(new QuestEvent.RewardGranted(sourcePlayer, recipient, state, resolved,
                        stepId, rewardSource));
                continue;
            }
            savedData.getPlayer(recipientId).pendingRewards.add(resolved);
            savedData.setDirty();
        }
    }

    private static IReward resolvedReward(IReward reward, Map<String, QuestVariableValue> questVariables,
                                          ServerPlayer expressionPlayer,
                                          net.minecraft.core.HolderLookup.Provider provider) {
        IReward resolved = copyReward(reward, provider);
        resolved.resolveDynamicValues(questVariables, expressionPlayer);
        return resolved;
    }

    private static IReward copyReward(IReward reward, net.minecraft.core.HolderLookup.Provider provider) {
        return CodecUtil.deserializeNBT(IReward.CODEC,
                CodecUtil.serializeNBT(IReward.CODEC, reward, provider),
                provider);
    }
}
