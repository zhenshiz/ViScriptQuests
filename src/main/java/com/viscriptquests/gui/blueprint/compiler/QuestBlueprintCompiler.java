package com.viscriptquests.gui.blueprint.compiler;

import com.lowdragmc.lowdraglib2.Platform;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.constant.Constant;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.graph.GraphModel;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.ConstantNodeModel;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.CustomNodeModelImpl;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.PortModel;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.SubgraphNodeModel;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.VariableNodeModelImpl;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.variable.VariableDeclarationModelBase;
import com.viscriptquests.gui.blueprint.QuestBlueprintFlowTypes;
import com.viscriptquests.gui.blueprint.QuestBlueprintGraph;
import com.viscriptquests.gui.blueprint.model.QuestSubQuestNodeModel;
import com.viscriptquests.gui.blueprint.node.flow.QuestStartNode;
import com.viscriptquests.gui.blueprint.node.flow.SubQuestStartNode;
import com.viscriptquests.gui.blueprint.node.flow.SubQuestNode;
import com.viscriptquests.gui.blueprint.node.reward.RewardPlaceholderNode;
import com.viscriptquests.quest.data.*;
import com.viscriptquests.quest.data.reward.IReward;
import com.viscriptquests.quest.data.runtime.RewardDisplay;
import com.viscriptquests.quest.data.task.ITask;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;

import java.util.*;

// 将蓝图图数据编译为运行时 QuestFile。
// 流程节点保持核心语义，任务/奖励/表达式/透传节点通过 compiler registry 扩展。
public final class QuestBlueprintCompiler {

    public static QuestFile compile(CompoundTag graphTag) {
        QuestBlueprintGraph graph = new QuestBlueprintGraph();
        if (!graphTag.isEmpty()) {
            graph.graphModel.deserializeNBT(Platform.getFrozenRegistry(), graphTag.copy());
        }

        QuestVariableBindingData variableBindings = buildVariableBindings(graph.graphModel);
        QuestCompileContext context = new QuestCompileContext(graph.graphModel, variableBindings);

        CustomNodeModelImpl startNodeModel = null;
        Map<String, SubQuestInfo> subQuests = new LinkedHashMap<>();
        Map<String, List<CustomNodeModelImpl>> taskNodes = new LinkedHashMap<>();
        Map<String, List<CustomNodeModelImpl>> rewardNodes = new LinkedHashMap<>();
        Map<String, List<RewardDisplay>> rewardPlaceholders = new LinkedHashMap<>();
        Map<String, List<ObjectiveAction>> objectiveActions = new LinkedHashMap<>();
        Map<String, Set<String>> initialObjectiveIds = new LinkedHashMap<>();

        for (var nodeModel : graph.graphModel.getNodeModels()) {
            if (!(nodeModel instanceof CustomNodeModelImpl custom)) continue;
            var node = custom.getNode();
            if (node == null) continue;

            if (node instanceof QuestStartNode) {
                if (startNodeModel != null) {
                    throw QuestBlueprintValidationException.create(
                            "viscript_quests.editor.quest.export.validation.duplicate_start");
                }
                startNodeModel = custom;
                continue;
            }
            if (node instanceof SubQuestNode) {
                String stepId = context.resolveStepId(custom);
                if (!stepId.isEmpty()) {
                    subQuests.put(stepId, new SubQuestInfo(
                            stepId,
                            context.getString(custom, "title"),
                            context.getString(custom, "subtitle"),
                            context.getStringArray(custom, "description")
                    ));
                    collectSubQuestContent(context, custom, stepId, taskNodes, rewardNodes, rewardPlaceholders,
                            objectiveActions, initialObjectiveIds);
                }
                continue;
            }

            IQuestTaskNodeCompiler taskCompiler = context.findTaskCompiler(custom);
            if (taskCompiler != null) {
                String stepId = context.resolveStepId(custom);
                if (!stepId.isEmpty()) {
                    taskNodes.computeIfAbsent(stepId, ignored -> new ArrayList<>()).add(custom);
                }
                continue;
            }

            IQuestRewardNodeCompiler rewardCompiler = context.findRewardCompiler(custom);
            if (rewardCompiler != null) {
                String stepId = context.resolveStepId(custom);
                if (!stepId.isEmpty()) {
                    rewardNodes.computeIfAbsent(stepId, ignored -> new ArrayList<>()).add(custom);
                }
            }
        }

        if (startNodeModel == null) {
            throw QuestBlueprintValidationException.create(
                    "viscript_quests.editor.quest.export.validation.missing_start");
        }

        QuestFile questFile = new QuestFile();
        questFile.quest.title = context.getString(startNodeModel, "title");
        questFile.quest.subtitle = context.getString(startNodeModel, "subtitle");
        questFile.quest.icon = context.getDisplayIcon(startNodeModel, "icon");

        QuestFlowGraphBuilder.Result result = QuestFlowGraphBuilder.build(startNodeModel, context);
        Map<String, List<CustomNodeModelImpl>> remainingTasks = new LinkedHashMap<>(taskNodes);
        for (String stepId : result.orderedStepIds()) {
            List<CustomNodeModelImpl> taskModels = remainingTasks.remove(stepId);
            if (taskModels == null || taskModels.isEmpty()) continue;
            addCompiledTasksAndStep(questFile, context, taskModels, stepId, subQuests.get(stepId),
                    objectiveActions.get(stepId), initialObjectiveIds.get(stepId));
        }
        for (var entry : remainingTasks.entrySet()) {
            String stepId = entry.getKey();
            addCompiledTasksAndStep(questFile, context, entry.getValue(), stepId, subQuests.get(stepId),
                    objectiveActions.get(stepId), initialObjectiveIds.get(stepId));
        }

        questFile.flowNodes.addAll(result.flowNodes());
        questFile.flowEdges.addAll(result.flowEdges());

        for (var entry : rewardNodes.entrySet()) {
            String stepId = entry.getKey();
            for (CustomNodeModelImpl rewardModel : entry.getValue()) {
                IQuestRewardNodeCompiler rewardCompiler = context.findRewardCompiler(rewardModel);
                if (rewardCompiler == null) continue;
                IReward reward = rewardCompiler.compileReward(context, rewardModel, stepId);
                if (reward != null) {
                    questFile.rewards.add(reward);
                }
            }
        }
        for (var displays : rewardPlaceholders.values()) {
            questFile.rewardPlaceholders.addAll(displays);
        }
        for (var actions : objectiveActions.values()) {
            questFile.objectiveActions.addAll(actions);
        }

        Set<String> processedVars = new LinkedHashSet<>();
        collectVariableDefaults(graph.graphModel, questFile, processedVars, new HashSet<>(), variableBindings);

        QuestBlueprintValidator.validateExport(questFile);
        return questFile;
    }

