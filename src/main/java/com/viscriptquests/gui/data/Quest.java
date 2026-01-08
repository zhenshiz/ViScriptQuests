package com.viscriptquests.gui.data;

import com.mojang.blaze3d.MethodsReturnNonnullByDefault;
import com.viscriptquests.quest.QuestInfo;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.common.util.INBTSerializable;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

@Getter
@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class Quest implements INBTSerializable<CompoundTag> {
    public static final String SUFFIX = ".quest";
    @Nullable
    @Setter
    private ResourceLocation questLocation;
    public QuestInfo questInfo;

    public Quest() {
        questInfo = new QuestInfo();
    }

    @Override
    public CompoundTag serializeNBT(HolderLookup.Provider provider) {
        return questInfo.serializeNBT(provider);
    }

    @Override
    public void deserializeNBT(HolderLookup.Provider provider, CompoundTag tag) {
        questInfo.deserializeNBT(provider, tag);
    }
}
