package com.viscriptquests.gui.data;

import com.lowdragmc.lowdraglib2.configurator.IConfigurable;
import com.lowdragmc.lowdraglib2.configurator.accessors.ItemStackAccessor;
import com.lowdragmc.lowdraglib2.configurator.annotation.ConfigSelector;
import com.lowdragmc.lowdraglib2.configurator.annotation.Configurable;
import com.lowdragmc.lowdraglib2.configurator.ui.ConfiguratorGroup;
import com.lowdragmc.lowdraglib2.configurator.ui.StringConfigurator;
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.texture.ItemStackTexture;
import com.lowdragmc.lowdraglib2.gui.texture.SpriteTexture;
import com.lowdragmc.lowdraglib2.syncdata.IPersistedSerializable;
import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.SneakyThrows;
import net.minecraft.network.chat.Component;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

@Data
public class IconTexture implements IConfigurable, IPersistedSerializable {
    @Configurable(name = "viscript_quests.icon.type")
    @ConfigSelector(subConfiguratorBuilder = "iconTypeSubConfiguratorBuilder")
    private IconType type = IconType.ITEM;
    @Persisted
    private ItemStack iconItem = ItemStack.EMPTY;
    @Persisted
    private String iconTexture = "";

    public IGuiTexture getIcon() {
        return switch (type) {
            case ITEM -> new ItemStackTexture(iconItem);
            case TEXTURE -> SpriteTexture.of(iconTexture);
        };
    }

    @SneakyThrows
    private void iconTypeSubConfiguratorBuilder(IconType value, ConfiguratorGroup group) {
        switch (value) {
            case ITEM -> {
                group.addConfigurator(new ItemStackAccessor().create("viscript_quests.icon.iconItem", this::getIconItem, this::setIconItem, true, this.getClass().getDeclaredField("iconItem"), this));
            }
            case TEXTURE -> {
                group.addConfigurator(new StringConfigurator("viscript_quests.icon.iconTexture", this::getIconTexture, this::setIconTexture, iconTexture, true).setResourceLocation(true));
            }
        }
    }

    @Getter
    @AllArgsConstructor
    public enum IconType implements StringRepresentable {
        ITEM(Component.translatable("viscript_quests.icon.type.item").getString()),
        TEXTURE(Component.translatable("viscript_quests.icon.type.texture").getString());

        private final String name;

        @Override
        public @NotNull String getSerializedName() {
            return name;
        }
    }
}
