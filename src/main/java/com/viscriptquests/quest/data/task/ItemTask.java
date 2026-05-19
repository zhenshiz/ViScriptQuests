package com.viscriptquests.quest.data.task;

import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegister;
import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted;
import com.viscriptquests.quest.data.DisplayIcon;
import com.viscriptquests.quest.data.QuestSubmitMode;
import com.viscriptquests.quest.data.runtime.TaskObjectiveProgress;
import com.viscriptquests.util.ItemUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

// 物品收集/提交任务，检查玩家背包中是否有指定物品
@LDLRegister(name = "item_task", registry = ITask.ID)
public class ItemTask extends ITask {
    @Persisted
    public ItemStack itemStack = ItemStack.EMPTY;
    @Persisted
    public boolean strictComponents = false;
    @Persisted
    public boolean consumeItem = true;
    @Persisted
    public QuestSubmitMode submitMode = QuestSubmitMode.AUTO;

    @Override
    public boolean checkCompletion(ServerPlayer player) {
        return ItemUtil.getItemForPlayerCount(player, itemStack, strictComponents) >= itemStack.getCount();
    }

    @Override
    public boolean onComplete(ServerPlayer player) {
        if (!consumeItem) {
            return true;
        }
        return ItemUtil.removeItemForPlayer(player, itemStack, strictComponents, itemStack.getCount());
    }

    @Override
    public boolean allowsAutoSubmit() {
        return submitMode.isAutoSubmit();
    }

    @Override
    public int getRequiredAmount() {
        return itemStack.isEmpty() ? 1 : itemStack.getCount();
    }

    @Override
    public void refreshObjectiveProgress(ServerPlayer player, TaskObjectiveProgress progress) {
        int required = getRequiredAmount();
        progress.requiredAmount = required;
        if (progress.manualSubmitRequired) {
            progress.currentAmount = progress.completed ? required : Math.min(progress.currentAmount, required);
            return;
        }
        progress.currentAmount = progress.completed
                ? required
                : Math.min(required, ItemUtil.getItemForPlayerCount(player, itemStack, strictComponents));
    }

    @Override
    public boolean submitObjective(ServerPlayer player, TaskObjectiveProgress progress) {
        if (submitMode.isAutoSubmit() || progress.completed || itemStack.isEmpty()) {
            return false;
        }
        int required = getRequiredAmount();
        int remaining = Math.max(0, required - progress.currentAmount);
        if (remaining == 0) {
            progress.completed = true;
            progress.currentAmount = required;
            return true;
        }
        int available = ItemUtil.getItemForPlayerCount(player, itemStack, strictComponents);
        if (available <= 0) {
            return false;
        }
        if (!consumeItem) {
            int newAmount = Math.min(required, Math.max(progress.currentAmount, available));
            if (newAmount <= progress.currentAmount && !progress.completed) {
                return false;
            }
            progress.currentAmount = newAmount;
            progress.requiredAmount = required;
            progress.completed = progress.currentAmount >= required;
            return true;
        }
        int toSubmit = Math.min(available, remaining);
        if (!ItemUtil.removeItemForPlayer(player, itemStack, strictComponents, toSubmit)) {
            return false;
        }
        progress.currentAmount = Math.min(required, progress.currentAmount + toSubmit);
        progress.requiredAmount = required;
        progress.completed = progress.currentAmount >= required;
        return true;
    }

    @Override
    protected Component getDefaultTaskHint() {
        if (itemStack.isEmpty()) return Component.empty();
        int count = itemStack.getCount();
        String key = consumeItem
                ? "viscript_quests.task_hint.item_task.submit"
                : "viscript_quests.task_hint.item_task.have";
        return Component.translatable(key, count, itemStack.getDisplayName());
    }

    @Override
    public DisplayIcon getDisplayIcon() {
        return DisplayIcon.item(itemStack);
    }
}
