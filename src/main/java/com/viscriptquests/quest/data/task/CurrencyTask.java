package com.viscriptquests.quest.data.task;

import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegister;
import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted;
import com.viscriptquests.quest.data.DisplayIcon;
import com.viscriptquests.quest.data.QuestSubmitMode;
import com.viscriptquests.quest.data.QuestValueToken;
import com.viscriptquests.quest.data.QuestVariableValue;
import com.viscriptquests.quest.data.runtime.TaskObjectiveProgress;
import com.viscriptshop.ViscriptShop;
import com.viscriptshop.util.ViScriptShopServerUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** A VSS balance requirement or currency submission, using the same modes as an item objective. */
@LDLRegister(name = "currency_task", registry = ITask.ID, modID = ViscriptShop.MOD_ID)
public class CurrencyTask extends ITask {
    @Persisted
    public int currency = 1;
    @Persisted
    public final List<QuestValueToken> currencyExpression = new ArrayList<>();
    @Persisted
    public boolean consumeCurrency = true;
    @Persisted
    public QuestSubmitMode submitMode = QuestSubmitMode.AUTO;

    @Override
    public boolean checkCompletion(ServerPlayer player) { return checkCompletion(player, null); }

    @Override
    public boolean checkCompletion(ServerPlayer player, Map<String, QuestVariableValue> questVariables) {
        return player != null && getPlayerCurrency(player) >= getRequiredAmount(questVariables, player);
    }

    @Override
    public boolean onComplete(ServerPlayer player) { return onComplete(player, null); }

    @Override
    public boolean onComplete(ServerPlayer player, Map<String, QuestVariableValue> questVariables) {
        int required = getRequiredAmount(questVariables, player);
        if (player == null || getPlayerCurrency(player) < required) return false;
        if (!consumeCurrency) return true;
        int paid = withdrawWholeCurrency(player, required);
        if (paid == required) return true;
        // An economy backend may deduct less than requested. A failed auto-submit must not spend money.
        if (paid > 0) ViScriptShopServerUtil.addMoney(player, paid);
        return false;
    }

    @Override
    public boolean allowsAutoSubmit() { return submitMode.isAutoSubmit(); }

    @Override
    public int getRequiredAmount() { return getRequiredAmount(null, null); }

    @Override
    public int getRequiredAmount(Map<String, QuestVariableValue> questVariables, ServerPlayer player) {
        return QuestValueToken.evaluateInt(currencyExpression, questVariables, player, currency, 1);
    }

    @Override
    public void refreshObjectiveProgress(ServerPlayer player, TaskObjectiveProgress progress) {
        refreshObjectiveProgress(player, progress, null);
    }

    @Override
    public void refreshObjectiveProgress(ServerPlayer player, TaskObjectiveProgress progress,
                                         Map<String, QuestVariableValue> questVariables) {
        int required = getRequiredAmount(questVariables, player);
        progress.requiredAmount = required;
        if (progress.isCompleted()) progress.currentAmount = required;
        else if (progress.manualSubmitRequired) progress.currentAmount = Math.clamp(progress.currentAmount, 0, required);
        else progress.currentAmount = Math.min(required, getPlayerCurrency(player));
    }

    @Override
    public boolean submitObjective(ServerPlayer player, TaskObjectiveProgress progress) {
        return submitObjective(player, progress, null);
    }

    @Override
    public boolean submitObjective(ServerPlayer player, TaskObjectiveProgress progress,
                                   Map<String, QuestVariableValue> questVariables) {
        if (player == null || submitMode.isAutoSubmit() || !progress.isActive()) return false;
        int required = getRequiredAmount(questVariables, player);
        int current = Math.clamp(progress.currentAmount, 0, required);
        int remaining = required - current;
        if (remaining == 0) {
            progress.currentAmount = required;
            progress.requiredAmount = required;
            progress.complete();
            return true;
        }
        int available = getPlayerCurrency(player);
        int next;
        if (consumeCurrency) {
            int paid = withdrawWholeCurrency(player, Math.min(available, remaining));
            if (paid == 0) return false;
            next = current + paid;
        } else {
            next = Math.min(required, Math.max(current, available));
            if (next <= current) return false;
        }
        progress.currentAmount = next;
        progress.requiredAmount = required;
        if (next >= required) progress.complete();
        return true;
    }

    @Override
    protected Component getDefaultTaskHint() { return getDefaultTaskHint(null, null); }

    @Override
    protected Component getDefaultTaskHint(ServerPlayer player, Map<String, QuestVariableValue> questVariables) {
        return Component.translatable(consumeCurrency ? "viscript_quests.task_hint.currency_task.submit"
                        : "viscript_quests.task_hint.currency_task.have", getRequiredAmount(questVariables, player));
    }

    @Override
    public DisplayIcon getDisplayIcon() { return DisplayIcon.item(Items.EMERALD.getDefaultInstance()); }

    private static int getPlayerCurrency(ServerPlayer player) {
        if (player == null) return 0;
        // Objective progress is integral. Keep fractional balances in the player's wallet.
        return (int) Math.clamp(Math.floor(ViScriptShopServerUtil.getMoney(player)), 0, Integer.MAX_VALUE);
    }

    private static int withdrawWholeCurrency(ServerPlayer player, int requested) {
        if (requested <= 0) return 0;
        double removed = ViScriptShopServerUtil.removeMoney(player, requested, false);
        int paid = (int) Math.clamp(Math.floor(removed), 0, requested);
        double remainder = removed - paid;
        if (remainder > 0) ViScriptShopServerUtil.addMoney(player, remainder);
        return paid;
    }
}
