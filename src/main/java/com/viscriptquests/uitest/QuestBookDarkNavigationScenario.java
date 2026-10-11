package com.viscriptquests.uitest;

import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.data.Horizontal;
import com.lowdragmc.lowdraglib2.gui.ui.data.Vertical;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.gui.ui.elements.ScrollerView;
import com.lowdragmc.lowdraglib2.gui.ui.elements.TextElement;
import com.lowdragmc.lowdraglib2.gui.ui.data.ScrollerMode;
import com.lowdragmc.lowdraglib2.registry.RegistrationEnvironment;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegisterClient;
import com.lowdragmc.lowdraglib2.uitest.ScenarioBuilder;
import com.lowdragmc.lowdraglib2.uitest.ScenarioOptions;
import com.lowdragmc.lowdraglib2.uitest.ElementBounds;
import com.lowdragmc.lowdraglib2.uitest.TestContext;
import com.lowdragmc.lowdraglib2.uitest.UIScenario;
import com.viscriptquests.ViScriptQuests;
import com.viscriptquests.config.ClientConfig;
import com.viscriptquests.config.QuestUiTheme;
import com.viscriptquests.gui.QuestBookUI;
import com.viscriptquests.quest.data.DisplayIcon;
import com.viscriptquests.quest.data.QuestSavedData;
import com.viscriptquests.quest.data.runtime.PlayerQuestState;
import com.viscriptquests.quest.data.runtime.QuestBookData;
import com.viscriptquests.quest.data.runtime.QuestCategoryData;
import com.viscriptquests.quest.data.runtime.QuestStatus;
import com.viscriptquests.quest.data.runtime.RewardDisplay;
import com.viscriptquests.quest.data.runtime.TaskObjectiveProgress;
import com.viscriptquests.quest.data.runtime.TaskProgress;
import com.viscriptquests.quest.data.runtime.TaskStatus;
import com.viscriptquests.quest.runtime.QuestManager;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

@LDLRegisterClient(name = "quest_book_dark_navigation", group = ViScriptQuests.MOD_ID,
        registry = UIScenario.REGISTRY, environment = RegistrationEnvironment.DEV_ONLY)
public final class QuestBookDarkNavigationScenario implements UIScenario {
    @Override
    public void configure(ScenarioOptions options) {
        options.tags("quest_book", "navigation");
    }

