package com.viscriptquests.gui.blueprint.data;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.LevelStem;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.stream.Stream;

/**
 * 蓝图编辑器补全用的服务端注册表快照。
 */
public final class QuestBlueprintRegistryCache {
    private static final String DIMENSION_IDS_TAG = "dimension_ids";
    private static final List<String> FALLBACK_DIMENSION_IDS = List.of(
            Level.OVERWORLD.location().toString(),
            Level.NETHER.location().toString(),
            Level.END.location().toString()
    );

    private static volatile List<String> dimensionIds = FALLBACK_DIMENSION_IDS;

    public static CompoundTag createServerPayload(MinecraftServer server) {
        CompoundTag tag = new CompoundTag();
        putStringList(tag, DIMENSION_IDS_TAG, collectServerDimensionIds(server));
        return tag;
    }

    public static void updateClientCache(CompoundTag tag) {
        setDimensionIds(readStringList(tag, DIMENSION_IDS_TAG));
    }

    public static Stream<String> dimensionIds() {
        return dimensionIds.stream();
    }

    private static List<String> collectServerDimensionIds(MinecraftServer server) {
        if (server == null) {
            return FALLBACK_DIMENSION_IDS;
        }

        List<String> ids = server.registryAccess()
                .registry(Registries.LEVEL_STEM)
                .map(QuestBlueprintRegistryCache::dimensionIdsFromRegistry)
                .orElseGet(() -> server.levelKeys().stream()
                        .map(ResourceKey::location)
                        .map(ResourceLocation::toString)
                        .toList());
        return normalizeIds(ids);
    }

    private static List<String> dimensionIdsFromRegistry(Registry<LevelStem> registry) {
        return registry.registryKeySet().stream()
                .map(ResourceKey::location)
                .map(ResourceLocation::toString)
                .toList();
    }

    private static void setDimensionIds(Collection<String> ids) {
        dimensionIds = normalizeIds(ids);
    }

    private static List<String> normalizeIds(Collection<String> ids) {
        LinkedHashSet<String> normalized = new LinkedHashSet<>();
        for (String id : ids) {
            if (id != null && !id.isBlank()) {
                normalized.add(id.trim());
            }
        }
        if (normalized.isEmpty()) {
            normalized.addAll(FALLBACK_DIMENSION_IDS);
        }
        return List.copyOf(normalized.stream().sorted().toList());
    }

    private static void putStringList(CompoundTag tag, String key, Collection<String> values) {
        ListTag list = new ListTag();
        for (String value : values) {
            list.add(StringTag.valueOf(value));
        }
        tag.put(key, list);
    }

    private static List<String> readStringList(CompoundTag tag, String key) {
        ListTag list = tag.getList(key, Tag.TAG_STRING);
        List<String> values = new ArrayList<>(list.size());
        for (int i = 0; i < list.size(); i++) {
            values.add(list.getString(i));
        }
        return values;
    }
}
