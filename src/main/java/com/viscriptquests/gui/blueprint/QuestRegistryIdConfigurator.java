package com.viscriptquests.gui.blueprint;

import com.lowdragmc.lowdraglib2.configurator.ui.Configurator;
import com.lowdragmc.lowdraglib2.configurator.ui.SearchComponentConfigurator;
import com.lowdragmc.lowdraglib2.gui.ui.data.Horizontal;
import com.lowdragmc.lowdraglib2.gui.ui.data.TextWrap;
import com.lowdragmc.lowdraglib2.gui.ui.data.Vertical;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.gui.ui.utils.UIElementProvider;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.IFieldValueConfigurable;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.type.TypeHandle;
import com.lowdragmc.lowdraglib2.utils.search.IResultHandler;
import com.viscriptquests.gui.blueprint.data.QuestBlueprintRegistryCache;
import com.viscriptquests.gui.blueprint.data.QuestRegistryId;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * 节点 option 的资源 ID 搜索框。
 * <p>
 * 节点常量保存为 QuestRegistryId，编译时再降级为普通字符串，避免污染运行时数据结构。
 */
public final class QuestRegistryIdConfigurator {
    public static Configurator create(IFieldValueConfigurable valueConfigurable, TypeHandle typeHandle) {
        var configurator = new SearchComponentConfigurator<>(
                "",
                () -> readValue(valueConfigurable),
                value -> {
                    valueConfigurable.setValue(value == null ? new QuestRegistryId() : value);
                    valueConfigurable.notifyValueChanged();
                },
                searchConfigurator(typeHandle),
                valueConfigurable.forceUpdate()
        );
        configurator.layout(layout -> layout.widthPercent(100));
        configurator.lineContainer.layout(layout -> layout.widthPercent(100));
        configurator.inlineContainer.layout(layout -> layout.widthPercent(100));
        configurator.searchComponent.layout(layout -> layout.widthPercent(100));
        return configurator;
    }

    private static SearchComponentConfigurator.ISearchConfigurator<QuestRegistryId> searchConfigurator(TypeHandle typeHandle) {
        if (QuestBlueprintTypes.ENTITY_TYPE_ID.equals(typeHandle)) {
            return new EntityTypeSearchConfigurator();
        }
        return new DimensionSearchConfigurator();
    }

    private static QuestRegistryId readValue(IFieldValueConfigurable valueConfigurable) {
        Object value = valueConfigurable.getValue();
        if (value instanceof QuestRegistryId id) {
            return id;
        }
        if (value instanceof String string) {
            return new QuestRegistryId(string);
        }
        Object defaultValue = valueConfigurable.getDefaultValue();
        if (defaultValue instanceof QuestRegistryId id) {
            return id;
        }
        if (defaultValue instanceof String string) {
            return new QuestRegistryId(string);
        }
        return new QuestRegistryId();
    }

    private abstract static class RegistryIdSearchConfigurator implements SearchComponentConfigurator.ISearchConfigurator<QuestRegistryId> {
        private final QuestRegistryId defaultValue;

        private RegistryIdSearchConfigurator(String defaultValue) {
            this.defaultValue = new QuestRegistryId(defaultValue);
        }

        @Override
        public QuestRegistryId defaultValue() {
            return defaultValue;
        }

        @Override
        public String resultText(@NotNull QuestRegistryId value) {
            return value.value();
        }

        @Override
        public Component mapping(@NotNull QuestRegistryId value) {
            return Component.literal(value.value());
        }

        @Override
        public @Nullable UIElementProvider<QuestRegistryId> candidateUIProvider() {
            return candidate -> new Label()
                    .textStyle(style -> style
                            .textWrap(TextWrap.HOVER_ROLL)
                            .textAlignHorizontal(Horizontal.LEFT)
                            .textAlignVertical(Vertical.CENTER))
                    .setText(candidate == null ? Component.literal("---") : mapping(candidate))
                    .setOverflowVisible(false);
        }

        protected void acceptMatches(String word, IResultHandler<QuestRegistryId> searchHandler, Supplier<Stream<String>> candidates) {
            String lowerWord = word == null ? "" : word.toLowerCase();
            candidates.get()
                    .filter(Objects::nonNull)
                    .distinct()
                    .sorted()
                    .filter(candidate -> lowerWord.isBlank() || candidate.toLowerCase().contains(lowerWord))
                    .limit(80)
                    .map(QuestRegistryId::new)
                    .forEach(searchHandler::acceptResult);
        }
    }

    private static class DimensionSearchConfigurator extends RegistryIdSearchConfigurator {
        private DimensionSearchConfigurator() {
            super(Level.OVERWORLD.location().toString());
        }

        @Override
        public void search(String word, IResultHandler<QuestRegistryId> searchHandler) {
            acceptMatches(word, searchHandler, this::dimensionIds);
        }

        private Stream<String> dimensionIds() {
            return QuestBlueprintRegistryCache.dimensionIds();
        }
    }

    private static class EntityTypeSearchConfigurator extends RegistryIdSearchConfigurator {
        private EntityTypeSearchConfigurator() {
            super(BuiltInRegistries.ENTITY_TYPE.getKey(EntityType.PIG).toString());
        }

        @Override
        public void search(String word, IResultHandler<QuestRegistryId> searchHandler) {
            acceptMatches(word, searchHandler, () -> BuiltInRegistries.ENTITY_TYPE.keySet().stream()
                    .sorted(Comparator.comparing(ResourceLocation::toString))
                    .map(ResourceLocation::toString));
        }

        @Override
        public Component mapping(@NotNull QuestRegistryId value) {
            ResourceLocation id = ResourceLocation.tryParse(value.value());
            EntityType<?> entityType = id == null ? null : BuiltInRegistries.ENTITY_TYPE.get(id);
            return entityType == null ? super.mapping(value) : entityType.getDescription().copy().append(" (" + value.value() + ")");
        }
    }
}
