package com.viscriptquests.gui.blueprint.compiler;

import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.CustomNodeModelImpl;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.PortModel;
import com.viscriptquests.gui.blueprint.node.flow.QuestBranchNode;
import com.viscriptquests.quest.data.ObjectiveAction;
import com.viscriptquests.quest.data.QuestDebugPrint;
import com.viscriptquests.quest.data.QuestFlowEdge;
import com.viscriptquests.quest.data.ScoreboardMutation;
import com.viscriptquests.quest.data.VariableMutation;
import com.viscriptquests.quest.data.reward.IReward;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Queue;
import java.util.Set;

// 编译小任务子图里“目标完成后”的动作链，复用主流程边上的条件、变量修改和调试输出模型。
public final class QuestObjectiveActionCompiler {
    private QuestObjectiveActionCompiler() {
    }

    public static List<ObjectiveAction> compile(QuestCompileContext context, CustomNodeModelImpl taskNode,
                                                String stepId, String objectiveId) {
        Queue<ActionEntry> queue = new ArrayDeque<>();
        followOutputFlow(taskNode, "next", new ActionEntry(null, objectiveId, new ArrayList<>(), new ArrayList<>(),
                new ArrayList<>(), new ArrayList<>(), objectiveId), queue);
        return compileQueue(context, queue, stepId, objectiveId);
    }

    private static List<ObjectiveAction> compileQueue(QuestCompileContext context,
                                                      Queue<ActionEntry> queue,
                                                      String stepId,
                                                      String objectiveId) {
        List<ObjectiveAction> actions = new ArrayList<>();
        Set<String> visited = new LinkedHashSet<>();
        int guard = 1000;
        while (!queue.isEmpty()) {
            if (guard-- <= 0) {
                throw QuestBlueprintValidationException.create(
                        "viscript_quests.editor.quest.export.validation.objective_action_trace_limit");
            }
            ActionEntry entry = queue.poll();
            CustomNodeModelImpl node = entry.node;
            if (node == null || node.getNode() == null) {
                continue;
            }
            String visitKey = node.getUid() + "|" + entry.pathKey + "|" + entry.mutations.hashCode()
                    + "|" + entry.scoreboardMutations.hashCode() + "|" + entry.debugPrints.hashCode()
                    + "|" + entry.gates.hashCode();
            if (!visited.add(visitKey)) {
                continue;
            }

            IQuestRewardNodeCompiler rewardCompiler = context.findRewardCompiler(node);
            if (rewardCompiler != null) {
                ObjectiveAction action = createAction(stepId, objectiveId, node, entry);
                IReward reward = rewardCompiler.compileReward(context, node, stepId);
                if (reward != null) {
                    action.rewards.add(reward);
                }
                addAction(actions, action);
                followOutputFlow(node, "next", entry.resetForNext(node), queue);
                continue;
            }

            if (node.getNode() instanceof QuestBranchNode) {
                compileBranch(context, node, entry, stepId, objectiveId, actions, queue);
                continue;
            }

            QuestPassthroughResult passthrough = compilePassthroughNode(context, node);
            List<VariableMutation> nextMutations = new ArrayList<>(entry.mutations);
            List<ScoreboardMutation> nextScoreboardMutations = new ArrayList<>(entry.scoreboardMutations);
            List<QuestDebugPrint> nextDebugPrints = new ArrayList<>(entry.debugPrints);
            nextMutations.addAll(passthrough.mutations);
            nextScoreboardMutations.addAll(passthrough.scoreboardMutations);
            nextDebugPrints.addAll(passthrough.debugPrints);
            ActionEntry nextEntry = new ActionEntry(null, objectiveId, nextMutations, nextScoreboardMutations,
                    nextDebugPrints, new ArrayList<>(entry.gates), entry.pathKey + ">" + node.getUid());
            int outgoingCount = outputConnectionCount(node, "next");
            boolean hasOutgoing = outgoingCount > 0;
            // 副作用节点后面分叉时，副作用应先执行一次，再把干净的执行线分发给各个后续分支。
            if (outgoingCount > 1 && hasSideEffects(nextEntry)) {
                addAction(actions, createAction(stepId, objectiveId, node, nextEntry));
                nextEntry = nextEntry.clearSideEffects();
            }
            followOutputFlow(node, "next", nextEntry, queue);
            if (!hasOutgoing && hasSideEffects(nextEntry)) {
                addAction(actions, createAction(stepId, objectiveId, node, nextEntry));
            }
        }
        return actions;
    }