    @Override
    public void define(ScenarioBuilder scenario) {
        scenario.step("保存主题配置", c -> {
            c.put("theme", ClientConfig.QUEST_UI_THEME.get());
            c.put("animations", ClientConfig.ENABLE_QUEST_UI_ANIMATIONS.get());
            ClientConfig.QUEST_UI_THEME.set(QuestUiTheme.GLASS_DARK);
            ClientConfig.ENABLE_QUEST_UI_ANIMATIONS.set(false);
        }).server("保存服务端分类记忆", c -> {
            var data = QuestSavedData.get(c.server()).getPlayer(c.player().getUUID());
            c.put("server_category", data.lastViewedCategoryId);
        }).openModularUI("打开全部任务", c -> {
            var book = new QuestBookUI(fixture());
            c.put("book", book);
            return new ModularUI(UI.of(book, QuestBookUI::getViewportSize));
        }).awaitModularUI().awaitElement("#quest_book_category_all")
                .checkClass("#quest_book_category_all", "selected")
                .checkVisible("#quest_book_category_all_underline")
                .checkHidden("#quest_book_category_underline")
                .checkExists("#quest_book_category_scroll")
                .checkNotExists("#quest_book_category_previous")
                .checkNotExists("#quest_book_category_pager")
                .checkExists("#quest_book_category_0").checkExists("#quest_book_category_4")
                .checkExists("#quest_book_category_5").checkExists("#quest_book_category_6")
                .checkCount(".quest-category-group", 4).checkCount(".quest-group", 4)
                .checkCount(".quest-step", 4).checkCount("#quest_step_shared", 1)
                .checkExists("#quest_book_category_group_0")
                .checkExists("#quest_book_category_group_1")
                .checkExists("#quest_book_category_group_5")
                .checkExists("#quest_book_category_group_uncategorized")
                .checkNotExists("#quest_book_category_group_2")
                .checkExists("#quest_step_uncategorized")
                .checkTextContains("#quest_book_detail_title", "补充锻造材料")
                .checkExists("#quest_objective_0").checkExists("#quest_objective_1")
                .checkCount("#quest_objective_0 > button", 1).checkCount("#quest_objective_1 > button", 1)
                .step("全屏布局与顶部导航", c -> {
                    var shell = c.el("#quest_book_panel_glass_dark").bounds();
                    var header = c.el("#quest_book_header").bounds();
                    var icon = c.el("#quest_book_icon").bounds();
                    var title = c.el("#quest_book_title").bounds();
                    var status = c.el("#quest_book_status_filters").bounds();
                    var tabs = c.el("#quest_book_category_tabs").bounds();
                    var list = c.el("#quest_book_list").bounds();
                    var body = ElementBounds.of(c.el("#quest_book_list").element().getParent());
                    var detail = c.el("#quest_book_detail").bounds();
                    var category = c.el("#quest_book_category_group_0").bounds();
                    var group = c.el("#quest_group_shared").bounds();
                    var step = c.el("#quest_step_shared").bounds();
                    c.check("深色面板覆盖全屏", Math.abs(shell.x()) <= 1 && Math.abs(shell.y()) <= 1
                            && Math.abs(shell.right() - c.screen().width) <= 1
                            && Math.abs(shell.bottom() - c.screen().height) <= 1);
                    c.check("分类位于列表和详情上方", tabs.bottom() <= list.y() + 1
                            && tabs.bottom() <= detail.y() + 1);
                    c.check("顶部导航高度压缩至约26像素", header.height() >= 24 && header.height() <= 28);
                    c.check("任务列表约占正文宽度的28%", body.width() > 0
                            && Math.abs(list.width() / body.width() - 0.28f) <= 0.015f);
                    c.check("书本图标较原布局放大", icon.width() > 18 && icon.height() > 18);
                    c.check("书本标题状态分类处于同一行", Math.abs(icon.centerY() - header.centerY()) <= 1
                            && Math.abs(title.centerY() - header.centerY()) <= 1
                            && Math.abs(status.centerY() - header.centerY()) <= 1
                            && Math.abs(tabs.centerY() - header.centerY()) <= 1);
                    c.check("顶部各组按顺序排列且互不遮挡", icon.right() <= title.x() + 1
                            && title.right() <= status.x() + 1 && status.right() <= tabs.x() + 1);
                    c.check("左右内容互不遮挡", list.right() <= detail.x() + 1);
                    c.check("内容完整处于屏幕内", list.x() >= 0 && detail.right() <= c.screen().width + 1
                            && list.bottom() <= c.screen().height + 1 && detail.bottom() <= c.screen().height + 1);
                    c.check("分类、任务组、步骤按三级顺序展示", category.bottom() <= group.y() + 1
                            && group.bottom() <= step.y() + 1);
                    for (String selector : new String[]{"#quest_book_category_all", "#quest_book_category_0",
                            "#quest_book_category_4", "#quest_book_category_5", "#quest_book_category_6"}) {
                        var button = c.el(selector).bounds();
                        c.check(selector + " 完整处于顶部区域", button.x() >= 0
                                && button.right() <= c.screen().width + 1 && button.y() >= tabs.y() - 1
                                && button.bottom() <= tabs.bottom() + 1);
                    }
                    for (String selector : new String[]{".quest-category-group", ".quest-group"}) {
                        for (var row : c.all(selector)) {
                            boolean countLabel = row.element().allChildrenStream()
                                    .filter(element -> element.isDisplayed() && element.isVisible())
                                    .filter(TextElement.class::isInstance).map(TextElement.class::cast)
                                    .anyMatch(element -> element.getText().getString().strip().matches("[0-9]+"));
                            c.check(row.element().getId() + " 不显示数字计数", !countLabel);
                        }
                    }
                }).step("分类横向滚动器承载全部图标", QuestBookDarkNavigationScenario::checkCategoryScroller)
                .step("分类仅显示图标并保留名称提示", QuestBookDarkNavigationScenario::checkCategoryIcons)
                .hover("#quest_book_status_filters").screenshot("dark_all_three_levels")
                .click("#quest_book_category_group_0")
                .waitUntil("折叠分类隐藏其任务组和步骤", c -> !c.exists("#quest_group_shared"))
                .checkNotExists("#quest_step_shared").checkExists("#quest_step_explore")
                .checkClass("#quest_book_category_all", "selected")
                .click("#quest_book_category_group_0").awaitElement("#quest_step_shared")
                .checkCount("#quest_step_shared", 1)
                .click("#quest_group_shared")
                .waitUntil("折叠任务组仅隐藏步骤", c -> !c.exists("#quest_step_shared"))
                .checkExists("#quest_group_shared").checkExists("#quest_book_category_group_0")
                .click("#quest_group_shared").awaitElement("#quest_step_shared")
                .click("#quest_step_shared")
                .checkTextContains("#quest_book_detail_title", "补充锻造材料");

        scenario.group("分类横向滚动与同步保持当前内容", s -> s
                .click("#quest_book_category_5")
                .waitUntil("选择任意分类后仅该分类显示任务", c -> c.count(".quest-step") == 1)
                .checkClass("#quest_book_category_5", "selected")
                .checkVisible("#quest_book_category_underline")
                .step("选中分类后下划线跟随共享指示器", QuestBookDarkNavigationScenario::checkCategoryIcons)
                .checkExists("#quest_step_craft").checkNotExists("#quest_step_shared")
                .checkTextContains("#quest_book_detail_title", "制作远行装备")
                .step("横向滚动访问末尾分类", QuestBookDarkNavigationScenario::scrollToLastCategory)
                .step("恢复分类滚动起点", c -> c.el("#quest_book_category_scroll").as(ScrollerView.class)
                        .horizontalScroller.setValue(0f))
                .step("同步完整任务书数据", c -> {
                    QuestBookUI book = c.get("book");
                    book.syncBookData(fixture());
                }).checkExists("#quest_book_category_0").checkExists("#quest_book_category_6")
                .checkTextContains("#quest_book_detail_title", "制作远行装备")
                .step("同步玩家进度不跳分类", c -> {
                    QuestBookUI book = c.get("book");
                    book.syncPlayerData(fixture().playerData);
                }).checkExists("#quest_book_category_0").checkExists("#quest_book_category_6")
                .checkTextContains("#quest_book_detail_title", "制作远行装备")
                .click("#quest_book_category_all")
                .waitUntil("全部恢复所有分类中的任务", c -> c.count(".quest-step") == 4)
                .checkClass("#quest_book_category_all", "selected")
                .checkVisible("#quest_book_category_all_underline").checkHidden("#quest_book_category_underline")
                .checkExists("#quest_book_category_0").checkExists("#quest_book_category_6"));

        scenario.group("状态与分类共同筛选", s -> s
                .click("#quest_book_status_completed")
                .waitUntil("全部中的已完成任务", c -> c.exists("#quest_step_completed"))
                .checkClass("#quest_book_status_completed", "selected")
                .checkVisible("#quest_book_status_completed_underline").checkHidden("#quest_book_status_active_underline")
                .checkClass("#quest_book_category_all", "selected")
                .checkCount(".quest-step", 1).checkCount(".quest-category-group", 1)
                .checkExists("#quest_book_category_group_0").checkNotExists("#quest_step_uncategorized")
                .checkExists("#quest_book_category_5").checkExists("#quest_book_category_0")
                .checkTextContains("#quest_book_detail_title", "完成村庄委托")
                .hover("#quest_book_status_filters").screenshot("dark_completed_all")
                .click("#quest_book_category_5")
                .waitUntil("无已完成任务时显示空态", c -> c.el("#quest_book_list_empty").isVisible())
                .checkCount(".quest-step", 0).checkExists("#quest_book_detail_empty")
                .checkClass("#quest_book_category_5", "selected")
                .step("非空任务切入无已完成任务时空态居中", QuestBookDarkNavigationScenario::checkEmptyCentered)
                .hover("#quest_book_status_filters").screenshot("dark_empty_centered")
                .click("#quest_book_status_active").awaitElement("#quest_step_craft")
                .checkVisible("#quest_book_status_active_underline").checkHidden("#quest_book_status_completed_underline")
                .checkCount(".quest-step", 1).checkHidden("#quest_book_list_empty")
                .click("#quest_step_craft").checkTextContains("#quest_book_detail_title", "制作远行装备")
                .click("#quest_book_category_1")
                .waitUntil("单独分类保留显式和运行时两种归属", c -> c.count(".quest-step") == 2)
                .checkExists("#quest_step_shared").checkExists("#quest_step_explore")
                .checkNotExists("#quest_step_uncategorized")
                .click("#quest_book_category_2")
                .waitUntil("空分类显示空态", c -> c.el("#quest_book_list_empty").isVisible())
                .checkCount(".quest-step", 0).checkExists("#quest_book_detail_empty")
                .checkClass("#quest_book_category_2", "selected")
                .checkVisible("#quest_book_category_underline")
                .step("非空分类切入空分类时空态居中", QuestBookDarkNavigationScenario::checkEmptyCentered)
                .click("#quest_book_category_all")
                .waitUntil("空分类返回全部后恢复任务树", c -> c.count(".quest-step") == 4)
                .checkCount("#quest_step_shared", 1).checkExists("#quest_step_uncategorized")
                .checkVisible("#quest_book_category_all_underline").checkHidden("#quest_book_category_underline")
                .checkHidden("#quest_book_list_empty")
                .click("#quest_step_shared").checkTextContains("#quest_book_detail_title", "补充锻造材料")
                .hover("#quest_book_status_filters").screenshot("dark_all_restored"));

        scenario.group("全部视图的真实服务端记忆", s -> s
                .click("#quest_book_category_0").checkClass("#quest_book_category_0", "selected")
                .server("预置非空分类记忆", c -> {
                    var saved = QuestSavedData.get(c.server());
                    var data = saved.getPlayer(c.player().getUUID());
                    data.lastViewedCategoryId = "quest_book_memory_sentinel";
                    saved.setDirty();
                    c.check("服务端原记忆非空", !data.lastViewedCategoryId.isEmpty());
                }).click("#quest_book_category_all").checkClass("#quest_book_category_all", "selected")
                .waitUntilServer("全部选择通过 C2S 清空服务端记忆", c -> QuestSavedData.get(c.server())
                        .getPlayer(c.player().getUUID()).lastViewedCategoryId.isEmpty())
                .checkServer("服务端保存全部视图", c -> QuestSavedData.get(c.server())
                        .getPlayer(c.player().getUUID()).lastViewedCategoryId.isEmpty())
                .closeScreen().server("从真实服务端入口重开任务书", c -> QuestManager.openQuestBook(c.player()))
                .awaitModularUI().awaitElement("#quest_book_category_all")
                .checkClass("#quest_book_category_all", "selected"));

        scenario.closeScreen().openModularUI("没有分类配置时打开全部任务", c -> {
            var data = fixture();
            data.categoryData.categories.clear();
            data.playerData.lastViewedCategoryId = "removed_category";
            return new ModularUI(UI.of(new QuestBookUI(data), QuestBookUI::getViewportSize));
        }).awaitModularUI().awaitElement("#quest_book_category_all")
                .checkClass("#quest_book_category_all", "selected")
                .checkNotExists("#quest_book_category_0")
                .checkNotExists("#quest_book_category_previous").checkNotExists("#quest_book_category_pager")
                .checkCount(".quest-category-group", 1)
                .checkExists("#quest_book_category_group_uncategorized")
                .checkCount(".quest-step", 4).checkExists("#quest_step_shared")
                .checkExists("#quest_step_uncategorized")
                .checkTextContains("#quest_book_detail_title", "补充锻造材料")
                .click("#quest_book_status_completed").awaitElement("#quest_step_completed")
                .checkCount(".quest-step", 1).checkCount(".quest-category-group", 1)
                .checkExists("#quest_book_category_group_uncategorized")
                .checkTextContains("#quest_book_detail_title", "完成村庄委托")
                .closeScreen().teardown("恢复主题配置", c -> {
            QuestUiTheme theme = c.get("theme");
            Boolean animations = c.get("animations");
            if (theme != null) ClientConfig.QUEST_UI_THEME.set(theme);
            if (animations != null) ClientConfig.ENABLE_QUEST_UI_ANIMATIONS.set(animations);
        }).teardownServer("恢复服务端分类记忆", c -> {
            String category = c.get("server_category");
            if (category != null) {
                var saved = QuestSavedData.get(c.server());
                saved.getPlayer(c.player().getUUID()).lastViewedCategoryId = category;
                saved.setDirty();
            }
        });
    }

