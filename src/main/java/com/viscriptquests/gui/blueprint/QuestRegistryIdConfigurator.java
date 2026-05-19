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
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * 节点 option 的资源 ID 搜索框。
 * <p>
 * 节点常量保存为 QuestRegistryId，编译时再降级为普通字符串，避免污染运行时数据结构。
 */
public final class QuestRegistryIdConfigurator {
    private static final Map<EntityType<?>, Boolean> LIVING_ENTITY_TYPE_CACHE = new IdentityHashMap<>();

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
            String lowerWord = normalizeSearchWord(word);
            candidates.get()
                    .filter(Objects::nonNull)
                    .distinct()
                    .sorted()
                    .filter(candidate -> lowerWord.isBlank() || candidate.toLowerCase(Locale.ROOT).contains(lowerWord))
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
            String lowerWord = normalizeSearchWord(word);
            BuiltInRegistries.ENTITY_TYPE.entrySet().stream()
                    .map(entry -> EntitySearchEntry.of(entry.getKey().location(), entry.getValue()))
                    .filter(Objects::nonNull)
                    .filter(EntitySearchEntry::livingEntityType)
                    .sorted(Comparator.comparing(EntitySearchEntry::displayName))
                    .filter(entry -> lowerWord.isBlank() || entry.matches(lowerWord))
                    .limit(80)
                    .map(entry -> new QuestRegistryId(entry.id()))
                    .forEach(searchHandler::acceptResult);
        }

        @Override
        public String resultText(@NotNull QuestRegistryId value) {
            return entityDisplayName(value).getString();
        }

        @Override
        public Component mapping(@NotNull QuestRegistryId value) {
            return entityDisplayName(value);
        }

        private Component entityDisplayName(@NotNull QuestRegistryId value) {
            ResourceLocation id = ResourceLocation.tryParse(value.value());
            EntityType<?> entityType = id == null ? null : BuiltInRegistries.ENTITY_TYPE.get(id);
            return entityType == null ? super.mapping(value) : entityType.getDescription();
        }
    }

    private record EntitySearchEntry(String id, String path, String displayName, String descriptionId, boolean livingEntityType) {
        private static EntitySearchEntry of(ResourceLocation id, EntityType<?> type) {
            if (id == null || type == null) {
                return null;
            }
            return new EntitySearchEntry(
                    id.toString(),
                    id.getPath(),
                    type.getDescription().getString(),
                    type.getDescriptionId(),
                    isLivingEntityType(type)
            );
        }

        private boolean matches(String lowerWord) {
            return id.toLowerCase(Locale.ROOT).contains(lowerWord)
                    || path.toLowerCase(Locale.ROOT).contains(lowerWord)
                    || displayName.toLowerCase(Locale.ROOT).contains(lowerWord)
                    || descriptionId.toLowerCase(Locale.ROOT).contains(lowerWord);
        }
    }

    private static boolean isLivingEntityType(EntityType<?> type) {
        Boolean cached = LIVING_ENTITY_TYPE_CACHE.get(type);
        if (cached != null) {
            return cached;
        }
        if (type == EntityType.PLAYER) {
            LIVING_ENTITY_TYPE_CACHE.put(type, true);
            return true;
        }
        Level level = Minecraft.getInstance().level;
        if (level == null) {
            return false;
        }
        try {
            Entity entity = type.create(level);
            if (entity == null) {
                return false;
            }
            boolean living = entity instanceof LivingEntity;
            entity.discard();
            LIVING_ENTITY_TYPE_CACHE.put(type, living);
            return living;
        } catch (RuntimeException ignored) {
            // 个别模组实体可能在仅用于补全的临时创建中依赖额外环境，失败时不加入击杀目标候选。
            LIVING_ENTITY_TYPE_CACHE.put(type, false);
            return false;
        }
    }

    private static String normalizeSearchWord(String word) {
        return word == null ? "" : word.toLowerCase(Locale.ROOT).trim();
    }
}
