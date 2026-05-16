package com.viscriptquests.quest.data;

import com.lowdragmc.lowdraglib2.syncdata.IPersistedSerializable;
import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib2.utils.PersistedParser;
import com.mojang.serialization.Codec;
import com.viscriptquests.quest.data.reward.IReward;
import com.viscriptquests.quest.data.task.ITask;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;

import java.util.*;

// 任务文件定义，包含任务元数据、目标和奖励
public class QuestFile implements IPersistedSerializable {
    public static final Codec<QuestFile> CODEC = PersistedParser.createCodec(QuestFile::new);
    public static final StreamCodec<ByteBuf, QuestFile> STREAM_CODEC = PersistedParser.createStreamCodec(QuestFile::new);

    @Persisted
    public int version = 1;
    //大任务
    @Persisted
    public QuestDefinition quest = new QuestDefinition();
    //目标
    @Persisted
    public final List<ITask> tasks = new ArrayList<>();
    //奖励
    @Persisted
    public final List<IReward> rewards = new ArrayList<>();
    // 步骤元数据列表（来自 SubQuestNode），按流程顺序排列
    @Persisted
    public final List<QuestStep> steps = new ArrayList<>();
    // 蓝图运行时流程节点，包含起点、任务、汇合、终点等虚拟节点
    @Persisted
    public final List<QuestFlowNode> flowNodes = new ArrayList<>();
    // 蓝图运行时流程边，条件和变量修改都挂在边上
    @Persisted
    public final List<QuestFlowEdge> flowEdges = new ArrayList<>();
    // 变量初始值（从蓝图黑板变量提取），发放任务时加载到玩家状态
    @Persisted
    public final Map<String, QuestVariableValue> variableDefaults = new LinkedHashMap<>();

    public List<ITask> findTasksForStep(String stepId) {
        return tasks.stream()
                .filter(task -> task.stepId.equals(stepId))
                .toList();
    }

    public List<IReward> findRewardsForStep(String stepId) {
        return rewards.stream()
                .filter(reward -> reward.stepId.equals(stepId))
                .toList();
    }

    public List<IReward> findQuestCompletionRewards() {
        return rewards.stream()
                .filter(reward -> reward.stepId == null || reward.stepId.isBlank())
                .toList();
    }

    public Optional<QuestStep> findStep(String stepId) {
        return steps.stream()
                .filter(step -> step.stepId.equals(stepId))
                .findFirst();
    }

    public Optional<QuestFlowNode> findFlowNode(String nodeId) {
        return flowNodes.stream()
                .filter(node -> node.nodeId.equals(nodeId))
                .findFirst();
    }

    public List<QuestFlowEdge> findFlowEdgesFrom(String nodeId) {
        return flowEdges.stream()
                .filter(edge -> edge.fromNodeId.equals(nodeId))
                .toList();
    }

    public int countIncomingFlowEdges(String nodeId) {
        return (int) flowEdges.stream()
                .filter(edge -> edge.toNodeId.equals(nodeId))
                .count();
    }
}
