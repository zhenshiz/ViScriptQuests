package com.viscriptquests.gui.blueprint.compiler.expression;

import com.lowdragmc.lowdraglib2.nodegraphtookit.api.node.Node;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.CustomNodeModelImpl;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegister;
import com.viscriptquests.gui.blueprint.compiler.IQuestExpressionNodeCompiler;
import com.viscriptquests.gui.blueprint.compiler.QuestCompileContext;
import com.viscriptquests.gui.blueprint.node.math.MathAddNode;
import com.viscriptquests.gui.blueprint.node.math.MathBinaryNode;
import com.viscriptquests.gui.blueprint.node.math.MathDivideNode;
import com.viscriptquests.gui.blueprint.node.math.MathMultiplyNode;
import com.viscriptquests.gui.blueprint.node.math.MathSubtractNode;
import com.viscriptquests.quest.data.QuestValueToken;

import java.util.ArrayList;
import java.util.List;

@LDLRegister(name = "binary_math", registry = IQuestExpressionNodeCompiler.ID)
public class BinaryMathNodeCompiler implements IQuestExpressionNodeCompiler {
    @Override
    public boolean supports(CustomNodeModelImpl node) {
        return node.getNode() instanceof MathBinaryNode;
    }

    @Override
    public List<QuestValueToken> compileExpression(QuestCompileContext context, CustomNodeModelImpl node, int depth) {
        List<QuestValueToken> left = context.compileRuntimeValueExpression(node, "value_a", depth - 1);
        List<QuestValueToken> right = context.compileRuntimeValueExpression(node, "value_b", depth - 1);
        if (left == null || right == null) {
            return null;
        }
        ArrayList<QuestValueToken> expression = new ArrayList<>(left);
        expression.addAll(right);
        expression.add(QuestValueToken.operator(binaryMathKind(node.getNode())));
        return expression;
    }

    private static QuestValueToken.Kind binaryMathKind(Node node) {
        if (node instanceof MathSubtractNode) return QuestValueToken.Kind.SUBTRACT;
        if (node instanceof MathMultiplyNode) return QuestValueToken.Kind.MULTIPLY;
        if (node instanceof MathDivideNode) return QuestValueToken.Kind.DIVIDE;
        if (node instanceof MathAddNode) return QuestValueToken.Kind.ADD;
        return QuestValueToken.Kind.ADD;
    }
}
