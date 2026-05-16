package com.viscriptquests.compat;

import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegister;
import com.wintercogs.beyonddimensions.api.dimensionnet.DimensionsNet;
import com.wintercogs.beyonddimensions.api.dimensionnet.UnifiedStorage;
import com.wintercogs.beyonddimensions.api.ids.BDConstants;
import com.wintercogs.beyonddimensions.api.storage.key.IStackKey;
import com.wintercogs.beyonddimensions.api.storage.key.KeyAmount;
import com.wintercogs.beyonddimensions.api.storage.key.impl.ItemStackKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/**
 * 超越维度兼容维度背包
 */
@LDLRegister(name = BDConstants.MODID, registry = IContainerHelper.CONTAINER_HELPER_ID, modID = BDConstants.MODID)
public class BeyondDimensionsHelper implements IContainerHelper {
    @Override
    public int getItemStackCount(ServerPlayer player, ItemStack item, boolean strictComponents) {
        DimensionsNet net = DimensionsNet.getNetFromPlayer(player);
        if (net != null) {
            UnifiedStorage storage = net.getUnifiedStorage();
            IStackKey<?> key = new ItemStackKey(item);
            KeyAmount keyAmount = storage.getStackByKey(key);
            return Math.clamp(keyAmount.amount(), 0, Integer.MAX_VALUE);
        }
        return 0;
    }

    @Override
    public int removeItemStackByCount(ServerPlayer player, ItemStack item, boolean strictComponents, int count) {
        DimensionsNet net = DimensionsNet.getNetFromPlayer(player);
        if (net == null || count <= 0) return 0;

        var storage = net.getUnifiedStorage();

        IStackKey<?> key = new ItemStackKey(item);

        KeyAmount extracted = storage.extract(key, count, false, false);

        return count - Math.clamp(extracted.amount(), 0, Integer.MAX_VALUE);
    }
}
