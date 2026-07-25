package com.viscriptquests.gui.hud;

import com.viscriptquests.compat.xaero.XaeroMinimapWaypointCompat;
import com.viscriptquests.quest.data.LocationGuideMarkerProvider;
import com.viscriptquests.quest.data.runtime.QuestGuideMarker;

import java.util.List;

// 将服务端同步的任务导航标记分发给可选客户端小地图联动。
public final class QuestGuideMarkerClientBridge {
    private QuestGuideMarkerClientBridge() {
    }

    public static void update(QuestHudData.ComponentState snapshot) {
        if (snapshot == null || snapshot.isEmpty()) {
            clearExternalMarkers();
            return;
        }
        List<QuestGuideMarker> xaeroMarkers = snapshot.guideMarkers().stream()
                .map(QuestHudData.MarkerState::marker)
                .filter(marker -> marker.markerProvider == LocationGuideMarkerProvider.XAERO_MINIMAP)
                .toList();
        if (!xaeroMarkers.isEmpty()) {
            XaeroMinimapWaypointCompat.sync(xaeroMarkers, snapshot.task().title);
        } else {
            clearExternalMarkers();
        }
    }

    public static boolean hidesBuiltInMarker(QuestGuideMarker marker) {
        return marker != null
                && marker.markerProvider == LocationGuideMarkerProvider.XAERO_MINIMAP
                && XaeroMinimapWaypointCompat.isActiveFor(marker);
    }

    private static void clearExternalMarkers() {
        XaeroMinimapWaypointCompat.clear();
    }
}
