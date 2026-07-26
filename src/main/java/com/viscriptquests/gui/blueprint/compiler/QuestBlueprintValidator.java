package com.viscriptquests.gui.blueprint.compiler;

import com.viscriptquests.gui.blueprint.QuestBlueprintFlowTypes;
import com.viscriptquests.quest.data.QuestFile;
import com.viscriptquests.quest.data.QuestFlowEdge;
import com.viscriptquests.quest.data.QuestFlowNode;
import com.viscriptquests.quest.data.QuestJoinMode;
import com.viscriptquests.quest.data.ObjectiveAction;
import com.viscriptquests.quest.data.QuestStep;
import com.viscriptquests.quest.data.task.ITask;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class QuestBlueprintValidator {
    private QuestBlueprintValidator() {
    }

    public static void validateExport(QuestFile questFile) {
        Map<String, QuestFlowNode> nodes = indexNodes(questFile);
        validateBasicShape(questFile, nodes);
        Set<String> reachable = collectReachable(nodes, questFile.flowEdges);
        validateReachableFlow(nodes, reachable);
        validateEdges(questFile, nodes, reachable);
        validateSubQuestTargets(questFile, nodes, reachable);
        validateObjectiveFlow(questFile);
        validateSubQuestResultOutputs(questFile, nodes, reachable);
        validateBranchNodes(questFile, nodes, reachable);
        validateJoinNodes(questFile, nodes, reachable);
        validateAllReachableNodesCanFinish(nodes, questFile.flowEdges, reachable);
    }

    private static Map<String, QuestFlowNode> indexNodes(QuestFile questFile) {
        Map<String, QuestFlowNode> nodes = new LinkedHashMap<>();
        for (QuestFlowNode node : questFile.flowNodes) {
            if (node == null || node.nodeId == null) {
                continue;
            }
            if (nodes.containsKey(node.nodeId)) {
                throw QuestBlueprintValidationException.create(
                        "viscript_quests.editor.quest.export.validation.duplicate_node", displayNode(node.nodeId));
            }
            nodes.put(node.nodeId, node);
        }
        return nodes;
    }

    private static void validateBasicShape(QuestFile questFile, Map<String, QuestFlowNode> nodes) {
        if (!nodes.containsKey("")) {
            throw QuestBlueprintValidationException.create("viscript_quests.editor.quest.export.validation.missing_start");
        }
        if (questFile.tasks.isEmpty() || questFile.steps.isEmpty()) {
            throw QuestBlueprintValidationException.create("viscript_quests.editor.quest.export.validation.missing_step");
        }
        if (questFile.flowEdges.isEmpty()) {
            throw QuestBlueprintValidationException.create("viscript_quests.editor.quest.export.validation.missing_flow_edge");
        }
    }

    private static Set<String> collectReachable(Map<String, QuestFlowNode> nodes, List<QuestFlowEdge> edges) {
        Set<String> reachable = new LinkedHashSet<>();
        ArrayDeque<String> queue = new ArrayDeque<>();
        reachable.add("");
        queue.add("");
        while (!queue.isEmpty()) {
            String nodeId = queue.removeFirst();
            for (QuestFlowEdge edge : outgoing(nodeId, edges)) {
                if (!nodes.containsKey(edge.toNodeId)) {
                    continue;
                }
                if (reachable.add(edge.toNodeId)) {
                    queue.add(edge.toNodeId);
                }
            }
        }
        return reachable;
    }

    private static void validateReachableFlow(Map<String, QuestFlowNode> nodes, Set<String> reachable) {
        for (QuestFlowNode node : nodes.values()) {
            if (!reachable.contains(node.nodeId)) {
                throw QuestBlueprintValidationException.create(
                        "viscript_quests.editor.quest.export.validation.unreachable_node", displayNode(node.nodeId));
            }
        }
    }

    private static void validateSubQuestTargets(QuestFile questFile, Map<String, QuestFlowNode> nodes, Set<String> reachable) {
        Set<String> stepIds = new LinkedHashSet<>();
        for (QuestStep step : questFile.steps) {
            if (step != null && step.stepId != null && !step.stepId.isBlank()) {
                stepIds.add(step.stepId);
            }
        }

        Map<String, Integer> taskCounts = new LinkedHashMap<>();
        for (ITask task : questFile.tasks) {
            if (task == null || task.stepId == null || task.stepId.isBlank()) {
                continue;
            }
            if (!nodes.containsKey(task.stepId)) {
                throw QuestBlueprintValidationException.create(
                        "viscript_quests.editor.quest.export.validation.task_not_in_flow", task.stepId);
            }
            taskCounts.merge(task.stepId, 1, Integer::sum);
        }

        for (QuestFlowNode node : nodes.values()) {
            if (!reachable.contains(node.nodeId) || !QuestBlueprintFlowTypes.isSubQuest(node)) {
                continue;
            }
            if (!stepIds.contains(node.stepId)) {
                throw QuestBlueprintValidationException.create(
                        "viscript_quests.editor.quest.export.validation.step_missing_metadata", displayNode(node.stepId));
            }
            if (taskCounts.getOrDefault(node.stepId, 0) <= 0) {
                throw QuestBlueprintValidationException.create(
                        "viscript_quests.editor.quest.export.validation.step_missing_task", displayNode(node.stepId));
            }
        }
    }

    private static void validateSubQuestResultOutputs(QuestFile questFile, Map<String, QuestFlowNode> nodes,
                                                      Set<String> reachable) {
        for (QuestFlowNode node : nodes.values()) {
            if (!reachable.contains(node.nodeId) || !QuestBlueprintFlowTypes.isSubQuest(node)) {
                continue;
            }
            boolean hasResultOutput = outgoing(node.nodeId, questFile.flowEdges).stream()
                    .anyMatch(edge -> edge.stepResult != null && edge.stepResult.isSpecificResult());
            if (!hasResultOutput) {
                throw QuestBlueprintValidationException.create(
                        "viscript_quests.editor.quest.export.validation.sub_quest_missing_result_output",
                        displayNode(node.stepId));
            }
        }
    }

    private static void validateObjectiveFlow(QuestFile questFile) {
        Map<String, Map<String, ITask>> tasksByStep = new LinkedHashMap<>();
        for (ITask task : questFile.tasks) {
            if (task == null || task.stepId == null || task.stepId.isBlank()
                    || task.objectiveId == null || task.objectiveId.isBlank()) {
                continue;
            }
            Map<String, ITask> tasks = tasksByStep.computeIfAbsent(task.stepId, ignored -> new LinkedHashMap<>());
            if (tasks.putIfAbsent(task.objectiveId, task) != null) {
                throw QuestBlueprintValidationException.create(
                        "viscript_quests.editor.quest.export.validation.objective_duplicate",
                        displayNode(task.objectiveId));
            }
        }

        for (var stepEntry : tasksByStep.entrySet()) {
            String stepId = stepEntry.getKey();
            Map<String, ITask> tasks = stepEntry.getValue();
            if (tasks.values().stream().noneMatch(task -> task.initiallyActive)) {
                throw QuestBlueprintValidationException.create(
                        "viscript_quests.editor.quest.export.validation.objective_flow_no_start",
                        displayNode(stepId));
            }

            Map<String, Set<String>> edges = new LinkedHashMap<>();
            for (ObjectiveAction action : questFile.objectiveActions) {
                if (action == null || !stepId.equals(action.stepId) || action.activateObjectiveIds.isEmpty()) {
                    continue;
                }
                if (!tasks.containsKey(action.objectiveId)) {
                    throw QuestBlueprintValidationException.create(
                            "viscript_quests.editor.quest.export.validation.objective_activation_source_missing",
                            displayNode(action.objectiveId), displayNode(stepId));
                }
                for (String targetId : action.activateObjectiveIds) {
                    if (!tasks.containsKey(targetId)) {
                        throw QuestBlueprintValidationException.create(
                                "viscript_quests.editor.quest.export.validation.objective_activation_target_missing",
                                displayNode(targetId), displayNode(stepId));
                    }
                    edges.computeIfAbsent(action.objectiveId, ignored -> new LinkedHashSet<>()).add(targetId);
                }
            }
            Set<String> visiting = new LinkedHashSet<>();
            Set<String> visited = new LinkedHashSet<>();
            for (String objectiveId : tasks.keySet()) {
                if (hasObjectiveCycle(objectiveId, edges, visiting, visited)) {
                    throw QuestBlueprintValidationException.create(
                            "viscript_quests.editor.quest.export.validation.objective_flow_cycle",
                            displayNode(objectiveId));
                }
            }
        }
    }

    private static boolean hasObjectiveCycle(String objectiveId, Map<String, Set<String>> edges,
                                             Set<String> visiting, Set<String> visited) {
        if (visited.contains(objectiveId)) {
            return false;
        }
        if (!visiting.add(objectiveId)) {
            return true;
        }
        for (String targetId : edges.getOrDefault(objectiveId, Set.of())) {
            if (hasObjectiveCycle(targetId, edges, visiting, visited)) {
                return true;
            }
        }
        visiting.remove(objectiveId);
        visited.add(objectiveId);
        return false;
    }

    private static void validateEdges(QuestFile questFile, Map<String, QuestFlowNode> nodes, Set<String> reachable) {
        for (QuestFlowEdge edge : questFile.flowEdges) {
            if (edge == null) {
                continue;
            }
            if (!nodes.containsKey(edge.fromNodeId)) {
                throw QuestBlueprintValidationException.create(
                        "viscript_quests.editor.quest.export.validation.edge_missing_from", displayNode(edge.fromNodeId));
            }
            if (!nodes.containsKey(edge.toNodeId)) {
                throw QuestBlueprintValidationException.create(
                        "viscript_quests.editor.quest.export.validation.edge_missing_to", displayNode(edge.toNodeId));
            }
            if (edge.fromNodeId.equals(edge.toNodeId)) {
                throw QuestBlueprintValidationException.create(
                        "viscript_quests.editor.quest.export.validation.self_loop", displayNode(edge.fromNodeId));
            }
            if (!reachable.contains(edge.fromNodeId) || !reachable.contains(edge.toNodeId)) {
                throw QuestBlueprintValidationException.create(
                        "viscript_quests.editor.quest.export.validation.unreachable_edge",
                        displayNode(edge.fromNodeId), displayNode(edge.toNodeId));
            }
        }
    }

    private static void validateBranchNodes(QuestFile questFile, Map<String, QuestFlowNode> nodes, Set<String> reachable) {
        for (QuestFlowNode node : nodes.values()) {
            if (!reachable.contains(node.nodeId) || !QuestBlueprintFlowTypes.isType(node, QuestBlueprintFlowTypes.BRANCH)) {
                continue;
            }
            List<QuestFlowEdge> outgoing = outgoing(node.nodeId, questFile.flowEdges);
            if (outgoing.isEmpty()) {
                throw QuestBlueprintValidationException.create(
                        "viscript_quests.editor.quest.export.validation.branch_missing_output", displayNode(node.nodeId));
            }
            boolean hasCondition = outgoing.stream()
                    .anyMatch(QuestBlueprintValidator::hasCondition);
            if (!hasCondition) {
                throw QuestBlueprintValidationException.create(
                        "viscript_quests.editor.quest.export.validation.branch_missing_condition", displayNode(node.nodeId));
            }
        }
    }

    private static void validateJoinNodes(QuestFile questFile, Map<String, QuestFlowNode> nodes, Set<String> reachable) {
        for (QuestFlowNode node : nodes.values()) {
            if (!reachable.contains(node.nodeId) || !QuestBlueprintFlowTypes.isJoin(node)) {
                continue;
            }
            int incomingCount = incoming(node.nodeId, questFile.flowEdges).size();
            int outgoingCount = outgoing(node.nodeId, questFile.flowEdges).size();
            if (incomingCount <= 0) {
                throw QuestBlueprintValidationException.create(
                        "viscript_quests.editor.quest.export.validation.join_no_input", displayNode(node.nodeId));
            }
            if (outgoingCount <= 0) {
                throw QuestBlueprintValidationException.create(
                        "viscript_quests.editor.quest.export.validation.join_no_output", displayNode(node.nodeId));
            }
            if (node.joinMode == QuestJoinMode.COUNT && (node.requiredCount <= 0 || node.requiredCount > incomingCount)) {
                throw QuestBlueprintValidationException.create(
                        "viscript_quests.editor.quest.export.validation.join_count_invalid",
                        displayNode(node.nodeId), node.requiredCount, incomingCount);
            }
        }
    }

    private static void validateAllReachableNodesCanFinish(Map<String, QuestFlowNode> nodes, List<QuestFlowEdge> edges,
                                                           Set<String> reachable) {
        Set<String> canFinish = new LinkedHashSet<>();
        boolean changed;
        do {
            changed = false;
            for (String nodeId : reachable) {
                if (canFinish.contains(nodeId)) {
                    continue;
                }
                QuestFlowNode node = nodes.get(nodeId);
                if (QuestBlueprintFlowTypes.isEnd(node)) {
                    changed |= canFinish.add(nodeId);
                    continue;
                }
                List<QuestFlowEdge> outgoing = outgoing(nodeId, edges).stream()
                        .filter(edge -> reachable.contains(edge.toNodeId))
                        .toList();
                if (!outgoing.isEmpty() && outgoing.stream().anyMatch(edge -> canFinish.contains(edge.toNodeId))) {
                    changed |= canFinish.add(nodeId);
                }
            }
        } while (changed);

        for (String nodeId : reachable) {
            if (!canFinish.contains(nodeId)) {
                throw QuestBlueprintValidationException.create(
                        "viscript_quests.editor.quest.export.validation.no_end_path", displayNode(nodeId));
            }
        }
    }

    private static List<QuestFlowEdge> outgoing(String nodeId, List<QuestFlowEdge> edges) {
        List<QuestFlowEdge> result = new ArrayList<>();
        if (edges == null) {
            return result;
        }
        for (QuestFlowEdge edge : edges) {
            if (edge != null && nodeId.equals(edge.fromNodeId)) {
                result.add(edge);
            }
        }
        return result;
    }

    private static List<QuestFlowEdge> incoming(String nodeId, List<QuestFlowEdge> edges) {
        List<QuestFlowEdge> result = new ArrayList<>();
        for (QuestFlowEdge edge : edges) {
            if (edge != null && nodeId.equals(edge.toNodeId)) {
                result.add(edge);
            }
        }
        return result;
    }

    private static boolean hasCondition(QuestFlowEdge edge) {
        return edge != null && ((edge.conditionVariable != null && !edge.conditionVariable.isBlank())
                || !edge.conditionLeftExpression.isEmpty()
                || !edge.conditionRightExpression.isEmpty());
    }

    private static String displayNode(String nodeId) {
        return nodeId == null || nodeId.isBlank() ? "START" : nodeId;
    }
}
