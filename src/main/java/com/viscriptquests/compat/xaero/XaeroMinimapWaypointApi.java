package com.viscriptquests.compat.xaero;

import com.viscriptquests.quest.data.LocationGuideMarkerProvider;
import com.viscriptquests.quest.data.runtime.QuestGuideMarker;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import xaero.common.XaeroMinimapSession;
import xaero.common.minimap.waypoints.Waypoint;
import xaero.common.minimap.waypoints.WaypointSet;
import xaero.common.minimap.waypoints.WaypointWorld;
import xaero.common.minimap.waypoints.WaypointsManager;
import xaero.hud.minimap.waypoint.WaypointColor;
import xaero.hud.minimap.waypoint.WaypointPurpose;
import xaero.hud.minimap.waypoint.WaypointSession;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// 这个类直接引用 Xaero API，只能在 XaeroMinimapWaypointCompat 确认模组已加载后调用。
final class XaeroMinimapWaypointApi {
    private static final String DEFAULT_SYMBOL = "Q";

    private static final Map<String, Waypoint> activeWaypoints = new LinkedHashMap<>();
    private static WaypointSet activeSet;

    private XaeroMinimapWaypointApi() {
    }

    static boolean isActiveFor(QuestGuideMarker marker) {
        return activeWaypoints.containsKey(markerKey(marker));
    }

    static void sync(QuestGuideMarker marker, String fallbackLabel) {
        sync(marker == null ? List.of() : List.of(marker), fallbackLabel);
    }

    static void sync(Collection<QuestGuideMarker> markers, String fallbackLabel) {
        Map<String, QuestGuideMarker> desired = desiredMarkers(markers);
        if (desired.isEmpty()) {
            clear();
            return;
        }
        try {
            WaypointsManager manager = currentWaypointsManager();
            if (manager == null) {
                clear();
                return;
            }
            WaypointWorld world = manager.getCurrentWorld();
            if (world == null) {
                clear();
                return;
            }
            WaypointSet set = world.getCurrentSet();
            if (set == null) {
                clear();
                return;
            }
            if (set == activeSet && activeWaypoints.keySet().equals(desired.keySet())) {
                return;
            }
            clear();
            for (QuestGuideMarker marker : desired.values()) {
                Waypoint waypoint = createWaypoint(marker, displayLabel(marker, fallbackLabel));
                set.add(waypoint, true);
                activeWaypoints.put(markerKey(marker), waypoint);
            }
            WaypointSession waypointSession = manager.getWaypointSession();
            if (waypointSession != null) {
                waypointSession.setSetChangedTime(System.currentTimeMillis());
            }
            activeSet = set;
        } catch (RuntimeException ignored) {
            clear();
        }
    }

    static void clear() {
        if (activeSet != null) {
            for (Waypoint waypoint : activeWaypoints.values()) {
                try {
                    activeSet.remove(waypoint);
                } catch (RuntimeException ignored) {
                }
            }
        }
        activeSet = null;
        activeWaypoints.clear();
    }

    private static Map<String, QuestGuideMarker> desiredMarkers(Collection<QuestGuideMarker> markers) {
        Map<String, QuestGuideMarker> desired = new LinkedHashMap<>();
        if (markers == null) {
            return desired;
        }
        for (QuestGuideMarker marker : markers) {
            if (canCreateWaypoint(marker)) {
                desired.put(markerKey(marker), marker);
            }
        }
        return desired;
    }

    private static boolean canCreateWaypoint(QuestGuideMarker marker) {
        return marker != null
                && marker.isEnabled()
                && marker.markerProvider == LocationGuideMarkerProvider.XAERO_MINIMAP
                && sameClientDimension(marker);
    }

    private static WaypointsManager currentWaypointsManager() {
        XaeroMinimapSession session = XaeroMinimapSession.getCurrentSession();
        return session == null ? null : session.getWaypointsManager();
    }

    private static Waypoint createWaypoint(QuestGuideMarker marker, String label) {
        return new Waypoint(
                Mth.floor(marker.x),
                Mth.floor(marker.y),
                Mth.floor(marker.z),
                label,
                DEFAULT_SYMBOL,
                closestColor(marker.color),
                WaypointPurpose.NORMAL,
                true,
                true
        );
    }

    private static WaypointColor closestColor(int argb) {
        int target = argb & 0xFFFFFF;
        WaypointColor closest = WaypointColor.WHITE;
        int closestDistance = Integer.MAX_VALUE;
        for (WaypointColor color : WaypointColor.values()) {
            int distance = colorDistance(target, color.getHex() & 0xFFFFFF);
            if (distance < closestDistance) {
                closestDistance = distance;
                closest = color;
            }
        }
        return closest;
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

    private static boolean sameClientDimension(QuestGuideMarker marker) {
        Minecraft minecraft = Minecraft.getInstance();
        return minecraft.player != null
                && minecraft.level != null
                && marker.dimension != null
                && minecraft.player.level().dimension().location().toString().equals(marker.dimension);
    }

    private static String displayLabel(QuestGuideMarker marker, String fallbackLabel) {
        if (marker.label != null && !marker.label.isBlank()) {
            return marker.label;
        }
        return fallbackLabel == null || fallbackLabel.isBlank() ? "Quest" : fallbackLabel;
    }

    private static String markerKey(QuestGuideMarker marker) {
        if (marker == null) {
            return "";
        }
        return marker.dimension
                + "|" + Mth.floor(marker.x)
                + "|" + Mth.floor(marker.y)
                + "|" + Mth.floor(marker.z)
                + "|" + marker.label
                + "|" + marker.color
                + "|" + marker.markerProvider;
    }
}
