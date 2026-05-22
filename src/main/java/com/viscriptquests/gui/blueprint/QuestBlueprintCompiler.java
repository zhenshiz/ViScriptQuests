package com.viscriptquests.gui.blueprint;

import com.lowdragmc.lowdraglib2.Platform;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.graph.GraphModel;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.CustomNodeModelImpl;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.PortModel;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.SubgraphNodeModel;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.VariableNodeModelImpl;
import com.viscriptquests.gui.blueprint.compiler.IQuestRewardNodeCompiler;
import com.viscriptquests.gui.blueprint.compiler.IQuestTaskNodeCompiler;
import com.viscriptquests.gui.blueprint.compiler.QuestCompileContext;
import com.viscriptquests.gui.blueprint.compiler.QuestFlowGraphBuilder;
import com.viscriptquests.gui.blueprint.compiler.QuestBlueprintValidationException;
import com.viscriptquests.gui.blueprint.compiler.QuestBlueprintValidator;
import com.viscriptquests.gui.blueprint.model.QuestSubQuestNodeModel;
import com.viscriptquests.gui.blueprint.node.flow.QuestStartNode;
import com.viscriptquests.gui.blueprint.node.flow.SubQuestNode;
import com.viscriptquests.quest.data.*;
import com.viscriptquests.quest.data.reward.IReward;
import com.viscriptquests.quest.data.task.ITask;
import lombok.AllArgsConstructor;
import lombok.Getter;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.StringRepresentable;
import org.jetbrains.annotations.NotNull;

import java.util.*;

// 将蓝图图数据编译为运行时 QuestFile。
// 流程节点保持核心语义，任务/奖励/表达式/透传节点通过 compiler registry 扩展。
public final class QuestBlueprintCompiler {

    public static QuestFile compile(CompoundTag graphTag) {
        QuestBlueprintGraph graph = new QuestBlueprintGraph();
        if (!graphTag.isEmpty()) {
            graph.graphModel.deserializeNBT(Platform.getFrozenRegistry(), graphTag.copy());
        }

        Map<UUID, String> portUuidToVarName = buildVarWireMap(graph.graphModel);
        QuestCompileContext context = new QuestCompileContext(graph.graphModel, portUuidToVarName);

        CustomNodeModelImpl startNodeModel = null;
        Map<String, SubQuestInfo> subQuests = new LinkedHashMap<>();
        Map<String, List<CustomNodeModelImpl>> taskNodes = new LinkedHashMap<>();
        Map<String, List<CustomNodeModelImpl>> rewardNodes = new LinkedHashMap<>();

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
                    collectSubQuestContent(context, custom, stepId, taskNodes, rewardNodes);
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
            addCompiledTasksAndStep(questFile, context, taskModels, stepId, subQuests.get(stepId));
        }
        for (var entry : remainingTasks.entrySet()) {
            String stepId = entry.getKey();
            addCompiledTasksAndStep(questFile, context, entry.getValue(), stepId, subQuests.get(stepId));
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

        Set<String> processedVars = new LinkedHashSet<>();
        collectVariableDefaults(graph.graphModel, questFile, processedVars, new HashSet<>());

        QuestBlueprintValidator.validateExport(questFile);
        return questFile;
    }

    private static void collectSubQuestContent(QuestCompileContext context,
                                               CustomNodeModelImpl subQuestNode,
                                               String stepId,
                                               Map<String, List<CustomNodeModelImpl>> taskNodes,
                                               Map<String, List<CustomNodeModelImpl>> rewardNodes) {
        GraphModel subgraph = null;
        if (subQuestNode instanceof QuestSubQuestNodeModel subQuestModel) {
            subgraph = subQuestModel.getSubgraphModel();
        }
        if (subgraph == null) {
            return;
        }
        collectTaskAndRewardNodesInGraph(context, subgraph, stepId, taskNodes, rewardNodes);
    }

    private static void collectTaskAndRewardNodesInGraph(QuestCompileContext context,
                                                         GraphModel graphModel,
                                                         String stepId,
                                                         Map<String, List<CustomNodeModelImpl>> taskNodes,
                                                         Map<String, List<CustomNodeModelImpl>> rewardNodes) {
        for (var nodeModel : graphModel.getNodeModels()) {
            if (nodeModel instanceof CustomNodeModelImpl custom) {
                if (context.findTaskCompiler(custom) != null) {
                    taskNodes.computeIfAbsent(stepId, ignored -> new ArrayList<>()).add(custom);
                    continue;
                }
                if (context.findRewardCompiler(custom) != null) {
                    rewardNodes.computeIfAbsent(stepId, ignored -> new ArrayList<>()).add(custom);
                }
            }
            if (nodeModel instanceof SubgraphNodeModel subgraphNode) {
                GraphModel nested = subgraphNode.getSubgraphModel();
                if (nested != null) {
                    collectTaskAndRewardNodesInGraph(context, nested, stepId, taskNodes, rewardNodes);
                }
            }
        }
    }

