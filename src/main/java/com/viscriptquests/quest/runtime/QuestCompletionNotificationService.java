package com.viscriptquests.quest.runtime;

import com.lowdragmc.lowdraglib2.networking.rpc.RPCPacketDistributor;
import com.viscriptquests.compat.team.QuestTeamScope;
import com.viscriptquests.compat.team.QuestTeamService;
import com.viscriptquests.network.s2c.S2CPayload;
import com.viscriptquests.quest.data.runtime.PlayerQuestState;
import com.viscriptquests.quest.data.runtime.QuestCompletionToastData;
import com.viscriptquests.quest.data.runtime.TaskProgress;
import net.minecraft.server.level.ServerPlayer;

final class QuestCompletionNotificationService {
    private QuestCompletionNotificationService() {
    }

    static void notifyTaskCompleted(ServerPlayer sourcePlayer, TaskProgress progress) {
        if (progress != null) {
            sendToOnlineScope(sourcePlayer, QuestCompletionToastData.task(progress));
        }
    }

    static void notifyQuestCompleted(ServerPlayer sourcePlayer, PlayerQuestState state) {
        if (state != null) {
            sendToOnlineScope(sourcePlayer, QuestCompletionToastData.quest(state));
        }
    }

    private static void sendToOnlineScope(ServerPlayer sourcePlayer, QuestCompletionToastData data) {
        if (sourcePlayer == null || sourcePlayer.getServer() == null) {
            return;
        }
        QuestTeamScope scope = QuestTeamService.scopeOf(sourcePlayer);
        for (ServerPlayer recipient : QuestTeamService.onlineMembers(sourcePlayer.getServer(), scope)) {
            RPCPacketDistributor.rpcToPlayer(recipient, S2CPayload.SHOW_QUEST_COMPLETION_TOAST, data);
        }
    }
}
