package com.viscriptquests.gui.data;

import com.mojang.blaze3d.MethodsReturnNonnullByDefault;
import com.viscriptquests.quest.QuestDefinition;
import lombok.Getter;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.neoforged.neoforge.common.util.INBTSerializable;

import javax.annotation.ParametersAreNonnullByDefault;

@Getter
@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class Quest implements INBTSerializable<CompoundTag> {
    public static final String SUFFIX = ".quest";
    public QuestDefinition questDefinition;

    public Quest() {
        questDefinition = new QuestDefinition();
    }

    @Override
    public CompoundTag serializeNBT(HolderLookup.Provider provider) {
        return questDefinition.serializeNBT(provider);
    }

    @Override
    public void deserializeNBT(HolderLookup.Provider provider, CompoundTag tag) {
        questDefinition.deserializeNBT(provider, tag);
    }
}
