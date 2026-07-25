package com.viscriptquests.gui.blueprint;

import com.lowdragmc.lowdraglib2.configurator.ui.Configurator;
import com.lowdragmc.lowdraglib2.configurator.ui.ValueConfigurator;
import com.lowdragmc.lowdraglib2.gui.ui.utils.UIElementProvider;
import com.lowdragmc.lowdraglib2.gui.ui.elements.SearchComponent;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.IFieldValueConfigurable;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.type.TypeHandle;
import com.lowdragmc.lowdraglib2.utils.search.IResultHandler;
import com.viscript_lib.gui.components.search.BiomeSearchBox;
import com.viscript_lib.gui.components.search.DataPackFileSearchBox;
import com.viscript_lib.gui.components.search.DimensionSearchBox;
import com.viscript_lib.gui.components.search.EntityTypeSearchBox;
import com.viscript_lib.gui.components.search.JsonFileSearchBox;
import com.viscript_lib.gui.components.search.StructureSearchBox;
import com.viscriptquests.compat.ponder.PonderComponentSearch;
import com.viscriptquests.gui.blueprint.data.QuestRegistryId;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.structure.Structure;

import java.util.Objects;
import java.util.function.Function;

/**
 * 节点 option 的资源 ID 搜索框。
 * <p>
 * 具体搜索能力交给 ViScriptLib 1.1.4 的现成 SearchBox，蓝图只负责把选中值
 * 转换为 QuestRegistryId，避免再维护一套维度/群系/结构补全逻辑。
 */
public final class QuestRegistryIdConfigurator {
    private static final String DEFAULT_DIMENSION_ID = "minecraft:overworld";
    private static final String DEFAULT_ENTITY_TYPE_ID = "minecraft:pig";
    private static final String DEFAULT_BIOME_ID = "minecraft:plains";
    private static final String DEFAULT_STRUCTURE_ID = "minecraft:village_plains";
    private static final String DEFAULT_ADVANCEMENT_ID = "minecraft:story/root";
    private static final String DEFAULT_PONDER_COMPONENT_ID = "minecraft:crafting_table";

    private QuestRegistryIdConfigurator() {
    }

    public static Configurator create(IFieldValueConfigurable valueConfigurable, TypeHandle typeHandle) {
        return create("", valueConfigurable, typeHandle);
    }

    public static Configurator create(String name, IFieldValueConfigurable valueConfigurable, TypeHandle typeHandle) {
        if (QuestBlueprintTypes.ENTITY_TYPE_ID.equals(typeHandle)) {
            return createEntityType(name, valueConfigurable, true);
        }
        if (QuestBlueprintTypes.ANY_ENTITY_TYPE_ID.equals(typeHandle)) {
            return createEntityType(name, valueConfigurable, false);
        }
        if (QuestBlueprintTypes.ADVANCEMENT_ID.equals(typeHandle)) {
            return createAdvancement(name, valueConfigurable);
        }
        if (QuestBlueprintTypes.PONDER_COMPONENT_ID.equals(typeHandle)) {
            return createPonderComponent(name, valueConfigurable);
        }
        if (QuestBlueprintTypes.BIOME_ID.equals(typeHandle)) {
            return createBiome(name, valueConfigurable);
        }
        if (QuestBlueprintTypes.STRUCTURE_ID.equals(typeHandle)) {
            return createStructure(name, valueConfigurable);
        }
        return createDimension(name, valueConfigurable);
    }

    private static Configurator createDimension(String name, IFieldValueConfigurable valueConfigurable) {
        return createSearchBoxConfigurator(
                name,
                valueConfigurable,
                DEFAULT_DIMENSION_ID,
                value -> ResourceKey.create(Registries.DIMENSION, idOrDefault(value, DEFAULT_DIMENSION_ID)),
                DimensionSearchBox::getDimensionIdString,
                DimensionSearchBox::new
        );
    }

    private static Configurator createBiome(String name, IFieldValueConfigurable valueConfigurable) {
        return createSearchBoxConfigurator(
                name,
                valueConfigurable,
                DEFAULT_BIOME_ID,
                QuestRegistryIdConfigurator::biomeHolderOf,
                BiomeSearchBox::getBiomeIdString,
                BiomeSearchBox::new
        );
    }

