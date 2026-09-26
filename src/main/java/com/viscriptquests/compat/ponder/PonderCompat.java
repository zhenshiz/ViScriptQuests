package com.viscriptquests.compat.ponder;

import com.lowdragmc.lowdraglib2.Platform;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.loading.FMLEnvironment;

public final class PonderCompat {
    public static final String MOD_ID = "ponder";

    private PonderCompat() {
    }

    public static boolean isLoaded() {
        return FMLEnvironment.dist.isClient() && Platform.isModLoaded(MOD_ID);
    }

    public static boolean open(String componentId) {
        if (!FMLEnvironment.dist.isClient()) {
            return false;
        }
        if (!ModList.get().isLoaded(MOD_ID)) {
            PonderMessages.missing();
            return false;
        }
        return PonderApi.open(componentId);
    }
}
