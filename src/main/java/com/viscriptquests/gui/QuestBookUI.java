package com.viscriptquests.gui;

import com.lowdragmc.lowdraglib2.gui.texture.ColorBorderTexture;
import com.lowdragmc.lowdraglib2.gui.texture.ColorRectTexture;
import com.lowdragmc.lowdraglib2.gui.texture.GuiTextureGroup;
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.texture.SpriteTexture;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.data.ScrollDisplay;
import com.lowdragmc.lowdraglib2.gui.ui.data.ScrollerMode;
import com.lowdragmc.lowdraglib2.gui.ui.data.TextWrap;
import com.lowdragmc.lowdraglib2.gui.ui.data.Horizontal;
import com.lowdragmc.lowdraglib2.gui.ui.data.Vertical;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.gui.ui.elements.ItemSlot;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.gui.ui.elements.ScrollerView;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.lowdragmc.lowdraglib2.math.Size;
import com.lowdragmc.lowdraglib2.networking.rpc.RPCPacketDistributor;
import com.viscriptquests.ViScriptQuests;
import com.viscriptquests.config.ClientConfig;
import com.viscriptquests.network.c2s.C2SPayload;
import com.viscriptquests.quest.data.DisplayIcon;
import com.viscriptquests.quest.data.runtime.PlayerQuestState;
import com.viscriptquests.quest.data.runtime.QuestCategoryData;
import com.viscriptquests.quest.data.runtime.QuestPlayerData;
import com.viscriptquests.quest.data.runtime.QuestStatus;
import com.viscriptquests.quest.data.runtime.RewardDisplay;
import com.viscriptquests.quest.data.runtime.TaskObjectiveProgress;
import com.viscriptquests.quest.data.runtime.TaskProgress;
import com.viscriptquests.quest.data.runtime.TaskStatus;
import dev.vfyjxf.taffy.style.AlignContent;
import dev.vfyjxf.taffy.style.AlignItems;
import dev.vfyjxf.taffy.style.FlexDirection;
import dev.vfyjxf.taffy.style.FlexWrap;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

// 玩家正式任务书 UI：左侧分类书签，书页左侧展示任务树，右侧展示当前任务/目标详情。
public class QuestBookUI extends UIElement {
    private static final ItemStack DEFAULT_ICON = new ItemStack(Items.WRITABLE_BOOK);
    private static final int CATEGORY_RAIL_WIDTH = 36;
    private static final int LEFT_PAGE_WIDTH = 230;
    private static final int QUEST_ITEM_HEIGHT = 32;
    private static final int TASK_ITEM_HEIGHT = 22;
    private static final int BOOKMARK_HEIGHT = 28;
    private static final int BOOKMARK_GAP = 3;
    private static final int BOOKMARK_ICON_SIZE = 16;
    private static final int QUEST_ICON_SIZE = 16;
    private static final int REWARD_SLOT_SIZE = 34;
    private static final int REWARD_ICON_SIZE = 18;
    private static final int OBJECTIVE_ROW_HEIGHT = 18;
    private static final int OBJECTIVE_ICON_SIZE = 16;
    private static final int SUB_TASK_INDENT = 24;
    private static final int CATEGORIES_PER_PAGE = 5;
    private static final float FONT_WINDOW_TITLE = 9.0f;
    private static final float FONT_PANEL_TITLE = 8.5f;
    private static final float FONT_ROW_TITLE = 8.0f;
    private static final float FONT_ROW_SUBTITLE = 7.5f;
    private static final float FONT_DETAIL_TITLE = 9.0f;
    private static final float FONT_BODY = 8.0f;
    private static final float FONT_SMALL = 7.5f;

    private static final int ROOT_BG = 0xE0060709;
    private static final int TEXT_GOLD = 0xFFFFD84B;
    private static final int TEXT_MAIN = 0xFFFFFFFF;
    private static final int TEXT_MUTED = 0xFFB5AA86;
    private static final IGuiTexture BOOK_FRAME = sprite("book_frame.png", 6);
    private static final IGuiTexture BOOK_ICON_TEXTURE = sprite("book_icon.png", 0);
    private static final IGuiTexture BOOKMARK_DEFAULT = sprite("bookmark_default.png", 4);
    private static final IGuiTexture BOOKMARK_HOVER = sprite("bookmark_hover.png", 4);
    private static final IGuiTexture CONTENT_PANEL = sprite("panel_green.png", 6);
    private static final IGuiTexture QUEST_DEFAULT = sprite("quest_default.png", 4);
    private static final IGuiTexture QUEST_SELECTED = sprite("quest_selected.png", 4);
    private static final IGuiTexture QUEST_COMPLETED = sprite("quest_completed.png", 4);
    private static final IGuiTexture SECTION_PANEL = sprite("section_panel.png", 5);
    private static final IGuiTexture STATUS_TAG = sprite("status_tag.png", 4);
    private static final IGuiTexture TRACK_BUTTON = sprite("track_button.png", 4);
    private static final IGuiTexture TRACK_BUTTON_HOVER = sprite("track_button_hover.png", 4);
    private static final IGuiTexture TRACK_BUTTON_PRESSED = sprite("track_button_pressed.png", 4);

    private QuestPlayerData playerData;
    private String selectedCategoryId = "";
    private PlayerQuestState selectedQuest;
    private String selectedStepId = "";
    private int categoryPage = 0;
    private final Set<String> expandedQuestIds = new LinkedHashSet<>();
    private UIElement categoryListPanel;
    private Button categoryPager;
    private Label categoryPagerLabel;
    private ScrollerView questTreeView;
    private ScrollerView detailScroller;

    public QuestBookUI(QuestPlayerData playerData) {
        this.playerData = playerData;
        buildUI();
    }

    public void syncPlayerData(QuestPlayerData playerData) {
        String selectedQuestId = selectedQuest == null ? "" : selectedQuest.questId;
        this.playerData = playerData;
        selectedQuest = playerData.findQuest(selectedQuestId).orElse(null);
        ensureSelectedQuestInCategory();
        refreshAll();
    }

