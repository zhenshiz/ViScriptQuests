package com.viscriptquests.gui.blueprint.node.logic;

import com.lowdragmc.lowdraglib2.nodegraphtookit.api.node.NodeAttribute;
import com.viscriptquests.gui.blueprint.QuestBlueprintGraph;
import com.viscriptquests.gui.blueprint.node.QuestBlueprintNode;
import net.minecraft.network.chat.Component;

// 大于比较节点
@NodeAttribute(name = "compare_gt", group = QuestBlueprintNode.LOGIC_GROUP, graphTypes = QuestBlueprintGraph.class)
public class CompareGtNode extends CompareNode {
    @Override
    public Component getDisplayName() {
        return nodeName("compare_gt");
    }
}
