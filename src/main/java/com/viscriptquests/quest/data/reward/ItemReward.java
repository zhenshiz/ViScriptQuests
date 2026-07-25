package com.viscriptquests.quest.data.reward;

import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegister;
import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted;
import com.viscriptquests.quest.data.DisplayIcon;
import com.viscriptquests.quest.data.QuestValueToken;
import com.viscriptquests.quest.data.QuestVariableValue;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.ItemHandlerHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

// 物品奖励，完成任务后给予玩家指定物品
@LDLRegister(name = "item_reward", registry = IReward.ID)
public class ItemReward extends IReward {
    @Persisted
    public ItemStack itemStack = ItemStack.EMPTY;
    @Persisted
    public int itemCount = 1;
    @Persisted
    public final List<QuestValueToken> itemCountExpression = new ArrayList<>();

    @Override
    public void grant(ServerPlayer player) {
        grant(player, null);
    }

    @Override
    public void grant(ServerPlayer player, Map<String, QuestVariableValue> questVariables) {
        ItemStack stack = resolvedItemStack(questVariables, player);
        if (player != null && !stack.isEmpty()) {
            ItemHandlerHelper.giveItemToPlayer(player, stack);
        }
    }

    @Override
    public void resolveDynamicValues(Map<String, QuestVariableValue> questVariables, ServerPlayer player) {
        itemCount = resolveItemCount(questVariables, player);
        itemCountExpression.clear();
        itemStack = itemIdentityStack().copyWithCount(itemCount);
    }

    @Override
    public Component getRewardHint() {
        return getRewardHint(null, null);
    }

    @Override
    public Component getRewardHint(ServerPlayer player, Map<String, QuestVariableValue> questVariables) {
        ItemStack stack = resolvedItemStack(questVariables, player);
        Component defaultHint = stack.isEmpty()
                ? Component.empty()
                : Component.translatable("viscript_quests.reward_hint.item_reward", stack.getDisplayName(), stack.getCount());
        return rewardHintOrDefault(defaultHint);
    }

    @Override
    public DisplayIcon getRewardIcon() {
        return rewardIconOrDefault(DisplayIcon.item(itemIdentityStack()));
    }

    public int resolveItemCount(Map<String, QuestVariableValue> questVariables, ServerPlayer player) {
        return QuestValueToken.evaluateInt(itemCountExpression, questVariables, player, itemCount, 1);
    }

    private ItemStack resolvedItemStack(Map<String, QuestVariableValue> questVariables, ServerPlayer player) {
        ItemStack stack = itemIdentityStack();
        return stack.isEmpty() ? ItemStack.EMPTY : stack.copyWithCount(resolveItemCount(questVariables, player));
    }

    private ItemStack itemIdentityStack() {
        return itemStack == null || itemStack.isEmpty() ? ItemStack.EMPTY : itemStack.copyWithCount(1);
    }
}
