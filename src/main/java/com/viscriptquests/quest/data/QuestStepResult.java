package com.viscriptquests.quest.data;

import lombok.AllArgsConstructor;
import lombok.Getter;
import net.minecraft.util.StringRepresentable;
import org.jetbrains.annotations.NotNull;

// 小任务节点完成后的出口结果，用于区分主图上的成功/失败执行流。
@Getter
@AllArgsConstructor
public enum QuestStepResult implements StringRepresentable {
    ANY("viscript_quests.quest_step_result.any"),
    SUCCESS("viscript_quests.quest_step_result.success"),
    FAILURE("viscript_quests.quest_step_result.failure");

    private final String name;

    public boolean isSpecificResult() {
        return this == SUCCESS || this == FAILURE;
    }

    @Override
    public @NotNull String getSerializedName() {
        return name;
    }
}
