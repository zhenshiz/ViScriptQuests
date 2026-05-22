package com.viscriptquests.quest.data.reward;

import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegister;
import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted;
import com.viscriptquests.quest.data.DisplayIcon;
import com.viscriptquests.quest.data.LootTableConfig;
import lombok.AllArgsConstructor;
import lombok.Getter;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import org.jetbrains.annotations.NotNull;

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

    public LootTableReward copyLootOptions() {
        LootTableReward reward = new LootTableReward();
        copyLootOptionsTo(reward);
        return reward;
    }

    public void copyLootOptionsTo(LootTableReward reward) {
        if (reward == null) {
            return;
        }
        reward.lootTableType = lootTableType == null ? LootTableType.DATA_PACK : lootTableType;
        reward.dataPackPath = dataPackPath == null ? "" : dataPackPath;
        reward.customLootTable.clear();
        for (LootTableConfig config : customLootTable) {
            if (config != null) {
                reward.customLootTable.add(config.copy());
            }
        }
    }

    public void normalizeForGrant() {
        lootTableType = lootTableType == null ? LootTableType.DATA_PACK : lootTableType;
        if (lootTableType == LootTableType.DATA_PACK) {
            dataPackPath = dataPackPath == null ? "" : dataPackPath.trim();
            customLootTable.clear();
            return;
        }
        dataPackPath = "";
    }

    @Override
    public void grant(ServerPlayer player) {
        if (player == null) {
            return;
        }
        normalizeForGrant();
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
        return rewardHintOrDefault(Component.translatable("viscript_quests.reward_hint.loot_table_reward"));
    }

    @Override
    public DisplayIcon getRewardIcon() {
        return rewardIconOrDefault(DisplayIcon.item(Items.CHEST.getDefaultInstance()));
    }

    @Getter
    @AllArgsConstructor
    public enum LootTableType implements StringRepresentable {
        DATA_PACK("viscript_quests.loot_table_type.data_pack"),
        CUSTOM("viscript_quests.loot_table_type.custom");

        private final String name;

        @Override
        public @NotNull String getSerializedName() {
            return name;
        }
    }
}
