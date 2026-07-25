package com.viscriptquests.gui.blueprint.node.reward;

import com.lowdragmc.lowdraglib2.nodegraphtookit.api.node.NodeAttribute;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.definition.IOptionDefinitionContext;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.definition.IPortDefinitionContext;
import com.viscriptquests.gui.blueprint.QuestBlueprintGraph;
import com.viscriptquests.gui.blueprint.node.QuestBlueprintNode;
import com.viscriptquests.quest.data.DisplayIcon;
import net.minecraft.network.chat.Component;

// 只用于任务书展示的奖励占位节点，不会编译成实际发放的 IReward。
@NodeAttribute(name = QuestBlueprintNode.ID + "reward_placeholder", group = QuestBlueprintNode.REWARD_GROUP, graphTypes = QuestBlueprintGraph.class)
public class RewardPlaceholderNode extends QuestBlueprintNode {
    public static final String DEFAULT_ICON_TEXTURE = "ldlib2:textures/gui/icon/help.png";

    public static DisplayIcon defaultIcon() {
        return DisplayIcon.texture(DEFAULT_ICON_TEXTURE);
    }

    @Override
    public Component getDisplayName() {
        return nodeName("reward_placeholder");
    }

    @Override
    public void onDefineOptions(IOptionDefinitionContext context) {
        displayIconOption(context, "reward_icon", defaultIcon());
        stringOption(context, "reward_tooltip", "");
    }

    @Override
    public void onDefinePorts(IPortDefinitionContext context) {
        rewardFlowPorts(context);
    }
}
