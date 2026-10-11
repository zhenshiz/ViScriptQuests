package com.viscriptquests.uitest;

import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.registry.RegistrationEnvironment;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegisterClient;
import com.lowdragmc.lowdraglib2.uitest.*;
import com.viscriptquests.ViScriptQuests;
import com.viscriptquests.config.ClientConfig;
import com.viscriptquests.config.QuestUiTheme;
import com.viscriptquests.gui.QuestBookUI;
import com.viscriptquests.quest.data.runtime.*;

@LDLRegisterClient(name = "quest_book_status_tabs", group = ViScriptQuests.MOD_ID,
        registry = UIScenario.REGISTRY, environment = RegistrationEnvironment.DEV_ONLY)
public final class QuestBookStatusTabsScenario implements UIScenario {
    @Override
    public void configure(ScenarioOptions options) {
        options.tags("quest_book", "status");
    }

    @Override
    public void define(ScenarioBuilder scenario) {
        scenario.step("保存主题配置", c -> {
            c.put("theme", ClientConfig.QUEST_UI_THEME.get());
            c.put("animations", ClientConfig.ENABLE_QUEST_UI_ANIMATIONS.get());
            ClientConfig.ENABLE_QUEST_UI_ANIMATIONS.set(false);
        });
        for (var theme : QuestUiTheme.values()) {
            scenario.openModularUI("打开任务书 " + theme, c -> {
                ClientConfig.QUEST_UI_THEME.set(theme);
                return new ModularUI(UI.of(new QuestBookUI(fixture()), QuestBookUI::getViewportSize));
            }).awaitModularUI().checkCount("#quest_book_status_filters > button", 2)
                    .checkVisible("#quest_book_status_active").checkVisible("#quest_book_status_completed")
                    .checkClass("#quest_book_status_active", "selected")
                    .checkExists("#quest_step_active").checkNotExists("#quest_step_completed")
                    .step("检查标签可见且互不遮挡 " + theme, c -> {
                        var active = c.el("#quest_book_status_active").bounds();
                        var completed = c.el("#quest_book_status_completed").bounds();
                        c.check("两个标签互不重叠", active.right() <= completed.x());
                        c.check("标签完整处于屏幕内", active.x() >= 0 && completed.right() <= c.screen().width
                                && active.bottom() <= c.screen().height && completed.bottom() <= c.screen().height);
                        if (theme == QuestUiTheme.CLASSIC_BOOK) {
                            var book = c.el("#quest_book_panel").bounds();
                            c.check("标签位于书本右下沿", active.x() > book.centerX() && active.y() > book.y() + book.height() * 0.9f);
                        } else {
                            var title = c.el("#quest_book_title").bounds();
                            var tabs = c.el("#quest_book_category_tabs").bounds();
                            c.check("状态文字与标题分类位于同一行", Math.abs(active.centerY() - title.centerY()) <= 1
                                    && Math.abs(completed.centerY() - title.centerY()) <= 1
                                    && Math.abs(tabs.centerY() - title.centerY()) <= 1);
                            for (String selector : new String[]{"#quest_book_status_active", "#quest_book_status_completed"}) {
                                var style = c.el(selector).as(Button.class).getButtonStyle();
                                c.check(selector + " 使用透明背景", style.baseTexture() == IGuiTexture.EMPTY
                                        && style.hoverTexture() == IGuiTexture.EMPTY && style.pressedTexture() == IGuiTexture.EMPTY);
                            }
                        }
                    }).step("进行中状态下划线 " + theme, c -> {
                        if (theme == QuestUiTheme.GLASS_DARK) {
                            c.check("仅进行中显示下划线", c.el("#quest_book_status_active_underline").isVisible()
                                    && !c.el("#quest_book_status_completed_underline").isVisible());
                        }
                    }).hover("#quest_book_status_filters").screenshot(theme.name().toLowerCase() + "_active")
                    .click("#quest_book_status_completed").checkClass("#quest_book_status_completed", "selected")
                    .checkExists("#quest_step_completed").checkNotExists("#quest_step_active")
                    .step("完成状态下划线切换 " + theme, c -> {
                        if (theme == QuestUiTheme.GLASS_DARK) {
                            c.check("仅已完成显示下划线", c.el("#quest_book_status_completed_underline").isVisible()
                                    && !c.el("#quest_book_status_active_underline").isVisible());
                        }
                    })
                    .hover("#quest_book_status_filters").screenshot(theme.name().toLowerCase() + "_completed")
                    .click("#quest_book_status_active").checkClass("#quest_book_status_active", "selected")
                    .step("返回进行中恢复下划线 " + theme, c -> {
                        if (theme == QuestUiTheme.GLASS_DARK) {
                            c.check("进行中下划线恢复", c.el("#quest_book_status_active_underline").isVisible()
                                    && !c.el("#quest_book_status_completed_underline").isVisible());
                        }
                    })
                    .checkExists("#quest_step_active").checkNotExists("#quest_step_completed").closeScreen();
        }
        scenario.openModularUI("打开经典主题多页分类", c -> {
            ClientConfig.QUEST_UI_THEME.set(QuestUiTheme.CLASSIC_BOOK);
            var book = new QuestBookUI(pagedFixture());
            c.put("classic_book", book);
            return new ModularUI(UI.of(book, QuestBookUI::getViewportSize));
        }).awaitModularUI().awaitElement("#quest_book_category_0")
                .checkExists("#quest_step_active").checkNotExists("#quest_book_category_5")
                .click("#quest_book_category_pager").awaitElement("#quest_book_category_5")
                .checkNotExists("#quest_book_category_0").checkExists("#quest_step_active")
                .step("经典主题完整同步保留浏览页", c -> {
                    QuestBookUI book = c.get("classic_book");
                    book.syncBookData(pagedFixture());
                }).checkExists("#quest_book_category_5").checkNotExists("#quest_book_category_0")
                .checkExists("#quest_step_active").checkNotExists("#quest_step_page_two")
                .step("经典主题玩家同步保留浏览页", c -> {
                    QuestBookUI book = c.get("classic_book");
                    book.syncPlayerData(pagedFixture().playerData);
                }).checkExists("#quest_book_category_5").checkNotExists("#quest_book_category_0")
                .checkExists("#quest_step_active").checkNotExists("#quest_step_page_two")
                .closeScreen().openModularUI("经典主题定位记忆中的第二页分类", c -> {
                    var data = pagedFixture();
                    data.playerData.lastViewedCategoryId = "page_category_5";
                    return new ModularUI(UI.of(new QuestBookUI(data), QuestBookUI::getViewportSize));
                }).awaitModularUI().awaitElement("#quest_book_category_5")
                .checkNotExists("#quest_book_category_0")
                .checkExists("#quest_step_page_two").checkNotExists("#quest_step_active").closeScreen();
        scenario.teardown("恢复主题配置", c -> {
            QuestUiTheme theme = c.get("theme");
            Boolean animations = c.get("animations");
            if (theme != null) ClientConfig.QUEST_UI_THEME.set(theme);
            if (animations != null) ClientConfig.ENABLE_QUEST_UI_ANIMATIONS.set(animations);
        });
    }