    private static Configurator createStructure(String name, IFieldValueConfigurable valueConfigurable) {
        return createSearchBoxConfigurator(
                name,
                valueConfigurable,
                DEFAULT_STRUCTURE_ID,
                value -> ResourceKey.create(Registries.STRUCTURE, idOrDefault(value, DEFAULT_STRUCTURE_ID)),
                StructureSearchBox::getStructureIdString,
                StructureSearchBox::new
        );
    }

    private static Configurator createEntityType(String name, IFieldValueConfigurable valueConfigurable, boolean livingOnly) {
        return createSearchBoxConfigurator(
                name,
                valueConfigurable,
                DEFAULT_ENTITY_TYPE_ID,
                QuestRegistryIdConfigurator::entityTypeOf,
                EntityTypeSearchBox::getEntityTypeIdString,
                entityType -> {
                    EntityTypeSearchBox searchBox = new EntityTypeSearchBox(entityType);
                    if (livingOnly) {
                        searchBox.onlyLivingEntities(Minecraft.getInstance().level);
                    }
                    return searchBox;
                }
        );
    }

    private static Configurator createAdvancement(String name, IFieldValueConfigurable valueConfigurable) {
        return createSearchBoxConfigurator(
                name,
                valueConfigurable,
                DEFAULT_ADVANCEMENT_ID,
                value -> idOrDefault(value, DEFAULT_ADVANCEMENT_ID),
                JsonFileSearchBox::getFileIdString,
                id -> new DataPackFileSearchBox("advancement", id).searchOnServer()
        );
    }

    private static Configurator createPonderComponent(String name, IFieldValueConfigurable valueConfigurable) {
        return createSearchBoxConfigurator(
                name,
                valueConfigurable,
                DEFAULT_PONDER_COMPONENT_ID,
                value -> idOrDefault(value, DEFAULT_PONDER_COMPONENT_ID),
                ResourceLocation::toString,
                QuestRegistryIdConfigurator::createPonderComponentSearchBox
        );
    }

    private static <T> Configurator createSearchBoxConfigurator(String name,
                                                               IFieldValueConfigurable valueConfigurable,
                                                               String defaultId,
                                                               Function<QuestRegistryId, T> valueToSearchValue,
                                                               Function<T, String> searchValueToId,
                                                               Function<T, SearchComponent<T>> searchBoxFactory) {
        QuestRegistryId initialValue = readValue(valueConfigurable, defaultId);
        T initialSearchValue = valueToSearchValue.apply(initialValue);
        SearchComponent<T> searchBox = searchBoxFactory.apply(initialSearchValue);
        return new VslRegistryIdConfigurator<>(
                name,
                valueConfigurable,
                defaultId,
                valueToSearchValue,
                searchValueToId,
                searchBox
        );
    }

    private static SearchComponent<ResourceLocation> createPonderComponentSearchBox(ResourceLocation defaultValue) {
        SearchComponent<ResourceLocation> searchBox = new SearchComponent<>();
        searchBox.setSearchUI(new SearchComponent.ISearchUI<>() {
            @Override
            public String resultText(ResourceLocation value) {
                return value == null ? "" : value.toString();
            }

            @Override
            public void onResultSelected(ResourceLocation value) {
            }

            @Override
            public void search(String word, IResultHandler<ResourceLocation> searchHandler) {
                PonderComponentSearch.search(word, searchHandler);
            }
        });
        searchBox.setCandidateUIProvider(UIElementProvider.optionalIconText(
                PonderComponentSearch::icon,
                PonderComponentSearch::displayName
        ));
        searchBox.setValue(defaultValue, false);
        return searchBox;
    }

    private static QuestRegistryId readValue(IFieldValueConfigurable valueConfigurable, String defaultId) {
        QuestRegistryId value = registryId(valueConfigurable.getValue());
        if (value != null && !value.value().isBlank()) {
            return value;
        }
        value = registryId(valueConfigurable.getDefaultValue());
        if (value != null && !value.value().isBlank()) {
            return value;
        }
        return new QuestRegistryId(defaultId);
    }

