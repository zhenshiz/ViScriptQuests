package com.viscriptquests.quest.reward;

import com.lowdragmc.lowdraglib2.Platform;
import com.lowdragmc.lowdraglib2.configurator.IConfigurable;
import com.lowdragmc.lowdraglib2.registry.ILDLRegister;
import com.lowdragmc.lowdraglib2.syncdata.IPersistedSerializable;
import com.lowdragmc.lowdraglib2.utils.LDLibExtraCodecs;
import com.lowdragmc.lowdraglib2.utils.PersistedParser;
import com.mojang.serialization.Codec;
import com.viscriptquests.ViScriptQuests;
import com.viscriptquests.ViScriptQuestsRegistries;
import com.viscriptquests.util.CodecUtil;
import io.netty.buffer.ByteBuf;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import javax.annotation.Nullable;
import java.util.function.Supplier;

public abstract class IQuestReward implements ILDLRegister<IQuestReward, Supplier<IQuestReward>>, IPersistedSerializable, IConfigurable {
    Codec<IQuestReward> CODEC = ViScriptQuestsRegistries.QUEST_REWARD.optionalCodec().dispatch(ILDLRegister::getRegistryHolderOptional,
            optional -> optional.map(holder -> PersistedParser.createCodec(holder.value()).fieldOf("data"))
                    .orElseGet(LDLibExtraCodecs::errorDecoder));
    StreamCodec<ByteBuf, IQuestReward> STREAM_CODEC = ByteBufCodecs.fromCodec(CODEC);
    public static final String ID = ViScriptQuests.MOD_ID + ":reward";

    @Nullable
    public CompoundTag serializeWrapper() {
        return (CompoundTag) CodecUtil.serializeNBT(CODEC, this, Platform.getFrozenRegistry());
    }

    @Nullable
    public IQuestReward deserializeWrapper(Tag tag) {
        return CodecUtil.deserializeNBT(CODEC, tag, Platform.getFrozenRegistry());
    }
}
