package com.viscriptquests.quest.data;

import lombok.AllArgsConstructor;
import lombok.Getter;
import net.minecraft.util.StringRepresentable;
import org.jetbrains.annotations.NotNull;

// Xaero's Minimap 路标使用固定的 16 色枚举；这里保留同名色板，运行时再映射到 Xaero 的 WaypointColor。
@Getter
@AllArgsConstructor
public enum LocationWaypointColor implements StringRepresentable {
    BLACK("viscript_quests.location_marker_color.black", 0xFF000000),
    DARK_BLUE("viscript_quests.location_marker_color.dark_blue", 0xFF0000AA),
    DARK_GREEN("viscript_quests.location_marker_color.dark_green", 0xFF00AA00),
    DARK_AQUA("viscript_quests.location_marker_color.dark_aqua", 0xFF00AAAA),
    DARK_RED("viscript_quests.location_marker_color.dark_red", 0xFFAA0000),
    DARK_PURPLE("viscript_quests.location_marker_color.dark_purple", 0xFFAA00AA),
    GOLD("viscript_quests.location_marker_color.gold", 0xFFFFAA00),
    GRAY("viscript_quests.location_marker_color.gray", 0xFFAAAAAA),
    DARK_GRAY("viscript_quests.location_marker_color.dark_gray", 0xFF555555),
    BLUE("viscript_quests.location_marker_color.blue", 0xFF5555FF),
    GREEN("viscript_quests.location_marker_color.green", 0xFF55FF55),
    AQUA("viscript_quests.location_marker_color.aqua", 0xFF55FFFF),
    RED("viscript_quests.location_marker_color.red", 0xFFFF0000),
    PURPLE("viscript_quests.location_marker_color.purple", 0xFFFF55FF),
    YELLOW("viscript_quests.location_marker_color.yellow", 0xFFFFFF55),
    WHITE("viscript_quests.location_marker_color.white", 0xFFFFFFFF);

    private final String name;
    private final int argb;

    public static LocationWaypointColor closestTo(int argb) {
        int target = argb & 0xFFFFFF;
        LocationWaypointColor closest = WHITE;
        int closestDistance = Integer.MAX_VALUE;
        for (LocationWaypointColor color : values()) {
            int distance = colorDistance(target, color.argb & 0xFFFFFF);
            if (distance < closestDistance) {
                closestDistance = distance;
                closest = color;
            }
        }
        return closest;
    }

    @Override
    public @NotNull String getSerializedName() {
        return name;
    }

    private static int colorDistance(int a, int b) {
        int ar = (a >> 16) & 0xFF;
        int ag = (a >> 8) & 0xFF;
        int ab = a & 0xFF;
        int br = (b >> 16) & 0xFF;
        int bg = (b >> 8) & 0xFF;
        int bb = b & 0xFF;
        int dr = ar - br;
        int dg = ag - bg;
        int db = ab - bb;
        return dr * dr + dg * dg + db * db;
    }
}
