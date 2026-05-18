package com.viscriptquests.gui.blueprint;

import com.lowdragmc.lowdraglib2.nodegraphtookit.api.node.Node;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.node.NodeAttribute;
import com.viscriptquests.gui.blueprint.node.QuestBlueprintNode;
import com.viscriptquests.gui.blueprint.node.flow.QuestBranchNode;
import com.viscriptquests.gui.blueprint.node.flow.QuestEndNode;
import com.viscriptquests.gui.blueprint.node.flow.QuestJoinNode;
import com.viscriptquests.gui.blueprint.node.flow.QuestStartNode;
import com.viscriptquests.gui.blueprint.node.flow.SubQuestNode;
import com.viscriptquests.quest.data.QuestFlowNode;
import com.viscriptquests.quest.data.QuestJoinMode;

import java.util.LinkedHashMap;
import java.util.Map;

// 从 QuestBlueprintGraph 的节点注册表提取 flow 节点类型，避免运行时数据层硬编码枚举。
public final class QuestBlueprintFlowTypes {
    public static final String START = typeOf(QuestStartNode.class);
    public static final String SUB_QUEST = typeOf(SubQuestNode.class);
    public static final String BRANCH = typeOf(QuestBranchNode.class);
    public static final String JOIN = typeOf(QuestJoinNode.class);
    public static final String END = typeOf(QuestEndNode.class);

    public static Map<String, Class<? extends Node>> getFlowNodeTypes() {
        return getNodeTypesByGroup(QuestBlueprintNode.FLOW_GROUP);
    }

    public static Map<String, Class<? extends Node>> getNodeTypesByGroup(String group) {
        Map<String, Class<? extends Node>> result = new LinkedHashMap<>();
        for (var holder : QuestBlueprintGraph.NODE_REGISTRY.getRegistry()) {
            NodeAttribute attribute = holder.annotation();
            if (!group.equals(attribute.group())) {
                continue;
            }
            result.put(attribute.name(), holder.value());
        }
        return result;
    }

    public static String typeOf(Class<? extends Node> nodeClass) {
        for (var holder : QuestBlueprintGraph.NODE_REGISTRY.getRegistry()) {
            if (holder.value() != nodeClass) {
                continue;
            }
            NodeAttribute attribute = holder.annotation();
            if (QuestBlueprintNode.FLOW_GROUP.equals(attribute.group())) {
                return attribute.name();
            }
        }

        NodeAttribute attribute = nodeClass.getAnnotation(NodeAttribute.class);
        if (attribute != null && QuestBlueprintNode.FLOW_GROUP.equals(attribute.group())) {
            return attribute.name();
        }
        throw new IllegalArgumentException("不是任务蓝图 flow 节点: " + nodeClass.getName());
    }

    public static QuestFlowNode createStart() {
        return create("", START);
    }

    public static QuestFlowNode createSubQuest(String stepId) {
        QuestFlowNode node = create(stepId, SUB_QUEST);
        node.stepId = stepId;
        return node;
    }

    public static QuestFlowNode createBranch(String nodeId) {
        return create(nodeId, BRANCH);
    }

    public static QuestFlowNode createJoin(String nodeId, QuestJoinMode mode, int requiredCount) {
        QuestFlowNode node = create(nodeId, JOIN);
        node.joinMode = mode;
        node.requiredCount = requiredCount;
        return node;
    }

    public static QuestFlowNode createEnd(String nodeId, boolean success) {
        QuestFlowNode node = create(nodeId, END);
        node.success = success;
        return node;
    }

    public static boolean isSubQuest(QuestFlowNode node) {
        return isType(node, SUB_QUEST);
    }

    public static boolean isJoin(QuestFlowNode node) {
        return isType(node, JOIN);
    }

    public static boolean isEnd(QuestFlowNode node) {
        return isType(node, END);
    }

    public static boolean isType(QuestFlowNode node, String type) {
        return node != null && type.equals(node.type);
    }

    private static QuestFlowNode create(String nodeId, String type) {
        QuestFlowNode node = new QuestFlowNode();
        node.nodeId = nodeId;
        node.type = type;
        return node;
    }
}
