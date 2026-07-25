package com.viscriptquests.quest.data;

import lombok.AllArgsConstructor;
import lombok.Getter;
import net.minecraft.util.StringRepresentable;
import org.jetbrains.annotations.NotNull;

// 到达位置目标的寻路点来源。
@Getter
@AllArgsConstructor
public enum LocationTargetType implements StringRepresentable {
    COORDINATES("viscript_quests.location_target_type.coordinates"),
    BIOME("viscript_quests.location_target_type.biome"),
    STRUCTURE("viscript_quests.location_target_type.structure");

    private final String name;

    public static LocationTargetType fromValue(Object value) {
        if (value instanceof LocationTargetType type) {
            return type;
        }
        if (value instanceof String serializedName) {
            for (LocationTargetType type : values()) {
                if (type.name().equalsIgnoreCase(serializedName)
                        || type.getSerializedName().equals(serializedName)
                        || type.getName().equals(serializedName)) {
                    return type;
                }
            }
        }
        if (value instanceof Number index) {
            int ordinal = index.intValue();
            LocationTargetType[] types = values();
            if (ordinal >= 0 && ordinal < types.length) {
                return types[ordinal];
            }
        }
        return COORDINATES;
    }

    @Override
    public @NotNull String getSerializedName() {
        return name;
    }
}