    private static void collectSubQuestContent(QuestCompileContext context,
                                               CustomNodeModelImpl subQuestNode,
                                               String stepId,
                                               Map<String, List<CustomNodeModelImpl>> taskNodes,
                                               Map<String, List<CustomNodeModelImpl>> rewardNodes,
                                               Map<String, List<RewardDisplay>> rewardPlaceholders,
                                               Map<String, List<ObjectiveAction>> objectiveActions,
                                               Map<String, Set<String>> initialObjectiveIds) {
        GraphModel subgraph = null;
        if (subQuestNode instanceof QuestSubQuestNodeModel subQuestModel) {
            subgraph = subQuestModel.getSubgraphModel();
        }
        if (subgraph == null) {
            return;
        }
        CustomNodeModelImpl start = findSubQuestStart(subgraph);
        if (start != null) {
            collectSubQuestFlowContent(context, start, stepId, taskNodes, rewardNodes, rewardPlaceholders,
                    objectiveActions, initialObjectiveIds);
            return;
        }
        collectTaskAndRewardNodesInGraph(context, subgraph, stepId, taskNodes, rewardNodes, rewardPlaceholders,
                objectiveActions);
    }

    private static void collectTaskAndRewardNodesInGraph(QuestCompileContext context,
                                                         GraphModel graphModel,
                                                         String stepId,
                                                         Map<String, List<CustomNodeModelImpl>> taskNodes,
                                                         Map<String, List<CustomNodeModelImpl>> rewardNodes,
                                                         Map<String, List<RewardDisplay>> rewardPlaceholders,
                                                         Map<String, List<ObjectiveAction>> objectiveActions) {
        for (var nodeModel : graphModel.getNodeModels()) {
            if (nodeModel instanceof CustomNodeModelImpl custom) {
                if (context.findTaskCompiler(custom) != null) {
                    taskNodes.computeIfAbsent(stepId, ignored -> new ArrayList<>()).add(custom);
                    String objectiveId = objectiveIdOf(custom);
                    objectiveActions.computeIfAbsent(stepId, ignored -> new ArrayList<>())
                            .addAll(QuestObjectiveActionCompiler.compile(context, custom, stepId, objectiveId));
                    continue;
                }
                if (context.findRewardCompiler(custom) != null) {
                    rewardNodes.computeIfAbsent(stepId, ignored -> new ArrayList<>()).add(custom);
                    continue;
                }
                RewardDisplay placeholder = compileRewardPlaceholder(context, custom, stepId);
                if (placeholder != null) {
                    rewardPlaceholders.computeIfAbsent(stepId, ignored -> new ArrayList<>()).add(placeholder);
                }
            }
            if (nodeModel instanceof SubgraphNodeModel subgraphNode) {
                GraphModel nested = subgraphNode.getSubgraphModel();
                if (nested != null) {
                    collectTaskAndRewardNodesInGraph(context, nested, stepId, taskNodes, rewardNodes,
                            rewardPlaceholders, objectiveActions);
                }
            }
        }
    }

