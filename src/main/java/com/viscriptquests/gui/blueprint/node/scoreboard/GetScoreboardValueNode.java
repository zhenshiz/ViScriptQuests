package com.viscriptquests.gui.blueprint.node.scoreboard;

import com.lowdragmc.lowdraglib2.nodegraphtookit.api.node.NodeAttribute;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.definition.IOptionDefinitionContext;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.definition.IPortDefinitionContext;
import com.viscriptquests.gui.blueprint.QuestBlueprintGraph;
import com.viscriptquests.gui.blueprint.node.QuestBlueprintNode;
import net.minecraft.network.chat.Component;

@NodeAttribute(name = "get_scoreboard_value", group = QuestBlueprintNode.SCOREBOARD_GROUP, graphTypes = QuestBlueprintGraph.class)
public class GetScoreboardValueNode extends QuestBlueprintNode {
    @Override
    public Component getDisplayName() {
        return nodeName("get_scoreboard_value");
    }

    @Override
    public void onDefineOptions(IOptionDefinitionContext context) {
        stringOption(context, "objective_name", "");
        stringOption(context, "score_holder", "");
    }

    @Override
    public void onDefinePorts(IPortDefinitionContext context) {
        floatOutput(context, "result");
    }
}
