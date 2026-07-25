package com.viscriptquests.gui.blueprint;

import com.lowdragmc.lowdraglib2.nodegraphtookit.api.graph.GraphLogger;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.type.TypeHandles;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.constant.Constant;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.graph.GraphModel;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.AbstractNodeModel;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.CustomNodeModelImpl;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.PortModel;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.SubgraphNodeModel;
import com.viscriptquests.gui.blueprint.model.QuestSubQuestNodeModel;
import com.viscriptquests.gui.blueprint.node.QuestBlueprintNode;
import com.viscriptquests.gui.blueprint.node.flow.QuestEndNode;
import com.viscriptquests.gui.blueprint.node.flow.QuestJoinNode;
import com.viscriptquests.gui.blueprint.node.flow.QuestStartNode;
import com.viscriptquests.gui.blueprint.node.flow.SubQuestNode;
import com.viscriptquests.gui.blueprint.node.flow.SubQuestStartNode;
import net.minecraft.network.chat.Component;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * 轻量图诊断，只检查编辑期结构问题，不触发完整导出编译。
 */
final class QuestBlueprintGraphDiagnostics {
    private static final String VALIDATION_KEY = "viscript_quests.editor.quest.export.validation.";
    private static final String DIAGNOSTIC_KEY = "viscript_quests.editor.quest.graph.diagnostic.";

    private QuestBlueprintGraphDiagnostics() {
    }

    static void log(QuestBlueprintGraphModel model, GraphLogger logger) {
        if (model.isSubQuestContentGraph()) {
            logSubQuestGraph(model, logger);
            return;
        }
        logRootGraph(model, logger);
    }

    private static void logRootGraph(QuestBlueprintGraphModel model, GraphLogger logger) {
        List<CustomNodeModelImpl> starts = findNodes(model, QuestStartNode.class);
        if (starts.isEmpty()) {
            logger.error(translatable("missing_start"));
        } else if (starts.size() > 1) {
            logger.error(translatable("duplicate_start"), starts.get(1));
        }

        if (findNodes(model, QuestEndNode.class).isEmpty()) {
            logger.warning(Component.translatable(DIAGNOSTIC_KEY + "missing_end"));
        }

        if (starts.size() == 1) {
            if (!hasConnectedOutput(starts.getFirst(), "next")) {
                logger.error(translatable("missing_flow_edge"), starts.getFirst());
            }
            warnUnreachableFlowNodes(model, starts.getFirst(), logger);
        }

        for (CustomNodeModelImpl node : customNodes(model)) {
            if (node.getNode() instanceof SubQuestNode) {
                logSubQuestNode(node, logger);
            } else if (node.getNode() instanceof QuestJoinNode) {
                logJoinNode(node, logger);
            }
        }
    }

    private static void logSubQuestGraph(QuestBlueprintGraphModel model, GraphLogger logger) {
        List<CustomNodeModelImpl> starts = findNodes(model, SubQuestStartNode.class);
        if (starts.size() > 1) {
            logger.error(translatable("duplicate_subquest_start"), starts.get(1));
        }

        if (starts.isEmpty()) {
            if (!hasNodeInGroup(model, QuestBlueprintNode.TASK_GROUP, new HashSet<>())) {
                logger.error(translatable("step_missing_task", Component.translatable("viscript_quests.blueprint.node.sub_quest")));
            } else {
                logger.info(Component.translatable(DIAGNOSTIC_KEY + "subquest_start_recommended"));
            }
            return;
        }

        CustomNodeModelImpl start = starts.getFirst();
        Set<UUID> reachable = collectReachable(start);
        boolean hasReachableTask = customNodes(model).stream()
                .filter(node -> reachable.contains(node.getUid()))
                .anyMatch(node -> isNodeInGroup(node, QuestBlueprintNode.TASK_GROUP));
        if (!hasReachableTask) {
            logger.error(translatable("step_missing_task", Component.translatable("viscript_quests.blueprint.node.sub_quest")), start);
        }
        warnUnreachableFlowNodes(model, start, logger);
    }

    private static void logSubQuestNode(CustomNodeModelImpl node, GraphLogger logger) {
        GraphModel subgraph = node instanceof QuestSubQuestNodeModel subQuestNode
                ? subQuestNode.getSubgraphModel()
                : null;
        if (subgraph == null) {
            logger.error(Component.translatable(DIAGNOSTIC_KEY + "sub_quest_missing_graph", nodeLabel(node)), node);
            return;
        }
        if (!hasNodeInGroup(subgraph, QuestBlueprintNode.TASK_GROUP, new HashSet<>())) {
            logger.error(translatable("step_missing_task", nodeLabel(node)), node);
        }
        if (!hasConnectedOutput(node, "success") && !hasConnectedOutput(node, "failure")) {
            logger.error(translatable("sub_quest_missing_result_output", nodeLabel(node)), node);
        }
    }

