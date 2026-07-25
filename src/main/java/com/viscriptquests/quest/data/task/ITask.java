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
import com.viscriptquests.quest.data.QuestVariableValue;
import com.viscriptquests.quest.data.TaskObjectiveType;
import com.viscriptquests.quest.data.runtime.QuestGuideMarker;
import com.viscriptquests.quest.data.runtime.TaskObjectiveProgress;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;

import java.util.Map;
import java.util.function.Supplier;

// 任务目标接口，所有任务类型（物品收集、击杀、到达位置等）都通过此接口注册和序列化
public abstract class ITask implements ILDLRegister<ITask, Supplier<ITask>>, IPersistedSerializable {
    public static final String ID = ViScriptQuests.MOD_ID + ":task";

    public static final Codec<ITask> CODEC = ViScriptQuestsRegistries.TASKS.optionalCodec()
            .dispatch(ILDLRegister::getRegistryHolderOptional,
                    optional -> optional.map(holder -> PersistedParser.createMapCodec(holder.value()))
                            .orElseGet(LDLibExtraCodecs::errorDecoder));
    public static final StreamCodec<ByteBuf, ITask> STREAM_CODEC = ByteBufCodecs.fromCodec(CODEC);

    // 所有任务类型共有的基础字段
    @Persisted
    public String stepId = "";
    // 目标节点的稳定 ID，用于目标完成后触发子图里的动态动作流。
    @Persisted
    public String objectiveId = "";
    // 自定义目标提示文本；为空时使用具体目标自己的默认提示。
    @Persisted
    public String taskHint = "";
    // 目标在小任务里的语义：必做、可选，或作为失败条件监听。
    @Persisted
    public TaskObjectiveType objectiveType = TaskObjectiveType.REQUIRED;

    // 任务类型的显示名称
    public Component getDisplayName() {
        return Component.translatable("viscript_quests.task." + name());
    }

    // 检查玩家是否满足完成条件
    public abstract boolean checkCompletion(ServerPlayer player);

    public boolean checkCompletion(ServerPlayer player, Map<String, QuestVariableValue> questVariables) {
        return checkCompletion(player);
    }

    // 完成时的处理逻辑（如扣除物品），返回是否成功
    public abstract boolean onComplete(ServerPlayer player);

    public boolean onComplete(ServerPlayer player, Map<String, QuestVariableValue> questVariables) {
        return onComplete(player);
    }

    // 自动提交能力由具体目标决定，避免所有目标都暴露没有意义的提交模式。
    public boolean allowsAutoSubmit() {
        return true;
    }

    // 返回任务提示文本，用于 UI 展示（如"需要收集 1 个 合成台"）
    public final Component getTaskHint() {
        return getTaskHint(null, null);
    }

    public final Component getTaskHint(ServerPlayer player, Map<String, QuestVariableValue> questVariables) {
        if (taskHint != null && !taskHint.isBlank()) {
            return Component.translatableWithFallback(taskHint, taskHint);
        }
        return getDefaultTaskHint(player, questVariables);
    }

    // 具体目标提供自己的默认提示；公共 taskHint 为空时才会使用。
    protected abstract Component getDefaultTaskHint();

    protected Component getDefaultTaskHint(ServerPlayer player, Map<String, QuestVariableValue> questVariables) {
        return getDefaultTaskHint();
    }

    // 任务目标显示用图片，任务书和 HUD 都可以复用。
    public DisplayIcon getDisplayIcon() {
        return new DisplayIcon();
    }

    // 目标进度需求量，默认目标只有完成/未完成两种状态。
    public int getRequiredAmount() {
        return 1;
    }

    public int getRequiredAmount(Map<String, QuestVariableValue> questVariables, ServerPlayer player) {
        return getRequiredAmount();
    }

    // 刷新目标展示进度，不执行扣物品、发奖励等副作用。
    public void refreshObjectiveProgress(ServerPlayer player, TaskObjectiveProgress progress) {
        refreshObjectiveProgress(player, progress, null);
    }

    public void refreshObjectiveProgress(ServerPlayer player, TaskObjectiveProgress progress,
                                         Map<String, QuestVariableValue> questVariables) {
        int required = Math.max(1, getRequiredAmount(questVariables, player));
        progress.requiredAmount = required;
        progress.currentAmount = progress.completed ? required : 0;
    }

    // 目标进度是否能从玩家当前状态直接重算；击杀次数这类事件累计目标需要保留已有进度。
    public boolean refreshesProgressFromPlayerState() {
        return true;
    }

    // 手动提交单个目标。默认目标不提供手动提交能力。
    public boolean submitObjective(ServerPlayer player, TaskObjectiveProgress progress) {
        return submitObjective(player, progress, null);
    }

    public boolean submitObjective(ServerPlayer player, TaskObjectiveProgress progress,
                                   Map<String, QuestVariableValue> questVariables) {
        return false;
    }

    // 自动提交单个目标，成功时只标记该目标完成，不直接完成整个小任务。
    public boolean autoCompleteObjective(ServerPlayer player, TaskObjectiveProgress progress) {
        return autoCompleteObjective(player, progress, null);
    }

    public boolean autoCompleteObjective(ServerPlayer player, TaskObjectiveProgress progress,
                                         Map<String, QuestVariableValue> questVariables) {
        if ((!allowsAutoSubmit() && !progress.isFailureCondition())
                || progress.completed
                || !checkCompletion(player, questVariables)
                || (!progress.isFailureCondition() && !onComplete(player, questVariables))) {
            return false;
        }
        progress.completed = true;
        progress.currentAmount = Math.max(1, getRequiredAmount(questVariables, player));
        progress.requiredAmount = progress.currentAmount;
        return true;
    }

    // 当前目标的导航标记。默认不显示，位置/实体类任务可以覆盖。
    public QuestGuideMarker getGuideMarker(ServerPlayer player) {
        return QuestGuideMarker.disabled();
    }

    // 客户端任务书附加操作：目标可以声明一个 Ponder 查看按钮，并给出要打开的组件 ID。
    public String getPonderComponentId() {
        return "";
    }

    public boolean hasPonderViewAction() {
        return false;
    }
}
