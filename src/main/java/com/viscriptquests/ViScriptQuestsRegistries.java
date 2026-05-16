package com.viscriptquests;

import com.lowdragmc.lowdraglib2.registry.AutoRegistry;
import com.viscriptquests.command.ICommand;
import com.viscriptquests.compat.IContainerHelper;
import com.viscriptquests.gui.blueprint.compiler.IQuestExpressionNodeCompiler;
import com.viscriptquests.gui.blueprint.compiler.IQuestPassthroughNodeCompiler;
import com.viscriptquests.gui.blueprint.compiler.IQuestRewardNodeCompiler;
import com.viscriptquests.gui.blueprint.compiler.IQuestTaskNodeCompiler;
import com.viscriptquests.quest.data.reward.IReward;
import com.viscriptquests.quest.data.task.ITask;
import net.minecraft.resources.ResourceLocation;

import java.util.function.Supplier;

public class ViScriptQuestsRegistries {
    public static AutoRegistry.LDLibRegister<ICommand, Supplier<ICommand>> COMMANDS;
    public static AutoRegistry.LDLibRegister<IContainerHelper, Supplier<IContainerHelper>> CONTAINER_HELPERS;
    public static AutoRegistry.LDLibRegister<ITask, Supplier<ITask>> TASKS;
    public static AutoRegistry.LDLibRegister<IReward, Supplier<IReward>> REWARDS;
    public static AutoRegistry.LDLibRegister<IQuestTaskNodeCompiler, Supplier<IQuestTaskNodeCompiler>> BLUEPRINT_TASK_NODE_COMPILERS;
    public static AutoRegistry.LDLibRegister<IQuestRewardNodeCompiler, Supplier<IQuestRewardNodeCompiler>> BLUEPRINT_REWARD_NODE_COMPILERS;
    public static AutoRegistry.LDLibRegister<IQuestExpressionNodeCompiler, Supplier<IQuestExpressionNodeCompiler>> BLUEPRINT_EXPRESSION_NODE_COMPILERS;
    public static AutoRegistry.LDLibRegister<IQuestPassthroughNodeCompiler, Supplier<IQuestPassthroughNodeCompiler>> BLUEPRINT_PASSTHROUGH_NODE_COMPILERS;

    static {
        COMMANDS = AutoRegistry.LDLibRegister.create(ResourceLocation.parse(ICommand.COMMAND_ID), ICommand.class, AutoRegistry::noArgsCreator);
        CONTAINER_HELPERS = AutoRegistry.LDLibRegister.create(ResourceLocation.parse(IContainerHelper.CONTAINER_HELPER_ID), IContainerHelper.class, AutoRegistry::noArgsCreator);
        TASKS = AutoRegistry.LDLibRegister.create(ResourceLocation.parse(ITask.ID), ITask.class, AutoRegistry::noArgsCreator);
        REWARDS = AutoRegistry.LDLibRegister.create(ResourceLocation.parse(IReward.ID), IReward.class, AutoRegistry::noArgsCreator);

        BLUEPRINT_TASK_NODE_COMPILERS = AutoRegistry.LDLibRegister.create(ResourceLocation.parse(IQuestTaskNodeCompiler.ID), IQuestTaskNodeCompiler.class, AutoRegistry::noArgsCreator);
        BLUEPRINT_REWARD_NODE_COMPILERS = AutoRegistry.LDLibRegister.create(ResourceLocation.parse(IQuestRewardNodeCompiler.ID), IQuestRewardNodeCompiler.class, AutoRegistry::noArgsCreator);
        BLUEPRINT_EXPRESSION_NODE_COMPILERS = AutoRegistry.LDLibRegister.create(ResourceLocation.parse(IQuestExpressionNodeCompiler.ID), IQuestExpressionNodeCompiler.class, AutoRegistry::noArgsCreator);
        BLUEPRINT_PASSTHROUGH_NODE_COMPILERS = AutoRegistry.LDLibRegister.create(ResourceLocation.parse(IQuestPassthroughNodeCompiler.ID), IQuestPassthroughNodeCompiler.class, AutoRegistry::noArgsCreator);
    }
}
