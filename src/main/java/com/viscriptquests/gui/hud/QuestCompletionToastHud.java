package com.viscriptquests.gui.hud;

import com.lowdragmc.lowdraglib2.gui.texture.ColorBorderTexture;
import com.lowdragmc.lowdraglib2.gui.texture.ColorRectTexture;
import com.lowdragmc.lowdraglib2.gui.texture.GuiTextureGroup;
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
import com.viscriptquests.quest.data.runtime.QuestCompletionToastData;
import dev.vfyjxf.taffy.style.AlignItems;
import dev.vfyjxf.taffy.style.FlexDirection;
import dev.vfyjxf.taffy.style.TaffyPosition;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;

/** 在屏幕上按配置位置排队展示大任务和小任务完成提示。 */
public class QuestCompletionToastHud extends UIElement {
    private static final int TOAST_WIDTH = 148;
    private static final int TOAST_HEIGHT = 25;
    private static final int ICON_SIZE = 16;
    private static final int MAX_VISIBLE = 3;
    private static final int MAX_PENDING = 16;
    private static final long DISPLAY_MILLIS = 5_000L;
    private static final int TEXT_TITLE = 0xFFF1F1F1;
    private static final int TEXT_COMPLETED = 0xFFFFD84B;
    private static final IGuiTexture BACKGROUND = GuiTextureGroup.of(
            new ColorRectTexture(0xD02A2A2A),
            new ColorBorderTexture(-1, 0x887A7A7A));

    private static final Deque<QuestCompletionToastData> PENDING = new ConcurrentLinkedDeque<>();

    private final UIElement toastStack = new UIElement();
    private final List<ActiveToast> activeToasts = new ArrayList<>();
    private float lastX = Float.NaN;
    private float lastY = Float.NaN;

    public QuestCompletionToastHud() {
        setAllowHitTest(false);
        layout(layout -> {
            layout.positionType(TaffyPosition.ABSOLUTE);
            layout.leftPercent(0);
            layout.topPercent(0);
            layout.widthPercent(100);
            layout.heightPercent(100);
        });

        toastStack.setAllowHitTest(false);
        toastStack.setVisible(false);
        toastStack.layout(layout -> {
            layout.positionType(TaffyPosition.ABSOLUTE);
            layout.leftPercent(0);
            layout.topPercent(0);
            layout.width(TOAST_WIDTH);
            layout.flexDirection(FlexDirection.COLUMN);
            layout.gapAll(2);
        });
        addChild(toastStack);
    }

    /**
     * 将一条任务完成提示加入客户端等待队列。
     *
     * <p>客户端配置关闭时忽略该数据；队列达到容量上限时优先丢弃最早等待的提示。
     *
     * @param data 服务端同步的任务完成提示数据
     */
    public static void enqueue(QuestCompletionToastData data) {
        if (data == null || !ClientConfig.SHOW_QUEST_COMPLETION_TOAST.get()) {
            return;
        }
        while (PENDING.size() >= MAX_PENDING) {
            PENDING.pollFirst();
        }
        PENDING.addLast(data);
    }

    @Override
    public void screenTick() {
        super.screenTick();
        updateLayoutFromConfig();
        if (!ClientConfig.SHOW_QUEST_COMPLETION_TOAST.get() || Minecraft.getInstance().player == null) {
            clearToasts();
            return;
        }

        long now = Util.getMillis();
        boolean changed = activeToasts.removeIf(toast -> toast.expiresAt <= now);
        QuestCompletionToastData pending;
        while (activeToasts.size() < MAX_VISIBLE && (pending = PENDING.pollFirst()) != null) {
            activeToasts.add(new ActiveToast(pending, now + DISPLAY_MILLIS));
            changed = true;
        }
        if (changed) {
            rebuildToasts();
        }
    }

    private void updateLayoutFromConfig() {
        float x = ClientConfig.QUEST_COMPLETION_TOAST_X_PERCENT.get().floatValue();
        float y = ClientConfig.QUEST_COMPLETION_TOAST_Y_PERCENT.get().floatValue();
        if (x == lastX && y == lastY) {
            return;
        }
        lastX = x;
        lastY = y;
        toastStack.layout(layout -> {
            layout.leftPercent(x);
            layout.topPercent(y);
        });
    }

