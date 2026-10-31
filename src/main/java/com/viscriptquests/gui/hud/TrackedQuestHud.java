package com.viscriptquests.gui.hud;

import com.lowdragmc.lowdraglib2.gui.texture.ColorRectTexture;
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.texture.SDFRectTexture;
import com.lowdragmc.lowdraglib2.gui.texture.SpriteTexture;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.data.Horizontal;
import com.lowdragmc.lowdraglib2.gui.ui.data.TextWrap;
import com.lowdragmc.lowdraglib2.gui.ui.data.Vertical;
import com.lowdragmc.lowdraglib2.gui.ui.elements.ItemSlot;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.viscriptquests.ViScriptQuests;
import com.viscriptquests.config.ClientConfig;
import com.viscriptquests.config.QuestHudTheme;
import com.viscriptquests.quest.data.DisplayIcon;
import com.viscriptquests.quest.data.runtime.TaskObjectiveProgress;
import dev.vfyjxf.taffy.style.AlignItems;
import dev.vfyjxf.taffy.style.FlexDirection;
import dev.vfyjxf.taffy.style.TaffyPosition;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
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
    private static final IGuiTexture CHARCOAL_BACKGROUND = SDFRectTexture.of(0xE51B1C1F)
            .setBorderColor(0xA0787060).setStroke(0.5f).setRadius(1);

    private final UIElement panel = new UIElement();
    private final UIElement card = new UIElement();
    private final UIElement divider = new UIElement();
    private final UIElement guideMarkerLayer = new UIElement();
    private final QuestCompletionToastHud completionToastHud = new QuestCompletionToastHud();
    private final List<QuestGuideMarkerElement> guideMarkers = new ArrayList<>();
    private final Label taskTitle = label(FONT_TITLE, TEXT_TITLE, false);
    private final UIElement objectivesColumn = new UIElement();
    private float lastX = Float.NaN;
    private float lastY = Float.NaN;
    private float lastWidth = Float.NaN;
    private float lastHeight = Float.NaN;
    private String lastContentKey = "";
    private QuestHudTheme lastTheme;

    public TrackedQuestHud() {
        layout(layout -> {
            layout.widthPercent(100);
            layout.heightPercent(100);
        });
        addChildren(panel, guideMarkerLayer, completionToastHud);
        panel.style(style -> style.backgroundTexture(IGuiTexture.EMPTY));
        panel.layout(layout -> {
            layout.positionType(TaffyPosition.ABSOLUTE);
            layout.flexDirection(FlexDirection.COLUMN);
            layout.paddingAllPercent(0);
        });
        card.setAllowHitTest(false);
        card.setId("tracked_quest_hud_card");
        card.layout(layout -> layout.widthPercent(100).flexDirection(FlexDirection.COLUMN));
        divider.setAllowHitTest(false);
        divider.style(style -> style.backgroundTexture(new ColorRectTexture(0xA0927B51)));
        divider.layout(layout -> layout.widthPercent(100).height(1));
        guideMarkerLayer.setAllowHitTest(false);
        guideMarkerLayer.layout(layout -> {
            layout.positionType(TaffyPosition.ABSOLUTE);
            layout.leftPercent(0);
            layout.topPercent(0);
            layout.widthPercent(100);
            layout.heightPercent(100);
        });

        taskTitle.layout(layout -> {
            layout.widthPercent(100);
            layout.height(TITLE_HEIGHT);
        });
        taskTitle.setId("tracked_quest_hud_title");

        objectivesColumn.layout(layout -> {
            layout.widthPercent(100);
            layout.flexDirection(FlexDirection.COLUMN);
            layout.gapAll(2);
        });
        objectivesColumn.setId("tracked_quest_hud_objectives");
        card.addChildren(taskTitle, objectivesColumn);
        panel.addChild(card);
    }

    @Override
    public void screenTick() {
        super.screenTick();
        updateLayoutFromConfig();
        QuestHudData.ComponentState snapshot = QuestHudData.snapshot();
        QuestGuideMarkerClientBridge.update(snapshot);
        updateContent(snapshot);
        updateGuideMarkers(snapshot);
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
        QuestHudTheme theme = ClientConfig.QUEST_HUD_THEME.get();
        if (theme != lastTheme) {
            applyTheme(theme);
            lastTheme = theme;
        }
        String contentKey = theme + "|" + snapshot.quest().questId + "|"
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
        int visibleRows = 0;
        if (snapshot.task().objectives.isEmpty()) {
            objectivesColumn.addChild(createObjectiveRow(snapshot.task().displayIcon,
                    snapshot.task().displayTaskHint(), null, TEXT_MAIN, theme));
            updateCardHeight(theme, 1);
            return;
        }
        for (TaskObjectiveProgress objective : snapshot.task().objectives) {
            if (objective == null || !objective.isVisibleInHud()) {
                continue;
            }
            objectivesColumn.addChild(createObjectiveRow(objective.displayIcon,
                    theme == QuestHudTheme.CHARCOAL ? objective.displayHint() : objective.progressHint(),
                    theme == QuestHudTheme.CHARCOAL ? displayProgress(objective) : null,
                    objective.displayTextColor(), theme));
            visibleRows++;
        }
        updateCardHeight(theme, visibleRows);
    }

    private void applyTheme(QuestHudTheme theme) {
        boolean charcoal = theme == QuestHudTheme.CHARCOAL;
        card.style(style -> style.backgroundTexture(charcoal ? CHARCOAL_BACKGROUND : IGuiTexture.EMPTY));
        taskTitle.textStyle(style -> style.textColor(charcoal ? 0xFFE5C272 : TEXT_TITLE));
        card.layout(layout -> {
            layout.paddingAll(charcoal ? 4 : 0);
            layout.gapAll(charcoal ? 3 : 4);
        });
        objectivesColumn.layout(layout -> layout.gapAll(charcoal ? 1 : 2));
        card.clearAllChildren();
        card.addChild(taskTitle);
        if (charcoal) card.addChild(divider);
        card.addChild(objectivesColumn);
    }

    private void updateCardHeight(QuestHudTheme theme, int rows) {
        boolean charcoal = theme == QuestHudTheme.CHARCOAL;
        int rowHeight = charcoal ? 16 : OBJECTIVE_ROW_HEIGHT;
        int rowGap = charcoal ? 1 : 2;
        int height = (charcoal ? 8 + TITLE_HEIGHT + 1 + 6 : TITLE_HEIGHT + 4)
                + rows * rowHeight + Math.max(0, rows - 1) * rowGap;
        card.layout(layout -> layout.height(height));
    }

    private static Component displayProgress(TaskObjectiveProgress objective) {
        String progress = objective.progressText();
        if (progress.startsWith("(") && progress.endsWith(")")) {
            progress = progress.substring(1, progress.length() - 1);
        }
        return Component.literal(progress);
    }

    private void updateGuideMarkers(QuestHudData.ComponentState snapshot) {
        List<QuestHudData.MarkerState> markers = snapshot.guideMarkers();
        while (guideMarkers.size() < markers.size()) {
            QuestGuideMarkerElement element = new QuestGuideMarkerElement();
            guideMarkers.add(element);
            guideMarkerLayer.addChild(element);
        }
        for (int i = 0; i < guideMarkers.size(); i++) {
            QuestGuideMarkerElement element = guideMarkers.get(i);
            if (i >= markers.size()) {
                element.setVisible(false);
                continue;
            }
            QuestHudData.MarkerState marker = markers.get(i);
            element.update(marker.marker(), marker.fallbackLabel());
        }
    }

    private static UIElement createObjectiveRow(DisplayIcon icon, Component hint, Component progress,
                                                int textColor, QuestHudTheme theme) {
        boolean charcoal = theme == QuestHudTheme.CHARCOAL;
        UIElement row = new UIElement();
        row.addClass("tracked-quest-hud-row");
        row.layout(layout -> {
            layout.widthPercent(100);
            layout.height(charcoal ? 16 : OBJECTIVE_ROW_HEIGHT);
            layout.flexDirection(FlexDirection.ROW);
            layout.alignItems(AlignItems.CENTER);
            layout.gapAll(2);
            if (charcoal) layout.paddingHorizontal(2);
        });
        if (charcoal) row.style(style -> style.backgroundTexture(new ColorRectTexture(0x49373330)));

        Label text = label(FONT_BODY, textColor, !charcoal);
        text.setText(hint == null ? Component.empty() : hint);
        text.layout(layout -> {
            layout.width(0);
            layout.flex(1);
            layout.height(OBJECTIVE_ROW_HEIGHT);
        });

        row.addChildren(createHudIcon(icon), text);
        if (progress != null) {
            Label count = new Label();
            count.addClass("tracked-quest-hud-progress");
            count.setAllowHitTest(false);
            count.setText(progress);
            count.layout(layout -> layout.minWidth(28).height(16));
            count.textStyle(style -> style.fontSize(7.5f)
                    .textColor(0xFFBCB8AC)
                    .textShadow(true)
                    .textWrap(TextWrap.HIDE)
                    .adaptiveWidth(true)
                    .textAlignHorizontal(Horizontal.RIGHT)
                    .textAlignVertical(Vertical.CENTER));
            row.addChild(count);
        }
        return row;
    }

    private static UIElement createHudIcon(DisplayIcon icon) {
        UIElement element;
        if (icon != null && icon.isTexture() && icon.getTexture() != null && !icon.getTexture().isBlank()) {
            ResourceLocation location = ResourceLocation.tryParse(icon.getTexture());
            element = textureIcon(location == null ? ViScriptQuests.id("textures/gui/icon/icon_task.png") : location);
        } else {
            ItemStack stack = icon == null ? ItemStack.EMPTY : icon.renderItemStack();
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
        ItemStack stack = icon.renderItemStack();
        return stack == null || stack.isEmpty() ? "item:" : "item:" + stack.getItem() + "x" + stack.getCount();
    }

    private static String objectivesKey(QuestHudData.ComponentState snapshot) {
        if (snapshot.task().objectives.isEmpty()) {
            return snapshot.task().displayTaskHint().getString() + "|" + iconKey(snapshot.task().displayIcon);
        }
        StringBuilder key = new StringBuilder();
        for (TaskObjectiveProgress objective : snapshot.task().objectives) {
            if (objective == null) {
                key.append("null;");
                continue;
            }
            key.append(objective.isVisibleInHud() ? "visible|" : "hidden|")
                    .append(objective.status).append(';');
            if (!objective.isVisibleInHud()) {
                continue;
            }
            key.append(objective.displayHint().getString())
                    .append('|').append(objective.objectiveType)
                    .append('|').append(objective.currentAmount)
                    .append('/').append(objective.requiredAmount)
                    .append('|').append(objective.progressTextOverride)
                    .append('|').append(objective.status)
                    .append('|').append(objective.ponderViewAction)
                    .append('|').append(objective.ponderComponentId)
                    .append('|').append(iconKey(objective.displayIcon))
                    .append(';');
        }
        return key.toString();
    }

    private static Label label(float fontSize, int color, boolean wrap) {
        Label label = new Label();
        label.layout(layout -> layout.widthPercent(100));
        label.textStyle(style -> {
            style.fontSize(fontSize)
                    .textColor(color)
                    .textShadow(true)
                    .textAlignHorizontal(Horizontal.LEFT)
                    .textAlignVertical(Vertical.CENTER)
                    .textWrap(wrap ? TextWrap.WRAP : TextWrap.HIDE);
            if (wrap) {
                style.adaptiveHeight(true);
            }
        });
        return label;
    }
}