    private void buildUI() {
        layout(layout -> {
            layout.widthPercent(100);
            layout.heightPercent(100);
            layout.paddingAll(8);
        });
        style(style -> style.backgroundTexture(new ColorRectTexture(ROOT_BG)));

        UIElement stage = new UIElement();
        stage.layout(layout -> {
            layout.widthPercent(100);
            layout.heightPercent(100);
            layout.flexDirection(FlexDirection.ROW);
            layout.gapAll(0);
        });
        addChild(stage);

        buildCategoryRail(stage);

        UIElement shell = texturedPanel(BOOK_FRAME);
        shell.layout(layout -> {
            layout.width(0);
            layout.flex(1);
            layout.heightPercent(100);
            layout.flexDirection(FlexDirection.COLUMN);
            layout.paddingAll(12);
            layout.gapAll(7);
        });
        stage.addChild(shell);

        buildTopBar(shell);

        UIElement body = new UIElement();
        body.layout(layout -> {
            layout.widthPercent(100);
            layout.flex(1);
            layout.flexDirection(FlexDirection.ROW);
            layout.gapAll(6);
        });
        shell.addChild(body);

        buildQuestTreePanel(body);
        buildDetailPanel(body);

        refreshAll();
    }

    private void buildTopBar(UIElement parent) {
        UIElement topBar = panel(0x88351C10, 0xAA7A431E);
        topBar.layout(layout -> {
            layout.widthPercent(100);
            layout.height(34);
            layout.flexDirection(FlexDirection.ROW);
            layout.alignItems(AlignItems.CENTER);
            layout.justifyContent(AlignContent.SPACE_BETWEEN);
            layout.paddingHorizontal(10);
            layout.gapAll(8);
        });

        UIElement titleBlock = new UIElement();
        titleBlock.layout(layout -> {
            layout.flex(1);
            layout.flexDirection(FlexDirection.ROW);
            layout.alignItems(AlignItems.CENTER);
            layout.gapAll(8);
        });

        UIElement bookIcon = texturedPanel(BOOK_ICON_TEXTURE);
        bookIcon.layout(layout -> {
            layout.width(24);
            layout.height(24);
        });
        titleBlock.addChild(bookIcon);

        Label title = label(Component.translatable("viscript_quests.quest_book.window_title"), 16);
        title.layout(layout -> {
            layout.width(92);
            layout.height(16);
        });
        title.textStyle(style -> style.textColor(TEXT_GOLD).fontSize(FONT_WINDOW_TITLE).textWrap(TextWrap.HIDE));
        titleBlock.addChild(title);

        topBar.addChild(titleBlock);
        parent.addChild(topBar);
    }

    private void buildCategoryRail(UIElement parent) {
        UIElement rail = new UIElement();
        rail.layout(layout -> {
            layout.width(CATEGORY_RAIL_WIDTH);
            layout.heightPercent(100);
            layout.flexDirection(FlexDirection.COLUMN);
            layout.paddingTop(58);
            layout.gapAll(BOOKMARK_GAP);
        });

        categoryListPanel = new UIElement();
        categoryListPanel.layout(layout -> {
            layout.widthPercent(100);
            layout.height(BOOKMARK_HEIGHT * CATEGORIES_PER_PAGE + BOOKMARK_GAP * (CATEGORIES_PER_PAGE - 1));
            layout.flexDirection(FlexDirection.COLUMN);
            layout.gapAll(BOOKMARK_GAP);
        });
        rail.addChild(categoryListPanel);

        UIElement spacer = new UIElement();
        spacer.layout(layout -> {
            layout.widthPercent(100);
            layout.flex(1);
        });
        rail.addChild(spacer);

        categoryPager = new Button();
        categoryPager.noText();
        categoryPager.buttonStyle(style -> style
                .baseTexture(BOOKMARK_DEFAULT)
                .hoverTexture(BOOKMARK_HOVER)
                .pressedTexture(BOOKMARK_HOVER));
        categoryPager.layout(layout -> {
            layout.widthPercent(100);
            layout.height(BOOKMARK_HEIGHT);
            layout.flexDirection(FlexDirection.COLUMN);
            layout.alignItems(AlignItems.CENTER);
            layout.justifyContent(AlignContent.CENTER);
        });
        categoryPagerLabel = label(Component.literal(">"), BOOKMARK_HEIGHT);
        categoryPagerLabel.textStyle(style -> style
                .textColor(TEXT_GOLD)
                .fontSize(FONT_BODY)
                .textAlignHorizontal(Horizontal.CENTER)
                .textAlignVertical(Vertical.CENTER)
                .textWrap(TextWrap.HIDE));
        categoryPager.addChild(categoryPagerLabel);
        categoryPager.addEventListener(UIEvents.CLICK, event -> {
            int maxPage = maxCategoryPage();
            if (maxPage <= 0) {
                return;
            }
            categoryPage = categoryPage >= maxPage ? 0 : categoryPage + 1;
            reloadCategoryList();
        });
        rail.addChild(categoryPager);

        parent.addChild(rail);
    }

    private void buildQuestTreePanel(UIElement parent) {
        UIElement questPanel = texturedPanel(CONTENT_PANEL);
        questPanel.layout(layout -> {
            layout.width(LEFT_PAGE_WIDTH);
            layout.heightPercent(100);
            layout.flexDirection(FlexDirection.COLUMN);
            layout.paddingAll(6);
            layout.gapAll(4);
        });

        Label title = label(Component.translatable("viscript_quests.quest_book.quest_list"), 14);
        title.layout(layout -> {
            layout.widthPercent(100);
            layout.height(14);
        });
        title.textStyle(style -> style.textColor(TEXT_GOLD).fontSize(FONT_PANEL_TITLE).textWrap(TextWrap.HIDE));
        questPanel.addChild(title);

        questTreeView = new ScrollerView();
        questTreeView.layout(layout -> {
            layout.widthPercent(100);
            layout.flex(1);
        });
        configureVerticalScroller(questTreeView, 1);
        questPanel.addChild(questTreeView);

        parent.addChild(questPanel);
    }

