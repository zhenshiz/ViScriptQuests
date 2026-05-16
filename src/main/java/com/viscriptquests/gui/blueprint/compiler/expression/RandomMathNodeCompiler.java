package com.viscriptquests.gui.blueprint.compiler.expression;

import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.CustomNodeModelImpl;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegister;
import com.viscriptquests.gui.blueprint.compiler.IQuestExpressionNodeCompiler;
import com.viscriptquests.gui.blueprint.compiler.QuestCompileContext;
import com.viscriptquests.gui.blueprint.node.math.MathRandomNode;
import com.viscriptquests.quest.data.QuestValueToken;

import java.util.ArrayList;
import java.util.List;

@LDLRegister(name = "math_random", registry = IQuestExpressionNodeCompiler.ID)
public class RandomMathNodeCompiler implements IQuestExpressionNodeCompiler {
    @Override
    public boolean supports(CustomNodeModelImpl node) {
        return node.getNode() instanceof MathRandomNode;
    }

    @Override
    public List<QuestValueToken> compileExpression(QuestCompileContext context, CustomNodeModelImpl node, int depth) {
        List<QuestValueToken> min = context.compileRuntimeValueExpression(node, "min", depth - 1);
        List<QuestValueToken> max = context.compileRuntimeValueExpression(node, "max", depth - 1);
        if (min == null || max == null) {
            return null;
        }
        ArrayList<QuestValueToken> expression = new ArrayList<>(min);
        expression.addAll(max);
        expression.add(QuestValueToken.operator(QuestValueToken.Kind.RANDOM));
        return expression;
    }
}
