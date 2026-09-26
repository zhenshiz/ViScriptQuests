package com.viscriptquests.event;

import com.viscriptquests.ViScriptQuests;
import com.viscriptquests.gui.hud.QuestHudLayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = ViScriptQuests.MOD_ID, value = Dist.CLIENT)
public class ClientHudEvents {
    @SubscribeEvent
    public static void registerGuiLayers(RenderGuiEvent.Post event) {
        QuestHudLayer.INSTANCE.render(null, event.getGuiGraphics(), event.getPartialTick(), 0, 0);
    }
}
