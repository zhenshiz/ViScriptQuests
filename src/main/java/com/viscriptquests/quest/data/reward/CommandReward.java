package com.viscriptquests.quest.data.reward;

import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegister;
import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted;
import com.viscriptquests.quest.data.DisplayIcon;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Items;

// 指令奖励，以领奖玩家作为命令源依次执行服务端指令，方便指令中的 @s 指向该玩家。
@LDLRegister(name = "command_reward", registry = IReward.ID)
public class CommandReward extends IReward {
    @Persisted
    public String command = "";

    @Override
    public void grant(ServerPlayer player) {
        if (player == null) {
            return;
        }
        String commandText = command == null ? "" : command.trim();
        if (commandText.isEmpty()) {
            return;
        }
        var commandSource = player.createCommandSourceStack().withPermission(2).withSuppressedOutput();
        for (String entry : commandText.split(";")) {
            String currentCommand = entry.trim();
            if (currentCommand.isEmpty()) {
                continue;
            }
            player.getServer().getCommands().performPrefixedCommand(commandSource, currentCommand);
        }
    }

    @Override
    public Component getRewardHint() {
        return rewardHintOrDefault(Component.translatable("viscript_quests.reward_hint.command_reward"));
    }

    @Override
    public DisplayIcon getRewardIcon() {
        return rewardIconOrDefault(DisplayIcon.item(Items.COMMAND_BLOCK.getDefaultInstance()));
    }
}
