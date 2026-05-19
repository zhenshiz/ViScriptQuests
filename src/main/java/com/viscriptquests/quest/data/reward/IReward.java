package com.viscriptquests.quest.data.reward;

import com.lowdragmc.lowdraglib2.registry.ILDLRegister;
import com.lowdragmc.lowdraglib2.syncdata.IPersistedSerializable;
import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib2.utils.LDLibExtraCodecs;
import com.lowdragmc.lowdraglib2.utils.PersistedParser;
import com.mojang.serialization.Codec;
import com.viscriptquests.ViScriptQuests;
import com.viscriptquests.ViScriptQuestsRegistries;
import com.viscriptquests.quest.data.DisplayIcon;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;

import java.util.function.Supplier;

// 任务奖励接口，所有奖励类型（物品、经验、命令等）都通过此接口注册和序列化
public abstract class IReward implements ILDLRegister<IReward, Supplier<IReward>>, IPersistedSerializable {
    public static final String ID = ViScriptQuests.MOD_ID + ":reward";

    public static final Codec<IReward> CODEC = ViScriptQuestsRegistries.REWARDS.optionalCodec()
            .dispatch(ILDLRegister::getRegistryHolderOptional,
                    optional -> optional.map(holder -> PersistedParser.createCodec(holder.value()).fieldOf("data"))
                            .orElseGet(LDLibExtraCodecs::errorDecoder));
    public static final StreamCodec<ByteBuf, IReward> STREAM_CODEC = ByteBufCodecs.fromCodec(CODEC);

    // 关联的小任务 ID，为空表示大任务完成时发放的全局奖励
    @Persisted
    public String stepId = "";
    // ViScriptTeam 联动：开启后队伍任务奖励只发给队长；未安装 VST 或玩家无队伍时仍发给当前玩家。
    @Persisted
    public boolean teamLeaderOnly = false;

    // 奖励类型的显示名称
    public Component getDisplayName() {
        return Component.translatable("viscript_quests.reward." + name());
    }

    // 发放奖励给玩家
    public abstract void grant(ServerPlayer player);

    // 奖励的 UI 显示文本（用于任务书展示）
    public Component getRewardHint() {
        return getDisplayName();
    }

    // 奖励的 UI 显示图标（用于任务书渲染）
    public DisplayIcon getRewardIcon() {
        return new DisplayIcon();
    }
}
