package com.viscriptquests.quest.data.runtime;

import com.lowdragmc.lowdraglib2.syncdata.IPersistedSerializable;
import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted;
import com.viscriptquests.quest.data.DisplayIcon;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

// 当前追踪目标的导航标记数据，只由服务端任务进度同步给对应玩家客户端。
public class QuestGuideMarker implements IPersistedSerializable {
    @Persisted
    public boolean enabled = false;
    @Persisted
    public String dimension = "";
    @Persisted
    public double x = 0.0;
    @Persisted
    public double y = 0.0;
    @Persisted
    public double z = 0.0;
    @Persisted
    public String label = "";
    @Persisted
    public DisplayIcon icon = new DisplayIcon();
    @Persisted
    public int color = 0xFFD8C7FF;
    @Persisted
    public boolean showLabel = true;
    @Persisted
    public boolean showDistance = true;
    @Persisted
    public boolean hideWhenReached = true;
    @Persisted
    public double arrivalRadius = 2.0;

    public static QuestGuideMarker disabled() {
        return new QuestGuideMarker();
    }

    public static QuestGuideMarker position(ResourceLocation dimension, Vec3 position, String label,
                                            DisplayIcon icon, int color, double arrivalRadius) {
        QuestGuideMarker marker = new QuestGuideMarker();
        marker.enabled = true;
        marker.dimension = dimension == null ? "" : dimension.toString();
        marker.x = position == null ? 0.0 : position.x;
        marker.y = position == null ? 0.0 : position.y;
        marker.z = position == null ? 0.0 : position.z;
        marker.label = label == null ? "" : label;
        marker.icon = icon == null ? new DisplayIcon() : icon.copy();
        marker.color = color;
        marker.arrivalRadius = Math.max(0.0, arrivalRadius);
        return marker;
    }

    public boolean isEnabled() {
        return enabled && dimension != null && !dimension.isBlank();
    }

    public boolean shouldShow(ServerPlayer player) {
        if (!enabled || player == null || dimension == null || dimension.isBlank()) {
            return false;
        }
        if (!player.level().dimension().location().toString().equals(dimension)) {
            return false;
        }
        return !hideWhenReached || player.position().distanceToSqr(position()) > arrivalRadius * arrivalRadius;
    }

    public Vec3 position() {
        return new Vec3(x, y, z);
    }

    public QuestGuideMarker copy() {
        QuestGuideMarker marker = new QuestGuideMarker();
        marker.enabled = enabled;
        marker.dimension = dimension;
        marker.x = x;
        marker.y = y;
        marker.z = z;
        marker.label = label;
        marker.icon = icon == null ? new DisplayIcon() : icon.copy();
        marker.color = color;
        marker.showLabel = showLabel;
        marker.showDistance = showDistance;
        marker.hideWhenReached = hideWhenReached;
        marker.arrivalRadius = arrivalRadius;
        return marker;
    }
}
