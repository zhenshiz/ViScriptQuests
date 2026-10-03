package com.viscriptquests.gui.blueprint.node.task;

import com.lowdragmc.lowdraglib2.nodegraphtookit.api.node.NodeAttribute;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.definition.IOptionDefinitionContext;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.definition.IPortDefinitionContext;
import com.viscriptquests.gui.blueprint.QuestBlueprintGraph;
import com.viscriptquests.gui.blueprint.QuestBlueprintTypes;
import com.viscriptquests.gui.blueprint.node.QuestBlueprintNode;
import com.viscriptquests.quest.data.QuestSubmitMode;
import com.viscriptshop.ViscriptShop;
import net.minecraft.network.chat.Component;

@NodeAttribute(name = QuestBlueprintNode.ID + "currency_task", group = QuestBlueprintNode.TASK_GROUP,
        graphTypes = QuestBlueprintGraph.class, modID = ViscriptShop.MOD_ID)
public class CurrencyTaskNode extends QuestBlueprintNode {
    @Override
    public Component getDisplayName() { return nodeName("currency_task"); }

    @Override
    public void onDefineOptions(IOptionDefinitionContext context) {
        boolOption(context, "consume_currency", true);
        enumOption(context, "submit_mode", QuestBlueprintTypes.SUBMIT_MODE, QuestSubmitMode.AUTO);
        taskCommonOptions(context);
    }

    @Override
    public void onDefinePorts(IPortDefinitionContext context) {
        taskFlowPorts(context);
        intInput(context, "currency", 1);
    }
}
