package com.viscriptquests.uitest;

import com.lowdragmc.lowdraglib2.Platform;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.registry.RegistrationEnvironment;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegisterClient;
import com.lowdragmc.lowdraglib2.uitest.ScenarioBuilder;
import com.lowdragmc.lowdraglib2.uitest.ScenarioOptions;
import com.lowdragmc.lowdraglib2.uitest.UIScenario;
import com.viscriptquests.ViScriptQuests;
import com.viscriptquests.config.ClientConfig;
import com.viscriptquests.config.QuestHudTheme;
import com.viscriptquests.config.QuestUiTheme;
import com.viscriptquests.gui.hud.QuestHudData;
import com.viscriptquests.gui.hud.TrackedQuestHud;
import com.viscriptquests.quest.data.runtime.PlayerQuestState;
import com.viscriptquests.quest.data.runtime.QuestPlayerData;
import com.viscriptquests.quest.data.runtime.TaskObjectiveProgress;
import com.viscriptquests.quest.data.runtime.TaskProgress;
import net.minecraft.network.chat.Component;

@LDLRegisterClient(name = "quest_hud_themes", group = ViScriptQuests.MOD_ID,
        registry = UIScenario.REGISTRY, environment = RegistrationEnvironment.DEV_ONLY)
public final class QuestHudThemeScenario implements UIScenario {
    @Override
    public void configure(ScenarioOptions options) {
        options.guiScale(2).tags("quest_hud", "theme");
    }

    @Override
    public void define(ScenarioBuilder scenario) {
        scenario.step("准备追踪任务", c -> {
            c.put("hud_theme", ClientConfig.QUEST_HUD_THEME.get());
            c.put("book_theme", ClientConfig.QUEST_UI_THEME.get());
            c.put("show_hud", ClientConfig.SHOW_TRACKED_QUEST_HUD.get());
            ClientConfig.QUEST_HUD_THEME.set(QuestHudTheme.CHARCOAL);
            ClientConfig.QUEST_UI_THEME.set(QuestUiTheme.CLASSIC_BOOK);
            ClientConfig.SHOW_TRACKED_QUEST_HUD.set(true);
            QuestHudData.update(fixture().serializeNBT(Platform.getFrozenRegistry()));
        }).openModularUI("打开追踪 HUD", c -> new ModularUI(UI.of(new TrackedQuestHud())))
                .awaitModularUI().awaitElement("#tracked_quest_hud_card")
                .waitUntil("三个目标显示", c -> c.count(".tracked-quest-hud-progress") == 3)
                .checkCount(".tracked-quest-hud-row", 3)
                .checkText("#tracked_quest_hud_title", "收集物资")
                .step("深灰面板按内容收紧", c -> {
                    var card = c.el("#tracked_quest_hud_card").bounds();
                    c.check("面板可见且高度紧凑", card.width() > 80 && card.height() > 70 && card.height() < 110);
                })
                .screenshot("quest_hud_charcoal")
                .step("更换任务书主题不改变 HUD", c -> ClientConfig.QUEST_UI_THEME.set(QuestUiTheme.GLASS_DARK))
                .checkCount(".tracked-quest-hud-progress", 3)
                .step("切换无背景 HUD", c -> ClientConfig.QUEST_HUD_THEME.set(QuestHudTheme.MINIMAL))
                .waitUntil("无背景主题恢复行内进度", c -> c.count(".tracked-quest-hud-progress") == 0)
                .checkCount(".tracked-quest-hud-row", 3)
                .screenshot("quest_hud_minimal")
                .closeScreen();
        scenario.teardown("恢复 HUD 配置", c -> {
            QuestHudTheme hudTheme = c.get("hud_theme");
            QuestUiTheme bookTheme = c.get("book_theme");
            Boolean showHud = c.get("show_hud");
            if (hudTheme != null) ClientConfig.QUEST_HUD_THEME.set(hudTheme);
            if (bookTheme != null) ClientConfig.QUEST_UI_THEME.set(bookTheme);
            if (showHud != null) ClientConfig.SHOW_TRACKED_QUEST_HUD.set(showHud);
            QuestHudData.update(new QuestPlayerData().serializeNBT(Platform.getFrozenRegistry()));
        });
    }

    private static QuestPlayerData fixture() {
        QuestPlayerData data = new QuestPlayerData();
        data.trackedQuestId = "hud_theme_quest";
        data.trackedStepId = "collect_supplies";
        PlayerQuestState quest = new PlayerQuestState();
        quest.questId = data.trackedQuestId;
        TaskProgress task = new TaskProgress();
        task.stepId = data.trackedStepId;
        task.title = "收集物资";
        for (int i = 0; i < 3; i++) {
            TaskObjectiveProgress objective = new TaskObjectiveProgress();
            objective.objectiveId = "item_" + i;
            objective.hint = Component.literal("交付物品 " + (i + 1));
            objective.currentAmount = i;
            objective.requiredAmount = 4;
            task.objectives.add(objective);
        }
        quest.taskProgresses.add(task);
        data.quests.add(quest);
        return data;
    }
}
