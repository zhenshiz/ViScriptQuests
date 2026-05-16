package com.viscriptquests.quest.runtime;

import com.viscriptquests.quest.data.QuestFile;
import com.viscriptquests.quest.data.QuestSavedData;
import com.viscriptquests.quest.data.runtime.QuestPlayerData;
import com.viscriptquests.quest.data.runtime.QuestStatus;
import com.viscriptquests.quest.data.runtime.TaskStatus;
import com.viscriptquests.quest.data.task.ITask;
import com.viscriptquests.util.QuestFileHelper;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;

/**
 * 处理玩家提交小任务的运行时服务。
 *
 * <p>该类只负责“小任务是否允许提交”和“提交成功后的运行时收尾”。
 * 具体目标是否满足由 {@link ITask#checkCompletion(ServerPlayer)} 判断，
 * 蓝图流程后续推进由 {@link QuestFlowExecutor} 处理。
 */
public class QuestSubmissionService {
    /**
     * 尝试提交玩家指定任务中的一个小任务。
     *
     * <p>提交成功后会标记小任务完成、发放该小任务奖励、继续推进任务流程，
     * 并刷新玩家当前追踪状态。任一校验失败时返回 {@code false}，调用方自行决定如何反馈。
     *
     * @param player 服务端玩家，提交小任务的玩家
     * @param questId 任务标识，必须已经是运行时使用的规范化标识
     * @param stepId 小任务标识，对应任务文件中的小任务节点
     * @param automatic 是否来自自动提交逻辑；自动提交会额外要求目标允许自动提交
     * @return 是否成功提交该小任务
     */
    public static boolean submit(ServerPlayer player, String questId, String stepId, boolean automatic) {
        QuestSavedData savedData = QuestSavedData.get(player.getServer());
        QuestPlayerData playerData = savedData.getPlayer(player.getUUID());
        var state = playerData.findQuest(questId);
        // 玩家没有接取该任务，或任务已经完成/失败时，不允许再提交小任务。
        if (state.isEmpty() || state.get().status != QuestStatus.ACTIVE) {
            return false;
        }

        QuestFile questFile = QuestFileHelper.getQuest(questId, player.registryAccess()).orElse(null);
        // 运行时任务文件不存在时，无法读取目标定义、奖励和流程边。
        if (questFile == null) {
            return false;
        }

        var questState = state.get();
        var progress = questState.findStepProgress(stepId);
        // 玩家任务状态里没有这个小任务，说明 stepId 不属于当前任务或存档状态异常。
        if (progress.isEmpty()) {
            return false;
        }
        // 已完成的小任务不能重复提交，避免重复发奖励和重复推进流程。
        if (progress.get().status == TaskStatus.COMPLETED) {
            return false;
        }
        // 只有当前被流程解锁的小任务可以提交；锁定、跳过等状态都不应该响应提交。
        if (progress.get().status != TaskStatus.ACTIVE) {
            return false;
        }

        List<ITask> tasks = questFile.findTasksForStep(stepId);
        // 一个可提交的小任务至少要有一个目标定义，否则无法判断完成条件。
        if (tasks.isEmpty()) {
            return false;
        }
        // 自动提交只处理明确允许自动提交的目标，防止 tick 检查误提交手动目标。
        if (automatic && tasks.stream().noneMatch(task -> task.submitMode.isAutoSubmit())) {
            return false;
        }
        // 同一个小任务下的所有目标都要完成，才算这个小任务完成。
        boolean completed = tasks.stream().allMatch(task -> task.checkCompletion(player));
        if (!completed) {
            return false;
        }
        // 目标完成后的副作用在这里执行，比如扣除物品；任意目标失败则本次提交失败。
        boolean applied = true;
        for (ITask task : tasks) {
            if (!task.onComplete(player)) {
                applied = false;
                break;
            }
        }
        if (!applied) {
            return false;
        }

        // 到这里才真正提交成功：写进度、发奖励、推进蓝图流程，并刷新任务追踪。
        progress.get().status = TaskStatus.COMPLETED;
        QuestRewardService.grantStepRewards(player, questFile, questState, stepId);
        QuestFlowExecutor.completeStepNode(player, questState, questFile, stepId);
        savedData.setDirty();
        QuestTrackingService.refreshAfterStepSubmit(player, playerData, questState, stepId);
        return true;
    }
}
