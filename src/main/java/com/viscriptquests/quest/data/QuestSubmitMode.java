package com.viscriptquests.quest.data;

import lombok.AllArgsConstructor;
import lombok.Getter;
import net.minecraft.util.StringRepresentable;
import org.jetbrains.annotations.NotNull;

//任务提交模式
@Getter
@AllArgsConstructor
public enum QuestSubmitMode implements StringRepresentable {
    AUTO("viscript_quests.quest.submit_mode.auto"),
    MANUAL("viscript_quests.quest.submit_mode.manual");

    private final String name;

    public boolean isAutoSubmit() {
        return this == AUTO;
    }

    @Override
    public @NotNull String getSerializedName() {
        return name;
    }
}
