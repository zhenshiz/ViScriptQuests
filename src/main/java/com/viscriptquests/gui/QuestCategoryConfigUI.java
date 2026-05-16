package com.viscriptquests.gui;

import com.lowdragmc.lowdraglib2.Platform;
import com.lowdragmc.lowdraglib2.configurator.ui.Configurator;
import com.lowdragmc.lowdraglib2.configurator.ui.ConfiguratorGroup;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.data.ScrollDisplay;
import com.lowdragmc.lowdraglib2.gui.ui.data.ScrollerMode;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.gui.ui.elements.ScrollerView;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.lowdragmc.lowdraglib2.math.Size;
import com.lowdragmc.lowdraglib2.networking.rpc.RPCPacketDistributor;
import com.viscriptquests.gui.components.DraggableUI;
import com.viscriptquests.network.c2s.C2SPayload;
import com.viscriptquests.quest.data.DisplayIcon;
import com.viscriptquests.quest.data.runtime.QuestCategoryData;
import com.viscriptquests.quest.data.runtime.QuestCategoryListData;
import dev.vfyjxf.taffy.style.AlignItems;
import dev.vfyjxf.taffy.style.FlexDirection;
import dev.vfyjxf.taffy.style.FlexWrap;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

public class QuestCategoryConfigUI extends UIElement {
    private final List<QuestCategoryData> categories = new ArrayList<>();
    private QuestCategoryData selectedCategory;
    private ScrollerView categoryListView;
    private DraggableUI<QuestCategoryData> categoryDragList;
    private ScrollerView editorView;

    public QuestCategoryConfigUI(QuestCategoryListData data) {
        categories.addAll(data.copyCategories());
        if (!categories.isEmpty()) {
            selectedCategory = categories.getFirst();
        }
        buildUI();
    }

    private void buildUI() {
        layout(layout -> {
            layout.widthPercent(100);
            layout.heightPercent(100);
            layout.flexDirection(FlexDirection.COLUMN);
            layout.paddingAll(8);
            layout.gapAll(6);
        });

        Label title = new Label();
        title.setText(Component.translatable("viscript_quests.category_config.title"));
        title.layout(layout -> layout.widthPercent(100));
        addChild(title);

        UIElement content = new UIElement();
        content.layout(layout -> {
            layout.widthPercent(100);
            layout.flex(1);
            layout.flexDirection(FlexDirection.ROW);
            layout.gapAll(8);
        });

        UIElement leftPanel = new UIElement();
        leftPanel.addClass("panel_bg");
        leftPanel.layout(layout -> {
            layout.width(150);
            layout.heightPercent(100);
            layout.flexDirection(FlexDirection.COLUMN);
            layout.paddingAll(4);
            layout.gapAll(4);
        });

        categoryListView = new ScrollerView();
        categoryListView.layout(layout -> {
            layout.widthPercent(100);
            layout.flex(1);
        });
        categoryListView.scrollerStyle(style -> style
                .mode(ScrollerMode.VERTICAL)
                .horizontalScrollDisplay(ScrollDisplay.NEVER)
                .verticalScrollDisplay(ScrollDisplay.AUTO));
        leftPanel.addChild(categoryListView);

        Button addButton = new Button();
        addButton.setText(Component.translatable("viscript_quests.category_config.add"));
        addButton.layout(layout -> layout.widthPercent(100));
        addButton.addEventListener(UIEvents.CLICK, event -> addCategory());
        leftPanel.addChild(addButton);

        UIElement rightPanel = new UIElement();
        rightPanel.addClass("panel_bg");
        rightPanel.layout(layout -> {
            layout.flex(1);
            layout.heightPercent(100);
            layout.flexDirection(FlexDirection.COLUMN);
            layout.paddingAll(6);
            layout.gapAll(4);
        });

        editorView = new ScrollerView();
        editorView.layout(layout -> {
            layout.widthPercent(100);
            layout.flex(1);
        });
        editorView.scrollerStyle(style -> style
                .mode(ScrollerMode.VERTICAL)
                .horizontalScrollDisplay(ScrollDisplay.NEVER)
                .verticalScrollDisplay(ScrollDisplay.AUTO));
        rightPanel.addChild(editorView);

        UIElement buttons = new UIElement();
        buttons.layout(layout -> {
            layout.widthPercent(100);
            layout.flexDirection(FlexDirection.ROW);
            layout.gapAll(4);
        });

        Button deleteButton = new Button();
        deleteButton.setText(Component.translatable("viscript_quests.category_config.delete"));
        deleteButton.layout(layout -> layout.width(80));
        deleteButton.addEventListener(UIEvents.CLICK, event -> deleteSelected());
        buttons.addChild(deleteButton);

        Button saveAllButton = new Button();
        saveAllButton.setText(Component.translatable("viscript_quests.category_config.save_all"));
        saveAllButton.layout(layout -> layout.width(90));
        saveAllButton.addEventListener(UIEvents.CLICK, event -> saveAll());
        buttons.addChild(saveAllButton);

        rightPanel.addChild(buttons);

        content.addChildren(leftPanel, rightPanel);
        addChild(content);

        reloadCategoryList();
        reloadEditor();
    }

    private void addCategory() {
        String baseId = "category";
        int index = categories.size() + 1;
        String id = baseId + index;
        while (findCategory(id) != null) {
            index++;
            id = baseId + index;
        }
        QuestCategoryData category = QuestCategoryData.of(id, id, DisplayIcon.item("minecraft:book"), id);
        categories.add(category);
        selectedCategory = category;
        reloadCategoryList();
        reloadEditor();
    }

    private boolean applyBeforeSave() {
        return normalizeCategories();
    }