    private static CustomNodeModelImpl findSubQuestStart(GraphModel subgraph) {
        CustomNodeModelImpl start = null;
        for (var nodeModel : subgraph.getNodeModels()) {
            if (!(nodeModel instanceof CustomNodeModelImpl custom)
                    || !(custom.getNode() instanceof SubQuestStartNode)) {
                continue;
            }
            if (start != null) {
                throw QuestBlueprintValidationException.create(
                        "viscript_quests.editor.quest.export.validation.duplicate_subquest_start");
            }
            start = custom;
        }
        return start;
    }

    private static void collectSubQuestFlowContent(QuestCompileContext context,
                                                   CustomNodeModelImpl start,
                                                   String stepId,
                                                   Map<String, List<CustomNodeModelImpl>> taskNodes,
                                                   Map<String, List<CustomNodeModelImpl>> rewardNodes,
                                                   Map<String, List<RewardDisplay>> rewardPlaceholders,
                                                   Map<String, List<ObjectiveAction>> objectiveActions,
                                                   Map<String, Set<String>> initialObjectiveIds) {
        Set<String> initialIds = initialObjectiveIds.computeIfAbsent(stepId, ignored -> new LinkedHashSet<>());
        for (CustomNodeModelImpl taskNode : collectFirstReachableTaskNodes(context, start, "objectives")) {
            initialIds.add(objectiveIdOf(taskNode));
        }
        for (CustomNodeModelImpl taskNode : collectReachableCustomNodes(start, "objectives")) {
            if (context.findTaskCompiler(taskNode) == null) {
                continue;
            }
            taskNodes.computeIfAbsent(stepId, ignored -> new ArrayList<>()).add(taskNode);
            String objectiveId = objectiveIdOf(taskNode);
            objectiveActions.computeIfAbsent(stepId, ignored -> new ArrayList<>())
                    .addAll(QuestObjectiveActionCompiler.compile(context, taskNode, stepId, objectiveId));
        }
        for (CustomNodeModelImpl rewardNode : collectReachableCustomNodes(start, "rewards")) {
            if (context.findRewardCompiler(rewardNode) != null) {
                rewardNodes.computeIfAbsent(stepId, ignored -> new ArrayList<>()).add(rewardNode);
                continue;
            }
            RewardDisplay placeholder = compileRewardPlaceholder(context, rewardNode, stepId);
            if (placeholder != null) {
                rewardPlaceholders.computeIfAbsent(stepId, ignored -> new ArrayList<>()).add(placeholder);
            }
        }
    }

    private static List<CustomNodeModelImpl> collectFirstReachableTaskNodes(QuestCompileContext context,
                                                                            CustomNodeModelImpl start,
                                                                            String portId) {
        List<CustomNodeModelImpl> result = new ArrayList<>();
        ArrayDeque<CustomNodeModelImpl> queue = new ArrayDeque<>();
        enqueueOutput(start, portId, queue);
        Set<UUID> visited = new LinkedHashSet<>();
        while (!queue.isEmpty()) {
            CustomNodeModelImpl node = queue.removeFirst();
            if (node == null || !visited.add(node.getUid())) {
                continue;
            }
            if (context.findTaskCompiler(node) != null) {
                result.add(node);
                continue;
            }
            enqueueOutput(node, "next", queue);
            if (node.getNode() instanceof com.viscriptquests.gui.blueprint.node.flow.QuestBranchNode) {
                enqueueOutput(node, "true", queue);
                enqueueOutput(node, "false", queue);
            }
        }
        return result;
    }

