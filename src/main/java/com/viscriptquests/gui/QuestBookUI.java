package com.viscriptquests.gui;

import com.lowdragmc.lowdraglib2.gui.texture.ColorRectTexture;
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.texture.SpriteTexture;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.data.Horizontal;
import com.lowdragmc.lowdraglib2.gui.ui.data.ScrollDisplay;
import com.lowdragmc.lowdraglib2.gui.ui.data.ScrollerMode;
import com.lowdragmc.lowdraglib2.gui.ui.data.TextWrap;
import com.lowdragmc.lowdraglib2.gui.ui.data.Vertical;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.gui.ui.elements.ItemSlot;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.gui.ui.elements.ScrollerView;
import com.lowdragmc.lowdraglib2.math.Size;
import com.lowdragmc.lowdraglib2.networking.rpc.RPCPacketDistributor;
import com.viscript_lib.util.RichTextUtil;
import com.viscriptquests.ViScriptQuests;
import com.viscriptquests.compat.ponder.PonderCompat;
import com.viscriptquests.config.ClientConfig;
import com.viscriptquests.network.c2s.C2SPayload;
import com.viscriptquests.quest.data.DisplayIcon;
import com.viscriptquests.quest.data.runtime.PlayerQuestState;
import com.viscriptquests.quest.data.runtime.QuestBookData;
import com.viscriptquests.quest.data.runtime.QuestCategoryData;
import com.viscriptquests.quest.data.runtime.QuestCategoryListData;
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
import dev.vfyjxf.taffy.style.TaffyPosition;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** 玩家任务书。界面使用固定 480×380 书本坐标系，任务与进度操作仍由原有运行时数据驱动。 */
public class QuestBookUI extends UIElement {
    private static final int BOOK_WIDTH = 480;
    private static final int BOOK_HEIGHT = 380;
    private static final float MAX_SCREEN_WIDTH_RATIO = 0.80f;
    private static final float MAX_SCREEN_HEIGHT_RATIO = 0.90f;

    private static final int CATEGORY_X = 19;
    private static final int CATEGORY_Y = 68;
    private static final int CATEGORY_HEIGHT = 18;
    private static final int CATEGORY_GAP = 5;
    private static final int CATEGORIES_PER_PAGE = 5;

    private static final int QUEST_LIST_X = 51;
    private static final int QUEST_LIST_Y = 63;
    private static final int QUEST_LIST_WIDTH = 167;
    private static final int QUEST_LIST_HEIGHT = 274;
    private static final int QUEST_SUMMARY_HEIGHT = 40;
    private static final int QUEST_GROUP_GAP = 4;
    private static final int SUB_TASK_SPRITE_HEIGHT = 12;
    private static final int SUB_TASK_HEIGHT = 16;

    private static final int DETAIL_X = 252;
    private static final int DETAIL_Y = 38;
    private static final int DETAIL_WIDTH = 188;
    private static final int DETAIL_HEIGHT = 314;

    private static final int TEXT_DARK = 0xFF61382F;
    private static final int TEXT_MUTED = 0xFF8A6657;
    private static final int TEXT_GREEN = 0xFF4D702F;
    private static final int TEXT_RED = 0xFF9A352E;
    private static final int TEXT_SELECTED = 0xFFFFD85A;

    private static final float FONT_TITLE = 9.0f;
    private static final float FONT_NORMAL = 8.0f;
    private static final float FONT_SMALL = 7.0f;

    private static final ItemStack DEFAULT_ICON = new ItemStack(Items.WRITABLE_BOOK);

    private static final IGuiTexture BOOK_BACKGROUND = sprite("book.png");
    private static final IGuiTexture QUEST_LIST_HEADER = sprite("quest_list_header.png");
    private static final IGuiTexture QUEST_TITLE_DECORATION = sprite("quest_title_decoration.png");
    private static final IGuiTexture QUEST_SUMMARY_BACKGROUND =
            spriteRegion("quest_list_entry.png", 0, 0, QUEST_LIST_WIDTH, QUEST_SUMMARY_HEIGHT);
    private static final IGuiTexture SUB_TASK_BACKGROUND =
            spriteRegion("quest_list_entry.png", 0, QUEST_SUMMARY_HEIGHT, QUEST_LIST_WIDTH, SUB_TASK_SPRITE_HEIGHT);
    private static final IGuiTexture OBJECTIVE_ICON_FRAME = sprite("objective_icon_frame.png");
    private static final IGuiTexture REWARD_ICON_FRAME = sprite("reward_icon_frame.png");
    private static final IGuiTexture SECTION_BACKGROUND_SHORT = sprite("section_background_short.png");
    private static final IGuiTexture SECTION_BACKGROUND_TALL = sprite("section_background_tall.png");
    private static final IGuiTexture DESCRIPTION_ICON = sprite("icon/quest_description.png");
    private static final IGuiTexture OBJECTIVES_ICON = sprite("icon/icon_task.png");
    private static final IGuiTexture REWARDS_ICON = sprite("icon/quest_reward.png");
    private static final IGuiTexture BUTTON_DEFAULT = borderedSprite("button/button_default.png", 3);
    private static final IGuiTexture BUTTON_HOVER = borderedSprite("button/button_hover.png", 3);
    private static final IGuiTexture BUTTON_HOLD = borderedSprite("button/button_hold.png", 3);

