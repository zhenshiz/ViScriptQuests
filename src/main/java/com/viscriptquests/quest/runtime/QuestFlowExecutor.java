package com.viscriptquests.quest.runtime;

import com.viscriptquests.ViScriptQuests;
import com.viscriptquests.event.neoforge.QuestEvent;
import com.viscriptquests.gui.blueprint.QuestBlueprintFlowTypes;
import com.viscriptquests.quest.data.*;
import com.viscriptquests.quest.data.runtime.*;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.common.NeoForge;

import java.util.*;

/**
 * 推进玩家任务的运行时流程状态机。
 *
 * <p>该执行器只负责解释 {@link QuestFile} 中已经编译好的流程节点和流程边，
 * 并把结果写回玩家自己的 {@link PlayerQuestState}。蓝图编译、具体目标检测、
 * 任务追踪选择和任务书 UI 展示都不属于该类的职责。
 * <p>流程推进以 {@link PlayerQuestState#activeFlowNodes} 作为入口集合：
 * 普通流程节点会被立刻消费并沿出边继续推进，小任务节点会被激活后停住，
 * 等待目标提交成功后再继续，Join 节点则根据到达分支数量判断是否可以放行。
 */
public class QuestFlowExecutor {
    /**
     * 推进指定玩家任务中当前所有可继续执行的流程节点。
     *
     * <p>方法会持续消费普通流程节点、处理 Join 节点、激活小任务节点，
     * 直到流程没有新的变化、遇到需要玩家完成的小任务，或者任务被结束。
     * 小任务节点本身不会在这里检查目标完成条件，只会把对应进度从锁定状态改为激活状态。
     *
     * @param player 服务端玩家，提供注册表上下文、奖励发放和调试输出目标
     * @param state 玩家任务状态，保存当前活跃节点、已完成节点、Join 进度和任务变量
     * @param questFile 运行时任务文件，提供流程节点、流程边和奖励定义
     */
    public static void advance(ServerPlayer player, PlayerQuestState state, QuestFile questFile) {
        boolean changed;
        int guard = 1000;
        do {
            changed = false;
            List<String> activeSnapshot = new ArrayList<>(state.activeFlowNodes);
            for (String nodeId : activeSnapshot) {
                if (!state.activeFlowNodes.contains(nodeId)) {
                    continue;
                }
                QuestFlowNode node = questFile.findFlowNode(nodeId).orElse(null);
                if (node == null) {
                    state.activeFlowNodes.remove(nodeId);
                    changed = true;
                    continue;
                }

                if (QuestBlueprintFlowTypes.isSubQuest(node)) {
                    activateStepNode(state, questFile, node.stepId, player);
                    continue;
                }

                if (QuestBlueprintFlowTypes.isJoin(node)) {
                    if (tryResolveJoin(player, state, questFile, node)) {
                        applyOutgoingEdges(player, state, questFile, nodeId);
                        changed = true;
                    }
                    continue;
                }

                state.activeFlowNodes.remove(nodeId);
                state.completedFlowNodes.add(nodeId);
                if (QuestBlueprintFlowTypes.isEnd(node)) {
                    finish(player, state, questFile, node.success, false, "");
                    return;
                }
                applyOutgoingEdges(player, state, questFile, nodeId);
                changed = true;
            }
        } while (changed && guard-- > 0 && state.status == QuestStatus.ACTIVE);

        if (state.activeFlowNodes.isEmpty() && !state.completedFlowNodes.isEmpty() && state.status == QuestStatus.ACTIVE) {
            finish(player, state, questFile, true, false, "");
        }
    }

