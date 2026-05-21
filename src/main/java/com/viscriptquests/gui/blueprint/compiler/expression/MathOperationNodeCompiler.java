package com.viscriptquests.gui.blueprint.compiler.expression;

import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.CustomNodeModelImpl;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegister;
import com.viscriptquests.gui.blueprint.compiler.IQuestExpressionNodeCompiler;
import com.viscriptquests.gui.blueprint.compiler.QuestCompileContext;
import com.viscriptquests.gui.blueprint.data.MathOperation;
import com.viscriptquests.gui.blueprint.node.math.MathOperationNode;
import com.viscriptquests.quest.data.QuestValueToken;

import java.util.ArrayList;
import java.util.List;

@LDLRegister(name = "math_operation", registry = IQuestExpressionNodeCompiler.ID)
public class MathOperationNodeCompiler implements IQuestExpressionNodeCompiler {
    @Override
    public boolean supports(CustomNodeModelImpl node) {
        return node.getNode() instanceof MathOperationNode;
    }

    @Override
    public List<QuestValueToken> compileExpression(QuestCompileContext context, CustomNodeModelImpl node, int depth) {
        MathOperation operation = MathOperationNode.operationOf(node);
        return switch (operation) {
            case ADD, SUBTRACT, MULTIPLY, DIVIDE -> compileVariadicExpression(context, node, operation, depth);
            case CLAMP -> compileClampExpression(context, node, depth);
            case RANDOM -> compileRandomExpression(context, node, depth);
        };
    }

    private static List<QuestValueToken> compileVariadicExpression(QuestCompileContext context,
                                                                   CustomNodeModelImpl node,
                                                                   MathOperation operation,
                                                                   int depth) {
        int inputCount = MathOperationNode.inputCountOf(node);
        List<QuestValueToken> expression = context.compileRuntimeValueExpression(node, MathOperationNode.inputId(1), depth - 1);
        if (expression == null) {
            return null;
        }
        ArrayList<QuestValueToken> result = new ArrayList<>(expression);
        for (int i = 2; i <= inputCount; i++) {
            List<QuestValueToken> next = context.compileRuntimeValueExpression(node, MathOperationNode.inputId(i), depth - 1);
            if (next == null) {
                return null;
            }
            result.addAll(next);
            result.add(QuestValueToken.operator(tokenKind(operation)));
        }
        return result;
    }

    private static List<QuestValueToken> compileClampExpression(QuestCompileContext context,
                                                                CustomNodeModelImpl node,
                                                                int depth) {
        List<QuestValueToken> value = context.compileRuntimeValueExpression(node, "value", depth - 1);
        List<QuestValueToken> min = context.compileRuntimeValueExpression(node, "min", depth - 1);
        List<QuestValueToken> max = context.compileRuntimeValueExpression(node, "max", depth - 1);
        if (value == null || min == null || max == null) {
            return null;
        }
        ArrayList<QuestValueToken> expression = new ArrayList<>(value);
        expression.addAll(min);
        expression.addAll(max);
        expression.add(QuestValueToken.operator(QuestValueToken.Kind.CLAMP));
        return expression;
    }

    private static List<QuestValueToken> compileRandomExpression(QuestCompileContext context,
                                                                 CustomNodeModelImpl node,
                                                                 int depth) {
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

    private static QuestValueToken.Kind tokenKind(MathOperation operation) {
        return switch (operation) {
            case SUBTRACT -> QuestValueToken.Kind.SUBTRACT;
            case MULTIPLY -> QuestValueToken.Kind.MULTIPLY;
            case DIVIDE -> QuestValueToken.Kind.DIVIDE;
            default -> QuestValueToken.Kind.ADD;
        };
    }
}
