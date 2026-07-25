package com.viscriptquests.gui.blueprint.data;

import com.lowdragmc.lowdraglib2.syncdata.IPersistedSerializable;
import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted;
import com.viscriptquests.quest.data.LocationTargetType;

// 到达位置节点的复合目标配置。保持为一个稳定 option，避免切换模式时 LDLib2 旧组件残留。
public class LocationTargetConfig implements IPersistedSerializable {
    @Persisted
    public LocationTargetType targetType = LocationTargetType.COORDINATES;
    @Persisted
    public QuestRegistryId dimension = new QuestRegistryId("minecraft:overworld");
    @Persisted
    public double x = 0.0;
    @Persisted
    public double y = 64.0;
    @Persisted
    public double z = 0.0;
    @Persisted
    public double arrivalRadius = 3.0;
    @Persisted
    public QuestRegistryId biomeId = new QuestRegistryId("minecraft:plains");
    @Persisted
    public QuestRegistryId structureId = new QuestRegistryId("minecraft:village_plains");

    public static LocationTargetConfig defaults() {
        return new LocationTargetConfig();
    }

    public LocationTargetConfig copy() {
        LocationTargetConfig copy = new LocationTargetConfig();
        copy.targetType = targetTypeOrDefault();
        copy.dimension = new QuestRegistryId(dimensionId());
        copy.x = x;
        copy.y = y;
        copy.z = z;
        copy.arrivalRadius = arrivalRadius();
        copy.biomeId = new QuestRegistryId(biomeId());
        copy.structureId = new QuestRegistryId(structureId());
        return copy;
    }

    public void ensureDefaults() {
        if (targetType == null) {
            targetType = LocationTargetType.COORDINATES;
        }
        if (dimension == null) {
            dimension = new QuestRegistryId("minecraft:overworld");
        }
        if (biomeId == null) {
            biomeId = new QuestRegistryId("minecraft:plains");
        }
        if (structureId == null) {
            structureId = new QuestRegistryId("minecraft:village_plains");
        }
        if (!Double.isFinite(arrivalRadius)) {
            arrivalRadius = 3.0;
        } else if (arrivalRadius < 0.0) {
            arrivalRadius = 0.0;
        }
    }

    public LocationTargetType targetTypeOrDefault() {
        return targetType == null ? LocationTargetType.COORDINATES : targetType;
    }

    public String dimensionId() {
        return dimension == null || dimension.value().isBlank() ? "minecraft:overworld" : dimension.value();
    }

    public String biomeId() {
        return biomeId == null || biomeId.value().isBlank() ? "minecraft:plains" : biomeId.value();
    }

    public String structureId() {
        return structureId == null || structureId.value().isBlank() ? "minecraft:village_plains" : structureId.value();
    }

    public double arrivalRadius() {
        return Double.isFinite(arrivalRadius) ? Math.max(0.0, arrivalRadius) : 3.0;
    }
}
