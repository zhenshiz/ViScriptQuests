package com.viscriptquests.quest.data.task;

import com.mojang.datafixers.util.Pair;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegister;
import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted;
import com.viscriptquests.quest.data.DisplayIcon;
import com.viscriptquests.quest.data.LocationGuideMarkerProvider;
import com.viscriptquests.quest.data.LocationTargetType;
import com.viscriptquests.quest.data.runtime.QuestGuideMarker;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.Vec3;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

// 到达指定位置的目标，同时为客户端提供任务导航标记。
@LDLRegister(name = "location_task", registry = ITask.ID)
public class LocationTask extends ITask {
    private static final int BIOME_SEARCH_RADIUS = 6400;
    private static final int BIOME_SEARCH_HORIZONTAL_STEP = 32;
    private static final int BIOME_SEARCH_VERTICAL_STEP = 64;
    private static final int STRUCTURE_SEARCH_RADIUS = 100;
    private static final long FAILED_SEARCH_RETRY_TICKS = 200L;
    private static final Set<MapColor> SURFACE_MATERIALS = Set.of(
            MapColor.GRASS,
            MapColor.SAND,
            MapColor.SNOW,
            MapColor.CLAY,
            MapColor.DIRT,
            MapColor.STONE,
            MapColor.PODZOL,
            MapColor.NETHER,
            MapColor.TERRACOTTA_WHITE,
            MapColor.TERRACOTTA_ORANGE,
            MapColor.TERRACOTTA_MAGENTA,
            MapColor.TERRACOTTA_LIGHT_BLUE,
            MapColor.TERRACOTTA_YELLOW,
            MapColor.TERRACOTTA_LIGHT_GREEN,
            MapColor.TERRACOTTA_PINK,
            MapColor.TERRACOTTA_GRAY,
            MapColor.TERRACOTTA_LIGHT_GRAY,
            MapColor.TERRACOTTA_CYAN,
            MapColor.TERRACOTTA_PURPLE,
            MapColor.TERRACOTTA_BLUE,
            MapColor.TERRACOTTA_BROWN,
            MapColor.TERRACOTTA_GREEN,
            MapColor.TERRACOTTA_RED,
            MapColor.TERRACOTTA_BLACK,
            MapColor.CRIMSON_NYLIUM,
            MapColor.WARPED_NYLIUM,
            MapColor.DEEPSLATE
    );
    private static final Map<String, TargetCacheEntry> TARGET_CACHE = new ConcurrentHashMap<>();

    @Persisted
    public LocationTargetType targetType = LocationTargetType.COORDINATES;
    @Persisted
    public String dimension = "minecraft:overworld";
    @Persisted
    public double x = 0.0;
    @Persisted
    public double y = 64.0;
    @Persisted
    public double z = 0.0;
    @Persisted
    public String biomeId = "minecraft:plains";
    @Persisted
    public String structureId = "minecraft:village_plains";
    @Persisted
    public double arrivalRadius = 3.0;
    @Persisted
    public LocationGuideMarkerProvider markerProvider = LocationGuideMarkerProvider.BUILT_IN;
    @Persisted
    public String markerLabel = "";
    @Persisted
    public DisplayIcon markerIcon = DisplayIcon.item(Items.COMPASS.getDefaultInstance());
    @Persisted
    public int markerColor = 0xFFD8C7FF;

    @Override
    public boolean checkCompletion(ServerPlayer player) {
        if (player == null) {
            return false;
        }
        return switch (targetTypeOrDefault()) {
            case COORDINATES -> checkCoordinateCompletion(player);
            case BIOME -> isPlayerInTargetBiome(player);
            case STRUCTURE -> isPlayerInTargetStructure(player);
        };
    }

    @Override
    public boolean onComplete(ServerPlayer player) {
        return true;
    }

    @Override
    protected Component getDefaultTaskHint() {
        if (targetTypeOrDefault() == LocationTargetType.COORDINATES) {
            return Component.translatable("viscript_quests.task_hint.location_task", x, y, z);
        }
        return Component.translatable(targetSearchHintKey(), targetId());
    }

