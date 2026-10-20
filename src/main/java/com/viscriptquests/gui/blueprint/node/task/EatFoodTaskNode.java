package com.viscriptquests.gui.blueprint.node.task;

import com.lowdragmc.lowdraglib2.nodegraphtookit.api.node.NodeAttribute;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.definition.IOptionDefinitionContext;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.definition.IPortDefinitionContext;
import com.viscriptquests.gui.blueprint.QuestBlueprintGraph;
import com.viscriptquests.gui.blueprint.QuestBlueprintTypes;
import com.viscriptquests.gui.blueprint.node.QuestBlueprintNode;
import com.viscriptquests.quest.data.ItemUseHand;
import net.minecraft.network.chat.Component;

/**
 * 配置食物或饮品、物品组件、使用手部及食用或饮用次数的目标节点。
 */
@NodeAttribute(name = QuestBlueprintNode.ID + "eat_food_task", group = QuestBlueprintNode.TASK_GROUP, graphTypes = QuestBlueprintGraph.class)
public class EatFoodTaskNode extends QuestBlueprintNode {
    @Override
    public Component getDisplayName() {
        return nodeName("eat_food_task");
    }

    @Override
    public void onDefineOptions(IOptionDefinitionContext context) {
        boolOption(context, "any_food", true);
        if (Boolean.FALSE.equals(getOptionValue("any_food"))) {
            itemStackOption(context, "item_stack");
            itemMatchRuleOption(context, "item_match_rule");
        }
        enumOption(context, "use_hand", QuestBlueprintTypes.ITEM_USE_HAND, ItemUseHand.ANY);
        taskCommonOptions(context);
    }

    @Override
    public boolean retainsOptionValue(String optionId) {
        return "item_stack".equals(optionId) || "item_match_rule".equals(optionId)
                || super.retainsOptionValue(optionId);
    }

    @Override
    public void onDefinePorts(IPortDefinitionContext context) {
        taskFlowPorts(context);
        intInput(context, "eat_count", 1);
    }
}
