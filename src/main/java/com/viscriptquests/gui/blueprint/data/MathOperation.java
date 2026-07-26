package com.viscriptquests.gui.blueprint.data;

import lombok.AllArgsConstructor;
import lombok.Getter;
import net.minecraft.util.StringRepresentable;
import org.jetbrains.annotations.NotNull;

@Getter
@AllArgsConstructor
public enum MathOperation implements StringRepresentable {
    ADD("viscript_quests.math_operation.add"),
    SUBTRACT("viscript_quests.math_operation.subtract"),
    MULTIPLY("viscript_quests.math_operation.multiply"),
    DIVIDE("viscript_quests.math_operation.divide"),
    CLAMP("viscript_quests.math_operation.clamp"),
    RANDOM("viscript_quests.math_operation.random");

    private final String name;

    public boolean usesVariadicInputs() {
        return this == ADD || this == SUBTRACT || this == MULTIPLY || this == DIVIDE;
    }

    public float defaultInputValue(int index) {
        return switch (this) {
            case MULTIPLY -> 1f;
            case DIVIDE -> index <= 1 ? 0f : 1f;
            default -> 0f;
        };
    }

    public static MathOperation fromValue(Object value) {
        return value instanceof MathOperation operation ? operation : ADD;
    }

    @Override
    public @NotNull String getSerializedName() {
        return name;
    }
}
