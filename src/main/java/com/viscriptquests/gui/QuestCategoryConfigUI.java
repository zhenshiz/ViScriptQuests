package com.viscriptquests.gui;

import com.lowdragmc.lowdraglib2.Platform;
import com.lowdragmc.lowdraglib2.configurator.ui.Configurator;
import com.lowdragmc.lowdraglib2.configurator.ui.ConfiguratorGroup;
import com.lowdragmc.lowdraglib2.gui.ui.data.Horizontal;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.data.ScrollDisplay;
import com.lowdragmc.lowdraglib2.gui.ui.data.ScrollerMode;
import com.lowdragmc.lowdraglib2.gui.ui.data.TextWrap;
import com.lowdragmc.lowdraglib2.gui.ui.data.Vertical;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.gui.ui.elements.ScrollerView;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Selector;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.lowdragmc.lowdraglib2.gui.texture.SpriteTexture;
import com.lowdragmc.lowdraglib2.math.Size;
import com.lowdragmc.lowdraglib2.networking.rpc.RPCPacketDistributor;
import com.viscript_lib.gui.components.DraggableUI;
import com.viscriptquests.network.c2s.C2SPayload;
import com.viscriptquests.quest.data.DisplayIcon;
import com.viscriptquests.quest.data.runtime.QuestCategoryConfigData;
import com.viscriptquests.quest.data.runtime.QuestCategoryData;
import com.viscriptquests.quest.data.runtime.QuestCategoryListData;
import com.viscriptquests.util.QuestFileHelper;
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
    private final List<String> availableQuestIds = new ArrayList<>();
    private QuestCategoryData selectedCategory;
    private ScrollerView categoryListView;
    private DraggableUI<QuestCategoryData> categoryDragList;
    private ScrollerView editorView;

    public QuestCategoryConfigUI(QuestCategoryConfigData data) {
        QuestCategoryListData categoryData = data == null || data.categoryData == null
                ? new QuestCategoryListData()
                : data.categoryData;
        categories.addAll(categoryData.copyCategories());
        if (data != null) {
            availableQuestIds.addAll(data.copyQuestIds());
        }
        if (!categories.isEmpty()) {
            selectedCategory = categories.getFirst();
        }
        buildUI();
    }

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
        QuestCategoryData category = QuestCategoryData.of(id, id, DisplayIcon.item("minecraft:book"));
        categories.add(category);
        selectedCategory = category;
        reloadCategoryList();
        reloadEditor();
    }

    private boolean applyBeforeSave() {
        return normalizeCategories();
    }

    private void deleteSelected() {
        if (selectedCategory == null) {
            return;
        }
        categories.remove(selectedCategory);
        selectedCategory = categories.isEmpty() ? null : categories.getFirst();
        reloadCategoryList();
        reloadEditor();
    }

    private void saveAll() {
        saveCategories(true);
    }

    private boolean saveCategories(boolean notifyPlayer) {
        if (!applyBeforeSave()) {
            return false;
        }
        CompoundTag tag = QuestCategoryListData.of(categories).serializeNBT(Platform.getFrozenRegistry());
        RPCPacketDistributor.rpcToServer(C2SPayload.SAVE_QUEST_CATEGORIES, tag);
        if (notifyPlayer && Minecraft.getInstance().player != null) {
            Minecraft.getInstance().player.displayClientMessage(
                    Component.translatable("viscript_quests.category_config.saved"), false);
        }
        return true;
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
            layout.gapAll(2);
        });

        boolean selected = selectedCategory == category;
        UIElement tabPreview = new UIElement();
        tabPreview.layout(layout -> layout.width(24).height(18));
        tabPreview.style(style -> style.backgroundTexture(
                SpriteTexture.of(category.tabBackgroundLocation(selected))));
        tabPreview.setAllowHitTest(false);
        row.addChild(tabPreview);

        Button button = new Button();
        String title = category.title == null || category.title.isBlank() ? category.id : category.title;
        button.setText(Component.literal((selected ? "§e" : "§f") + title + " §8(" + category.id + ")"));
        button.layout(layout -> {
            layout.width(0);
            layout.flex(1);
            layout.height(18);
        });
        button.text.layout(layout -> {
            layout.width(0);
            layout.flex(1);
            layout.heightPercent(100);
        });
        button.textStyle(style -> style
                .adaptiveWidth(false)
                .textWrap(TextWrap.HOVER_ROLL)
                .textAlignHorizontal(Horizontal.LEFT)
                .textAlignVertical(Vertical.CENTER));
        button.setOverflowVisible(false);
        button.text.setOverflowVisible(false);
        button.addEventListener(UIEvents.MOUSE_DOWN, event -> event.stopPropagation());
        button.setOnClick(event -> selectCategory(category));
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
        addQuestListEditor(group);
        editorView.addScrollViewChild(group);
    }

    private void addQuestListEditor(ConfiguratorGroup group) {
        ConfiguratorGroup quests = new ConfiguratorGroup("viscript_quests.questCategory.questIds", false);
        quests.setCanCollapse(false);
        quests.layout(layout -> layout.widthPercent(100));
        quests.configuratorContainer.layout(layout -> {
            layout.widthPercent(100);
            layout.flexDirection(FlexDirection.COLUMN);
            layout.gapAll(3);
        });
        for (int i = 0; i < selectedCategory.questIds.size(); i++) {
            quests.configuratorContainer.addChild(createQuestIdRow(i));
        }

        UIElement buttons = new UIElement();
        buttons.layout(layout -> {
            layout.widthPercent(100);
            layout.flexDirection(FlexDirection.ROW);
            layout.gapAll(4);
        });

        Button addQuest = new Button();
        addQuest.setText(Component.translatable("viscript_quests.category_config.quest.add"));
        addQuest.layout(layout -> layout.width(90).height(18));
        addQuest.addEventListener(UIEvents.CLICK, event -> {
            selectedCategory.questIds.add(defaultNewQuestId());
            reloadEditor();
        });
        buttons.addChild(addQuest);

        quests.addChild(buttons);
        group.addChild(quests);
    }

    private UIElement createQuestIdRow(int index) {
        UIElement row = new UIElement();
        row.layout(layout -> {
            layout.widthPercent(100);
            layout.height(22);
            layout.flexDirection(FlexDirection.ROW);
            layout.alignItems(AlignItems.CENTER);
            layout.gapAll(4);
        });

        Selector<String> selector = new Selector<>();
        selector.setCandidates(createQuestCandidates(questId(index)));
        selector.setCandidateUIProvider(candidate -> {
            Label label = new Label();
            label.layout(layout -> layout.widthPercent(100).height(18));
            label.textStyle(style -> style
                    .textWrap(TextWrap.HOVER_ROLL)
                    .textAlignHorizontal(Horizontal.LEFT)
                    .textAlignVertical(Vertical.CENTER));
            label.setText(questCandidateText(candidate));
            label.setOverflowVisible(false);
            return label;
        });
        selector.setSelected(questId(index), false);
        selector.setOnValueChanged(value -> {
            if (index >= 0 && index < selectedCategory.questIds.size()) {
                selectedCategory.questIds.set(index, QuestFileHelper.normalizeQuestId(value == null ? "" : value));
            }
        });
        selector.selectorStyle(style -> style.maxItemCount(8).scrollerViewHeight(110));
        selector.layout(layout -> {
            layout.width(130);
            layout.height(18);
        });
        row.addChild(selector);

        Button editButton = new Button();
        editButton.setText(Component.translatable("viscript_quests.category_config.quest.edit"));
        editButton.layout(layout -> layout.width(42).height(18));
        editButton.addEventListener(UIEvents.CLICK, event -> openQuestProject(questId(index)));
        row.addChild(editButton);

        Button removeButton = new Button();
        removeButton.setText(Component.translatable("viscript_quests.category_config.quest.remove"));
        removeButton.layout(layout -> layout.width(42).height(18));
        removeButton.addEventListener(UIEvents.CLICK, event -> {
            if (index >= 0 && index < selectedCategory.questIds.size()) {
                selectedCategory.questIds.remove(index);
                reloadEditor();
            }
        });
        row.addChild(removeButton);
        return row;
    }

    private String questId(int index) {
        return index >= 0 && index < selectedCategory.questIds.size()
                ? selectedCategory.questIds.get(index)
                : "";
    }

    private List<String> createQuestCandidates(String currentQuestId) {
        List<String> candidates = new ArrayList<>();
        if (availableQuestIds.isEmpty()) {
            candidates.add("");
        } else {
            candidates.addAll(availableQuestIds);
        }
        String normalizedCurrentId = QuestFileHelper.normalizeQuestId(currentQuestId == null ? "" : currentQuestId);
        if (!normalizedCurrentId.isBlank() && !candidates.contains(normalizedCurrentId)) {
            candidates.add(normalizedCurrentId);
        }
        return candidates;
    }

    private String defaultNewQuestId() {
        return availableQuestIds.isEmpty() ? "" : availableQuestIds.getFirst();
    }

    private Component questCandidateText(String questId) {
        String normalizedQuestId = QuestFileHelper.normalizeQuestId(questId == null ? "" : questId);
        if (normalizedQuestId.isBlank()) {
            return Component.translatable("viscript_quests.category_config.quest.empty");
        }
        return Component.literal(normalizedQuestId);
    }

    private void openQuestProject(String questId) {
        String normalized = questId == null ? "" : questId.trim();
        if (normalized.isBlank()) {
            return;
        }
        if (!saveCategories(false)) {
            return;
        }
        CompoundTag tag = new CompoundTag();
        tag.putString("projectId", normalized);
        RPCPacketDistributor.rpcToServer(C2SPayload.REQUEST_OPEN_EDITOR_PROJECT, tag);
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
        if (category.displayIcon == null) {
            category.displayIcon = new DisplayIcon();
        }
        category.normalizeTabBackgrounds();
        QuestCategoryListData.sanitizeQuestIds(category.questIds);
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
