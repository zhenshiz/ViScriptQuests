package com.viscriptquests.gui.blueprint.compiler;

import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.CustomNodeModelImpl;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.PortModel;
import com.viscriptquests.gui.blueprint.QuestBlueprintCompiler.CompareOp;
import com.viscriptquests.gui.blueprint.QuestBlueprintFlowTypes;
import com.viscriptquests.gui.blueprint.node.flow.*;
import com.viscriptquests.gui.blueprint.node.logic.*;
import com.viscriptquests.quest.data.*;

import java.util.*;

// 将蓝图执行流编译为运行时流程图
public final class QuestFlowGraphBuilder {

    // BFS 追踪蓝图执行流，将任务、汇合、分支、终点都落成运行时流程图节点。
    public static Result build(CustomNodeModelImpl startNode, QuestCompileContext context) {
        List<String> ordered = new ArrayList<>();
        Map<String, QuestFlowNode> flowNodes = new LinkedHashMap<>();
        List<QuestFlowEdge> flowEdges = new ArrayList<>();
        Set<String> visitedStepIds = new LinkedHashSet<>();

        Queue<FlowEntry> queue = new ArrayDeque<>();
        Set<String> visitedFlows = new LinkedHashSet<>();
        flowNodes.put("", QuestBlueprintFlowTypes.createStart());
        queue.add(new FlowEntry(startNode, "", new ArrayList<>(), new ArrayList<>(), new ArrayList<>()));

        int maxSteps = 1000;
        while (!queue.isEmpty()) {
            if (maxSteps-- <= 0) {
                throw QuestBlueprintValidationException.create(
                        "viscript_quests.editor.quest.export.validation.flow_trace_limit");
            }
            FlowEntry entry = queue.poll();
            CustomNodeModelImpl currentNode = entry.node;
            String fromNodeId = entry.fromNodeId;
            List<VariableMutation> pendingMutations = entry.mutations;
            List<ScoreboardMutation> pendingScoreboardMutations = entry.scoreboardMutations;
            List<QuestDebugPrint> pendingDebugPrints = entry.debugPrints;
            var nodeInstance = currentNode.getNode();
            if (nodeInstance == null) continue;

            if (nodeInstance instanceof QuestStartNode) {
                followOutputFlow(currentNode, "next", fromNodeId, pendingMutations, pendingScoreboardMutations,
                        pendingDebugPrints, queue);
            } else if (nodeInstance instanceof SubQuestNode) {
                String stepId = context.resolveStepId(currentNode);
                if (!stepId.isEmpty()) {
                    flowNodes.putIfAbsent(stepId, QuestBlueprintFlowTypes.createSubQuest(stepId));
                    addFlowEdge(flowEdges, fromNodeId, stepId, pendingMutations, pendingScoreboardMutations,
                            pendingDebugPrints);
                    if (visitedStepIds.add(stepId)) {
                        ordered.add(stepId);
                    }
                    followOutputFlow(currentNode, "next", stepId, new ArrayList<>(), new ArrayList<>(),
                            new ArrayList<>(), queue);
                }
            } else if (nodeInstance instanceof QuestBranchNode) {
                String branchNodeId = "branch_" + currentNode.getUid();
                flowNodes.putIfAbsent(branchNodeId, QuestBlueprintFlowTypes.createBranch(branchNodeId));
                addFlowEdge(flowEdges, fromNodeId, branchNodeId, pendingMutations, pendingScoreboardMutations,
                        pendingDebugPrints);
                if (!visitedFlows.add(branchNodeId + "_" + fromNodeId)) continue;
                CompareCondition condition = traceCondition(currentNode, context);
                validateBranch(currentNode, branchNodeId, condition);
                followBranchOutput(currentNode, "true", branchNodeId, new ArrayList<>(), new ArrayList<>(),
                        new ArrayList<>(),
                        context,
                        condition, false, flowNodes, flowEdges, queue);
                followBranchOutput(currentNode, "false", branchNodeId, new ArrayList<>(), new ArrayList<>(),
                        new ArrayList<>(),
                        context,
                        condition, true, flowNodes, flowEdges, queue);
            } else if (nodeInstance instanceof QuestEndNode) {
                if (!fromNodeId.isEmpty()) {
                    String endNodeId = "end_" + currentNode.getUid();
                    QuestFlowNode endNode = QuestBlueprintFlowTypes.createEnd(endNodeId, context.getBool(currentNode, "success"));
                    flowNodes.putIfAbsent(endNodeId, endNode);
                    addFlowEdge(flowEdges, fromNodeId, endNodeId, pendingMutations, pendingScoreboardMutations,
                            pendingDebugPrints);
                }
            } else if (nodeInstance instanceof QuestJoinNode) {
                String joinId = "join_" + currentNode.getUid();
                QuestJoinMode joinMode = context.getJoinMode(currentNode);
                int requiredCount = joinMode == QuestJoinMode.COUNT ? context.getPortInt(currentNode, "required_count") : 0;
                flowNodes.putIfAbsent(joinId, QuestBlueprintFlowTypes.createJoin(joinId, joinMode, requiredCount));
                if (!fromNodeId.isEmpty()) {
                    addFlowEdge(flowEdges, fromNodeId, joinId, pendingMutations, pendingScoreboardMutations,
                            pendingDebugPrints);
                }
                followOutputFlow(currentNode, "next", joinId,
                        new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), queue);
            } else {
                QuestPassthroughResult passthrough = compilePassthroughNode(context, currentNode);
                List<VariableMutation> newMutations = new ArrayList<>(pendingMutations);
                List<ScoreboardMutation> newScoreboardMutations = new ArrayList<>(pendingScoreboardMutations);
                List<QuestDebugPrint> newDebugPrints = new ArrayList<>(pendingDebugPrints);
                newMutations.addAll(passthrough.mutations);
                newScoreboardMutations.addAll(passthrough.scoreboardMutations);
                newDebugPrints.addAll(passthrough.debugPrints);
                followOutputFlow(currentNode, "next", fromNodeId, newMutations, newScoreboardMutations,
                        newDebugPrints, queue);
            }
        }