    /**
     * 标记一个小任务流程节点完成，并从该节点继续推进后续流程。
     *
     * <p>该方法通常在目标检测和提交逻辑已经确认小任务完成后调用。
     * 它不会再次检查目标条件，只负责更新流程节点集合、执行出边逻辑并触发后续推进。
     *
     * @param player 服务端玩家，提供注册表上下文、奖励发放和调试输出目标
     * @param state 玩家任务状态，保存当前活跃节点、已完成节点、Join 进度和任务变量
     * @param questFile 运行时任务文件，提供流程节点和流程边
     * @param stepId 小任务节点标识，对应已经完成的小任务流程节点
     */
    public static void completeStepNode(ServerPlayer player, PlayerQuestState state, QuestFile questFile, String stepId) {
        state.activeFlowNodes.remove(stepId);
        state.completedFlowNodes.add(stepId);
        applyOutgoingEdges(player, state, questFile, stepId, QuestStepResult.SUCCESS);
        advance(player, state, questFile);
    }

    /**
     * 标记一个小任务流程节点失败，并从该节点的失败出口继续推进。
     *
     * <p>失败条件现在只代表“小任务失败”。如果蓝图作者连接了失败出口，
     * 流程会沿失败出口继续执行；如果没有失败出口，才把整个任务结算为失败。
     * 这样限时、实体死亡等失败条件既能做真正失败，也能做补救分支或不同奖励。
     */
    public static void failStepNode(ServerPlayer player, PlayerQuestState state, QuestFile questFile, String failedStepId) {
        state.findStepProgress(failedStepId).ifPresent(progress -> {
            progress.status = TaskStatus.FAILED;
            TaskObjectiveProgress failedObjective = progress.objectives.stream()
                    .filter(objective -> objective != null && objective.isFailureCondition() && objective.completed)
                    .findFirst()
                    .orElse(null);
            NeoForge.EVENT_BUS.post(new QuestEvent.TaskFailed(player, state, progress, failedObjective));
        });
        state.activeFlowNodes.remove(failedStepId);
        state.completedFlowNodes.add(failedStepId);
        if (!applyOutgoingEdges(player, state, questFile, failedStepId, QuestStepResult.FAILURE)) {
            finish(player, state, questFile, false, false, failedStepId);
            return;
        }
        advance(player, state, questFile);
    }

    /**
     * 直接把整条任务结算为失败。
     *
     * <p>保留该入口给命令、兼容旧逻辑或异常兜底使用。普通失败条件应优先调用
     * {@link #failStepNode(ServerPlayer, PlayerQuestState, QuestFile, String)}，让蓝图失败出口有机会接管流程。
     */
    public static void failQuest(ServerPlayer player, PlayerQuestState state, QuestFile questFile, String failedStepId) {
        state.findStepProgress(failedStepId).ifPresent(progress -> {
            progress.status = TaskStatus.FAILED;
            TaskObjectiveProgress failedObjective = progress.objectives.stream()
                    .filter(objective -> objective != null && objective.isFailureCondition() && objective.completed)
                    .findFirst()
                    .orElse(null);
            NeoForge.EVENT_BUS.post(new QuestEvent.TaskFailed(player, state, progress, failedObjective));
        });
        finish(player, state, questFile, false, false, failedStepId);
    }

    /**
     * 强制完成指定玩家任务。
     *
     * <p>该方法用于指令、调试或脚本控制路径。它会把所有小任务标记为完成，
     * 清空活跃流程节点，并在任务文件存在时发放小任务奖励和任务完成奖励。
     *
     * @param player 服务端玩家，接收奖励并提供游戏时间
     * @param state 玩家任务状态，要被强制改为完成状态
     * @param questFile 运行时任务文件；为 {@code null} 时只修改任务状态，不发放文件内奖励
     */
    public static void completeQuest(ServerPlayer player, PlayerQuestState state, QuestFile questFile) {
        List<TaskProgress> newlyCompleted = new ArrayList<>();
        for (TaskProgress progress : state.taskProgresses) {
            if (progress.status != TaskStatus.COMPLETED) {
                newlyCompleted.add(progress);
            }
            progress.status = TaskStatus.COMPLETED;
            state.completedFlowNodes.add(progress.stepId);
        }
        for (TaskProgress progress : newlyCompleted) {
            NeoForge.EVENT_BUS.post(new QuestEvent.TaskCompleted(player, state, progress, true));
            QuestCompletionNotificationService.notifyTaskCompleted(player, progress);
        }
        state.activeFlowNodes.clear();
        if (questFile != null) {
            for (TaskProgress progress : state.taskProgresses) {
                QuestRewardService.grantStepRewards(player, questFile, state, progress.stepId);
            }
            finish(player, state, questFile, true, true, "");
        } else {
            boolean newlyCompletedQuest = state.status != QuestStatus.COMPLETED;
            state.status = QuestStatus.COMPLETED;
            state.completedGameTime = player.level().getGameTime();
            if (newlyCompletedQuest) {
                NeoForge.EVENT_BUS.post(new QuestEvent.QuestCompleted(player, state, true));
                QuestCompletionNotificationService.notifyQuestCompleted(player, state);
            }
        }
    }

