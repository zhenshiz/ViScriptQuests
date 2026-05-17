package com.viscriptquests.gui.hud;

import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.texture.SpriteTexture;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.data.Horizontal;
import com.lowdragmc.lowdraglib2.gui.ui.data.TextWrap;
import com.lowdragmc.lowdraglib2.gui.ui.data.Vertical;
import com.lowdragmc.lowdraglib2.gui.ui.elements.ItemSlot;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.viscriptquests.ViScriptQuests;
import com.viscriptquests.config.ClientConfig;
import com.viscriptquests.quest.data.DisplayIcon;
import com.viscriptquests.quest.data.runtime.TaskObjectiveProgress;
import dev.vfyjxf.taffy.style.AlignItems;
import dev.vfyjxf.taffy.style.FlexDirection;
import dev.vfyjxf.taffy.style.TaffyPosition;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.Arrays;
import java.util.List;

// 当前追踪小任务 HUD。根元素全屏，面板位置和尺寸全部读取客户端百分比配置。
public class TrackedQuestHud extends UIElement {
    private static final ItemStack DEFAULT_ICON = new ItemStack(Items.PAPER);
    private static final int TEXT_TITLE = 0xFFFFD84B;
    private static final int TEXT_MAIN = 0xFFFFFFFF;
    private static final int ICON_SIZE = 13;
    private static final int TITLE_HEIGHT = 13;
    private static final int OBJECTIVE_ROW_HEIGHT = 15;
    private static final float FONT_TITLE = 8.5f;
    private static final float FONT_BODY = 8.0f;

    private final UIElement panel = new UIElement();
    private final QuestGuideMarkerElement guideMarker = new QuestGuideMarkerElement();
    private final Label taskTitle = label(FONT_TITLE, TEXT_TITLE, false);
    private final UIElement objectivesColumn = new UIElement();
    private float lastX = Float.NaN;
    private float lastY = Float.NaN;
    private float lastWidth = Float.NaN;
    private float lastHeight = Float.NaN;
    private String lastContentKey = "";

    public TrackedQuestHud() {
        layout(layout -> {
            layout.widthPercent(100);
            layout.heightPercent(100);
        });
        addChildren(panel, guideMarker);
        panel.style(style -> style.backgroundTexture(IGuiTexture.EMPTY));
        panel.layout(layout -> {
            layout.positionType(TaffyPosition.ABSOLUTE);
            layout.flexDirection(FlexDirection.COLUMN);
            layout.paddingAllPercent(0);
            layout.gapAll(4);
        });

        taskTitle.layout(layout -> {
            layout.widthPercent(100);
            layout.height(TITLE_HEIGHT);
        });

        objectivesColumn.layout(layout -> {
            layout.widthPercent(100);
            layout.flexDirection(FlexDirection.COLUMN);
            layout.gapAll(2);
        });
        panel.addChildren(taskTitle, objectivesColumn);
    }

    @Override
    public void screenTick() {
        super.screenTick();
        updateLayoutFromConfig();
        QuestHudData.ComponentState snapshot = QuestHudData.snapshot();
        updateContent(snapshot);
        guideMarker.update(snapshot);
    }

    private void updateLayoutFromConfig() {
        float x = ClientConfig.TRACKED_QUEST_HUD_X_PERCENT.get().floatValue();
        float y = ClientConfig.TRACKED_QUEST_HUD_Y_PERCENT.get().floatValue();
        float width = ClientConfig.TRACKED_QUEST_HUD_WIDTH_PERCENT.get().floatValue();
        float height = ClientConfig.TRACKED_QUEST_HUD_HEIGHT_PERCENT.get().floatValue();
        if (x == lastX && y == lastY && width == lastWidth && height == lastHeight) {
            return;
        }
        lastX = x;
        lastY = y;
        lastWidth = width;
        lastHeight = height;
        panel.layout(layout -> {
            layout.leftPercent(x);
            layout.topPercent(y);
            layout.widthPercent(width);
            layout.heightPercent(height);
        });
    }

    private void updateContent(QuestHudData.ComponentState snapshot) {
        boolean visible = ClientConfig.SHOW_TRACKED_QUEST_HUD.get() && !snapshot.isEmpty();
        panel.setVisible(visible);
        if (!visible) {
            lastContentKey = "";
            return;
        }
        String contentKey = snapshot.quest().questId + "|"
                + snapshot.task().stepId + "|"
                + snapshot.task().title + "|"
                + snapshot.task().status + "|"
                + objectivesKey(snapshot);
        if (contentKey.equals(lastContentKey)) {
            return;
        }
        lastContentKey = contentKey;

        taskTitle.setText(Component.literal(snapshot.task().title));
        objectivesColumn.clearAllChildren();
        if (snapshot.task().objectives.isEmpty()) {
            for (String line : fallbackObjectiveHints(snapshot.task().taskHint)) {
                objectivesColumn.addChild(createObjectiveRow(snapshot.task().displayIcon, line));
            }
            return;
        }
        for (TaskObjectiveProgress objective : snapshot.task().objectives) {
            objectivesColumn.addChild(createObjectiveRow(objective.displayIcon, objective.progressText() + objective.hint));
        }
    }

