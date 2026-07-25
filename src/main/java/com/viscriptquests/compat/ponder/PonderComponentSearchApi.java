package com.viscriptquests.compat.ponder;

import net.createmod.catnip.registry.RegisteredObjectsHelper;
import net.createmod.ponder.foundation.PonderIndex;
import net.createmod.ponder.foundation.registration.PonderIndexExclusionHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ItemLike;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

final class PonderComponentSearchApi {
    private PonderComponentSearchApi() {
    }

    static List<ResourceLocation> componentIds() {
        var entries = PonderIndex.getSceneAccess().getRegisteredEntries();
        if (entries.isEmpty()) {
            PonderIndex.registerAll();
            entries = PonderIndex.getSceneAccess().getRegisteredEntries();
        }

        List<Predicate<ItemLike>> exclusions = PonderIndex.streamPlugins()
                .flatMap(PonderIndexExclusionHelper::pluginToExclusions)
                .toList();

        return entries.stream()
                .map(Map.Entry::getKey)
                .distinct()
                .filter(id -> isIncluded(id, exclusions))
                .sorted(Comparator.comparing(ResourceLocation::toString))
                .toList();
    }

    private static boolean isIncluded(ResourceLocation id, List<Predicate<ItemLike>> exclusions) {
        ItemLike item = RegisteredObjectsHelper.getItemOrBlock(id);
        return item != null && exclusions.stream().noneMatch(predicate -> predicate.test(item));
    }
}