    /**
     * 激活小任务节点对应的进度。
     *
     * <p>小任务节点是流程推进的等待点。流程到达这里后只会解锁对应小任务，
     * 后续是否完成由目标检测和提交服务决定。
     *
     * @param state 玩家任务状态，保存小任务进度和流程节点集合
     * @param stepId 小任务节点标识
     */
    private static void activateStepNode(PlayerQuestState state, QuestFile questFile, String stepId, ServerPlayer player) {
        if (stepId == null || stepId.isEmpty()) {
            return;
        }
        state.activeFlowNodes.add(stepId);
        state.findStepProgress(stepId).ifPresent(progress -> {
            boolean reentered = progress.status == TaskStatus.COMPLETED
                    || progress.status == TaskStatus.FAILED
                    || progress.status == TaskStatus.SKIPPED;
            if (progress.status == TaskStatus.COMPLETED
                    || progress.status == TaskStatus.FAILED
                    || progress.status == TaskStatus.SKIPPED) {
                // 回环重新进入同一个小任务时，重置目标进度并允许再次等待玩家完成。
                state.rewardedSteps.remove(stepId);
                state.triggeredObjectiveActions.removeIf(actionKey -> actionKey.startsWith(stepId + ":"));
                TaskProgress refreshed = TaskProgress.fromTasks(stepId, questFile.findTasksForStep(stepId),
                        questFile.findStep(stepId).orElse(null), player, state.questVariables);
                progress.title = refreshed.title;
                progress.subtitle = refreshed.subtitle;
                progress.description = refreshed.description.clone();
                progress.taskHint = refreshed.taskHint;
                progress.manualSubmitRequired = refreshed.manualSubmitRequired;
                progress.displayIcon = refreshed.displayIcon;
                progress.guideMarker = refreshed.guideMarker;
                progress.objectives.clear();
                progress.objectives.addAll(refreshed.objectives);
            }
            boolean activating = progress.status == TaskStatus.LOCKED
                    || progress.status == TaskStatus.COMPLETED
                    || progress.status == TaskStatus.FAILED
                    || progress.status == TaskStatus.SKIPPED;
            if (activating) {
                progress.status = TaskStatus.ACTIVE;
                progress.refreshObjectives(questFile, player, state.questVariables);
                NeoForge.EVENT_BUS.post(new QuestEvent.TaskStarted(player, state, progress, reentered));
            }
        });
    }

