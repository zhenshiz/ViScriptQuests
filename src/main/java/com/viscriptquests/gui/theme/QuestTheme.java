package com.viscriptquests.gui.theme;

import com.lowdragmc.lowdraglib2.gui.texture.ColorRectTexture;
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.texture.SDFRectTexture;
import com.viscriptquests.config.ClientConfig;
import com.viscriptquests.config.QuestUiTheme;

/**
 * 任务书界面的客户端视觉主题。
 *
 * <p>旧 HUD 颜色字段保留供兼容使用；追踪 HUD 与完成提示读取独立的 HUD 主题配置。
 * 主题不保存任务数据，也不参与任务状态判断。
 */
public record QuestTheme(
        QuestUiTheme type,
        int primaryText,
        int secondaryText,
        int mutedText,
        int accentText,
        int dangerText,
        int rootOverlay,
        IGuiTexture shellBackground,
        IGuiTexture headerBackground,
        IGuiTexture panelBackground,
        IGuiTexture cardBackground,
        IGuiTexture selectedCardBackground,
        IGuiTexture sectionBackground,
        IGuiTexture buttonBase,
        IGuiTexture buttonHover,
        IGuiTexture buttonPressed,
        IGuiTexture hudBackground,
        int hudText,
        int hudAccent
) {
    private static final QuestTheme CLASSIC = classicBook();
    private static final QuestTheme GLASS = glassDark();

    /** 返回当前客户端配置选择的主题。 */
    public static QuestTheme current() {
        return switch (ClientConfig.QUEST_UI_THEME.get()) {
            case GLASS_DARK -> GLASS;
            case CLASSIC_BOOK -> CLASSIC;
        };
    }

    public boolean isClassicBook() {
        return type == QuestUiTheme.CLASSIC_BOOK;
    }

    private static QuestTheme classicBook() {
        return new QuestTheme(
                QuestUiTheme.CLASSIC_BOOK,
                0xFF61382F,
                0xFF8A6657,
                0xFF8A6657,
                0xFFFFD85A,
                0xFF9A352E,
                0x70000000,
                IGuiTexture.EMPTY,
                IGuiTexture.EMPTY,
                IGuiTexture.EMPTY,
                IGuiTexture.EMPTY,
                IGuiTexture.EMPTY,
                IGuiTexture.EMPTY,
                IGuiTexture.EMPTY,
                IGuiTexture.EMPTY,
                IGuiTexture.EMPTY,
                IGuiTexture.EMPTY,
                0xFFF1F1F1,
                0xFFFFD84B
        );
    }

    private static QuestTheme glassDark() {
        int border = 0x70D6E5F2;
        return new QuestTheme(
                QuestUiTheme.GLASS_DARK,
                0xFFF1F1F1,
                0xFFD0D8DF,
                0xFF9DAAB5,
                0xFFE4BE68,
                0xFFFF8E8E,
                0x56000000,
                new ColorRectTexture(0xB01B2431),
                rounded(0x502C3746, 0x407B8996, 0.5f, 0),
                rounded(0x50111922, 0x307B8996, 0.5f, 1),
                rounded(0x60323D4C, 0x407B8996, 0.5f, 0),
                rounded(0x90425162, 0xA0E4BE68, 0.5f, 0),
                rounded(0x6E17212B, border, 1, 4),
                rounded(0xA83B4652, border, 1, 3),
                rounded(0xC0506270, 0x90EAF7FF, 1, 3),
                rounded(0xB02A333E, border, 1, 3),
                rounded(0xA0202934, border, 1, 4),
                0xFFF1F1F1,
                0xFFE4BE68
        );
    }

    private static SDFRectTexture rounded(int fill, int border, float stroke, float radius) {
        return SDFRectTexture.of(fill)
                .setBorderColor(border)
                .setStroke(stroke)
                .setRadius(radius);
    }

    /** 创建主题共用的细分隔线。 */
    public IGuiTexture divider() {
        return isClassicBook()
                ? new ColorRectTexture(0xFFB9956C)
                : new ColorRectTexture(0x607E8D9C);
    }

    /**
     * 返回深色任务书中选中任务的浅色底板。
     *
     * @return 带金色边框的浅色任务底板
     */
    public IGuiTexture glassSelectedTask() {
        return rounded(0xE6E8E3D7, 0xFFD4BF89, 0.5f, 0);
    }

}
