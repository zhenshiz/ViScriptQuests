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
        if (value instanceof MathOperation operation) {
            return operation;
        }
        if (value instanceof String serializedName) {
            for (MathOperation operation : values()) {
                if (operation.name().equalsIgnoreCase(serializedName)
                        || operation.getSerializedName().equals(serializedName)) {
                    return operation;
                }
            }
        }
        if (value instanceof Number index) {
            MathOperation[] operations = values();
            int ordinal = index.intValue();
            if (ordinal >= 0 && ordinal < operations.length) {
                return operations[ordinal];
            }
        }
        return ADD;
    }

    @Override
    public @NotNull String getSerializedName() {
        return name;
    }
}