    @Override
    protected Component getDefaultTaskHint(ServerPlayer player,
                                           Map<String, com.viscriptquests.quest.data.QuestVariableValue> questVariables) {
        return getDefaultTaskHint();
    }

    @Override
    public DisplayIcon getDisplayIcon() {
        return markerIcon == null ? DisplayIcon.item(new ItemStack(Items.COMPASS)) : markerIcon.copy();
    }

    @Override
    public QuestGuideMarker getGuideMarker(ServerPlayer player) {
        ResolvedLocation target = resolveTarget(player);
        if (target == null) {
            return QuestGuideMarker.disabled();
        }
        return QuestGuideMarker.position(
                target.dimension(),
                target.position(),
                markerLabel,
                getDisplayIcon(),
                markerColor,
                markerArrivalRadius(),
                markerProviderOrDefault()
        );
    }

    private boolean checkCoordinateCompletion(ServerPlayer player) {
        ResolvedLocation target = resolveTarget(player);
        if (target == null || !player.level().dimension().location().equals(target.dimension())) {
            return false;
        }
        double radius = arrivalRadiusOrDefault();
        return player.position().distanceToSqr(target.position()) <= radius * radius;
    }

    private boolean isPlayerInTargetBiome(ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel level)) {
            return false;
        }
        ResourceLocation location = ResourceLocation.tryParse(targetId());
        if (location == null) {
            return false;
        }
        ResourceKey<Biome> key = ResourceKey.create(Registries.BIOME, location);
        return level.getBiome(player.blockPosition()).is(key);
    }

    private boolean isPlayerInTargetStructure(ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel level)) {
            return false;
        }
        Optional<Holder.Reference<Structure>> holder = structureHolder(level);
        return holder.isPresent()
                && level.structureManager()
                .getStructureWithPieceAt(player.blockPosition(), HolderSet.direct(holder.get()))
                .isValid();
    }

    private ResolvedLocation resolveTarget(ServerPlayer player) {
        if (targetTypeOrDefault() == LocationTargetType.COORDINATES) {
            return new ResolvedLocation(targetDimension(), targetPosition(), x, y, z);
        }
        if (player == null || !(player.level() instanceof ServerLevel level)) {
            return null;
        }
        String cacheKey = cacheKey(player, level);
        long now = level.getGameTime();
        TargetCacheEntry cached = TARGET_CACHE.get(cacheKey);
        if (cached != null && (cached.location() != null || now - cached.searchedAtGameTime() < FAILED_SEARCH_RETRY_TICKS)) {
            return cached.location();
        }
        ResolvedLocation located = switch (targetTypeOrDefault()) {
            case BIOME -> locateBiome(level, player.blockPosition());
            case STRUCTURE -> locateStructure(level, player.blockPosition());
            case COORDINATES -> new ResolvedLocation(targetDimension(), targetPosition(), x, y, z);
        };
        TARGET_CACHE.put(cacheKey, new TargetCacheEntry(located, now));
        return located;
    }

    private ResolvedLocation locateBiome(ServerLevel level, BlockPos origin) {
        ResourceLocation location = ResourceLocation.tryParse(targetId());
        if (location == null) {
            return null;
        }
        ResourceKey<Biome> key = ResourceKey.create(Registries.BIOME, location);
        if (level.registryAccess().registryOrThrow(Registries.BIOME).getHolder(key).isEmpty()) {
            return null;
        }
        Pair<BlockPos, Holder<Biome>> pair = level.findClosestBiome3d(
                holder -> holder.is(key),
                origin,
                BIOME_SEARCH_RADIUS,
                BIOME_SEARCH_HORIZONTAL_STEP,
                BIOME_SEARCH_VERTICAL_STEP
        );
        return pair == null ? null : resolveBiomeLocation(level, pair.getFirst());
    }

    private ResolvedLocation locateStructure(ServerLevel level, BlockPos origin) {
        if (!level.getServer().getWorldData().worldGenOptions().generateStructures()) {
            return null;
        }
        Optional<Holder.Reference<Structure>> holder = structureHolder(level);
        if (holder.isEmpty()) {
            return null;
        }
        Pair<BlockPos, Holder<Structure>> pair = level.getChunkSource()
                .getGenerator()
                .findNearestMapStructure(level, HolderSet.direct(holder.get()), origin, STRUCTURE_SEARCH_RADIUS, false);
        return pair == null ? null : resolveStructureLocation(level, pair);
    }

    private ResolvedLocation resolveBiomeLocation(ServerLevel level, BlockPos located) {
        BlockPos standable = nearestStandableNearY(level, located, 2);
        return toResolvedLocation(level, standable == null ? located : standable);
    }

    private ResolvedLocation resolveStructureLocation(ServerLevel level, Pair<BlockPos, Holder<Structure>> located) {
        Optional<BlockPos> structureReference = structureReferencePosition(level, located.getSecond(), located.getFirst());
        return resolveSurfaceLocation(level, structureReference.orElse(located.getFirst()));
    }

    private ResolvedLocation resolveSurfaceLocation(ServerLevel level, BlockPos located) {
        BlockPos surface = getSurfaceBlockPos(level, located);
        BlockPos standable = nearestStandableAround(level, surface, 2);
        BlockPos target = standable == null ? surface : standable;
        return toResolvedLocation(level, target);
    }

    private ResolvedLocation toResolvedLocation(ServerLevel level, BlockPos target) {
        Vec3 position = Vec3.atBottomCenterOf(target);
        return new ResolvedLocation(level.dimension().location(), position,
                target.getX(), target.getY(), target.getZ());
    }

    private Optional<BlockPos> structureReferencePosition(ServerLevel level, Holder<Structure> holder, BlockPos located) {
        Structure structure = holder.value();
        StructureStart nearestStart = null;
        double nearestDistance = Double.MAX_VALUE;
        for (StructureStart start : level.structureManager().startsForStructure(new ChunkPos(located),
                candidate -> candidate == structure || candidate.equals(structure))) {
            if (!start.isValid()) {
                continue;
            }
            BoundingBox boundingBox = start.getBoundingBox();
            BlockPos center = boundingBox.getCenter();
            double distance = center.distSqr(located);
            if (distance < nearestDistance) {
                nearestDistance = distance;
                nearestStart = start;
            }
        }
        if (nearestStart == null) {
            return Optional.empty();
        }
        BoundingBox boundingBox = nearestStart.getBoundingBox();
        BlockPos center = boundingBox.getCenter();
        int y = Math.max(level.getMinBuildHeight() + 1, Math.min(level.getMaxBuildHeight() - 2, boundingBox.minY()));
        return Optional.of(new BlockPos(center.getX(), y, center.getZ()));
    }

    public static BlockPos getSurfaceBlockPos(ServerLevel serverLevel, BlockPos pos) {
        return getSurfaceBlockPos(serverLevel, pos.getX(), pos.getZ());
    }

    public static BlockPos getSurfaceBlockPos(ServerLevel serverLevel, int x, int z) {
        int maxY = serverLevel.getMaxBuildHeight() - 1;
        int minY = serverLevel.getMinBuildHeight();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos(x, maxY, z);
        for (int y = maxY; y > minY; y--) {
            pos.set(x, y, z);
            BlockState blockState = serverLevel.getBlockState(pos);
            MapColor mapColor = blockState.getMapColor(serverLevel, pos);
            if (blockState.getLightBlock(serverLevel, pos) >= 15 || SURFACE_MATERIALS.contains(mapColor)) {
                return pos.above().immutable();
            }
        }

        return new BlockPos(x, maxY, z);
    }

    private static BlockPos nearestStandableAround(ServerLevel level, BlockPos origin, int maxRadius) {
        if (isStandable(level, origin)) {
            return origin;
        }
        for (int radius = 1; radius <= maxRadius; radius++) {
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (Math.abs(dx) != radius && Math.abs(dz) != radius) {
                        continue;
                    }
                    BlockPos candidate = getSurfaceBlockPos(level, origin.getX() + dx, origin.getZ() + dz);
                    if (isStandable(level, candidate)) {
                        return candidate;
                    }
                }
            }
        }
        return null;
    }

    private static BlockPos nearestStandableNearY(ServerLevel level, BlockPos origin, int maxRadius) {
        BlockPos direct = standableNearY(level, origin.getX(), origin.getY(), origin.getZ());
        if (direct != null) {
            return direct;
        }
        for (int radius = 1; radius <= maxRadius; radius++) {
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (Math.abs(dx) != radius && Math.abs(dz) != radius) {
                        continue;
                    }
                    BlockPos candidate = standableNearY(level, origin.getX() + dx, origin.getY(), origin.getZ() + dz);
                    if (candidate != null) {
                        return candidate;
                    }
                }
            }
        }
        return null;
    }

    private static BlockPos standableNearY(ServerLevel level, int x, int y, int z) {
        int maxY = Math.min(level.getMaxBuildHeight() - 2, y + 8);
        int minY = Math.max(level.getMinBuildHeight() + 1, y - 48);
        for (int candidateY = maxY; candidateY >= minY; candidateY--) {
            BlockPos pos = new BlockPos(x, candidateY, z);
            if (isStandable(level, pos)) {
                return pos;
            }
        }
        return null;
    }

    private static boolean isStandable(ServerLevel level, BlockPos feetPos) {
        BlockState floor = level.getBlockState(feetPos.below());
        BlockState feet = level.getBlockState(feetPos);
        BlockState head = level.getBlockState(feetPos.above());
        return !floor.getCollisionShape(level, feetPos.below()).isEmpty()
                && feet.getCollisionShape(level, feetPos).isEmpty()
                && head.getCollisionShape(level, feetPos.above()).isEmpty();
    }

    private String cacheKey(ServerPlayer player, ServerLevel level) {
        return player.getUUID()
                + "|" + stepId
                + "|" + objectiveId
                + "|" + targetTypeOrDefault()
                + "|" + targetId()
                + "|" + level.dimension().location();
    }

    private ResourceLocation targetDimension() {
        ResourceLocation dimensionId = ResourceLocation.tryParse(dimension);
        return dimensionId == null ? Level.OVERWORLD.location() : dimensionId;
    }

    private Vec3 targetPosition() {
        return new Vec3(x + 0.5, y + 0.5, z + 0.5);
    }

    private LocationTargetType targetTypeOrDefault() {
        return targetType == null ? LocationTargetType.COORDINATES : targetType;
    }

    private LocationGuideMarkerProvider markerProviderOrDefault() {
        return markerProvider == null ? LocationGuideMarkerProvider.BUILT_IN : markerProvider;
    }

    private double arrivalRadiusOrDefault() {
        return Double.isFinite(arrivalRadius) ? Math.max(0.0, arrivalRadius) : 3.0;
    }

    private double markerArrivalRadius() {
        return targetTypeOrDefault() == LocationTargetType.COORDINATES ? arrivalRadiusOrDefault() : 0.0;
    }

    private Optional<Holder.Reference<Structure>> structureHolder(ServerLevel level) {
        ResourceLocation location = ResourceLocation.tryParse(targetId());
        if (location == null) {
            return Optional.empty();
        }
        Registry<Structure> registry = level.registryAccess().registryOrThrow(Registries.STRUCTURE);
        return registry.getHolder(ResourceKey.create(Registries.STRUCTURE, location));
    }

    private String targetId() {
        return switch (targetTypeOrDefault()) {
            case BIOME -> biomeId == null || biomeId.isBlank() ? "minecraft:plains" : biomeId.trim();
            case STRUCTURE -> structureId == null || structureId.isBlank() ? "minecraft:village_plains" : structureId.trim();
            case COORDINATES -> dimension == null || dimension.isBlank() ? "minecraft:overworld" : dimension.trim();
        };
    }

    private String targetSearchHintKey() {
        return switch (targetTypeOrDefault()) {
            case BIOME -> "viscript_quests.task_hint.location_task.biome";
            case STRUCTURE -> "viscript_quests.task_hint.location_task.structure";
            case COORDINATES -> "viscript_quests.task_hint.location_task";
        };
    }

    private record TargetCacheEntry(ResolvedLocation location, long searchedAtGameTime) {
    }

    private record ResolvedLocation(ResourceLocation dimension, Vec3 position, double x, double y, double z) {
    }
}
