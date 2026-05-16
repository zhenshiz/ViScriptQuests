package com.viscriptquests.util;

import com.lowdragmc.lowdraglib2.registry.AutoRegistry;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegister;
import com.viscriptquests.ViScriptQuestsRegistries;
import com.viscriptquests.compat.IContainerHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public final class ItemUtil {
    public static boolean isSameItem(ItemStack stack, ItemStack item, boolean strictComponents) {
        if (stack.isEmpty() || item.isEmpty()) {
            return false;
        }
        return strictComponents ? ItemStack.isSameItemSameComponents(stack, item) : stack.is(item.getItem());
    }

    //删除玩家物品，兼容背包，精妙背包，超越维度
    public static boolean removeItemForPlayer(ServerPlayer player, ItemStack itemStack, boolean strictComponents, int count) {
        if (player == null || itemStack.isEmpty() || count <= 0) {
            return true;
        }
        if (getItemForPlayerCount(player, itemStack, strictComponents) < count) {
            return false;
        }
        for (AutoRegistry.Holder<LDLRegister, IContainerHelper, Supplier<IContainerHelper>> containerHelperSupplierHolder : ViScriptQuestsRegistries.CONTAINER_HELPERS) {
            IContainerHelper iContainerHelper = containerHelperSupplierHolder.value().get();
            if (count > 0) {
                count = iContainerHelper.removeItemStackByCount(player, itemStack, strictComponents, count);
            }
        }
        player.containerMenu.broadcastChanges();
        return count <= 0;
    }

    //获取玩家物品，兼容背包，精妙背包，超越维度
    public static int getItemForPlayerCount(ServerPlayer player, ItemStack item, boolean strictComponents) {
        int count = 0;
        if (player != null) {
            for (AutoRegistry.Holder<LDLRegister, IContainerHelper, Supplier<IContainerHelper>> containerHelperSupplierHolder : ViScriptQuestsRegistries.CONTAINER_HELPERS) {
                IContainerHelper iContainerHelper = containerHelperSupplierHolder.value().get();
                count += iContainerHelper.getItemStackCount(player, item, strictComponents);
            }
        }
        return count;
    }

    /**
     * 获取物品数量
     *
     * @param container 背包
     * @param item      物品
     * @return 该物品在背包里的数量
     */
    public static int getItemCountByContainer(Container container, ItemStack item, boolean strictComponents) {
        int count = 0;
        if (container == null || item.isEmpty()) {
            return count;
        }
        for (int i = 0; i < container.getContainerSize(); i++) {
            ItemStack stack = container.getItem(i);
            if (isSameItem(stack, item, strictComponents)) {
                count += stack.getCount();
            }
        }

        return count;
    }

    /**
     * 删除物品
     *
     * @param container 背包
     * @param item      要删的物品
     * @param count     要求数量
     * @return 删了后还有的数量
     */
    public static int removeItemByContainer(Container container, ItemStack item, boolean strictComponents, int count) {
        if (container == null || item.isEmpty() || count <= 0) {
            return Math.max(count, 0);
        }
        for (int i = 0; i < container.getContainerSize() && count > 0; i++) {
            ItemStack stack = container.getItem(i);
            if (isSameItem(stack, item, strictComponents)) {
                int toRemove = Math.min(count, stack.getCount());
                stack.shrink(toRemove);
                if (stack.isEmpty()) {
                    container.setItem(i, ItemStack.EMPTY);
                }
                count -= toRemove;
            }
        }
        container.setChanged();
        return count;
    }

    public static PlayerItemCountCache createPlayerItemCountCache(ServerPlayer player) {
        return new PlayerItemCountCache(player);
    }

    // 自动提交一次检查内复用的物品数量缓存，避免多个目标反复遍历所有兼容容器。
    public static class PlayerItemCountCache {
        private final ServerPlayer player;
        private final Map<ItemKey, Integer> counts = new HashMap<>();

        private PlayerItemCountCache(ServerPlayer player) {
            this.player = player;
        }

        public int getItemCount(ItemStack itemStack, boolean strictComponents) {
            if (itemStack.isEmpty()) {
                return 0;
            }
            ItemKey key = strictComponents ? ItemKey.strict(itemStack) : ItemKey.loose(itemStack);
            return counts.computeIfAbsent(key, ignored -> ItemUtil.getItemForPlayerCount(player, itemStack, strictComponents));
        }
    }

    private record ItemKey(Object item, Object components) {
        private static ItemKey loose(ItemStack stack) {
            return new ItemKey(stack.getItem(), null);
        }

        private static ItemKey strict(ItemStack stack) {
            return new ItemKey(stack.getItem(), stack.getComponents());
        }
    }
}
