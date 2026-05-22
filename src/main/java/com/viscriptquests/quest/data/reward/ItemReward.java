package com.viscriptquests.quest.data.reward;

import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegister;
import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted;
import com.viscriptquests.quest.data.DisplayIcon;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.ItemHandlerHelper;

// 物品奖励，完成任务后给予玩家指定物品
@LDLRegister(name = "item_reward", registry = IReward.ID)
public class ItemReward extends IReward {
    @Persisted
    public ItemStack itemStack = ItemStack.EMPTY;

    @Override
    public void grant(ServerPlayer player) {
        ItemHandlerHelper.giveItemToPlayer(player, itemStack);
    }

    @Override
    public Component getRewardHint() {
        Component defaultHint = itemStack.isEmpty()
                ? Component.empty()
                : Component.translatable("viscript_quests.reward_hint.item_reward", itemStack.getDisplayName(), itemStack.getCount());
        return rewardHintOrDefault(defaultHint);
    }

    @Override
    public DisplayIcon getRewardIcon() {
        return rewardIconOrDefault(DisplayIcon.item(itemStack));
    }
}
