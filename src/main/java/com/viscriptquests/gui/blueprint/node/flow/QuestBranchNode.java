package com.viscriptquests.gui.blueprint.node.flow;

import com.lowdragmc.lowdraglib2.nodegraphtookit.api.node.NodeAttribute;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.definition.IPortDefinitionContext;
import com.viscriptquests.gui.blueprint.QuestBlueprintGraph;
import com.viscriptquests.gui.blueprint.node.QuestBlueprintNode;
import net.minecraft.network.chat.Component;

// 条件分支节点，根据 condition 输入端口的 BOOL 值选择执行路径
// condition 应连接到 CompareOperationNode 的 result 输出，由比较节点提供条件判断
@NodeAttribute(name = QuestBlueprintNode.ID + "quest_branch", group = QuestBlueprintNode.FLOW_GROUP, graphTypes = QuestBlueprintGraph.class)
public class QuestBranchNode extends QuestBlueprintNode {
    @Override
    public Component getDisplayName() {
        return nodeName("quest_branch");
    }

    @Override
    public void onDefinePorts(IPortDefinitionContext context) {
        inputFlow(context);
        boolInput(context, "condition", false);
        outputFlow(context, "true");
        outputFlow(context, "false");
    }
}
