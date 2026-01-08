package com.viscriptquests.util;

import com.lowdragmc.lowdraglib2.Platform;
import com.viscriptquests.gui.data.Quest;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.HashMap;
import java.util.Map;

@ParametersAreNonnullByDefault
public class QuestHelper {
    private final static Map<ResourceLocation, Quest> CACHE = new HashMap<>();
    public static final String SHOP_PATH = "quest/";

    public static int clearCache() {
        var count = CACHE.size();
        CACHE.clear();
        return count;
    }

    @Nullable
    public static Quest getQuest(ResourceLocation shopLocation) {
        return getQuest(shopLocation, true);
    }


    @Nullable
    public static Quest getQuest(ResourceLocation shopLocation, boolean useCache) {
        return useCache ? CACHE.computeIfAbsent(shopLocation, location -> loadQuest(shopLocation)) : loadQuest(shopLocation);
    }

    public static Quest loadQuest(ResourceLocation shopLocation) {
        ResourceLocation resourceLocation = ResourceLocation.fromNamespaceAndPath(shopLocation.getNamespace(), SHOP_PATH + shopLocation.getPath() + Quest.SUFFIX);
        try (var inputStream = Minecraft.getInstance().getResourceManager().open(resourceLocation)) {
            var tag = NbtIo.readCompressed(inputStream, NbtAccounter.unlimitedHeap());
            var quest = new Quest();
            quest.setQuestLocation(shopLocation);
            quest.deserializeNBT(Platform.getFrozenRegistry(), tag);
            return quest;
        } catch (Exception ignored) {
            return null;
        }
    }
}
