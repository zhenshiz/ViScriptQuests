package com.viscriptquests.quest.data.task;

import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegister;
import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted;
import com.viscriptquests.quest.data.DisplayIcon;
import com.viscriptquests.quest.data.QuestVariableValue;
import com.viscriptquests.quest.data.runtime.TaskObjectiveProgress;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;

import java.util.Map;
import java.util.Optional;

// 手动查看 Ponder 的目标。完成条件由任务书里的“查看”按钮触发，不依赖观看时长。
@LDLRegister(name = "ponder_task", registry = ITask.ID)
public class PonderTask extends ITask {
    @Persisted
    public String ponderComponentId = "minecraft:crafting_table";

    @Override
    public boolean checkCompletion(ServerPlayer player) {
        return false;
    }

    @Override
    public boolean onComplete(ServerPlayer player) {
        return true;
    }

    @Override
    public boolean allowsAutoSubmit() {
        return false;
    }

    @Override
    public boolean submitObjective(ServerPlayer player, TaskObjectiveProgress progress,
                                   Map<String, QuestVariableValue> questVariables) {
        if (progress == null || !progress.isActive() || normalizedComponentId().isBlank()) {
            return false;
        }
        progress.requiredAmount = 1;
        progress.currentAmount = 1;
        progress.complete();
        progress.ponderComponentId = normalizedComponentId();
        progress.ponderViewAction = true;
        return true;
    }

    @Override
    public void refreshObjectiveProgress(ServerPlayer player, TaskObjectiveProgress progress,
                                         Map<String, QuestVariableValue> questVariables) {
        progress.requiredAmount = 1;
        progress.currentAmount = progress.isCompleted() ? 1 : 0;
        progress.ponderComponentId = normalizedComponentId();
        progress.ponderViewAction = true;
    }

    @Override
    public boolean refreshesProgressFromPlayerState() {
        return false;
    }

    @Override
    public String getPonderComponentId() {
        return normalizedComponentId();
    }

    @Override
    public boolean hasPonderViewAction() {
        return true;
    }

    @Override
    protected Component getDefaultTaskHint() {
        return Component.translatable("viscript_quests.task_hint.ponder_task", componentDisplayName());
    }

    @Override
    public DisplayIcon getDisplayIcon() {
        ItemStack stack = componentStack();
        return DisplayIcon.item(stack.isEmpty() ? Items.KNOWLEDGE_BOOK.getDefaultInstance() : stack);
    }

    private Component componentDisplayName() {
        ResourceLocation id = componentResourceLocation();
        if (id == null) {
            String normalized = normalizedComponentId();
            return Component.literal(normalized.isBlank() ? "-" : normalized);
        }
        Optional<Item> item = BuiltInRegistries.ITEM.getOptional(id);
        if (item.isPresent()) {
            return item.get().getDescription();
        }
        Optional<Block> block = BuiltInRegistries.BLOCK.getOptional(id);
        if (block.isPresent()) {
            return block.get().getName();
        }
        return Component.literal(id.toString());
    }

    private ItemStack componentStack() {
        ResourceLocation id = componentResourceLocation();
        if (id == null) {
            return ItemStack.EMPTY;
        }
        Optional<Item> item = BuiltInRegistries.ITEM.getOptional(id);
        if (item.isPresent()) {
            return new ItemStack(item.get());
        }
        Optional<Block> block = BuiltInRegistries.BLOCK.getOptional(id);
        return block.map(value -> new ItemStack(value.asItem())).filter(stack -> !stack.isEmpty()).orElse(ItemStack.EMPTY);
    }

    private ResourceLocation componentResourceLocation() {
        return ResourceLocation.tryParse(normalizedComponentId());
    }

    private String normalizedComponentId() {
        return ponderComponentId == null ? "" : ponderComponentId.trim();
    }
}
