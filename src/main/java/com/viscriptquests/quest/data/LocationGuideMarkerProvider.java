package com.viscriptquests.quest.data;

import lombok.AllArgsConstructor;
import lombok.Getter;
import net.minecraft.util.StringRepresentable;
import org.jetbrains.annotations.NotNull;

// 到达位置目标使用哪一种客户端导航标记。
@Getter
@AllArgsConstructor
public enum LocationGuideMarkerProvider implements StringRepresentable {
    BUILT_IN("viscript_quests.location_marker_provider.built_in"),
    XAERO_MINIMAP("viscript_quests.location_marker_provider.xaero_minimap");

    private final String name;

    public boolean usesBuiltInHudMarker() {
        return this == BUILT_IN;
    }

    public static LocationGuideMarkerProvider fromValue(Object value) {
        if (value instanceof LocationGuideMarkerProvider provider) {
            return provider;
        }
        if (value instanceof String serializedName) {
            for (LocationGuideMarkerProvider provider : values()) {
                if (provider.name().equalsIgnoreCase(serializedName)
                        || provider.getSerializedName().equals(serializedName)
                        || provider.getName().equals(serializedName)) {
                    return provider;
                }
            }
        }
        if (value instanceof Number index) {
            int ordinal = index.intValue();
            LocationGuideMarkerProvider[] providers = values();
            if (ordinal >= 0 && ordinal < providers.length) {
                return providers[ordinal];
            }
        }
        return BUILT_IN;
    }

    @Override
    public @NotNull String getSerializedName() {
        return name;
    }
}