    private static QuestBookData fixture() {
        var book = new QuestBookData();
        var category = QuestCategoryData.of("status_tabs", "任务列表", "minecraft:book");
        book.categoryData.categories.add(category); book.playerData.lastViewedCategoryId = category.id;
        for (boolean completed : new boolean[]{false, true}) {
            var quest = new PlayerQuestState();
            quest.questId = completed ? "completed_quest" : "active_quest";
            quest.categoryId = category.id;
            quest.title = completed ? "已完成任务" : "进行中任务";
            quest.subtitle = "任务书状态筛选";
            quest.status = completed ? QuestStatus.COMPLETED : QuestStatus.ACTIVE;
            var task = new TaskProgress();
            task.stepId = completed ? "completed" : "active";
            task.title = completed ? "收集完成" : "收集泥土";
            task.status = completed ? TaskStatus.COMPLETED : TaskStatus.ACTIVE;
            quest.taskProgresses.add(task); book.playerData.quests.add(quest);
        }
        return book;
    }

    private static QuestBookData pagedFixture() {
        var book = fixture();
        for (int index = 1; index <= 5; index++) {
            book.categoryData.categories.add(QuestCategoryData.of("page_category_" + index,
                    "任务分类 " + (index + 1), "minecraft:book"));
        }
        var quest = new PlayerQuestState();
        quest.questId = "page_two_quest";
        quest.categoryId = "page_category_5";
        quest.title = "远行准备";
        var task = new TaskProgress();
        task.stepId = "page_two";
        task.title = "准备远行物资";
        quest.taskProgresses.add(task);
        book.playerData.quests.add(quest);
        return book;
    }
}
