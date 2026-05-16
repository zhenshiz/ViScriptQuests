package com.viscriptquests.quest.data.runtime;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.minecraft.network.chat.Component;

// 任务的状态
@Getter
@RequiredArgsConstructor
public enum QuestStatus {
    /**
     * 表示任务已经发放给玩家，并且仍在进行中。
     *
     * <p>只有激活状态的任务会接受目标提交、运行流程推进、变量调试写入和追踪设置。
     * 新建的玩家任务实例默认进入此状态。
     */
    ACTIVE("viscript_quests.quest_book.quest_status.active"),

    /**
     * 表示任务已经成功完成。
     *
     * <p>流程到达成功结束节点、所有活跃流程节点自然结束，或通过调试/命令强制完成时会进入此状态。
     * 进入该状态后会记录完成时间，未完成的小任务会被清理为跳过或已完成，并停止继续推进流程。
     */
    COMPLETED("viscript_quests.quest_book.quest_status.completed"),

    /**
     * 表示任务以失败结果结束。
     *
     * <p>流程到达失败结束节点时会进入此状态。失败任务不会发放任务完成奖励，
     * 并会清理仍在运行的小任务和追踪信息。
     */
    FAILED("viscript_quests.quest_book.quest_status.failed"),

    /**
     * 表示任务被撤销。
     *
     * <p>该状态用于需要保留撤销记录的运行时或存档扩展。当前撤销命令会直接从玩家任务列表移除任务，
     * 因此通常不会把已有任务实例切换到此状态。
     */
    REVOKED("viscript_quests.quest_book.quest_status.revoked");

    private final String translationKey;

    public Component displayName() {
        return Component.translatable(translationKey);
    }
}