    private static RewardDisplay compileRewardPlaceholder(QuestCompileContext context,
                                                          CustomNodeModelImpl node,
                                                          String stepId) {
        if (!(node.getNode() instanceof RewardPlaceholderNode)) {
            return null;
        }
        RewardDisplay display = new RewardDisplay();
        display.stepId = stepId == null ? "" : stepId;
        String tooltip = context.getString(node, "reward_tooltip").trim();
        display.displayText = tooltip.isEmpty() ? Component.empty() : Component.literal(tooltip);
        DisplayIcon icon = context.getDisplayIcon(node, "reward_icon");
        display.icon = hasDisplayIcon(icon) ? icon : RewardPlaceholderNode.defaultIcon();
        return display;
    }

    private static boolean hasDisplayIcon(DisplayIcon icon) {
        if (icon == null) {
            return false;
        }
        if (icon.isTexture()) {
            return icon.getTexture() != null && !icon.getTexture().isBlank();
        }
        return icon.getItemStack() != null && !icon.getItemStack().isEmpty();
    }

    private static List<CustomNodeModelImpl> collectReachableCustomNodes(CustomNodeModelImpl start, String portId) {
        List<CustomNodeModelImpl> result = new ArrayList<>();
        ArrayDeque<CustomNodeModelImpl> queue = new ArrayDeque<>();
        enqueueOutput(start, portId, queue);
        Set<UUID> visited = new LinkedHashSet<>();
        while (!queue.isEmpty()) {
            CustomNodeModelImpl node = queue.removeFirst();
            if (node == null || !visited.add(node.getUid())) {
                continue;
            }
            result.add(node);
            enqueueOutput(node, "next", queue);
            if (node.getNode() instanceof com.viscriptquests.gui.blueprint.node.flow.QuestBranchNode) {
                enqueueOutput(node, "true", queue);
                enqueueOutput(node, "false", queue);
            }
        }
        return result;
    }

    private static void enqueueOutput(CustomNodeModelImpl node, String portId, ArrayDeque<CustomNodeModelImpl> queue) {
        PortModel port = node.getOutputsById().get(portId);
        if (port == null) {
            return;
        }
        for (PortModel connectedPort : port.getConnectedPorts()) {
            if (connectedPort.getNodeModel() instanceof CustomNodeModelImpl custom) {
                queue.add(custom);
            }
        }
    }

    private static void collectVariableDefaults(GraphModel graphModel, QuestFile questFile,
                                                Set<String> processedVars, Set<UUID> visitedGraphs,
                                                QuestVariableBindingData variableBindings) {
        if (!visitedGraphs.add(graphModel.getUid())) {
            return;
        }
        for (var nodeModel : graphModel.getNodeModels()) {
            if (nodeModel instanceof VariableNodeModelImpl varNode) {
                var decl = varNode.getVariableDeclarationModel();
                if (decl == null || isInheritedVariable(decl, variableBindings)) {
                    continue;
                }
                String runtimeName = variableNameFor(decl, variableBindings.variableAliases());
                if (processedVars.add(runtimeName)) {
                    QuestVariableValue override = defaultOverride(decl, variableBindings);
                    questFile.variableDefaults.put(runtimeName, override == null
                            ? QuestVariableValue.fromConstant(decl.getDataTypeHandle(), decl.getInitializationModel())
                            : override.copy());
                }
            }
            if (nodeModel instanceof QuestSubQuestNodeModel subQuestNode) {
                GraphModel nested = subQuestNode.getSubgraphModel();
                if (nested != null) {
                    collectVariableDefaults(nested, questFile, processedVars, visitedGraphs, variableBindings);
                }
            }
            if (nodeModel instanceof SubgraphNodeModel subgraphNode) {
                GraphModel nested = subgraphNode.getSubgraphModel();
                if (nested != null) {
                    collectVariableDefaults(nested, questFile, processedVars, visitedGraphs, variableBindings);
                }
            }
        }
        List<GraphModel> localSubGraphs = graphModel.getLocalSubGraphs();
        if (localSubGraphs != null) {
            for (GraphModel subgraph : localSubGraphs) {
                if (subgraph != null) {
                    collectVariableDefaults(subgraph, questFile, processedVars, visitedGraphs, variableBindings);
                }
            }
        }
    }

