package com.viscriptquests.gui.blueprint.compiler.passthrough;

import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.CustomNodeModelImpl;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegister;
import com.viscriptquests.gui.blueprint.compiler.IQuestPassthroughNodeCompiler;
import com.viscriptquests.gui.blueprint.compiler.QuestCompileContext;
import com.viscriptquests.gui.blueprint.compiler.QuestPassthroughResult;
import com.viscriptquests.gui.blueprint.node.debug.DebugPrintNode;

@LDLRegister(name = "debug_print", registry = IQuestPassthroughNodeCompiler.ID)
public class DebugPrintNodeCompiler implements IQuestPassthroughNodeCompiler {
    @Override
    public boolean supports(CustomNodeModelImpl node) {
        return node.getNode() instanceof DebugPrintNode;
    }

    @Override
    public void compilePassthrough(QuestCompileContext context, CustomNodeModelImpl node, QuestPassthroughResult result) {
        result.addDebugPrint(context.getPortString(node, "message"), context.getBool(node, "send_to_chat"));
    }
}
