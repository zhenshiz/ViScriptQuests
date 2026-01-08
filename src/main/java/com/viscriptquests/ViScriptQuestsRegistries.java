package com.viscriptquests;

import com.lowdragmc.lowdraglib2.registry.AutoRegistry;
import com.viscriptquests.command.ICommand;
import com.viscriptquests.quest.reward.IQuestReward;
import com.viscriptquests.quest.task.IQuestTask;

import java.util.function.Supplier;

public class ViScriptQuestsRegistries {
    public static AutoRegistry.LDLibRegister<ICommand, Supplier<ICommand>> COMMANDS;

    public static AutoRegistry.LDLibRegister<IQuestTask, Supplier<IQuestTask>> QUEST_TASK;

    public static AutoRegistry.LDLibRegister<IQuestReward, Supplier<IQuestReward>> QUEST_REWARD;

    static {
        COMMANDS = AutoRegistry.LDLibRegister.create(ViScriptQuests.id("command"), ICommand.class, AutoRegistry::noArgsCreator);
        QUEST_TASK = AutoRegistry.LDLibRegister.create(ViScriptQuests.id("quest_task"), IQuestTask.class, AutoRegistry::noArgsCreator);
        QUEST_REWARD = AutoRegistry.LDLibRegister.create(ViScriptQuests.id("quest_reward"), IQuestReward.class, AutoRegistry::noArgsCreator);
    }
}
