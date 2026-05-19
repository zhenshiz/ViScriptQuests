package com.viscriptquests.quest.data;

import lombok.AllArgsConstructor;
import lombok.Getter;
import net.minecraft.util.StringRepresentable;
import org.jetbrains.annotations.NotNull;

@Getter
@AllArgsConstructor
public enum LootTableType implements StringRepresentable {
    DATA_PACK("viscript_quests.loot_table_type.data_pack"),
    CUSTOM("viscript_quests.loot_table_type.custom");

    private final String name;

    @Override
    public @NotNull String getSerializedName() {
        return name;
    }
}
