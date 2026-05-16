package com.viscriptquests.compat;

import com.lowdragmc.lowdraglib2.registry.ILDLRegister;
import com.viscriptquests.ViScriptQuests;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.function.Supplier;

// 玩家物品来源扩展点。默认支持背包和末影箱，附属兼容可以注册更多容器来源。
public interface IContainerHelper extends ILDLRegister<IContainerHelper, Supplier<IContainerHelper>> {
    String CONTAINER_HELPER_ID = ViScriptQuests.MOD_ID + ":container_helper";

    /**
     * 获取物品的数量
     *
     * @param player 玩家
     * @param item   物品
     * @return 该物品的数量
     */
    int getItemStackCount(ServerPlayer player, ItemStack item, boolean strictComponents);

    /**
     * 删除物品
     *
     * @param player 玩家
     * @param item   物品
     * @param count  要删除的物品数量
     * @return 删除后剩余的数量
     */
    int removeItemStackByCount(ServerPlayer player, ItemStack item, boolean strictComponents, int count);
}