    private void buildDetailPanel(UIElement parent) {
        UIElement detailPanel = texturedPanel(CONTENT_PANEL);
        detailPanel.layout(layout -> {
            layout.width(0);
            layout.flex(1);
            layout.heightPercent(100);
            layout.flexDirection(FlexDirection.COLUMN);
            layout.paddingAll(7);
        });

        detailScroller = new ScrollerView();
        detailScroller.layout(layout -> {
            layout.widthPercent(100);
            layout.flex(1);
        });
        configureVerticalScroller(detailScroller, 1);
        detailPanel.addChild(detailScroller);

        parent.addChild(detailPanel);
    }

    private void configureVerticalScroller(ScrollerView scroller, int padding) {
        scroller.scrollerStyle(style -> style
                .mode(ScrollerMode.VERTICAL)
                .horizontalScrollDisplay(ScrollDisplay.NEVER)
                .verticalScrollDisplay(ScrollDisplay.NEVER)
                .scrollerViewStyle(3));
        scroller.viewPort(view -> view
                .layout(layout -> layout.paddingAll(padding))
                .style(style -> style.backgroundTexture(new ColorRectTexture(0x00000000))));
        scroller.viewContainer(view -> view.layout(layout -> {
            layout.widthPercent(100);
            layout.flexDirection(FlexDirection.COLUMN);
            layout.gapAll(3);
        }));
    }

    private void refreshAll() {
        ensureSelectedCategory();
        ensureSelectedQuestInCategory();
        reloadCategoryList();
        reloadQuestTree();
        reloadQuestDetail();
    }

    private void ensureSelectedCategory() {
        categoryPage = Math.max(0, Math.min(categoryPage, maxCategoryPage()));
        if (playerData.categories.isEmpty()) {
            selectedCategoryId = "";
            return;
        }
        if (findCategory(selectedCategoryId) == null) {
            selectedCategoryId = playerData.categories.getFirst().id;
        }
        int selectedIndex = categoryIndex(selectedCategoryId);
        if (selectedIndex >= 0 && selectedIndex / CATEGORIES_PER_PAGE != categoryPage) {
            categoryPage = selectedIndex / CATEGORIES_PER_PAGE;
        }
    }

    private void ensureSelectedQuestInCategory() {
        List<PlayerQuestState> filtered = getFilteredQuests();
        if (selectedQuest != null) {
            for (PlayerQuestState quest : filtered) {
                if (quest.questId.equals(selectedQuest.questId)) {
                    selectedQuest = quest;
                    ensureSelectedTaskInQuest();
                    return;
                }
            }
        }
        selectedQuest = filtered.isEmpty() ? null : filtered.getFirst();
        if (selectedQuest != null) {
            expandedQuestIds.add(selectedQuest.questId);
        }
        ensureSelectedTaskInQuest();
    }

    private void selectCategory(String categoryId) {
        selectedCategoryId = QuestCategoryData.normalizeId(categoryId);
        selectedQuest = null;
        selectedStepId = "";
        refreshAll();
    }

    private void selectQuest(PlayerQuestState quest) {
        selectedQuest = quest;
        selectedStepId = "";
        expandedQuestIds.add(quest.questId);
        reloadQuestTree();
        reloadQuestDetail();
    }

    private void selectTask(PlayerQuestState quest, String stepId) {
        selectedQuest = quest;
        selectedStepId = stepId == null ? "" : stepId;
        expandedQuestIds.add(quest.questId);
        reloadQuestTree();
        reloadQuestDetail();
    }

    private void toggleQuestExpanded(PlayerQuestState quest) {
        boolean switchedQuest = selectedQuest == null || !selectedQuest.questId.equals(quest.questId);
        if (expandedQuestIds.contains(quest.questId)) {
            expandedQuestIds.remove(quest.questId);
        } else {
            expandedQuestIds.add(quest.questId);
        }
        selectedQuest = quest;
        selectedStepId = "";
        if (switchedQuest) {
            expandedQuestIds.add(quest.questId);
        }
        reloadQuestTree();
        reloadQuestDetail();
    }

    private void ensureSelectedTaskInQuest() {
        if (selectedQuest == null) {
            selectedStepId = "";
            return;
        }
        if (selectedStepId == null || selectedStepId.isBlank()) {
            return;
        }
        boolean exists = getVisibleTasks(selectedQuest).stream()
                .anyMatch(progress -> progress.stepId.equals(selectedStepId));
        if (!exists) {
            selectedStepId = "";
        }
    }

    private void reloadCategoryList() {
        categoryPage = Math.max(0, Math.min(categoryPage, maxCategoryPage()));
        categoryListPanel.clearAllChildren();
        List<QuestCategoryData> categories = pagedCategories();
        for (QuestCategoryData category : categories) {
            addCategoryItem(category.id,
                    Component.literal(category.title),
                    category);
        }
        reloadCategoryPager();
    }

    private void addCategoryItem(String categoryId, Component title, Object icon) {
        boolean selected = categoryId.equals(selectedCategoryId);
        Button row = new Button();
        row.noText();
        row.buttonStyle(style -> style
                .baseTexture(selected ? BOOKMARK_HOVER : BOOKMARK_DEFAULT)
                .hoverTexture(BOOKMARK_HOVER)
                .pressedTexture(BOOKMARK_HOVER));
        row.layout(layout -> {
            layout.widthPercent(100);
            layout.height(BOOKMARK_HEIGHT);
            layout.flexDirection(FlexDirection.COLUMN);
            layout.alignItems(AlignItems.CENTER);
            layout.paddingVertical(3);
            layout.paddingHorizontal(3);
        });
        row.style(style -> style.tooltips(title));
        row.addEventListener(UIEvents.CLICK, event -> selectCategory(categoryId));

        row.addChild(createCategoryIcon(icon, title));
        categoryListPanel.addChild(row);
    }