    private static void addCompiledTasksAndStep(QuestFile questFile, QuestCompileContext context,
                                                List<CustomNodeModelImpl> taskModels, String stepId, SubQuestInfo subInfo,
                                                List<ObjectiveAction> actions, Set<String> initialObjectiveIds) {
        Set<String> activatedByPredecessor = new LinkedHashSet<>();
        if (actions != null) {
            for (ObjectiveAction action : actions) {
                if (action != null) {
                    activatedByPredecessor.addAll(action.activateObjectiveIds);
                }
            }
        }
        boolean addedTask = false;
        for (CustomNodeModelImpl taskModel : taskModels) {
            IQuestTaskNodeCompiler taskCompiler = context.findTaskCompiler(taskModel);
            if (taskCompiler == null) {
                continue;
            }
            ITask task = taskCompiler.compileTask(context, taskModel, stepId);
            if (task == null) {
                continue;
            }
            task.objectiveId = objectiveIdOf(taskModel);
            task.initiallyActive = initialObjectiveIds == null || initialObjectiveIds.isEmpty()
                    ? !activatedByPredecessor.contains(task.objectiveId)
                    : initialObjectiveIds.contains(task.objectiveId);
            IQuestTaskNodeCompiler.applyCommonOptions(context, taskModel, task);
            questFile.tasks.add(task);
            addedTask = true;
        }
        if (addedTask && subInfo != null) {
            addCompiledStep(questFile, stepId, subInfo);
        }
    }

    private static void addCompiledStep(QuestFile questFile, String stepId, SubQuestInfo subInfo) {
        QuestStep step = new QuestStep();
        step.stepId = stepId;
        step.title = subInfo.title;
        step.subtitle = subInfo.subtitle;
        step.description = subInfo.description.clone();
        questFile.steps.add(step);
    }

    private static String objectiveIdOf(CustomNodeModelImpl taskModel) {
        return taskModel == null || taskModel.getUid() == null ? "" : taskModel.getUid().toString();
    }

    private static QuestVariableBindingData buildVariableBindings(GraphModel graphModel) {
        Map<QuestGraphElementKey, String> portMap = new LinkedHashMap<>();
        Map<QuestGraphElementKey, String> variableAliases = new LinkedHashMap<>();
        Map<QuestGraphElementKey, QuestVariableValue> defaultOverrides = new LinkedHashMap<>();
        Set<QuestGraphElementKey> inheritedVariables = new LinkedHashSet<>();
        buildVariableBindings(graphModel, portMap, variableAliases, defaultOverrides, inheritedVariables, new HashSet<>());
        return new QuestVariableBindingData(portMap, variableAliases, defaultOverrides, inheritedVariables);
    }

    private static void buildVariableBindings(GraphModel graphModel,
                                              Map<QuestGraphElementKey, String> portMap,
                                              Map<QuestGraphElementKey, String> variableAliases,
                                              Map<QuestGraphElementKey, QuestVariableValue> defaultOverrides,
                                              Set<QuestGraphElementKey> inheritedVariables,
                                              Set<UUID> visitedGraphs) {
        if (!visitedGraphs.add(graphModel.getUid())) {
            return;
        }
        for (var nodeModel : graphModel.getNodeModels()) {
            if (nodeModel instanceof VariableNodeModelImpl varNode) {
                var decl = varNode.getVariableDeclarationModel();
                if (decl == null) continue;
                String varName = variableNameFor(decl, variableAliases);
                for (var entry : varNode.getOutputsById().entrySet()) {
                    PortModel outputPort = entry.getValue();
                    for (PortModel connectedPort : outputPort.getConnectedPorts()) {
                        portMap.put(QuestGraphElementKey.of(connectedPort), varName);
                    }
                }
            }
            if (nodeModel instanceof QuestSubQuestNodeModel subQuestNode) {
                GraphModel nested = subQuestNode.getSubgraphModel();
                if (nested != null) {
                    collectSubQuestVariableBindings(subQuestNode, nested, variableAliases, defaultOverrides,
                            inheritedVariables);
                    buildVariableBindings(nested, portMap, variableAliases, defaultOverrides, inheritedVariables,
                            visitedGraphs);
                }
            }
            if (nodeModel instanceof SubgraphNodeModel subgraphNode) {
                GraphModel nested = subgraphNode.getSubgraphModel();
                if (nested != null) {
                    buildVariableBindings(nested, portMap, variableAliases, defaultOverrides, inheritedVariables,
                            visitedGraphs);
                }
            }
        }
        List<GraphModel> localSubGraphs = graphModel.getLocalSubGraphs();
        if (localSubGraphs != null) {
            for (GraphModel subgraph : localSubGraphs) {
                if (subgraph != null) {
                    buildVariableBindings(subgraph, portMap, variableAliases, defaultOverrides, inheritedVariables,
                            visitedGraphs);
                }
            }
        }
    }

