package com.viscriptquests.gui.blueprint.compiler.expression;

import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.CustomNodeModelImpl;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegister;
import com.viscriptquests.gui.blueprint.compiler.IQuestExpressionNodeCompiler;
import com.viscriptquests.gui.blueprint.compiler.QuestCompileContext;
import com.viscriptquests.gui.blueprint.node.scoreboard.GetScoreboardValueNode;
import com.viscriptquests.quest.data.QuestValueToken;

import java.util.List;

@LDLRegister(name = "get_scoreboard_value", registry = IQuestExpressionNodeCompiler.ID)
public class GetScoreboardValueNodeCompiler implements IQuestExpressionNodeCompiler {
    @Override
    public boolean supports(CustomNodeModelImpl node) {
        return node.getNode() instanceof GetScoreboardValueNode;
    }

    @Override
    public List<QuestValueToken> compileExpression(QuestCompileContext context, CustomNodeModelImpl node, int depth) {
        String objectiveName = context.getString(node, "objective_name");
        if (objectiveName.isBlank()) {
            return null;
        }
        return List.of(QuestValueToken.scoreboard(objectiveName, context.getString(node, "score_holder")));
    }
}
