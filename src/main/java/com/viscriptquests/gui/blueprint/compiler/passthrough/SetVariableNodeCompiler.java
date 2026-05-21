package com.viscriptquests.gui.blueprint.compiler.passthrough;

import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.CustomNodeModelImpl;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegister;
import com.viscriptquests.gui.blueprint.compiler.IQuestPassthroughNodeCompiler;
import com.viscriptquests.gui.blueprint.compiler.QuestCompileContext;
import com.viscriptquests.gui.blueprint.compiler.QuestPassthroughResult;
import com.viscriptquests.gui.blueprint.node.variable.SetVariableNode;
import com.viscriptquests.quest.data.QuestValueToken;
import com.viscriptquests.quest.data.VariableMutation;
import com.viscriptquests.quest.data.VariableMutationOp;

import java.util.List;

@LDLRegister(name = "set_variable", registry = IQuestPassthroughNodeCompiler.ID)
public class SetVariableNodeCompiler implements IQuestPassthroughNodeCompiler {
    @Override
    public boolean supports(CustomNodeModelImpl node) {
        return node.getNode() instanceof SetVariableNode;
    }

    @Override
    public void compilePassthrough(QuestCompileContext context, CustomNodeModelImpl node, QuestPassthroughResult result) {
        String variableName = resolveVariableName(context, node);
        if (variableName.isEmpty()) {
            return;
        }
        List<QuestValueToken> expression = context.compileRuntimeValueExpression(node, "value", 12);
        if (expression == null || expression.isEmpty()) {
            expression = List.of(QuestValueToken.constant(context.getPortFloat(node, "value")));
        }

        VariableMutation mutation = new VariableMutation();
        mutation.variableName = variableName;
        mutation.operation = VariableMutationOp.SET;
        mutation.expression.addAll(expression);
        result.mutations.add(mutation);
    }

    private static String resolveVariableName(QuestCompileContext context, CustomNodeModelImpl node) {
        return context.getString(node, "variable_name");
    }
}
