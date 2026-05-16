package com.viscriptquests.gui.blueprint.compiler;

import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.CustomNodeModelImpl;
import com.lowdragmc.lowdraglib2.registry.ILDLRegister;
import com.viscriptquests.ViScriptQuests;

import java.util.function.Supplier;

// 处理带执行流输入/输出、但本身不是步骤节点的透传节点，例如调试打印、变量修改、未来的动作节点。
public interface IQuestPassthroughNodeCompiler extends ILDLRegister<IQuestPassthroughNodeCompiler, Supplier<IQuestPassthroughNodeCompiler>> {
    String ID = ViScriptQuests.MOD_ID + ":blueprint_passthrough_node_compiler";

    boolean supports(CustomNodeModelImpl node);

    void compilePassthrough(QuestCompileContext context, CustomNodeModelImpl node, QuestPassthroughResult result);
}
