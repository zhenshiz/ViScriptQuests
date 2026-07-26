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
        return value instanceof LocationTargetType type ? type : COORDINATES;
    }

    @Override
    public @NotNull String getSerializedName() {
        return name;
    }
}