    private QuestPlayerData playerData;
    private QuestCategoryListData categoryData;
    private String selectedCategoryId = "";
    private PlayerQuestState selectedQuest;
    private String selectedStepId = "";
    private int categoryPage;
    private final Set<String> collapsedQuestIds = new HashSet<>();

    private UIElement categoryListPanel;
    private Button categoryPager;
    private ScrollerView questListView;
    private UIElement detailPanel;
    private UIElement bookElement;

    public QuestBookUI(QuestBookData bookData) {
        this.playerData = bookData == null || bookData.playerData == null
                ? new QuestPlayerData()
                : bookData.playerData;
        this.categoryData = bookData == null || bookData.categoryData == null
                ? new QuestCategoryListData()
                : bookData.categoryData;
        buildUI();
    }

    public void syncBookData(QuestBookData bookData) {
        String selectedQuestId = selectedQuest == null ? "" : selectedQuest.questId;
        String selectedTaskId = selectedStepId;
        playerData = bookData == null || bookData.playerData == null
                ? new QuestPlayerData()
                : bookData.playerData;
        categoryData = bookData == null || bookData.categoryData == null
                ? new QuestCategoryListData()
                : bookData.categoryData;
        selectedQuest = playerData.findQuest(selectedQuestId).orElse(null);
        selectedStepId = selectedTaskId == null ? "" : selectedTaskId;
        refreshAll();
    }

    private void buildUI() {
        layout(layout -> {
            layout.widthPercent(100);
            layout.heightPercent(100);
            layout.alignItems(AlignItems.CENTER);
            layout.justifyContent(AlignContent.CENTER);
        });
        style(style -> style.backgroundTexture(new ColorRectTexture(0x70000000)));

        bookElement = texturedPanel(BOOK_BACKGROUND);
        bookElement.layout(layout -> {
            layout.width(BOOK_WIDTH);
            layout.height(BOOK_HEIGHT);
        });
        addChild(bookElement);

        buildCategoryTabs(bookElement);
        buildQuestList(bookElement);
        buildDetailPanel(bookElement);
        refreshAll();
    }

    private void buildCategoryTabs(UIElement book) {
        categoryListPanel = place(new UIElement(), CATEGORY_X, CATEGORY_Y, 30,
                CATEGORIES_PER_PAGE * (CATEGORY_HEIGHT + CATEGORY_GAP));
        categoryListPanel.layout(layout -> {
            layout.flexDirection(FlexDirection.COLUMN);
            layout.gapAll(CATEGORY_GAP);
        });
        book.addChild(categoryListPanel);

        categoryPager = new Button().noText();
        categoryPager.buttonStyle(style -> style
                .baseTexture(SpriteTexture.of(QuestCategoryData.DEFAULT_TAB_BACKGROUND))
                .hoverTexture(SpriteTexture.of(QuestCategoryData.DEFAULT_SELECTED_TAB_BACKGROUND))
                .pressedTexture(SpriteTexture.of(QuestCategoryData.DEFAULT_SELECTED_TAB_BACKGROUND)));
        place(categoryPager, CATEGORY_X, CATEGORY_Y + CATEGORIES_PER_PAGE * (CATEGORY_HEIGHT + CATEGORY_GAP),
                20, CATEGORY_HEIGHT);
        Label pagerText = centeredLabel(Component.literal("»"), FONT_NORMAL, TEXT_DARK);
        pagerText.setAllowHitTest(false);
        categoryPager.addChild(pagerText);
        categoryPager.setOnClick(event -> {
            int maxPage = maxCategoryPage();
            if (maxPage > 0) {
                categoryPage = categoryPage >= maxPage ? 0 : categoryPage + 1;
                reloadCategoryTabs();
            }
        });
        book.addChild(categoryPager);
    }

    private void buildQuestList(UIElement book) {
        UIElement header = place(new UIElement(), QUEST_LIST_X, 47, QUEST_LIST_WIDTH, 15);
        UIElement decoration = place(texturedPanel(QUEST_LIST_HEADER), 0, 5, 113, 9);
        decoration.setAllowHitTest(false);
        header.addChild(decoration);

        Label title = place(label(Component.translatable("viscript_quests.quest_book.quest_list"), FONT_TITLE,
                TEXT_DARK), 10, 3, 145, 11);
        title.setAllowHitTest(false);
        header.addChild(title);
        book.addChild(header);

        questListView = place(new ScrollerView(), QUEST_LIST_X, QUEST_LIST_Y, QUEST_LIST_WIDTH, QUEST_LIST_HEIGHT);
        configureVerticalScroller(questListView, 0, 0);
        book.addChild(questListView);
    }

    private void buildDetailPanel(UIElement book) {
        detailPanel = place(new UIElement(), DETAIL_X, DETAIL_Y, DETAIL_WIDTH, DETAIL_HEIGHT);
        book.addChild(detailPanel);
    }

