package com.viscriptquests.gui.blueprint;

import com.lowdragmc.lowdraglib2.Platform;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.node.Node;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.constant.Constant;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.graph.CustomGraphModelImpl;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.graph.GraphModel;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.CustomNodeModelImpl;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.NodeModel;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.NodeOption;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.PortModel;
import com.viscriptquests.gui.blueprint.model.QuestSubQuestNodeModel;
import com.viscriptquests.gui.blueprint.node.flow.QuestEndNode;
import com.viscriptquests.gui.blueprint.node.flow.QuestStartNode;
import com.viscriptquests.gui.blueprint.node.flow.SubQuestNode;
import com.viscriptquests.gui.blueprint.node.flow.SubQuestStartNode;
import com.viscriptquests.gui.blueprint.node.reward.ItemRewardNode;
import com.viscriptquests.gui.blueprint.node.task.ItemTaskNode;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.joml.Vector2f;

// 新建项目的默认示例图，保持为一个可导出的最小任务流程。
public final class QuestBlueprintExamples {
    private QuestBlueprintExamples() {
    }

    public static CompoundTag createSimpleQuestGraphTag() {
        QuestBlueprintGraph graph = new QuestBlueprintGraph();

        CustomNodeModelImpl start = createNode(graph.graphModel, new QuestStartNode(), new Vector2f(0, 0));
        setOption(start, "title", "示例任务");
        setOption(start, "subtitle", "收集 4 个泥土并领取奖励");

        CustomNodeModelImpl subQuest = createNode(graph.graphModel, new SubQuestNode(), new Vector2f(320, 0));
        setOption(subQuest, "title", "收集泥土");
        setOption(subQuest, "subtitle", "带回 4 个泥土");
        setOption(subQuest, "description", new String[]{
                "这是新建任务项目自动生成的示例流程。",
                "双击小任务节点可以进入目标和奖励子图。"
        });

        CustomNodeModelImpl end = createNode(graph.graphModel, new QuestEndNode(), new Vector2f(640, 0));
        connect(graph.graphModel, start, "next", subQuest, "in");
        connect(graph.graphModel, subQuest, "success", end, "in");

        if (subQuest instanceof QuestSubQuestNodeModel subQuestModel) {
            fillSubQuestExample(subQuestModel);
        }

        return graph.graphModel.serializeNBT(Platform.getFrozenRegistry());
    }

    private static void fillSubQuestExample(QuestSubQuestNodeModel subQuestModel) {
        GraphModel subgraph = subQuestModel.ensureLocalSubgraph();
        if (!(subgraph instanceof CustomGraphModelImpl customGraph)) {
            return;
        }

        CustomNodeModelImpl subStart = createNode(customGraph, new SubQuestStartNode(), new Vector2f(0, 0));

        CustomNodeModelImpl itemTask = createNode(customGraph, new ItemTaskNode(), new Vector2f(320, -90));
        setOption(itemTask, "item_stack", new ItemStack(Items.DIRT, 4));

        CustomNodeModelImpl itemReward = createNode(customGraph, new ItemRewardNode(), new Vector2f(320, 120));
        setOption(itemReward, "item_stack", new ItemStack(Items.DIAMOND, 1));

        connect(customGraph, subStart, "objectives", itemTask, "in");
        connect(customGraph, subStart, "rewards", itemReward, "in");
    }

    private static CustomNodeModelImpl createNode(CustomGraphModelImpl graphModel, Node node, Vector2f position) {
        if (graphModel.createNodeModel(node, position) instanceof CustomNodeModelImpl customNode) {
            return customNode;
        }
        throw new IllegalStateException("Quest blueprint example node must be a custom node: " + node.getClass().getName());
    }

    private static void connect(GraphModel graphModel,
                                CustomNodeModelImpl fromNode,
                                String outputPort,
                                CustomNodeModelImpl toNode,
                                String inputPort) {
        PortModel from = fromNode.getOutputsById().get(outputPort);
        PortModel to = toNode.getInputsById().get(inputPort);
        if (from != null && to != null) {
            graphModel.createWire(to, from);
        }
    }

    private static void setOption(NodeModel nodeModel, String optionId, Object value) {
        Constant constant = nodeModel.getInputConstantsById().get(NodeOption.PORT_ID_PREFIX + optionId);
        if (constant == null) {
            return;
        }
        constant.setDefaultValue(value);
        constant.setValue(value);
    }
}
