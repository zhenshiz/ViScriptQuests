package com.viscriptquests.quest.data.reward;

import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegister;
import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted;
import com.viscriptquests.quest.data.DisplayIcon;
import com.viscriptquests.quest.data.QuestValueToken;
import com.viscriptquests.quest.data.QuestVariableValue;
import com.viscriptshop.ViscriptShop;
import com.viscriptshop.util.ViScriptShopServerUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

// ViScriptShop 货币奖励。
@LDLRegister(name = "currency_reward", registry = IReward.ID, modID = ViscriptShop.MOD_ID)
public class CurrencyReward extends IReward {
    @Persisted
    public int currency = 1;
    @Persisted
    public final List<QuestValueToken> currencyExpression = new ArrayList<>();

    @Override
    public void grant(ServerPlayer player) {
        grant(player, null);
    }

    @Override
    public void grant(ServerPlayer player, Map<String, QuestVariableValue> questVariables) {
        if (player != null) {
            ViScriptShopServerUtil.addMoney(player, resolveCurrency(questVariables, player));
        }
    }

    @Override
    public void resolveDynamicValues(Map<String, QuestVariableValue> questVariables, ServerPlayer player) {
        currency = resolveCurrency(questVariables, player);
        currencyExpression.clear();
    }

    @Override
    public Component getRewardHint() {
        return getRewardHint(null, null);
    }

    @Override
    public Component getRewardHint(ServerPlayer player, Map<String, QuestVariableValue> questVariables) {
        return rewardHintOrDefault(Component.translatable("viscript_quests.reward_hint.currency_reward",
                resolveCurrency(questVariables, player)));
    }

    @Override
    public DisplayIcon getRewardIcon() {
        return rewardIconOrDefault(DisplayIcon.item(Items.EMERALD.getDefaultInstance()));
    }

    public int resolveCurrency(Map<String, QuestVariableValue> questVariables, ServerPlayer player) {
        return QuestValueToken.evaluateInt(currencyExpression, questVariables, player, currency, 0);
    }
}
