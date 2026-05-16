package com.viscriptquests.quest.data;

import lombok.AllArgsConstructor;
import lombok.Getter;
import net.minecraft.util.StringRepresentable;
import org.jetbrains.annotations.NotNull;

// 变量修改运算符，决定如何将新值应用到当前变量值
@Getter
@AllArgsConstructor
public enum VariableMutationOp implements StringRepresentable {
    SET("viscript_quests.var_mutation.set"),
    ADD("viscript_quests.var_mutation.add"),
    SUBTRACT("viscript_quests.var_mutation.subtract"),
    MULTIPLY("viscript_quests.var_mutation.multiply"),
    DIVIDE("viscript_quests.var_mutation.divide");

    private final String name;

    // 将运算应用到当前值上
    public float apply(float currentValue, float operand) {
        return switch (this) {
            case SET -> operand;
            case ADD -> currentValue + operand;
            case SUBTRACT -> currentValue - operand;
            case MULTIPLY -> currentValue * operand;
            case DIVIDE -> operand != 0 ? currentValue / operand : currentValue;
        };
    }

    @Override
    public @NotNull String getSerializedName() {
        return name;
    }
}