    private void reloadCategoryPager() {
        int maxPage = maxCategoryPage();
        if (maxPage <= 0) {
            categoryPager.setVisible(false);
            return;
        }
        categoryPager.setVisible(true);
        categoryPagerLabel.setText(Component.literal(">"));
        categoryPager.style(style -> style.tooltips(Component.literal((categoryPage + 1) + "/" + (maxPage + 1))));
    }

    private void reloadQuestTree() {
        questTreeView.clearAllScrollViewChildren();

        List<PlayerQuestState> quests = getFilteredQuests();
        if (!ClientConfig.SHOW_COMPLETED_QUESTS_IN_BOOK.get()) {
            quests = quests.stream()
                    .filter(quest -> quest.status != QuestStatus.COMPLETED)
                    .toList();
        }
        if (quests.isEmpty()) {
            questTreeView.addScrollViewChild(emptyMessage("viscript_quests.quest_book.empty"));
            return;
        }

        for (PlayerQuestState quest : quests) {
            addQuestTreeItem(quest);
        }
    }

    private void addQuestTreeItem(PlayerQuestState quest) {
        boolean selected = selectedQuest != null && selectedQuest.questId.equals(quest.questId);
        boolean tracked = playerData.trackedQuestId.equals(quest.questId);
        boolean completedQuest = quest.status == QuestStatus.COMPLETED;
        boolean expanded = expandedQuestIds.contains(quest.questId);

        IGuiTexture rowTexture = selected ? QUEST_SELECTED : completedQuest ? QUEST_COMPLETED : QUEST_DEFAULT;
        UIElement row = texturedPanel(rowTexture);
        row.layout(layout -> {
            layout.widthPercent(100);
            layout.height(QUEST_ITEM_HEIGHT);
            layout.flexDirection(FlexDirection.ROW);
            layout.alignItems(AlignItems.CENTER);
            layout.paddingVertical(2);
            layout.paddingHorizontal(3);
            layout.gapAll(3);
        });
        row.addEventListener(UIEvents.CLICK, event -> {
            toggleQuestExpanded(quest);
        });

        Label arrow = label(Component.literal(expanded ? "v" : ">"), 11);
        arrow.layout(layout -> layout.width(8));
        arrow.textStyle(style -> style.textColor(selected ? TEXT_GOLD : TEXT_MUTED).fontSize(FONT_ROW_TITLE));
        row.addChild(arrow);

        UIElement iconSlot = createDisplayIcon(quest.icon, Component.empty());
        iconSlot.layout(layout -> {
            layout.width(QUEST_ICON_SIZE);
            layout.height(QUEST_ICON_SIZE);
        });
        row.addChild(iconSlot);

        UIElement titleCol = new UIElement();
        titleCol.layout(layout -> {
            layout.width(0);
            layout.flex(1);
            layout.flexDirection(FlexDirection.COLUMN);
            layout.justifyContent(AlignContent.CENTER);
            layout.gapAll(1);
        });

        MutableComponent titleText = Component.literal(questTitle(quest));
        if (tracked) {
            titleText.append(Component.literal("  *"));
        }
        Label titleLabel = label(titleText, 11);
        titleLabel.textStyle(style -> style
                .textColor(completedQuest ? 0xFF9E977D : tracked ? TEXT_GOLD : TEXT_MAIN)
                .fontSize(FONT_ROW_TITLE)
                .textWrap(TextWrap.HIDE));
        titleCol.addChild(titleLabel);

        if (quest.subtitle != null && !quest.subtitle.isBlank()) {
            Label preview = label(Component.literal(quest.subtitle), 10);
            preview.textStyle(style -> style
                    .textColor(completedQuest ? 0xFF7F7968 : TEXT_MUTED)
                    .fontSize(FONT_ROW_SUBTITLE)
                    .textWrap(TextWrap.HIDE));
            titleCol.addChild(preview);
        }

        row.addChild(titleCol);

        questTreeView.addScrollViewChild(row);

        if (expanded) {
            List<TaskProgress> visibleTasks = getVisibleTasks(quest);
            for (TaskProgress taskProgress : visibleTasks) {
                addQuestTreeTaskItem(quest, taskProgress);
            }
        }
    }

    private void addQuestTreeTaskItem(PlayerQuestState quest, TaskProgress taskProgress) {
        boolean selected = selectedQuest != null
                && selectedQuest.questId.equals(quest.questId)
                && taskProgress.stepId.equals(selectedStepId);
        UIElement wrapper = new UIElement();
        wrapper.layout(layout -> {
            layout.widthPercent(100);
            layout.height(TASK_ITEM_HEIGHT);
            layout.paddingLeft(SUB_TASK_INDENT);
        });

        UIElement row = texturedPanel(selected ? QUEST_SELECTED : taskProgress.status == TaskStatus.COMPLETED ? QUEST_COMPLETED : QUEST_DEFAULT);
        row.layout(layout -> {
            layout.widthPercent(100);
            layout.heightPercent(100);
            layout.flexDirection(FlexDirection.ROW);
            layout.alignItems(AlignItems.CENTER);
            layout.paddingVertical(1);
            layout.paddingLeft(4);
            layout.paddingRight(4);
            layout.gapAll(3);
        });
        row.addEventListener(UIEvents.CLICK, event -> selectTask(quest, taskProgress.stepId));

        Label statusIcon = label(getTaskStatusIcon(taskProgress.status), 10);
        statusIcon.layout(layout -> layout.width(10));
        statusIcon.textStyle(style -> style.fontSize(FONT_SMALL));
        row.addChild(statusIcon);

        UIElement titleCol = new UIElement();
        titleCol.layout(layout -> {
            layout.width(0);
            layout.flex(1);
            layout.flexDirection(FlexDirection.COLUMN);
            layout.justifyContent(AlignContent.CENTER);
        });

        Label title = label(Component.literal(taskTitle(taskProgress)), 11);
        title.textStyle(style -> style
                .textColor(statusColor(taskProgress.status))
                .fontSize(FONT_ROW_TITLE)
                .textWrap(TextWrap.HIDE));
        titleCol.addChild(title);

        row.addChild(titleCol);

        UIElement statusTag = texturedPanel(STATUS_TAG);
        statusTag.layout(layout -> {
            layout.width(50);
            layout.height(15);
            layout.justifyContent(AlignContent.CENTER);
            layout.alignItems(AlignItems.CENTER);
            layout.paddingHorizontal(2);
        });
        Label statusText = label(taskProgress.status.displayName(), 10);
        statusText.textStyle(style -> style
                .textColor(statusColor(taskProgress.status))
                .fontSize(FONT_SMALL)
                .textAlignHorizontal(Horizontal.CENTER)
                .textAlignVertical(Vertical.CENTER)
                .textWrap(TextWrap.HIDE));
        statusTag.addChild(statusText);
        row.addChild(statusTag);

        wrapper.addChild(row);
        questTreeView.addScrollViewChild(wrapper);
    }