    private void refreshAll() {
        ensureSelectedCategory();
        ensureSelectedEntry();
        reloadCategoryTabs();
        reloadQuestList();
        reloadDetail();
    }

    private void ensureSelectedCategory() {
        categoryPage = Math.max(0, Math.min(categoryPage, maxCategoryPage()));
        if (categoryData.categories.isEmpty()) {
            selectedCategoryId = "";
            return;
        }
        if (findCategory(selectedCategoryId) == null) {
            selectedCategoryId = categoryData.categories.getFirst().id;
        }
        int selectedIndex = categoryIndex(selectedCategoryId);
        if (selectedIndex >= 0) {
            categoryPage = selectedIndex / CATEGORIES_PER_PAGE;
        }
    }

    private void ensureSelectedEntry() {
        if (selectedQuest == null || selectedStepId == null || selectedStepId.isBlank()) {
            selectedQuest = null;
            selectedStepId = "";
            return;
        }

        QuestListEntry selected = getQuestListEntries().stream()
                .filter(entry -> entry.quest.questId.equals(selectedQuest.questId)
                        && entry.stepId().equals(selectedStepId))
                .findFirst()
                .orElse(null);
        if (selected == null) {
            selectedQuest = null;
            selectedStepId = "";
            return;
        }
        selectedQuest = selected.quest;
        selectedStepId = selected.stepId();
    }

    private void selectCategory(String categoryId) {
        selectedCategoryId = QuestCategoryData.normalizeId(categoryId);
        selectedQuest = null;
        selectedStepId = "";
        refreshAll();
    }

    private void selectEntry(QuestListEntry entry) {
        selectedQuest = entry.quest;
        selectedStepId = entry.stepId();
        reloadQuestList();
        reloadDetail();
    }

    private void toggleQuestCollapsed(PlayerQuestState quest) {
        if (!collapsedQuestIds.add(quest.questId)) {
            collapsedQuestIds.remove(quest.questId);
        }
        reloadQuestList();
    }

    private void reloadCategoryTabs() {
        categoryPage = Math.max(0, Math.min(categoryPage, maxCategoryPage()));
        categoryListPanel.clearAllChildren();
        for (QuestCategoryData category : pagedCategories()) {
            boolean selected = category.id.equals(selectedCategoryId);
            Button tab = new Button().noText();
            IGuiTexture normalTexture = SpriteTexture.of(category.tabBackgroundLocation(false));
            IGuiTexture selectedTexture = SpriteTexture.of(category.tabBackgroundLocation(true));
            tab.buttonStyle(style -> style
                    .baseTexture(selected ? selectedTexture : normalTexture)
                    .hoverTexture(selectedTexture)
                    .pressedTexture(selectedTexture));
            tab.layout(layout -> {
                layout.width(selected ? 30 : 20);
                layout.height(CATEGORY_HEIGHT);
            });
            String categoryTitle = category.title == null || category.title.isBlank() ? category.id : category.title;
            tab.style(style -> style.tooltips(richText(categoryTitle)));
            tab.setOnClick(event -> selectCategory(category.id));

            UIElement icon = createDisplayIcon(category.displayIcon, Component.empty());
            place(icon, 5, 2, 14, 14);
            icon.setAllowHitTest(false);
            tab.addChild(icon);
            categoryListPanel.addChild(tab);
        }

        int maxPage = maxCategoryPage();
        categoryPager.setVisible(maxPage > 0);
        if (maxPage > 0) {
            categoryPager.style(style -> style.tooltips(Component.literal(
                    (categoryPage + 1) + "/" + (maxPage + 1))));
        }
    }

    private void reloadQuestList() {
        questListView.clearAllScrollViewChildren();
        boolean firstGroup = true;
        for (QuestListGroup group : getQuestListGroups()) {
            if (!firstGroup) {
                questListView.addScrollViewChild(createQuestGroupSpacer());
            }
            firstGroup = false;
            questListView.addScrollViewChild(createQuestSummary(group.quest));
            if (!collapsedQuestIds.contains(group.quest.questId)) {
                for (TaskProgress task : group.tasks) {
                    questListView.addScrollViewChild(createSubTaskEntry(new QuestListEntry(group.quest, task)));
                }
            }
        }
    }

    private UIElement createQuestGroupSpacer() {
        UIElement spacer = new UIElement();
        spacer.layout(layout -> {
            layout.width(QUEST_LIST_WIDTH);
            layout.height(QUEST_GROUP_GAP);
        });
        spacer.setAllowHitTest(false);
        return spacer;
    }

