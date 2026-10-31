package com.viscriptquests.gui;

import com.lowdragmc.lowdraglib2.gui.texture.ColorRectTexture;
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.texture.Icons;
import com.lowdragmc.lowdraglib2.gui.texture.SpriteTexture;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.data.*;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.gui.ui.elements.ItemSlot;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.gui.ui.elements.ScrollerView;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvent;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEventListener;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.lowdragmc.lowdraglib2.gui.ui.style.PropertyRegistry;
import com.lowdragmc.lowdraglib2.math.Size;
import com.lowdragmc.lowdraglib2.math.interpolate.Eases;
import com.lowdragmc.lowdraglib2.networking.rpc.RPCPacketDistributor;
import com.lowdragmc.lowdraglib2.syncdata.ISubscription;
import com.viscript_lib.util.RichTextUtil;
import com.viscript_lib.util.math.Clamp;
import com.viscriptquests.ViScriptQuests;
import com.viscriptquests.compat.ponder.PonderCompat;
import com.viscriptquests.config.ClientConfig;
import com.viscriptquests.gui.theme.QuestTheme;
import com.viscriptquests.network.c2s.C2SPayload;
import com.viscriptquests.quest.data.DisplayIcon;
import com.viscriptquests.quest.data.runtime.*;
import dev.vfyjxf.taffy.style.*;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.*;

/**
 * 玩家任务书。书本主题使用固定 480×380 坐标系，深色主题使用全屏分类导航与双栏布局；
 * 两种主题的任务与进度操作都由同一套运行时数据驱动。
 */
public class QuestBookUI extends UIElement {
    private static final int BOOK_WIDTH = 480;
    private static final int BOOK_HEIGHT = 380;
    private static final float MAX_SCREEN_WIDTH_RATIO = 0.80f;
    private static final float MAX_SCREEN_HEIGHT_RATIO = 0.90f;

    private static final int CATEGORY_X = 13;
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
    private static final int TEXT_BLACK = 0xFF000000;
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
    private static final IGuiTexture STATUS_BOOKMARK = borderedSprite("tag/status_bookmark.png", 3);
    private static final IGuiTexture STATUS_BOOKMARK_SELECTED = borderedSprite("tag/status_bookmark_selected.png", 3);

    private QuestPlayerData playerData;
    private QuestCategoryListData categoryData;
    private String selectedCategoryId = "";
    private PlayerQuestState selectedQuest;
    private String selectedStepId = "";
    private int categoryPage;
    private final Set<String> collapsedQuestIds = new HashSet<>();
    private final Set<String> collapsedCategoryIds = new HashSet<>();
    private final Map<UIElement, ItemStack> recipeLookupItems = new IdentityHashMap<>();
    private final QuestTheme theme;
    private QuestStatusFilter statusFilter = QuestStatusFilter.ACTIVE;

    private UIElement categoryListPanel;
    private Button categoryPager;
    private Button previousCategoryPage;
    private Label categoryPageLabel;
    private ScrollerView categoryScroller;
    private Button allCategoriesButton;
    private UIElement statusFilterPanel;
    private Button activeFilterButton;
    private Button completedFilterButton;
    private UIElement statusSelectionUnderline;
    private ISubscription statusUnderlineTransition;
    private boolean statusUnderlineInitialized;
    private UIElement categorySelectionUnderline;
    private ISubscription categoryUnderlineTransition;
    private boolean categoryUnderlineInitialized;
    private ScrollerView questListView;
    private Label questListEmptyLabel;
    private UIElement detailPanel;
    private UIElement bookElement;
    private ISubscription detailTransition;
    private ISubscription selectedHighlightTransition;

    public QuestBookUI(QuestBookData bookData) {
        this.playerData = bookData == null || bookData.playerData == null
                ? new QuestPlayerData()
                : bookData.playerData;
        this.categoryData = bookData == null || bookData.categoryData == null
                ? new QuestCategoryListData()
                : bookData.categoryData;
        this.theme = QuestTheme.current();
        selectedCategoryId = QuestCategoryData.normalizeId(playerData.lastViewedCategoryId);
        categoryPage = Math.max(0, categoryIndex(selectedCategoryId)) / CATEGORIES_PER_PAGE;
        buildUI();
    }

    public void syncBookData(QuestBookData bookData) {
        categoryData = bookData == null || bookData.categoryData == null
                ? new QuestCategoryListData()
                : bookData.categoryData;
        syncPlayerData(bookData == null ? null : bookData.playerData);
    }

    /**
     * 更新任务进度与状态，同时保留当前浏览的分类、任务和分类分页。
     *
     * @param data 服务端同步的玩家任务数据；为 <code>null</code> 时清空任务展示
     */
    public void syncPlayerData(QuestPlayerData data) {
        String selectedQuestId = selectedQuest == null ? "" : selectedQuest.questId;
        playerData = data == null ? new QuestPlayerData() : data;
        selectedQuest = playerData.findQuest(selectedQuestId).orElse(null);
        ensureSelectedCategory();
        ensureSelectedEntry();
        reloadCategoryTabs();
        reloadQuestList();
        reloadDetail();
    }

    /**
     * 获取命中元素所属目标行的真实物品，按钮和非物品目标不提供查询物品。
     *
     * @param hoveredElement 经过界面命中检测的鼠标悬停元素
     * @return 可查询的目标物品；未命中目标时返回空物品栈
     */
    public ItemStack getRecipeLookupItem(UIElement hoveredElement) {
        for (var element = hoveredElement; element != null; element = element.getParent()) {
            if (element instanceof Button) return ItemStack.EMPTY;
            var item = recipeLookupItems.get(element);
            if (item != null) return item;
        }
        return ItemStack.EMPTY;
    }

    private void buildUI() {
        layout(layout -> {
            layout.widthPercent(100);
            layout.heightPercent(100);
            layout.alignItems(AlignItems.CENTER);
            layout.justifyContent(AlignContent.CENTER);
        });
        style(style -> style.backgroundTexture(new ColorRectTexture(theme.rootOverlay())));

        if (theme.isClassicBook()) {
            bookElement = texturedPanel(BOOK_BACKGROUND);
            bookElement.setId("quest_book_panel");
            bookElement.layout(layout -> {
                layout.width(BOOK_WIDTH);
                layout.height(BOOK_HEIGHT);
            });
            addChild(bookElement);

            buildCategoryTabs(bookElement);
            buildQuestList(bookElement);
            buildDetailPanel(bookElement);
            buildStatusFilters(bookElement, true);
        } else {
            buildGlassDarkUI();
        }
        // 初次构建直接显示首个任务，动画只用于用户主动切换分类、状态或任务。
        refreshAll(false);
    }