    private void clearToasts() {
        if (activeToasts.isEmpty() && PENDING.isEmpty()) {
            return;
        }
        activeToasts.clear();
        PENDING.clear();
        toastStack.clearAllChildren();
        toastStack.setVisible(false);
    }

    private void rebuildToasts() {
        toastStack.clearAllChildren();
        for (ActiveToast toast : activeToasts) {
            toastStack.addChild(createToast(toast.data));
        }
        toastStack.setVisible(!activeToasts.isEmpty());
    }

    private static UIElement createToast(QuestCompletionToastData data) {
        UIElement card = new UIElement();
        card.setAllowHitTest(false);
        card.style(style -> style.backgroundTexture(BACKGROUND));
        card.layout(layout -> {
            layout.width(TOAST_WIDTH);
            layout.height(TOAST_HEIGHT);
            layout.flexDirection(FlexDirection.ROW);
            layout.alignItems(AlignItems.CENTER);
            layout.paddingHorizontal(4);
            layout.gapAll(4);
        });

        UIElement icon = createIcon(data.icon, data.questCompletion);
        icon.setAllowHitTest(false);
        icon.layout(layout -> {
            layout.width(ICON_SIZE);
            layout.height(ICON_SIZE);
        });

        UIElement textColumn = new UIElement();
        textColumn.setAllowHitTest(false);
        textColumn.layout(layout -> {
            layout.width(0);
            layout.flex(1);
            layout.height(19);
            layout.flexDirection(FlexDirection.COLUMN);
        });

        Label title = label(data.titleComponent(), 7.0f, TEXT_TITLE, 10);
        Label completed = label(Component.translatable(data.questCompletion
                ? "viscript_quests.completion_toast.quest_completed"
                : "viscript_quests.completion_toast.task_completed"), 6.6f, TEXT_COMPLETED, 9);
        textColumn.addChildren(title, completed);
        card.addChildren(icon, textColumn);
        return card;
    }

    private static UIElement createIcon(DisplayIcon displayIcon, boolean questCompletion) {
        if (displayIcon != null && displayIcon.isTexture()
                && displayIcon.getTexture() != null && !displayIcon.getTexture().isBlank()) {
            ResourceLocation location = ResourceLocation.tryParse(displayIcon.getTexture());
            UIElement icon = new UIElement();
            icon.style(style -> style.backgroundTexture(SpriteTexture.of(location == null
                    ? ViScriptQuests.id("textures/gui/icon/icon_task.png")
                    : location)));
            return icon;
        }
        ItemStack stack = displayIcon == null ? ItemStack.EMPTY : displayIcon.renderItemStack();
        if (stack == null || stack.isEmpty()) {
            stack = new ItemStack(questCompletion ? Items.WRITABLE_BOOK : Items.PAPER);
        }
        ItemSlot slot = new ItemSlot();
        slot.setItem(stack);
        slot.style(style -> style.backgroundTexture(IGuiTexture.EMPTY));
        slot.slotStyle(style -> style
                .slotOverlay(IGuiTexture.EMPTY)
                .hoverOverlay(IGuiTexture.EMPTY)
                .showItemTooltips(false));
        return slot;
    }

    private static Label label(Component text, float fontSize, int color, int height) {
        Label label = new Label();
        label.setAllowHitTest(false);
        label.setText(text);
        label.layout(layout -> {
            layout.widthPercent(100);
            layout.height(height);
        });
        label.textStyle(style -> style
                .fontSize(fontSize)
                .textColor(color)
                .textShadow(true)
                .textAlignHorizontal(Horizontal.LEFT)
                .textAlignVertical(Vertical.CENTER)
                .textWrap(TextWrap.HIDE));
        return label;
    }

    private record ActiveToast(QuestCompletionToastData data, long expiresAt) {
    }
}
