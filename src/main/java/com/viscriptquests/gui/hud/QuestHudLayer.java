package com.viscriptquests.gui.hud;

import com.lowdragmc.lowdraglib2.gui.hud.ModularHudLayer;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.viscriptquests.ViScriptQuests;
import net.minecraft.resources.ResourceLocation;

public enum QuestHudLayer implements ModularHudLayer {
    INSTANCE;

    public static final ResourceLocation ID = ViScriptQuests.id("tracked_quest_hud");

    private final ModularUI modularUI = new ModularUI(UI.of(new TrackedQuestHud()));

    @Override
    public ModularUI getModularUI() {
        return modularUI;
    }
}
