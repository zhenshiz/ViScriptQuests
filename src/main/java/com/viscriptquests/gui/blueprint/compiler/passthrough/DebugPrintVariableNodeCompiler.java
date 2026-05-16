package com.viscriptquests.gui.blueprint.compiler.passthrough;

import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.CustomNodeModelImpl;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegister;
import com.viscriptquests.gui.blueprint.compiler.IQuestPassthroughNodeCompiler;
import com.viscriptquests.gui.blueprint.compiler.QuestCompileContext;
import com.viscriptquests.gui.blueprint.compiler.QuestPassthroughResult;
import com.viscriptquests.gui.blueprint.node.debug.DebugPrintVariableNode;
import com.viscriptquests.quest.data.DebugValuePrint;
import com.viscriptquests.quest.data.QuestDebugPrint;
import com.viscriptquests.quest.data.QuestValueToken;

import java.util.List;

@LDLRegister(name = "debug_print_var", registry = IQuestPassthroughNodeCompiler.ID)
public class DebugPrintVariableNodeCompiler implements IQuestPassthroughNodeCompiler {
    @Override
    public boolean supports(CustomNodeModelImpl node) {
        return node.getNode() instanceof DebugPrintVariableNode;
    }

    @Override
    public void compilePassthrough(QuestCompileContext context, CustomNodeModelImpl node, QuestPassthroughResult result) {
        DebugPrintValueResult print = compileDebugValuePrint(context, node);
        if (print == null) {
            return;
        }
        QuestDebugPrint debugPrint = result.addDebugPrint(print.message, context.getBool(node, "send_to_chat"));
        if (debugPrint != null && print.debugValuePrint != null) {
            debugPrint.valuePrints.add(print.debugValuePrint);
        }
    }

    private static DebugPrintValueResult compileDebugValuePrint(QuestCompileContext context, CustomNodeModelImpl node) {
        List<QuestValueToken> expression = context.compileRuntimeValueExpression(node, "value", 12);
        if (expression != null && !expression.isEmpty()) {
            DebugValuePrint debugValuePrint = new DebugValuePrint();
            debugValuePrint.placeholder = "__debug_value_" + node.getUid().toString().replace("-", "") + "__";
            debugValuePrint.expression.addAll(expression);
            return new DebugPrintValueResult("{" + debugValuePrint.placeholder + "}", debugValuePrint);
        }

        String varName = context.getString(node, "variable_name");
        if (!varName.isEmpty()) {
            return new DebugPrintValueResult("{" + varName + "}", null);
        }

        varName = context.findVariableName(node, "value");
        if (!varName.isEmpty()) {
            return new DebugPrintValueResult("{" + varName + "}", null);
        }

        Float constValue = context.tryEvaluateCompileTimeValue(node, "value");
        return constValue == null ? null
                : new DebugPrintValueResult(QuestCompileContext.formatFloatValue(constValue), null);
    }

    private record DebugPrintValueResult(String message, DebugValuePrint debugValuePrint) {
    }
}
