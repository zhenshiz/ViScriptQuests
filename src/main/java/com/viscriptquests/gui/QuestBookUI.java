package com.viscriptquests.gui;

import com.lowdragmc.lowdraglib2.gui.texture.ColorRectTexture;
import com.lowdragmc.lowdraglib2.gui.texture.SpriteTexture;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.gui.ui.elements.ScrollerView;
import com.viscriptquests.ViScriptQuests;
import org.appliedenergistics.yoga.*;

// 任务书 UI
public class QuestBookUI extends UIElement {
    private static final SpriteTexture BOOK = SpriteTexture.of(ViScriptQuests.formattedMod("textures/gui/book.png"));
    private static final SpriteTexture ARROW = SpriteTexture.of(ViScriptQuests.formattedMod("textures/gui/arrow.png")); // 建议这个箭头图包含左右两个方向，或者旋转使用
    private static final SpriteTexture DEFAULT_BOOKMARK = SpriteTexture.of(ViScriptQuests.formattedMod("textures/gui/default_bookmark.png"));
    private static final SpriteTexture SELECTED_BOOKMARK = SpriteTexture.of(ViScriptQuests.formattedMod("textures/gui/selected_bookmark.png"));
    private static final SpriteTexture SCROLL_BACKGROUND = SpriteTexture.of(ViScriptQuests.formattedMod("textures/gui/scroll_background.png"));
    private static final SpriteTexture SCROLL_DEFAULT_BUTTON = SpriteTexture.of(ViScriptQuests.formattedMod("textures/gui/scroll_default_button.png"));
    private static final SpriteTexture SCROLL_CLICK_BUTTON = SpriteTexture.of(ViScriptQuests.formattedMod("textures/gui/scroll_click_button.png"));

    public ScrollerView questsList = new ScrollerView();
    private UIElement tabGroup;

    //data
    

    public QuestBookUI() {
        //发包获取任务列表的信息
        

        this.layout(layout -> {
            layout.setWidthPercent(100);
            layout.setHeightPercent(100);
            layout.setJustifyContent(YogaJustify.CENTER);
            layout.setAlignItems(YogaAlign.CENTER);
            layout.setFlexDirection(YogaFlexDirection.ROW);
        });

        tabGroup = new UIElement();
        tabGroup.layout(layout -> {
            layout.setWidth(30);
            layout.setHeightPercent(60);
            layout.setFlexDirection(YogaFlexDirection.COLUMN);
            layout.setJustifyContent(YogaJustify.FLEX_START);
            layout.setMargin(YogaEdge.RIGHT, -5);
            layout.setMargin(YogaEdge.TOP, 20);
        });

        addTab(true);
        addTab(false);
        addTab(false);

        this.addChild(tabGroup);

        UIElement bookBody = new UIElement();
        bookBody.layout(layout -> {
            layout.setWidthPercent(80);
            layout.setHeightPercent(80);
            layout.setFlexDirection(YogaFlexDirection.ROW);
        }).style(style -> {
            style.backgroundTexture(BOOK);
        });

        UIElement leftPage = new UIElement();
        leftPage.layout(layout -> {
            layout.setWidthPercent(50);
            layout.setHeightPercent(100);
            layout.setPadding(YogaEdge.ALL, 15);
            layout.setFlexDirection(YogaFlexDirection.COLUMN);
        });


        UIElement headerBar = new UIElement();
        headerBar.layout(layout -> {
            layout.setWidthPercent(100);
            layout.setHeight(20);
            layout.setFlexDirection(YogaFlexDirection.ROW);
            layout.setJustifyContent(YogaJustify.SPACE_BETWEEN);
            layout.setAlignItems(YogaAlign.CENTER);
            layout.setMargin(YogaEdge.BOTTOM, 5);
        });
        // 左箭头
        headerBar.addChild(new UIElement().layout(l -> {
            l.setWidth(10);
            l.setHeight(10);
        }).style(style -> {
            style.backgroundTexture(ARROW);
        }));
        // 标题文字
        headerBar.addChild(new Label().setText("Main Quests").textStyle(s -> {
            s.textColor(0x333333);
        }));
        // 右箭头
        headerBar.addChild(new UIElement().layout(l -> {
            l.setWidth(10);
            l.setHeight(10);
        }).style(style -> {
            style.backgroundTexture(ARROW);
        }));

        leftPage.addChild(headerBar);

        // 2. 任务列表 (ScrollerView)
        setupQuestList();
        leftPage.addChild(questsList);

        // --- B2. 右页 (Right Page) ---
        UIElement rightPage = new UIElement();
        rightPage.layout(layout -> {
            layout.setWidthPercent(50); // 右半边
            layout.setHeightPercent(100);
            layout.setPadding(YogaEdge.ALL, 15);
            layout.setPadding(YogaEdge.LEFT, 5); // 中缝处稍微少一点
            layout.setFlexDirection(YogaFlexDirection.COLUMN);
        });

        // 构建右页详情内容
        buildRightPageDetails(rightPage);

        // 将左右页加入书本
        bookBody.addChild(leftPage);
        bookBody.addChild(rightPage);

        // 将书本加入主容器
        this.addChild(bookBody);
    }

