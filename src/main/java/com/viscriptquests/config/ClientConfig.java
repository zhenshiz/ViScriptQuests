package com.viscriptquests.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public class ClientConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue SHOW_COMPLETED_QUESTS_IN_BOOK;
    public static final ModConfigSpec.BooleanValue SHOW_COMPLETED_TASKS_IN_BOOK;
    public static final ModConfigSpec.BooleanValue SHOW_TRACKED_QUEST_HUD;
    public static final ModConfigSpec.DoubleValue TRACKED_QUEST_HUD_X_PERCENT;
    public static final ModConfigSpec.DoubleValue TRACKED_QUEST_HUD_Y_PERCENT;
    public static final ModConfigSpec.DoubleValue TRACKED_QUEST_HUD_WIDTH_PERCENT;
    public static final ModConfigSpec.DoubleValue TRACKED_QUEST_HUD_HEIGHT_PERCENT;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        builder.push("questBook");

        // 是否在任务书中显示已经完成的大任务章节。
        SHOW_COMPLETED_QUESTS_IN_BOOK = builder
                .translation("viscript_quests.configuration.quest_book.show_completed_quests")
                .define("showCompletedQuests", false);

        // 是否在任务书中显示已经完成或跳过的小任务。
        SHOW_COMPLETED_TASKS_IN_BOOK = builder
                .translation("viscript_quests.configuration.quest_book.show_completed_tasks")
                .define("showCompletedTasks", false);

        builder.pop();

        builder.push("trackedQuestHud");

        // 是否显示当前追踪小任务的 HUD。
        SHOW_TRACKED_QUEST_HUD = builder
                .translation("viscript_quests.configuration.tracked_quest_hud.show")
                .define("showTrackedQuestHud", true);

        // HUD 左上角横向位置，占屏幕宽度百分比。
        TRACKED_QUEST_HUD_X_PERCENT = builder
                .translation("viscript_quests.configuration.tracked_quest_hud.x_percent")
                .defineInRange("xPercent", 74.0, 0.0, 100.0);

        // HUD 左上角纵向位置，占屏幕高度百分比。
        TRACKED_QUEST_HUD_Y_PERCENT = builder
                .translation("viscript_quests.configuration.tracked_quest_hud.y_percent")
                .defineInRange("yPercent", 22.0, 0.0, 100.0);

        // HUD 宽度，占屏幕宽度百分比。
        TRACKED_QUEST_HUD_WIDTH_PERCENT = builder
                .translation("viscript_quests.configuration.tracked_quest_hud.width_percent")
                .defineInRange("widthPercent", 24.0, 5.0, 100.0);

        // HUD 高度，占屏幕高度百分比。
        TRACKED_QUEST_HUD_HEIGHT_PERCENT = builder
                .translation("viscript_quests.configuration.tracked_quest_hud.height_percent")
                .defineInRange("heightPercent", 13.0, 4.0, 100.0);

        builder.pop();
        SPEC = builder.build();
    }
}
