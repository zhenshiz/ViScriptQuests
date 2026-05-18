package com.viscriptquests.gui.blueprint.compiler;

import net.minecraft.network.chat.Component;

public class QuestBlueprintValidationException extends IllegalStateException {
    public QuestBlueprintValidationException(Component message) {
        super(message.getString());
    }

    public static QuestBlueprintValidationException create(String key, Object... args) {
        return new QuestBlueprintValidationException(Component.translatable(key, args));
    }
}
