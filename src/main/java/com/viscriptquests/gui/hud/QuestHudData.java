package com.viscriptquests.gui.hud;

import com.lowdragmc.lowdraglib2.Platform;
import com.viscriptquests.quest.data.runtime.PlayerQuestState;
import com.viscriptquests.quest.data.runtime.QuestPlayerData;
import com.viscriptquests.quest.data.runtime.QuestGuideMarker;
import com.viscriptquests.quest.data.runtime.TaskObjectiveProgress;
import com.viscriptquests.quest.data.runtime.TaskProgress;
import net.minecraft.nbt.CompoundTag;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

// 客户端 HUD 使用的任务数据快照，由服务端在追踪状态变化时同步。
public final class QuestHudData {
    private static final QuestPlayerData PLAYER_DATA = new QuestPlayerData();

    public static void update(CompoundTag data) {
        PLAYER_DATA.beforeDeserialize();
        PLAYER_DATA.deserializeNBT(Platform.getFrozenRegistry(), data);
        QuestGuideMarkerClientBridge.update(snapshot());
    }

    public static boolean hasTrackedTask() {
        return trackedTask().isPresent();
    }

    public static ComponentState snapshot() {
        Optional<PlayerQuestState> quest = trackedQuest();
        Optional<TaskProgress> task = trackedTask(quest.orElse(null));
        if (quest.isEmpty() || task.isEmpty()) {
            return ComponentState.EMPTY;
        }
        return new ComponentState(quest.get(), task.get());
    }

    private static Optional<PlayerQuestState> trackedQuest() {
        if (PLAYER_DATA.trackedQuestId == null || PLAYER_DATA.trackedQuestId.isBlank()) {
            return Optional.empty();
        }
        return PLAYER_DATA.findQuest(PLAYER_DATA.trackedQuestId);
    }

    private static Optional<TaskProgress> trackedTask() {
        return trackedQuest().flatMap(QuestHudData::trackedTask);
    }

    private static Optional<TaskProgress> trackedTask(PlayerQuestState quest) {
        if (quest == null || PLAYER_DATA.trackedStepId == null || PLAYER_DATA.trackedStepId.isBlank()) {
            return Optional.empty();
        }
        return quest.findStepProgress(PLAYER_DATA.trackedStepId);
    }

    public record ComponentState(PlayerQuestState quest, TaskProgress task) {
        private static final ComponentState EMPTY = new ComponentState(null, null);

        public boolean isEmpty() {
            return quest == null || task == null;
        }

        public List<MarkerState> guideMarkers() {
            if (isEmpty()) {
                return List.of();
            }
            List<MarkerState> markers = new ArrayList<>();
            if (!task.objectives.isEmpty()) {
                for (TaskObjectiveProgress objective : task.objectives) {
                    if (objective == null || !objective.isActive() || objective.guideMarker == null
                            || !objective.guideMarker.isEnabled()) {
                        continue;
                    }
                    markers.add(new MarkerState(objective.guideMarker, objective.displayHint().getString()));
                }
            }
            if (markers.isEmpty() && task.guideMarker != null && task.guideMarker.isEnabled()) {
                markers.add(new MarkerState(task.guideMarker, task.title));
            }
            return markers;
        }
    }

    public record MarkerState(QuestGuideMarker marker, String fallbackLabel) {
    }
}