    private UIElement createQuestSummary(PlayerQuestState quest) {
        UIElement row = texturedPanel(QUEST_SUMMARY_BACKGROUND);
        row.layout(layout -> {
            layout.width(QUEST_LIST_WIDTH);
            layout.height(QUEST_SUMMARY_HEIGHT);
        });
        row.addEventListener(com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents.CLICK,
                event -> toggleQuestCollapsed(quest));

        UIElement iconFrame = place(texturedPanel(OBJECTIVE_ICON_FRAME), 10, 5, 22, 22);
        UIElement icon = place(createDisplayIcon(quest.icon, Component.empty()), 3, 3, 16, 16);
        icon.setAllowHitTest(false);
        iconFrame.addChild(icon);
        iconFrame.setAllowHitTest(false);
        row.addChild(iconFrame);

        String questTitle = quest.title == null || quest.title.isBlank() ? quest.questId : quest.title;
        Label title = place(label(richText(questTitle), FONT_NORMAL, TEXT_DARK), 39, 5, 116, 10);
        title.textStyle(style -> style.textWrap(TextWrap.HIDE));
        title.setAllowHitTest(false);
        row.addChild(title);

        String subtitleText = quest.subtitle == null ? "" : quest.subtitle;
        Label subtitle = place(label(richText(subtitleText), FONT_SMALL, TEXT_MUTED), 39, 16, 116, 9);
        subtitle.textStyle(style -> style.textWrap(TextWrap.HIDE));
        subtitle.setAllowHitTest(false);
        row.addChild(subtitle);
        return row;
    }

    private UIElement createSubTaskEntry(QuestListEntry entry) {
        boolean selected = selectedQuest != null
                && selectedQuest.questId.equals(entry.quest.questId)
                && selectedStepId.equals(entry.stepId());
        UIElement row = texturedPanel(SUB_TASK_BACKGROUND);
        row.layout(layout -> {
            layout.width(QUEST_LIST_WIDTH);
            layout.height(SUB_TASK_HEIGHT);
        });
        row.addEventListener(com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents.CLICK, event -> selectEntry(entry));

        UIElement content = new UIElement();
        content.layout(layout -> {
            layout.widthPercent(100);
            layout.height(SUB_TASK_SPRITE_HEIGHT);
            layout.flexDirection(FlexDirection.ROW);
            layout.alignItems(AlignItems.CENTER);
            layout.paddingHorizontal(4);
            layout.gapAll(3);
        });
        row.addChild(content);

        Label title = label(entry.title(), FONT_SMALL, selected ? TEXT_SELECTED : TEXT_DARK);
        title.layout(layout -> {
            layout.width(0);
            layout.flex(1);
            layout.minWidth(20);
            layout.height(10);
        });
        title.textStyle(style -> style
                .textAlignVertical(Vertical.CENTER)
                .textWrap(TextWrap.HIDE));
        title.setAllowHitTest(false);
        content.addChild(title);

        Label status = label(entry.statusName(), FONT_SMALL, entry.statusColor());
        status.layout(layout -> {
            layout.height(12);
            layout.paddingHorizontal(5);
        });
        status.style(style -> style.backgroundTexture(
                SpriteTexture.of(entry.task.status.getTagTexture()).setBorder(2)));
        status.textStyle(style -> style
                .adaptiveWidth(true)
                .textAlignHorizontal(Horizontal.CENTER)
                .textAlignVertical(Vertical.CENTER)
                .textWrap(TextWrap.NONE));
        status.setAllowHitTest(false);
        content.addChild(status);
        return row;
    }

    private void reloadDetail() {
        detailPanel.clearAllChildren();
        TaskProgress task = getSelectedTask(selectedQuest);
        if (selectedQuest == null || task == null) {
            return;
        }
        addTaskDetail(selectedQuest, task);
    }

    private void addTaskDetail(PlayerQuestState quest, TaskProgress task) {
        addDetailTitle(richText(taskTitle(task)), richText(task.subtitle));

        List<Component> description = new ArrayList<>();
        if (task.description != null) {
            for (String line : task.description) {
                if (line != null && !line.isBlank()) {
                    description.add(richText(line));
                }
            }
        }
        if (description.isEmpty() && task.subtitle != null && !task.subtitle.isBlank()) {
            description.add(richText(task.subtitle));
        }
        if (description.isEmpty()) {
            description.add(taskHint(task));
        }
        addDescription(description);
        addObjectives(quest, task);
        addRewards(rewardsForStep(quest, task.stepId));
        addTrackButton(quest, task);
    }

    private void addDetailTitle(Component titleText, Component subtitleText) {
        UIElement decoration = place(texturedPanel(QUEST_TITLE_DECORATION), 15, 10, 159, 16);
        decoration.setAllowHitTest(false);
        detailPanel.addChild(decoration);

        Label title = place(centeredLabel(titleText, FONT_TITLE, TEXT_DARK), 25, 12, 139, 12);
        title.textStyle(style -> style.textWrap(TextWrap.HOVER_ROLL));
        detailPanel.addChild(title);

        if (hasTooltipText(subtitleText)) {
            Label subtitle = place(centeredLabel(subtitleText, FONT_SMALL, TEXT_MUTED), 25, 27, 139, 9);
            subtitle.textStyle(style -> style.textWrap(TextWrap.HOVER_ROLL));
            detailPanel.addChild(subtitle);
        }
    }

