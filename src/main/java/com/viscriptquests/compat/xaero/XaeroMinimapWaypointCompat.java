package com.viscriptquests.compat.xaero;

import com.lowdragmc.lowdraglib2.Platform;
import com.viscriptquests.quest.data.runtime.QuestGuideMarker;

import java.util.Collection;

// 外层只负责可选模组存在性判断，真实 API 调用放在 XaeroMinimapWaypointApi 中，避免未安装 Xaero 时加载其类。
public final class XaeroMinimapWaypointCompat {
    private static final String MOD_ID = "xaerominimap";

    private XaeroMinimapWaypointCompat() {
    }

    public static boolean isLoaded() {
        return Platform.isModLoaded(MOD_ID);
    }

    public static boolean isActiveFor(QuestGuideMarker marker) {
        return isLoaded() && XaeroMinimapWaypointApi.isActiveFor(marker);
    }

    public static void sync(QuestGuideMarker marker, String fallbackLabel) {
        if (isLoaded()) {
            XaeroMinimapWaypointApi.sync(marker, fallbackLabel);
        }
    }

    public static void sync(Collection<QuestGuideMarker> markers, String fallbackLabel) {
        if (isLoaded()) {
            XaeroMinimapWaypointApi.sync(markers, fallbackLabel);
        }
    }

    public static void clear() {
        if (isLoaded()) {
            XaeroMinimapWaypointApi.clear();
        }
    }
}
