package com.viscriptquests.quest.data.reward;

import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegister;
import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted;
import com.viscriptquests.quest.data.DisplayIcon;
import com.viscriptshop.ViscriptShop;
import com.viscriptshop.util.ViScriptShopServerUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Items;

// ViScriptShop 货币奖励。
@LDLRegister(name = "currency_reward", registry = IReward.ID, modID = ViscriptShop.MOD_ID)
public class CurrencyReward extends IReward {
    @Persisted
    public int currency = 1;

    @Override
    public void grant(ServerPlayer player) {
        ViScriptShopServerUtil.addMoney(player, Math.max(0, currency));
    }

    @Override
    public Component getRewardHint() {
        return rewardHintOrDefault(Component.translatable("viscript_quests.reward_hint.currency_reward", Math.max(0, currency)));
    }

    @Override
    public DisplayIcon getRewardIcon() {
        return rewardIconOrDefault(DisplayIcon.item(Items.EMERALD.getDefaultInstance()));
    }
}