    /**
     * 判断 Join 节点是否已经满足放行条件。
     *
     * <p>Join 进度记录哪些前置分支已经到达该节点。满足 {@code ANY}、{@code ALL}
     * 或 {@code COUNT} 规则后，Join 节点会被标记完成；对于未被选择的分支，
     * 会递归标记为跳过，避免任务已经结束的分支继续触发小任务逻辑。
     *
     * @param state 玩家任务状态，保存 Join 进度和流程节点集合
     * @param questFile 运行时任务文件，提供 Join 入边数量和分支结构
     * @param node Join 流程节点
     * @return 是否成功解析并放行该 Join 节点
     */
    private static boolean tryResolveJoin(ServerPlayer player, PlayerQuestState state, QuestFile questFile,
                                          QuestFlowNode node) {
        JoinProgress progress = state.findFlowJoinProgress(node.nodeId).orElseGet(() -> {
            JoinProgress created = new JoinProgress();
            created.joinNodeId = node.nodeId;
            state.flowJoinProgresses.add(created);
            return created;
        });
        if (progress.resolved) {
            state.activeFlowNodes.remove(node.nodeId);
            state.completedFlowNodes.add(node.nodeId);
            return false;
        }

        int incomingCount = questFile.countIncomingFlowEdges(node.nodeId);
        int required = switch (node.joinMode) {
            case ANY -> 1;
            case ALL -> incomingCount;
            case COUNT -> Math.max(1, Math.min(node.requiredCount, incomingCount));
        };
        if (progress.arrivedFromNodeIds.size() < required) {
            return false;
        }

        progress.resolved = true;
        state.activeFlowNodes.remove(node.nodeId);
        state.completedFlowNodes.add(node.nodeId);
        skipUnchosenBranches(player, state, questFile, node.nodeId, progress.arrivedFromNodeIds);
        return true;
    }

    /**
     * 应用指定流程节点的所有可通过出边。
     *
     * <p>每条出边会先判断条件，再应用变量修改和调试输出。目标节点存在且尚未完成时，
     * 会被加入活跃节点集合；目标是 Join 节点时，同时记录该 Join 从哪个前置节点到达。
     *
     * @param player 服务端玩家，提供注册表上下文和调试输出目标
     * @param state 玩家任务状态，保存任务变量和流程节点集合
     * @param questFile 运行时任务文件，提供出边和目标节点定义
     * @param fromNodeId 起始流程节点标识
     */
    private static boolean applyOutgoingEdges(ServerPlayer player, PlayerQuestState state, QuestFile questFile, String fromNodeId) {
        return applyOutgoingEdges(player, state, questFile, fromNodeId, QuestStepResult.ANY);
    }

    private static boolean applyOutgoingEdges(ServerPlayer player, PlayerQuestState state, QuestFile questFile,
                                              String fromNodeId, QuestStepResult stepResult) {
        boolean applied = false;
        for (QuestFlowEdge edge : questFile.findFlowEdgesFrom(fromNodeId, stepResult)) {
            if (!edge.evaluate(state.questVariables, player.registryAccess(), player)) {
                continue;
            }
            edge.applyMutations(state.questVariables, player.registryAccess(), player);
            printDebugMessages(player, state, edge);

            QuestFlowNode target = questFile.findFlowNode(edge.toNodeId).orElse(null);
            if (target == null) {
                continue;
            }
            boolean reenterable = QuestBlueprintFlowTypes.isSubQuest(target)
                    || QuestBlueprintFlowTypes.isType(target, QuestBlueprintFlowTypes.BRANCH);
            if (state.completedFlowNodes.contains(edge.toNodeId) && !reenterable) {
                continue;
            }
            if (reenterable) {
                state.completedFlowNodes.remove(edge.toNodeId);
            }
            if (QuestBlueprintFlowTypes.isJoin(target)) {
                JoinProgress joinProgress = state.findFlowJoinProgress(target.nodeId).orElseGet(() -> {
                    JoinProgress created = new JoinProgress();
                    created.joinNodeId = target.nodeId;
                    state.flowJoinProgresses.add(created);
                    return created;
                });
                joinProgress.arrivedFromNodeIds.add(fromNodeId);
            }
            state.activeFlowNodes.add(edge.toNodeId);
            applied = true;
        }
        return applied;
    }