    private static void checkCategoryIcons(TestContext context) {
        var underline = context.el("#quest_book_category_underline");
        var allUnderline = context.el("#quest_book_category_all_underline");
        boolean allSelected = context.el("#quest_book_category_all").hasClass("selected");
        context.check("全部分类使用自身下划线", allSelected == allUnderline.isVisible());
        context.check("普通分类使用共享下划线", allSelected != underline.isVisible());
        var selectedTab = context.all("#quest_book_category_tabs button").stream()
                .filter(element -> element.hasClass("selected"))
                .findFirst().orElse(null);
        if (!allSelected && selectedTab != null) {
            var line = underline.bounds();
            var button = selectedTab.bounds();
            context.check("分类下划线跟随选中分类", line.width() > 0 && line.height() > 0
                    && line.height() <= 3 && line.x() >= button.x() - 1 && line.right() <= button.right() + 1
                    && line.bottom() <= button.bottom() + 1 && line.bottom() >= button.bottom() - 4);
        }
        for (int index = -1; index < 7; index++) {
            String selector = index < 0 ? "#quest_book_category_all" : "#quest_book_category_" + index;
            if (!context.exists(selector)) continue;
            var tab = context.el(selector);
            boolean visibleText = tab.element().allChildrenStream()
                    .filter(element -> element.isDisplayed() && element.isVisible())
                    .filter(TextElement.class::isInstance).map(TextElement.class::cast)
                    .anyMatch(element -> !element.getText().getString().isBlank());
            context.check(selector + " 不显示分类文字", !visibleText);
            context.check(selector + " 提供分类名称提示", tab.element().getStyle().tooltips().asList().stream()
                    .anyMatch(text -> !text.getString().isBlank()));
            context.check(selector + " 选中状态由按钮类标识", tab.hasClass("selected")
                    == (selectedTab != null && selectedTab.element() == tab.element()));
        }
    }

