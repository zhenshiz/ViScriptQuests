package com.viscriptquests.quest.data.runtime;

import com.lowdragmc.lowdraglib2.syncdata.IPersistedSerializable;
import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted;
import com.viscriptquests.gui.blueprint.QuestBlueprintFlowTypes;
import com.viscriptquests.quest.data.DisplayIcon;
import com.viscriptquests.quest.data.QuestFile;
import com.viscriptquests.quest.data.QuestFlowNode;
import com.viscriptquests.quest.data.QuestStep;
import com.viscriptquests.quest.data.QuestVariableValue;
import com.viscriptquests.quest.data.reward.IReward;
import com.viscriptquests.quest.data.task.ITask;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class PlayerQuestState implements IPersistedSerializable {
    @Persisted
    public String questId = "";
    @Persisted
    public String categoryId = "";
    @Persisted
    public String title = "";
    @Persisted
    public String subtitle = "";
    @Persisted
    public DisplayIcon icon = new DisplayIcon();
    @Persisted
    public QuestStatus status = QuestStatus.ACTIVE;
    @Persisted
    public long grantedGameTime = 0;
    @Persisted
    public long completedGameTime = -1;
    // 运行时任务进度列表（TaskProgress 已注册 CustomDirectAccessor，CollectionAccessor 自动处理）
    @Persisted
    public final List<TaskProgress> taskProgresses = new ArrayList<>();
    // 已发放奖励的目标 ID 集合
    @Persisted
    public final Set<String> rewardedSteps = new LinkedHashSet<>();
    // 已触发过的目标动态动作，避免同一个目标在刷新/队伍同步后重复发放动态奖励。
    @Persisted
    public final Set<String> triggeredObjectiveActions = new LinkedHashSet<>();
    // 任务作用域变量，由外部命令/事件设置，用于分支条件判断
    @Persisted
    public final Map<String, QuestVariableValue> questVariables = new LinkedHashMap<>();
    // 当前活跃的流程节点 ID。任务节点状态由 TaskProgress 单独记录。
    @Persisted
    public final Set<String> activeFlowNodes = new LinkedHashSet<>();
    // 已处理完成的流程节点 ID，防止虚拟节点重复推进。
    @Persisted
    public final Set<String> completedFlowNodes = new LinkedHashSet<>();
    // Join 节点的到达进度。
    @Persisted
    public final List<JoinProgress> flowJoinProgresses = new ArrayList<>();
    // 奖励显示数据（从真实奖励和展示占位符提取），用于客户端 UI 展示。
    @Persisted
    public final List<RewardDisplay> rewardDisplays = new ArrayList<>();

    public Optional<TaskProgress> findStepProgress(String stepId) {
        return taskProgresses.stream()
                .filter(progress -> progress.stepId.equals(stepId))
                .findFirst();
    }

    public boolean isAllTasksCompleted() {
        return !taskProgresses.isEmpty() && taskProgresses.stream()
                .allMatch(progress -> progress.status == TaskStatus.COMPLETED);
    }

    public static PlayerQuestState fromQuestFile(QuestFile file, long gameTime) {
        return fromQuestFile(file, gameTime, null);
    }

    public static PlayerQuestState fromQuestFile(QuestFile file, long gameTime, net.minecraft.server.level.ServerPlayer player) {
        PlayerQuestState state = new PlayerQuestState();
        state.questId = file.quest.questId;
        state.title = file.quest.title;
        state.subtitle = file.quest.subtitle;
        state.icon = file.quest.icon.copy();
        state.status = QuestStatus.ACTIVE;
        state.grantedGameTime = gameTime;

        // 从蓝图黑板变量加载初始值到运行时变量，后续目标/奖励的动态表达式会读取这些默认值。
        for (var entry : file.variableDefaults.entrySet()) {
            state.questVariables.put(entry.getKey(), entry.getValue().copy());
        }

        state.refreshRewardDisplays(file, player);

        // 按小任务聚合目标进度：一个 SubQuest 可以包含多个 ITask。
        for (QuestStep step : file.steps) {
            List<ITask> tasks = file.findTasksForStep(step.stepId);
            if (tasks.isEmpty()) {
                continue;
            }
            TaskProgress progress = TaskProgress.fromTasks(step.stepId, tasks, step, null, state.questVariables);
            progress.status = TaskStatus.LOCKED;
            progress.lockObjectives();
            state.taskProgresses.add(progress);
        }
        for (ITask task : file.tasks) {
            if (state.findStepProgress(task.stepId).isPresent()) {
                continue;
            }
            QuestStep step = file.findStep(task.stepId).orElse(null);
            TaskProgress progress = TaskProgress.fromTasks(task.stepId, file.findTasksForStep(task.stepId), step,
                    null, state.questVariables);
            progress.status = TaskStatus.LOCKED;
            progress.lockObjectives();
            state.taskProgresses.add(progress);
        }

        // 初始从 START 流程节点开始，QuestManager 会继续推进到第一个任务/分支/Join。
        state.activeFlowNodes.add("");

        for (QuestFlowNode node : file.flowNodes) {
            if (QuestBlueprintFlowTypes.isJoin(node)) {
                JoinProgress progress = new JoinProgress();
                progress.joinNodeId = node.nodeId;
                state.flowJoinProgresses.add(progress);
            }
        }

        return state;
    }

    public float getVariable(String name, HolderLookup.Provider provider) {
        QuestVariableValue value = questVariables.get(name);
        return value == null ? 0f : value.asFloat();
    }

    public void setVariable(String name, float value, HolderLookup.Provider provider) {
        questVariables.compute(name, (k, current) -> current == null ? QuestVariableValue.ofFloat(value) : current.withNumericValue(value));
    }

    public void setVariable(String name, QuestVariableValue value) {
        if (name == null || name.isEmpty() || value == null) {
            return;
        }
        questVariables.put(name, value.copy());
    }

    public Optional<JoinProgress> findFlowJoinProgress(String joinNodeId) {
        return flowJoinProgresses.stream()
                .filter(progress -> progress.joinNodeId.equals(joinNodeId))
                .findFirst();
    }

    public void refreshRewardDisplays(QuestFile file) {
        refreshRewardDisplays(file, null);
    }

    public void refreshRewardDisplays(QuestFile file, net.minecraft.server.level.ServerPlayer player) {
        rewardDisplays.clear();
        if (file == null) {
            return;
        }
        for (IReward reward : file.rewards) {
            if (reward == null || !reward.showInRewardList) {
                continue;
            }
            RewardDisplay display = new RewardDisplay();
            display.stepId = reward.stepId == null ? "" : reward.stepId;
            Component hint = reward.getRewardHint(player, questVariables);
            display.displayText = hint == null ? Component.empty() : hint.copy();
            DisplayIcon icon = reward.getRewardIcon();
            display.icon = icon == null ? new DisplayIcon() : icon.copy();
            rewardDisplays.add(display);
        }
        for (RewardDisplay placeholder : file.rewardPlaceholders) {
            RewardDisplay display = new RewardDisplay();
            display.stepId = placeholder.stepId == null ? "" : placeholder.stepId;
            display.displayText = placeholder.displayText == null ? Component.empty() : placeholder.displayText.copy();
            display.icon = placeholder.icon == null ? new DisplayIcon() : placeholder.icon.copy();
            rewardDisplays.add(display);
        }
    }
}
