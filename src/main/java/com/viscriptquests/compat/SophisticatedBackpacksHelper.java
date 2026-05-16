package com.viscriptquests.compat;

import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegister;
import com.viscriptquests.util.ItemUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.p3pp3rf1y.sophisticatedbackpacks.SophisticatedBackpacks;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.BackpackStorage;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.wrapper.BackpackWrapper;
import net.p3pp3rf1y.sophisticatedbackpacks.common.gui.BackpackContainer;
import net.p3pp3rf1y.sophisticatedbackpacks.common.gui.BackpackContext;
import net.p3pp3rf1y.sophisticatedbackpacks.network.BackpackContentsPayload;
import net.p3pp3rf1y.sophisticatedbackpacks.util.PlayerInventoryProvider;
import net.p3pp3rf1y.sophisticatedcore.init.ModCoreDataComponents;
import net.p3pp3rf1y.sophisticatedcore.inventory.InventoryHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * 精妙背包兼容
 */
@LDLRegister(name = SophisticatedBackpacks.MOD_ID, registry = IContainerHelper.CONTAINER_HELPER_ID, modID = SophisticatedBackpacks.MOD_ID)
public class SophisticatedBackpacksHelper implements IContainerHelper {
    @Override
    public int getItemStackCount(ServerPlayer player, ItemStack item, boolean strictComponents) {
        int count = 0;
        for (ItemStack itemStack : getItemsFromInventoryBackpack(player)) {
            if (ItemUtil.isSameItem(itemStack, item, strictComponents)) {
                count += itemStack.getCount();
            }
        }
        return count;
    }

    //从精妙背包中扣除指定物品
    @Override
    public int removeItemStackByCount(ServerPlayer player, ItemStack item, boolean strictComponents, int count) {
        if (count <= 0) return 0;

        final int[] remain = {count};
        for (ItemStack backpackItem : getAllInventoryBackpack(player)) {
            modifyInventoryBackpack(player, backpackItem, (inventoryHandler) -> {
                for (int i = 0; i < inventoryHandler.getSlots(); i++) {
                    if (remain[0] <= 0) break;
                    ItemStack stackInSlot = inventoryHandler.getStackInSlot(i);
                    if (ItemUtil.isSameItem(stackInSlot, item, strictComponents)) {
                        int canRemove = Math.min(stackInSlot.getCount(), remain[0]);
                        ItemStack removed = inventoryHandler.extractItem(i, canRemove, false);
                        remain[0] -= removed.getCount();
                    }
                }
            });
        }

        return remain[0];
    }

    //获取玩家所有背包中所有的物品，不包括玩家物品栏
    public static List<ItemStack> getItemsFromInventoryBackpack(Player player) {
        List<ItemStack> items = new ArrayList<>();
        getAllInventoryBackpack(player).forEach(itemStack -> {
            items.addAll(getItemsFromBackpackItem(itemStack));
        });
        return items;
    }


    //获取玩家背包中所有的背包
    public static List<ItemStack> getAllInventoryBackpack(Player player) {
        List<ItemStack> items = new ArrayList<>();
        PlayerInventoryProvider.get().runOnBackpacks(player, (backpack, inventoryName, identifier, index) -> {
            items.add(backpack);
            return false;
        });
        return items;
    }

    //获取背包中所有的物品
    public static List<ItemStack> getItemsFromBackpackItem(ItemStack itemStack) {
        List<ItemStack> items = new ArrayList<>();
        BackpackWrapper backpackWrapper = new BackpackWrapper(itemStack);
        InventoryHandler handler = backpackWrapper.getInventoryHandler();
        Integer size = itemStack.get(ModCoreDataComponents.NUMBER_OF_INVENTORY_SLOTS);
        if (size == null) return items;
        for (int i = 0; i < size; i++) {
            ItemStack item = handler.getStackInSlot(i);
            items.add(item);
        }
        return items;
    }

    public static void modifyInventoryBackpack(ServerPlayer player, ItemStack backpackItem, Consumer<IItemHandler> action) {
        PlayerInventoryProvider.get().runOnBackpacks(player, (backpack, inventoryName, identifier, index) -> {
            if (!backpack.equals(backpackItem)) return false;
            BackpackContext.Item backpackContext = new BackpackContext.Item(inventoryName, identifier, index);
            modifyBackpack(player, backpackContext, action);
            return false;
        });
    }

    public static void modifyBackpack(ServerPlayer player, BackpackContext backpackContext, Consumer<IItemHandler> action) {
        BackpackContainer container = new BackpackContainer(player.containerMenu.containerId + 1, player, backpackContext);
        int size = container.realInventorySlots.size() - player.getInventory().items.size();
        InventoryHandler inventoryHandler = container.getStorageWrapper().getInventoryHandler();
        action.accept(inventoryHandler);
        for (int i = 0; i < size; i++) {
            container.realInventorySlots.get(i).set(inventoryHandler.getStackInSlot(i));
        }
        UUID uuid = container.getStorageWrapper().getContentsUuid().get();
        CompoundTag backpackContent = BackpackStorage.get().getOrCreateBackpackContents(uuid);
        player.connection.send(new BackpackContentsPayload(uuid, backpackContent));
    }
}