    private static void checkCategoryScroller(TestContext context) {
        var scroller = context.el("#quest_book_category_scroll").as(ScrollerView.class);
        var viewport = ElementBounds.of(scroller.viewPort);
        context.check("分类使用横向 ScrollerView", scroller.getScrollerViewStyle().mode() == ScrollerMode.HORIZONTAL);
        context.check("分类滚动区域位于顶部导航", viewport.y() >= context.el("#quest_book_category_tabs").bounds().y() - 1
                && viewport.bottom() <= context.el("#quest_book_category_tabs").bounds().bottom() + 1);
        context.check("分类滚动器包含全部分类条目", context.exists("#quest_book_category_6")
                && context.exists("#quest_book_category_39"));
        context.check("末尾分类保留名称提示", context.el("#quest_book_category_39").element().getStyle()
                .tooltips().asList().stream().anyMatch(text -> !text.getString().isBlank()));
        context.check("深色分类不再显示分页控件", !context.exists("#quest_book_category_previous")
                && !context.exists("#quest_book_category_pager"));
    }

    private static void scrollToLastCategory(TestContext context) {
        var scroller = context.el("#quest_book_category_scroll").as(ScrollerView.class);
        var viewport = ElementBounds.of(scroller.viewPort);
        scroller.horizontalScroller.setValue(1f);
        context.check("分类内容宽于视口并支持横向滚动", scroller.getContainerWidth() > viewport.width());
        context.check("滚动器已移动到分类末端", scroller.horizontalScroller.getValue() >= 0.99f);
        context.check("末尾分类仍可通过横向滚动访问", context.exists("#quest_book_category_39"));
    }