    private void deleteSelected() {
        if (selectedCategory == null || categories.size() <= 1) {
            return;
        }
        categories.remove(selectedCategory);
        selectedCategory = categories.isEmpty() ? null : categories.getFirst();
        reloadCategoryList();
        reloadEditor();
    }

    private void saveAll() {
        if (!applyBeforeSave()) {
            return;
        }
        CompoundTag tag = QuestCategoryListData.of(categories).serializeNBT(Platform.getFrozenRegistry());
        RPCPacketDistributor.rpcToServer(C2SPayload.SAVE_DEFAULT_QUEST_CATEGORIES, tag);
        if (Minecraft.getInstance().player != null) {
            Minecraft.getInstance().player.displayClientMessage(
                    Component.translatable("viscript_quests.category_config.saved"), false);
        }
    }

    private void selectCategory(QuestCategoryData category) {
        selectedCategory = category;
        reloadCategoryList();
        reloadEditor();
    }

    private QuestCategoryData findCategory(String id) {
        String normalizedId = QuestCategoryData.normalizeId(id);
        return categories.stream()
                .filter(category -> category.id.equals(normalizedId))
                .findFirst()
                .orElse(null);
    }

    private void reloadCategoryList() {
        categoryListView.clearAllScrollViewChildren();
        categoryDragList = new DraggableUI<>(categories, this::applyCategoryOrder);
        categoryDragList.layout(layout -> {
            layout.widthPercent(100);
            layout.flexDirection(FlexDirection.COLUMN);
            layout.wrap(FlexWrap.NO_WRAP);
            layout.gapAll(3);
            layout.paddingAll(0);
        });
        for (QuestCategoryData category : categories) {
            var row = createCategoryRow(category);
            categoryDragList.addSortableCard(category, row.root());
        }
        categoryListView.addScrollViewChild(categoryDragList);
    }

    private CategoryRow createCategoryRow(QuestCategoryData category) {
        UIElement row = new UIElement();
        row.layout(layout -> {
            layout.widthPercent(100);
            layout.height(22);
            layout.flexDirection(FlexDirection.ROW);
            layout.alignItems(AlignItems.CENTER);
            layout.paddingAll(2);
        });

        Button button = new Button();
        boolean selected = selectedCategory == category;
        button.setText(Component.literal((selected ? "§e" : "§f") + category.title + " §8(" + category.id + ")"));
        button.layout(layout -> {
            layout.width(0);
            layout.flex(1);
            layout.height(18);
        });
        button.addEventListener(UIEvents.MOUSE_DOWN, event -> event.stopPropagation());
        button.addEventListener(UIEvents.CLICK, event -> selectCategory(category));
        row.addChild(button);
        return new CategoryRow(row);
    }

    private void applyCategoryOrder(List<QuestCategoryData> newOrder) {
        categories.clear();
        categories.addAll(newOrder);
    }

    private void reloadEditor() {
        editorView.clearAllScrollViewChildren();
        if (selectedCategory == null) {
            return;
        }
        ConfiguratorGroup group = new ConfiguratorGroup("", false).hideTitle();
        group.setCanCollapse(false);
        group.layout(layout -> layout.widthPercent(100));
        group.configuratorContainer.layout(layout -> {
            layout.widthPercent(100);
            layout.paddingAll(4);
            layout.marginLeft(0);
        });
        group.addEventListener(Configurator.CHANGE_EVENT, event -> reloadCategoryList());
        selectedCategory.buildConfigurator(group);
        editorView.addScrollViewChild(group);
    }

    private boolean normalizeCategories() {
        String previousSelectionId = selectedCategory == null ? "" : selectedCategory.id;
        for (QuestCategoryData category : categories) {
            normalizeCategory(category);
        }
        List<QuestCategoryData> sanitized = QuestCategoryListData.sanitize(categories);
        if (sanitized.stream().anyMatch(category -> category.id.isBlank())) {
            return false;
        }
        categories.clear();
        categories.addAll(sanitized);
        selectedCategory = previousSelectionId.isBlank()
                ? categories.isEmpty() ? null : categories.getFirst()
                : findCategory(previousSelectionId);
        if (selectedCategory == null && !categories.isEmpty()) {
            selectedCategory = categories.getFirst();
        }
        reloadCategoryList();
        reloadEditor();
        return true;
    }

    private void normalizeCategory(QuestCategoryData category) {
        if (category == null) {
            return;
        }
        category.id = QuestCategoryData.normalizeId(category.id);
        category.title = category.title == null ? "" : category.title.trim();
        category.tooltip = category.tooltip == null ? "" : category.tooltip.trim();
        if (category.displayIcon == null) {
            category.displayIcon = new DisplayIcon();
        }
    }

    private record CategoryRow(UIElement root) {
    }

    @Override
    public void initScreen(int screenWidth, int screenHeight) {
        super.initScreen(screenWidth, screenHeight);
        applyAutoGuiScaleTransform();
    }

    public static Size getAutoGuiScaledSize(Size screenSize) {
        float scale = getAutoGuiScaleFactor();
        if (scale <= 0f) return screenSize;
        return Size.of(
                Math.max(1, Math.round(screenSize.getWidth() / scale)),
                Math.max(1, Math.round(screenSize.getHeight() / scale))
        );
    }

    private void applyAutoGuiScaleTransform() {
        float scale = getAutoGuiScaleFactor();
        transform(transform -> transform.pivot(0.5f, 0.5f).scale(scale));
    }

    private static float getAutoGuiScaleFactor() {
        Minecraft minecraft = Minecraft.getInstance();
        var window = minecraft.getWindow();
        double currentScale = window.getGuiScale();
        if (currentScale <= 0d) return 1f;
        int autoScale = window.calculateScale(0, minecraft.isEnforceUnicode());
        return Math.max(1f, (float) (autoScale / currentScale));
    }
}
