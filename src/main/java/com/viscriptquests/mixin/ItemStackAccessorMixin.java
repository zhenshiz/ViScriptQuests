package com.viscriptshop.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import com.lowdragmc.lowdraglib2.configurator.accessors.ItemStackAccessor;
import com.lowdragmc.lowdraglib2.configurator.ui.Configurator;
import com.lowdragmc.lowdraglib2.gui.ui.elements.ItemSlot;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.viscriptshop.gui.components.SelectItemDialog;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.lang.reflect.Field;
import java.util.function.Consumer;
import java.util.function.Supplier;

@Mixin(ItemStackAccessor.class)
public class ItemStackAccessorMixin {

    @Inject(method = "create", at = @At(value = "INVOKE", target = "Lcom/lowdragmc/lowdraglib2/gui/ui/UIElement;addChild(Lcom/lowdragmc/lowdraglib2/gui/ui/UIElement;)Lcom/lowdragmc/lowdraglib2/gui/ui/UIElement;"))
    public void create(String name, Supplier<ItemStack> supplier, Consumer<ItemStack> consumer, boolean forceUpdate, Field field, Object owner, CallbackInfoReturnable<Configurator> cir, @Local(name = "slot") ItemSlot slot) {
        slot.addEventListener(UIEvents.CLICK, event -> {
            var dialog = new SelectItemDialog(itemStack -> {
                slot.setItem(itemStack);
                consumer.accept(itemStack);
            });
            dialog.show(slot.getModularUI());
        });

    }
}