    private void addDescription(List<Component> lines) {
        addSectionHeader(DESCRIPTION_ICON, "viscript_quests.quest_book.description_label", 44);
        UIElement card = place(texturedPanel(SECTION_BACKGROUND_SHORT), 0, 62, 188, 42);
        ScrollerView scroller = place(new ScrollerView(), 8, 4, 172, 34);
        configureVerticalScroller(scroller, 0, 1);
        for (Component line : lines) {
            Label text = wrappedLabel(line, TEXT_DARK);
            scroller.addScrollViewChild(text);
        }
        card.addChild(scroller);
        detailPanel.addChild(card);
    }

    private void addObjectives(PlayerQuestState quest, TaskProgress task) {
        addSectionHeader(OBJECTIVES_ICON, "viscript_quests.quest_book.objectives_label", 115);
        UIElement card = place(texturedPanel(SECTION_BACKGROUND_TALL), 0, 133, 188, 72);
        ScrollerView scroller = place(new ScrollerView(), 8, 3, 172, 66);
        configureVerticalScroller(scroller, 0, 1);

        if (task.objectives.isEmpty()) {
            scroller.addScrollViewChild(wrappedLabel(taskHint(task), TEXT_DARK));
        } else {
            for (int i = 0; i < task.objectives.size(); i++) {
                scroller.addScrollViewChild(createObjectiveRow(quest, task, i, task.objectives.get(i)));
            }
        }
        card.addChild(scroller);
        detailPanel.addChild(card);
    }

    private UIElement createObjectiveRow(PlayerQuestState quest, TaskProgress task, int objectiveIndex,
                                         TaskObjectiveProgress objective) {
        UIElement row = new UIElement();
        row.layout(layout -> {
            layout.width(172);
            layout.minHeight(22);
            layout.flexDirection(FlexDirection.ROW);
            layout.wrap(FlexWrap.WRAP);
            layout.alignItems(AlignItems.CENTER);
            layout.gapAll(3);
        });

        UIElement iconFrame = texturedPanel(OBJECTIVE_ICON_FRAME);
        iconFrame.layout(layout -> layout.width(22).height(22));
        UIElement icon = place(createDisplayIcon(objective.displayIcon, objectiveTooltip(objective)), 3, 3, 16, 16);
        iconFrame.addChild(icon);
        row.addChild(iconFrame);

        Label objectiveText = label(richText(objective.progressHintWithType()), FONT_SMALL, objectiveTextColor(objective));
        objectiveText.layout(layout -> {
            layout.width(0);
            layout.flex(1);
            layout.minWidth(60);
            layout.minHeight(20);
        });
        objectiveText.textStyle(style -> style
                .adaptiveHeight(true)
                .textAlignVertical(Vertical.CENTER)
                .textWrap(TextWrap.WRAP));
        row.addChild(objectiveText);

        if (objective.hasPonderView()) {
            row.addChild(createPonderButton(quest, task, objectiveIndex, objective));
        } else if (task.status == TaskStatus.ACTIVE && objective.manualSubmitRequired && !objective.completed) {
            row.addChild(createSubmitButton(quest, task, objectiveIndex));
        }
        return row;
    }

    private void addRewards(List<RewardDisplay> rewards) {
        addSectionHeader(REWARDS_ICON, "viscript_quests.quest_book.rewards_label", 217);
        ScrollerView rewardScroller = place(new ScrollerView(), 0, 238, 188, 28);
        configureHorizontalScroller(rewardScroller, 0, 8);
        for (RewardDisplay reward : rewards) {
            rewardScroller.addScrollViewChild(createRewardSlot(reward));
        }
        detailPanel.addChild(rewardScroller);
    }

    private UIElement createRewardSlot(RewardDisplay reward) {
        Component tooltip = reward == null ? Component.empty() : richText(reward.displayText());
        UIElement frame = texturedPanel(REWARD_ICON_FRAME);
        frame.layout(layout -> layout.width(24).height(24));
        if (hasTooltipText(tooltip)) {
            frame.style(style -> style.tooltips(tooltip));
        }
        DisplayIcon rewardIcon = reward == null ? null : reward.icon;
        UIElement icon = place(createDisplayIcon(rewardIcon, tooltip), 4, 4, 16, 16);
        frame.addChild(icon);
        return frame;
    }

    private void addTrackButton(PlayerQuestState quest, TaskProgress task) {
        boolean tracked = isTrackedTask(quest, task);
        Button button = actionButton(Component.translatable(tracked
                ? "viscript_quests.quest_book.untrack_task_button"
                : "viscript_quests.quest_book.track_task_button"));
        place(button, 110, 294, 78, 18);
        button.text.layout(layout -> {
            layout.width(0);
            layout.flex(1);
            layout.heightPercent(100);
        });
        button.textStyle(style -> style.adaptiveWidth(false).textWrap(TextWrap.HOVER_ROLL));
        button.setOverflowVisible(false);
        button.text.setOverflowVisible(false);
        button.setOnClick(event -> toggleTrackedTask(quest, task));
        detailPanel.addChild(button);
    }