    private static void checkEmptyCentered(TestContext context) {
        var empty = context.el("#quest_book_list_empty");
        var label = empty.as(Label.class);
        var bounds = empty.bounds();
        var list = context.el("#quest_book_list").as(ScrollerView.class);
        var viewport = ElementBounds.of(list.viewPort);
        context.check("空态文字非空且可见", empty.isVisible() && !label.getText().getString().isBlank());
        context.check("空态覆盖区域填满任务列表视口", Math.abs(bounds.width() - viewport.width()) <= 1
                && Math.abs(bounds.height() - viewport.height()) <= 1);
        context.check("空态中心与列表视口中心一致", Math.abs(bounds.centerX() - viewport.centerX()) <= 1
                && Math.abs(bounds.centerY() - viewport.centerY()) <= 1);
        context.check("空态文字水平垂直居中", label.getTextStyle().textAlignHorizontal() == Horizontal.CENTER
                && label.getTextStyle().textAlignVertical() == Vertical.CENTER);
    }

    static QuestBookData fixture() {
        var book = new QuestBookData();
        String[] names = {"主线任务", "探索委托", "成长任务", "日常委托", "冒险见闻", "工艺挑战", "节日活动"};
        String[] icons = {"book", "compass", "diamond", "emerald", "feather", "anvil", "firework_star"};
        for (int index = 0; index < names.length; index++) {
            book.categoryData.categories.add(QuestCategoryData.of("category_" + index, names[index], "minecraft:" + icons[index]));
        }
        // 测试数据保留足够分类以触发真实横向溢出，实际任务书通常不会有这么多分类。
        for (int index = names.length; index < 40; index++) {
            book.categoryData.categories.add(QuestCategoryData.of("category_" + index,
                    "扩展分类 " + index, "minecraft:book"));
        }
        book.categoryData.categories.get(0).questIds.add("shared");
        book.categoryData.categories.get(1).questIds.add("shared");
        var mainQuest = addQuest(book, "shared", "category_1", "新手村的委托", "补充锻造材料", false);
        mainQuest.icon = DisplayIcon.item("minecraft:book");
        var mainTask = mainQuest.taskProgresses.getFirst();
        mainTask.subtitle = "村里的铁匠需要一些材料来重新开炉。";
        mainTask.description = new String[]{"矿洞中的铁矿已经可以开采了。准备好铁锭，交给铁匠；你也可以先查看锻造流程，了解工具的制作方式。"};
        var iron = new TaskObjectiveProgress();
        iron.objectiveId = "iron";
        iron.hint = Component.literal("交付 8 个铁锭");
        iron.displayIcon = DisplayIcon.item("minecraft:iron_ingot");
        iron.recipeLookupItem = new ItemStack(Items.IRON_INGOT);
        iron.currentAmount = 8;
        iron.requiredAmount = 8;
        iron.manualSubmitRequired = true;
        mainTask.objectives.add(iron);
        var guide = new TaskObjectiveProgress();
        guide.objectiveId = "forge_guide";
        guide.hint = Component.literal("查看锻造流程");
        guide.displayIcon = DisplayIcon.item("minecraft:book");
        guide.ponderViewAction = true;
        guide.ponderComponentId = "minecraft:iron_ingot";
        mainTask.objectives.add(guide);
        String[] rewardItems = {"minecraft:emerald", "minecraft:bread", "minecraft:experience_bottle"};
        String[] rewardLabels = {"绿宝石 × 3", "面包 × 5", "经验 × 50"};
        for (int index = 0; index < rewardItems.length; index++) {
            var reward = new RewardDisplay();
            reward.stepId = "shared";
            reward.icon = DisplayIcon.item(rewardItems[index]);
            reward.displayText = Component.literal(rewardLabels[index]);
            mainQuest.rewardDisplays.add(reward);
        }
        addQuest(book, "explore", "category_1", "探索边境", "拜访村庄哨站", false);
        addQuest(book, "craft", "category_5", "工匠试炼", "制作远行装备", false);
        addQuest(book, "uncategorized", "missing_category", "远方来信", "寻找信件的主人", false);
        addQuest(book, "completed", "category_0", "初来乍到", "完成村庄委托", true);
        return book;
    }

    private static PlayerQuestState addQuest(QuestBookData book, String id, String category, String title,
                                             String stepTitle, boolean completed) {
        var quest = new PlayerQuestState();
        quest.questId = id;
        quest.categoryId = category;
        quest.title = title;
        quest.subtitle = switch (id) {
            case "shared" -> "第一章 · 重新开炉";
            case "explore" -> "边境来客 · 哨站见闻";
            case "craft" -> "工匠之路 · 远行准备";
            case "uncategorized" -> "一封未署名的信";
            case "completed" -> "序章 · 村庄的新朋友";
            default -> title;
        };
        quest.status = completed ? QuestStatus.COMPLETED : QuestStatus.ACTIVE;
        var task = new TaskProgress();
        task.stepId = id;
        task.title = stepTitle;
        task.subtitle = "为下一段旅程做好准备。";
        task.description = new String[]{"查看任务目标，完成委托后继续探索。"};
        task.status = completed ? TaskStatus.COMPLETED : TaskStatus.ACTIVE;
        quest.taskProgresses.add(task);
        book.playerData.quests.add(quest);
        return quest;
    }
}
