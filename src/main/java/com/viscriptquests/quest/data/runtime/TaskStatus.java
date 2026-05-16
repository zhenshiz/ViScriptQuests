package com.viscriptquests.quest.data.runtime;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.minecraft.network.chat.Component;

// 任务目标的状态。
@Getter
@RequiredArgsConstructor
public enum TaskStatus {
    /**
     * 表示任务目标尚未解锁。
     *
     * <p>锁定目标不会显示在默认任务书列表中，也不能被提交。流程推进到对应小任务节点时，
     * 该状态会变为 <code>ACTIVE</code>。
     */
    LOCKED("viscript_quests.quest_book.task_status.locked"),

    /**
     * 表示任务目标已经解锁并等待玩家完成。
     *
     * <p>只有激活状态的目标会参与手动提交或自动提交检查，也会被任务追踪服务选为可追踪目标。
     */
    ACTIVE("viscript_quests.quest_book.task_status.active"),

    /**
     * 表示任务目标已经完成。
     *
     * <p>完成状态通常由提交服务在条件满足并成功执行完成逻辑后设置。完成目标不会再次提交，
     * 并会根据客户端配置决定是否继续显示在任务书中。
     */
    COMPLETED("viscript_quests.quest_book.task_status.completed"),

    /**
     * 表示任务目标执行失败。
     *
     * <p>该状态用于保留失败目标的结果显示或后续扩展逻辑。当前流程主要通过任务整体失败和
     * 跳过分支处理结束路径，不会把普通未完成目标自动改为此状态。
     */
    FAILED("viscript_quests.quest_book.task_status.failed"),

    /**
     * 表示任务目标因为流程分支或任务结束而被跳过。
     *
     * <p>当 Join 节点选择了其他分支，或任务结束时仍有激活和锁定目标未完成，这些目标会被标记为
     * 跳过。跳过目标不会再提交，并会根据客户端配置决定是否继续显示在任务书中。
     */
    SKIPPED("viscript_quests.quest_book.task_status.skipped"),

    /**
     * 表示任务目标对玩家隐藏。
     *
     * <p>隐藏目标不会显示在默认任务书列表中。该状态用于需要在运行时保留进度数据，
     * 但暂时不希望玩家看到的目标。
     */
    HIDDEN("viscript_quests.quest_book.task_status.hidden");

    private final String translationKey;

    public Component displayName() {
        return Component.translatable(translationKey);
    }
}
