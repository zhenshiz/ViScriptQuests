package com.viscriptquests.event;

import com.viscriptquests.ViScriptQuests;
import com.viscriptquests.gui.hud.QuestHudLayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

@EventBusSubscriber(modid = ViScriptQuests.MOD_ID, value = Dist.CLIENT)
public class ClientHudEvents {
    @SubscribeEvent
    public static void registerGuiLayers(RegisterGuiLayersEvent event) {
        event.registerAbove(VanillaGuiLayers.HOTBAR, QuestHudLayer.ID, QuestHudLayer.INSTANCE);
    }
}
