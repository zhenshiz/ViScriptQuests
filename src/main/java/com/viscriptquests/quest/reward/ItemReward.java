package com.viscriptquests.quest.reward;

import com.lowdragmc.lowdraglib2.configurator.annotation.Configurable;
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.texture.ItemStackTexture;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegister;
import lombok.Data;
import lombok.EqualsAndHashCode;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.items.ItemHandlerHelper;

@EqualsAndHashCode(callSuper = true)
@Data
@LDLRegister(name = "item", registry = IQuestReward.ID)
public class ItemReward extends IQuestReward {
    @Configurable(name = "viscript_quests.reward.item.itemStack")
    private ItemStack itemStack = ItemStack.EMPTY;
}