    /** 构建全屏深色任务书及顶部分类导航。 */
    private void buildGlassDarkUI() {
        bookElement = texturedPanel(theme.shellBackground());
        bookElement.setId("quest_book_panel_glass_dark");
        bookElement.layout(layout -> layout.widthPercent(100).heightPercent(100)
                .flexDirection(FlexDirection.COLUMN).paddingAll(12).gapAll(7));
        addChild(bookElement);

        UIElement header = new UIElement();
        header.setId("quest_book_header");
        header.layout(layout -> layout.widthPercent(100).height(26)
                .flexDirection(FlexDirection.ROW).alignItems(AlignItems.CENTER).gapAll(6));
        UIElement bookIcon = createDisplayIcon(DisplayIcon.item("minecraft:book"), Component.empty());
        bookIcon.setId("quest_book_icon");
        bookIcon.layout(layout -> layout.width(22).height(22));
        Label title = label(Component.translatable("viscript_quests.quest_book.title"), 13, theme.primaryText());
        title.setId("quest_book_title");
        title.layout(layout -> layout.width(51).height(20));
        title.textStyle(style -> style.textWrap(TextWrap.HIDE).textAlignVertical(Vertical.CENTER));
        header.addChildren(bookIcon, title);
        buildStatusFilters(header, false);

        UIElement categoryBar = new UIElement();
        categoryBar.setId("quest_book_category_tabs");
        categoryBar.layout(layout -> layout.width(0).flex(1).heightPercent(100)
                .flexDirection(FlexDirection.ROW).alignItems(AlignItems.CENTER)
                .paddingLeft(12).gapAll(3));
        allCategoriesButton = new Button().noText();
        allCategoriesButton.setId("quest_book_category_all");
        allCategoriesButton.layout(layout -> layout.width(26).height(26)
                .alignItems(AlignItems.CENTER).justifyContent(AlignContent.CENTER));
        allCategoriesButton.style(style -> style.tooltips(Component.translatable("viscript_quests.quest_book.all_categories")));
        UIElement allIcon = createAllCategoryIcon();
        allIcon.layout(layout -> layout.width(20).height(20));
        allIcon.setAllowHitTest(false);
        allCategoriesButton.addChild(allIcon);
        allCategoriesButton.setOnClick(event -> selectCategory(""));
        categoryBar.addChild(allCategoriesButton);
        categoryScroller = new QuestCategoryScrollerView();
        categoryScroller.setId("quest_book_category_scroll");
        categoryScroller.layout(layout -> layout.width(0).flex(1).heightPercent(100));
        configureHorizontalScroller(categoryScroller, 0, 3);
        categoryListPanel = categoryScroller.viewContainer;
        categoryBar.addChild(categoryScroller);
        header.addChild(categoryBar);

        Button close = actionButton(Component.literal("×"));
        close.setId("quest_book_close");
        close.layout(layout -> layout.width(20).height(20));
        close.textStyle(style -> style.fontSize(14));
        close.style(style -> style.tooltips(Component.translatable("gui.close")));
        styleGlassNavigationTab(close, false);
        close.setOnClick(event -> {
            var screen = Minecraft.getInstance().screen;
            if (screen != null) screen.onClose();
        });
        header.addChild(close);
        bookElement.addChild(header);
        UIElement headerDivider = texturedPanel(theme.divider());
        headerDivider.layout(layout -> layout.widthPercent(100).height(1));
        headerDivider.setAllowHitTest(false);
        bookElement.addChild(headerDivider);

        UIElement body = new UIElement();
        body.layout(layout -> layout.widthPercent(100).height(0).flex(1)
                .flexDirection(FlexDirection.ROW).gapAll(12));
        questListView = new ScrollerView();
        questListView.setId("quest_book_list");
        questListView.layout(layout -> layout.widthPercent(28).heightPercent(100));
        // 层级本身已经提供视觉分隔，滚动内容保持贴合，让分类、大任务和小任务连成一棵树。
        configureVerticalScroller(questListView, 0, 0);
        questListView.style(style -> style.backgroundTexture(theme.panelBackground()));
        questListEmptyLabel = centeredLabel(Component.translatable("viscript_quests.quest_book.empty_filter"),
                8, theme.mutedText());
        questListEmptyLabel.setId("quest_book_list_empty");
        questListEmptyLabel.layout(layout -> layout.positionType(TaffyPosition.ABSOLUTE)
                .left(0).top(0).widthPercent(100).heightPercent(100).paddingAll(12));
        questListEmptyLabel.textStyle(style -> style.textWrap(TextWrap.WRAP));
        questListEmptyLabel.setAllowHitTest(false);
        questListEmptyLabel.setDisplay(false);
        questListView.viewPort.addChild(questListEmptyLabel);

        detailPanel = new UIElement();
        detailPanel.setId("quest_book_detail");
        detailPanel.style(style -> style.backgroundTexture(theme.panelBackground()));
        detailPanel.layout(layout -> layout.width(0).flex(1).heightPercent(100)
                .flexDirection(FlexDirection.COLUMN).paddingAll(10).gapAll(5));
        body.addChildren(questListView, detailPanel);
        bookElement.addChild(body);
    }

