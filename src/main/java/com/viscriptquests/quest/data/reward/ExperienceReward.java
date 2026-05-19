package com.viscriptquests.quest.data.reward;

import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegister;
import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted;
import com.viscriptquests.quest.data.DisplayIcon;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Items;

// 经验奖励，完成小任务或大任务后给予玩家指定经验点数。
@LDLRegister(name = "experience_reward", registry = IReward.ID)
public class ExperienceReward extends IReward {
    @Persisted
    public int experience = 1;

    @Override
    public void grant(ServerPlayer player) {
        if (player != null) {
            player.giveExperiencePoints(Math.max(0, experience));
        }
    }

    @Override
    public Component getRewardHint() {
        return Component.translatable("viscript_quests.reward_hint.experience_reward", Math.max(0, experience));
    }

    @Override
    public DisplayIcon getRewardIcon() {
        return DisplayIcon.item(Items.EXPERIENCE_BOTTLE.getDefaultInstance());
    }
}
