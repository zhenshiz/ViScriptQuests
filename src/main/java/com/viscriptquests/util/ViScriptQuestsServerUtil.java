package com.viscriptquests.util;

import com.viscriptquests.event.neoforge.QuestEvent;
import com.viscriptquests.quest.data.QuestSavedData;
import com.viscriptquests.quest.data.runtime.PlayerQuestState;
import com.viscriptquests.quest.data.runtime.TaskProgress;
import com.viscriptquests.quest.runtime.QuestManager;
import dev.latvian.mods.kubejs.typings.Info;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

public final class ViScriptQuestsServerUtil {
    private ViScriptQuestsServerUtil() {
    }

    @Info("向玩家发放任务并启动任务流程。")
    public static boolean grant(ServerPlayer player, String questId) {
        return QuestManager.grant(player, questId);
    }

    @Info("从玩家或玩家所在的共享任务队伍移除任务。")
    public static boolean revoke(ServerPlayer player, String questId) {
        return QuestManager.revoke(player, questId);
    }

    @Info("强制完成任务，不检查任务目标。")
    public static boolean complete(ServerPlayer player, String questId) {
        return QuestManager.complete(player, questId);
    }

    @Info("提交小任务中当前所有满足条件的目标。")
    public static boolean submit(ServerPlayer player, String questId, String stepId) {
        return QuestManager.submit(player, questId, stepId);
    }

    @Info("按从 0 开始的目标索引提交小任务中的一个目标。")
    public static boolean submitObjective(ServerPlayer player, String questId, String stepId, int objectiveIndex) {
        return QuestManager.submitObjective(player, questId, stepId, objectiveIndex);
    }

    @Info("完成所有匹配触发 ID 的活跃自定义触发目标。")
    public static boolean triggerCustom(ServerPlayer player, String triggerId) {
        return QuestManager.triggerCustom(player, triggerId);
    }

    @Info("追踪任务中的第一个活跃小任务。")
    public static boolean track(ServerPlayer player, String questId) {
        return QuestManager.track(player, questId);
    }

    @Info("设置进行中任务的数值型运行时变量。")
    public static boolean setVariable(ServerPlayer player, String questId, String variableName, float value) {
        return QuestManager.setVariable(player, questId, variableName, value);
    }

    @Info("为玩家打开任务书。")
    public static void openQuestBook(ServerPlayer player) {
        QuestManager.openQuestBook(player);
    }

    @Info("返回玩家的任务状态；如果任务不存在则返回 null。")
    @Nullable
    public static PlayerQuestState getQuest(ServerPlayer player, String questId) {
        if (player == null || player.getServer() == null || questId == null || questId.isBlank()) {
            return null;
        }
        return QuestSavedData.get(player.getServer())
                .getPlayer(player.getUUID())
                .findQuest(QuestFileHelper.normalizeQuestId(questId))
                .orElse(null);
    }

    @Info("返回玩家某个任务中的小任务状态；如果不存在则返回 null。")
    @Nullable
    public static TaskProgress getTask(ServerPlayer player, String questId, String stepId) {
        PlayerQuestState quest = getQuest(player, questId);
        if (quest == null || stepId == null || stepId.isBlank()) {
            return null;
        }
        return quest.findStepProgress(stepId).orElse(null);
    }

    @Info("返回任务级 KubeJS 事件监听器使用的标准化目标。")
    public static String questTarget(String questId) {
        return QuestFileHelper.normalizeQuestId(questId);
    }

    @Info("返回小任务级 KubeJS 事件监听器使用的目标。")
    public static String taskTarget(String questId, String stepId) {
        return QuestEvent.taskTarget(questTarget(questId), stepId);
    }

    @Info("当目标拥有 ID 时，返回目标级 KubeJS 事件监听器使用的目标。")
    public static String objectiveTarget(String questId, String stepId, String objectiveId) {
        return QuestEvent.objectiveTarget(questTarget(questId), stepId, objectiveId);
    }

    @Info("当目标 ID 为空时，返回目标级 KubeJS 事件监听器使用的目标。")
    public static String objectiveIndexTarget(String questId, String stepId, int objectiveIndex) {
        return QuestEvent.objectiveIndexTarget(questTarget(questId), stepId, objectiveIndex);
    }
}
