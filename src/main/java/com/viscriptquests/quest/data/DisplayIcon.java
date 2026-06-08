package com.viscriptquests.quest.data;

import com.lowdragmc.lowdraglib2.Platform;
import com.lowdragmc.lowdraglib2.configurator.IConfigurable;
import com.lowdragmc.lowdraglib2.configurator.accessors.ItemStackAccessor;
import com.lowdragmc.lowdraglib2.configurator.annotation.ConfigSelector;
import com.lowdragmc.lowdraglib2.configurator.annotation.Configurable;
import com.lowdragmc.lowdraglib2.configurator.ui.ConfiguratorGroup;
import com.lowdragmc.lowdraglib2.configurator.ui.StringConfigurator;
import com.lowdragmc.lowdraglib2.syncdata.IPersistedSerializable;
import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib2.utils.PersistedParser;
import com.mojang.serialization.Codec;
import com.viscript_lib.util.CodecUtil;
import io.netty.buffer.ByteBuf;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.SneakyThrows;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.Tag;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

@Data
public class DisplayIcon implements IPersistedSerializable, IConfigurable {
    public static final Codec<DisplayIcon> CODEC = PersistedParser.createCodec(DisplayIcon::new);
    public static final StreamCodec<ByteBuf, DisplayIcon> STREAM_CODEC = PersistedParser.createStreamCodec(DisplayIcon::new);

    @Configurable(name = "viscript_quests.displayIcon.type")
    @ConfigSelector(subConfiguratorBuilder = "iconTypeSubConfiguratorBuilder")
    private IconType type = IconType.ITEM;
    @Persisted
    private ItemStack itemStack = ItemStack.EMPTY;
    @Persisted
    private String texture = "";

    public static DisplayIcon item(String itemId) {
        ResourceLocation id = ResourceLocation.tryParse(itemId == null ? "" : itemId);
        if (id == null) {
            return item(ItemStack.EMPTY);
        }
        return item(BuiltInRegistries.ITEM.getOptional(id)
                .map(ItemStack::new)
                .orElse(ItemStack.EMPTY));
    }

    public static DisplayIcon item(ItemStack itemStack) {
        DisplayIcon icon = new DisplayIcon();
        icon.type = IconType.ITEM;
        icon.itemStack = itemStack == null ? ItemStack.EMPTY : itemStack.copy();
        return icon;
    }

    public static DisplayIcon texture(String texture) {
        DisplayIcon icon = new DisplayIcon();
        icon.type = IconType.TEXTURE;
        icon.texture = texture == null ? "" : texture.trim();
        return icon;
    }

    public DisplayIcon copy() {
        Tag tag = CodecUtil.serializeNBT(CODEC, this, Platform.getFrozenRegistry());
        return CodecUtil.deserializeNBT(CODEC, tag, Platform.getFrozenRegistry());
    }

    public ItemStack renderItemStack() {
        if (itemStack == null || itemStack.isEmpty()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = itemStack.copy();
        stack.setCount(1);
        return stack;
    }

    public boolean isTexture() {
        return type == IconType.TEXTURE;
    }

    public String itemId() {
        if (itemStack == null || itemStack.isEmpty()) {
            return "";
        }
        return BuiltInRegistries.ITEM.getKey(itemStack.getItem()).toString();
    }

    @SneakyThrows
    private void iconTypeSubConfiguratorBuilder(IconType value, ConfiguratorGroup group) {
        switch (value) {
            case ITEM -> {
                group.addConfigurator(new ItemStackAccessor().create("viscript_quests.displayIcon.itemStack", this::getItemStack, this::setItemStack, true, this.getClass().getDeclaredField("itemStack"), this));
            }
            case TEXTURE -> {
                group.addConfigurator(new StringConfigurator("viscript_quests.displayIcon.texture", this::getTexture, this::setTexture, texture, true).setResourceLocation(true));
            }
        }
    }

    @Getter
    @AllArgsConstructor
    public enum IconType implements StringRepresentable {
        ITEM("viscript_quests.displayIcon.type.item"),
        TEXTURE("viscript_quests.displayIcon.type.texture");

        private final String name;

        @Override
        public @NotNull String getSerializedName() {
            return name;
        }
    }
}
