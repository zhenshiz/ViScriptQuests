package com.viscriptquests.quest.data.reward;

import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegister;
import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted;
import com.viscriptquests.quest.data.DisplayIcon;
import com.viscriptquests.quest.data.QuestValueToken;
import com.viscriptquests.quest.data.QuestVariableValue;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

// 经验奖励，完成小任务或大任务后给予玩家指定经验点数。
@LDLRegister(name = "experience_reward", registry = IReward.ID)
public class ExperienceReward extends IReward {
    @Persisted
    public int experience = 1;
    @Persisted
    public final List<QuestValueToken> experienceExpression = new ArrayList<>();

    @Override
    public void grant(ServerPlayer player) {
        grant(player, null);
    }

    @Override
    public void grant(ServerPlayer player, Map<String, QuestVariableValue> questVariables) {
        if (player != null) {
            player.giveExperiencePoints(resolveExperience(questVariables, player));
        }
    }

    @Override
    public void resolveDynamicValues(Map<String, QuestVariableValue> questVariables, ServerPlayer player) {
        experience = resolveExperience(questVariables, player);
        experienceExpression.clear();
    }

    @Override
    public Component getRewardHint() {
        return getRewardHint(null, null);
    }

    @Override
    public Component getRewardHint(ServerPlayer player, Map<String, QuestVariableValue> questVariables) {
        return rewardHintOrDefault(Component.translatable("viscript_quests.reward_hint.experience_reward",
                resolveExperience(questVariables, player)));
    }

    @Override
    public DisplayIcon getRewardIcon() {
        return rewardIconOrDefault(DisplayIcon.item(Items.EXPERIENCE_BOTTLE.getDefaultInstance()));
    }

    public int resolveExperience(Map<String, QuestVariableValue> questVariables, ServerPlayer player) {
        return QuestValueToken.evaluateInt(experienceExpression, questVariables, player, experience, 0);
    }
}
