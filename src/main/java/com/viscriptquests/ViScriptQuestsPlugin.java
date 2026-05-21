package com.viscriptquests;

import com.lowdragmc.lowdraglib2.plugin.ILDLibPlugin;
import com.lowdragmc.lowdraglib2.plugin.LDLibPlugin;
import com.lowdragmc.lowdraglib2.syncdata.AccessorRegistries;
import com.lowdragmc.lowdraglib2.syncdata.accessor.direct.CustomDirectAccessor;
import com.lowdragmc.lowdraglib2.utils.PersistedParser;
import com.viscriptquests.gui.blueprint.data.QuestRegistryId;
import com.viscriptquests.quest.data.*;
import com.viscriptquests.quest.data.LootTableConfig;
import com.viscriptquests.quest.data.reward.IReward;
import com.viscriptquests.quest.data.runtime.*;
import com.viscriptquests.quest.data.task.ITask;

import java.util.function.Supplier;

@LDLibPlugin
public class ViScriptQuestsPlugin implements ILDLibPlugin {
    @Override
    public void onLoad() {
        AccessorRegistries.setPriority(0);
        registerAccessors();
    }

    /**
     * 注册自定义类型的 LDLib2 accessor
     */
    private static void registerAccessors() {
        // ITask 多态 dispatch accessor
        AccessorRegistries.registerAccessor(
                CustomDirectAccessor.builder(ITask.class)
                        .codec(ITask.CODEC)
                        .streamCodec(ITask.STREAM_CODEC)
                        .codecMark()
                        .build());

        // IReward 多态 dispatch accessor
        AccessorRegistries.registerAccessor(
                CustomDirectAccessor.builder(IReward.class)
                        .codec(IReward.CODEC)
                        .streamCodec(IReward.STREAM_CODEC)
                        .codecMark()
                        .build());

        // 基础嵌套类型要先注册，避免后续数据类创建 codec 时拿到只读 accessor。
        register(DisplayIcon.class, DisplayIcon::new);
        register(QuestGuideMarker.class, QuestGuideMarker::new);
        register(LootTableConfig.class, LootTableConfig::new);

        // 运行时数据类型
        register(TaskObjectiveProgress.class, TaskObjectiveProgress::new);
        register(TaskProgress.class, TaskProgress::new);
        register(QuestStep.class, QuestStep::new);
        register(VariableMutation.class, VariableMutation::new);
        register(ScoreboardMutation.class, ScoreboardMutation::new);
        register(QuestValueToken.class, QuestValueToken::new);
        register(QuestVariableValue.class, QuestVariableValue::new);
        register(DebugValuePrint.class, DebugValuePrint::new);
        register(QuestCategoryData.class, QuestCategoryData::new);
        register(QuestCategoryListData.class, QuestCategoryListData::new);
        register(QuestCategoryConfigData.class, QuestCategoryConfigData::new);
        register(QuestBookData.class, QuestBookData::new);
        register(QuestPlayerData.class, QuestPlayerData::new);
        register(PlayerQuestState.class, PlayerQuestState::new);
        register(QuestFlowNode.class, QuestFlowNode::new);
        register(QuestDebugPrint.class, QuestDebugPrint::new);
        register(QuestFlowEdge.class, QuestFlowEdge::new);
        register(JoinProgress.class, JoinProgress::new);
        register(RewardDisplay.class, RewardDisplay::new);
        register(QuestRegistryId.class, QuestRegistryId::new);
    }

    private static <T> void register(Class<T> type, Supplier<T> factory) {
        AccessorRegistries.registerAccessor(
                CustomDirectAccessor.builder(type)
                        .codec(PersistedParser.createCodec(factory))
                        .streamCodec(PersistedParser.createStreamCodec(factory))
                        .codecMark()
                        .build());
    }
}
