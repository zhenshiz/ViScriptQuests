package com.viscriptquests;

import com.lowdragmc.lowdraglib2.registry.AutoRegistry;
import com.viscriptquests.command.ICommand;
import com.viscriptquests.quest.reward.IQuestReward;
import com.viscriptquests.quest.task.IQuestTask;
import net.minecraft.resources.ResourceLocation;

import java.util.function.Supplier;

public class ViScriptQuestsRegistries {
    public static AutoRegistry.LDLibRegister<ICommand, Supplier<ICommand>> COMMANDS;

    public static AutoRegistry.LDLibRegister<IQuestTask, Supplier<IQuestTask>> QUEST_TASK;

    public static AutoRegistry.LDLibRegister<IQuestReward, Supplier<IQuestReward>> QUEST_REWARD;

    static {
        COMMANDS = AutoRegistry.LDLibRegister.create(ResourceLocation.parse(ICommand.COMMAND_ID), ICommand.class, AutoRegistry::noArgsCreator);
        QUEST_TASK = AutoRegistry.LDLibRegister.create(ResourceLocation.parse(IQuestTask.ID), IQuestTask.class, AutoRegistry::noArgsCreator);
        QUEST_REWARD = AutoRegistry.LDLibRegister.create(ResourceLocation.parse(IQuestReward.ID), IQuestReward.class, AutoRegistry::noArgsCreator);
    }
}
