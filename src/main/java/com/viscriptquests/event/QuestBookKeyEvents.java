package com.viscriptquests.event;

import com.lowdragmc.lowdraglib2.networking.rpc.RPCPacketDistributor;
import com.lowdragmc.lowdraglib2.gui.holder.ModularUIScreen;
import com.mojang.blaze3d.platform.InputConstants;
import com.viscriptquests.ViScriptQuests;
import com.viscriptquests.config.ClientConfig;
import com.viscriptquests.compat.jei.JeiHelper;
import com.viscriptquests.gui.QuestBookUI;
import com.viscriptquests.network.c2s.C2SPayload;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import org.lwjgl.glfw.GLFW;

@EventBusSubscriber(modid = ViScriptQuests.MOD_ID, value = Dist.CLIENT)
public class QuestBookKeyEvents {
    private static final KeyMapping OPEN_QUEST_BOOK = new KeyMapping(
            "key.viscript_quests.open_quest_book",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_J,
            "key.categories.viscript_quests");
    private static boolean registered;

    @SubscribeEvent
    public static void onQuestBookKeyPressed(ScreenEvent.KeyPressed.Pre event) {
        if (!(event.getScreen() instanceof ModularUIScreen screen)
                || !(screen.modularUI.ui.rootElement instanceof QuestBookUI book)) return;

        var minecraft = Minecraft.getInstance();
        var key = InputConstants.getKey(event.getKeyCode(), event.getScanCode());
        if (minecraft.options.keyInventory.isActiveAndMatches(key)
                || (registered && OPEN_QUEST_BOOK.isActiveAndMatches(key))) {
            screen.onClose();
            event.setCanceled(true);
            return;
        }
        if (ModList.get().isLoaded("jei") && JeiHelper.handleRecipeLookupKey(
                book.getRecipeLookupItem(screen.modularUI.getLastHoveredElement()),
                event.getKeyCode(), event.getScanCode())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onQuestBookMousePressed(ScreenEvent.MouseButtonPressed.Pre event) {
        if (!(event.getScreen() instanceof ModularUIScreen screen)
                || !(screen.modularUI.ui.rootElement instanceof QuestBookUI book)
                || !ModList.get().isLoaded("jei")) return;

        var target = screen.modularUI.hitTestAtScreen((float) event.getMouseX(), (float) event.getMouseY());
        if (JeiHelper.handleRecipeLookupMouse(book.getRecipeLookupItem(target), event.getButton())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void registerKeyMappings(RegisterKeyMappingsEvent event) {
        if (!ClientConfig.REGISTER_OPEN_QUEST_BOOK_KEY.get()) {
            return;
        }
        registered = true;
        event.register(OPEN_QUEST_BOOK);
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        if (!registered) {
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
