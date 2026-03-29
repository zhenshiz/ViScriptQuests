package com.viscriptquests.command;

import com.lowdragmc.lowdraglib2.registry.ILDLRegister;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.viscriptquests.ViScriptQuests;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

import java.util.function.Supplier;

public interface ICommand extends ILDLRegister<ICommand, Supplier<ICommand>> {
    String COMMAND_ID = ViScriptQuests.MOD_ID + ":command";

    void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext buildContext, Commands.CommandSelection commandSelection);

    default CommandSyntaxException playerOnlyException() {
        return new CommandSyntaxException(CommandSyntaxException.BUILT_IN_EXCEPTIONS.dispatcherUnknownArgument(), Component.translatable("command.target.entity.only"));
    }

    default CommandSyntaxException entityOnlyException() {
        return new CommandSyntaxException(CommandSyntaxException.BUILT_IN_EXCEPTIONS.dispatcherUnknownArgument(), Component.translatable("command.target.player.only"));
    }
}