    private Button createSubmitButton(PlayerQuestState quest, TaskProgress task, int objectiveIndex) {
        Button button = actionButton(Component.translatable("viscript_quests.quest_book.submit_task_button"));
        button.style(style -> style.tooltips(
                Component.translatable("viscript_quests.quest_book.submit_task_button.tooltip")));
        button.setOnClick(event -> submitObjective(quest, task, objectiveIndex));
        return button;
    }

    private Button createPonderButton(PlayerQuestState quest, TaskProgress task, int objectiveIndex,
                                      TaskObjectiveProgress objective) {
        Button button = actionButton(Component.translatable("viscript_quests.quest_book.view_ponder_button"));
        button.style(style -> style.tooltips(Component.translatable(
                "viscript_quests.quest_book.view_ponder_button.tooltip",
                objective.ponderComponentId == null ? "" : objective.ponderComponentId)));
        button.setOnClick(event -> viewPonderObjective(quest, task, objectiveIndex, objective));
        return button;
    }

    private Button actionButton(Component text) {
        Button button = new Button();
        button.setText(text);
        button.buttonStyle(style -> style
                .baseTexture(BUTTON_DEFAULT)
                .hoverTexture(BUTTON_HOVER)
                .pressedTexture(BUTTON_HOLD));
        button.textStyle(style -> style
                .textColor(TEXT_DARK)
                .fontSize(FONT_SMALL)
                .textShadow(false)
                .adaptiveWidth(true)
                .textAlignHorizontal(Horizontal.CENTER)
                .textAlignVertical(Vertical.CENTER)
                .textWrap(TextWrap.NONE));
        button.layout(layout -> layout.height(18));
        return button;
    }

    private void addSectionHeader(IGuiTexture iconTexture, String translationKey, int y) {
        UIElement icon = place(texturedPanel(iconTexture), 0, y, 16, 16);
        icon.setAllowHitTest(false);
        detailPanel.addChild(icon);

        Label title = place(label(Component.translatable(translationKey), FONT_TITLE, TEXT_DARK),
                18, y, 170, 16);
        title.textStyle(style -> style.textAlignVertical(Vertical.CENTER));
        title.setAllowHitTest(false);
        detailPanel.addChild(title);
    }

    private void configureVerticalScroller(ScrollerView scroller, int padding, int gap) {
        scroller.scrollerStyle(style -> style
                .mode(ScrollerMode.VERTICAL)
                .horizontalScrollDisplay(ScrollDisplay.NEVER)
                .verticalScrollDisplay(ScrollDisplay.NEVER)
                .scrollerViewStyle(0));
        scroller.viewPort(view -> view
                .layout(layout -> layout.paddingAll(padding))
                .style(style -> style.backgroundTexture(IGuiTexture.EMPTY)));
        scroller.viewContainer(view -> view.layout(layout -> {
            layout.widthPercent(100);
            layout.flexDirection(FlexDirection.COLUMN);
            layout.gapAll(gap);
        }));
    }

    private void configureHorizontalScroller(ScrollerView scroller, int padding, int gap) {
        scroller.scrollerStyle(style -> style
                .mode(ScrollerMode.HORIZONTAL)
                .horizontalScrollDisplay(ScrollDisplay.NEVER)
                .verticalScrollDisplay(ScrollDisplay.NEVER)
                .scrollerViewStyle(0));
        scroller.viewPort(view -> view
                .layout(layout -> layout.paddingAll(padding))
                .style(style -> style.backgroundTexture(IGuiTexture.EMPTY)));
        scroller.viewContainer(view -> view.layout(layout -> {
            layout.heightPercent(100);
            layout.flexDirection(FlexDirection.ROW);
            layout.gapAll(gap);
        }));
    }

    private List<QuestListEntry> getQuestListEntries() {
        List<QuestListEntry> entries = new ArrayList<>();
        for (QuestListGroup group : getQuestListGroups()) {
            for (TaskProgress task : group.tasks) {
                entries.add(new QuestListEntry(group.quest, task));
            }
        }
        return entries;
    }

    private List<QuestListGroup> getQuestListGroups() {
        List<QuestListGroup> groups = new ArrayList<>();
        for (PlayerQuestState quest : getFilteredQuests()) {
            if (!ClientConfig.SHOW_COMPLETED_QUESTS_IN_BOOK.get() && quest.status == QuestStatus.COMPLETED) {
                continue;
            }
            List<TaskProgress> visibleTasks = quest.taskProgresses.stream()
                    .filter(this::isVisibleTaskInBook)
                    .toList();
            if (!visibleTasks.isEmpty()) {
                groups.add(new QuestListGroup(quest, visibleTasks));
            }
        }
        return groups;
    }

    private List<PlayerQuestState> getFilteredQuests() {
        List<PlayerQuestState> filtered = new ArrayList<>();
        QuestCategoryData category = findCategory(selectedCategoryId);
        if (category == null) {
            return filtered;
        }
        for (PlayerQuestState quest : playerData.quests) {
            if (category.containsQuest(quest.questId) || selectedCategoryId.equals(quest.categoryId)) {
                filtered.add(quest);
            }
        }
        return filtered;
    }

