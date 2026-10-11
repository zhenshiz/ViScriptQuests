package com.viscriptquests.uitest;

import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.data.Transform2D;
import com.lowdragmc.lowdraglib2.gui.ui.style.PropertyRegistry;
import com.lowdragmc.lowdraglib2.registry.RegistrationEnvironment;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegisterClient;
import com.lowdragmc.lowdraglib2.uitest.ScenarioBuilder;
import com.lowdragmc.lowdraglib2.uitest.ScenarioOptions;
import com.lowdragmc.lowdraglib2.uitest.TestContext;
import com.lowdragmc.lowdraglib2.uitest.UIScenario;
import com.viscriptquests.ViScriptQuests;
import com.viscriptquests.config.ClientConfig;
import com.viscriptquests.config.QuestUiTheme;
import com.viscriptquests.gui.QuestBookUI;

/** 验证深色任务书在连续切换任务时只移动选中底板，且详情最终显示最后一次选择。 */
@LDLRegisterClient(name = "quest_book_animation", group = ViScriptQuests.MOD_ID,
        registry = UIScenario.REGISTRY, environment = RegistrationEnvironment.DEV_ONLY)
public final class QuestBookAnimationScenario implements UIScenario {
    @Override
    public void configure(ScenarioOptions options) {
        options.tags("quest_book", "animation");
    }

    @Override
    public void define(ScenarioBuilder scenario) {
        scenario.step("保存并启用任务书动画", c -> {
            c.put("theme", ClientConfig.QUEST_UI_THEME.get());
            c.put("animations", ClientConfig.ENABLE_QUEST_UI_ANIMATIONS.get());
            ClientConfig.QUEST_UI_THEME.set(QuestUiTheme.GLASS_DARK);
            ClientConfig.ENABLE_QUEST_UI_ANIMATIONS.set(true);
        }).openModularUI("打开动画任务书", c -> new ModularUI(
                UI.of(new QuestBookUI(QuestBookDarkNavigationScenario.fixture()), QuestBookUI::getViewportSize)))
                .awaitModularUI().awaitElement("#quest_step_shared")
                .click("#quest_step_explore")
                // 连续点击会取消上一段动画，最终内容必须跟随最后一次选择。
                .click("#quest_step_uncategorized")
                .waitUntil("详情渐入并完成最后一次选择", c -> hasFinalSelection(c, "寻找信件的主人"))
                .step("只移动选中底板", QuestBookAnimationScenario::checkSelectionLayer)
                .closeScreen().teardown("恢复任务书动画设置", c -> {
                    QuestUiTheme theme = c.get("theme");
                    Boolean animations = c.get("animations");
                    if (theme != null) ClientConfig.QUEST_UI_THEME.set(theme);
                    if (animations != null) ClientConfig.ENABLE_QUEST_UI_ANIMATIONS.set(animations);
                });
    }

    private static boolean hasFinalSelection(TestContext context, String title) {
        if (!context.exists("#quest_book_detail_title") || !context.exists("#quest_step_uncategorized_selection")) {
            return false;
        }
        var detail = context.el("#quest_book_detail").element();
        Float opacity = detail.getStyleBag().getComputed(PropertyRegistry.OPACITY);
        return context.el("#quest_book_detail_title").as(com.lowdragmc.lowdraglib2.gui.ui.elements.Label.class)
                        .getText().getString().contains(title)
                && (opacity == null || opacity >= 0.99f);
    }

    private static void checkSelectionLayer(TestContext context) {
        var row = context.el("#quest_step_uncategorized");
        var selection = context.el("#quest_step_uncategorized_selection");
        var rowBounds = row.bounds();
        var selectionBounds = selection.bounds();
        context.check("详情最终保持不透明", value(context.el("#quest_book_detail").element()
                .getStyleBag().getComputed(PropertyRegistry.OPACITY)) >= 0.99f);
        Transform2D rowTransform = row.element().getStyleBag().getComputed(PropertyRegistry.TRANSFORM_2D);
        context.check("任务文字与点击行没有位移动画", rowTransform == null || rowTransform.isIdentity());
        context.check("选中底板最终覆盖目标任务行", Math.abs(rowBounds.x() - selectionBounds.x()) <= 1
                && Math.abs(rowBounds.y() - selectionBounds.y()) <= 1
                && Math.abs(rowBounds.width() - selectionBounds.width()) <= 1
                && Math.abs(rowBounds.height() - selectionBounds.height()) <= 1);
    }

    private static float value(Float value) {
        return value == null ? 1f : value;
    }
}
