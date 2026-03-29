package com.viscriptquests.quest.task;

import com.lowdragmc.lowdraglib2.configurator.annotation.Configurable;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegister;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

@EqualsAndHashCode(callSuper = true)
@Data
@LDLRegister(name = "item", registry = IQuestTask.ID)
public class ItemTask extends IQuestTask {
    @Configurable(name = "viscript_quests.task.item.itemStack")
    private ItemStack itemStack = ItemStack.EMPTY;
    @Configurable(name = "viscript_quests.task.item.itemTaskAction")
    private ItemTaskAction itemTaskAction = ItemTaskAction.GATHER;
    @Configurable(name = "viscript_quests.task.item.strictComponents")
    private boolean strictComponents = true;

    public boolean matches(ItemStack stack) {
        if (stack.isEmpty()) return false;

        if (!stack.is(itemStack.getItem())) return false;

        if (strictComponents) {
            return ItemStack.isSameItemSameComponents(itemStack, stack);
        }

        return true;
    }

    @Getter
    @AllArgsConstructor
    public enum ItemTaskAction implements StringRepresentable {
        GATHER("viscript_quests.task.item.action.gather"),
        CRAFT("viscript_quests.task.item.action.craft");

        private final String name;

        @Override
        public @NotNull String getSerializedName() {
            return name;
        }
    }
}
