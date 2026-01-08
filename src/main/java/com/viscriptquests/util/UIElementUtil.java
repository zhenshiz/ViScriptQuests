package com.viscriptquests.util;

import com.lowdragmc.lowdraglib2.configurator.ui.SearchComponentConfigurator;
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.data.TextWrap;
import com.lowdragmc.lowdraglib2.gui.ui.data.Vertical;
import com.lowdragmc.lowdraglib2.gui.ui.elements.TextElement;
import com.lowdragmc.lowdraglib2.utils.search.IResultHandler;
import net.minecraft.network.chat.Component;
import org.appliedenergistics.yoga.YogaFlexDirection;
import org.appliedenergistics.yoga.YogaGutter;
import org.appliedenergistics.yoga.YogaOverflow;

import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class UIElementUtil {
    public static SearchComponentConfigurator<String> createTaskTypeSearchComponentConfigurator(String name, Map<String, IGuiTexture> arr, Supplier<String> getter, Consumer<String> setter) {
        return new SearchComponentConfigurator<>(name,
                getter,
                setter,
                "",
                false,
                (word, searchHandler) -> {
                    String lowerWord = word.toLowerCase();
                    arr.forEach((key, value) -> {
                        if (Thread.currentThread().isInterrupted()) return;
                        if (key.toLowerCase().contains(lowerWord) || Component.translatable("viscript_quests.task." + key).getString().toLowerCase().contains(lowerWord)) {
                            ((IResultHandler<String>) searchHandler).acceptResult(key);
                        }
                    });
                },
                (value) -> BeanUtil.getValueOrDefault(value, ""),
                value -> {
                    UIElement container = (new UIElement()).layout((layout) -> {
                        layout.setFlexDirection(YogaFlexDirection.ROW);
                        layout.setGap(YogaGutter.ALL, 2.0F);
                        layout.setHeight(10.0F);
                    });
                    UIElement icon = (new UIElement()).layout((layout) -> {
                        layout.setAspectRatio(1.0F);
                        layout.setHeightPercent(100.0F);
                    }).style((style) -> style.backgroundTexture(arr.get(value)));
                    UIElement label = (new TextElement()).textStyle((style) -> style.textWrap(TextWrap.HOVER_ROLL).textAlignVertical(Vertical.CENTER)).setText(value.isEmpty() ? "" : "viscript_quests.task." + value).layout((layout) -> {
                        layout.setHeightPercent(100.0F);
                        layout.setFlex(1.0F);
                    }).setOverflow(YogaOverflow.HIDDEN);
                    container.addChildren(icon, label);
                    return container;
                }
        );
    }
}
