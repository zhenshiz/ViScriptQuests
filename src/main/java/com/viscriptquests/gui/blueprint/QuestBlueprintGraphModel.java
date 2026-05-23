package com.viscriptquests.gui.blueprint;

import com.lowdragmc.lowdraglib2.nodegraphtookit.model.GraphElementModel;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.node.Node;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.node.NodeAttribute;
import com.lowdragmc.lowdraglib2.nodegraphtookit.gui.itemlibrary.GraphNodeCreationData;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.type.TypeHandle;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.type.TypeHandles;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.graph.CustomGraphModelImpl;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.graph.GraphModel;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.AbstractNodeModel;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.CustomNodeModelImpl;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.NodeModel;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.PortModel;
import com.viscriptquests.gui.blueprint.model.QuestSubQuestNodeModel;
import com.viscriptquests.gui.blueprint.model.QuestMathOperationNodeModel;
import com.viscriptquests.gui.blueprint.node.QuestBlueprintNode;
import com.viscriptquests.gui.blueprint.node.flow.QuestBranchNode;
import com.viscriptquests.gui.blueprint.node.flow.SubQuestStartNode;
import com.viscriptquests.gui.blueprint.node.flow.SubQuestNode;
import com.viscriptquests.gui.blueprint.node.math.MathOperationNode;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.Tag;
import org.joml.Vector2f;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

// 自定义图模型，支持数值类型之间的隐式连线转换（INT ↔ FLOAT）
public class QuestBlueprintGraphModel extends CustomGraphModelImpl {
    private boolean subQuestContentGraph;

    public QuestBlueprintGraphModel(QuestBlueprintGraph graph) {
        super(graph);
    }

    public static AbstractNodeModel createNodeFromData(GraphNodeCreationData data, Class<? extends Node> nodeClass) {
        if (nodeClass == SubQuestNode.class) {
            return data.graphModel().createNode(
                    QuestSubQuestNodeModel.class,
                    "",
                    data.position(),
                    data.uuid(),
                    node -> {
                        if (node instanceof QuestSubQuestNodeModel customNode) {
                            customNode.initCustomNode(new SubQuestNode());
                        }
                    },
                    data.spawnFlags()
            );
        }
        if (nodeClass == MathOperationNode.class) {
            return data.graphModel().createNode(
                    QuestMathOperationNodeModel.class,
                    "",
                    data.position(),
                    data.uuid(),
                    node -> {
                        if (node instanceof QuestMathOperationNodeModel customNode) {
                            customNode.initCustomNode(new MathOperationNode());
                        }
                    },
                    data.spawnFlags()
            );
        }
        return CustomGraphModelImpl.createNodeFromData(data, nodeClass);
    }

    @Override
    public List<Class<? extends Node>> getSupportNodes() {
        return getGraph().getSupportNodes().stream()
                .filter(this::isNodeAvailableInCurrentGraph)
                .toList();
    }

    @Override
    public CustomGraphModelImpl createLocalSubgraphInstance() {
        CustomGraphModelImpl subgraph = super.createLocalSubgraphInstance();
        if (subgraph instanceof QuestBlueprintGraphModel questSubgraph) {
            // LDLib2 在反序列化本地子图后才设置 parentGraph，因此这里提前标记子图用途。
            questSubgraph.subQuestContentGraph = true;
        }
        return subgraph;
    }

    @Override
    public CopyPasteData copyElements(List<? extends GraphElementModel> elements, HolderLookup.Provider provider) {
        CopyPasteData data = super.copyElements(elements, provider);
        var selectedNodes = elements.stream()
                .filter(element -> element instanceof AbstractNodeModel)
                .map(element -> (AbstractNodeModel) element)
                .toList();
        if (selectedNodes.isEmpty() || !data.tag().contains("nodes")) {
            return data;
        }

        var nodeTags = data.tag().getList("nodes", Tag.TAG_COMPOUND);
        int count = Math.min(selectedNodes.size(), nodeTags.size());
        for (int i = 0; i < count; i++) {
            if (!(selectedNodes.get(i) instanceof QuestSubQuestNodeModel subQuestNode)) {
                continue;
            }
            var subgraph = subQuestNode.getSubgraphModel();
            if (subgraph == null) {
                continue;
            }
            QuestSubQuestNodeModel.putCopiedSubgraph(nodeTags.getCompound(i), subgraph.serializeNBT(provider));
        }
        return data;
    }

    @Override
    public NodeModel createNodeModel(Node node, Vector2f position) {
        if (node instanceof SubQuestNode) {
            return createNodeWithType(QuestSubQuestNodeModel.class, "", position, null,
                    model -> model.initCustomNode(node), null);
        }
        if (node instanceof MathOperationNode) {
            return createNodeWithType(QuestMathOperationNodeModel.class, "", position, null,
                    model -> model.initCustomNode(node), null);
        }
        return super.createNodeModel(node, position);
    }

