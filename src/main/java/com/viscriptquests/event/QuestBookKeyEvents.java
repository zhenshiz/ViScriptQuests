package com.viscriptquests.event;

import com.lowdragmc.lowdraglib2.networking.rpc.RPCPacketDistributor;
import com.mojang.blaze3d.platform.InputConstants;
import com.viscriptquests.ViScriptQuests;
import com.viscriptquests.config.ClientConfig;
import com.viscriptquests.network.c2s.C2SPayload;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

@Mod.EventBusSubscriber(modid = ViScriptQuests.MOD_ID, value = Dist.CLIENT)
public class QuestBookKeyEvents {
    private static final KeyMapping OPEN_QUEST_BOOK = new KeyMapping(
            "key.viscript_quests.open_quest_book",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_J,
            "key.categories.viscript_quests");
    private static boolean registered;

    @SubscribeEvent
    public static void registerKeyMappings(RegisterKeyMappingsEvent event) {
        if (!ClientConfig.REGISTER_OPEN_QUEST_BOOK_KEY.get()) {
            return;
        }
        registered = true;
        event.register(OPEN_QUEST_BOOK);
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (!registered || event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.screen != null) {
            return;
        }
        while (OPEN_QUEST_BOOK.consumeClick()) {
            RPCPacketDistributor.rpcToServer(C2SPayload.REQUEST_OPEN_QUEST_BOOK);
        }
    }
}