    private static void compileBranch(QuestCompileContext context,
                                      CustomNodeModelImpl branchNode,
                                      ActionEntry entry,
                                      String stepId,
                                      String objectiveId,
                                      List<ObjectiveAction> actions,
                                      Queue<ActionEntry> queue) {
        ActionEntry branchEntry = entry;
        if (hasSideEffects(entry)) {
            addAction(actions, createAction(stepId, objectiveId, branchNode, entry));
            branchEntry = entry.resetForNext(branchNode);
        }
        QuestFlowGraphBuilder.CompareCondition condition = QuestFlowGraphBuilder.traceCondition(branchNode, context);
        QuestFlowGraphBuilder.validateBranch(branchNode, "objective_branch_" + branchNode.getUid(), condition);
        followBranchOutput(branchNode, "true", branchEntry, objectiveId, condition, false, queue);
        followBranchOutput(branchNode, "false", branchEntry, objectiveId, condition, true, queue);
    }

    private static void followBranchOutput(CustomNodeModelImpl branchNode,
                                           String portId,
                                           ActionEntry entry,
                                           String objectiveId,
                                           QuestFlowGraphBuilder.CompareCondition condition,
                                           boolean negate,
                                           Queue<ActionEntry> queue) {
        PortModel port = branchNode.getOutputsById().get(portId);
        if (port == null) {
            return;
        }
        for (PortModel connectedPort : port.getConnectedPorts()) {
            if (!(connectedPort.getNodeModel() instanceof CustomNodeModelImpl targetNode)) {
                continue;
            }
            QuestFlowEdge gate = new QuestFlowEdge();
            if (!condition.isEmpty()) {
                gate.compareOp = negate ? QuestFlowGraphBuilder.negateOp(condition.compareOp()) : condition.compareOp();
                gate.conditionLeftExpression.addAll(condition.leftExpression());
                gate.conditionRightExpression.addAll(condition.rightExpression());
            }
            List<QuestFlowEdge> gates = new ArrayList<>(entry.gates);
            gates.add(gate);
            queue.add(new ActionEntry(targetNode, objectiveId,
                    new ArrayList<>(entry.mutations),
                    new ArrayList<>(entry.scoreboardMutations),
                    new ArrayList<>(entry.debugPrints),
                    gates,
                    entry.pathKey + ">" + branchNode.getUid() + ":" + portId));
        }
    }

    private static QuestPassthroughResult compilePassthroughNode(QuestCompileContext context, CustomNodeModelImpl node) {
        QuestPassthroughResult result = new QuestPassthroughResult();
        IQuestPassthroughNodeCompiler compiler = context.findPassthroughCompiler(node);
        if (compiler != null) {
            compiler.compilePassthrough(context, node, result);
        }
        return result;
    }

    private static ObjectiveAction createAction(String stepId, String objectiveId,
                                                CustomNodeModelImpl node,
                                                ActionEntry entry) {
        ObjectiveAction action = new ObjectiveAction();
        action.actionId = objectiveId + ":" + entry.pathKey + ":" + node.getUid();
        action.stepId = stepId;
        action.objectiveId = objectiveId;
        action.gates.addAll(entry.gates);
        copyEdge(buildEdge(objectiveId, objectiveId, entry), action.edge);
        return action;
    }