    @Override
    protected String getNodeDiscriminator(AbstractNodeModel node) {
        if (node instanceof QuestSubQuestNodeModel
                || node instanceof CustomNodeModelImpl custom && custom.getNode() instanceof SubQuestNode) {
            return "quest_sub_quest";
        }
        if (node instanceof QuestMathOperationNodeModel
                || node instanceof CustomNodeModelImpl custom && custom.getNode() instanceof MathOperationNode) {
            return "quest_math_operation";
        }
        return super.getNodeDiscriminator(node);
    }

    @Override
    protected AbstractNodeModel createNodeFromDiscriminator(String type) {
        if ("quest_sub_quest".equals(type)) {
            return new QuestSubQuestNodeModel();
        }
        if ("quest_math_operation".equals(type)) {
            return new QuestMathOperationNodeModel();
        }
        return super.createNodeFromDiscriminator(type);
    }

    @Override
    public void deserializeAdditionalNBT(Tag tag, HolderLookup.Provider provider) {
        super.deserializeAdditionalNBT(tag, provider);
        splitSharedSubQuestSubgraphs(provider);
    }

    @Override
    public boolean canAssignTo(PortModel fromPort, PortModel toPort) {
        TypeHandle fromType = fromPort.getDataTypeHandle();
        TypeHandle toType = toPort.getDataTypeHandle();
        // 允许数值类型之间的隐式连接（INT ↔ FLOAT）
        if (isNumericType(fromType) && isNumericType(toType)) {
            return true;
        }
        // 允许任意类型连入 Object 端口，使 DebugPrintVariableNode 等通用节点能接收所有变量类型
        if (toType.equals(QuestBlueprintTypes.OBJECT)) {
            return true;
        }
        return super.canAssignTo(fromPort, toPort);
    }

    private static boolean isNumericType(TypeHandle type) {
        return type.equals(TypeHandles.INT) || type.equals(TypeHandles.FLOAT);
    }

    private boolean isNodeAvailableInCurrentGraph(Class<? extends Node> nodeClass) {
        if (isSubQuestContentGraph()) {
            return isSubQuestContentNode(nodeClass);
        }
        return nodeClass != SubQuestStartNode.class
                && !isNodeInGroup(nodeClass, QuestBlueprintNode.TASK_GROUP)
                && !isNodeInGroup(nodeClass, QuestBlueprintNode.REWARD_GROUP);
    }

    private boolean isSubQuestContentGraph() {
        return subQuestContentGraph || getParentGraph() != null;
    }

    public static boolean isSubQuestContentNode(Class<? extends Node> nodeClass) {
        if (nodeClass == SubQuestStartNode.class || nodeClass == QuestBranchNode.class) {
            return true;
        }
        return isNodeInGroup(nodeClass, QuestBlueprintNode.TASK_GROUP)
                || isNodeInGroup(nodeClass, QuestBlueprintNode.LOGIC_GROUP)
                || isNodeInGroup(nodeClass, QuestBlueprintNode.MATH_GROUP)
                || isNodeInGroup(nodeClass, QuestBlueprintNode.DEBUG_GROUP)
                || isNodeInGroup(nodeClass, QuestBlueprintNode.SCOREBOARD_GROUP)
                || isNodeInGroup(nodeClass, QuestBlueprintNode.VARIABLE_GROUP)
                || isNodeInGroup(nodeClass, QuestBlueprintNode.REWARD_GROUP);
    }

    public static boolean isNodeInGroup(Class<? extends Node> nodeClass, String group) {
        NodeAttribute attribute = nodeClass.getAnnotation(NodeAttribute.class);
        return attribute != null && group.equals(attribute.group());
    }

    private void splitSharedSubQuestSubgraphs(HolderLookup.Provider provider) {
        List<GraphModel> localSubGraphs = getLocalSubGraphs();
        if (localSubGraphs == null || localSubGraphs.isEmpty()) {
            return;
        }

        Map<UUID, QuestSubQuestNodeModel> firstOwnerByGraph = new HashMap<>();
        for (AbstractNodeModel nodeModel : getNodeModels()) {
            if (!(nodeModel instanceof QuestSubQuestNodeModel subQuestNode)) {
                continue;
            }
            GraphModel subgraph = subQuestNode.getSubgraphModel();
            if (subgraph == null || firstOwnerByGraph.putIfAbsent(subgraph.getUid(), subQuestNode) == null) {
                continue;
            }
            GraphModel copy = createLocalSubgraphInstance();
            if (copy == null) {
                continue;
            }
            copy.deserializeNBT(provider, subgraph.serializeNBT(provider));
            copy.setUid(UUID.randomUUID());
            addLocalSubgraph(copy);
            subQuestNode.setLocalSubgraph(copy);
        }
    }
}