    /**
     * 跳过未参与当前 Join 结果的其它分支。
     *
     * <p>该方法主要服务于 {@code ANY} 和 {@code COUNT} 语义。当 Join 已经满足条件后，
     * 没有到达 Join 的并行分支不应该继续保留活跃小任务，否则玩家可能在任务结束后仍触发旧分支。
     *
     * @param state 玩家任务状态，保存流程节点集合和小任务进度
     * @param questFile 运行时任务文件，提供流程边结构
     * @param joinNodeId Join 节点标识
     * @param arrivedFromNodeIds 已经到达该 Join 的前置节点集合
     */
    private static void skipUnchosenBranches(ServerPlayer player, PlayerQuestState state, QuestFile questFile,
                                             String joinNodeId,
                                             Set<String> arrivedFromNodeIds) {
        for (QuestFlowEdge edge : questFile.flowEdges) {
            if (!edge.toNodeId.equals(joinNodeId) || arrivedFromNodeIds.contains(edge.fromNodeId)) {
                continue;
            }
            markFlowBranchSkipped(player, state, questFile, edge.fromNodeId);
            markIncomingBranchSkipped(player, state, questFile, edge.fromNodeId, new HashSet<>());
        }
    }

    /**
     * 从一个节点向上递归跳过尚未完成的入边分支。
     *
     * @param state 玩家任务状态，保存流程节点集合和小任务进度
     * @param questFile 运行时任务文件，提供流程边结构
     * @param nodeId 当前要向上检查的流程节点标识
     * @param visited 已访问节点集合，用于阻止循环图导致递归重复
     */
    private static void markIncomingBranchSkipped(ServerPlayer player, PlayerQuestState state, QuestFile questFile,
                                                  String nodeId,
                                                  Set<String> visited) {
        if (!visited.add(nodeId)) {
            return;
        }
        for (QuestFlowEdge edge : questFile.flowEdges) {
            if (!edge.toNodeId.equals(nodeId) || state.completedFlowNodes.contains(edge.fromNodeId)) {
                continue;
            }
            markFlowBranchSkipped(player, state, questFile, edge.fromNodeId);
            markIncomingBranchSkipped(player, state, questFile, edge.fromNodeId, visited);
        }
    }

    /**
     * 将指定节点以及它后续可到达的分支标记为跳过。
     *
     * <p>如果节点对应小任务，激活或锁定状态的小任务进度会变为 {@link TaskStatus#SKIPPED}。
     * 如果节点是 Join，会清理该 Join 的到达记录，避免被跳过分支影响后续判断。
     *
     * @param state 玩家任务状态，保存流程节点集合、Join 进度和小任务进度
     * @param questFile 运行时任务文件，提供流程节点和出边结构
     * @param nodeId 要跳过的流程节点标识
     */
    private static void markFlowBranchSkipped(ServerPlayer player, PlayerQuestState state, QuestFile questFile,
                                              String nodeId) {
        if (nodeId == null || nodeId.isEmpty() || state.completedFlowNodes.contains(nodeId)) {
            return;
        }
        QuestFlowNode node = questFile.findFlowNode(nodeId).orElse(null);
        if (QuestBlueprintFlowTypes.isJoin(node)) {
            state.findFlowJoinProgress(nodeId)
                    .ifPresent(progress -> progress.arrivedFromNodeIds.clear());
        }
        state.activeFlowNodes.remove(nodeId);
        state.completedFlowNodes.add(nodeId);
        if (QuestBlueprintFlowTypes.isSubQuest(node)) {
            state.findStepProgress(node.stepId)
                    .filter(progress -> progress.status == TaskStatus.ACTIVE || progress.status == TaskStatus.LOCKED)
                    .ifPresent(progress -> {
                        progress.status = TaskStatus.SKIPPED;
                        NeoForge.EVENT_BUS.post(new QuestEvent.TaskSkipped(player, state, progress, "branch"));
                    });
        }
        for (QuestFlowEdge outgoing : questFile.findFlowEdgesFrom(nodeId)) {
            if (!outgoing.toNodeId.equals(nodeId)) {
                markFlowBranchSkipped(player, state, questFile, outgoing.toNodeId);
            }
        }
    }

