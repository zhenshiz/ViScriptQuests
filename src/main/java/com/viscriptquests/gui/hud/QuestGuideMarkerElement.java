package com.viscriptquests.gui.hud;

import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.texture.Icons;
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
import com.viscriptquests.quest.data.runtime.QuestGuideMarker;
import dev.vfyjxf.taffy.style.AlignItems;
import dev.vfyjxf.taffy.style.FlexDirection;
import dev.vfyjxf.taffy.style.TaffyPosition;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

// 当前追踪小任务的位置导航标记。服务端只同步目标点，屏幕投影和边缘吸附都在客户端计算。
public class QuestGuideMarkerElement extends UIElement {
    private static final ItemStack DEFAULT_ICON = new ItemStack(Items.COMPASS);
    private static final int TEXT_WHITE = 0xFFFFFFFF;
    private static final int TEXT_MUTED = 0xFFDAD5E8;
    private static final float FONT_LABEL = 3.0f;
    private static final float FONT_DISTANCE = 3.0f;
    private static final float FONT_EDGE_DISTANCE = 4.5f;
    private static final int LABEL_HEIGHT = 4;
    private static final int DISTANCE_HEIGHT = 4;
    private static final int EDGE_DISTANCE_HEIGHT = 6;

    private final UIElement markerCard = new UIElement();
    private final UIElement edgeCard = new UIElement();
    private final UIElement arrow = new UIElement();
    private final UIElement iconHolder = new UIElement();
    private final Label label = label(FONT_LABEL, TEXT_WHITE);
    private final Label distanceLabel = label(FONT_DISTANCE, TEXT_MUTED);
    private final Label edgeDistanceLabel = label(FONT_EDGE_DISTANCE, TEXT_MUTED);
    private String lastIconKey = "";
    private int lastIconSize = -1;

    public QuestGuideMarkerElement() {
        setAllowHitTest(false);
        setVisible(false);
        layout(layout -> {
            layout.positionType(TaffyPosition.ABSOLUTE);
            layout.leftPercent(0);
            layout.topPercent(0);
            layout.widthPercent(100);
            layout.heightPercent(100);
        });

        markerCard.setAllowHitTest(false);
        markerCard.style(style -> style.backgroundTexture(IGuiTexture.EMPTY));
        markerCard.layout(layout -> {
            layout.positionType(TaffyPosition.ABSOLUTE);
            layout.flexDirection(FlexDirection.COLUMN);
            layout.alignItems(AlignItems.CENTER);
            layout.gapAll(0);
        });

        edgeCard.setAllowHitTest(false);
        edgeCard.style(style -> style.backgroundTexture(IGuiTexture.EMPTY));
        edgeCard.layout(layout -> {
            layout.positionType(TaffyPosition.ABSOLUTE);
            layout.flexDirection(FlexDirection.ROW);
            layout.alignItems(AlignItems.CENTER);
            layout.gapAll(2);
        });

        arrow.setAllowHitTest(false);
        arrow.style(style -> style.backgroundTexture(Icons.RIGHT_ARROW_NO_BAR.copy()));

        iconHolder.setAllowHitTest(false);

        markerCard.addChildren(label, iconHolder, distanceLabel);
        edgeCard.addChildren(arrow, edgeDistanceLabel);
        addChildren(markerCard, edgeCard);
    }

    public void update(QuestHudData.ComponentState snapshot) {
        if (snapshot == null || snapshot.isEmpty()) {
            hide();
            return;
        }
        update(snapshot.task().guideMarker, snapshot.task().title);
    }

