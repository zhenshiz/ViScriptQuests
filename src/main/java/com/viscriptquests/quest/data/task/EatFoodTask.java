package com.viscriptquests.quest.data.task;

import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegister;
import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted;
import com.viscriptquests.quest.data.DisplayIcon;
import com.viscriptquests.quest.data.ItemMatchRule;
import com.viscriptquests.quest.data.QuestValueToken;
import com.viscriptquests.quest.data.QuestVariableValue;
import com.viscriptquests.quest.data.runtime.TaskObjectiveProgress;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.UseAnim;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 累计目标激活后玩家完整食用食物或饮用物品的次数。
 *
 * <p>匹配使用前的物品栈，每次完成事件计一次，不按原物品栈的数量计数。
 */
@LDLRegister(name = "eat_food_task", registry = ITask.ID)
public class EatFoodTask extends ITask {
    @Persisted
    public boolean anyFood = true;
    @Persisted
    public ItemStack itemStack = ItemStack.EMPTY;
    @Persisted
    public ItemMatchRule itemMatchRule = new ItemMatchRule();
//    @Persisted
//    public ItemUseHand hand = ItemUseHand.ANY;
    @Persisted
    public int eatCount = 1;
    @Persisted
    public final List<QuestValueToken> eatCountExpression = new ArrayList<>();

    /**
     * 判断使用完成的物品及手部是否符合食用或饮用目标。
     *
     * @param player 服务端玩家，用于解析物品的食物属性
     * @param consumedStack 使用前的物品栈副本
     * @return 具有食物属性或饮用动画，且通过物品、组件和手部筛选时返回 {@code true}
     */
    public boolean matches(ServerPlayer player, ItemStack consumedStack) {
        if (consumedStack == null || consumedStack.isEmpty()
                || (consumedStack.getFoodProperties(player) == null && consumedStack.getUseAnimation() != UseAnim.DRINK)) {
            return false;
        }
        return anyFood || (!itemStack.isEmpty() && itemMatchRule.matches(consumedStack, itemStack));
    }

    @Override
    public boolean checkCompletion(ServerPlayer player) {
        return false;
    }

    @Override
    public boolean onComplete(ServerPlayer player) {
        return true;
    }

    @Override
    public int getRequiredAmount() {
        return getRequiredAmount(null, null);
    }

    @Override
    public int getRequiredAmount(Map<String, QuestVariableValue> questVariables, ServerPlayer player) {
        return QuestValueToken.evaluateInt(eatCountExpression, questVariables, player, eatCount, 1);
    }

    @Override
    public void refreshObjectiveProgress(ServerPlayer player, TaskObjectiveProgress progress) {
        refreshObjectiveProgress(player, progress, null);
    }

    @Override
    public void refreshObjectiveProgress(ServerPlayer player, TaskObjectiveProgress progress,
                                         Map<String, QuestVariableValue> questVariables) {
        int required = getRequiredAmount(questVariables, player);
        progress.requiredAmount = required;
        progress.currentAmount = progress.isCompleted() ? required : Math.min(required, Math.max(0, progress.currentAmount));
        if (progress.isActive() && progress.currentAmount >= required) {
            progress.complete();
        }
    }

    @Override
    public boolean refreshesProgressFromPlayerState() {
        return false;
    }

    @Override
    public boolean autoCompleteObjective(ServerPlayer player, TaskObjectiveProgress progress) {
        return autoCompleteObjective(player, progress, null);
    }

    @Override
    public boolean autoCompleteObjective(ServerPlayer player, TaskObjectiveProgress progress,
                                         Map<String, QuestVariableValue> questVariables) {
        if (!progress.isActive()) {
            return false;
        }
        refreshObjectiveProgress(player, progress, questVariables);
        return progress.isCompleted();
    }

    @Override
    protected Component getDefaultTaskHint() {
        return getDefaultTaskHint(null, null);
    }

    @Override
    protected Component getDefaultTaskHint(ServerPlayer player, Map<String, QuestVariableValue> questVariables) {
        Component foodName = anyFood
                ? Component.translatable("viscript_quests.task.food.any")
                : itemStack.isEmpty() ? Component.translatable("viscript_quests.task.food.unset") : itemStack.getHoverName();
        return Component.translatable("viscript_quests.task_hint.eat_food_task",
                getRequiredAmount(questVariables, player), foodName);
    }

    @Override
    public DisplayIcon getDisplayIcon() {
        return DisplayIcon.item(anyFood || itemStack.isEmpty()
                ? Items.APPLE.getDefaultInstance() : itemStack.copyWithCount(1));
    }
}
