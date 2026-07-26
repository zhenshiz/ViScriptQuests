package com.viscriptquests.quest.runtime;

import com.viscriptquests.quest.data.QuestSavedData;
import com.viscriptquests.quest.data.runtime.PlayerQuestState;
import com.viscriptquests.quest.data.runtime.QuestStatus;
import com.viscriptquests.quest.data.runtime.TaskStatus;
import com.viscriptquests.util.QuestFileHelper;
import lombok.experimental.UtilityClass;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;

// Debug调试用
public class QuestRuntimeDebug {

    public static boolean setVariable(ServerPlayer player, String questId, String varName, float value) {
        String normalizedQuestId = QuestFileHelper.normalizeQuestId(questId);
        QuestSavedData savedData = QuestSavedData.get(player.getServer());
        var playerData = savedData.getPlayer(player.getUUID());
        var state = playerData.findQuest(normalizedQuestId);
        if (state.isEmpty() || state.get().status != QuestStatus.ACTIVE) {
            return false;
        }
        state.get().setVariable(varName, value, player.registryAccess());
        savedData.setDirty();
        return true;
    }

    public static List<Component> list(ServerPlayer player) {
        var playerData = QuestSavedData.get(player.getServer()).getPlayer(player.getUUID());
        List<Component> lines = new ArrayList<>();
        lines.add(Component.translatable("commands.viscript_quests.quest.list.header",
                player.getDisplayName(), playerData.quests.size()));
        for (PlayerQuestState state : playerData.quests) {
            String tracked = playerData.trackedQuestId.equals(state.questId) ? " *" : "";
            int completed = (int) state.taskProgresses.stream()
                    .filter(progress -> progress.status == TaskStatus.COMPLETED)
                    .count();
            lines.add(Component.translatable("commands.viscript_quests.quest.list.entry",
                    state.questId, state.status.name(), completed, state.taskProgresses.size(), tracked));
        }
        return lines;
    }
}