    private static void collectSubQuestVariableBindings(QuestSubQuestNodeModel subQuestNode,
                                                        GraphModel subgraph,
                                                        Map<QuestGraphElementKey, String> variableAliases,
                                                        Map<QuestGraphElementKey, QuestVariableValue> defaultOverrides,
                                                        Set<QuestGraphElementKey> inheritedVariables) {
        for (VariableDeclarationModelBase variable : subgraph.getGraphVariableModels()) {
            if (variable == null || variable.getUid() == null) {
                continue;
            }
            QuestGraphElementKey variableKey = QuestGraphElementKey.of(variable);
            variableAliases.putIfAbsent(variableKey, scopedSubQuestVariableName(subQuestNode, variable));
            if (!QuestSubQuestNodeModel.hasExposedInput(variable)) {
                continue;
            }
            PortModel inputPort = subQuestNode.getInputsById().get(QuestSubQuestNodeModel.exposedInputPortId(variable));
            if (inputPort == null) {
                continue;
            }
            String alias = findVariableAlias(inputPort, variableAliases);
            if (alias != null && !alias.isBlank()) {
                variableAliases.put(variableKey, alias);
                defaultOverrides.remove(variableKey);
                inheritedVariables.add(variableKey);
                continue;
            }
            QuestVariableValue override = findConstantOverride(subQuestNode, inputPort, variable);
            if (override != null) {
                defaultOverrides.put(variableKey, override);
            }
        }
    }

    private static String findVariableAlias(PortModel inputPort, Map<QuestGraphElementKey, String> variableAliases) {
        for (PortModel connectedPort : inputPort.getConnectedPorts()) {
            if (connectedPort.getNodeModel() instanceof VariableNodeModelImpl varNode) {
                var declaration = varNode.getVariableDeclarationModel();
                if (declaration != null) {
                    return variableNameFor(declaration, variableAliases);
                }
            }
        }
        return null;
    }

    private static QuestVariableValue findConstantOverride(QuestSubQuestNodeModel subQuestNode, PortModel inputPort,
                                                           VariableDeclarationModelBase variable) {
        for (PortModel connectedPort : inputPort.getConnectedPorts()) {
            if (connectedPort.getNodeModel() instanceof ConstantNodeModel constantNode) {
                Constant constant = constantNode.getConstant();
                if (constant != null) {
                    return QuestVariableValue.fromConstant(variable.getDataTypeHandle(), constant);
                }
            }
        }
        if (inputPort.getConnectedPorts().isEmpty()) {
            Constant embedded = subQuestNode.getInputConstantsById().get(inputPort.getUniqueName());
            if (embedded != null) {
                return QuestVariableValue.fromConstant(variable.getDataTypeHandle(), embedded);
            }
        }
        return null;
    }

    private static String variableNameFor(VariableDeclarationModelBase declaration,
                                          Map<QuestGraphElementKey, String> variableAliases) {
        if (declaration.getUid() != null) {
            String alias = variableAliases.get(QuestGraphElementKey.of(declaration));
            if (alias != null && !alias.isBlank()) {
                return alias;
            }
        }
        return declaration.getName();
    }

    private static String scopedSubQuestVariableName(QuestSubQuestNodeModel subQuestNode,
                                                     VariableDeclarationModelBase variable) {
        return subQuestRuntimeId(subQuestNode) + "/" + variable.getName();
    }

    private static String subQuestRuntimeId(QuestSubQuestNodeModel subQuestNode) {
        return subQuestNode.getUid() == null ? "sub_quest" : subQuestNode.getUid().toString();
    }

    private static boolean isInheritedVariable(VariableDeclarationModelBase declaration,
                                               QuestVariableBindingData variableBindings) {
        return variableBindings.isInherited(declaration);
    }

    private static QuestVariableValue defaultOverride(VariableDeclarationModelBase declaration,
                                                      QuestVariableBindingData variableBindings) {
        return variableBindings.defaultOverrideFor(declaration);
    }

    private record SubQuestInfo(String stepId, String title, String subtitle, String[] description) {
    }
}
