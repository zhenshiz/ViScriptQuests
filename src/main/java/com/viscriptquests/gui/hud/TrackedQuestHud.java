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
import dev.vfyjxf.taffy.style.AlignItems;
import dev.vfyjxf.taffy.style.FlexDirection;
import dev.vfyjxf.taffy.style.TaffyPosition;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

// 当前追踪小任务 HUD。根元素全屏，面板位置和尺寸全部读取客户端百分比配置。
public class TrackedQuestHud extends UIElement {
    private static final ItemStack DEFAULT_ICON = new ItemStack(Items.PAPER);
    private static final int TEXT_TITLE = 0xFFFFD84B;
    private static final int TEXT_MAIN = 0xFFFFFFFF;
    private static final int ICON_SIZE = 12;
    private static final float FONT_TITLE = 8.5f;
    private static final float FONT_BODY = 8.0f;

    private final UIElement panel = new UIElement();
    private final Label taskTitle = label(FONT_TITLE, TEXT_TITLE, false);
    private final UIElement objectiveRow = new UIElement();
    private final Label taskHint = label(FONT_BODY, TEXT_MAIN, true);
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
        addChild(panel);
        panel.style(style -> style.backgroundTexture(IGuiTexture.EMPTY));
        panel.layout(layout -> {
            layout.positionType(TaffyPosition.ABSOLUTE);
            layout.flexDirection(FlexDirection.COLUMN);
            layout.paddingAllPercent(0);
            layout.gapAllPercent(2);
        });

        objectiveRow.layout(layout -> {
            layout.widthPercent(100);
            layout.minHeightPercent(35);
            layout.flexDirection(FlexDirection.ROW);
            layout.alignItems(AlignItems.CENTER);
            layout.gapAll(4);
        });
        taskHint.layout(layout -> {
            layout.width(0);
            layout.flex(1);
            layout.minHeightPercent(100);
        });
        panel.addChildren(taskTitle, objectiveRow);
    }

    @Override
    public void screenTick() {
        super.screenTick();
        updateLayoutFromConfig();
        updateContent();
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

    private void updateContent() {
        QuestHudData.ComponentState snapshot = QuestHudData.snapshot();
        boolean visible = ClientConfig.SHOW_TRACKED_QUEST_HUD.get() && !snapshot.isEmpty();
        panel.setVisible(visible);
        if (!visible) {
            lastContentKey = "";
            return;
        }
        String contentKey = snapshot.quest().questId + "|"
                + snapshot.task().stepId + "|"
                + snapshot.task().title + "|"
                + snapshot.task().taskHint + "|"
                + snapshot.task().status + "|"
                + iconKey(snapshot.task().hudIcon);
        if (contentKey.equals(lastContentKey)) {
            return;
        }
        lastContentKey = contentKey;

        taskTitle.setText(Component.literal(snapshot.task().title));
        taskHint.setText(Component.literal(snapshot.task().taskHint));
        objectiveRow.clearAllChildren();
        objectiveRow.addChild(createHudIcon(snapshot.task().hudIcon));
        objectiveRow.addChild(taskHint);
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
