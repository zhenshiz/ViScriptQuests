package com.viscriptquests.quest.task;

import com.lowdragmc.lowdraglib2.configurator.annotation.Configurable;
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.texture.ItemStackTexture;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegister;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import net.minecraft.network.chat.Component;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.NotNull;

@EqualsAndHashCode(callSuper = true)
@Data
@LDLRegister(name = "item", registry = "viscript_quests:quest_task")
public class ItemTask extends IQuestTask {
    @Configurable(name = "viscript_quests.task.item.itemStack")
    private ItemStack itemStack = ItemStack.EMPTY;
    @Configurable(name = "viscript_quests.task.item.itemTaskAction")
    private ItemTaskAction itemTaskAction = ItemTaskAction.GATHER;
    @Configurable(name = "viscript_quests.task.item.strictComponents")
    private boolean strictComponents = true;

    //runtime
    private int currentCount;

    @Override
    public String getType() {
        return "item";
    }

    @Override
    public IGuiTexture getIcon() {
        return new ItemStackTexture(Items.DIAMOND);
    }

    @Override
    public Component getName() {
        return Component.translatable("viscript_quests.task.item");
    }

    @Override
    public Component getProgressText() {
        return Component.translatable("viscript_quests.task.item.progressText",
                Component.translatable(itemTaskAction.name + ".progressText").getString(),
                Component.translatable(itemStack.getDescriptionId()).getString(),
                currentCount, itemStack.getCount());
    }

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
            return Component.translatable(name).getString();
        }
    }
}