    private void reloadQuestDetail() {
        detailScroller.clearAllScrollViewChildren();
        TaskProgress selectedTask = getSelectedTask(selectedQuest);
        if (selectedQuest != null && selectedTask != null) {
            addTaskDetail(selectedQuest, selectedTask);
        }
    }

    private void addTaskDetail(PlayerQuestState quest, TaskProgress taskProgress) {
        addTaskDetailHeader(taskProgress);
        addTaskDescription(taskProgress);
        addTaskRequirement(quest, taskProgress);
        addRewardsForTask(quest, taskProgress.stepId);
        addTrackTaskButton(quest, taskProgress);
    }

    private void addTaskDetailHeader(TaskProgress taskProgress) {
        UIElement header = new UIElement();
        header.layout(layout -> {
            layout.widthPercent(100);
            layout.minHeight(30);
            layout.flexDirection(FlexDirection.COLUMN);
            layout.paddingAll(3);
            layout.gapAll(2);
        });

        UIElement titleCol = new UIElement();
        titleCol.layout(layout -> {
            layout.widthPercent(100);
            layout.flexDirection(FlexDirection.COLUMN);
            layout.gapAll(2);
        });

        Label title = label(Component.literal(taskTitle(taskProgress)), 14);
        title.textStyle(style -> style.textColor(TEXT_GOLD).fontSize(FONT_DETAIL_TITLE).textWrap(TextWrap.HIDE));
        titleCol.addChild(title);

        if (taskProgress.subtitle != null && !taskProgress.subtitle.isBlank()) {
            Label subtitle = label(Component.literal(taskProgress.subtitle), 12);
            subtitle.textStyle(style -> style.textColor(TEXT_MUTED).fontSize(FONT_BODY).textWrap(TextWrap.HIDE));
            titleCol.addChild(subtitle);
        }
        header.addChild(titleCol);

        detailScroller.addScrollViewChild(header);
    }

    private void addTaskDescription(TaskProgress taskProgress) {
        if (taskProgress.description == null || taskProgress.description.length == 0) {
            return;
        }
        addSectionHeader("viscript_quests.quest_book.description_label");
        UIElement card = texturedPanel(SECTION_PANEL);
        card.layout(layout -> {
            layout.widthPercent(100);
            layout.flexDirection(FlexDirection.COLUMN);
            layout.paddingAll(4);
            layout.gapAll(2);
        });
        for (String line : taskProgress.description) {
            if (line != null && !line.isBlank()) {
                card.addChild(wrappedLabel(Component.literal(line), 12, TEXT_MAIN));
            }
        }
        detailScroller.addScrollViewChild(card);
    }

    private void addTaskRequirement(PlayerQuestState quest, TaskProgress taskProgress) {
        addSectionHeader("viscript_quests.quest_book.objectives_label");
        UIElement card = texturedPanel(SECTION_PANEL);
        card.layout(layout -> {
            layout.widthPercent(100);
            layout.minHeight(24);
            layout.flexDirection(FlexDirection.COLUMN);
            layout.paddingAll(4);
            layout.gapAll(3);
        });

        if (taskProgress.objectives.isEmpty()) {
            card.addChild(wrappedLabel(taskHint(taskProgress), 12, TEXT_MAIN));
        } else {
            for (int i = 0; i < taskProgress.objectives.size(); i++) {
                card.addChild(createObjectiveRow(quest, taskProgress, i, taskProgress.objectives.get(i)));
            }
        }

        detailScroller.addScrollViewChild(card);
    }

    private UIElement createObjectiveRow(PlayerQuestState quest, TaskProgress taskProgress,
                                         int objectiveIndex, TaskObjectiveProgress objective) {
        UIElement row = new UIElement();
        row.layout(layout -> {
            layout.widthPercent(100);
            layout.height(OBJECTIVE_ROW_HEIGHT);
            layout.flexDirection(FlexDirection.ROW);
            layout.alignItems(AlignItems.CENTER);
            layout.gapAll(4);
        });

        UIElement icon = createDisplayIcon(objective.displayIcon, objective.displayHint());
        icon.layout(layout -> {
            layout.width(OBJECTIVE_ICON_SIZE);
            layout.height(OBJECTIVE_ICON_SIZE);
        });
        row.addChild(icon);

        int textColor = objective.completed ? 0xFF9DE8A8 : TEXT_MAIN;
        Label taskLabel = objectiveLabel(objective.progressHint(), textColor);
        taskLabel.layout(layout -> {
            layout.width(0);
            layout.flex(1);
        });
        row.addChild(taskLabel);

        if (taskProgress.status == TaskStatus.ACTIVE && objective.manualSubmitRequired && !objective.completed) {
            row.addChild(createObjectiveSubmitButton(quest, taskProgress, objectiveIndex));
        }
        return row;
    }

