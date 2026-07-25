package com.viscriptquests.compat.ponder;

import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.texture.ItemStackTexture;
import com.lowdragmc.lowdraglib2.utils.search.IResultHandler;
import com.viscriptquests.ViScriptQuests;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

public final class PonderComponentSearch {
    private static final int RESULT_LIMIT = 120;

    private PonderComponentSearch() {
    }

    public static void search(String word, IResultHandler<ResourceLocation> searchHandler) {
        String lowerWord = word == null ? "" : word.toLowerCase(Locale.ROOT).trim();
        componentIds().stream()
                .filter(id -> matches(id, lowerWord))
                .limit(RESULT_LIMIT)
                .forEach(searchHandler::acceptResult);
    }

    public static Component displayName(ResourceLocation id) {
        ItemStack stack = stackOf(id);
        if (stack.isEmpty()) {
            return Component.literal(id == null ? "" : id.toString());
        }
        return stack.getHoverName();
    }

    public static IGuiTexture icon(ResourceLocation id) {
        ItemStack stack = stackOf(id);
        return stack.isEmpty() ? IGuiTexture.EMPTY : new ItemStackTexture(stack);
    }

    private static List<ResourceLocation> componentIds() {
        if (PonderCompat.isLoaded()) {
            try {
                List<ResourceLocation> ponderIds = PonderComponentSearchApi.componentIds();
                if (!ponderIds.isEmpty()) {
                    return ponderIds;
                }
            } catch (Throwable e) {
                ViScriptQuests.LOGGER.warn("Failed to read Ponder component index, falling back to item/block ids", e);
            }
        }
        return fallbackItemAndBlockIds();
    }

    private static boolean matches(ResourceLocation id, String lowerWord) {
        if (lowerWord.isBlank()) {
            return true;
        }
        String idString = id.toString();
        if (idString.toLowerCase(Locale.ROOT).contains(lowerWord)
                || id.getPath().toLowerCase(Locale.ROOT).contains(lowerWord)) {
            return true;
        }
        return displayName(id).getString().toLowerCase(Locale.ROOT).contains(lowerWord);
    }

    private static List<ResourceLocation> fallbackItemAndBlockIds() {
        LinkedHashSet<ResourceLocation> ids = new LinkedHashSet<>();
        BuiltInRegistries.ITEM.keySet().stream()
                .filter(Objects::nonNull)
                .forEach(ids::add);
        BuiltInRegistries.BLOCK.keySet().stream()
                .filter(Objects::nonNull)
                .forEach(ids::add);
        return ids.stream()
                .sorted(Comparator.comparing(ResourceLocation::toString))
                .toList();
    }

    private static ItemStack stackOf(ResourceLocation id) {
        if (id == null) {
            return ItemStack.EMPTY;
        }
        Optional<Item> item = BuiltInRegistries.ITEM.getOptional(id);
        if (item.isPresent()) {
            return new ItemStack(item.get());
        }
        Optional<Block> block = BuiltInRegistries.BLOCK.getOptional(id);
        return block.map(value -> new ItemStack(value.asItem()))
                .filter(stack -> !stack.isEmpty())
                .orElse(ItemStack.EMPTY);
    }
}