    public void update(QuestGuideMarker marker, String fallbackLabel) {
        if (!ClientConfig.SHOW_QUEST_GUIDE_MARKER.get()) {
            hide();
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (!canShow(marker, minecraft)) {
            hide();
            return;
        }

        Vec3 target = marker.position();
        double distance = minecraft.player.position().distanceTo(target);
        if (marker.hideWhenReached && distance <= Math.max(0.0, marker.arrivalRadius)) {
            hide();
            return;
        }

        int screenWidth = minecraft.getWindow().getGuiScaledWidth();
        int screenHeight = minecraft.getWindow().getGuiScaledHeight();
        int iconSize = markerIconSize(screenWidth, screenHeight);
        int arrowSize = Math.max(9, Math.round(iconSize * 0.82f));
        boolean showLabel = marker.showLabel && !displayLabel(marker, fallbackLabel).isBlank();
        boolean showDistance = marker.showDistance;
        int textWidth = showLabel || showDistance ? Math.max(42, Math.round(screenWidth * 0.07f)) : iconSize;
        int cardWidth = Math.max(iconSize, textWidth);
        int cardHeight = iconSize
                + (showLabel ? LABEL_HEIGHT : 0)
                + (showDistance ? DISTANCE_HEIGHT : 0);
        ProjectedPoint point = project(target, screenWidth, screenHeight, cardWidth, cardHeight, minecraft);
        if (point == null) {
            hide();
            return;
        }
        int edgeWidth = arrowSize + (showDistance ? 32 : 0);
        int edgeHeight = Math.max(arrowSize, EDGE_DISTANCE_HEIGHT);
        if (!point.onScreen) {
            ProjectedPoint edgePoint = project(target, screenWidth, screenHeight, edgeWidth, edgeHeight, minecraft);
            if (edgePoint == null) {
                hide();
                return;
            }
            point = new ProjectedPoint(edgePoint.x, edgePoint.y, false, edgePoint.angle);
        }

        setVisible(true);
        updateLayout(point, cardWidth, cardHeight, edgeWidth, edgeHeight, iconSize, arrowSize, textWidth);
        updateText(marker, fallbackLabel, distance, showLabel, showDistance, point.onScreen);
        updateArrow(point);
        if (point.onScreen) {
            updateIcon(marker.icon, iconSize);
        }
    }

    private void hide() {
        setVisible(false);
        lastIconKey = "";
    }

    private static boolean canShow(QuestGuideMarker marker, Minecraft minecraft) {
        if (marker == null || !marker.enabled || minecraft.player == null || minecraft.level == null) {
            return false;
        }
        if (QuestGuideMarkerClientBridge.hidesBuiltInMarker(marker)) {
            return false;
        }
        if (marker.dimension == null || marker.dimension.isBlank()) {
            return false;
        }
        return minecraft.player.level().dimension().location().toString().equals(marker.dimension);
    }

    private static int markerIconSize(int screenWidth, int screenHeight) {
        double percent = ClientConfig.QUEST_GUIDE_MARKER_ICON_SIZE_PERCENT.get();
        return Mth.clamp((int) Math.round(Math.min(screenWidth, screenHeight) * percent / 100.0), 10, 28);
    }

    private void updateLayout(ProjectedPoint point, int cardWidth, int cardHeight, int edgeWidth, int edgeHeight,
                              int iconSize, int arrowSize, int textWidth) {
        markerCard.layout(layout -> {
            layout.left(point.x - cardWidth / 2.0f);
            layout.top(point.y - cardHeight / 2.0f);
            layout.width(cardWidth);
            layout.height(cardHeight);
        });
        edgeCard.layout(layout -> {
            layout.left(point.x - edgeWidth / 2.0f);
            layout.top(point.y - edgeHeight / 2.0f);
            layout.width(edgeWidth);
            layout.height(edgeHeight);
        });
        arrow.layout(layout -> {
            layout.width(arrowSize);
            layout.height(arrowSize);
        });
        iconHolder.layout(layout -> {
            layout.width(iconSize);
            layout.height(iconSize);
        });
        label.layout(layout -> {
            layout.width(textWidth);
            layout.height(LABEL_HEIGHT);
        });
        distanceLabel.layout(layout -> {
            layout.width(textWidth);
            layout.height(DISTANCE_HEIGHT);
        });
        edgeDistanceLabel.layout(layout -> {
            layout.width(28);
            layout.height(EDGE_DISTANCE_HEIGHT);
        });
    }

    private void updateText(QuestGuideMarker marker, String fallbackLabel,
                            double distance, boolean showLabel, boolean showDistance, boolean onScreen) {
        updateTextColor(marker.color);
        label.setVisible(onScreen && showLabel);
        distanceLabel.setVisible(onScreen && showDistance);
        edgeDistanceLabel.setVisible(!onScreen && showDistance);
        if (onScreen && showLabel) {
            label.setText(Component.literal(displayLabel(marker, fallbackLabel)));
        }
        if (showDistance) {
            Component distanceText = Component.literal(formatDistance(distance));
            if (onScreen) {
                distanceLabel.setText(distanceText);
            } else {
                edgeDistanceLabel.setText(distanceText);
            }
        }
    }

    private void updateArrow(ProjectedPoint point) {
        markerCard.setVisible(point.onScreen);
        edgeCard.setVisible(!point.onScreen);
        if (!point.onScreen) {
            arrow.style(style -> style.backgroundTexture(Icons.RIGHT_ARROW_NO_BAR.copy()
                    .setColor(TEXT_WHITE)
                    .rotate(point.angle)));
        }
    }

    private void updateTextColor(int color) {
        int normalizedColor = normalizeColor(color);
        label.textStyle(style -> style.textColor(normalizedColor));
        distanceLabel.textStyle(style -> style.textColor(normalizedColor));
        edgeDistanceLabel.textStyle(style -> style.textColor(normalizedColor));
    }

    private void updateIcon(DisplayIcon icon, int iconSize) {
        String iconKey = iconKey(icon);
        if (iconKey.equals(lastIconKey) && iconSize == lastIconSize) {
            return;
        }
        lastIconKey = iconKey;
        lastIconSize = iconSize;
        iconHolder.clearAllChildren();
        UIElement iconElement = createIcon(icon);
        iconElement.setAllowHitTest(false);
        iconElement.layout(layout -> {
            layout.width(iconSize);
            layout.height(iconSize);
        });
        iconHolder.addChild(iconElement);
    }

    private static UIElement createIcon(DisplayIcon icon) {
        if (icon != null && icon.isTexture() && icon.getTexture() != null && !icon.getTexture().isBlank()) {
            ResourceLocation location = ResourceLocation.tryParse(icon.getTexture());
            UIElement element = new UIElement();
            element.style(style -> style.backgroundTexture(SpriteTexture.of(location == null
                    ? ViScriptQuests.id("textures/gui/icon/icon_task.png")
                    : location)));
            return element;
        }
        ItemStack stack = icon == null ? ItemStack.EMPTY : icon.renderItemStack();
        ItemSlot slot = new ItemSlot();
        slot.setItem(stack == null || stack.isEmpty() ? DEFAULT_ICON : stack);
        slot.style(style -> style.backgroundTexture(IGuiTexture.EMPTY));
        slot.slotStyle(style -> style
                .slotOverlay(IGuiTexture.EMPTY)
                .hoverOverlay(IGuiTexture.EMPTY)
                .showItemTooltips(false));
        return slot;
    }

    private static ProjectedPoint project(Vec3 target, int screenWidth, int screenHeight,
                                          int cardWidth, int cardHeight, Minecraft minecraft) {
        Camera camera = minecraft.gameRenderer.getMainCamera();
        if (!camera.isInitialized()) {
            return null;
        }
        Vec3 delta = target.subtract(camera.getPosition());
        Vector3f forward = camera.getLookVector();
        Vector3f left = camera.getLeftVector();
        Vector3f up = camera.getUpVector();
        double depth = dot(delta, forward);
        double horizontal = -dot(delta, left);
        double vertical = dot(delta, up);
        double fov = Math.max(30.0, minecraft.options.fov().get().doubleValue());
        double tanHalfVertical = Math.tan(Math.toRadians(fov) / 2.0);
        double aspect = screenHeight == 0 ? 1.0 : (double) screenWidth / (double) screenHeight;
        double tanHalfHorizontal = tanHalfVertical * aspect;
        double safeDepth = Math.max(0.01, Math.abs(depth));
        double normalizedX = horizontal / (safeDepth * tanHalfHorizontal);
        double normalizedY = -vertical / (safeDepth * tanHalfVertical);
        DirectionVector edgeDirection = edgeDirection(horizontal, vertical, depth, normalizedX, normalizedY);

        float x = (float) (screenWidth * 0.5 + normalizedX * screenWidth * 0.5);
        float y = (float) (screenHeight * 0.5 + normalizedY * screenHeight * 0.5);
        float halfCardWidth = cardWidth / 2.0f;
        float halfCardHeight = cardHeight / 2.0f;
        float marginX = (float) (screenWidth * ClientConfig.QUEST_GUIDE_MARKER_EDGE_MARGIN_PERCENT.get() / 100.0) + halfCardWidth;
        float marginY = (float) (screenHeight * ClientConfig.QUEST_GUIDE_MARKER_EDGE_MARGIN_PERCENT.get() / 100.0) + halfCardHeight;
        float leftEdge = marginX;
        float rightEdge = screenWidth - marginX;
        float topEdge = marginY;
        float bottomEdge = screenHeight - marginY;
        boolean onScreen = depth > 0.01
                && x >= leftEdge && x <= rightEdge
                && y >= topEdge && y <= bottomEdge;
        if (onScreen) {
            return new ProjectedPoint(x, y, true, 0.0f);
        }
        float centerX = screenWidth * 0.5f;
        float centerY = screenHeight * 0.5f;
        float dx = edgeDirection.x;
        float dy = edgeDirection.y;
        float scaleX = dx > 0 ? (rightEdge - centerX) / dx : dx < 0 ? (leftEdge - centerX) / dx : Float.POSITIVE_INFINITY;
        float scaleY = dy > 0 ? (bottomEdge - centerY) / dy : dy < 0 ? (topEdge - centerY) / dy : Float.POSITIVE_INFINITY;
        float scale = Math.min(Math.abs(scaleX), Math.abs(scaleY));
        float clampedX = centerX + dx * scale;
        float clampedY = centerY + dy * scale;
        // 箭头角度使用最终吸附到屏幕边缘的位置计算，避免位置和旋转各算一套导致方向漂移。
        float angle = arrowAngle(clampedX - centerX, clampedY - centerY);
        return new ProjectedPoint(clampedX, clampedY, false, angle);
    }

    private static DirectionVector edgeDirection(double horizontal, double vertical, double depth,
                                                 double normalizedX, double normalizedY) {
        double x = normalizedX;
        double y = normalizedY;
        if (depth <= 0.01) {
            x = horizontal;
            y = -vertical;
        }
        if (Math.abs(x) < 0.001 && Math.abs(y) < 0.001) {
            y = 1.0;
        }
        return new DirectionVector((float) x, (float) y);
    }

    private static float arrowAngle(float dx, float dy) {
        // LDLib2 这张箭头贴图的 0 度基准实际朝下，换算到“屏幕方向角”时需要逆时针补偿 90 度。
        return (float) Math.toDegrees(Math.atan2(dy, dx)) - 90.0f;
    }

    private static double dot(Vec3 vector, Vector3f axis) {
        return vector.x * axis.x() + vector.y * axis.y() + vector.z * axis.z();
    }

    private static String displayLabel(QuestGuideMarker marker, String fallbackLabel) {
        if (marker.label != null && !marker.label.isBlank()) {
            return marker.label;
        }
        return fallbackLabel == null ? "" : fallbackLabel;
    }

    private static String formatDistance(double distance) {
        if (distance >= 100.0) {
            return Math.round(distance) + "m";
        }
        return String.format(java.util.Locale.ROOT, "%.1fm", distance);
    }

    private static int normalizeColor(int color) {
        return (color & 0xFF000000) == 0 ? color | 0xFF000000 : color;
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

    private static Label label(float fontSize, int color) {
        Label label = new Label();
        label.textStyle(style -> style
                .fontSize(fontSize)
                .textColor(color)
                .textShadow(true)
                .textAlignHorizontal(Horizontal.CENTER)
                .textAlignVertical(Vertical.CENTER)
                .textWrap(TextWrap.HIDE));
        return label;
    }

    private record ProjectedPoint(float x, float y, boolean onScreen, float angle) {
    }

    private record DirectionVector(float x, float y) {
    }
}
