package com.viscriptquests.gui.blueprint.compiler;

import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.CustomNodeModelImpl;
import com.lowdragmc.lowdraglib2.registry.ILDLRegister;
import com.viscriptquests.ViScriptQuests;
import com.viscriptquests.quest.data.QuestValueToken;

import java.util.List;
import java.util.function.Supplier;

// 将可输出数值的蓝图节点编译成运行时表达式 token 的扩展点。
public interface IQuestExpressionNodeCompiler extends ILDLRegister<IQuestExpressionNodeCompiler, Supplier<IQuestExpressionNodeCompiler>> {
    String ID = ViScriptQuests.MOD_ID + ":blueprint_expression_node_compiler";

    boolean supports(CustomNodeModelImpl node);

    List<QuestValueToken> compileExpression(QuestCompileContext context, CustomNodeModelImpl node, int depth);
}