    private void addRewardsForTask(PlayerQuestState quest, String stepId) {
        List<RewardDisplay> rewards = quest.rewardDisplays.stream()
                .filter(reward -> reward.stepId.equals(stepId))
                .toList();
        addRewards(rewards);
    }

    private void addRewards(List<RewardDisplay> rewardsToShow) {
        if (rewardsToShow.isEmpty()) return;
        addSectionHeader("viscript_quests.quest_book.rewards_label");
        UIElement rewards = new UIElement();
        rewards.layout(layout -> {
            layout.widthPercent(100);
            layout.flexDirection(FlexDirection.ROW);
            layout.wrap(FlexWrap.WRAP);
            layout.paddingVertical(2);
            layout.gapAll(6);
        });

        for (RewardDisplay rewardDisplay : rewardsToShow) {
            addRewardRow(rewards, rewardDisplay);
        }
        detailScroller.addScrollViewChild(rewards);
    }

    private void addRewardRow(UIElement parent, RewardDisplay rewardDisplay) {
        UIElement slotFrame = texturedPanel(QUEST_DEFAULT);
        slotFrame.layout(layout -> {
            layout.width(REWARD_SLOT_SIZE);
            layout.height(REWARD_SLOT_SIZE);
            layout.justifyContent(AlignContent.CENTER);
            layout.alignItems(AlignItems.CENTER);
        });
        if (!rewardDisplay.displayText().getString().isBlank()) {
            slotFrame.style(style -> style.tooltips(rewardDisplay.displayText()));
        }

        UIElement rewardIcon = createDisplayIcon(rewardDisplay.icon, rewardDisplay.displayText().getString().isBlank()
                ? Component.empty()
                : rewardDisplay.displayText());
        rewardIcon.layout(layout -> {
            layout.width(REWARD_ICON_SIZE);
            layout.height(REWARD_ICON_SIZE);
        });
        slotFrame.addChild(rewardIcon);
        parent.addChild(slotFrame);
    }

    private void addTrackTaskButton(PlayerQuestState quest, TaskProgress taskProgress) {
        UIElement row = new UIElement();
        row.layout(layout -> {
            layout.widthPercent(100);
            layout.height(24);
            layout.flexDirection(FlexDirection.ROW);
            layout.justifyContent(AlignContent.FLEX_END);
            layout.alignItems(AlignItems.CENTER);
            layout.marginTop(4);
        });

        boolean tracked = isTrackedTask(quest, taskProgress);
        Button button = new Button();
        button.setText(Component.translatable(tracked
                ? "viscript_quests.quest_book.untrack_task_button"
                : "viscript_quests.quest_book.track_task_button"));
        button.buttonStyle(style -> style
                .baseTexture(TRACK_BUTTON)
                .hoverTexture(TRACK_BUTTON_HOVER)
                .pressedTexture(TRACK_BUTTON_PRESSED));
        button.textStyle(style -> style
                .textColor(tracked ? TEXT_MUTED : TEXT_GOLD)
                .fontSize(FONT_BODY)
                .textAlignHorizontal(Horizontal.CENTER)
                .textAlignVertical(Vertical.CENTER)
                .textWrap(TextWrap.HIDE));
        button.layout(layout -> {
            layout.width(82);
            layout.height(18);
        });
        button.addEventListener(UIEvents.CLICK, event -> toggleTrackedTask(quest, taskProgress));
        row.addChild(button);
        detailScroller.addScrollViewChild(row);
    }

    private Button createObjectiveSubmitButton(PlayerQuestState quest, TaskProgress taskProgress, int objectiveIndex) {
        Button button = new Button();
        button.setText(Component.translatable("viscript_quests.quest_book.submit_task_button"));
        button.buttonStyle(style -> style
                .baseTexture(TRACK_BUTTON)
                .hoverTexture(TRACK_BUTTON_HOVER)
                .pressedTexture(TRACK_BUTTON_PRESSED));
        button.textStyle(style -> style
                .textColor(TEXT_GOLD)
                .fontSize(FONT_BODY)
                .textAlignHorizontal(Horizontal.CENTER)
                .textAlignVertical(Vertical.CENTER)
                .textWrap(TextWrap.HIDE));
        button.layout(layout -> {
            layout.width(54);
            layout.height(16);
        });
        button.style(style -> style.tooltips(Component.translatable("viscript_quests.quest_book.submit_task_button.tooltip")));
        button.addEventListener(UIEvents.CLICK, event -> submitObjective(quest, taskProgress, objectiveIndex));
        return button;
    }

    private void addSectionHeader(String key) {
        UIElement header = new UIElement();
        header.layout(layout -> {
            layout.widthPercent(100);
            layout.flexDirection(FlexDirection.ROW);
            layout.alignItems(AlignItems.CENTER);
            layout.gapAll(5);
            layout.marginTop(2);
        });

        Label marker = label(Component.literal(">"), 12);
        marker.layout(layout -> layout.width(8));
        marker.textStyle(style -> style.textColor(TEXT_GOLD).fontSize(FONT_SMALL));
        header.addChild(marker);

        Label label = label(Component.translatable(key), 12);
        label.layout(layout -> {
            layout.width(0);
            layout.flex(1);
        });
        label.textStyle(style -> style.textColor(TEXT_GOLD).fontSize(FONT_BODY));
        header.addChild(label);
        detailScroller.addScrollViewChild(header);
    }

    private UIElement emptyMessage(String key) {
        UIElement empty = texturedPanel(SECTION_PANEL);
        empty.layout(layout -> {
            layout.widthPercent(100);
            layout.minHeight(34);
            layout.paddingAll(7);
            layout.justifyContent(AlignContent.CENTER);
        });
        Label label = wrappedLabel(Component.translatable(key), 12, TEXT_MAIN);
        empty.addChild(label);
        return empty;
    }