    private static void collectVariableDefaults(GraphModel graphModel, QuestFile questFile,
                                                Set<String> processedVars, Set<UUID> visitedGraphs) {
        if (!visitedGraphs.add(graphModel.getUid())) {
            return;
        }
        for (var nodeModel : graphModel.getNodeModels()) {
            if (nodeModel instanceof VariableNodeModelImpl varNode) {
                var decl = varNode.getVariableDeclarationModel();
                if (decl != null && processedVars.add(decl.getName())) {
                    questFile.variableDefaults.put(decl.getName(),
                            QuestVariableValue.fromConstant(decl.getDataTypeHandle(), decl.getInitializationModel()));
                }
            }
            if (nodeModel instanceof SubgraphNodeModel subgraphNode) {
                GraphModel nested = subgraphNode.getSubgraphModel();
                if (nested != null) {
                    collectVariableDefaults(nested, questFile, processedVars, visitedGraphs);
                }
            }
        }
        List<GraphModel> localSubGraphs = graphModel.getLocalSubGraphs();
        if (localSubGraphs != null) {
            for (GraphModel subgraph : localSubGraphs) {
                if (subgraph != null) {
                    collectVariableDefaults(subgraph, questFile, processedVars, visitedGraphs);
                }
            }
        }
    }

    private static void addCompiledTasksAndStep(QuestFile questFile, QuestCompileContext context,
                                                List<CustomNodeModelImpl> taskModels, String stepId, SubQuestInfo subInfo) {
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

    // 构建变量端口连线反向映射：输入端口 UUID -> 变量名。
    private static Map<UUID, String> buildVarWireMap(GraphModel graphModel) {
        Map<UUID, String> map = new LinkedHashMap<>();
        buildVarWireMap(graphModel, map, new HashSet<>());
        return map;
    }

    private static void buildVarWireMap(GraphModel graphModel, Map<UUID, String> map, Set<UUID> visitedGraphs) {
        if (!visitedGraphs.add(graphModel.getUid())) {
            return;
        }
        for (var nodeModel : graphModel.getNodeModels()) {
            if (nodeModel instanceof VariableNodeModelImpl varNode) {
                var decl = varNode.getVariableDeclarationModel();
                if (decl == null) continue;
                String varName = decl.getName();
                for (var entry : varNode.getOutputsById().entrySet()) {
                    PortModel outputPort = entry.getValue();
                    for (PortModel connectedPort : outputPort.getConnectedPorts()) {
                        map.put(connectedPort.getUid(), varName);
                    }
                }
            }
            if (nodeModel instanceof SubgraphNodeModel subgraphNode) {
                GraphModel nested = subgraphNode.getSubgraphModel();
                if (nested != null) {
                    buildVarWireMap(nested, map, visitedGraphs);
                }
            }
        }
        List<GraphModel> localSubGraphs = graphModel.getLocalSubGraphs();
        if (localSubGraphs != null) {
            for (GraphModel subgraph : localSubGraphs) {
                if (subgraph != null) {
                    buildVarWireMap(subgraph, map, visitedGraphs);
                }
            }
        }
    }

    private record SubQuestInfo(String stepId, String title, String subtitle, String[] description) {
    }

    // 步骤转移条件使用的比较运算符
    @Getter
    @AllArgsConstructor
    public enum CompareOp implements StringRepresentable {
        EQ("viscript_quests.compare_op.eq"),
        NE("viscript_quests.compare_op.ne"),
        GT("viscript_quests.compare_op.gt"),
        GE("viscript_quests.compare_op.ge"),
        LT("viscript_quests.compare_op.lt"),
        LE("viscript_quests.compare_op.le");

        private final String name;

        // 评估比较结果
        public boolean test(float actual, float expected) {
            return switch (this) {
                case EQ -> actual == expected;
                case NE -> actual != expected;
                case GT -> actual > expected;
                case GE -> actual >= expected;
                case LT -> actual < expected;
                case LE -> actual <= expected;
            };
        }

        @Override
        public @NotNull String getSerializedName() {
            return name;
        }
    }
}
