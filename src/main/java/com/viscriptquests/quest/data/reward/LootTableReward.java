package com.viscriptquests.quest.data.reward;

import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegister;
import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted;
import com.viscriptquests.quest.data.DisplayIcon;
import com.viscriptquests.quest.data.LootTableConfig;
import com.viscriptquests.quest.data.LootTableType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.neoforged.neoforge.items.ItemHandlerHelper;

import java.util.ArrayList;
import java.util.List;

// 战利品表奖励：可读取数据包 loot table，也可用内置的简单概率表抽取物品。
@LDLRegister(name = "loot_table_reward", registry = IReward.ID)
public class LootTableReward extends IReward {
    @Persisted
    public LootTableType lootTableType = LootTableType.DATA_PACK;
    @Persisted
    public String dataPackPath = "";
    @Persisted
    public final List<LootTableConfig> customLootTable = new ArrayList<>();

    @Override
    public void grant(ServerPlayer player) {
        if (player == null) {
            return;
        }
        if (lootTableType == LootTableType.CUSTOM) {
            grantCustomLoot(player);
            return;
        }
        grantDataPackLoot(player);
    }

    private void grantDataPackLoot(ServerPlayer player) {
        ResourceLocation location = ResourceLocation.tryParse(dataPackPath == null ? "" : dataPackPath.trim());
        if (location == null) {
            return;
        }
        ResourceKey<LootTable> key = ResourceKey.create(Registries.LOOT_TABLE, location);
        LootParams lootParams = new LootParams.Builder(player.serverLevel())
                .withParameter(LootContextParams.THIS_ENTITY, player)
                .withParameter(LootContextParams.ORIGIN, player.position())
                .withLuck(player.getLuck())
                .create(LootContextParamSets.ADVANCEMENT_REWARD);
        for (ItemStack stack : player.getServer().reloadableRegistries().getLootTable(key).getRandomItems(lootParams)) {
            giveItem(player, stack);
        }
    }

    private void grantCustomLoot(ServerPlayer player) {
        for (LootTableConfig config : customLootTable) {
            if (config == null || config.itemStack == null || config.itemStack.isEmpty()) {
                continue;
            }
            if (player.getRandom().nextFloat() <= config.clampedProbability()) {
                giveItem(player, config.itemStack.copy());
            }
        }
    }

    private static void giveItem(ServerPlayer player, ItemStack stack) {
        if (stack != null && !stack.isEmpty()) {
            ItemHandlerHelper.giveItemToPlayer(player, stack.copy());
        }
    }

    @Override
    public Component getRewardHint() {
        return Component.translatable("viscript_quests.reward_hint.loot_table_reward");
    }

    @Override
    public DisplayIcon getRewardIcon() {
        return DisplayIcon.item(Items.CHEST.getDefaultInstance());
    }
}
