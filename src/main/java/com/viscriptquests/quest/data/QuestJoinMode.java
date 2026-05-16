package com.viscriptquests.quest.data;

import lombok.AllArgsConstructor;
import lombok.Getter;
import net.minecraft.util.StringRepresentable;
import org.jetbrains.annotations.NotNull;

@Getter
@AllArgsConstructor
public enum QuestJoinMode implements StringRepresentable {
    ANY("viscript_quests.join_mode.any"),
    ALL("viscript_quests.join_mode.all"),
    COUNT("viscript_quests.join_mode.count");

    private final String name;

    @Override
    public @NotNull String getSerializedName() {
        return name;
    }
}