    // --- 辅助方法：设置任务列表 ---
    private void setupQuestList() {
        questsList.verticalScroller(scroller -> {
            scroller.style(style -> {
                style.backgroundTexture(SCROLL_BACKGROUND);
            });
        }).layout(layout -> {
            layout.setFlexGrow(1);
            layout.setWidthPercent(100);
        });
        questsList.viewContainer.layout(layout -> {
            layout.setFlexDirection(YogaFlexDirection.COLUMN);
        });

        // 添加一些测试任务条目
        for (int i = 0; i < 10; i++) {
            questsList.addChild(createQuestItem("Quest Name " + i));
        }
    }

    // --- 辅助方法：创建单个任务条目 ---
    private UIElement createQuestItem(String title) {
        UIElement item = new UIElement();
        item.layout(layout -> {
            layout.setWidthPercent(100);
            layout.setHeight(15); // 条目高度
            layout.setMargin(YogaEdge.BOTTOM, 2);
            layout.setFlexDirection(YogaFlexDirection.ROW);
            layout.setAlignItems(YogaAlign.CENTER);
            layout.setPadding(YogaEdge.HORIZONTAL, 2);
        });

        item.addChild(new Label().setText(title).textStyle(s -> s.textColor(0x555555)));
        return item;
    }

    private void addTab(boolean isSelected) {
        UIElement tab = new UIElement();
        tab.layout(layout -> {
            layout.setWidth(28);
            layout.setHeight(25);
            layout.setMargin(YogaEdge.BOTTOM, 2);
        }).style(style -> {
            style.backgroundTexture(isSelected ? SELECTED_BOOKMARK : DEFAULT_BOOKMARK);
        });

        // 这里可以addChild添加具体的图标（剑、镐子等）

        tabGroup.addChild(tab);
    }

    // --- 辅助方法：构建右页详情 ---
    private void buildRightPageDetails(UIElement parent) {
        // 1. 大标题
        Label title = (Label) new Label().setText("Secrets of the Deep");
        title.textStyle(s -> s.textColor(0x000000));
        title.layout(l -> l.setMargin(YogaEdge.BOTTOM, 10).setAlignSelf(YogaAlign.CENTER));
        parent.addChild(title);

        // 2. Objectives 区域
        parent.addChild(new Label().setText("Objectives:").textStyle(s -> s.textColor(0x333333)));
        parent.addChild(new Label().setText("[x] Find an Ancient City").textStyle(s -> s.textColor(0x00aa00)));
        parent.addChild(new Label().setText("[ ] Activate a Sculk Shrieker").textStyle(s -> s.textColor(0x555555)));

        // 3. 分割线 (用一个高度为1的 UIElement 模拟)
        UIElement divider = new UIElement();
        divider.layout(l -> {
                    l.setWidthPercent(100);
                    l.setHeight(1);
                    l.setMargin(YogaEdge.VERTICAL, 8);
                })
                .style(s -> s.backgroundTexture(new ColorRectTexture(0x44555555))); // 虚线或灰色线
        parent.addChild(divider);

        // 4. Description 区域
        parent.addChild(new Label().setText("Description:").textStyle(s -> s.textColor(0x333333)));
        Label desc = (Label) new Label().setText("Rumors speak of a lost city buried deep underground...");
        desc.layout(l -> l.setWidthPercent(100)); // 允许换行
        parent.addChild(desc);

        parent.addChild(divider.copy()); // 再次添加分割线

        // 5. Rewards 区域
        parent.addChild(new Label().setText("Rewards:").textStyle(s -> s.textColor(0x333333)));
        UIElement rewardRow = new UIElement();
        rewardRow.layout(l -> {
            l.setFlexDirection(YogaFlexDirection.ROW);
            l.setMargin(YogaEdge.TOP, 5);
            l.setGap(YogaGutter.ALL, 5);
        });

        // 添加模拟的奖励槽位
        rewardRow.addChild(createRewardSlot());
        rewardRow.addChild(createRewardSlot());
        rewardRow.addChild(createRewardSlot());

        parent.addChild(rewardRow);
    }

    // 模拟奖励槽位
    private UIElement createRewardSlot() {
        UIElement slot = new UIElement();
        slot.layout(l -> {
                    l.setWidth(18);
                    l.setHeight(18);
                })
                .style(s -> s.backgroundTexture(new ColorRectTexture(0x22888888))); // 灰色背景模拟槽位
        return slot;
    }
}