    private boolean isVisibleTaskInBook(TaskProgress task) {
        if (task.status == TaskStatus.HIDDEN || task.status == TaskStatus.LOCKED) {
            return false;
        }
        if (task.status == TaskStatus.COMPLETED || task.status == TaskStatus.SKIPPED) {
            return ClientConfig.SHOW_COMPLETED_TASKS_IN_BOOK.get();
        }
        return true;
    }

    private TaskProgress getSelectedTask(PlayerQuestState quest) {
        if (quest == null || selectedStepId == null || selectedStepId.isBlank()) {
            return null;
        }
        return quest.taskProgresses.stream()
                .filter(task -> task.stepId.equals(selectedStepId))
                .findFirst()
                .orElse(null);
    }

    private List<RewardDisplay> rewardsForStep(PlayerQuestState quest, String stepId) {
        String normalizedStepId = stepId == null ? "" : stepId;
        return quest.rewardDisplays.stream()
                .filter(reward -> normalizedStepId.equals(reward.stepId))
                .toList();
    }

    private boolean isTrackedTask(PlayerQuestState quest, TaskProgress task) {
        return quest.questId.equals(playerData.trackedQuestId)
                && task.stepId.equals(playerData.trackedStepId);
    }

    private void toggleTrackedTask(PlayerQuestState quest, TaskProgress task) {
        boolean tracked = isTrackedTask(quest, task);
        playerData.trackedQuestId = tracked ? "" : quest.questId;
        playerData.trackedStepId = tracked ? "" : task.stepId;
        CompoundTag tag = new CompoundTag();
        tag.putString("trackedQuestId", playerData.trackedQuestId == null ? "" : playerData.trackedQuestId);
        tag.putString("trackedStepId", playerData.trackedStepId == null ? "" : playerData.trackedStepId);
        RPCPacketDistributor.rpcToServer(C2SPayload.SAVE_TRACKED_QUEST, tag);
        reloadQuestList();
        reloadDetail();
    }

    private void submitObjective(PlayerQuestState quest, TaskProgress task, int objectiveIndex) {
        CompoundTag tag = new CompoundTag();
        tag.putString("questId", quest.questId);
        tag.putString("stepId", task.stepId);
        tag.putInt("objectiveIndex", objectiveIndex);
        RPCPacketDistributor.rpcToServer(C2SPayload.SUBMIT_QUEST_TASK, tag);
    }

    private void viewPonderObjective(PlayerQuestState quest, TaskProgress task, int objectiveIndex,
                                     TaskObjectiveProgress objective) {
        if (objective == null || !PonderCompat.open(objective.ponderComponentId)) {
            return;
        }
        if (task.status == TaskStatus.ACTIVE && !objective.completed && !objective.isFailureCondition()) {
            objective.requiredAmount = Math.max(1, objective.requiredAmount);
            objective.currentAmount = objective.requiredAmount;
            objective.completed = true;
            if (task.areAllObjectivesCompleted()) {
                task.status = TaskStatus.COMPLETED;
            }
            reloadQuestList();
            reloadDetail();
            submitObjective(quest, task, objectiveIndex);
        }
    }

    private List<QuestCategoryData> pagedCategories() {
        int start = categoryPage * CATEGORIES_PER_PAGE;
        int end = Math.min(categoryData.categories.size(), start + CATEGORIES_PER_PAGE);
        return start >= end ? List.of() : categoryData.categories.subList(start, end);
    }

    private int maxCategoryPage() {
        int count = categoryData.categories.size();
        return count <= CATEGORIES_PER_PAGE ? 0 : (count - 1) / CATEGORIES_PER_PAGE;
    }

    private QuestCategoryData findCategory(String categoryId) {
        String normalized = QuestCategoryData.normalizeId(categoryId);
        return categoryData.categories.stream()
                .filter(category -> category.id.equals(normalized))
                .findFirst()
                .orElse(null);
    }

    private int categoryIndex(String categoryId) {
        String normalized = QuestCategoryData.normalizeId(categoryId);
        for (int i = 0; i < categoryData.categories.size(); i++) {
            if (categoryData.categories.get(i).id.equals(normalized)) {
                return i;
            }
        }
        return -1;
    }

    private String taskTitle(TaskProgress task) {
        return task.title == null || task.title.isBlank() ? task.stepId : task.title;
    }

    private Component taskHint(TaskProgress task) {
        Component hint = richText(task.displayTaskHint());
        if (hasTooltipText(hint)) {
            return hint;
        }
        return richText(taskTitle(task));
    }

    private Component objectiveTooltip(TaskObjectiveProgress objective) {
        if (objective == null) {
            return Component.empty();
        }
        Component hint = richText(objective.displayHint());
        if (objective.isRequired()) {
            return hint;
        }
        return hasTooltipText(hint)
                ? objective.objectiveTypeLabel().copy().append(" ").append(hint)
                : objective.objectiveTypeLabel();
    }

    private int objectiveTextColor(TaskObjectiveProgress objective) {
        if (objective.completed) {
            return TEXT_GREEN;
        }
        if (objective.isFailureCondition()) {
            return TEXT_RED;
        }
        return objective.isOptional() ? TEXT_MUTED : TEXT_DARK;
    }

