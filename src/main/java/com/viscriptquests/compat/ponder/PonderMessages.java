package com.viscriptquests.compat.ponder;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

final class PonderMessages {
    private PonderMessages() {
    }

    static void missing() {
        send(Component.translatable("viscript_quests.ponder.missing"));
    }

    static void invalidComponent(String componentId) {
        send(Component.translatable("viscript_quests.ponder.invalid_component", componentId));
    }

    static void noScene(String componentId) {
        send(Component.translatable("viscript_quests.ponder.no_scene", componentId));
    }

    static void openFailed(String message) {
        send(Component.translatable("viscript_quests.ponder.open_failed", message));
    }

    private static void send(Component message) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player != null) {
            minecraft.player.displayClientMessage(message, false);
        }
    }
}
