package com.viscriptquests.compat.ponder;

import com.viscriptquests.ViScriptQuests;
import net.createmod.catnip.gui.ScreenOpener;
import net.createmod.ponder.foundation.PonderIndex;
import net.createmod.ponder.foundation.ui.PonderUI;
import net.minecraft.resources.ResourceLocation;

final class PonderApi {
    private PonderApi() {
    }

    static boolean open(String componentId) {
        String normalized = componentId == null ? "" : componentId.trim();
        ResourceLocation id = ResourceLocation.tryParse(normalized);
        if (id == null) {
            PonderMessages.invalidComponent(normalized);
            return false;
        }
        if (!PonderIndex.getSceneAccess().doScenesExistForId(id)) {
            PonderMessages.noScene(id.toString());
            return false;
        }
        try {
            ScreenOpener.open(PonderUI.of(id));
            return true;
        } catch (Exception e) {
            ViScriptQuests.LOGGER.error("Failed to open Ponder scene for {}", id, e);
            PonderMessages.openFailed(e.getMessage() == null ? id.toString() : e.getMessage());
            return false;
        }
    }
}