    private static void logJoinNode(CustomNodeModelImpl node, GraphLogger logger) {
        int incomingCount = connectedInputCount(node, "in");
        int outgoingCount = connectedOutputCount(node, "next");
        if (incomingCount <= 0) {
            logger.error(translatable("join_no_input", nodeLabel(node)), node);
        }
        if (outgoingCount <= 0) {
            logger.error(translatable("join_no_output", nodeLabel(node)), node);
        }

        PortModel requiredCountPort = node.getInputsById().get("required_count");
        if (requiredCountPort == null || requiredCountPort.isConnected()) {
            return;
        }
        Constant constant = requiredCountPort.getEmbeddedValue();
        Object value = constant == null ? null : constant.getValue();
        int requiredCount = value instanceof Number number ? number.intValue() : 0;
        if (requiredCount <= 0 || requiredCount > incomingCount) {
            logger.error(translatable("join_count_invalid", nodeLabel(node), requiredCount, incomingCount), node);
        }
    }

    private static void warnUnreachableFlowNodes(GraphModel graphModel, CustomNodeModelImpl start, GraphLogger logger) {
        Set<UUID> reachable = collectReachable(start);
        for (CustomNodeModelImpl node : customNodes(graphModel)) {
            if (node == start || !isFlowNode(node) || reachable.contains(node.getUid())) {
                continue;
            }
            logger.warning(translatable("unreachable_node", nodeLabel(node)), node);
        }
    }

    private static Set<UUID> collectReachable(CustomNodeModelImpl start) {
        Set<UUID> visited = new LinkedHashSet<>();
        ArrayDeque<CustomNodeModelImpl> queue = new ArrayDeque<>();
        queue.add(start);
        while (!queue.isEmpty()) {
            CustomNodeModelImpl current = queue.removeFirst();
            if (current == null || !visited.add(current.getUid())) {
                continue;
            }
            for (PortModel output : current.getOutputsById().values()) {
                if (!TypeHandles.EXECUTION_FLOW.equals(output.getDataTypeHandle())) {
                    continue;
                }
                for (PortModel connectedPort : output.getConnectedPorts()) {
                    if (connectedPort.getNodeModel() instanceof CustomNodeModelImpl custom) {
                        queue.add(custom);
                    }
                }
            }
        }
        return visited;
    }

    private static boolean hasNodeInGroup(GraphModel graphModel, String group, Set<UUID> visitedGraphs) {
        if (graphModel == null || graphModel.getUid() == null || !visitedGraphs.add(graphModel.getUid())) {
            return false;
        }
        for (AbstractNodeModel nodeModel : graphModel.getNodeModels()) {
            if (nodeModel instanceof CustomNodeModelImpl custom && isNodeInGroup(custom, group)) {
                return true;
            }
            GraphModel nested = nestedGraph(nodeModel);
            if (nested != null && hasNodeInGroup(nested, group, visitedGraphs)) {
                return true;
            }
        }
        return false;
    }

    private static GraphModel nestedGraph(AbstractNodeModel nodeModel) {
        if (nodeModel instanceof QuestSubQuestNodeModel subQuestNode) {
            return subQuestNode.getSubgraphModel();
        }
        if (nodeModel instanceof SubgraphNodeModel subgraphNode) {
            return subgraphNode.getSubgraphModel();
        }
        return null;
    }

    private static boolean isFlowNode(CustomNodeModelImpl node) {
        return node.getInputsById().values().stream()
                .anyMatch(port -> TypeHandles.EXECUTION_FLOW.equals(port.getDataTypeHandle()))
                || node.getOutputsById().values().stream()
                .anyMatch(port -> TypeHandles.EXECUTION_FLOW.equals(port.getDataTypeHandle()));
    }

    private static boolean isNodeInGroup(CustomNodeModelImpl node, String group) {
        return node.getNode() != null
                && QuestBlueprintGraphModel.isNodeInGroup(node.getNode().getClass(), group);
    }

    private static boolean hasConnectedOutput(CustomNodeModelImpl node, String portId) {
        return connectedOutputCount(node, portId) > 0;
    }

    private static int connectedInputCount(CustomNodeModelImpl node, String portId) {
        PortModel port = node.getInputsById().get(portId);
        return port == null ? 0 : port.getConnectedPorts().size();
    }

    private static int connectedOutputCount(CustomNodeModelImpl node, String portId) {
        PortModel port = node.getOutputsById().get(portId);
        return port == null ? 0 : port.getConnectedPorts().size();
    }

    private static <T> List<CustomNodeModelImpl> findNodes(GraphModel graphModel, Class<T> nodeType) {
        List<CustomNodeModelImpl> result = new ArrayList<>();
        for (CustomNodeModelImpl node : customNodes(graphModel)) {
            if (nodeType.isInstance(node.getNode())) {
                result.add(node);
            }
        }
        return result;
    }

    private static List<CustomNodeModelImpl> customNodes(GraphModel graphModel) {
        List<CustomNodeModelImpl> result = new ArrayList<>();
        for (AbstractNodeModel nodeModel : graphModel.getNodeModels()) {
            if (nodeModel instanceof CustomNodeModelImpl custom) {
                result.add(custom);
            }
        }
        return result;
    }

    private static Component translatable(String key, Object... args) {
        return Component.translatable(VALIDATION_KEY + key, args);
    }

    private static Component nodeLabel(CustomNodeModelImpl node) {
        Component title = node.getTitle();
        if (title != null) {
            return title;
        }
        String name = node.getName();
        if (name != null && !name.isBlank()) {
            return Component.literal(name);
        }
        return Component.literal(node.getUid() == null ? "node" : node.getUid().toString());
    }
}