    private ItemSlot displayItem(ItemStack stack) {
        ItemSlot slot = new ItemSlot();
        slot.setItem(stack == null ? ItemStack.EMPTY : stack);
        slot.style(style -> style.backgroundTexture(IGuiTexture.EMPTY));
        slot.slotStyle(style -> style
                .slotOverlay(IGuiTexture.EMPTY)
                .hoverOverlay(IGuiTexture.EMPTY)
                .showItemTooltips(false));
        return slot;
    }

    private UIElement createCategoryIcon(Object icon, Component tooltip) {
        if (icon instanceof DisplayIcon displayIcon && displayIcon.isTexture()) {
            UIElement texture = texturedPanel(resolveCategoryTexture(displayIcon.getTexture()));
            texture.layout(layout -> {
                layout.width(BOOKMARK_ICON_SIZE);
                layout.height(BOOKMARK_ICON_SIZE);
            });
            applyOptionalTooltip(texture, tooltip);
            return texture;
        }
        ItemStack stack = icon instanceof DisplayIcon displayIcon
                ? displayIcon.renderItemStack()
                : icon instanceof ItemStack itemStack ? itemStack : DEFAULT_ICON;
        ItemSlot iconSlot = displayItem(stack == null || stack.isEmpty() ? DEFAULT_ICON : stack);
        iconSlot.layout(layout -> {
            layout.width(BOOKMARK_ICON_SIZE);
            layout.height(BOOKMARK_ICON_SIZE);
        });
        applyOptionalTooltip(iconSlot, tooltip);
        return iconSlot;
    }

    private UIElement createDisplayIcon(DisplayIcon icon, Component tooltip) {
        if (icon != null && icon.isTexture()) {
            UIElement texture = texturedPanel(resolveCategoryTexture(icon.getTexture()));
            applyOptionalTooltip(texture, tooltip);
            return texture;
        }
        ItemStack stack = icon == null ? ItemStack.EMPTY : icon.renderItemStack();
        ItemSlot slot = displayItem(stack == null || stack.isEmpty() ? DEFAULT_ICON : stack);
        applyOptionalTooltip(slot, tooltip);
        return slot;
    }

    private void applyOptionalTooltip(UIElement element, Component tooltip) {
        if (hasTooltipText(tooltip)) {
            element.style(style -> style.tooltips(tooltip));
        } else {
            element.setAllowHitTest(false);
        }
    }

    private boolean hasTooltipText(Component tooltip) {
        return tooltip != null && !tooltip.getString().isBlank();
    }

    private UIElement texturedPanel(IGuiTexture texture) {
        UIElement panel = new UIElement();
        panel.style(style -> style.backgroundTexture(texture));
        return panel;
    }

    private UIElement panel(int background, int border) {
        UIElement panel = new UIElement();
        panel.style(style -> style.backgroundTexture(texture(background, border)));
        return panel;
    }

    private static IGuiTexture sprite(String fileName, int border) {
        SpriteTexture texture = SpriteTexture.of(ViScriptQuests.id("textures/gui/quest_book/" + fileName));
        if (border > 0) {
            texture.setBorder(border);
        }
        return texture;
    }

    private static IGuiTexture texture(int background, int border) {
        return GuiTextureGroup.of(new ColorRectTexture(background), new ColorBorderTexture(-1, border));
    }

    private Label label(Component text, int height) {
        Label label = new Label();
        label.setText(text);
        label.layout(layout -> {
            layout.widthPercent(100);
            layout.height(height);
        });
        label.textStyle(style -> style.textColor(TEXT_MAIN));
        return label;
    }

    private Label wrappedLabel(Component text, int minHeight, int color) {
        Label label = new Label();
        label.setText(text);
        label.layout(layout -> {
            layout.widthPercent(100);
            layout.minHeight(minHeight);
        });
        label.textStyle(style -> style
                .textColor(color)
                .fontSize(FONT_BODY)
                .textWrap(TextWrap.WRAP)
                .adaptiveHeight(true));
        return label;
    }

    private Label objectiveLabel(Component text, int color) {
        Label label = new Label();
        label.setText(text);
        label.layout(layout -> {
            layout.widthPercent(100);
            layout.height(OBJECTIVE_ROW_HEIGHT);
        });
        label.textStyle(style -> style
                .textColor(color)
                .fontSize(FONT_BODY)
                .textAlignHorizontal(Horizontal.LEFT)
                .textAlignVertical(Vertical.CENTER)
                .textWrap(TextWrap.HIDE));
        return label;
    }

    private List<PlayerQuestState> getFilteredQuests() {
        List<PlayerQuestState> filtered = new ArrayList<>();
        if (findCategory(selectedCategoryId) == null) {
            return filtered;
        }
        for (PlayerQuestState quest : playerData.quests) {
            if (selectedCategoryId.equals(quest.categoryId)) {
                filtered.add(quest);
            }
        }
        return filtered;
    }

    private List<QuestCategoryData> pagedCategories() {
        int start = categoryPage * CATEGORIES_PER_PAGE;
        int end = Math.min(playerData.categories.size(), start + CATEGORIES_PER_PAGE);
        if (start >= end) {
            return List.of();
        }
        return playerData.categories.subList(start, end);
    }

    private int maxCategoryPage() {
        int count = playerData.categories.size();
        if (count <= CATEGORIES_PER_PAGE) {
            return 0;
        }
        return (count - 1) / CATEGORIES_PER_PAGE;
    }

    private QuestCategoryData findCategory(String categoryId) {
        String normalizedId = QuestCategoryData.normalizeId(categoryId);
        return playerData.categories.stream()
                .filter(category -> category.id.equals(normalizedId))
                .findFirst()
                .orElse(null);
    }

    private int categoryIndex(String categoryId) {
        String normalizedId = QuestCategoryData.normalizeId(categoryId);
        for (int i = 0; i < playerData.categories.size(); i++) {
            if (playerData.categories.get(i).id.equals(normalizedId)) {
                return i;
            }
        }
        return -1;
    }

