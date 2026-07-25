package com.viscriptquests.quest.runtime;

import com.viscriptquests.compat.team.QuestTeamScope;
import com.viscriptquests.compat.team.QuestTeamService;
import com.viscriptquests.event.neoforge.QuestEvent;
import com.viscriptquests.quest.data.QuestSavedData;
import com.viscriptquests.quest.data.runtime.PlayerQuestState;
import com.viscriptquests.quest.data.runtime.QuestPlayerData;
import com.viscriptquests.quest.data.runtime.QuestStatus;
import com.viscriptquests.quest.data.runtime.TaskProgress;
import com.viscriptquests.quest.data.runtime.TaskStatus;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.common.NeoForge;

import java.util.Optional;
import java.util.UUID;
import java.util.function.Predicate;

/**
 * 将玩家任务进度同步到 VST 队伍范围。
 *
 * <p>VSQ 仍然按玩家保存任务数据；队伍联动只是把同一份任务状态复制到队员身上。
 * 这样目标进度共享，但玩家当前追踪的小任务仍尽量保持自己的选择。
 */
public class QuestTeamProgressService {
    public record QuestStateRef(UUID ownerId, PlayerQuestState state) {
    }

    public static Optional<QuestStateRef> findCompletedQuestInScope(ServerPlayer player, QuestSavedData savedData, String questId) {
        return findQuestInScope(player, savedData, questId, state -> state.status == QuestStatus.COMPLETED);
    }

    public static Optional<QuestStateRef> findActiveQuestInScope(ServerPlayer player, QuestSavedData savedData, String questId) {
        return findQuestInScope(player, savedData, questId, state -> state.status == QuestStatus.ACTIVE);
    }

    public static void syncQuestState(ServerPlayer sourcePlayer, PlayerQuestState sourceState) {
        if (sourcePlayer == null || sourceState == null || sourcePlayer.getServer() == null) {
            return;
        }
        QuestTeamScope scope = QuestTeamService.scopeOf(sourcePlayer);
        QuestSavedData savedData = QuestSavedData.get(sourcePlayer.getServer());
        HolderLookup.Provider provider = sourcePlayer.registryAccess();
        for (UUID memberId : scope.memberIds()) {
            QuestPlayerData memberData = savedData.getPlayer(memberId);
            PlayerQuestState copiedState = copyState(sourceState, provider);
            memberData.putQuest(copiedState);
            repairTracking(memberData, copiedState);
            ServerPlayer onlineMember = sourcePlayer.getServer().getPlayerList().getPlayer(memberId);
            if (onlineMember != null) {
                QuestTrackingService.refresh(onlineMember);
            }
        }
        savedData.setDirty();
    }

    public static boolean revokeQuestInScope(ServerPlayer sourcePlayer, String questId) {
        if (sourcePlayer == null || sourcePlayer.getServer() == null || questId == null || questId.isBlank()) {
            return false;
        }
        QuestTeamScope scope = QuestTeamService.scopeOf(sourcePlayer);
        QuestSavedData savedData = QuestSavedData.get(sourcePlayer.getServer());
        boolean changed = false;
        PlayerQuestState revokedState = savedData.getPlayer(sourcePlayer.getUUID())
                .findQuest(questId)
                .orElse(null);
        for (UUID memberId : scope.memberIds()) {
            QuestPlayerData memberData = savedData.getPlayer(memberId);
            if (revokedState == null) {
                revokedState = memberData.findQuest(questId).orElse(null);
            }
            boolean removed = memberData.removeQuest(questId);
            if (memberData.trackedQuestId.equals(questId)) {
                memberData.trackedQuestId = "";
                memberData.trackedStepId = "";
                removed = true;
            }
            if (!removed) {
                continue;
            }
            changed = true;
            ServerPlayer onlineMember = sourcePlayer.getServer().getPlayerList().getPlayer(memberId);
            if (onlineMember != null) {
                QuestTrackingService.refresh(onlineMember);
            }
        }
        if (changed) {
            savedData.setDirty();
            if (revokedState != null) {
                NeoForge.EVENT_BUS.post(new QuestEvent.QuestRevoked(sourcePlayer, revokedState));
            }
        }
        return changed;
    }

    public static PlayerQuestState copyState(PlayerQuestState source, HolderLookup.Provider provider) {
        PlayerQuestState copy = new PlayerQuestState();
        CompoundTag tag = source.serializeNBT(provider);
        copy.deserializeNBT(provider, tag.copy());
        return copy;
    }

    private static Optional<QuestStateRef> findQuestInScope(ServerPlayer player, QuestSavedData savedData,
                                                            String questId, Predicate<PlayerQuestState> predicate) {
        if (player == null || savedData == null || questId == null || questId.isBlank()) {
            return Optional.empty();
        }
        QuestTeamScope scope = QuestTeamService.scopeOf(player);
        for (UUID memberId : scope.memberIds()) {
            Optional<PlayerQuestState> state = savedData.getPlayer(memberId).findQuest(questId);
            if (state.isPresent() && predicate.test(state.get())) {
                return Optional.of(new QuestStateRef(memberId, state.get()));
            }
        }
        return Optional.empty();
    }

    private static void repairTracking(QuestPlayerData playerData, PlayerQuestState state) {
        if (state.status != QuestStatus.ACTIVE) {
            if (playerData.trackedQuestId.equals(state.questId)) {
                playerData.trackedQuestId = "";
                playerData.trackedStepId = "";
            }
            return;
        }
        if (playerData.trackedQuestId.equals(state.questId)) {
            if (!isActiveStep(state, playerData.trackedStepId)) {
                playerData.trackedStepId = firstActiveStepId(state);
            }
            return;
        }
        if (playerData.trackedQuestId == null || playerData.trackedQuestId.isBlank()) {
            playerData.trackedQuestId = state.questId;
            playerData.trackedStepId = firstActiveStepId(state);
        }
    }

    private static boolean isActiveStep(PlayerQuestState state, String stepId) {
        return stepId != null && !stepId.isBlank() && state.findStepProgress(stepId)
                .filter(progress -> progress.status == TaskStatus.ACTIVE)
                .isPresent();
    }

    private static String firstActiveStepId(PlayerQuestState state) {
        return state.taskProgresses.stream()
                .filter(progress -> progress.status == TaskStatus.ACTIVE)
                .map(progress -> progress.stepId)
                .findFirst()
                .orElse("");
    }
}
