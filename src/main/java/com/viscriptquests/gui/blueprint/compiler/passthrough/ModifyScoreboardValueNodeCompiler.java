package com.viscriptquests.gui.blueprint.compiler.passthrough;

import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.CustomNodeModelImpl;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegister;
import com.viscriptquests.gui.blueprint.compiler.IQuestPassthroughNodeCompiler;
import com.viscriptquests.gui.blueprint.compiler.QuestCompileContext;
import com.viscriptquests.gui.blueprint.compiler.QuestPassthroughResult;
import com.viscriptquests.gui.blueprint.node.scoreboard.ModifyScoreboardValueNode;
import com.viscriptquests.quest.data.QuestValueToken;
import com.viscriptquests.quest.data.ScoreboardMutation;

import java.util.List;

@LDLRegister(name = "modify_scoreboard_value", registry = IQuestPassthroughNodeCompiler.ID)
public class ModifyScoreboardValueNodeCompiler implements IQuestPassthroughNodeCompiler {
    @Override
    public boolean supports(CustomNodeModelImpl node) {
        return node.getNode() instanceof ModifyScoreboardValueNode;
    }

    @Override
    public void compilePassthrough(QuestCompileContext context, CustomNodeModelImpl node, QuestPassthroughResult result) {
        String objectiveName = context.getString(node, "objective_name");
        if (objectiveName.isBlank()) {
            return;
        }
        List<QuestValueToken> expression = context.compileRuntimeValueExpression(node, "value", 12);
        if (expression == null || expression.isEmpty()) {
            expression = List.of(QuestValueToken.constant(context.getPortFloat(node, "value")));
        }

        ScoreboardMutation mutation = new ScoreboardMutation();
        mutation.objectiveName = objectiveName;
        mutation.scoreHolder = context.getString(node, "score_holder");
        mutation.operation = ModifyScoreboardValueNode.operationOf(node);
        mutation.expression.addAll(expression);
        result.scoreboardMutations.add(mutation);
    }
}