    /**
     * 结束玩家任务并清理运行时状态。
     *
     * <p>方法会清空活跃流程节点、跳过仍未完成的小任务、锁定 Join 进度、按成功状态发放任务完成奖励，
     * 并在当前任务被追踪时清空追踪信息。
     *
     * @param player 服务端玩家，接收奖励并提供保存数据入口
     * @param state 玩家任务状态，要被结束的任务状态
     * @param questFile 运行时任务文件，提供任务完成奖励
     * @param success 是否按成功完成任务处理；为 {@code false} 时任务进入失败状态
     */
    private static void finish(ServerPlayer player, PlayerQuestState state, QuestFile questFile,
                               boolean success, boolean forced, String failedStepId) {
        if (state.status == QuestStatus.COMPLETED || state.status == QuestStatus.FAILED) {
            return;
        }
        state.activeFlowNodes.clear();
        for (TaskProgress progress : state.taskProgresses) {
            if (progress.status == TaskStatus.ACTIVE || progress.status == TaskStatus.LOCKED) {
                progress.status = TaskStatus.SKIPPED;
                NeoForge.EVENT_BUS.post(new QuestEvent.TaskSkipped(player, state, progress, "quest_finished"));
            }
        }
        for (JoinProgress joinProgress : state.flowJoinProgresses) {
            joinProgress.resolved = true;
        }
        if (success) {
            QuestRewardService.grantQuestCompletionRewards(player, questFile, state);
        }
        state.status = success ? QuestStatus.COMPLETED : QuestStatus.FAILED;
        state.completedGameTime = player.level().getGameTime();
        if (success) {
            NeoForge.EVENT_BUS.post(new QuestEvent.QuestCompleted(player, state, forced));
            QuestCompletionNotificationService.notifyQuestCompleted(player, state);
        } else {
            NeoForge.EVENT_BUS.post(new QuestEvent.QuestFailed(player, state, failedStepId));
        }
        QuestPlayerData playerData = QuestSavedData.get(player.getServer()).getPlayer(player.getUUID());
        if (playerData.trackedQuestId.equals(state.questId)) {
            playerData.trackedQuestId = "";
            playerData.trackedStepId = "";
        }
        QuestTrackingService.refresh(player);
    }

    static void printDebugMessages(ServerPlayer player, PlayerQuestState state, QuestFlowEdge edge) {
        for (QuestDebugPrint debugPrint : edge.debugPrints) {
            if (debugPrint.message == null || debugPrint.message.isEmpty()) {
                continue;
            }
            String interpolated = interpolateVariables(debugPrint.message, state.questVariables,
                    debugPrint.valuePrints, player.registryAccess(), player);
            if (debugPrint.sendToChat) {
                player.sendSystemMessage(Component.literal(interpolated));
            } else {
                ViScriptQuests.LOGGER.info("[Quest Debug] {} quest={} message={}",
                        player.getGameProfile().getName(), state.questId, interpolated);
            }
        }
    }

    private static String interpolateVariables(String message, Map<String, QuestVariableValue> variables,
                                               List<DebugValuePrint> debugValuePrints,
                                               net.minecraft.core.HolderLookup.Provider provider,
                                               ServerPlayer player) {
        if (message == null || message.isEmpty()) return message;
        for (DebugValuePrint debugValuePrint : debugValuePrints) {
            if (debugValuePrint.placeholder == null || debugValuePrint.placeholder.isEmpty()) continue;
            message = message.replace("{" + debugValuePrint.placeholder + "}",
                    formatFloatValue(debugValuePrint.evaluate(variables, provider, player)));
        }
        for (var entry : variables.entrySet()) {
            message = message.replace("{" + entry.getKey() + "}", entry.getValue().displayValue());
        }
        return message;
    }

    private static String formatFloatValue(float value) {
        if (value == (long) value) {
            return String.valueOf((long) value);
        }
        return String.valueOf(value);
    }
}
