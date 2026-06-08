package com.viscriptquests.quest.data;

import net.minecraft.util.StringRepresentable;
import org.jetbrains.annotations.NotNull;

// 任务流程条件使用的比较运算符，运行时和蓝图编译器共用。
public enum CompareOp implements StringRepresentable {
    EQ("viscript_quests.compare_op.eq"),
    NE("viscript_quests.compare_op.ne"),
    GT("viscript_quests.compare_op.gt"),
    GE("viscript_quests.compare_op.ge"),
    LT("viscript_quests.compare_op.lt"),
    LE("viscript_quests.compare_op.le");

    private final String name;

    CompareOp(String name) {
        this.name = name;
    }

    public boolean test(float actual, float expected) {
        return switch (this) {
            case EQ -> actual == expected;
            case NE -> actual != expected;
            case GT -> actual > expected;
            case GE -> actual >= expected;
            case LT -> actual < expected;
            case LE -> actual <= expected;
        };
    }

    @Override
    public @NotNull String getSerializedName() {
        return name;
    }
}
