package com.viscriptquests.command;

import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegister;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.viscriptquests.ViScriptQuests;
import com.viscriptquests.util.ViScriptQuestsServerUtil;
import lombok.SneakyThrows;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;

@LDLRegister(name = "quest", registry = ICommand.COMMAND_ID)
public class QuestCommand implements ICommand {
    @Override
    public void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext buildContext, Commands.CommandSelection commandSelection) {
        dispatcher.register(Commands.literal(ViScriptQuests.MOD_ID).requires(commandSourceStack -> commandSourceStack.hasPermission(Commands.LEVEL_OWNERS))
                .then(Commands.literal("editor")
                        .executes(this::openEditor)
                )
                .then(Commands.literal("openQuestBook")
                        .executes(this::openQuestBook)
                )
        );
    }

    @SneakyThrows
    private int openEditor(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        ServerPlayer player = source.getPlayer();
        if (player != null) {
            ViScriptQuestsServerUtil.openQuestEditor(player);
            return 1;
        } else {
            throw playerOnlyException();
        }
    }

    @SneakyThrows
    private int openQuestBook(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        ServerPlayer player = source.getPlayer();
        if (player != null) {
            ViScriptQuestsServerUtil.openQuestBook(player);
            return 1;
        } else {
            throw playerOnlyException();
        }
    }
}