    private static UIElement createObjectiveRow(DisplayIcon icon, String hint) {
        UIElement row = new UIElement();
        row.layout(layout -> {
            layout.widthPercent(100);
            layout.height(OBJECTIVE_ROW_HEIGHT);
            layout.flexDirection(FlexDirection.ROW);
            layout.alignItems(AlignItems.CENTER);
            layout.gapAll(2);
        });

        Label text = label(FONT_BODY, TEXT_MAIN, true);
        text.setText(Component.literal(hint == null ? "" : hint));
        text.layout(layout -> {
            layout.width(0);
            layout.flex(1);
            layout.height(OBJECTIVE_ROW_HEIGHT);
        });

        row.addChildren(createHudIcon(icon), text);
        return row;
    }

    private static UIElement createHudIcon(DisplayIcon icon) {
        UIElement element;
        if (icon != null && icon.isTexture() && icon.getTexture() != null && !icon.getTexture().isBlank()) {
            ResourceLocation location = ResourceLocation.tryParse(icon.getTexture());
            element = textureIcon(location == null ? ViScriptQuests.id("textures/gui/quest_book/book_icon.png") : location);
        } else {
            ItemStack stack = icon == null ? ItemStack.EMPTY : icon.getItemStack();
            element = itemIcon(stack == null || stack.isEmpty() ? DEFAULT_ICON : stack);
        }
        element.layout(layout -> {
            layout.width(ICON_SIZE);
            layout.height(ICON_SIZE);
        });
        return element;
    }

    private static UIElement textureIcon(ResourceLocation location) {
        UIElement icon = new UIElement();
        icon.style(style -> style.backgroundTexture(SpriteTexture.of(location)));
        return icon;
    }

    private static ItemSlot itemIcon(ItemStack stack) {
        ItemSlot slot = new ItemSlot();
        slot.setItem(stack == null ? ItemStack.EMPTY : stack);
        slot.style(style -> style.backgroundTexture(IGuiTexture.EMPTY));
        slot.slotStyle(style -> style
                .slotOverlay(IGuiTexture.EMPTY)
                .hoverOverlay(IGuiTexture.EMPTY)
                .showItemTooltips(false));
        return slot;
    }

    private static String iconKey(DisplayIcon icon) {
        if (icon == null) {
            return "";
        }
        if (icon.isTexture()) {
            return "texture:" + icon.getTexture();
        }
        ItemStack stack = icon.getItemStack();
        return stack == null || stack.isEmpty() ? "item:" : "item:" + stack.getItem() + "x" + stack.getCount();
    }

    private static String objectivesKey(QuestHudData.ComponentState snapshot) {
        if (snapshot.task().objectives.isEmpty()) {
            return snapshot.task().taskHint + "|" + iconKey(snapshot.task().displayIcon);
        }
        StringBuilder key = new StringBuilder();
        for (TaskObjectiveProgress objective : snapshot.task().objectives) {
            key.append(objective.hint)
                    .append('|').append(objective.currentAmount)
                    .append('/').append(objective.requiredAmount)
                    .append('|').append(objective.completed)
                    .append('|').append(iconKey(objective.displayIcon))
                    .append(';');
        }
        return key.toString();
    }

    private static List<String> fallbackObjectiveHints(String taskHint) {
        if (taskHint == null || taskHint.isBlank()) {
            return List.of("");
        }
        return Arrays.stream(taskHint.split("\\R"))
                .map(String::trim)
                .filter(line -> !line.isEmpty())
                .toList();
    }

    private static Label label(float fontSize, int color, boolean wrap) {
        Label label = new Label();
        label.layout(layout -> {
            layout.widthPercent(100);
            layout.minHeightPercent(14);
        });
        label.textStyle(style -> {
            style.fontSize(fontSize)
                    .textColor(color)
                    .textShadow(true)
                    .textAlignHorizontal(Horizontal.LEFT)
                    .textAlignVertical(Vertical.CENTER);
            if (wrap) {
                style.textWrap(TextWrap.WRAP)
                        .adaptiveHeight(true);
            }
        });
        return label;
    }
}