    private String questTitle(PlayerQuestState quest) {
        return quest.title == null || quest.title.isBlank() ? quest.questId : quest.title;
    }

    private Component getQuestPreview(PlayerQuestState quest) {
        if (quest.subtitle != null && !quest.subtitle.isBlank()) {
            return Component.literal(quest.subtitle);
        }
        for (TaskProgress taskProgress : quest.taskProgresses) {
            if (taskProgress.status == TaskStatus.ACTIVE) {
                return taskHint(taskProgress);
            }
        }
        return Component.literal(quest.questId);
    }

    private TaskProgress getSelectedTask(PlayerQuestState quest) {
        if (quest == null || selectedStepId == null || selectedStepId.isBlank()) {
            return null;
        }
        return quest.taskProgresses.stream()
                .filter(progress -> progress.stepId.equals(selectedStepId))
                .findFirst()
                .orElse(null);
    }

    private List<TaskProgress> getVisibleTasks(PlayerQuestState quest) {
        return quest.taskProgresses.stream()
                .filter(this::isVisibleTaskInBook)
                .toList();
    }

    private boolean isVisibleTaskInBook(TaskProgress progress) {
        if (progress.status == TaskStatus.HIDDEN || progress.status == TaskStatus.LOCKED) {
            return false;
        }
        if (progress.status == TaskStatus.COMPLETED || progress.status == TaskStatus.SKIPPED) {
            return ClientConfig.SHOW_COMPLETED_TASKS_IN_BOOK.get();
        }
        return true;
    }

    private boolean isTrackedTask(PlayerQuestState quest, TaskProgress taskProgress) {
        return playerData.trackedQuestId.equals(quest.questId)
                && playerData.trackedStepId.equals(taskProgress.stepId);
    }

    private void toggleTrackedTask(PlayerQuestState quest, TaskProgress taskProgress) {
        boolean tracked = isTrackedTask(quest, taskProgress);
        playerData.trackedQuestId = tracked ? "" : quest.questId;
        playerData.trackedStepId = tracked ? "" : taskProgress.stepId;
        sendTrackedTask();
        reloadQuestTree();
        reloadQuestDetail();
    }

    private void sendTrackedTask() {
        CompoundTag tag = new CompoundTag();
        tag.putString("trackedQuestId", playerData.trackedQuestId == null ? "" : playerData.trackedQuestId);
        tag.putString("trackedStepId", playerData.trackedStepId == null ? "" : playerData.trackedStepId);
        RPCPacketDistributor.rpcToServer(C2SPayload.SAVE_TRACKED_QUEST, tag);
    }

    private void submitObjective(PlayerQuestState quest, TaskProgress taskProgress, int objectiveIndex) {
        CompoundTag tag = new CompoundTag();
        tag.putString("questId", quest.questId);
        tag.putString("stepId", taskProgress.stepId);
        tag.putInt("objectiveIndex", objectiveIndex);
        RPCPacketDistributor.rpcToServer(C2SPayload.SUBMIT_QUEST_TASK, tag);
    }

    private String taskTitle(TaskProgress taskProgress) {
        return taskProgress.title == null || taskProgress.title.isBlank()
                ? taskProgress.stepId
                : taskProgress.title;
    }

    private Component taskHint(TaskProgress taskProgress) {
        if (taskProgress.displayTaskHint() != null && !taskProgress.displayTaskHint().getString().isBlank()) {
            return taskProgress.displayTaskHint();
        }
        if (taskProgress.title != null && !taskProgress.title.isBlank()) {
            return Component.literal(taskProgress.title);
        }
        return Component.literal(taskProgress.stepId);
    }

    private static Component getTaskStatusIcon(TaskStatus status) {
        return switch (status) {
            case COMPLETED -> Component.literal("§a✓");
            case FAILED -> Component.literal("§c×");
            case HIDDEN -> Component.literal("§7?");
            case ACTIVE -> Component.literal("§e●");
            case LOCKED -> Component.literal("§8□");
            case SKIPPED -> Component.literal("§7-");
        };
    }

    private static int statusColor(TaskStatus status) {
        return switch (status) {
            case COMPLETED -> 0xFF9EE08F;
            case FAILED -> 0xFFFF8A8A;
            case ACTIVE -> 0xFFFFE6A0;
            case SKIPPED -> 0xFF9B9B92;
            case HIDDEN, LOCKED -> 0xFF777B76;
        };
    }

    private static IGuiTexture resolveCategoryTexture(String textureId) {
        if (textureId == null || textureId.isBlank()) {
            return BOOK_ICON_TEXTURE;
        }
        ResourceLocation resourceLocation = ResourceLocation.tryParse(textureId);
        return resourceLocation == null ? BOOK_ICON_TEXTURE : SpriteTexture.of(resourceLocation);
    }

    private static ItemStack resolveCategoryItemIcon(String itemId) {
        if (itemId == null || itemId.isBlank()) {
            return DEFAULT_ICON;
        }
        try {
            ResourceLocation resourceLocation = ResourceLocation.tryParse(itemId);
            if (resourceLocation == null) {
                return DEFAULT_ICON;
            }
            return BuiltInRegistries.ITEM.getOptional(resourceLocation)
                    .map(ItemStack::new)
                    .orElse(DEFAULT_ICON);
        } catch (Exception ignored) {
            return DEFAULT_ICON;
        }
    }
    @Override
    public void initScreen(int screenWidth, int screenHeight) {
        super.initScreen(screenWidth, screenHeight);
        applyAutoGuiScaleTransform();
    }

    public static Size getAutoGuiScaledSize(Size screenSize) {
        float scale = getAutoGuiScaleFactor();
        if (scale <= 0f) {
            return screenSize;
        }
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
        if (currentScale <= 0d) {
            return 1f;
        }
        int autoScale = window.calculateScale(0, minecraft.isEnforceUnicode());
        return Math.max(1f, (float) (autoScale / currentScale));
    }
}
