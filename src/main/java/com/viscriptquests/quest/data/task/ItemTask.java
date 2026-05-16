package com.viscriptquests.quest.data.task;

import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegister;
import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted;
import com.viscriptquests.quest.data.DisplayIcon;
import com.viscriptquests.util.ItemUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

// 物品收集/提交任务，检查玩家背包中是否有指定物品
@LDLRegister(name = "item_task", registry = ITask.ID)
public class ItemTask extends ITask {
    @Persisted
    public ItemStack itemStack = ItemStack.EMPTY;
    @Persisted
    public boolean strictComponents = false;
    @Persisted
    public boolean consumeItem = true;

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
    public Component getTaskHint() {
        if (itemStack.isEmpty()) return Component.empty();
        int count = itemStack.getCount();
        String key = consumeItem
                ? "viscript_quests.task_hint.item_task.submit"
                : "viscript_quests.task_hint.item_task.have";
        return Component.translatable(key, count, itemStack.getDisplayName());
    }

    @Override
    public DisplayIcon getHudIcon() {
        return DisplayIcon.item(Items.DIAMOND.getDefaultInstance());
    }

    private DisplayIcon fallbackItemIcon() {
        return DisplayIcon.item(itemStack);
    }
}