        return new Result(ordered, new ArrayList<>(flowNodes.values()), flowEdges);
    }

    private static QuestPassthroughResult compilePassthroughNode(QuestCompileContext context, CustomNodeModelImpl node) {
        QuestPassthroughResult result = new QuestPassthroughResult();
        var compiler = context.findPassthroughCompiler(node);
        if (compiler != null) {
            compiler.compilePassthrough(context, node, result);
        }
        return result;
    }

    private static boolean addFlowEdge(List<QuestFlowEdge> flowEdges, String fromNodeId, String toNodeId,
                                       List<VariableMutation> mutations, List<ScoreboardMutation> scoreboardMutations,
                                       List<QuestDebugPrint> debugPrints) {
        if (fromNodeId.equals(toNodeId)) {
            return false;
        }
        QuestFlowEdge edge = new QuestFlowEdge();
        edge.fromNodeId = fromNodeId;
        edge.toNodeId = toNodeId;
        edge.variableMutations.addAll(mutations);
        edge.scoreboardMutations.addAll(scoreboardMutations);
        edge.debugPrints.addAll(debugPrints);
        if (flowEdges.stream().anyMatch(existing -> sameFlowEdge(existing, edge))) {
            return false;
        }
        flowEdges.add(edge);
        return true;
    }

    private static boolean addConditionalFlowEdge(List<QuestFlowEdge> flowEdges, String fromNodeId, String toNodeId,
                                                  List<VariableMutation> mutations, List<ScoreboardMutation> scoreboardMutations,
                                                  List<QuestDebugPrint> debugPrints,
                                                  CompareCondition condition, boolean negate) {
        if (fromNodeId.equals(toNodeId)) {
            return false;
        }
        QuestFlowEdge edge = new QuestFlowEdge();
        edge.fromNodeId = fromNodeId;
        edge.toNodeId = toNodeId;
        edge.variableMutations.addAll(mutations);
        edge.scoreboardMutations.addAll(scoreboardMutations);
        edge.debugPrints.addAll(debugPrints);
        if (!condition.leftExpression.isEmpty() || !condition.rightExpression.isEmpty()) {
            edge.compareOp = negate ? negateOp(condition.compareOp) : condition.compareOp;
            edge.conditionLeftExpression.addAll(condition.leftExpression);
            edge.conditionRightExpression.addAll(condition.rightExpression);
        }
        if (flowEdges.stream().anyMatch(existing -> sameFlowEdge(existing, edge))) {
            return false;
        }
        flowEdges.add(edge);
        return true;
    }

    private static boolean sameFlowEdge(QuestFlowEdge a, QuestFlowEdge b) {
        return a.fromNodeId.equals(b.fromNodeId)
                && a.toNodeId.equals(b.toNodeId)
                && Objects.equals(a.conditionVariable, b.conditionVariable)
                && a.compareOp == b.compareOp
                && Float.compare(a.compareValue, b.compareValue) == 0
                && Objects.equals(a.conditionLeftExpression, b.conditionLeftExpression)
                && Objects.equals(a.conditionRightExpression, b.conditionRightExpression)
                && Objects.equals(a.variableMutations, b.variableMutations)
                && Objects.equals(a.scoreboardMutations, b.scoreboardMutations)
                && Objects.equals(a.debugPrints, b.debugPrints);
    }

    private static void followOutputFlow(CustomNodeModelImpl node, String portId, String fromStepId,
                                         List<VariableMutation> mutations, List<ScoreboardMutation> scoreboardMutations,
                                         List<QuestDebugPrint> debugPrints,
                                         Queue<FlowEntry> queue) {
        PortModel port = node.getOutputsById().get(portId);
        if (port == null) return;
        for (PortModel connected : port.getConnectedPorts()) {
            if (connected.getNodeModel() instanceof CustomNodeModelImpl targetNode) {
                queue.add(new FlowEntry(targetNode, fromStepId, new ArrayList<>(mutations),
                        new ArrayList<>(scoreboardMutations),
                        new ArrayList<>(debugPrints)));
            }
        }
    }

    private static void followBranchOutput(CustomNodeModelImpl branchNode, String portId, String fromNodeId,
                                           List<VariableMutation> mutations, List<ScoreboardMutation> scoreboardMutations,
                                           List<QuestDebugPrint> debugPrints,
                                           QuestCompileContext context,
                                           CompareCondition condition, boolean negate,
                                           Map<String, QuestFlowNode> flowNodes, List<QuestFlowEdge> flowEdges,
                                           Queue<FlowEntry> queue) {
        PortModel port = branchNode.getOutputsById().get(portId);
        if (port == null) return;
        for (PortModel connectedPort : port.getConnectedPorts()) {
            if (!(connectedPort.getNodeModel() instanceof CustomNodeModelImpl targetNode)) {
                continue;
            }
            Queue<FlowEntry> traceQueue = new ArrayDeque<>();
            traceQueue.add(new FlowEntry(targetNode, fromNodeId, new ArrayList<>(mutations),
                    new ArrayList<>(scoreboardMutations),
                    new ArrayList<>(debugPrints)));
            traceBranchFlow(traceQueue, context, condition, negate, flowNodes, flowEdges, queue);
        }
    }

    private static void traceBranchFlow(Queue<FlowEntry> traceQueue, QuestCompileContext context,
                                        CompareCondition condition, boolean negate,
                                        Map<String, QuestFlowNode> flowNodes, List<QuestFlowEdge> flowEdges,
                                        Queue<FlowEntry> mainQueue) {
        int maxHops = 100;
        while (!traceQueue.isEmpty()) {
            if (maxHops-- <= 0) {
                throw QuestBlueprintValidationException.create(
                        "viscript_quests.editor.quest.export.validation.branch_trace_limit");
            }
            FlowEntry entry = traceQueue.poll();
            CustomNodeModelImpl currentNode = entry.node;
            String fromNodeId = entry.fromNodeId;
            List<VariableMutation> mutations = entry.mutations;
            List<ScoreboardMutation> scoreboardMutations = entry.scoreboardMutations;
            List<QuestDebugPrint> debugPrints = entry.debugPrints;
            var node = currentNode.getNode();
            if (node == null) continue;

            if (node instanceof SubQuestNode) {
                String stepId = context.resolveStepId(currentNode);
                if (stepId.isEmpty()) continue;
                flowNodes.putIfAbsent(stepId, QuestBlueprintFlowTypes.createSubQuest(stepId));
                addConditionalFlowEdge(flowEdges, fromNodeId, stepId, mutations, scoreboardMutations,
                        debugPrints, condition, negate);
                followOutputFlow(currentNode, "next", stepId, new ArrayList<>(), new ArrayList<>(),
                        new ArrayList<>(), mainQueue);
                continue;
            }

            if (node instanceof QuestJoinNode) {
                String joinId = "join_" + currentNode.getUid();
                QuestJoinMode mode = context.getJoinMode(currentNode);
                int requiredCount = mode == QuestJoinMode.COUNT ? context.getPortInt(currentNode, "required_count") : 0;
                flowNodes.putIfAbsent(joinId, QuestBlueprintFlowTypes.createJoin(joinId, mode, requiredCount));
                addConditionalFlowEdge(flowEdges, fromNodeId, joinId, mutations, scoreboardMutations,
                        debugPrints, condition, negate);
                followOutputFlow(currentNode, "next", joinId, new ArrayList<>(), new ArrayList<>(),
                        new ArrayList<>(), mainQueue);
                continue;
            }

            if (node instanceof QuestBranchNode) {
                String branchNodeId = "branch_" + currentNode.getUid();
                flowNodes.putIfAbsent(branchNodeId, QuestBlueprintFlowTypes.createBranch(branchNodeId));
                addConditionalFlowEdge(flowEdges, fromNodeId, branchNodeId, mutations, scoreboardMutations,
                        debugPrints, condition, negate);
                CompareCondition nestedCondition = traceCondition(currentNode, context);
                validateBranch(currentNode, branchNodeId, nestedCondition);
                mainQueue.add(new FlowEntry(currentNode, branchNodeId, new ArrayList<>(), new ArrayList<>(),
                        new ArrayList<>()));
                continue;
            }

            if (node instanceof QuestEndNode) {
                String endNodeId = "end_" + currentNode.getUid();
                flowNodes.putIfAbsent(endNodeId, QuestBlueprintFlowTypes.createEnd(endNodeId, context.getBool(currentNode, "success")));
                addConditionalFlowEdge(flowEdges, fromNodeId, endNodeId, mutations, scoreboardMutations,
                        debugPrints, condition, negate);
                continue;
            }

            QuestPassthroughResult passthrough = compilePassthroughNode(context, currentNode);
            List<VariableMutation> nextMutations = new ArrayList<>(mutations);
            List<ScoreboardMutation> nextScoreboardMutations = new ArrayList<>(scoreboardMutations);
            List<QuestDebugPrint> nextDebugPrints = new ArrayList<>(debugPrints);
            nextMutations.addAll(passthrough.mutations);
            nextScoreboardMutations.addAll(passthrough.scoreboardMutations);
            nextDebugPrints.addAll(passthrough.debugPrints);
            followOutputFlow(currentNode, "next", fromNodeId, nextMutations, nextScoreboardMutations,
                    nextDebugPrints, traceQueue);
        }
    }

    private static CompareCondition traceCondition(CustomNodeModelImpl branchNode, QuestCompileContext context) {
        PortModel conditionPort = branchNode.getInputsById().get("condition");
        if (conditionPort == null) return CompareCondition.empty();

        List<PortModel> connected = conditionPort.getConnectedPorts();
        if (connected.isEmpty()) return CompareCondition.empty();

        for (PortModel connectedPort : connected) {
            if (connectedPort.getNodeModel() instanceof CustomNodeModelImpl sourceNode) {
                var sourceInstance = sourceNode.getNode();
                if (sourceInstance instanceof CompareOperationNode) {
                    CompareOp op = CompareOperationNode.operationOf(sourceNode);
                    List<QuestValueToken> left = context.compileRuntimeValueExpression(sourceNode, "value_a", 12);
                    List<QuestValueToken> right = context.compileRuntimeValueExpression(sourceNode, "value_b", 12);
                    if (left != null && right != null && (!left.isEmpty() || !right.isEmpty())) {
                        return new CompareCondition(left, op, right);
                    }
                    break;
                }
            }
        }
        return CompareCondition.empty();
    }

    private static void validateBranch(CustomNodeModelImpl branchNode, String branchNodeId, CompareCondition condition) {
        if (condition.isEmpty()) {
            throw QuestBlueprintValidationException.create(
                    "viscript_quests.editor.quest.export.validation.branch_missing_condition", branchNodeId);
        }
        if (!hasOutputConnection(branchNode, "true") || !hasOutputConnection(branchNode, "false")) {
            throw QuestBlueprintValidationException.create(
                    "viscript_quests.editor.quest.export.validation.branch_missing_output", branchNodeId);
        }
    }

    private static boolean hasOutputConnection(CustomNodeModelImpl node, String portId) {
        PortModel port = node.getOutputsById().get(portId);
        return port != null && !port.getConnectedPorts().isEmpty();
    }

    private static CompareOp reverseOp(CompareOp op) {
        return switch (op) {
            case EQ -> CompareOp.EQ;
            case NE -> CompareOp.NE;
            case GT -> CompareOp.LT;
            case GE -> CompareOp.LE;
            case LT -> CompareOp.GT;
            case LE -> CompareOp.GE;
        };
    }

    private static CompareOp negateOp(CompareOp op) {
        return switch (op) {
            case EQ -> CompareOp.NE;
            case NE -> CompareOp.EQ;
            case GT -> CompareOp.LE;
            case GE -> CompareOp.LT;
            case LT -> CompareOp.GE;
            case LE -> CompareOp.GT;
        };
    }

    public record Result(List<String> orderedStepIds, List<QuestFlowNode> flowNodes, List<QuestFlowEdge> flowEdges) {
    }

    private record CompareCondition(List<QuestValueToken> leftExpression, CompareOp compareOp,
                                    List<QuestValueToken> rightExpression) {
        private static CompareCondition empty() {
            return new CompareCondition(List.of(), CompareOp.EQ, List.of());
        }

        private boolean isEmpty() {
            return leftExpression.isEmpty() && rightExpression.isEmpty();
        }
    }

    private record FlowEntry(CustomNodeModelImpl node, String fromNodeId, List<VariableMutation> mutations,
                             List<ScoreboardMutation> scoreboardMutations, List<QuestDebugPrint> debugPrints) {
    }
}
