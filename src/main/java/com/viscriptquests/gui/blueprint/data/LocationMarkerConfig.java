package com.viscriptquests.gui.blueprint.data;

import com.lowdragmc.lowdraglib2.syncdata.IPersistedSerializable;
import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted;
import com.viscriptquests.quest.data.DisplayIcon;
import com.viscriptquests.quest.data.LocationGuideMarkerProvider;
import net.minecraft.world.item.Items;

// 到达位置节点的导航标配置。外部小地图模式只读取名称和颜色，内置 HUD 模式额外读取图标。
public class LocationMarkerConfig implements IPersistedSerializable {
    @Persisted
    public LocationGuideMarkerProvider provider = LocationGuideMarkerProvider.BUILT_IN;
    @Persisted
    public String label = "";
    @Persisted
    public DisplayIcon icon = defaultIcon();
    @Persisted
    public int color = 0xFFD8C7FF;

    public static LocationMarkerConfig defaults() {
        return new LocationMarkerConfig();
    }

    public LocationMarkerConfig copy() {
        LocationMarkerConfig copy = new LocationMarkerConfig();
        copy.provider = providerOrDefault();
        copy.label = label();
        copy.icon = icon();
        copy.color = color;
        return copy;
    }

    public void ensureDefaults() {
        if (provider == null) {
            provider = LocationGuideMarkerProvider.BUILT_IN;
        }
        if (label == null) {
            label = "";
        }
        if (icon == null) {
            icon = defaultIcon();
        }
    }

    public LocationGuideMarkerProvider providerOrDefault() {
        return provider == null ? LocationGuideMarkerProvider.BUILT_IN : provider;
    }

    public String label() {
        return label == null ? "" : label;
    }

    public DisplayIcon icon() {
        return icon == null ? defaultIcon() : icon.copy();
    }

    private static DisplayIcon defaultIcon() {
        return DisplayIcon.item(Items.COMPASS.getDefaultInstance());
    }
}
