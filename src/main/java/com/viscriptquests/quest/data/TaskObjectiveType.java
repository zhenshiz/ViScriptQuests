package com.viscriptquests.quest.data;

import lombok.AllArgsConstructor;
import lombok.Getter;
import net.minecraft.util.StringRepresentable;
import org.jetbrains.annotations.NotNull;

// 小任务内单个目标的完成语义。
@Getter
@AllArgsConstructor
public enum TaskObjectiveType implements StringRepresentable {
    // 必做目标会阻塞小任务完成，是普通目标的默认类型。
    REQUIRED("viscript_quests.task_objective_type.required"),
    // 可选目标可以记录进度，但不会在存在必做目标时阻塞小任务完成。
    OPTIONAL("viscript_quests.task_objective_type.optional"),
    // 失败条件一旦被触发，就会让当前任务按失败结果结束。
    FAILURE("viscript_quests.task_objective_type.failure");

    private final String name;

    public boolean isRequired() {
        return this == REQUIRED;
    }

    public boolean isOptional() {
        return this == OPTIONAL;
    }

    public boolean isFailureCondition() {
        return this == FAILURE;
    }

    @Override
    public @NotNull String getSerializedName() {
        return name;
    }
}
