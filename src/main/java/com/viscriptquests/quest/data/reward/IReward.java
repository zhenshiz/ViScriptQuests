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
import com.viscriptquests.quest.data.QuestVariableValue;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;

import java.util.Map;
import java.util.function.Supplier;

// 任务奖励接口，所有奖励类型（物品、经验、命令等）都通过此接口注册和序列化
public abstract class IReward implements ILDLRegister<IReward, Supplier<IReward>>, IPersistedSerializable {
    public static final String ID = ViScriptQuests.MOD_ID + ":reward";

    public static final Codec<IReward> CODEC = ViScriptQuestsRegistries.REWARDS.optionalCodec()
            .dispatch(ILDLRegister::getRegistryHolderOptional,
                    optional -> optional.map(holder -> PersistedParser.createMapCodec(holder.value()))
                            .orElseGet(LDLibExtraCodecs::errorDecoder));
    public static final StreamCodec<ByteBuf, IReward> STREAM_CODEC = ByteBufCodecs.fromCodec(CODEC);

    // 关联的小任务 ID，为空表示大任务完成时发放的全局奖励
    @Persisted
    public String stepId = "";
    // ViScriptTeam 联动：开启后队伍任务奖励只发给队长；未安装 VST 或玩家无队伍时仍发给当前玩家。
    @Persisted
    public boolean teamLeaderOnly = false;
    // 是否在任务书奖励栏中展示；不影响奖励实际发放。
    @Persisted
    public boolean showInRewardList = true;
    // 任务书中的奖励展示图标；未配置时由具体奖励类型提供默认图标。
    @Persisted
    public DisplayIcon rewardIcon = new DisplayIcon();
    // 任务书中鼠标悬浮到奖励图标时显示的文本；为空时使用奖励类型的默认说明。
    @Persisted
    public String rewardTooltip = "";

    // 奖励类型的显示名称
    public Component getDisplayName() {
        return Component.translatable("viscript_quests.reward." + name());
    }

    // 发放奖励给玩家
    public abstract void grant(ServerPlayer player);

    public void grant(ServerPlayer player, Map<String, QuestVariableValue> questVariables) {
        grant(player);
    }

    public void resolveDynamicValues(Map<String, QuestVariableValue> questVariables, ServerPlayer player) {
    }

    // 奖励的 UI 显示文本（用于任务书展示）
    public Component getRewardHint() {
        return rewardHintOrDefault(getDisplayName());
    }

    public Component getRewardHint(ServerPlayer player, Map<String, QuestVariableValue> questVariables) {
        return getRewardHint();
    }

    // 奖励的 UI 显示图标（用于任务书渲染）
    public DisplayIcon getRewardIcon() {
        return rewardIconOrDefault(new DisplayIcon());
    }

    protected Component rewardHintOrDefault(Component defaultHint) {
        String tooltip = rewardTooltip == null ? "" : rewardTooltip.trim();
        if (!tooltip.isEmpty()) {
            return Component.literal(tooltip);
        }
        return defaultHint == null ? Component.empty() : defaultHint;
    }

    protected DisplayIcon rewardIconOrDefault(DisplayIcon defaultIcon) {
        if (hasCustomRewardIcon()) {
            return rewardIcon.copy();
        }
        return defaultIcon == null ? new DisplayIcon() : defaultIcon;
    }

    private boolean hasCustomRewardIcon() {
        if (rewardIcon == null) {
            return false;
        }
        if (rewardIcon.isTexture()) {
            return rewardIcon.getTexture() != null && !rewardIcon.getTexture().isBlank();
        }
        return rewardIcon.getItemStack() != null && !rewardIcon.getItemStack().isEmpty();
    }
}
