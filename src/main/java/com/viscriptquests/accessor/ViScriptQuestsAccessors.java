package com.viscriptquests.accessor;

import com.lowdragmc.lowdraglib2.syncdata.AccessorRegistries;
import com.lowdragmc.lowdraglib2.syncdata.accessor.direct.CustomDirectAccessor;
import com.viscript_lib.annotation.ViScriptRegisterAccessors;
import com.viscript_lib.event.RegisterAccessorEvent;
import com.viscriptquests.gui.blueprint.data.LocationMarkerConfig;
import com.viscriptquests.gui.blueprint.data.LocationTargetConfig;
import com.viscriptquests.gui.blueprint.data.QuestRegistryId;
import com.viscriptquests.quest.data.*;
import com.viscriptquests.quest.data.reward.IReward;
import com.viscriptquests.quest.data.reward.LootTableReward;
import com.viscriptquests.quest.data.runtime.*;
import com.viscriptquests.quest.data.task.ITask;

/**
 * 任务系统需要在 LDLib2 RPC 扫描前注册的持久化访问器。
 */
public final class ViScriptQuestsAccessors {
    private ViScriptQuestsAccessors() {
    }

    @ViScriptRegisterAccessors
    public static void register(RegisterAccessorEvent event) {
        registerTaskAccessor();
        registerRewardAccessor();

        event.register(DisplayIcon.class, DisplayIcon::new);
        event.register(QuestCompletionToastData.class, QuestCompletionToastData::new);
        event.register(QuestGuideMarker.class, QuestGuideMarker::new);
        event.register(LootTableConfig.class, LootTableConfig::new);
        event.register(LootTableReward.class, LootTableReward::new);

        event.register(QuestValueToken.class, QuestValueToken::new);
        event.register(QuestVariableValue.class, QuestVariableValue::new);
        event.register(DebugValuePrint.class, DebugValuePrint::new);
        event.register(VariableMutation.class, VariableMutation::new);
        event.register(ScoreboardMutation.class, ScoreboardMutation::new);
        event.register(QuestDebugPrint.class, QuestDebugPrint::new);
        event.register(QuestFlowEdge.class, QuestFlowEdge::new);

        event.register(TaskObjectiveProgress.class, TaskObjectiveProgress::new);
        event.register(TaskProgress.class, TaskProgress::new);
        event.register(QuestStep.class, QuestStep::new);
        event.register(ObjectiveAction.class, ObjectiveAction::new);
        event.register(QuestCategoryData.class, QuestCategoryData::new);
        event.register(QuestCategoryListData.class, QuestCategoryListData::new);
        event.register(QuestCategoryConfigData.class, QuestCategoryConfigData::new);
        event.register(QuestBookData.class, QuestBookData::new);
        event.register(QuestPlayerData.class, QuestPlayerData::new);
        event.register(PlayerQuestState.class, PlayerQuestState::new);
        event.register(QuestFlowNode.class, QuestFlowNode::new);
        event.register(JoinProgress.class, JoinProgress::new);
        event.register(RewardDisplay.class, RewardDisplay::new);
        event.register(QuestRegistryId.class, QuestRegistryId::new);
        event.register(LocationTargetConfig.class, LocationTargetConfig::new);
        event.register(LocationMarkerConfig.class, LocationMarkerConfig::new);
    }

    private static void registerTaskAccessor() {
        AccessorRegistries.registerAccessor(
                CustomDirectAccessor.builder(ITask.class)
                        .codec(ITask.CODEC)
                        .streamCodec(ITask.STREAM_CODEC)
                        .codecMark()
                        .build(), 0);
    }

    private static void registerRewardAccessor() {
        AccessorRegistries.registerAccessor(
                CustomDirectAccessor.builder(IReward.class)
                        .codec(IReward.CODEC)
                        .streamCodec(IReward.STREAM_CODEC)
                        .codecMark()
                        .build(), 0);
    }
}
