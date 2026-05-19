package com.viscriptquests.quest.data.task;

import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegister;
import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted;
import com.viscriptquests.quest.data.DisplayIcon;
import com.viscriptquests.quest.data.runtime.TaskObjectiveProgress;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

// 破坏方块目标，进度由方块破坏事件累计，不能从玩家当前状态反推。
@LDLRegister(name = "break_block_task", registry = ITask.ID)
public class BreakBlockTask extends ITask {
    @Persisted
    public Block block = Blocks.STONE;
    @Persisted
    public int breakCount = 1;

    public boolean matches(BlockState state) {
        return state != null && block != null && block != Blocks.AIR && state.is(block);
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
        return Math.max(1, breakCount);
    }

    @Override
    public void refreshObjectiveProgress(ServerPlayer player, TaskObjectiveProgress progress) {
        int required = getRequiredAmount();
        progress.requiredAmount = required;
        progress.currentAmount = progress.completed ? required : Math.min(required, Math.max(0, progress.currentAmount));
        progress.completed = progress.completed || progress.currentAmount >= required;
    }

    @Override
    public boolean refreshesProgressFromPlayerState() {
        return false;
    }

    @Override
    public boolean autoCompleteObjective(ServerPlayer player, TaskObjectiveProgress progress) {
        if (progress.completed) {
            return false;
        }
        refreshObjectiveProgress(player, progress);
        return progress.completed;
    }

    @Override
    protected Component getDefaultTaskHint() {
        return Component.translatable("viscript_quests.task_hint.break_block_task",
                getRequiredAmount(), blockDisplayName());
    }

    @Override
    public DisplayIcon getDisplayIcon() {
        if (block != null && block != Blocks.AIR) {
            ItemStack stack = block.asItem().getDefaultInstance();
            if (!stack.isEmpty()) {
                return DisplayIcon.item(stack);
            }
        }
        return DisplayIcon.item(Items.IRON_PICKAXE.getDefaultInstance());
    }

    private Component blockDisplayName() {
        if (block == null || block == Blocks.AIR) {
            return Component.translatable("viscript_quests.task.block.invalid");
        }
        return block.getName();
    }
}