    private UIElement createDisplayIcon(DisplayIcon displayIcon, Component tooltip) {
        if (displayIcon != null && displayIcon.isTexture()) {
            ResourceLocation location = ResourceLocation.tryParse(displayIcon.getTexture());
            UIElement texture = texturedPanel(location == null ? OBJECTIVES_ICON : SpriteTexture.of(location));
            applyOptionalTooltip(texture, tooltip);
            return texture;
        }
        ItemStack stack = displayIcon == null ? ItemStack.EMPTY : displayIcon.renderItemStack();
        ItemSlot slot = new ItemSlot();
        slot.setItem(stack == null || stack.isEmpty() ? DEFAULT_ICON : stack);
        slot.style(style -> style.backgroundTexture(IGuiTexture.EMPTY));
        slot.slotStyle(style -> style
                .slotOverlay(IGuiTexture.EMPTY)
                .hoverOverlay(IGuiTexture.EMPTY)
                .showItemTooltips(false));
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

    private boolean hasTooltipText(Component text) {
        return text != null && !text.getString().isBlank();
    }

    private Component richText(String text) {
        return RichTextUtil.parse(text == null ? "" : text);
    }

    private Component richText(Component text) {
        if (text == null) {
            return Component.empty();
        }
        String value = text.getString();
        return value.indexOf(RichTextUtil.AMPERSAND_FORMAT_PREFIX) >= 0 ? RichTextUtil.parse(value) : text;
    }

    private Label label(Component text, float fontSize, int color) {
        Label label = new Label();
        label.setText(text == null ? Component.empty() : text);
        label.textStyle(style -> style
                .textColor(color)
                .fontSize(fontSize)
                .textShadow(false));
        return label;
    }

    private Label centeredLabel(Component text, float fontSize, int color) {
        Label label = label(text, fontSize, color);
        label.layout(layout -> {
            layout.widthPercent(100);
            layout.heightPercent(100);
        });
        label.textStyle(style -> style
                .textAlignHorizontal(Horizontal.CENTER)
                .textAlignVertical(Vertical.CENTER)
                .textWrap(TextWrap.HIDE));
        return label;
    }

    private Label wrappedLabel(Component text, int color) {
        Label label = label(text, FONT_SMALL, color);
        label.layout(layout -> {
            layout.widthPercent(100);
            layout.minHeight(9);
        });
        label.textStyle(style -> style
                .textWrap(TextWrap.WRAP)
                .adaptiveHeight(true));
        return label;
    }

    private UIElement texturedPanel(IGuiTexture texture) {
        UIElement panel = new UIElement();
        panel.style(style -> style.backgroundTexture(texture));
        return panel;
    }

    private static IGuiTexture sprite(String path) {
        return SpriteTexture.of(ViScriptQuests.id("textures/gui/" + path));
    }

    private static IGuiTexture spriteRegion(String path, int x, int y, int width, int height) {
        return SpriteTexture.of(ViScriptQuests.id("textures/gui/" + path))
                .setSprite(x, y, width, height);
    }

    private static IGuiTexture borderedSprite(String path, int border) {
        return SpriteTexture.of(ViScriptQuests.id("textures/gui/" + path)).setBorder(border);
    }

    private static <T extends UIElement> T place(T element, float x, float y, float width, float height) {
        element.layout(layout -> {
            layout.positionType(TaffyPosition.ABSOLUTE);
            layout.left(x);
            layout.top(y);
            layout.width(width);
            layout.height(height);
        });
        return element;
    }

    private static int statusColor(TaskStatus status) {
        return switch (status) {
            case ACTIVE -> TEXT_DARK;
            case COMPLETED -> TEXT_GREEN;
            case FAILED -> TEXT_RED;
            case LOCKED, SKIPPED, HIDDEN -> TEXT_MUTED;
        };
    }

    private record QuestListGroup(PlayerQuestState quest, List<TaskProgress> tasks) {
    }

    private record QuestListEntry(PlayerQuestState quest, TaskProgress task) {
        String stepId() {
            return task.stepId;
        }

        Component title() {
            String value = task.title == null || task.title.isBlank() ? task.stepId : task.title;
            return RichTextUtil.parse(value);
        }

        Component statusName() {
            return task.status.displayName();
        }

        int statusColor() {
            return QuestBookUI.statusColor(task.status);
        }

    }

    @Override
    public void initScreen(int screenWidth, int screenHeight) {
        super.initScreen(screenWidth, screenHeight);
        applyViewportScale(screenWidth, screenHeight);
    }

    public static Size getViewportSize(Size screenSize) {
        return screenSize;
    }

    private void applyViewportScale(int screenWidth, int screenHeight) {
        float widthScale = screenWidth * MAX_SCREEN_WIDTH_RATIO / BOOK_WIDTH;
        float heightScale = screenHeight * MAX_SCREEN_HEIGHT_RATIO / BOOK_HEIGHT;
        float scale = Math.max(0.01f, Math.min(widthScale, heightScale));
        bookElement.transform(transform -> transform.pivot(0.5f, 0.5f).scale(scale));
    }
}
