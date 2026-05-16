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
import com.viscriptquests.quest.data.QuestSubmitMode;
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
    @Persisted
    public QuestSubmitMode submitMode = QuestSubmitMode.MANUAL;

    // 任务类型的显示名称
    public Component getDisplayName() {
        return Component.translatable("viscript_quests.task." + name());
    }

    // 检查玩家是否满足完成条件
    public abstract boolean checkCompletion(ServerPlayer player);

    // 完成时的处理逻辑（如扣除物品），返回是否成功
    public abstract boolean onComplete(ServerPlayer player);

    // 返回任务提示文本，用于 UI 展示（如"需要收集 1 个 合成台"）
    public abstract Component getTaskHint();

    // hud显示的图片
    public abstract DisplayIcon getHudIcon();
}