    private static QuestRegistryId registryId(Object value) {
        if (value instanceof QuestRegistryId id) {
            return new QuestRegistryId(id.value());
        }
        if (value instanceof String id) {
            return new QuestRegistryId(id);
        }
        return null;
    }

    private static ResourceLocation idOrDefault(QuestRegistryId value, String defaultId) {
        ResourceLocation id = ResourceLocation.tryParse(value == null ? "" : value.value());
        return id == null ? ResourceLocation.parse(defaultId) : id;
    }

    private static EntityType<?> entityTypeOf(QuestRegistryId value) {
        EntityType<?> entityType = BuiltInRegistries.ENTITY_TYPE.get(idOrDefault(value, DEFAULT_ENTITY_TYPE_ID));
        return entityType == null ? EntityType.PIG : entityType;
    }

    private static QuestRegistryId valueOrDefault(QuestRegistryId value, String defaultId) {
        return value == null || value.value().isBlank() ? new QuestRegistryId(defaultId) : new QuestRegistryId(value.value());
    }

    private static Holder<Biome> biomeHolderOf(QuestRegistryId value) {
        ResourceKey<Biome> biomeKey = ResourceKey.create(Registries.BIOME, idOrDefault(value, DEFAULT_BIOME_ID));
        return BiomeSearchBox.getBiomeHolder(biomeKey);
    }

    private static <T> QuestRegistryId selectedId(T selected, Function<T, String> searchValueToId, String defaultId) {
        if (selected == null) {
            return new QuestRegistryId(defaultId);
        }
        String id = searchValueToId.apply(selected);
        return id == null || id.isBlank() ? new QuestRegistryId(defaultId) : new QuestRegistryId(id);
    }

    private static void configureSearchBox(SearchComponent<?> searchBox) {
        searchBox.searchStyle(style -> {
            style.maxItemCount(8);
            style.scrollerViewHeight(120);
            style.closeAfterSelect(true);
        });
        searchBox.layout(layout -> {
            layout.widthPercent(100);
            layout.height(18);
        });
    }

    private static final class VslRegistryIdConfigurator<T> extends ValueConfigurator<QuestRegistryId> {
        private final String defaultId;
        private final Function<QuestRegistryId, T> valueToSearchValue;
        private final Function<T, String> searchValueToId;
        private final SearchComponent<T> searchBox;

        private VslRegistryIdConfigurator(String name,
                                          IFieldValueConfigurable valueConfigurable,
                                          String defaultId,
                                          Function<QuestRegistryId, T> valueToSearchValue,
                                          Function<T, String> searchValueToId,
                                          SearchComponent<T> searchBox) {
            super(
                    name,
                    () -> readValue(valueConfigurable, defaultId),
                    value -> {
                        valueConfigurable.setValue(valueOrDefault(value, defaultId));
                        valueConfigurable.notifyValueChanged();
                    },
                    new QuestRegistryId(defaultId),
                    valueConfigurable.forceUpdate()
            );
            this.defaultId = defaultId;
            this.valueToSearchValue = valueToSearchValue;
            this.searchValueToId = searchValueToId;
            this.searchBox = searchBox;

            configureSearchBox(searchBox);
            searchBox.setSelected(valueToSearchValue.apply(valueOrDefault(value, defaultId)), false);
            searchBox.setOnValueChanged(selected -> updateValueActively(selectedId(
                    selected,
                    searchValueToId,
                    defaultId
            )));
            addInlineChild(searchBox);

            layout(layout -> layout.widthPercent(100));
            lineContainer.layout(layout -> layout.widthPercent(100));
            inlineContainer.layout(layout -> layout.widthPercent(100));
        }

        @Override
        protected void onValueUpdatePassively(QuestRegistryId value) {
            QuestRegistryId normalizedValue = valueOrDefault(value, defaultId);
            super.onValueUpdatePassively(normalizedValue);

            T selected = valueToSearchValue.apply(normalizedValue);
            if (!Objects.equals(searchBox.getValue(), selected)) {
                searchBox.setSelected(selected, false);
            }
        }
    }
}
