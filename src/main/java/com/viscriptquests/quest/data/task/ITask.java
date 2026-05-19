package com.viscriptquests.quest.data.task;

import com.lowdragmc.lowdraglib2.registry.ILDLRegister;
import com.lowdragmc.lowdraglib2.syncdata.IPersistedSerializable;
import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib2.utils.LDLibExtraCodecs;
import com.lowdragmc.lowdraglib2.utils.PersistedParser;
import com.mojang.serialization.Codec;
import com.viscriptquests.ViScriptQuests;
import com.viscriptquests.ViScriptQuestsRegistries;
import com.viscriptquests.quest.data.DisplayIcon;
import com.viscriptquests.quest.data.runtime.QuestGuideMarker;
import com.viscriptquests.quest.data.runtime.TaskObjectiveProgress;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;

import java.util.function.Supplier;

// 任务目标接口，所有任务类型（物品收集、击杀、到达位置等）都通过此接口注册和序列化
public abstract class ITask implements ILDLRegister<ITask, Supplier<ITask>>, IPersistedSerializable {
    public static final String ID = ViScriptQuests.MOD_ID + ":task";

    public static final Codec<ITask> CODEC = ViScriptQuestsRegistries.TASKS.optionalCodec()
            .dispatch(ILDLRegister::getRegistryHolderOptional,
                    optional -> optional.map(holder -> PersistedParser.createCodec(holder.value()).fieldOf("data"))
                            .orElseGet(LDLibExtraCodecs::errorDecoder));
    public static final StreamCodec<ByteBuf, ITask> STREAM_CODEC = ByteBufCodecs.fromCodec(CODEC);

    // 所有任务类型共有的基础字段
    @Persisted
    public String stepId = "";

    // 任务类型的显示名称
    public Component getDisplayName() {
        return Component.translatable("viscript_quests.task." + name());
    }

    // 检查玩家是否满足完成条件
    public abstract boolean checkCompletion(ServerPlayer player);

    // 完成时的处理逻辑（如扣除物品），返回是否成功
    public abstract boolean onComplete(ServerPlayer player);

    // 自动提交能力由具体目标决定，避免所有目标都暴露没有意义的提交模式。
    public boolean allowsAutoSubmit() {
        return true;
    }

    // 返回任务提示文本，用于 UI 展示（如"需要收集 1 个 合成台"）
    public abstract Component getTaskHint();

    // 任务目标显示用图片，任务书和 HUD 都可以复用。
    public DisplayIcon getDisplayIcon() {
        return new DisplayIcon();
    }

    // 目标进度需求量，默认目标只有完成/未完成两种状态。
    public int getRequiredAmount() {
        return 1;
    }

    // 刷新目标展示进度，不执行扣物品、发奖励等副作用。
    public void refreshObjectiveProgress(ServerPlayer player, TaskObjectiveProgress progress) {
        int required = Math.max(1, getRequiredAmount());
        progress.requiredAmount = required;
        progress.currentAmount = progress.completed ? required : 0;
    }

    // 目标进度是否能从玩家当前状态直接重算；击杀次数这类事件累计目标需要保留已有进度。
    public boolean refreshesProgressFromPlayerState() {
        return true;
    }

    // 手动提交单个目标。默认目标不提供手动提交能力。
    public boolean submitObjective(ServerPlayer player, TaskObjectiveProgress progress) {
        return false;
    }

    // 自动提交单个目标，成功时只标记该目标完成，不直接完成整个小任务。
    public boolean autoCompleteObjective(ServerPlayer player, TaskObjectiveProgress progress) {
        if (!allowsAutoSubmit() || progress.completed || !checkCompletion(player) || !onComplete(player)) {
            return false;
        }
        progress.completed = true;
        progress.currentAmount = Math.max(1, getRequiredAmount());
        progress.requiredAmount = progress.currentAmount;
        return true;
    }

    // 当前目标的导航标记。默认不显示，位置/实体类任务可以覆盖。
    public QuestGuideMarker getGuideMarker(ServerPlayer player) {
        return QuestGuideMarker.disabled();
    }
}
