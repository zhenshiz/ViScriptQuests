package com.viscriptquests.gui.blueprint.compiler.expression;

import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.CustomNodeModelImpl;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegister;
import com.viscriptquests.gui.blueprint.compiler.IQuestExpressionNodeCompiler;
import com.viscriptquests.gui.blueprint.compiler.QuestCompileContext;
import com.viscriptquests.gui.blueprint.node.player.GetPlayerItemCountNode;
import com.viscriptquests.quest.data.QuestValueToken;

import java.util.List;

/**
 * 将玩家物品数量节点编译为按当前玩家背包求值的运行时表达式。
 */
@LDLRegister(name = "get_player_item_count", registry = IQuestExpressionNodeCompiler.ID)
public class GetPlayerItemCountNodeCompiler implements IQuestExpressionNodeCompiler {
    @Override
    public boolean supports(CustomNodeModelImpl node) {
        return node.getNode() instanceof GetPlayerItemCountNode;
    }

    @Override
    public List<QuestValueToken> compileExpression(QuestCompileContext context, CustomNodeModelImpl node, int depth) {
        return List.of(QuestValueToken.playerItemCount(
                context.getItemStack(node, "item_stack"),
                context.getItemMatchRule(node, "item_match_rule")
        ));
    }
}
