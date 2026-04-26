package com.viscriptquests.gui.components;

import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Dialog;
import com.lowdragmc.lowdraglib2.gui.ui.elements.ItemSlot;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import dev.vfyjxf.taffy.style.FlexDirection;
import dev.vfyjxf.taffy.style.FlexWrap;
import dev.vfyjxf.taffy.style.TaffyDimension;
import dev.vfyjxf.taffy.style.TaffyDisplay;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;

import java.util.function.Consumer;

//选择玩家背包里的物品
public class SelectItemDialog extends Dialog {
    public Consumer<ItemStack> onItemSelected;

    public SelectItemDialog(Consumer<ItemStack> onItemSelected) {
        super();
        this.onItemSelected = onItemSelected;
        this.setTitle("viscript_shop.selectItemDialog.title");
        this.buttonContainer.setDisplay(TaffyDisplay.NONE);
        this.width(TaffyDimension.length(200));
        this.getStyle().zIndex(100);

        // 主容器：垂直布局
        UIElement mainContainer = new UIElement();
        mainContainer.layout(layout -> {
            layout.widthPercent(100);
            layout.flexDirection(FlexDirection.COLUMN);
            layout.gapAll(5);  // 背包和快捷栏之间的间距
            layout.paddingAll(5);
        });

        Minecraft minecraft = Minecraft.getInstance();
        var player = minecraft.player;
        if (player != null) {
            var inventory = player.getInventory();

            // 1. 主背包（3行9列，索引 9-35）
            UIElement backpackContainer = new UIElement();
            backpackContainer.layout(layout -> {
                layout.flexDirection(FlexDirection.ROW);
                layout.wrap(FlexWrap.WRAP);
                layout.gapAll(2);
                layout.width(9 * 18 + 8 * 2);
            });

            for (int i = 9; i < 36; i++) {
                ItemStack stack = inventory.getItem(i);
                backpackContainer.addChild(createItemSlot(stack));
            }

            // 2. 快捷栏（1行9列，索引 0-8）
            UIElement hotbarContainer = new UIElement();
            hotbarContainer.layout(layout -> {
                layout.flexDirection(FlexDirection.ROW);
                layout.gapAll(2);
                layout.width(9 * 18 + 8 * 2);
            });

            for (int i = 0; i < 9; i++) {
                ItemStack stack = inventory.getItem(i);
                hotbarContainer.addChild(createItemSlot(stack));
            }

            mainContainer.addChildren(backpackContainer, hotbarContainer);
        }

        this.addContent(mainContainer);
    }

    private ItemSlot createItemSlot(ItemStack stack) {
        ItemSlot slot = new ItemSlot();
        slot.setItem(stack.isEmpty() ? ItemStack.EMPTY : stack);
        slot.layout(layout -> {
            layout.width(18);
            layout.height(18);
        });

        final ItemStack finalStack = stack.copy();
        slot.addEventListener(UIEvents.CLICK, event -> {
            if (onItemSelected != null) {
                onItemSelected.accept(finalStack);
                this.close();
            }
        });

        return slot;
    }
}