    private void nextCategoryPage() {
        int maxPage = maxCategoryPage();
        if (maxPage <= 0) {
            return;
        }
        categoryPage = categoryPage >= maxPage ? 0 : categoryPage + 1;
        reloadCategoryTabs();
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
        categoryPager.setId("quest_book_category_pager");
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
            nextCategoryPage();
        });
        book.addChild(categoryPager);
    }

    private void buildStatusFilters(UIElement parent, boolean classic) {
        statusFilterPanel = new UIElement();
        statusFilterPanel.setId("quest_book_status_filters");
        statusFilterPanel.layout(layout -> {
            if (classic) {
                layout.positionType(TaffyPosition.ABSOLUTE);
                layout.right(36);
                layout.top(BOOK_HEIGHT - 20);
                layout.width(76);
                layout.height(28);
            } else {
                // 两个 56px 页签加上间距需要完整的横向空间；文字在 26px 行内居中。
                layout.width(116);
                layout.height(26);
            }
            layout.flexDirection(FlexDirection.ROW);
            layout.gapAll(4);
        });
        activeFilterButton = createStatusFilterButton("viscript_quests.quest_book.status.active");
        activeFilterButton.setId("quest_book_status_active");
        completedFilterButton = createStatusFilterButton("viscript_quests.quest_book.status.completed");
        completedFilterButton.setId("quest_book_status_completed");
        statusFilterPanel.addChildren(activeFilterButton, completedFilterButton);
        if (!classic) {
            statusSelectionUnderline = texturedPanel(new ColorRectTexture(theme.accentText()));
            statusSelectionUnderline.setId("quest_book_status_underline");
            statusSelectionUnderline.layout(layout -> layout.positionType(TaffyPosition.ABSOLUTE)
                    .left(0).bottom(0).width(56).height(1.5f));
            statusSelectionUnderline.setAllowHitTest(false);
            statusFilterPanel.addChild(statusSelectionUnderline);
        }
        parent.addChild(statusFilterPanel);
        updateStatusFilterButtons();
    }

    private Button createStatusFilterButton(String translationKey) {
        Button button = new Button();
        button.setText(Component.translatable(translationKey));
        button.style(style -> style.tooltips(Component.translatable(translationKey)));
        button.layout(layout -> {
            layout.width(theme.isClassicBook() ? 36 : 56);
            layout.height(theme.isClassicBook() ? 24 : 26);
            if (theme.isClassicBook()) {
                layout.paddingHorizontal(2);
                layout.paddingTop(2);
                layout.paddingBottom(7);
            }
        });
        button.setOnClick(event -> {
            QuestStatusFilter next = button == activeFilterButton
                    ? QuestStatusFilter.ACTIVE : QuestStatusFilter.COMPLETED;
            if (statusFilter != next) {
                statusFilter = next;
                selectedQuest = null;
                selectedStepId = "";
                refreshAll();
            }
        });
        return button;
    }

    private void updateStatusFilterButtons() {
        if (activeFilterButton == null || completedFilterButton == null) {
            return;
        }
        updateStatusFilterButton(activeFilterButton, statusFilter == QuestStatusFilter.ACTIVE);
        updateStatusFilterButton(completedFilterButton, statusFilter == QuestStatusFilter.COMPLETED);
    }

    private void updateStatusFilterButton(Button button, boolean selected) {
        if (!theme.isClassicBook()) {
            styleGlassNavigationTab(button, selected);
            button.selectId(button.getId() + "_underline").findFirst()
                    .ifPresent(underline -> underline.style(style -> style.opacity(0f)));
            button.textStyle(style -> style.fontSize(8).textShadow(false)
                    .textAlignHorizontal(Horizontal.CENTER).textAlignVertical(Vertical.CENTER).textWrap(TextWrap.HIDE));
            if (selected) {
                moveStatusSelectionUnderline(button);
            }
            return;
        }
        IGuiTexture selectedTexture = STATUS_BOOKMARK_SELECTED;
        if (selected) button.addClass("selected");
        else button.removeClass("selected");
        button.layout(layout -> layout.height(selected ? 28 : 24));
        button.buttonStyle(style -> style
                .baseTexture(selected ? selectedTexture : STATUS_BOOKMARK)
                .hoverTexture(selectedTexture)
                .pressedTexture(STATUS_BOOKMARK_SELECTED));
        button.textStyle(style -> style
                .textColor(selected ? TEXT_SELECTED : 0xFFF2DBB2)
                .fontSize(FONT_SMALL)
                .textShadow(false)
                .textAlignHorizontal(Horizontal.CENTER)
                .textAlignVertical(Vertical.CENTER)
                .textWrap(TextWrap.WRAP));
    }

    private void moveStatusSelectionUnderline(Button selectedButton) {
        if (statusSelectionUnderline == null || statusFilterPanel == null) {
            return;
        }
        float targetX = selectedButton == completedFilterButton ? 60f : 0f;
        float currentX = computedTransformX(statusSelectionUnderline, statusFilterPanel.getSizeWidth());
        if (Float.isNaN(currentX)) {
            currentX = targetX;
        }
        if (!statusUnderlineInitialized) {
            statusSelectionUnderline.getStyle().transform2D(Transform2D.identity().translate(targetX, 0));
            statusUnderlineInitialized = true;
            return;
        }
        if (statusUnderlineTransition != null) {
            statusUnderlineTransition.unsubscribe();
        }
        statusSelectionUnderline.getStyle().transform2D(Transform2D.identity().translate(currentX, 0));
        if (!ClientConfig.ENABLE_QUEST_UI_ANIMATIONS.get()) {
            statusSelectionUnderline.getStyle().transform2D(Transform2D.identity().translate(targetX, 0));
            return;
        }
        statusUnderlineTransition = statusSelectionUnderline.animation()
                .duration(0.20f)
                .ease(Eases.QUART_OUT)
                .style(PropertyRegistry.TRANSFORM_2D, Transform2D.identity().translate(targetX, 0))
                .start();
    }

    private void moveCategorySelectionUnderline(String categoryId) {
        if (categorySelectionUnderline == null) {
            return;
        }
        if (categoryId == null || categoryId.isEmpty()) {
            if (categoryUnderlineTransition != null) {
                categoryUnderlineTransition.unsubscribe();
                categoryUnderlineTransition = null;
            }
            categorySelectionUnderline.setVisible(false);
            return;
        }
        int index = categoryIndex(categoryId);
        if (index < 0) {
            categorySelectionUnderline.setVisible(false);
            return;
        }
        categorySelectionUnderline.setVisible(true);
        float targetX = index * 29f;
        float currentX = computedTransformX(categorySelectionUnderline, categoryListPanel.getSizeWidth());
        if (Float.isNaN(currentX)) {
            currentX = targetX;
        }
        if (!categoryUnderlineInitialized) {
            categorySelectionUnderline.getStyle().transform2D(Transform2D.identity().translate(targetX, 0));
            categoryUnderlineInitialized = true;
            return;
        }
        if (categoryUnderlineTransition != null) {
            categoryUnderlineTransition.unsubscribe();
        }
        categorySelectionUnderline.getStyle().transform2D(Transform2D.identity().translate(currentX, 0));
        if (!ClientConfig.ENABLE_QUEST_UI_ANIMATIONS.get()) {
            categorySelectionUnderline.getStyle().transform2D(Transform2D.identity().translate(targetX, 0));
            return;
        }
        categoryUnderlineTransition = categorySelectionUnderline.animation()
                .duration(0.20f)
                .ease(Eases.QUART_OUT)
                .style(PropertyRegistry.TRANSFORM_2D, Transform2D.identity().translate(targetX, 0))
                .start();
    }

    private float computedTransformX(UIElement element, float percentBase) {
        Transform2D transform = element.getStyleBag().getComputed(PropertyRegistry.TRANSFORM_2D);
        if (transform == null) {
            return Float.NaN;
        }
        var translateX = transform.translate().getX();
        return translateX.isPercent() ? translateX.resolve(percentBase) : translateX.getValue();
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
        refreshAll(true);
    }

    private void refreshAll(boolean animateDetail) {
        ensureSelectedCategory();
        ensureSelectedEntry();
        reloadCategoryTabs();
        reloadQuestList();
        if (animateDetail) {
            animateDetailTransition();
        } else {
            reloadDetail();
        }
        updateStatusFilterButtons();
    }

    private void ensureSelectedCategory() {
        categoryPage = Clamp.clamp(categoryPage, 0, maxCategoryPage());
        if (!theme.isClassicBook()) {
            if (findCategory(selectedCategoryId) == null) selectedCategoryId = "";
            return;
        }
        if (categoryData.categories.isEmpty()) {
            selectedCategoryId = "";
            return;
        }
        if (findCategory(selectedCategoryId) == null) {
            selectedCategoryId = categoryData.categories.get(0).id;
            categoryPage = 0;
        }
    }

    private void ensureSelectedEntry() {
        if (!theme.isClassicBook()) {
            List<QuestListEntry> entries = getQuestListEntries();
            QuestListEntry entry = entries.stream()
                    .filter(candidate -> selectedQuest != null
                            && candidate.quest.questId.equals(selectedQuest.questId)
                            && candidate.stepId().equals(selectedStepId))
                    .findFirst().orElseGet(() -> entries.stream()
                            .filter(candidate -> isTrackedTask(candidate.quest, candidate.task))
                            .findFirst().orElse(entries.isEmpty() ? null : entries.get(0)));
            selectedQuest = entry == null ? null : entry.quest;
            selectedStepId = entry == null ? "" : entry.stepId();
            return;
        }
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
        float previousSelectedY = selectedQuestRowY();
        selectedCategoryId = QuestCategoryData.normalizeId(categoryId);
        int index = categoryIndex(selectedCategoryId);
        if (index >= 0) categoryPage = index / CATEGORIES_PER_PAGE;
        selectedQuest = null;
        selectedStepId = "";
        refreshAll();
        animateSelectedQuestFrom(previousSelectedY);
        if (!selectedCategoryId.equals(playerData.lastViewedCategoryId)) {
            playerData.lastViewedCategoryId = selectedCategoryId;
            RPCPacketDistributor.rpcToServer(C2SPayload.SAVE_QUEST_BOOK_CATEGORY, selectedCategoryId);
        }
    }

    private void selectEntry(QuestListEntry entry) {
        float previousSelectedY = selectedQuestRowY();
        selectedQuest = entry.quest;
        selectedStepId = entry.stepId();
        reloadQuestList();
        animateDetailTransition();
        animateSelectedQuestFrom(previousSelectedY);
    }

    private void toggleQuestCollapsed(PlayerQuestState quest) {
        if (!collapsedQuestIds.add(quest.questId)) {
            collapsedQuestIds.remove(quest.questId);
        }
        reloadQuestList();
    }

    private void reloadCategoryTabs() {
        if (!theme.isClassicBook()) {
            reloadGlassCategoryTabs();
            return;
        }
        categoryPage = Clamp.clamp(categoryPage, 0, maxCategoryPage());
        categoryListPanel.clearAllChildren();
        for (QuestCategoryData category : pagedCategories()) {
            boolean selected = category.id.equals(selectedCategoryId);
            Button tab = new Button().noText();
            tab.setId("quest_book_category_" + categoryIndex(category.id));
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

    private void reloadGlassCategoryTabs() {
        float previousUnderlineX = categorySelectionUnderline == null ? Float.NaN
                : computedTransformX(categorySelectionUnderline, categoryListPanel.getSizeWidth());
        categoryListPanel.clearAllChildren();
        styleGlassNavigationTab(allCategoriesButton, selectedCategoryId.isEmpty());
        if (categoryUnderlineTransition != null) {
            categoryUnderlineTransition.unsubscribe();
            categoryUnderlineTransition = null;
        }
        for (QuestCategoryData category : categoryData.categories) {
            Button tab = new Button().noText();
            tab.setId("quest_book_category_" + categoryIndex(category.id));
            styleGlassNavigationTab(tab, category.id.equals(selectedCategoryId));
            tab.selectId(tab.getId() + "_underline").findFirst()
                    .ifPresent(underline -> underline.style(style -> style.opacity(0f)));
            tab.layout(layout -> layout.width(26).height(26)
                    .alignItems(AlignItems.CENTER).justifyContent(AlignContent.CENTER));
            String categoryTitle = category.title == null || category.title.isBlank() ? category.id : category.title;
            tab.style(style -> style.tooltips(richText(categoryTitle)));
            tab.setOnClick(event -> selectCategory(category.id));
            UIElement icon = createDisplayIcon(category.displayIcon, Component.empty());
            icon.layout(layout -> layout.width(18).height(18));
            icon.setAllowHitTest(false);
            tab.addChild(icon);
            categoryListPanel.addChild(tab);
        }
        categorySelectionUnderline = texturedPanel(new ColorRectTexture(theme.accentText()));
        categorySelectionUnderline.setId("quest_book_category_underline");
        categorySelectionUnderline.layout(layout -> layout.positionType(TaffyPosition.ABSOLUTE)
                .left(0).bottom(0).width(26).height(1.5f));
        categorySelectionUnderline.setAllowHitTest(false);
        categoryListPanel.addChild(categorySelectionUnderline);
        if (Float.isNaN(previousUnderlineX)) {
            categoryUnderlineInitialized = false;
        } else {
            categorySelectionUnderline.getStyle().transform2D(
                    Transform2D.identity().translate(previousUnderlineX, 0));
            categoryUnderlineInitialized = true;
        }
        moveCategorySelectionUnderline(selectedCategoryId);
    }

    private void styleGlassNavigationTab(Button tab, boolean selected) {
        if (selected) tab.addClass("selected");
        else tab.removeClass("selected");
        tab.buttonStyle(style -> style.baseTexture(IGuiTexture.EMPTY)
                .hoverTexture(IGuiTexture.EMPTY).pressedTexture(IGuiTexture.EMPTY));
        tab.textStyle(style -> style.textColor(selected ? theme.accentText() : theme.secondaryText()).textShadow(false));
        String underlineId = tab.getId() + "_underline";
        UIElement underline = tab.selectId(underlineId).findFirst().orElseGet(() -> {
            UIElement line = texturedPanel(new ColorRectTexture(theme.accentText()));
            line.setId(underlineId);
            line.layout(layout -> layout.positionType(TaffyPosition.ABSOLUTE).left(0).right(0).bottom(0).height(1.5f));
            line.setAllowHitTest(false);
            tab.addChild(line);
            return line;
        });
        underline.setVisible(selected);
    }

    /** 创建深色主题分类和任务展开使用的紧凑箭头。 */
    private UIElement glassCollapseArrow(boolean collapsed, int color) {
        UIElement arrow = texturedPanel((collapsed ? Icons.RIGHT_ARROW_NO_BAR : Icons.DOWN_ARROW_NO_BAR)
                .copy().setColor(color));
        arrow.layout(layout -> layout.width(14).height(14));
        arrow.setAllowHitTest(false);
        return arrow;
    }

    private void reloadGlassQuestList() {
        List<QuestListGroup> groups = getQuestListGroups();
        if (!selectedCategoryId.isEmpty()) {
            questListEmptyLabel.setDisplay(groups.isEmpty());
            for (QuestListGroup group : groups) addGlassQuestGroup(questListView.viewContainer, group);
            return;
        }
        Map<String, List<QuestListGroup>> categorized = new LinkedHashMap<>();
        for (QuestCategoryData category : categoryData.categories) categorized.put(category.id, new ArrayList<>());
        categorized.put("", new ArrayList<>());
        for (QuestListGroup group : groups) {
            QuestCategoryData category = categoryForQuest(group.quest);
            categorized.get(category == null ? "" : category.id).add(group);
        }
        boolean hasVisibleCategory = false;
        for (var entry : categorized.entrySet()) {
            QuestCategoryData category = findCategory(entry.getKey());
            if (entry.getValue().isEmpty() && (category == null || !category.showWhenEmpty)) continue;
            hasVisibleCategory = true;
            Button heading = new Button().noText();
            heading.setId("quest_book_category_group_" + (category == null ? "uncategorized" : categoryIndex(category.id)));
            heading.addClass("quest-category-group");
            heading.buttonStyle(style -> style.baseTexture(theme.headerBackground())
                    .hoverTexture(theme.cardBackground()).pressedTexture(theme.buttonPressed()));
            heading.layout(layout -> layout.widthPercent(100).height(22)
                    .flexDirection(FlexDirection.ROW).alignItems(AlignItems.CENTER).paddingHorizontal(5).gapAll(3));
            boolean collapsed = collapsedCategoryIds.contains(entry.getKey());
            if (collapsed) heading.addClass("collapsed");
            UIElement arrow = glassCollapseArrow(collapsed, theme.accentText());
            UIElement icon = createDisplayIcon(category == null ? null : category.displayIcon, Component.empty());
            icon.layout(layout -> layout.width(13).height(13));
            Component categoryTitle = category == null
                    ? Component.translatable("viscript_quests.quest_book.uncategorized")
                    : richText(category.title == null || category.title.isBlank() ? category.id : category.title);
            Label title = label(categoryTitle, 8, theme.accentText());
            title.layout(layout -> layout.width(0).flex(1).height(16));
            title.textStyle(style -> style.textWrap(TextWrap.HIDE).textAlignVertical(Vertical.CENTER));
            title.setAllowHitTest(false);
            heading.addChildren(arrow, icon, title);
            heading.setOnClick(event -> {
                if (!collapsedCategoryIds.add(entry.getKey())) collapsedCategoryIds.remove(entry.getKey());
                reloadQuestList();
            });
            questListView.addScrollViewChild(heading);
            if (!collapsed && !entry.getValue().isEmpty()) {
                UIElement contents = new UIElement();
                contents.layout(layout -> layout.widthPercent(100).paddingLeft(2)
                        .flexDirection(FlexDirection.COLUMN).gapAll(0));
                for (QuestListGroup group : entry.getValue()) addGlassQuestGroup(contents, group);
                questListView.addScrollViewChild(contents);
            }
        }
        questListEmptyLabel.setDisplay(!hasVisibleCategory);
    }

    private void addGlassQuestGroup(UIElement parent, QuestListGroup group) {
        parent.addChild(createGlassQuestSummary(group.quest));
        if (collapsedQuestIds.contains(group.quest.questId)) return;
        UIElement steps = new UIElement();
        steps.layout(layout -> layout.widthPercent(100).paddingLeft(4)
                .flexDirection(FlexDirection.COLUMN).gapAll(0));
        for (TaskProgress task : group.tasks) steps.addChild(createGlassSubTaskEntry(new QuestListEntry(group.quest, task)));
        parent.addChild(steps);
    }

    private QuestCategoryData categoryForQuest(PlayerQuestState quest) {
        return categoryData.categories.stream().filter(category -> category.containsQuest(quest.questId))
                .findFirst().orElseGet(() -> findCategory(quest.categoryId));
    }

    private void reloadQuestList() {
        questListView.clearAllScrollViewChildren();
        if (!theme.isClassicBook()) {
            reloadGlassQuestList();
            return;
        }
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
            if (theme.isClassicBook()) {
                layout.width(QUEST_LIST_WIDTH);
            } else {
                layout.widthPercent(100);
            }
            layout.height(QUEST_GROUP_GAP);
        });
        spacer.setAllowHitTest(false);
        return spacer;
    }

    private UIElement createQuestSummary(PlayerQuestState quest) {
        if (!theme.isClassicBook()) {
            return createGlassQuestSummary(quest);
        }
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

    private UIElement createGlassQuestSummary(PlayerQuestState quest) {
        Button row = new Button().noText();
        row.setId("quest_group_" + quest.questId);
        row.addClass("quest-group");
        boolean collapsed = collapsedQuestIds.contains(quest.questId);
        if (collapsed) row.addClass("collapsed");
        row.buttonStyle(style -> style.baseTexture(theme.cardBackground())
                .hoverTexture(theme.selectedCardBackground()).pressedTexture(theme.buttonPressed()));
        row.layout(layout -> layout.widthPercent(100).height(26)
                .flexDirection(FlexDirection.ROW).alignItems(AlignItems.CENTER).paddingHorizontal(5).gapAll(3));
        row.setOnClick(event -> toggleQuestCollapsed(quest));
        UIElement arrow = glassCollapseArrow(collapsed, theme.secondaryText());
        row.addChild(arrow);

        UIElement textColumn = new UIElement();
        textColumn.layout(layout -> layout.width(0).flex(1).height(24)
                .flexDirection(FlexDirection.COLUMN)
                .justifyContent(AlignContent.CENTER));
        textColumn.setAllowHitTest(false);
        String questTitle = quest.title == null || quest.title.isBlank() ? quest.questId : quest.title;
        Label title = label(richText(questTitle), 8, theme.primaryText());
        title.layout(layout -> layout.widthPercent(100).height(13));
        title.textStyle(style -> style.textWrap(TextWrap.HIDE)
                .textAlignVertical(Vertical.CENTER));
        title.setAllowHitTest(false);
        Label subtitle = label(richText(quest.subtitle), 7, theme.mutedText());
        subtitle.layout(layout -> layout.widthPercent(100).height(10));
        subtitle.textStyle(style -> style.textWrap(TextWrap.HIDE)
                .textAlignVertical(Vertical.CENTER));
        subtitle.setAllowHitTest(false);
        textColumn.addChildren(title, subtitle);
        row.addChild(textColumn);
        row.style(style -> style.tooltips(richText(questTitle)));
        return row;
    }

    private UIElement createSubTaskEntry(QuestListEntry entry) {
        if (!theme.isClassicBook()) {
            return createGlassSubTaskEntry(entry);
        }
        boolean selected = selectedQuest != null
                && selectedQuest.questId.equals(entry.quest.questId)
                && selectedStepId.equals(entry.stepId());
        UIElement row = texturedPanel(SUB_TASK_BACKGROUND);
        row.setId("quest_step_" + entry.stepId());
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

        Label status = label(entry.statusName(), FONT_SMALL, TEXT_BLACK);
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

    private UIElement createGlassSubTaskEntry(QuestListEntry entry) {
        boolean selected = selectedQuest != null && selectedQuest.questId.equals(entry.quest.questId)
                && selectedStepId.equals(entry.stepId());
        Button row = new Button().noText();
        row.setId("quest_step_" + entry.stepId());
        row.addClass("quest-step");
        if (selected) row.addClass("selected");
        row.buttonStyle(style -> style.baseTexture(IGuiTexture.EMPTY)
                .hoverTexture(selected ? theme.glassSelectedTask() : theme.cardBackground())
                .pressedTexture(theme.selectedCardBackground()));
        row.layout(layout -> layout.widthPercent(100).height(27)
                .flexDirection(FlexDirection.ROW).alignItems(AlignItems.CENTER).paddingHorizontal(7).gapAll(4));
        row.setOnClick(event -> selectEntry(entry));
        row.style(style -> style.tooltips(entry.title()));
        if (selected) {
            UIElement selection = texturedPanel(theme.glassSelectedTask());
            selection.setId("quest_step_" + entry.stepId() + "_selection");
            selection.layout(layout -> layout.positionType(TaffyPosition.ABSOLUTE)
                    .left(0).top(0).widthPercent(100).heightPercent(100));
            selection.setAllowHitTest(false);
            UIElement accent = texturedPanel(new ColorRectTexture(theme.accentText()));
            accent.layout(layout -> layout.positionType(TaffyPosition.ABSOLUTE)
                    .left(0).top(0).width(2).heightPercent(100));
            accent.setAllowHitTest(false);
            selection.addChild(accent);
            row.addChild(selection);
        }
        Label title = label(entry.title(), 8, selected ? 0xFF28313C : theme.primaryText());
        title.layout(layout -> layout.width(0).flex(1).height(18));
        title.textStyle(style -> style.textAlignVertical(Vertical.CENTER).textWrap(TextWrap.HIDE));
        title.setAllowHitTest(false);
        boolean tracked = isTrackedTask(entry.quest, entry.task);
        Component statusText = entry.task.status == TaskStatus.ACTIVE
                ? Component.translatable("viscript_quests.quest_book.status.active") : entry.statusName();
        Label status = label(tracked ? Component.translatable("viscript_quests.quest_book.tracked") : statusText,
                7, selected ? 0xFF705D38 : tracked ? theme.accentText() : theme.mutedText());
        status.layout(layout -> layout.width(43).height(16));
        status.textStyle(style -> style.textAlignHorizontal(Horizontal.RIGHT)
                .textAlignVertical(Vertical.CENTER).textWrap(TextWrap.HIDE));
        status.setAllowHitTest(false);
        row.addChildren(title, status);
        return row;
    }

    private void reloadDetail() {
        reloadDetail(false);
    }

    /**
     * 在任务内容切换时先淡出旧内容，再重建详情并淡入新内容。详情面板本身不参与布局动画，
     * 因此快速连续点击任务时只会保留最后一次切换的内容。
     */
    private void animateDetailTransition() {
        if (detailPanel == null) {
            return;
        }
        if (detailTransition != null) {
            detailTransition.unsubscribe();
            detailTransition = null;
        }
        if (theme.isClassicBook() || !ClientConfig.ENABLE_QUEST_UI_ANIMATIONS.get()) {
            reloadDetail();
            detailPanel.style(style -> style.opacity(1f));
            return;
        }
        detailTransition = detailPanel.animation()
                .duration(0.08f)
                .ease(Eases.QUAD_IN)
                .style(PropertyRegistry.OPACITY, 0f)
                .onFinished(element -> {
                    reloadDetail();
                    animateIn(element, 0f);
                })
                .start();
    }

    private void reloadDetail(boolean animate) {
        recipeLookupItems.clear();
        detailPanel.clearAllChildren();
        TaskProgress task = getSelectedTask(selectedQuest);
        if (selectedQuest == null || task == null) {
            if (!theme.isClassicBook()) {
                Label empty = centeredLabel(Component.translatable("viscript_quests.quest_book.no_quest_selected"),
                        9, theme.mutedText());
                empty.setId("quest_book_detail_empty");
                detailPanel.addChild(empty);
            }
            return;
        }
        addTaskDetail(selectedQuest, task);
        if (animate) {
            animateIn(detailPanel, 0.02f);
        }
    }

    private void addTaskDetail(PlayerQuestState quest, TaskProgress task) {
        if (!theme.isClassicBook()) {
            addGlassTaskDetail(quest, task);
            return;
        }
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

    private void addGlassTaskDetail(PlayerQuestState quest, TaskProgress task) {
        QuestCategoryData category = selectedCategoryId.isEmpty() ? categoryForQuest(quest) : findCategory(selectedCategoryId);
        Component categoryTitle = category == null ? Component.translatable("viscript_quests.quest_book.uncategorized")
                : richText(category.title == null || category.title.isBlank() ? category.id : category.title);
        Label breadcrumb = label(categoryTitle.copy().append(" / ")
                .append(richText(quest.title == null || quest.title.isBlank() ? quest.questId : quest.title)), 7, theme.mutedText());
        breadcrumb.layout(layout -> layout.widthPercent(100).height(11));
        breadcrumb.textStyle(style -> style.textWrap(TextWrap.HIDE));
        detailPanel.addChild(breadcrumb);
        UIElement titleRow = new UIElement();
        titleRow.layout(layout -> layout.widthPercent(100).height(24)
                .flexDirection(FlexDirection.ROW).alignItems(AlignItems.CENTER).gapAll(5));
        Label title = label(richText(taskTitle(task)), 13, theme.primaryText());
        title.setId("quest_book_detail_title");
        title.layout(layout -> layout.width(0).flex(1).height(23));
        title.textStyle(style -> style.textWrap(TextWrap.HOVER_ROLL));
        Label status = label(task.status == TaskStatus.ACTIVE
                ? Component.translatable("viscript_quests.quest_book.status.active") : task.status.displayName(),
                7, theme.accentText());
        status.layout(layout -> layout.paddingHorizontal(6).height(18));
        status.textStyle(style -> style.adaptiveWidth(true).textAlignHorizontal(Horizontal.CENTER).textAlignVertical(Vertical.CENTER));
        status.style(style -> style.backgroundTexture(theme.cardBackground()));
        titleRow.addChildren(title, status);
        detailPanel.addChild(titleRow);
        if (task.subtitle != null && !task.subtitle.isBlank()) {
            Label subtitle = label(richText(task.subtitle), 8, theme.secondaryText());
            subtitle.layout(layout -> layout.widthPercent(100).height(13));
            subtitle.textStyle(style -> style.textWrap(TextWrap.HOVER_ROLL));
            detailPanel.addChild(subtitle);
        }
        UIElement divider = texturedPanel(theme.divider());
        divider.layout(layout -> layout.widthPercent(100).height(1));
        detailPanel.addChild(divider);

        ScrollerView content = new ScrollerView();
        content.setId("quest_book_detail_content");
        content.layout(layout -> layout.widthPercent(100).height(0).flex(1));
        configureVerticalScroller(content, 0, 6);
        boolean hasDescription = false;
        if (task.description != null) {
            for (String line : task.description) {
                if (line == null || line.isBlank()) continue;
                Label paragraph = wrappedLabel(richText(line), theme.secondaryText());
                paragraph.textStyle(style -> style.fontSize(8));
                content.addScrollViewChild(paragraph);
                hasDescription = true;
            }
        }
        if (!hasDescription) content.addScrollViewChild(wrappedLabel(taskHint(task), theme.secondaryText()));
        UIElement objectives = glassSection("viscript_quests.quest_book.objectives_label");
        objectives.setId("quest_objectives");
        if (task.objectives.isEmpty()) {
            objectives.addChild(wrappedLabel(taskHint(task), theme.secondaryText()));
        } else {
            for (int i = 0; i < task.objectives.size(); i++) {
                TaskObjectiveProgress objective = task.objectives.get(i);
                if (objective != null && objective.isVisibleInTaskBook()) {
                    objectives.addChild(createGlassObjectiveRow(quest, task, i, objective));
                }
            }
        }
        content.addScrollViewChild(objectives);
        detailPanel.addChild(content);

        UIElement rewards = glassSection("viscript_quests.quest_book.rewards_label");
        ScrollerView rewardScroller = new ScrollerView();
        rewardScroller.layout(layout -> layout.widthPercent(100).height(25));
        configureHorizontalScroller(rewardScroller, 0, 5);
        List<RewardDisplay> displays = rewardsForStep(quest, task.stepId);
        if (displays.isEmpty()) {
            Label noRewards = label(Component.translatable("viscript_quests.quest_book.no_rewards"), 7, theme.mutedText());
            noRewards.layout(layout -> layout.height(18));
            noRewards.textStyle(style -> style.adaptiveWidth(true));
            rewardScroller.addScrollViewChild(noRewards);
        } else {
            for (RewardDisplay reward : displays) rewardScroller.addScrollViewChild(createRewardSlot(reward));
        }
        rewards.addChild(rewardScroller);
        detailPanel.addChild(rewards);
        UIElement footer = new UIElement();
        footer.layout(layout -> layout.widthPercent(100).height(23)
                .flexDirection(FlexDirection.ROW).alignItems(AlignItems.CENTER).gapAll(5));
        long completed = task.objectives.stream()
                .filter(objective -> objective != null && objective.isVisibleInTaskBook() && objective.isCompleted()).count();
        long total = task.objectives.stream()
                .filter(objective -> objective != null && objective.isVisibleInTaskBook()).count();
        Label progress = label(Component.translatable("viscript_quests.quest_book.objectives", completed, total), 7, theme.mutedText());
        progress.layout(layout -> layout.width(0).flex(1).height(16));
        progress.textStyle(style -> style.textWrap(TextWrap.HIDE).textAlignVertical(Vertical.CENTER));
        footer.addChild(progress);
        if (task.status == TaskStatus.ACTIVE) {
            Button track = actionButton(Component.translatable(isTrackedTask(quest, task)
                    ? "viscript_quests.quest_book.untrack_task_button" : "viscript_quests.quest_book.track_task_button"));
            track.setId("quest_book_track");
            track.layout(layout -> layout.width(82).height(22));
            track.setOnClick(event -> toggleTrackedTask(quest, task));
            footer.addChild(track);
        }
        detailPanel.addChild(footer);
    }

    private UIElement glassSection(String translationKey) {
        UIElement section = new UIElement();
        section.layout(layout -> layout.widthPercent(100).flexDirection(FlexDirection.COLUMN).gapAll(4));
        Label header = label(Component.translatable(translationKey), 8, theme.accentText());
        header.layout(layout -> layout.widthPercent(100).height(13));
        header.textStyle(style -> style.textWrap(TextWrap.HIDE));
        header.setAllowHitTest(false);
        section.addChild(header);
        return section;
    }

    private UIElement createGlassObjectiveRow(PlayerQuestState quest, TaskProgress task, int objectiveIndex,
                                              TaskObjectiveProgress objective) {
        UIElement row = texturedPanel(theme.cardBackground());
        if (!objective.recipeLookupItem.isEmpty()) recipeLookupItems.put(row, objective.recipeLookupItem);
        row.setId("quest_objective_" + objectiveIndex);
        row.layout(layout -> {
            layout.widthPercent(100);
            layout.minHeight(31);
            layout.flexDirection(FlexDirection.ROW);
            layout.alignItems(AlignItems.CENTER);
            layout.paddingHorizontal(4);
            layout.gapAll(4);
        });

        UIElement icon = createDisplayIcon(objective.displayIcon, objectiveTooltip(objective));
        icon.layout(layout -> layout.width(20).height(20));
        row.addChild(icon);

        UIElement textColumn = new UIElement();
        textColumn.layout(layout -> {
            layout.width(0);
            layout.flex(1);
            layout.minHeight(25);
            layout.flexDirection(FlexDirection.COLUMN);
        });
        Label text = label(richText(objective.progressHintWithType()), 7,
                objectiveTextColor(objective));
        text.layout(layout -> layout.widthPercent(100).height(14));
        text.textStyle(style -> style.textWrap(TextWrap.HIDE));
        text.setAllowHitTest(false);
        textColumn.addChild(text);

        UIElement meter = new UIElement();
        meter.style(style -> style.backgroundTexture(new ColorRectTexture(0x664A5964)));
        meter.layout(layout -> {
            layout.widthPercent(100);
            layout.height(3);
        });
        UIElement fill = new UIElement();
        fill.style(style -> style.backgroundTexture(new ColorRectTexture(
                objective.isCompleted() ? 0xFFE4BE68 : 0xFF8DC9DB)));
        float progress = objective.requiredAmount <= 0 ? 0
                : Clamp.clamp((float) objective.currentAmount / objective.requiredAmount, 0f, 1f);
        fill.layout(layout -> {
            layout.widthPercent(progress * 100f);
            layout.heightPercent(100);
        });
        meter.addChild(fill);
        textColumn.addChild(meter);
        row.addChild(textColumn);

        if (objective.hasPonderView()) {
            row.addChild(createPonderButton(quest, task, objectiveIndex, objective));
        } else if (task.status == TaskStatus.ACTIVE && objective.manualSubmitRequired && objective.isActive()) {
            row.addChild(createSubmitButton(quest, task, objectiveIndex));
        }
        return row;
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
        scroller.setId("quest_objectives");
        configureVerticalScroller(scroller, 0, 1);

        if (task.objectives.isEmpty()) {
            scroller.addScrollViewChild(wrappedLabel(taskHint(task), TEXT_DARK));
        } else {
            for (int i = 0; i < task.objectives.size(); i++) {
                TaskObjectiveProgress objective = task.objectives.get(i);
                if (objective == null || !objective.isVisibleInTaskBook()) {
                    continue;
                }
                scroller.addScrollViewChild(createObjectiveRow(quest, task, i, objective));
            }
        }
        card.addChild(scroller);
        detailPanel.addChild(card);
    }

    private UIElement createObjectiveRow(PlayerQuestState quest, TaskProgress task, int objectiveIndex,
                                         TaskObjectiveProgress objective) {
        UIElement row = new UIElement();
        if (!objective.recipeLookupItem.isEmpty()) {
            recipeLookupItems.put(row, objective.recipeLookupItem);
        }
        row.setId("quest_objective_" + objectiveIndex);
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
        icon.setId("quest_objective_icon_" + objectiveIndex);
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
        } else if (task.status == TaskStatus.ACTIVE && objective.manualSubmitRequired && objective.isActive()) {
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
        if (!theme.isClassicBook()) {
            UIElement card = texturedPanel(theme.cardBackground());
            card.layout(layout -> layout.height(24).paddingHorizontal(5).gapAll(4)
                    .flexDirection(FlexDirection.ROW).alignItems(AlignItems.CENTER));
            UIElement icon = createDisplayIcon(reward == null ? null : reward.icon, tooltip);
            icon.layout(layout -> layout.width(16).height(16));
            Label title = label(tooltip, 7, theme.primaryText());
            title.layout(layout -> layout.height(16));
            title.textStyle(style -> style.adaptiveWidth(true).textAlignVertical(Vertical.CENTER));
            card.addChildren(icon, title);
            return card;
        }
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
        if (task.status != TaskStatus.ACTIVE) {
            return;
        }
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
                .baseTexture(theme.isClassicBook() ? BUTTON_DEFAULT : theme.buttonBase())
                .hoverTexture(theme.isClassicBook() ? BUTTON_HOVER : theme.buttonHover())
                .pressedTexture(theme.isClassicBook() ? BUTTON_HOLD : theme.buttonPressed()));
        button.textStyle(style -> style
                .textColor(theme.isClassicBook() ? TEXT_DARK : theme.primaryText())
                .fontSize(theme.isClassicBook() ? FONT_SMALL : 7)
                .textShadow(!theme.isClassicBook())
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

    /**
     * 分类导航专用的横向滚动容器。
     *
     * <p>触摸板在纵向滑动时可能同时产生很小的横向增量。LDLib2 的横向滚动器只要检测到
     * {@code deltaX != 0} 就会优先使用该增量，导致分类已经位于左边界时被横向噪声带回右侧。
     * 这里选择绝对值更大的轴，保留真正的横向滑动，同时让纵向触摸板手势稳定地走纵向回退量。</p>
     */
    private static final class QuestCategoryScrollerView extends ScrollerView {
        @Override
        protected void onScrollWheel(UIEvent event) {
            float delta = Math.abs(event.deltaX) > Math.abs(event.deltaY)
                    ? event.deltaX : event.deltaY;
            horizontalScroller.scrollByWheel(delta);
        }
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
            if (statusFilter == QuestStatusFilter.COMPLETED && quest.status != QuestStatus.COMPLETED
                    && quest.taskProgresses.stream().noneMatch(task ->
                    task.status == TaskStatus.COMPLETED || task.status == TaskStatus.SKIPPED)) {
                continue;
            }
            if (statusFilter == QuestStatusFilter.ACTIVE && quest.status == QuestStatus.COMPLETED) {
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
        if (!theme.isClassicBook() && selectedCategoryId.isEmpty()) return playerData.quests;
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
        return statusFilter == QuestStatusFilter.COMPLETED
                ? task.status == TaskStatus.COMPLETED || task.status == TaskStatus.SKIPPED
                : task.status != TaskStatus.COMPLETED && task.status != TaskStatus.SKIPPED;
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
        if (!tracked && task.status != TaskStatus.ACTIVE) {
            return;
        }
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
        if (task.status == TaskStatus.ACTIVE && objective.isActive() && !objective.isFailureCondition()) {
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
        if (objective.isCompleted()) {
            return theme.isClassicBook() ? TEXT_GREEN : theme.accentText();
        }
        if (objective.isFailureCondition()) {
            return theme.isClassicBook() ? TEXT_RED : theme.dangerText();
        }
        if (objective.isOptional()) {
            return theme.isClassicBook() ? TEXT_MUTED : theme.mutedText();
        }
        return theme.isClassicBook() ? TEXT_DARK : theme.primaryText();
    }

    private void animateIn(UIElement element, float delay) {
        if (element == null) {
            return;
        }
        if (!ClientConfig.ENABLE_QUEST_UI_ANIMATIONS.get()) {
            element.style(style -> style.opacity(1f));
            return;
        }
        element.style(style -> style.opacity(0f));
        ISubscription animationSubscription = element.animation()
                .duration(0.16f)
                .delay(delay)
                .ease(Eases.QUAD_OUT)
                .style(PropertyRegistry.OPACITY, 1f)
                .start();
        if (element == detailPanel) {
            detailTransition = animationSubscription;
        }
    }

    private float selectedQuestRowY() {
        if (theme.isClassicBook() || selectedQuest == null || selectedStepId == null
                || selectedStepId.isBlank() || questListView == null) {
            return Float.NaN;
        }
        String rowId = "quest_step_" + selectedStepId;
        return questListView.selectId(rowId).findFirst()
                .map(this::selectedQuestVisualY)
                .orElse(Float.NaN);
    }

    private float selectedQuestVisualY(UIElement row) {
        float y = row.getPositionY();
        UIElement selection = row.selectId(row.getId() + "_selection").findFirst().orElse(null);
        if (selection == null) {
            return y;
        }
        Transform2D transform = selection.getStyleBag().getComputed(PropertyRegistry.TRANSFORM_2D);
        if (transform == null) {
            return y;
        }
        var translateY = transform.translate().getY();
        return y + (translateY.isPercent() ? translateY.resolve(selection.getSizeHeight()) : translateY.getValue());
    }

    /**
     * 让新选中的任务行从旧任务行的位置滑入，形成选中框移动到目标任务的效果。
     * 列表重建后旧行已经被移除，所以这里只移动新行本身；视觉上选中底板会沿旧位置过渡。
     */
    private void animateSelectedQuestFrom(float previousSelectedY) {
        if (theme.isClassicBook() || !ClientConfig.ENABLE_QUEST_UI_ANIMATIONS.get()
                || Float.isNaN(previousSelectedY) || questListView == null || selectedQuest == null
                || selectedStepId == null || selectedStepId.isBlank()) {
            return;
        }
        String rowId = "quest_step_" + selectedStepId;
        UIElement row = questListView.selectId(rowId).findFirst().orElse(null);
        UIElement selection = row == null ? null
                : row.selectId(rowId + "_selection").findFirst().orElse(null);
        if (row == null || selection == null) {
            return;
        }
        UIEventListener[] layoutListener = new UIEventListener[1];
        layoutListener[0] = event -> {
            row.removeEventListener(UIEvents.LAYOUT_CHANGED, layoutListener[0]);
            float deltaY = previousSelectedY - row.getPositionY();
            if (Math.abs(deltaY) < 0.5f) {
                return;
            }
            if (selectedHighlightTransition != null) {
                selectedHighlightTransition.unsubscribe();
            }
            selection.getStyle().transform2D(Transform2D.identity().translate(0, deltaY));
            selectedHighlightTransition = selection.animation()
                    .duration(0.24f)
                    .ease(Eases.QUART_OUT)
                    .style(PropertyRegistry.TRANSFORM_2D, Transform2D.identity())
                    .start();
        };
        row.addEventListener(UIEvents.LAYOUT_CHANGED, layoutListener[0]);
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

    private UIElement createAllCategoryIcon() {
        DisplayIcon configured = categoryData == null ? null : categoryData.allCategoryIcon;
        if (configured != null && (configured.isTexture()
                ? ResourceLocation.tryParse(configured.getTexture()) != null
                : !configured.renderItemStack().isEmpty())) {
            return createDisplayIcon(configured, Component.empty());
        }
        return texturedPanel(Icons.GRID.copy().setColor(theme.secondaryText()));
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

    }

    private enum QuestStatusFilter {
        ACTIVE,
        COMPLETED
    }

    @Override
    public void initScreen(int screenWidth, int screenHeight) {
        super.initScreen(screenWidth, screenHeight);
        applyViewportScale(screenWidth, screenHeight);
    }

    public static Size getViewportSize(Size screenSize) {
        if (QuestTheme.current().isClassicBook()) return screenSize;
        float scale = getGlassGuiScaleFactor();
        return Size.of(Math.max(1, Math.round(screenSize.getWidth() / scale)),
                Math.max(1, Math.round(screenSize.getHeight() / scale)));
    }

    private static float getGlassGuiScaleFactor() {
        Minecraft minecraft = Minecraft.getInstance();
        var window = minecraft.getWindow();
        if (window.getGuiScale() <= 0) return 1;
        int autoScale = window.calculateScale(0, minecraft.isEnforceUnicode());
        // 保留双栏阅读所需的逻辑空间，同时跟随窗口大小缩放。
        float readableScale = Math.max(1, Math.min(window.getWidth() / 640f, window.getHeight() / 360f));
        return (float) (Math.min(autoScale, readableScale) / window.getGuiScale());
    }

    private void applyViewportScale(int screenWidth, int screenHeight) {
        if (!theme.isClassicBook()) {
            float scale = getGlassGuiScaleFactor();
            transform(transform -> transform.pivot(0.5f, 0.5f).scale(scale));
            return;
        }
        float widthScale = screenWidth * MAX_SCREEN_WIDTH_RATIO / BOOK_WIDTH;
        float heightScale = screenHeight * MAX_SCREEN_HEIGHT_RATIO / BOOK_HEIGHT;
        float scale = Math.max(0.01f, Math.min(widthScale, heightScale));
        bookElement.transform(transform -> transform.pivot(0.5f, 0.5f).scale(scale));
    }
}
