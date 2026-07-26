package com.viscriptquests.quest.data.task;

import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegister;
import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted;
import com.viscriptquests.quest.data.DisplayIcon;
import com.viscriptquests.quest.data.ItemMatchRule;
import com.viscriptquests.quest.data.QuestSubmitMode;
import com.viscriptquests.quest.data.QuestValueToken;
import com.viscriptquests.quest.data.QuestVariableValue;
import com.viscriptquests.quest.data.runtime.TaskObjectiveProgress;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

// 物品收集/提交任务，检查玩家背包中是否有指定物品
@LDLRegister(name = "item_task", registry = ITask.ID)
public class ItemTask extends ITask {
    @Persisted
    public ItemStack itemStack = ItemStack.EMPTY;
    @Persisted
    public int itemCount = 1;
    @Persisted
    public final List<QuestValueToken> itemCountExpression = new ArrayList<>();
    @Persisted
    public ItemMatchRule itemMatchRule = new ItemMatchRule();
    @Persisted
    public boolean consumeItem = true;
    @Persisted
    public QuestSubmitMode submitMode = QuestSubmitMode.AUTO;

    @Override
    public boolean checkCompletion(ServerPlayer player) {
        return checkCompletion(player, null);
    }

    @Override
    public boolean checkCompletion(ServerPlayer player, Map<String, QuestVariableValue> questVariables) {
        return getPlayerItemCount(player) >= getRequiredAmount(questVariables, player);
    }

    @Override
    public boolean onComplete(ServerPlayer player) {
        return onComplete(player, null);
    }

    @Override
    public boolean onComplete(ServerPlayer player, Map<String, QuestVariableValue> questVariables) {
        if (!consumeItem) {
            return true;
        }
        return removePlayerItems(player, getRequiredAmount(questVariables, player));
    }

    @Override
    public boolean allowsAutoSubmit() {
        return submitMode.isAutoSubmit();
    }

    @Override
    public int getRequiredAmount() {
        return getRequiredAmount(null, null);
    }

    @Override
    public int getRequiredAmount(Map<String, QuestVariableValue> questVariables, ServerPlayer player) {
        return QuestValueToken.evaluateInt(itemCountExpression, questVariables, player, itemCount, 1);
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
        if (progress.manualSubmitRequired) {
            progress.currentAmount = progress.isCompleted() ? required : Math.min(progress.currentAmount, required);
            return;
        }
        progress.currentAmount = progress.isCompleted()
                ? required
                : Math.min(required, getPlayerItemCount(player));
    }

    @Override
    public boolean submitObjective(ServerPlayer player, TaskObjectiveProgress progress) {
        return submitObjective(player, progress, null);
    }

    @Override
    public boolean submitObjective(ServerPlayer player, TaskObjectiveProgress progress,
                                   Map<String, QuestVariableValue> questVariables) {
        if (submitMode.isAutoSubmit() || !progress.isActive() || itemIdentityStack().isEmpty()) {
            return false;
        }
        int required = getRequiredAmount(questVariables, player);
        int remaining = Math.max(0, required - progress.currentAmount);
        if (remaining == 0) {
            progress.complete();
            progress.currentAmount = required;
            return true;
        }
        int available = getPlayerItemCount(player);
        if (available <= 0) {
            return false;
        }
        if (!consumeItem) {
            int newAmount = Math.min(required, Math.max(progress.currentAmount, available));
            if (newAmount <= progress.currentAmount) {
                return false;
            }
            progress.currentAmount = newAmount;
            progress.requiredAmount = required;
            if (progress.currentAmount >= required) {
                progress.complete();
            }
            return true;
        }
        int toSubmit = Math.min(available, remaining);
        if (!removePlayerItems(player, toSubmit)) {
            return false;
        }
        progress.currentAmount = Math.min(required, progress.currentAmount + toSubmit);
        progress.requiredAmount = required;
        if (progress.currentAmount >= required) {
            progress.complete();
        }
        return true;
    }

    @Override
    protected Component getDefaultTaskHint() {
        return getDefaultTaskHint(null, null);
    }

    @Override
    protected Component getDefaultTaskHint(ServerPlayer player, Map<String, QuestVariableValue> questVariables) {
        ItemStack identity = itemIdentityStack();
        if (identity.isEmpty()) return Component.empty();
        int count = getRequiredAmount(questVariables, player);
        String key = consumeItem
                ? "viscript_quests.task_hint.item_task.submit"
                : "viscript_quests.task_hint.item_task.have";
        return Component.translatable(key, count, identity.getDisplayName().copy().setStyle(Style.EMPTY));
    }

    @Override
    public DisplayIcon getDisplayIcon() {
        return DisplayIcon.item(itemIdentityStack());
    }

    private int getPlayerItemCount(ServerPlayer player) {
        if (itemIdentityStack().isEmpty()) {
            return 0;
        }
        ItemStack identity = itemIdentityStack();
        return itemMatchRule.getItemForPlayerCount(player, identity);
    }

    private boolean removePlayerItems(ServerPlayer player, int count) {
        if (player == null || itemIdentityStack().isEmpty() || count <= 0) {
            return true;
        }
        ItemStack identity = itemIdentityStack();
        int remaining = itemMatchRule.removeItemForPlayer(player, identity, count);
        player.containerMenu.broadcastChanges();
        return remaining <= 0;
    }

    private ItemStack itemIdentityStack() {
        return itemStack == null || itemStack.isEmpty() ? ItemStack.EMPTY : itemStack.copyWithCount(1);
    }
}
