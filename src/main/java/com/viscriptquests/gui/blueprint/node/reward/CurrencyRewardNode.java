package com.viscriptquests.gui.blueprint.node.reward;

import com.lowdragmc.lowdraglib2.nodegraphtookit.api.node.NodeAttribute;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.definition.IOptionDefinitionContext;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.definition.IPortDefinitionContext;
import com.viscriptquests.gui.blueprint.QuestBlueprintGraph;
import com.viscriptquests.gui.blueprint.node.QuestBlueprintNode;
import com.viscriptshop.ViscriptShop;
import net.minecraft.network.chat.Component;

// ViScriptShop 货币奖励节点，只保存货币数量；是否能实际发放由运行时联动层判断。
@NodeAttribute(name = "currency_reward", group = QuestBlueprintNode.REWARD_GROUP, graphTypes = QuestBlueprintGraph.class, modID = ViscriptShop.MOD_ID)
public class CurrencyRewardNode extends QuestBlueprintNode {
    @Override
    public Component getDisplayName() {
        return nodeName("currency_reward");
    }

    @Override
    public void onDefineOptions(IOptionDefinitionContext context) {
        intOption(context, "currency", 1);
        rewardCommonOptions(context);
    }

    @Override
    public void onDefinePorts(IPortDefinitionContext context) {
        rewardFlowPorts(context);
    }
}
