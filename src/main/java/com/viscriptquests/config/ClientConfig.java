package com.viscriptquests.config;

import net.minecraftforge.common.ForgeConfigSpec;

public class ClientConfig {
    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.EnumValue<QuestUiTheme> QUEST_UI_THEME;
    public static final ForgeConfigSpec.BooleanValue ENABLE_QUEST_UI_ANIMATIONS;
    public static final ForgeConfigSpec.BooleanValue SHOW_QUEST_COMPLETION_TOAST;
    public static final ForgeConfigSpec.DoubleValue QUEST_COMPLETION_TOAST_X_PERCENT;
    public static final ForgeConfigSpec.DoubleValue QUEST_COMPLETION_TOAST_Y_PERCENT;
    public static final ForgeConfigSpec.BooleanValue SHOW_TRACKED_QUEST_HUD;
    public static final ForgeConfigSpec.DoubleValue TRACKED_QUEST_HUD_X_PERCENT;
    public static final ForgeConfigSpec.DoubleValue TRACKED_QUEST_HUD_Y_PERCENT;
    public static final ForgeConfigSpec.DoubleValue TRACKED_QUEST_HUD_WIDTH_PERCENT;
    public static final ForgeConfigSpec.DoubleValue TRACKED_QUEST_HUD_HEIGHT_PERCENT;
    public static final ForgeConfigSpec.BooleanValue SHOW_QUEST_GUIDE_MARKER;
    public static final ForgeConfigSpec.DoubleValue QUEST_GUIDE_MARKER_EDGE_MARGIN_PERCENT;
    public static final ForgeConfigSpec.DoubleValue QUEST_GUIDE_MARKER_ICON_SIZE_PERCENT;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        builder
                .translation("viscript_quests.configuration.quest_book")
                .push("questBook");

        // 任务书、追踪 HUD 和完成提示共用的客户端主题。
        QUEST_UI_THEME = builder
                .translation("viscript_quests.configuration.quest_book.ui_theme")
                .defineEnum("uiTheme", QuestUiTheme.CLASSIC_BOOK);

        // 是否播放任务界面的短交互动画；关闭后不影响任何任务数据和交互。
        ENABLE_QUEST_UI_ANIMATIONS = builder
                .translation("viscript_quests.configuration.quest_book.enable_animations")
                .define("enableAnimations", true);

        builder.pop();

        builder
                .translation("viscript_quests.configuration.completion_toast")
                .push("completionToast");

        // 是否在大任务或小任务完成时显示屏幕提示。
        SHOW_QUEST_COMPLETION_TOAST = builder
                .translation("viscript_quests.configuration.completion_toast.show")
                .define("showCompletionToast", true);

        // 第一条任务完成提示的左上角横向位置，占屏幕宽度百分比。
        QUEST_COMPLETION_TOAST_X_PERCENT = builder
                .translation("viscript_quests.configuration.completion_toast.x_percent")
                .defineInRange("xPercent", 1.0, 0.0, 100.0);

        // 第一条任务完成提示的左上角纵向位置，占屏幕高度百分比。
        QUEST_COMPLETION_TOAST_Y_PERCENT = builder
                .translation("viscript_quests.configuration.completion_toast.y_percent")
                .defineInRange("yPercent", 8.0, 0.0, 100.0);

        builder.pop();

        builder
                .translation("viscript_quests.configuration.tracked_quest_hud")
                .push("trackedQuestHud");

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

        builder
                .translation("viscript_quests.configuration.quest_guide_marker")
                .push("questGuideMarker");

        // 是否显示当前追踪小任务的导航标记。
        SHOW_QUEST_GUIDE_MARKER = builder
                .translation("viscript_quests.configuration.quest_guide_marker.show")
                .define("showQuestGuideMarker", true);

        // 导航标记吸附到屏幕边缘时保留的边距，占屏幕尺寸百分比。
        QUEST_GUIDE_MARKER_EDGE_MARGIN_PERCENT = builder
                .translation("viscript_quests.configuration.quest_guide_marker.edge_margin_percent")
                .defineInRange("edgeMarginPercent", 3.0, 0.0, 20.0);

        // 导航标记图标大小，占屏幕较短边百分比。
        QUEST_GUIDE_MARKER_ICON_SIZE_PERCENT = builder
                .translation("viscript_quests.configuration.quest_guide_marker.icon_size_percent")
                .defineInRange("iconSizePercent", 2.6, 1.0, 8.0);

        builder.pop();
        SPEC = builder.build();
    }
}