    private static QuestFlowEdge buildEdge(String fromNodeId, String toNodeId,
                                           ActionEntry entry) {
        QuestFlowEdge edge = new QuestFlowEdge();
        edge.fromNodeId = fromNodeId;
        edge.toNodeId = toNodeId;
        edge.variableMutations.addAll(entry.mutations);
        edge.scoreboardMutations.addAll(entry.scoreboardMutations);
        edge.debugPrints.addAll(entry.debugPrints);
        return edge;
    }

    private static void copyEdge(QuestFlowEdge source, QuestFlowEdge target) {
        target.fromNodeId = source.fromNodeId;
        target.toNodeId = source.toNodeId;
        target.conditionVariable = source.conditionVariable;
        target.compareOp = source.compareOp;
        target.compareValue = source.compareValue;
        target.conditionLeftExpression.clear();
        target.conditionLeftExpression.addAll(source.conditionLeftExpression);
        target.conditionRightExpression.clear();
        target.conditionRightExpression.addAll(source.conditionRightExpression);
        target.variableMutations.clear();
        target.variableMutations.addAll(source.variableMutations);
        target.scoreboardMutations.clear();
        target.scoreboardMutations.addAll(source.scoreboardMutations);
        target.debugPrints.clear();
        target.debugPrints.addAll(source.debugPrints);
    }

    private static void addAction(List<ObjectiveAction> actions, ObjectiveAction action) {
        boolean duplicate = actions.stream().anyMatch(existing ->
                existing.stepId.equals(action.stepId)
                        && existing.objectiveId.equals(action.objectiveId)
                        && existing.actionId.equals(action.actionId)
                        && existing.gates.equals(action.gates)
                        && QuestFlowGraphBuilder.sameFlowEdge(existing.edge, action.edge)
                        && existing.rewards.equals(action.rewards));
        if (!duplicate) {
            actions.add(action);
        }
    }

    private static void followOutputFlow(CustomNodeModelImpl node, String portId, ActionEntry entry,
                                         Queue<ActionEntry> queue) {
        PortModel port = node.getOutputsById().get(portId);
        if (port == null) {
            return;
        }
        for (PortModel connected : port.getConnectedPorts()) {
            if (connected.getNodeModel() instanceof CustomNodeModelImpl targetNode) {
                queue.add(entry.withNode(targetNode));
            }
        }
    }

    private static boolean hasSideEffects(ActionEntry entry) {
        return !entry.mutations.isEmpty()
                || !entry.scoreboardMutations.isEmpty()
                || !entry.debugPrints.isEmpty();
    }

    private static boolean hasOutputConnection(CustomNodeModelImpl node, String portId) {
        return outputConnectionCount(node, portId) > 0;
    }

    private static int outputConnectionCount(CustomNodeModelImpl node, String portId) {
        PortModel port = node.getOutputsById().get(portId);
        return port == null ? 0 : port.getConnectedPorts().size();
    }

    private record ActionEntry(CustomNodeModelImpl node, String fromNodeId, List<VariableMutation> mutations,
                               List<ScoreboardMutation> scoreboardMutations, List<QuestDebugPrint> debugPrints,
                               List<QuestFlowEdge> gates, String pathKey) {
        private ActionEntry withNode(CustomNodeModelImpl node) {
            return new ActionEntry(node, fromNodeId, new ArrayList<>(mutations),
                    new ArrayList<>(scoreboardMutations), new ArrayList<>(debugPrints),
                    new ArrayList<>(gates), pathKey);
        }

        private ActionEntry resetForNext(CustomNodeModelImpl node) {
            return new ActionEntry(null, fromNodeId, new ArrayList<>(), new ArrayList<>(), new ArrayList<>(),
                    new ArrayList<>(gates), pathKey + ">" + node.getUid());
        }

        private ActionEntry clearSideEffects() {
            return new ActionEntry(null, fromNodeId, new ArrayList<>(), new ArrayList<>(), new ArrayList<>(),
                    new ArrayList<>(gates), pathKey);
        }
    }
}
