package com.viscriptquests.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public class ClientConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue SHOW_COMPLETED_QUESTS_IN_BOOK;
    public static final ModConfigSpec.BooleanValue SHOW_COMPLETED_TASKS_IN_BOOK;

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
        SPEC = builder.build();
    }
}
